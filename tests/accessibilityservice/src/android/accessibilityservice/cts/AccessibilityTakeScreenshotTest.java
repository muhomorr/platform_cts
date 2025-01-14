/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.accessibilityservice.cts;

import static android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT;
import static android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT;
import static android.accessibilityservice.cts.utils.AccessibilityEventFilterUtils.filterWindowsChangedWithChangeTypes;
import static android.accessibilityservice.cts.utils.ActivityLaunchUtils.launchActivityOnSpecifiedDisplayAndWaitForItToBeOnscreen;
import static android.accessibilityservice.cts.utils.ActivityLaunchUtils.supportsMultiDisplay;
import static android.accessibilityservice.cts.utils.AsyncUtils.DEFAULT_TIMEOUT_MS;
import static android.accessibilityservice.cts.utils.DisplayUtils.VirtualDisplaySession;
import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.accessibility.cts.common.AccessibilityDumpOnFailureRule;
import android.accessibility.cts.common.InstrumentedAccessibilityService;
import android.accessibility.cts.common.InstrumentedAccessibilityServiceTestRule;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.accessibilityservice.cts.activities.AccessibilityWindowQueryActivity;
import android.accessibilityservice.cts.utils.ActivityLaunchUtils;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.hardware.HardwareBuffer;
import android.os.SystemClock;
import android.platform.test.annotations.Presubmit;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.ImageView;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.ApiTest;
import com.android.compatibility.common.util.CddTest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test cases for accessibility service takeScreenshot API.
 */
@RunWith(AndroidJUnit4.class)
@CddTest(requirements = {"3.10/C-1-1,C-1-2"})
@Presubmit
public class AccessibilityTakeScreenshotTest {
    /**
     * The timeout for waiting screenshot had been taken done.
     */
    private static final long TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS = 1000;
    public static final int SECURE_WINDOW_CONTENT_COLOR = Color.BLUE;

    private static Instrumentation sInstrumentation;
    private static UiAutomation sUiAutomation;

    private InstrumentedAccessibilityServiceTestRule<StubTakeScreenshotService> mServiceRule =
            new InstrumentedAccessibilityServiceTestRule<>(StubTakeScreenshotService.class);

    private AccessibilityDumpOnFailureRule mDumpOnFailureRule =
            new AccessibilityDumpOnFailureRule();

    @Rule
    public final RuleChain mRuleChain = RuleChain
            .outerRule(mServiceRule)
            .around(mDumpOnFailureRule);

    private StubTakeScreenshotService mService;
    private Context mContext;
    private Point mDisplaySize;
    private long mStartTestingTime;
    @Mock
    private TakeScreenshotCallback mCallback;
    @Captor
    private ArgumentCaptor<ScreenshotResult> mSuccessResultArgumentCaptor;

    @BeforeClass
    public static void oneTimeSetup() {
        sInstrumentation = InstrumentationRegistry.getInstrumentation();
        sUiAutomation = sInstrumentation.getUiAutomation();
    }

    @AfterClass
    public static void finalTearDown() {
        sUiAutomation.destroy();
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mService = mServiceRule.getService();
        mContext = mService.getApplicationContext();

        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();

        mDisplaySize = new Point();
        display.getRealSize(mDisplaySize);
    }

    @Test
    public void testTakeScreenshot_GetScreenshotResult() {
        takeScreenshot(Display.DEFAULT_DISPLAY);
        verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                mSuccessResultArgumentCaptor.capture());

        verifyScreenshotResult(mSuccessResultArgumentCaptor.getValue());
    }

    @Test
    public void testTakeScreenshot_RequestIntervalTime() throws Exception {
        takeScreenshot(Display.DEFAULT_DISPLAY);
        verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                mSuccessResultArgumentCaptor.capture());

        Thread.sleep(
                AccessibilityService.ACCESSIBILITY_TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES_MS / 2);
        // Requests the API again during interval time from calling the first time.
        takeScreenshot(Display.DEFAULT_DISPLAY);
        verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onFailure(
                AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT);

        Thread.sleep(
                AccessibilityService.ACCESSIBILITY_TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES_MS / 2 +
                        1);
        // Requests the API again after interval time from calling the first time.
        takeScreenshot(Display.DEFAULT_DISPLAY);
        verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                mSuccessResultArgumentCaptor.capture());
    }

    @Test
    public void testTakeScreenshotOnVirtualDisplay_GetScreenshotResult() throws Exception {
        assumeTrue(supportsMultiDisplay(sInstrumentation.getContext()));
        try (VirtualDisplaySession displaySession = new VirtualDisplaySession()) {
            final int virtualDisplayId =
                    displaySession.createDisplayWithDefaultDisplayMetricsAndWait(mContext,
                            false).getDisplayId();
            // Launches an activity on virtual display.
            final Activity activity = launchActivityOnSpecifiedDisplayAndWaitForItToBeOnscreen(
                    sInstrumentation, sUiAutomation, AccessibilityWindowQueryActivity.class,
                    virtualDisplayId);
            try {
                takeScreenshot(virtualDisplayId);
                verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                        mSuccessResultArgumentCaptor.capture());

                verifyScreenshotResult(mSuccessResultArgumentCaptor.getValue());
            } finally {
                sInstrumentation.runOnMainSync(() -> {
                    activity.finish();
                });
            }
        }
    }

    @Test
    public void testTakeScreenshotOnPrivateDisplay_GetErrorCode() {
        try (VirtualDisplaySession displaySession = new VirtualDisplaySession()) {
            final int virtualDisplayId =
                    displaySession.createDisplayWithDefaultDisplayMetricsAndWait(mContext,
                            true).getDisplayId();
            takeScreenshot(virtualDisplayId);
            verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onFailure(
                    AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY);
        }
    }

    @Test
    public void testTakeScreenshotWithSecureWindow_GetScreenshotAndVerifyBitmap() throws Throwable {
        final Activity activity = launchActivityOnSpecifiedDisplayAndWaitForItToBeOnscreen(
                sInstrumentation, sUiAutomation, AccessibilityWindowQueryActivity.class,
                Display.DEFAULT_DISPLAY);

        final ImageView image = new ImageView(activity);
        image.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        image.setImageDrawable(new ColorDrawable(SECURE_WINDOW_CONTENT_COLOR));

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SECURE;

        sUiAutomation.executeAndWaitForEvent(() -> sInstrumentation.runOnMainSync(
                () -> {
                    activity.getWindowManager().addView(image, params);
                }),
                filterWindowsChangedWithChangeTypes(WINDOWS_CHANGE_ADDED),
                DEFAULT_TIMEOUT_MS);
        takeScreenshot(Display.DEFAULT_DISPLAY);

        verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                mSuccessResultArgumentCaptor.capture());

        assertThat(doesScreenshotContainColor(mSuccessResultArgumentCaptor.getValue(),
                SECURE_WINDOW_CONTENT_COLOR)).isFalse();
    }

    @Test
    @ApiTest(apis = {"android.accessibilityservice.AccessibilityService#takeScreenshotOfWindow"})
    public void testTakeScreenshotOfWindow_GetScreenshotResult() throws Throwable {
        final Activity activity = launchActivityOnSpecifiedDisplayAndWaitForItToBeOnscreen(
                sInstrumentation, sUiAutomation, AccessibilityWindowQueryActivity.class,
                Display.DEFAULT_DISPLAY);
        try {
            final AccessibilityWindowInfo activityWindowInfo =
                    ActivityLaunchUtils.findWindowByTitle(sUiAutomation, activity.getTitle());
            assertThat(activityWindowInfo).isNotNull();

            final long timestampBeforeTakeScreenshot = SystemClock.uptimeMillis();
            mService.takeScreenshotOfWindow(activityWindowInfo.getId(),
                    mContext.getMainExecutor(), mCallback);
            verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                    mSuccessResultArgumentCaptor.capture());

            final View activityRootView = activity.getWindow().getDecorView();
            verifyScreenshotResult(mSuccessResultArgumentCaptor.getValue(),
                    activityRootView.getWidth(), activityRootView.getHeight(),
                    timestampBeforeTakeScreenshot);
        } finally {
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @Test
    @ApiTest(apis = {"android.accessibilityservice.AccessibilityService#takeScreenshotOfWindow"})
    public void testTakeScreenshotOfWindow_ErrorForSecureWindow() throws Throwable {
        final Activity activity = launchActivityOnSpecifiedDisplayAndWaitForItToBeOnscreen(
                sInstrumentation, sUiAutomation, AccessibilityWindowQueryActivity.class,
                Display.DEFAULT_DISPLAY);
        try {
            final ImageView image = new ImageView(activity);
            image.setImageDrawable(new ColorDrawable(SECURE_WINDOW_CONTENT_COLOR));
            final String secureWindowTitle = "Secure Window";
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.flags = WindowManager.LayoutParams.FLAG_SECURE;
            params.accessibilityTitle = secureWindowTitle;
            sUiAutomation.executeAndWaitForEvent(
                    () -> sInstrumentation.runOnMainSync(
                            () -> activity.getWindowManager().addView(image, params)),
                    filterWindowsChangedWithChangeTypes(WINDOWS_CHANGE_ADDED),
                    DEFAULT_TIMEOUT_MS);

            final AccessibilityWindowInfo secureWindowInfo =
                    ActivityLaunchUtils.findWindowByTitle(sUiAutomation, secureWindowTitle);
            assertThat(secureWindowTitle).isNotNull();

            mService.takeScreenshotOfWindow(secureWindowInfo.getId(),
                    mContext.getMainExecutor(), mCallback);
            verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onFailure(
                    AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW);
        } finally {
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @Test
    @ApiTest(apis = {"android.accessibilityservice.AccessibilityService#takeScreenshotOfWindow"})
    public void testTakeScreenshotOfWindow_MultipleWindowsIntervalTime() throws Throwable {
        final long halfInterval =
                AccessibilityService.ACCESSIBILITY_TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES_MS / 2;
        final List<Integer> accessibilityWindowIds = sUiAutomation.getWindows()
                .stream().map(AccessibilityWindowInfo::getId).collect(Collectors.toList());

        // The initial batch of window screenshots should succeed.
        for (TakeScreenshotCallback callback : takeScreenshotsOfWindows(accessibilityWindowIds)) {
            verify(callback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                    Mockito.any());
        }

        // The next batch of window screenshots, taken during the interval time, should fail.
        Thread.sleep(halfInterval);
        for (TakeScreenshotCallback callback : takeScreenshotsOfWindows(accessibilityWindowIds)) {
            verify(callback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onFailure(
                    AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT);
        }

        // The next batch of window screenshots, taken after the interval time, should succeed.
        Thread.sleep(halfInterval + 1);
        for (TakeScreenshotCallback callback : takeScreenshotsOfWindows(accessibilityWindowIds)) {
            verify(callback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onSuccess(
                    Mockito.any());
        }
    }

    @Test
    @ApiTest(apis = {"android.accessibilityservice.AccessibilityService#takeScreenshotOfWindow"})
    public void testTakeScreenshotOfWindow_InvalidWindow() {
        mService.takeScreenshotOfWindow(-1,
                mContext.getMainExecutor(), mCallback);
        verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onFailure(
                AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW);
    }

    @Test
    @ApiTest(apis = {"android.accessibilityservice.AccessibilityService#takeScreenshotOfWindow"})
    public void testTakeScreenshotOfWindow_ErrorForServiceWithoutScreenshotCapability() {
        final InstrumentedAccessibilityService serviceWithoutScreenshotCapability =
                InstrumentedAccessibilityService.enableService(
                        TouchExplorationStubAccessibilityService.class);
        try {
            // Make sure this service can retrieve windows but not take screenshots before we
            // go forward with the test.
            final int capabilities =
                    serviceWithoutScreenshotCapability.getServiceInfo().getCapabilities();
            assertThat(capabilities & CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT).isNotEqualTo(0);
            assertThat(capabilities & CAPABILITY_CAN_TAKE_SCREENSHOT).isEqualTo(0);

            final int accessibilityWindowId = serviceWithoutScreenshotCapability
                    .getWindows().get(0).getId();
            serviceWithoutScreenshotCapability.takeScreenshotOfWindow(accessibilityWindowId,
                    mContext.getMainExecutor(), mCallback);
            verify(mCallback, timeout(TIMEOUT_TAKE_SCREENSHOT_DONE_MILLIS)).onFailure(
                    AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS);
        } finally {
            serviceWithoutScreenshotCapability.disableSelfAndRemove();
        }
    }

    private List<TakeScreenshotCallback> takeScreenshotsOfWindows(
            List<Integer> accessibilityWindowIds) {
        final List<TakeScreenshotCallback> result = new ArrayList<>();
        for (Integer id : accessibilityWindowIds) {
            TakeScreenshotCallback callback = Mockito.mock(TakeScreenshotCallback.class);
            mService.takeScreenshotOfWindow(id, mContext.getMainExecutor(), callback);
            result.add(callback);
        }
        return result;
    }

    private boolean doesScreenshotContainColor(ScreenshotResult screenshot, int color) {
        final Bitmap bitmap = Bitmap.wrapHardwareBuffer(screenshot.getHardwareBuffer(),
                screenshot.getColorSpace()).copy(Bitmap.Config.ARGB_8888, false);
        final int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(),
                bitmap.getHeight());
        final int r = Color.red(color);
        final int g = Color.green(color);
        final int b = Color.blue(color);
        for (int pixel : pixels) {
            if (Color.red(pixel) == r && Color.green(pixel) == g && Color.blue(pixel) == b) {
                return true;
            }
        }
        return false;
    }

    private void takeScreenshot(int displayId) {
        mStartTestingTime = SystemClock.uptimeMillis();
        mService.takeScreenshot(displayId, mContext.getMainExecutor(),
                mCallback);
    }

    private void verifyScreenshotResult(AccessibilityService.ScreenshotResult screenshot) {
        verifyScreenshotResult(screenshot, mDisplaySize.x, mDisplaySize.y, mStartTestingTime);
    }

    private void verifyScreenshotResult(AccessibilityService.ScreenshotResult screenshot,
            int expectedWidth, int expectedHeight, long timestampBeforeTakeScreenshot) {
        assertThat(screenshot).isNotNull();
        assertThat(screenshot.getTimestamp()).isGreaterThan(timestampBeforeTakeScreenshot);

        final HardwareBuffer hardwareBuffer = screenshot.getHardwareBuffer();
        assertThat(hardwareBuffer).isNotNull();
        assertThat(hardwareBuffer.getWidth()).isEqualTo(expectedWidth);
        assertThat(hardwareBuffer.getHeight()).isEqualTo(expectedHeight);

        final ColorSpace colorSpace = screenshot.getColorSpace();
        assertThat(colorSpace).isNotNull();
        assertThat(Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)).isNotNull();
    }
}
