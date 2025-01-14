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
package android.autofillservice.cts.commontests;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.app.PendingIntent;
import android.autofillservice.cts.R;
import android.autofillservice.cts.activities.AbstractAutoFillActivity;
import android.autofillservice.cts.testcore.Helper;
import android.autofillservice.cts.testcore.UiBot;
import android.content.Intent;
import android.service.autofill.CustomDescription;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.test.filters.FlakyTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;

import org.junit.Test;

/**
 * Template for tests cases that test what happens when a link in the {@link CustomDescription} is
 * tapped by the user.
 *
 * <p>It must be extend by 2 sub-class to provide tests for the 2 distinct scenarios:
 * <ul>
 *   <li>Save is triggered by 1st activity finishing and launching a 2nd activity.
 *   <li>Save is triggered by explicit {@link android.view.autofill.AutofillManager#commit()} call
 *       and shown in the same activity.
 * </ul>
 *
 * <p>The overall behavior should be the same in both cases, although the implementation of the
 * tests per se will be sligthly different.
 */
public abstract class CustomDescriptionWithLinkTestCase<A extends AbstractAutoFillActivity> extends
        AutoFillServiceTestCase.AutoActivityLaunch<A> {

    private static final String ID_LINK = "link";

    private final Class<A> mActivityClass;

    protected A mActivity;

    protected CustomDescriptionWithLinkTestCase(@NonNull Class<A> activityClass) {
        mActivityClass = activityClass;
    }

    protected void startActivity() {
        startActivity(false);
    }

    protected void startActivity(boolean remainOnRecents) {
        final Intent intent = new Intent(mContext, mActivityClass);
        if (remainOnRecents) {
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mActivity = launchActivity(intent);
    }

    /**
     * Tests scenarios when user taps a link in the custom description and then taps back:
     * the Save UI should have been restored.
     */
    @Test
    public final void testTapLink_tapBack() throws Exception {
        saveUiRestoredAfterTappingLinkTest(PostSaveLinkTappedAction.TAP_BACK_BUTTON);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, change the screen
     * orientation while the new activity is show, then taps back:
     * the Save UI should have been restored.
     */
    @Test
    public final void testTapLink_changeOrientationThenTapBack() throws Exception {
        assumeTrue("Rotation is supported", Helper.isRotationSupported(mContext));
        assumeTrue("Device state is not REAR_DISPLAY",
                !Helper.isDeviceInState(mContext, Helper.DeviceStateEnum.REAR_DISPLAY));

        mUiBot.assumeMinimumResolution(500);
        mUiBot.setScreenOrientation(UiBot.PORTRAIT);
        try {
            saveUiRestoredAfterTappingLinkTest(
                    PostSaveLinkTappedAction.ROTATE_THEN_TAP_BACK_BUTTON);
        } finally {
            try {
                mUiBot.setScreenOrientation(UiBot.PORTRAIT);
                cleanUpAfterScreenOrientationIsBackToPortrait();
            } catch (Exception e) {
                mSafeCleanerRule.add(e);
            } finally {
                mUiBot.resetScreenResolution();
            }
        }
    }

    /**
     * Tests scenarios when user taps a link in the custom description, then the new activity
     * finishes:
     * the Save UI should have been restored.
     */
    @Test
    public final void testTapLink_finishActivity() throws Exception {
        saveUiRestoredAfterTappingLinkTest(PostSaveLinkTappedAction.FINISH_ACTIVITY);
    }

    protected abstract void saveUiRestoredAfterTappingLinkTest(PostSaveLinkTappedAction type)
            throws Exception;

    protected void cleanUpAfterScreenOrientationIsBackToPortrait() throws Exception {
    }

    /**
     * Tests scenarios when user taps a link in the custom description, taps back to return to the
     * activity with the Save UI, and touch outside the Save UI to dismiss it.
     *
     * <p>Then user starts a new session by focusing in a field.
     */
    @Test
    public final void testTapLink_tapBack_thenStartOverByTouchOutsideAndFocus()
            throws Exception {
        tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction.TOUCH_OUTSIDE, false);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, taps back to return to the
     * activity with the Save UI, and touch outside the Save UI to dismiss it.
     *
     * <p>Then user starts a new session by forcing autofill.
     */
    @Test
    public void testTapLink_tapBack_thenStartOverByTouchOutsideAndManualRequest()
            throws Exception {
        tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction.TOUCH_OUTSIDE, true);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, taps back to return to the
     * activity with the Save UI, and tap the "No" button to dismiss it.
     *
     * <p>Then user starts a new session by focusing in a field.
     */
    @Test
    public final void testTapLink_tapBack_thenStartOverBySayingNoAndFocus()
            throws Exception {
        tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction.TAP_NO_ON_SAVE_UI,
                false);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, taps back to return to the
     * activity with the Save UI, and tap the "No" button to dismiss it.
     *
     * <p>Then user starts a new session by forcing autofill.
     */
    @Test
    public final void testTapLink_tapBack_thenStartOverBySayingNoAndManualRequest()
            throws Exception {
        tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction.TAP_NO_ON_SAVE_UI, true);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, taps back to return to the
     * activity with the Save UI, and the "Yes" button to save it.
     *
     * <p>Then user starts a new session by focusing in a field.
     */
    @Test
    public final void testTapLink_tapBack_thenStartOverBySayingYesAndFocus()
            throws Exception {
        tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction.TAP_YES_ON_SAVE_UI,
                false);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, taps back to return to the
     * activity with the Save UI, and the "Yes" button to save it.
     *
     * <p>Then user starts a new session by forcing autofill.
     */
    @Test
    public final void testTapLink_tapBack_thenStartOverBySayingYesAndManualRequest()
            throws Exception {
        tapLinkThenTapBackThenStartOverTest(PostSaveLinkTappedAction.TAP_YES_ON_SAVE_UI, true);
    }

    protected abstract void tapLinkThenTapBackThenStartOverTest(
            PostSaveLinkTappedAction action, boolean manualRequest) throws Exception;

    /**
     * Tests scenarios when user taps a link in the custom description, then re-launches the
     * original activity:
     * the Save UI should have been canceled.
     */
    @Test
    public final void testTapLink_backToPreviousActivityByLaunchingIt()
            throws Exception {
        saveUiCancelledAfterTappingLinkTest(PostSaveLinkTappedAction.LAUNCH_PREVIOUS_ACTIVITY);
    }

    /**
     * Tests scenarios when user taps a link in the custom description, then launches a 3rd
     * activity:
     * the Save UI should have been canceled.
     */
    @Test
    public final void testTapLink_launchNewActivityThenTapBack() throws Exception {
        saveUiCancelledAfterTappingLinkTest(PostSaveLinkTappedAction.LAUNCH_NEW_ACTIVITY);
    }

    protected abstract void saveUiCancelledAfterTappingLinkTest(PostSaveLinkTappedAction type)
            throws Exception;

    @Test
    @FlakyTest(bugId = 177259617)
    public final void testTapLink_launchTrampolineActivityThenTapBackAndStartNewSession()
            throws Exception {
        // Reset AutofillOptions to avoid cts package was added to augmented autofill allowlist.
        Helper.resetApplicationAutofillOptions(sContext);

        tapLinkLaunchTrampolineActivityThenTapBackAndStartNewSessionTest();

        // Clear AutofillOptions.
        Helper.clearApplicationAutofillOptions(sContext);
    }

    protected abstract void tapLinkLaunchTrampolineActivityThenTapBackAndStartNewSessionTest()
            throws Exception;

    @Test
    public final void testTapLinkAfterUpdateAppliedToLinkView() throws Exception {
        tapLinkAfterUpdateAppliedTest(true);
    }

    @Test
    public final void testTapLinkAfterUpdateAppliedToAnotherView() throws Exception {
        tapLinkAfterUpdateAppliedTest(false);
    }

    protected abstract void tapLinkAfterUpdateAppliedTest(boolean updateLinkView) throws Exception;

    public enum PostSaveLinkTappedAction {
        TAP_BACK_BUTTON,
        ROTATE_THEN_TAP_BACK_BUTTON,
        FINISH_ACTIVITY,
        LAUNCH_NEW_ACTIVITY,
        LAUNCH_PREVIOUS_ACTIVITY,
        TOUCH_OUTSIDE,
        TAP_NO_ON_SAVE_UI,
        TAP_YES_ON_SAVE_UI
    }

    protected final void startActivityOnNewTask(Class<?> clazz) {
        final Intent intent = new Intent(mContext, clazz);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    protected RemoteViews newTemplate() {
        final RemoteViews presentation = new RemoteViews(mPackageName,
                R.layout.custom_description_with_link);
        return presentation;
    }

    protected final CustomDescription.Builder newCustomDescriptionBuilder(
            Class<? extends Activity> activityClass) {
        final Intent intent = new Intent(mContext, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return newCustomDescriptionBuilder(intent);
    }

    protected final CustomDescription newCustomDescription(
            Class<? extends Activity> activityClass) {
        return newCustomDescriptionBuilder(activityClass).build();
    }

    protected final CustomDescription.Builder newCustomDescriptionBuilder(Intent intent) {
        final RemoteViews presentation = newTemplate();
        final PendingIntent pendingIntent =
                PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_MUTABLE);
        presentation.setOnClickPendingIntent(R.id.link, pendingIntent);
        return new CustomDescription.Builder(presentation);
    }

    protected final CustomDescription newCustomDescription(Intent intent) {
        return newCustomDescriptionBuilder(intent).build();
    }

    protected final UiObject2 assertSaveUiWithLinkIsShown(int saveType) throws Exception {
        return assertSaveUiWithLinkIsShown(saveType, "DON'T TAP ME!");
    }

    protected final UiObject2 assertSaveUiWithLinkIsShown(int saveType, String expectedText)
            throws Exception {
        // First make sure the UI is shown...
        final UiObject2 saveUi = mUiBot.assertSaveShowing(saveType);
        // Then make sure it does have the custom view with link on it...
        final UiObject2 link = getLink(saveUi);
        assertThat(link.getText()).isEqualTo(expectedText);
        return saveUi;
    }

    protected final UiObject2 getLink(final UiObject2 container) {
        final UiObject2 link = container.findObject(By.res(mPackageName, ID_LINK));
        assertThat(link).isNotNull();
        return link;
    }

    protected final void tapSaveUiLink(UiObject2 saveUi) {
        getLink(saveUi).click();
    }
}
