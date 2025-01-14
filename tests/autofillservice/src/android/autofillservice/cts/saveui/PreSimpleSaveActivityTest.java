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
package android.autofillservice.cts.saveui;

import static android.autofillservice.cts.activities.LoginActivity.ID_USERNAME_CONTAINER;
import static android.autofillservice.cts.activities.PreSimpleSaveActivity.ID_PRE_INPUT;
import static android.autofillservice.cts.activities.SimpleSaveActivity.ID_INPUT;
import static android.autofillservice.cts.testcore.Helper.ID_STATIC_TEXT;
import static android.autofillservice.cts.testcore.Helper.assertActivityShownInBackground;
import static android.autofillservice.cts.testcore.Helper.assertTextAndValue;
import static android.autofillservice.cts.testcore.Helper.findAutofillIdByResourceId;
import static android.autofillservice.cts.testcore.Helper.findNodeByResourceId;
import static android.service.autofill.SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS;
import static android.service.autofill.SaveInfo.SAVE_DATA_TYPE_PASSWORD;

import static com.google.common.truth.Truth.assertThat;

import android.autofillservice.cts.R;
import android.autofillservice.cts.activities.LoginActivity;
import android.autofillservice.cts.activities.PreSimpleSaveActivity;
import android.autofillservice.cts.activities.SimpleSaveActivity;
import android.autofillservice.cts.activities.TrampolineWelcomeActivity;
import android.autofillservice.cts.activities.WelcomeActivity;
import android.autofillservice.cts.commontests.CustomDescriptionWithLinkTestCase;
import android.autofillservice.cts.testcore.AutofillActivityTestRule;
import android.autofillservice.cts.testcore.CannedFillResponse;
import android.autofillservice.cts.testcore.InstrumentedAutoFillService.SaveRequest;
import android.autofillservice.cts.testcore.UiBot;
import android.service.autofill.BatchUpdates;
import android.service.autofill.CustomDescription;
import android.service.autofill.RegexValidator;
import android.service.autofill.Validator;
import android.view.View;
import android.view.autofill.AutofillId;
import android.widget.RemoteViews;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;

import java.util.regex.Pattern;

public class PreSimpleSaveActivityTest
        extends CustomDescriptionWithLinkTestCase<PreSimpleSaveActivity> {

    private static final AutofillActivityTestRule<PreSimpleSaveActivity> sActivityRule =
            new AutofillActivityTestRule<PreSimpleSaveActivity>(PreSimpleSaveActivity.class, false);

    public PreSimpleSaveActivityTest() {
        super(PreSimpleSaveActivity.class);
    }

    @Override
    protected AutofillActivityTestRule<PreSimpleSaveActivity> getActivityRule() {
        return sActivityRule;
    }

    @Override
    protected void saveUiRestoredAfterTappingLinkTest(PostSaveLinkTappedAction type)
            throws Exception {
        startActivity(/* remainOnRecents= */ false);
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_PRE_INPUT)
                .setSaveInfoVisitor((contexts, builder) -> builder
                        .setCustomDescription(newCustomDescription(WelcomeActivity.class)))
                .build());

        // Trigger autofill.
        mActivity.syncRunOnUiThread(() -> mActivity.mPreInput.requestFocus());
        sReplier.getNextFillRequest();

        // Trigger save.
        mActivity.setTextAndWaitTextChange("108");
        mActivity.syncRunOnUiThread(() -> mActivity.mSubmit.performClick());

        // Make sure post-save activity is shown...
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Tap the link.
        final UiObject2 saveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD);
        tapSaveUiLink(saveUi);

        // Make sure new activity is shown...
        WelcomeActivity.assertShowingDefaultMessage(mUiBot);
        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);

        // .. then do something to return to previous activity...
        switch (type) {
            case ROTATE_THEN_TAP_BACK_BUTTON:
                mUiBot.setScreenOrientation(UiBot.LANDSCAPE);
                WelcomeActivity.assertShowingDefaultMessage(mUiBot);
                // not breaking on purpose
            case TAP_BACK_BUTTON:
                mUiBot.pressBack();
                break;
            case FINISH_ACTIVITY:
                // ..then finishes it.
                WelcomeActivity.finishIt();
                break;
            default:
                throw new IllegalArgumentException("invalid type: " + type);
        }

        // ... and tap save.
        final UiObject2 newSaveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD);
        mUiBot.saveForAutofill(newSaveUi, true);

        final SaveRequest saveRequest = sReplier.getNextSaveRequest();
        assertTextAndValue(findNodeByResourceId(saveRequest.structure, ID_PRE_INPUT), "108");
    }

    @Override
    protected void tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction action,
            boolean manualRequest) throws Exception {
        startActivity(/* remainOnRecents= */ false);
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_PRE_INPUT)
                .setSaveInfoVisitor((contexts, builder) -> builder
                        .setCustomDescription(newCustomDescription(WelcomeActivity.class)))
                .build());

        // Trigger autofill.
        mActivity.syncRunOnUiThread(() -> mActivity.mPreInput.requestFocus());
        sReplier.getNextFillRequest();

        // Trigger save.
        mActivity.setTextAndWaitTextChange("108");
        mActivity.syncRunOnUiThread(() -> mActivity.mSubmit.performClick());

        // Make sure post-save activity is shown...
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Tap the link.
        final UiObject2 saveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD);
        tapSaveUiLink(saveUi);

        // Make sure new activity is shown...
        WelcomeActivity.assertShowingDefaultMessage(mUiBot);
        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);

        // Tap back to restore the Save UI...
        mUiBot.pressBack();

        // ...but don't tap it...
        final UiObject2 saveUi2 = mUiBot.assertSaveShowing(SAVE_DATA_TYPE_PASSWORD);

        // ...instead, do something to dismiss it:
        switch (action) {
            case TOUCH_OUTSIDE:
                mUiBot.touchOutsideSaveDialog();
                break;
            case TAP_NO_ON_SAVE_UI:
                mUiBot.saveForAutofill(saveUi2, false);
                break;
            case TAP_YES_ON_SAVE_UI:
                mUiBot.saveForAutofill(true, SAVE_DATA_TYPE_PASSWORD);

                final SaveRequest saveRequest = sReplier.getNextSaveRequest();
                assertTextAndValue(findNodeByResourceId(saveRequest.structure, ID_PRE_INPUT),
                        "108");
                break;
            default:
                throw new IllegalArgumentException("invalid action: " + action);
        }
        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);

        // Make sure previous session was finished.

        // Now triggers a new session in the new activity (SaveActivity) and do business as usual...
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_EMAIL_ADDRESS, ID_INPUT)
                .build());

        // Trigger autofill.
        final SimpleSaveActivity newActivty = SimpleSaveActivity.getInstance();
        if (manualRequest) {
            newActivty.getAutofillManager().requestAutofill(newActivty.mInput);
        } else {
            newActivty.syncRunOnUiThread(() -> newActivty.mPassword.requestFocus());
        }

        sReplier.getNextFillRequest();

        // Trigger save.
        newActivty.setTextAndWaitTextChange(/* input= */ "42", /* password= */  null);
        newActivty.syncRunOnUiThread(() -> newActivty.mCommit.performClick());

        // Make sure post-save activity is shown...
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Save it...
        mUiBot.saveForAutofill(true, SAVE_DATA_TYPE_EMAIL_ADDRESS);

        // ... and assert results
        final SaveRequest saveRequest = sReplier.getNextSaveRequest();
        assertTextAndValue(findNodeByResourceId(saveRequest.structure, ID_INPUT), "42");
    }

    @Override
    protected void saveUiCancelledAfterTappingLinkTest(PostSaveLinkTappedAction type)
            throws Exception {
        startActivity(/* remainOnRecents= */ false);
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_PRE_INPUT)
                .setSaveInfoVisitor((contexts, builder) -> builder
                        .setCustomDescription(newCustomDescription(WelcomeActivity.class)))
                .build());

        // Trigger autofill.
        mActivity.syncRunOnUiThread(() -> mActivity.mPreInput.requestFocus());
        sReplier.getNextFillRequest();

        // Trigger save.
        mActivity.setTextAndWaitTextChange("108");
        mActivity.syncRunOnUiThread(() -> mActivity.mSubmit.performClick());

        // Make sure post-save activity is shown...
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Tap the link.
        final UiObject2 saveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD);
        tapSaveUiLink(saveUi);

        // Make sure linked activity is shown...
        WelcomeActivity.assertShowingDefaultMessage(mUiBot);
        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);

        switch (type) {
            case LAUNCH_PREVIOUS_ACTIVITY:
                startActivityOnNewTask(PreSimpleSaveActivity.class);
                mUiBot.assertShownByRelativeId(ID_INPUT);
                break;
            case LAUNCH_NEW_ACTIVITY:
                // Launch a 3rd activity...
                startActivityOnNewTask(LoginActivity.class);
                mUiBot.assertShownByRelativeId(ID_USERNAME_CONTAINER);
                // ...then go back
                mUiBot.pressBack();
                mUiBot.assertShownByRelativeId(ID_INPUT);
                break;
            default:
                throw new IllegalArgumentException("invalid type: " + type);
        }

        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);
    }

    @Override
    protected void tapLinkLaunchTrampolineActivityThenTapBackAndStartNewSessionTest()
            throws Exception {
        // Prepare activity.
        startActivity(/* remainOnRecents= */ false);
        mActivity.mPreInput.getRootView()
                .setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);

        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_PRE_INPUT)
                .setSaveInfoVisitor((contexts, builder) -> builder.setCustomDescription(
                        newCustomDescription(TrampolineWelcomeActivity.class)))
                .build());

        // Trigger autofill.
        mActivity.getAutofillManager().requestAutofill(mActivity.mPreInput);
        sReplier.getNextFillRequest();

        // Trigger save.
        mActivity.setTextAndWaitTextChange("108");
        mActivity.syncRunOnUiThread(() -> mActivity.mSubmit.performClick());

        final UiObject2 saveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD);

        // Tap the link.
        tapSaveUiLink(saveUi);

        // Make sure new activity is shown...
        assertActivityShownInBackground(WelcomeActivity.class);

        // Save UI should be showing as well, since Trampoline finished.
        mUiBot.assertSaveShowing(SAVE_DATA_TYPE_PASSWORD);

        // Go back and make sure it's showing the right activity.
        // first BACK cancels save dialog
        mUiBot.pressBack();
        // second BACK cancel WelcomeActivity
        mUiBot.pressBack();
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Now triggers a new session in the new activity (SaveActivity) and do business as usual...
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_EMAIL_ADDRESS, ID_INPUT)
                .build());

        // Trigger autofill.
        final SimpleSaveActivity newActivty = SimpleSaveActivity.getInstance();
        newActivty.getAutofillManager().requestAutofill(newActivty.mInput);

        sReplier.getNextFillRequest();

        // Trigger save.
        newActivty.setTextAndWaitTextChange(/* input= */ "42", /* password= */  null);
        newActivty.syncRunOnUiThread(() -> newActivty.mCommit.performClick());
        // Make sure post-save activity is shown...
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Save it...
        mUiBot.saveForAutofill(true, SAVE_DATA_TYPE_EMAIL_ADDRESS);

        // ... and assert results
        final SaveRequest saveRequest1 = sReplier.getNextSaveRequest();
        assertTextAndValue(findNodeByResourceId(saveRequest1.structure, ID_INPUT), "42");
    }

    @Override
    protected void tapLinkAfterUpdateAppliedTest(boolean updateLinkView) throws Exception {
        startActivity(/* remainOnRecents= */ false);
        // Set service.
        enableService();

        // Set expectations.
        sReplier.addResponse(new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_PASSWORD, ID_PRE_INPUT)
                .setSaveInfoVisitor((contexts, builder) -> {
                    final CustomDescription.Builder customDescription =
                            newCustomDescriptionBuilder(WelcomeActivity.class);
                    final RemoteViews update = newTemplate();
                    if (updateLinkView) {
                        update.setCharSequence(R.id.link, "setText", "TAP ME IF YOU CAN");
                    } else {
                        update.setCharSequence(R.id.static_text, "setText", "ME!");
                    }
                    final AutofillId id = findAutofillIdByResourceId(contexts.get(0), ID_PRE_INPUT);
                    final Validator validCondition = new RegexValidator(id, Pattern.compile(".*"));
                    customDescription.batchUpdate(validCondition,
                            new BatchUpdates.Builder().updateTemplate(update).build());
                    builder.setCustomDescription(customDescription.build());
                })
                .build());

        // Trigger autofill.
        mActivity.syncRunOnUiThread(() -> mActivity.mPreInput.requestFocus());
        sReplier.getNextFillRequest();

        // Trigger save.
        mActivity.setTextAndWaitTextChange("108");
        mActivity.syncRunOnUiThread(() -> mActivity.mSubmit.performClick());

        // Make sure post-save activity is shown...
        assertActivityShownInBackground(SimpleSaveActivity.class);

        // Tap the link.
        final UiObject2 saveUi;
        if (updateLinkView) {
            saveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD, "TAP ME IF YOU CAN");
        } else {
            saveUi = assertSaveUiWithLinkIsShown(SAVE_DATA_TYPE_PASSWORD);
            final UiObject2 changed = saveUi.findObject(By.res(mPackageName, ID_STATIC_TEXT));
            assertThat(changed.getText()).isEqualTo("ME!");
        }
        tapSaveUiLink(saveUi);

        // Make sure new activity is shown...
        WelcomeActivity.assertShowingDefaultMessage(mUiBot);
        mUiBot.assertSaveNotShowing(SAVE_DATA_TYPE_PASSWORD);
    }
}
