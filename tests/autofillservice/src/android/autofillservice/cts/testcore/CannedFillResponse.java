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
package android.autofillservice.cts.testcore;

import static android.autofillservice.cts.testcore.Helper.createInlinePresentation;
import static android.autofillservice.cts.testcore.Helper.createPresentation;
import static android.autofillservice.cts.testcore.Helper.getAutofillIds;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.content.IntentSender;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.service.autofill.Field;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillResponse;
import android.service.autofill.InlinePresentation;
import android.service.autofill.Presentations;
import android.service.autofill.SaveInfo;
import android.service.autofill.UserData;
import android.util.Log;
import android.util.Pair;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Helper class used to produce a {@link FillResponse} based on expected fields that should be
 * present in the {@link AssistStructure}.
 *
 * <p>Typical usage:
 *
 * <pre class="prettyprint">
 * InstrumentedAutoFillService.getReplier().addResponse(new CannedFillResponse.Builder()
 *               .addDataset(new CannedDataset.Builder("dataset_name")
 *                   .setField("resource_id1", AutofillValue.forText("value1"))
 *                   .setField("resource_id2", AutofillValue.forText("value2"))
 *                   .build())
 *               .build());
 * </pre class="prettyprint">
 */
public final class CannedFillResponse {

    private static final String TAG = CannedFillResponse.class.getSimpleName();

    private final ResponseType mResponseType;
    private final List<CannedDataset> mDatasets;
    private final String mFailureMessage;
    private final int mSaveType;
    private final String[] mRequiredSavableIds;
    private final String[] mOptionalSavableIds;
    private final AutofillId[] mRequiredSavableAutofillIds;
    private final CharSequence mSaveDescription;
    private final Bundle mExtras;
    private final RemoteViews mPresentation;
    private final InlinePresentation mInlinePresentation;
    private final RemoteViews mHeader;
    private final RemoteViews mFooter;
    private final IntentSender mAuthentication;
    private final String[] mAuthenticationIds;
    private final String[] mIgnoredIds;
    private final int mNegativeActionStyle;
    private final IntentSender mNegativeActionListener;
    private final int mPositiveActionStyle;
    private final int mSaveInfoFlags;
    private final int mFillResponseFlags;
    private final AutofillId mSaveTriggerId;
    private final long mDisableDuration;
    private final String[] mFieldClassificationIds;
    private final boolean mFieldClassificationIdsOverflow;
    private final SaveInfoDecorator mSaveInfoDecorator;
    private final UserData mUserData;
    private final DoubleVisitor<List<FillContext>, FillResponse.Builder> mVisitor;
    private DoubleVisitor<List<FillContext>, SaveInfo.Builder> mSaveInfoVisitor;
    private final int[] mCancelIds;
    private final String[] mDialogTriggerIds;
    private final RemoteViews mDialogHeaderPresentation;
    private final int mIconResourceId;
    private final int mServiceDisplayNameResourceId;
    private final boolean mShowFillDialogIcon;
    private final boolean mShowSaveDialogIcon;


    private CannedFillResponse(Builder builder) {
        mResponseType = builder.mResponseType;
        mDatasets = builder.mDatasets;
        mFailureMessage = builder.mFailureMessage;
        mRequiredSavableIds = builder.mRequiredSavableIds;
        mRequiredSavableAutofillIds = builder.mRequiredSavableAutofillIds;
        mOptionalSavableIds = builder.mOptionalSavableIds;
        mSaveDescription = builder.mSaveDescription;
        mSaveType = builder.mSaveType;
        mExtras = builder.mExtras;
        mPresentation = builder.mPresentation;
        mInlinePresentation = builder.mInlinePresentation;
        mHeader = builder.mHeader;
        mFooter = builder.mFooter;
        mAuthentication = builder.mAuthentication;
        mAuthenticationIds = builder.mAuthenticationIds;
        mIgnoredIds = builder.mIgnoredIds;
        mNegativeActionStyle = builder.mNegativeActionStyle;
        mNegativeActionListener = builder.mNegativeActionListener;
        mPositiveActionStyle = builder.mPositiveActionStyle;
        mSaveInfoFlags = builder.mSaveInfoFlags;
        mFillResponseFlags = builder.mFillResponseFlags;
        mSaveTriggerId = builder.mSaveTriggerId;
        mDisableDuration = builder.mDisableDuration;
        mFieldClassificationIds = builder.mFieldClassificationIds;
        mFieldClassificationIdsOverflow = builder.mFieldClassificationIdsOverflow;
        mSaveInfoDecorator = builder.mSaveInfoDecorator;
        mUserData = builder.mUserData;
        mVisitor = builder.mVisitor;
        mSaveInfoVisitor = builder.mSaveInfoVisitor;
        mCancelIds = builder.mCancelIds;
        mDialogTriggerIds = builder.mDialogTriggerIds;
        mDialogHeaderPresentation = builder.mDialogHeaderPresentation;
        mIconResourceId = builder.mIconResourceId;
        mServiceDisplayNameResourceId = builder.mServiceDisplayNameResourceId;
        mShowFillDialogIcon = builder.mShowFillDialogIcon;
        mShowSaveDialogIcon = builder.mShowSaveDialogIcon;
    }

    /**
     * Constant used to pass a {@code null} response to the
     * {@link FillCallback#onSuccess(FillResponse)} method.
     */
    public static final CannedFillResponse NO_RESPONSE =
            new Builder(ResponseType.NULL).build();

    /**
     * Constant used to fail the test when an expected request was made.
     */
    public static final CannedFillResponse NO_MOAR_RESPONSES =
            new Builder(ResponseType.NO_MORE).build();

    /**
     * Constant used to emulate a timeout by not calling any method on {@link FillCallback}.
     */
    public static final CannedFillResponse DO_NOT_REPLY_RESPONSE =
            new Builder(ResponseType.TIMEOUT).build();

    /**
     * Constant used to call {@link FillCallback#onFailure(CharSequence)} method.
     */
    public static final CannedFillResponse FAIL =
            new Builder(ResponseType.FAILURE).build();

    public String getFailureMessage() {
        return mFailureMessage;
    }

    public ResponseType getResponseType() {
        return mResponseType;
    }

    /**
     * Creates a new response, replacing the dataset field ids by the real ids from the assist
     * structure.
     */
    public FillResponse asFillResponse(@Nullable List<FillContext> contexts,
            @NonNull Function<String, ViewNode> nodeResolver) {
        return asFillResponseWithAutofillId(contexts, (id)-> {
            ViewNode node = nodeResolver.apply(id);
            if (node == null) {
                throw new AssertionError("No node with resource id " + id);
            }
            return node.getAutofillId();
        });
    }

    public FillResponse asFillResponseWithAutofillId(@Nullable List<FillContext> contexts,
            @NonNull Function<String, AutofillId> autofillIdResolver) {
        return asFillResponseWithAutofillId(contexts, autofillIdResolver,
            (cannedDataset) -> {
                return cannedDataset.asDatasetWithAutofillIdResolver(autofillIdResolver);
            });
    }

    private Function<String, AutofillId> getAutofillIdResolver(
             @NonNull Function<String, ViewNode> nodeResolver) {
        return (id) -> {
            ViewNode node = nodeResolver.apply(id);
            if (node == null) {
                throw new AssertionError("No node with resource id " + id);
            }
            return node.getAutofillId();
        };
    }

    public FillResponse asPccFillResponse(@Nullable List<FillContext> contexts,
            @NonNull Function<String, ViewNode> nodeResolver) {
        final Function<String, AutofillId> autofillPccResolver =
                (id)-> {
                    ViewNode node = nodeResolver.apply(id);
                    if (node == null) {
                        return null;
                    }
                    return node.getAutofillId();
                };
        return asFillResponseWithAutofillId(
                contexts,
                autofillPccResolver,
                (cannedDataset) -> cannedDataset.asDatasetForPcc(autofillPccResolver));
    }

    /**
     * Creates a new response, replacing the dataset field ids by the real ids from the assist
     * structure.
     */
    public FillResponse asFillResponseWithAutofillId(@Nullable List<FillContext> contexts,
            @NonNull Function<String, AutofillId> autofillIdResolver,
            Function<CannedDataset, Dataset> cannedDatasetToDataset) {
        final FillResponse.Builder builder = new FillResponse.Builder()
                .setFlags(mFillResponseFlags);
        if (mDatasets != null) {
            for (CannedDataset cannedDataset : mDatasets) {
                final Dataset dataset = cannedDatasetToDataset.apply(cannedDataset);
                assertWithMessage("Cannot create dataset").that(dataset).isNotNull();
                builder.addDataset(dataset);
            }
        }
        final SaveInfo.Builder saveInfoBuilder;
        if (mRequiredSavableIds != null || mOptionalSavableIds != null
                || mRequiredSavableAutofillIds != null || mSaveInfoDecorator != null) {
            if (mRequiredSavableAutofillIds != null) {
                saveInfoBuilder = new SaveInfo.Builder(mSaveType, mRequiredSavableAutofillIds);
            } else {
                saveInfoBuilder = mRequiredSavableIds == null || mRequiredSavableIds.length == 0
                        ? new SaveInfo.Builder(mSaveType)
                            : new SaveInfo.Builder(mSaveType,
                                    getAutofillIds(autofillIdResolver, mRequiredSavableIds));
            }

            saveInfoBuilder.setFlags(mSaveInfoFlags);

            if (mOptionalSavableIds != null) {
                saveInfoBuilder.setOptionalIds(
                        getAutofillIds(autofillIdResolver, mOptionalSavableIds));
            }
            if (mSaveDescription != null) {
                saveInfoBuilder.setDescription(mSaveDescription);
            }
            if (mNegativeActionListener != null) {
                saveInfoBuilder.setNegativeAction(mNegativeActionStyle, mNegativeActionListener);
            }

            saveInfoBuilder.setPositiveAction(mPositiveActionStyle);

            if (mSaveTriggerId != null) {
                saveInfoBuilder.setTriggerId(mSaveTriggerId);
            }
        } else if (mSaveInfoFlags != 0) {
            saveInfoBuilder = new SaveInfo.Builder(mSaveType).setFlags(mSaveInfoFlags);
        } else {
            saveInfoBuilder = null;
        }
        if (saveInfoBuilder != null) {
            // TODO: merge decorator and visitor
            if (mSaveInfoDecorator != null) {
                mSaveInfoDecorator.decorate(saveInfoBuilder, autofillIdResolver);
            }
            if (mSaveInfoVisitor != null) {
                Log.d(TAG, "Visiting saveInfo " + saveInfoBuilder);
                mSaveInfoVisitor.visit(contexts, saveInfoBuilder);
            }
            final SaveInfo saveInfo = saveInfoBuilder.build();
            Log.d(TAG, "saveInfo:" + saveInfo);
            builder.setSaveInfo(saveInfo);
        }
        if (mIgnoredIds != null) {
            builder.setIgnoredIds(getAutofillIds(autofillIdResolver, mIgnoredIds));
        }
        if (mAuthenticationIds != null) {
            builder.setAuthentication(getAutofillIds(autofillIdResolver, mAuthenticationIds),
                    mAuthentication, mPresentation, mInlinePresentation);
        }
        if (mDisableDuration > 0) {
            builder.disableAutofill(mDisableDuration);
        }
        if (mFieldClassificationIdsOverflow) {
            final int length = UserData.getMaxFieldClassificationIdsSize() + 1;
            final AutofillId[] fieldIds = new AutofillId[length];
            for (int i = 0; i < length; i++) {
                fieldIds[i] = new AutofillId(i);
            }
            builder.setFieldClassificationIds(fieldIds);
        } else if (mFieldClassificationIds != null) {
            builder.setFieldClassificationIds(
                    getAutofillIds(autofillIdResolver, mFieldClassificationIds));
        }
        if (mExtras != null) {
            builder.setClientState(mExtras);
        }
        if (mHeader != null) {
            builder.setHeader(mHeader);
        }
        if (mFooter != null) {
            builder.setFooter(mFooter);
        }
        if (mUserData != null) {
            builder.setUserData(mUserData);
        }
        if (mVisitor != null) {
            Log.d(TAG, "Visiting " + builder);
            mVisitor.visit(contexts, builder);
        }
        builder.setPresentationCancelIds(mCancelIds);
        if (mDialogTriggerIds != null) {
            builder.setFillDialogTriggerIds(
                    getAutofillIds(autofillIdResolver, mDialogTriggerIds));
        }
        if (mDialogHeaderPresentation != null) {
            builder.setDialogHeader(mDialogHeaderPresentation);
        }

        builder.setIconResourceId(mIconResourceId);
        builder.setServiceDisplayNameResourceId(mServiceDisplayNameResourceId);
        builder.setShowFillDialogIcon(mShowFillDialogIcon);
        builder.setShowSaveDialogIcon(mShowSaveDialogIcon);

        final FillResponse response = builder.build();
        Log.v(TAG, "Response: " + response);
        return response;
    }

    @Override
    public String toString() {
        return "CannedFillResponse: [type=" + mResponseType
                + ",datasets=" + mDatasets
                + ", requiredSavableIds=" + Arrays.toString(mRequiredSavableIds)
                + ", optionalSavableIds=" + Arrays.toString(mOptionalSavableIds)
                + ", requiredSavableAutofillIds=" + Arrays.toString(mRequiredSavableAutofillIds)
                + ", saveInfoFlags=" + mSaveInfoFlags
                + ", fillResponseFlags=" + mFillResponseFlags
                + ", failureMessage=" + mFailureMessage
                + ", saveDescription=" + mSaveDescription
                + ", hasPresentation=" + (mPresentation != null)
                + ", hasInlinePresentation=" + (mInlinePresentation != null)
                + ", hasHeader=" + (mHeader != null)
                + ", hasFooter=" + (mFooter != null)
                + ", hasAuthentication=" + (mAuthentication != null)
                + ", authenticationIds=" + Arrays.toString(mAuthenticationIds)
                + ", ignoredIds=" + Arrays.toString(mIgnoredIds)
                + ", saveTriggerId=" + mSaveTriggerId
                + ", disableDuration=" + mDisableDuration
                + ", fieldClassificationIds=" + Arrays.toString(mFieldClassificationIds)
                + ", fieldClassificationIdsOverflow=" + mFieldClassificationIdsOverflow
                + ", saveInfoDecorator=" + mSaveInfoDecorator
                + ", userData=" + mUserData
                + ", visitor=" + mVisitor
                + ", saveInfoVisitor=" + mSaveInfoVisitor
                + "]";
    }

    public enum ResponseType {
        NORMAL,
        NULL,
        NO_MORE,
        TIMEOUT,
        FAILURE,
        DELAY
    }

    public static final class Builder {
        private final List<CannedDataset> mDatasets = new ArrayList<>();
        private final ResponseType mResponseType;
        private String mFailureMessage;
        private String[] mRequiredSavableIds;
        private String[] mOptionalSavableIds;
        private AutofillId[] mRequiredSavableAutofillIds;
        private CharSequence mSaveDescription;
        public int mSaveType = -1;
        private Bundle mExtras;
        private RemoteViews mPresentation;
        private InlinePresentation mInlinePresentation;
        private RemoteViews mFooter;
        private RemoteViews mHeader;
        private IntentSender mAuthentication;
        private String[] mAuthenticationIds;
        private String[] mIgnoredIds;
        private int mNegativeActionStyle;
        private IntentSender mNegativeActionListener;
        private int mPositiveActionStyle;
        private int mSaveInfoFlags;
        private int mFillResponseFlags;
        private AutofillId mSaveTriggerId;
        private long mDisableDuration;
        private String[] mFieldClassificationIds;
        private boolean mFieldClassificationIdsOverflow;
        private SaveInfoDecorator mSaveInfoDecorator;
        private UserData mUserData;
        private DoubleVisitor<List<FillContext>, FillResponse.Builder> mVisitor;
        private DoubleVisitor<List<FillContext>, SaveInfo.Builder> mSaveInfoVisitor;
        private int[] mCancelIds;
        private String[] mDialogTriggerIds;
        private RemoteViews mDialogHeaderPresentation;
        private int mIconResourceId;
        private int mServiceDisplayNameResourceId;
        private boolean mShowFillDialogIcon = true;
        private boolean mShowSaveDialogIcon = true;


        public Builder(ResponseType type) {
            mResponseType = type;
        }

        public Builder() {
            this(ResponseType.NORMAL);
        }

        public Builder addDataset(CannedDataset dataset) {
            assertWithMessage("already set failure").that(mFailureMessage).isNull();
            mDatasets.add(dataset);
            return this;
        }

        public Builder setIconResourceId(int id) {
            mIconResourceId = id;
            return this;
        }

        public Builder setServiceDisplayNameResourceId(int id) {
            mServiceDisplayNameResourceId = id;
            return this;
        }

        public Builder setShowFillDialogIcon(boolean show) {
            mShowFillDialogIcon = show;
            return this;
        }

        public Builder setShowSaveDialogIcon(boolean show) {
            mShowSaveDialogIcon = show;
            return this;
        }

        /**
         * Sets the required savable ids based on their {@code resourceId}.
         */
        public Builder setRequiredSavableIds(int type, String... ids) {
            mSaveType = type;
            mRequiredSavableIds = ids;
            return this;
        }

        /**
         * Sets the valid Save types, for when PCC Detection is enabled
         */
        public Builder setSaveTypes(int type) {
            mSaveType = type;
            return this;
        }

        public Builder setSaveInfoFlags(int flags) {
            mSaveInfoFlags = flags;
            return this;
        }

        public Builder setFillResponseFlags(int flags) {
            mFillResponseFlags = flags;
            return this;
        }

        /**
         * Sets the optional savable ids based on they {@code resourceId}.
         */
        public Builder setOptionalSavableIds(String... ids) {
            mOptionalSavableIds = ids;
            return this;
        }

        /**
         * Sets the description passed to the {@link SaveInfo}.
         */
        public Builder setSaveDescription(CharSequence description) {
            mSaveDescription = description;
            return this;
        }

        /**
         * Sets the extra passed to {@link
         * android.service.autofill.FillResponse.Builder#setClientState(Bundle)}.
         */
        public Builder setExtras(Bundle data) {
            mExtras = data;
            return this;
        }

        /**
         * Sets the view to present the response in the UI.
         */
        public Builder setPresentation(RemoteViews presentation) {
            mPresentation = presentation;
            return this;
        }

        /**
         * Sets the view to present the response in the UI.
         */
        public Builder setInlinePresentation(InlinePresentation inlinePresentation) {
            mInlinePresentation = inlinePresentation;
            return this;
        }

        /**
         * Sets views to present the response in the UI by the type.
         */
        public Builder setPresentation(String message, boolean inlineMode) {
            mPresentation = createPresentation(message);
            if (inlineMode) {
                mInlinePresentation = createInlinePresentation(message);
            }
            return this;
        }

        /**
         * Sets the authentication intent.
         */
        public Builder setAuthentication(IntentSender authentication, String... ids) {
            mAuthenticationIds = ids;
            mAuthentication = authentication;
            return this;
        }

        /**
         * Sets the ignored fields based on resource ids.
         */
        public Builder setIgnoreFields(String...ids) {
            mIgnoredIds = ids;
            return this;
        }

        /**
         * Sets the negative action spec.
         */
        public Builder setNegativeAction(int style, IntentSender listener) {
            mNegativeActionStyle = style;
            mNegativeActionListener = listener;
            return this;
        }

        /**
         * Sets the positive action spec.
         */
        public Builder setPositiveAction(int style) {
            mPositiveActionStyle = style;
            return this;
        }

        public CannedFillResponse build() {
            return new CannedFillResponse(this);
        }

        /**
         * Sets the response to call {@link FillCallback#onFailure(CharSequence)}.
         */
        public Builder returnFailure(String message) {
            assertWithMessage("already added datasets").that(mDatasets).isEmpty();
            mFailureMessage = message;
            return this;
        }

        /**
         * Sets the view that explicitly triggers save.
         */
        public Builder setSaveTriggerId(AutofillId id) {
            assertWithMessage("already set").that(mSaveTriggerId).isNull();
            mSaveTriggerId = id;
            return this;
        }

        public Builder disableAutofill(long duration) {
            assertWithMessage("already set").that(mDisableDuration).isEqualTo(0L);
            mDisableDuration = duration;
            return this;
        }

        /**
         * Sets the ids used for field classification.
         */
        public Builder setFieldClassificationIds(String... ids) {
            assertWithMessage("already set").that(mFieldClassificationIds).isNull();
            mFieldClassificationIds = ids;
            return this;
        }

        /**
         * Forces the service to throw an exception when setting the fields classification ids.
         */
        public Builder setFieldClassificationIdsOverflow() {
            mFieldClassificationIdsOverflow = true;
            return this;
        }

        public Builder setHeader(RemoteViews header) {
            assertWithMessage("already set").that(mHeader).isNull();
            mHeader = header;
            return this;
        }

        public Builder setFooter(RemoteViews footer) {
            assertWithMessage("already set").that(mFooter).isNull();
            mFooter = footer;
            return this;
        }

        public Builder setSaveInfoDecorator(SaveInfoDecorator decorator) {
            assertWithMessage("already set").that(mSaveInfoDecorator).isNull();
            mSaveInfoDecorator = decorator;
            return this;
        }

        /**
         * Sets the package-specific UserData.
         *
         * <p>Overrides the default UserData for field classification.
         */
        public Builder setUserData(UserData userData) {
            assertWithMessage("already set").that(mUserData).isNull();
            mUserData = userData;
            return this;
        }

        /**
         * Sets a generic visitor for the "real" request and response.
         *
         * <p>Typically used in cases where the test need to infer data from the request to build
         * the response.
         */
        public Builder setVisitor(
                @NonNull DoubleVisitor<List<FillContext>, FillResponse.Builder> visitor) {
            mVisitor = visitor;
            return this;
        }

        /**
         * Sets a generic visitor for the "real" request and save info.
         *
         * <p>Typically used in cases where the test need to infer data from the request to build
         * the response.
         */
        public Builder setSaveInfoVisitor(
                @NonNull DoubleVisitor<List<FillContext>, SaveInfo.Builder> visitor) {
            mSaveInfoVisitor = visitor;
            return this;
        }

        /**
         * Sets targets that cancel current session
         */
        public Builder setPresentationCancelIds(int[] ids) {
            mCancelIds = ids;
            return this;
        }

        /**
         * Sets the id of views which trigger the fill dialog.
         */
        public Builder setDialogTriggerIds(String... ids) {
            mDialogTriggerIds = ids;
            return this;
        }

        /**
         * Sets the header of the fill dialog.
         */
        public Builder setDialogHeader(RemoteViews header) {
            mDialogHeaderPresentation = header;
            return this;
        }
    }

    /**
     * Helper class used to produce a {@link Dataset} based on expected fields that should be
     * present in the {@link AssistStructure}.
     *
     * <p>Typical usage:
     *
     * <pre class="prettyprint">
     * InstrumentedAutoFillService.setFillResponse(new CannedFillResponse.Builder()
     *               .addDataset(new CannedDataset.Builder("dataset_name")
     *                   .setField("resource_id1", AutofillValue.forText("value1"))
     *                   .setField("resource_id2", AutofillValue.forText("value2"))
     *                   .build())
     *               .build());
     * </pre class="prettyprint">
     */
    public static class CannedDataset {
        private final Map<String, AutofillValue> mFieldValues;
        private final Map<String, RemoteViews> mFieldPresentations;
        private final Map<String, RemoteViews> mFieldDialogPresentations;
        private final Map<String, InlinePresentation> mFieldInlinePresentations;
        private final Map<String, InlinePresentation> mFieldInlineTooltipPresentations;
        private final Map<String, Pair<Boolean, Pattern>> mFieldFilters;
        private final RemoteViews mPresentation;
        private final RemoteViews mDialogPresentation;
        private final InlinePresentation mInlinePresentation;
        private final InlinePresentation mInlineTooltipPresentation;
        private final IntentSender mAuthentication;
        private final String mId;

        private CannedDataset(Builder builder) {
            mFieldValues = builder.mFieldValues;
            mFieldPresentations = builder.mFieldPresentations;
            mFieldDialogPresentations = builder.mFieldDialogPresentations;
            mFieldInlinePresentations = builder.mFieldInlinePresentations;
            mFieldInlineTooltipPresentations = builder.mFieldInlineTooltipPresentations;
            mFieldFilters = builder.mFieldFilters;
            mPresentation = builder.mPresentation;
            mDialogPresentation = builder.mDialogPresentation;
            mInlinePresentation = builder.mInlinePresentation;
            mInlineTooltipPresentation = builder.mInlineTooltipPresentation;
            mAuthentication = builder.mAuthentication;
            mId = builder.mId;
        }

        /**
         * Creates a new dataset, replacing the field ids by the real ids from the assist structure.
         */
        public Dataset asDatasetWithNodeResolver(Function<String, ViewNode> nodeResolver) {
            return asDatasetWithAutofillIdResolver((id) -> {
                ViewNode node = nodeResolver.apply(id);
                if (node == null) {
                    throw new AssertionError("No node with resource id " + id);
                }
                return node.getAutofillId();
            });
        }

        public Dataset asDatasetForPcc(Function<String, AutofillId> autofillIdResolver) {
            final Presentations.Builder presentationsBuilder = new Presentations.Builder();
            if (mPresentation != null) {
                presentationsBuilder.setMenuPresentation(mPresentation);
            }
            if (mDialogPresentation != null) {
                presentationsBuilder.setDialogPresentation(mDialogPresentation);
            }
            if (mInlinePresentation != null) {
                presentationsBuilder.setInlinePresentation(mInlinePresentation);
            }
            if (mInlineTooltipPresentation != null) {
                presentationsBuilder.setInlineTooltipPresentation(mInlineTooltipPresentation);
            }

            Presentations presentations = null;
            try {
                presentations = presentationsBuilder.build();
            } catch (IllegalStateException e) {
                // No presentation in presentationsBuilder, do nothing.
            }
            final Dataset.Builder builder = presentations != null
                    ? new Dataset.Builder(presentations)
                    : new Dataset.Builder();
            if (mFieldValues != null) {
                for (Map.Entry<String, AutofillValue> entry : mFieldValues.entrySet()) {
                    final Field.Builder fieldBuilder = new Field.Builder();
                    final AutofillValue value = entry.getValue();
                    final String id = entry.getKey();
                    if (value != null) {
                        fieldBuilder.setValue(value);
                    }
                    final Presentations.Builder fieldPresentationsBuilder =
                            new Presentations.Builder();
                    final RemoteViews presentation = mFieldPresentations.get(id);
                    if (presentation != null) {
                        fieldPresentationsBuilder.setMenuPresentation(presentation);
                    }
                    final RemoteViews dialogPresentation = mFieldDialogPresentations.get(id);
                    if (dialogPresentation != null) {
                        fieldPresentationsBuilder.setDialogPresentation(dialogPresentation);
                    }
                    final InlinePresentation inlinePresentation = mFieldInlinePresentations.get(id);
                    if (inlinePresentation != null) {
                        fieldPresentationsBuilder.setInlinePresentation(inlinePresentation);
                    }
                    final InlinePresentation tooltipPresentation =
                            mFieldInlineTooltipPresentations.get(id);
                    if (tooltipPresentation != null) {
                        fieldPresentationsBuilder.setInlineTooltipPresentation(tooltipPresentation);
                    }
                    try {
                        fieldBuilder.setPresentations(fieldPresentationsBuilder.build());
                    } catch (IllegalStateException e) {
                        // no presentation in fieldPresentationsBuilder, nothing
                    }
                    final Pair<Boolean, Pattern> filter = mFieldFilters.get(id);
                    if (filter != null) {
                        fieldBuilder.setFilter(filter.second);
                    }

                    final AutofillId autofillId = autofillIdResolver.apply(id);
                    if (autofillId == null) {
                        // Treat the id as autofill hints
                        builder.setField(id, fieldBuilder.build());
                    } else {
                        builder.setField(autofillId, fieldBuilder.build());
                    }
                }
            }
            builder.setId(mId).setAuthentication(mAuthentication);
            return builder.build();
        }

        /**
         * Creates a new dataset, replacing the field ids by the real ids from the assist structure.
         */
        public Dataset asDatasetWithAutofillIdResolver(
                Function<String, AutofillId> autofillIdResolver) {
            final Presentations.Builder presentationsBuilder = new Presentations.Builder();
            if (mPresentation != null) {
                presentationsBuilder.setMenuPresentation(mPresentation);
            }
            if (mDialogPresentation != null) {
                presentationsBuilder.setDialogPresentation(mDialogPresentation);
            }
            if (mInlinePresentation != null) {
                presentationsBuilder.setInlinePresentation(mInlinePresentation);
            }
            if (mInlineTooltipPresentation != null) {
                presentationsBuilder.setInlineTooltipPresentation(mInlineTooltipPresentation);
            }

            Presentations presentations = null;
            try {
                presentations = presentationsBuilder.build();
            } catch (IllegalStateException e) {
                // No presentation in presentationsBuilder, do nothing.
            }
            final Dataset.Builder builder = presentations != null
                    ? new Dataset.Builder(presentations)
                    : new Dataset.Builder();

            if (mFieldValues != null) {
                for (Map.Entry<String, AutofillValue> entry : mFieldValues.entrySet()) {
                    final String id = entry.getKey();

                    final AutofillId autofillId = autofillIdResolver.apply(id);
                    if (autofillId == null) {
                        throw new AssertionError("No node with resource id " + id);
                    }
                    final Field.Builder fieldBuilder = new Field.Builder();
                    final AutofillValue value = entry.getValue();
                    if (value != null) {
                        fieldBuilder.setValue(value);
                    }

                    final Presentations.Builder fieldPresentationsBuilder =
                            new Presentations.Builder();
                    final RemoteViews presentation = mFieldPresentations.get(id);
                    if (presentation != null) {
                        fieldPresentationsBuilder.setMenuPresentation(presentation);
                    }
                    final RemoteViews dialogPresentation = mFieldDialogPresentations.get(id);
                    if (dialogPresentation != null) {
                        fieldPresentationsBuilder.setDialogPresentation(dialogPresentation);
                    }
                    final InlinePresentation inlinePresentation = mFieldInlinePresentations.get(id);
                    if (inlinePresentation != null) {
                        fieldPresentationsBuilder.setInlinePresentation(inlinePresentation);
                    }
                    final InlinePresentation tooltipPresentation =
                            mFieldInlineTooltipPresentations.get(id);
                    if (tooltipPresentation != null) {
                        fieldPresentationsBuilder.setInlineTooltipPresentation(tooltipPresentation);
                    }
                    try {
                        fieldBuilder.setPresentations(fieldPresentationsBuilder.build());
                    } catch (IllegalStateException e) {
                        // no presentation in fieldPresentationsBuilder, nothing
                    }
                    final Pair<Boolean, Pattern> filter = mFieldFilters.get(id);
                    if (filter != null) {
                        fieldBuilder.setFilter(filter.second);
                    }
                    builder.setField(autofillId, fieldBuilder.build());
                }
            }
            builder.setId(mId).setAuthentication(mAuthentication);
            return builder.build();
        }

        @Override
        public String toString() {
            return "CannedDataset " + mId + " : [hasPresentation=" + (mPresentation != null)
                    + ", hasDialogPresentation=" + (mDialogPresentation != null)
                    + ", hasInlinePresentation=" + (mInlinePresentation != null)
                    + ", fieldPresentations=" + (mFieldPresentations)
                    + ", fieldDialogPresentations=" + (mFieldDialogPresentations)
                    + ", fieldInlinePresentations=" + (mFieldInlinePresentations)
                    + ", fieldTooltipInlinePresentations=" + (mFieldInlineTooltipPresentations)
                    + ", hasAuthentication=" + (mAuthentication != null)
                    + ", fieldValues=" + mFieldValues
                    + ", fieldFilters=" + mFieldFilters + "]";
        }

        public static class Builder {
            private final Map<String, AutofillValue> mFieldValues = new HashMap<>();
            private final Map<String, RemoteViews> mFieldPresentations = new HashMap<>();
            private final Map<String, RemoteViews> mFieldDialogPresentations = new HashMap<>();
            private final Map<String, InlinePresentation> mFieldInlinePresentations =
                    new HashMap<>();
            private final Map<String, InlinePresentation> mFieldInlineTooltipPresentations =
                    new HashMap<>();
            private final Map<String, Pair<Boolean, Pattern>> mFieldFilters = new HashMap<>();

            private RemoteViews mPresentation;
            private RemoteViews mDialogPresentation;
            private InlinePresentation mInlinePresentation;
            private IntentSender mAuthentication;
            private String mId;
            private InlinePresentation mInlineTooltipPresentation;

            public Builder() {

            }

            public Builder(RemoteViews presentation) {
                mPresentation = presentation;
            }

            /**
             * Sets the canned value of a text field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text) {
                return setField(id, AutofillValue.forText(text));
            }

            /**
             * Sets the canned value of a text field applicable for all hints.
             * This is applicable to PCC related datasets entry only.
             */
            public Builder setField(String text) {
                return setField(AutofillManager.ANY_HINT, AutofillValue.forText(text));
            }

            /**
             * Sets the canned value of a text field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, Pattern filter) {
                return setField(id, AutofillValue.forText(text), true, filter);
            }

            public Builder setUnfilterableField(String id, String text) {
                return setField(id, AutofillValue.forText(text), false, null);
            }

            /**
             * Sets the canned value of a list field based on its its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, int index) {
                return setField(id, AutofillValue.forList(index));
            }

            /**
             * Sets the canned value of a toggle field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, boolean toggled) {
                return setField(id, AutofillValue.forToggle(toggled));
            }

            /**
             * Sets the canned value of a date field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, long date) {
                return setField(id, AutofillValue.forDate(date));
            }

            /**
             * Sets the canned value of a date field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, AutofillValue value) {
                mFieldValues.put(id, value);
                return this;
            }

            /**
             * Sets the canned value of a date field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, AutofillValue value, boolean filterable,
                    Pattern filter) {
                setField(id, value);
                mFieldFilters.put(id, new Pair<>(filterable, filter));
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation) {
                setField(id, text);
                mFieldPresentations.put(id, presentation);
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation,
                    RemoteViews dialogPresentation) {
                setField(id, text, presentation);
                mFieldDialogPresentations.put(id, dialogPresentation);
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation,
                    Pattern filter) {
                setField(id, text, presentation);
                mFieldFilters.put(id, new Pair<>(true, filter));
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation,
                    InlinePresentation inlinePresentation) {
                setField(id, text);
                mFieldPresentations.put(id, presentation);
                mFieldInlinePresentations.put(id, inlinePresentation);
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation,
                    InlinePresentation inlinePresentation,
                    InlinePresentation inlineTooltipPresentation) {
                setField(id, text, presentation, inlinePresentation);
                mFieldInlineTooltipPresentations.put(id, inlineTooltipPresentation);
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation,
                    InlinePresentation inlinePresentation, Pattern filter) {
                setField(id, text, presentation, inlinePresentation);
                mFieldFilters.put(id, new Pair<>(true, filter));
                return this;
            }

            /**
             * Sets the canned value of a field based on its {@code id}.
             *
             * <p>The meaning of the id is defined by the object using the canned dataset.
             * For example, {@link InstrumentedAutoFillService.Replier} resolves the id based on
             * {@link IdMode}.
             */
            public Builder setField(String id, String text, RemoteViews presentation,
                    InlinePresentation inlinePresentation,
                    InlinePresentation inlineTooltipPresentation,
                    Pattern filter) {
                setField(id, text, presentation, inlinePresentation, inlineTooltipPresentation);
                mFieldFilters.put(id, new Pair<>(true, filter));

                return this;
            }

            /**
             * Sets the view to present the response in the UI.
             */
            public Builder setPresentation(RemoteViews presentation) {
                mPresentation = presentation;
                return this;
            }

            /**
             * Sets the view to present the response in the UI.
             */
            public Builder setInlinePresentation(InlinePresentation inlinePresentation) {
                mInlinePresentation = inlinePresentation;
                return this;
            }

            /**
             * Sets the inline tooltip to present the response in the UI.
             */
            public Builder setInlineTooltipPresentation(InlinePresentation tooltip) {
                mInlineTooltipPresentation = tooltip;
                return this;
            }

            public Builder setPresentation(String message, boolean inlineMode) {
                mPresentation = createPresentation(message);
                if (inlineMode) {
                    mInlinePresentation = createInlinePresentation(message);
                }
                return this;
            }

            /**
             * Sets the view to present the response in the UI.
             */
            public Builder setDialogPresentation(RemoteViews presentation) {
                mDialogPresentation = presentation;
                return this;
            }

            /**
             * Sets the authentication intent.
             */
            public Builder setAuthentication(IntentSender authentication) {
                mAuthentication = authentication;
                return this;
            }

            /**
             * Sets the name.
             */
            public Builder setId(String id) {
                mId = id;
                return this;
            }

            /**
             * Builds the canned dataset.
             */
            public CannedDataset build() {
                return new CannedDataset(this);
            }
        }
    }

    public interface SaveInfoDecorator {
        void decorate(SaveInfo.Builder builder, Function<String, AutofillId> nodeResolver);
    }
}
