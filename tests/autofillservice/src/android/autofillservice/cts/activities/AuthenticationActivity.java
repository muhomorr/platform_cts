/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.autofillservice.cts.activities;

import static android.autofillservice.cts.testcore.CannedFillResponse.ResponseType.NULL;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.autofillservice.cts.R;
import android.autofillservice.cts.testcore.CannedFillResponse;
import android.autofillservice.cts.testcore.CannedFillResponse.CannedDataset;
import android.autofillservice.cts.testcore.Helper;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * This class simulates authentication at the dataset at reponse level
 */
public class AuthenticationActivity extends AbstractAutoFillActivity {

    private static final String TAG = "AuthenticationActivity";
    private static final String EXTRA_DATASET_ID = "dataset_id";
    private static final String EXTRA_RESPONSE_ID = "response_id";

    /**
     * When launched with this intent, it will pass it back to the
     * {@link AutofillManager#EXTRA_CLIENT_STATE} of the result.
     */
    private static final String EXTRA_OUTPUT_CLIENT_STATE = "output_client_state";

    /**
     * When launched with this intent, it will pass it back to the
     * {@link AutofillManager#EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET} of the result.
     */
    private static final String EXTRA_OUTPUT_IS_EPHEMERAL_DATASET = "output_is_ephemeral_dataset";

    /**
     * When launched with a non-null intent associated with this extra, the intent will be returned
     * as the response.
     */
    private static final String EXTRA_RESPONSE_INTENT = "response_intent";


    private static final int MSG_WAIT_FOR_LATCH = 1;
    private static final int MSG_REQUEST_AUTOFILL = 2;

    private static Bundle sData;
    private static InlineSuggestionsRequest sInlineSuggestionsRequest;
    private static final SparseArray<CannedDataset> sDatasets = new SparseArray<>();
    private static final SparseArray<CannedFillResponse> sResponses = new SparseArray<>();
    private static final ArrayList<PendingIntent> sPendingIntents = new ArrayList<>();

    private static Object sLock = new Object();

    // Guarded by sLock
    private static int sResultCode;

    // Guarded by sLock
    // Used to block response until it's counted down.
    private static CountDownLatch sResponseLatch;

    // Guarded by sLock
    // Used to request autofill for a autofillable view in AuthenticationActivity
    private static boolean sRequestAutofill;

    private Handler mHandler;

    private EditText mPasswordEditText;
    private Button mYesButton;

    public static void resetStaticState() {
        setResultCode(null, RESULT_OK);
        setRequestAutofillForAuthenticationActivity(/* requestAutofill */ false);
        sDatasets.clear();
        sResponses.clear();
        sData = null;
        sInlineSuggestionsRequest = null;
        for (int i = 0; i < sPendingIntents.size(); i++) {
            final PendingIntent pendingIntent = sPendingIntents.get(i);
            Log.d(TAG, "Cancelling " + pendingIntent);
            pendingIntent.cancel();
        }
    }

    /**
     * Creates an {@link IntentSender} with the given unique id for the given dataset.
     */
    public static IntentSender createSender(Context context, int id, CannedDataset dataset) {
        return createSender(context, id, dataset, null);
    }

    public static IntentSender createSender(Context context, Intent responseIntent) {
        return createSender(context, null, 1, null, null, responseIntent);
    }

    public static IntentSender createSender(Context context, int id,
            CannedDataset dataset, Bundle outClientState) {
        return createSender(context, id, dataset, outClientState, null);
    }

    public static IntentSender createSender(Context context, int id,
            CannedDataset dataset, Bundle outClientState, Boolean isEphemeralDataset) {
        Preconditions.checkArgument(id > 0, "id must be positive");
        Preconditions.checkState(sDatasets.get(id) == null, "already have id");
        sDatasets.put(id, dataset);
        return createSender(context, EXTRA_DATASET_ID, id, outClientState, isEphemeralDataset,
                null);
    }

    /**
     * Creates an {@link IntentSender} with the given unique id for the given fill response.
     */
    public static IntentSender createSender(Context context, int id, CannedFillResponse response) {
        return createSender(context, id, response, null);
    }

    public static IntentSender createSender(Context context, int id,
            CannedFillResponse response, Bundle outData) {
        Preconditions.checkArgument(id > 0, "id must be positive");
        Preconditions.checkState(sResponses.get(id) == null, "already have id");
        sResponses.put(id, response);
        return createSender(context, EXTRA_RESPONSE_ID, id, outData, null, null);
    }

    private static IntentSender createSender(Context context, String extraName, int id,
            Bundle outClientState, Boolean isEphemeralDataset, Intent responseIntent) {
        Intent intent = new Intent(context, AuthenticationActivity.class);
        intent.putExtra(extraName, id);
        if (outClientState != null) {
            Log.d(TAG, "Create with " + outClientState + " as " + EXTRA_OUTPUT_CLIENT_STATE);
            intent.putExtra(EXTRA_OUTPUT_CLIENT_STATE, outClientState);
        }
        if (isEphemeralDataset != null) {
            Log.d(TAG, "Create with " + isEphemeralDataset + " as "
                    + EXTRA_OUTPUT_IS_EPHEMERAL_DATASET);
            intent.putExtra(EXTRA_OUTPUT_IS_EPHEMERAL_DATASET, isEphemeralDataset);
        }
        intent.putExtra(EXTRA_RESPONSE_INTENT, responseIntent);
        final PendingIntent pendingIntent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_MUTABLE);
        sPendingIntents.add(pendingIntent);
        return pendingIntent.getIntentSender();
    }

    /**
     * Creates an {@link IntentSender} with the given unique id.
     */
    public static IntentSender createSender(Context context, int id) {
        Preconditions.checkArgument(id > 0, "id must be positive");
        return PendingIntent
                .getActivity(context, id, new Intent(context, AuthenticationActivity.class),
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                .getIntentSender();
    }

    public static Bundle getData() {
        final Bundle data = sData;
        sData = null;
        return data;
    }

    public static InlineSuggestionsRequest getInlineSuggestionsRequest() {
        final InlineSuggestionsRequest request = sInlineSuggestionsRequest;
        sInlineSuggestionsRequest = null;
        return request;
    }

    /**
     * Sets the value that's passed to {@link Activity#setResult(int, Intent)} when on
     * {@link Activity#onCreate(Bundle)}.
     */
    public static void setResultCode(int resultCode) {
        synchronized (sLock) {
            sResultCode = resultCode;
        }
    }

    /**
     * Sets the value that's passed to {@link Activity#setResult(int, Intent)}, but only calls it
     * after the {@code latch}'s countdown reaches {@code 0}.
     */
    public static void setResultCode(CountDownLatch latch, int resultCode) {
        synchronized (sLock) {
            sResponseLatch = latch;
            sResultCode = resultCode;
        }
    }

    public static void setRequestAutofillForAuthenticationActivity(boolean requestAutofill) {
        synchronized (sLock) {
            sRequestAutofill = requestAutofill;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.authentication_activity);

        mPasswordEditText = findViewById(R.id.password);
        mYesButton = findViewById(R.id.yes);
        mYesButton.setOnClickListener(view -> doIt());

        mHandler = new Handler(Looper.getMainLooper(), (m) -> {
            switch (m.what) {
                case MSG_WAIT_FOR_LATCH:
                    waitForLatchAndDoIt();
                    break;
                case MSG_REQUEST_AUTOFILL:
                    requestFocusOnPassword();
                    break;
                default:
                    throw new IllegalArgumentException("invalid message: " + m);
            }
            return true;
        });

        if (sResponseLatch != null) {
            Log.d(TAG, "Delaying message until latch is counted down");
            mHandler.dispatchMessage(mHandler.obtainMessage(MSG_WAIT_FOR_LATCH));
        } else if (sRequestAutofill) {
            mHandler.dispatchMessage(mHandler.obtainMessage(MSG_REQUEST_AUTOFILL));
        } else {
            doIt();
        }
    }

    private void requestFocusOnPassword() {
        syncRunOnUiThread(() -> mPasswordEditText.requestFocus());
    }

    private void waitForLatchAndDoIt() {
        try {
            final boolean called = sResponseLatch.await(5, TimeUnit.SECONDS);
            if (!called) {
                throw new IllegalStateException("latch not called in 5 seconds");
            }
            doIt();
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new IllegalStateException("interrupted");
        }
    }

    private void doIt() {
        final int resultCode;
        synchronized (sLock) {
            resultCode = sResultCode;
        }

        // If responseIntent is provided, use that to return, otherwise contstruct the response.
        Intent responseIntent = getIntent().getParcelableExtra(EXTRA_RESPONSE_INTENT, Intent.class);
        if (responseIntent != null) {
            Log.d(TAG, "Returning code " + resultCode);
            setResult(resultCode, responseIntent);
            finish();
            return;
        }

        // We should get the assist structure...
        final AssistStructure structure = getIntent().getParcelableExtra(
                AutofillManager.EXTRA_ASSIST_STRUCTURE);
        assertWithMessage("structure not called").that(structure).isNotNull();

        // and the bundle
        sData = getIntent().getBundleExtra(AutofillManager.EXTRA_CLIENT_STATE);
        sInlineSuggestionsRequest = getIntent().getParcelableExtra(
                AutofillManager.EXTRA_INLINE_SUGGESTIONS_REQUEST);
        final CannedFillResponse response =
                sResponses.get(getIntent().getIntExtra(EXTRA_RESPONSE_ID, 0));
        final CannedDataset dataset =
                sDatasets.get(getIntent().getIntExtra(EXTRA_DATASET_ID, 0));

        final Parcelable result;

        final Function<String, AssistStructure.ViewNode> nodeResolver =
                (id) -> Helper.findNodeByResourceId(structure, id);
        final Function<String, AutofillId> autofillPccResolver =
                (id)-> {
                    AssistStructure.ViewNode node = nodeResolver.apply(id);
                    if (node == null) {
                        return null;
                    }
                    return node.getAutofillId();
                };
        if (response != null) {
            if (response.getResponseType() == NULL) {
                result = null;
            } else {
                result = response.asPccFillResponse(/* contexts= */ null, nodeResolver);
            }
        } else if (dataset != null) {
            result = dataset.asDatasetForPcc(autofillPccResolver);
        } else {
            throw new IllegalStateException("no dataset or response");
        }

        // Pass on the auth result
        final Intent intent = new Intent();
        intent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, result);

        final Bundle outClientState = getIntent().getBundleExtra(EXTRA_OUTPUT_CLIENT_STATE);
        if (outClientState != null) {
            Log.d(TAG, "Adding " + outClientState + " as " + AutofillManager.EXTRA_CLIENT_STATE);
            intent.putExtra(AutofillManager.EXTRA_CLIENT_STATE, outClientState);
        }
        if (getIntent().getExtras().containsKey(EXTRA_OUTPUT_IS_EPHEMERAL_DATASET)) {
            final boolean isEphemeralDataset = getIntent().getBooleanExtra(
                    EXTRA_OUTPUT_IS_EPHEMERAL_DATASET, false);
            Log.d(TAG, "Adding " + isEphemeralDataset + " as "
                    + AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET);
            intent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET,
                    isEphemeralDataset);
        }
        Log.d(TAG, "Returning code " + resultCode);
        setResult(resultCode, intent);

        // Done
        finish();
    }
}
