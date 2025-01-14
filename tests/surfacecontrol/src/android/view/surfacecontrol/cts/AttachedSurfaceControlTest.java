/*
 * Copyright (C) 2021 The Android Open Source Project
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
package android.view.surfacecontrol.cts;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.server.wm.BuildUtils.HW_TIMEOUT_MULTIPLIER;
import static android.view.cts.surfacevalidator.BitmapPixelChecker.validateScreenshot;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Rect;
import android.server.wm.IgnoreOrientationRequestSession;
import android.server.wm.WindowManagerStateHelper;
import android.util.Log;
import android.view.AttachedSurfaceControl;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.cts.surfacevalidator.BitmapPixelChecker;
import android.widget.FrameLayout;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.LargeTest;
import androidx.test.filters.RequiresDevice;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.SystemUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SuppressLint("RtlHardcoded")
@RequiresDevice
public class AttachedSurfaceControlTest {
    private static final String TAG = "AttachedSurfaceControlTest";
    private static final String FIXED_TO_USER_ROTATION_COMMAND =
            "cmd window fixed-to-user-rotation";
    private IgnoreOrientationRequestSession mOrientationSession;
    private WindowManagerStateHelper mWmState;

    private static final long WAIT_TIMEOUT_S = 5L * HW_TIMEOUT_MULTIPLIER;

    @Rule
    public TestName mName = new TestName();

    private static class TransformHintListener implements
            AttachedSurfaceControl.OnBufferTransformHintChangedListener {
        Activity activity;
        int expectedOrientation;
        CountDownLatch latch = new CountDownLatch(1);
        IntConsumer hintConsumer;

        TransformHintListener(Activity activity, int expectedOrientation,
                IntConsumer hintConsumer) {
            this.activity = activity;
            this.expectedOrientation = expectedOrientation;
            this.hintConsumer = hintConsumer;
        }

        @Override
        public void onBufferTransformHintChanged(int hint) {
            int orientation = activity.getResources().getConfiguration().orientation;
            Log.d(TAG, "onBufferTransformHintChanged: orientation actual=" + orientation
                    + " expected=" + expectedOrientation + " transformHint=" + hint);
            Assert.assertEquals("Failed to switch orientation hint=" + hint, orientation,
                    expectedOrientation);
            hintConsumer.accept(hint);
            latch.countDown();
            activity.getWindow().getRootSurfaceControl()
                    .removeOnBufferTransformHintChangedListener(this);
        }
    }

    @Before
    public void setup() {
        mOrientationSession = new IgnoreOrientationRequestSession(false /* enable */);
        mWmState = new WindowManagerStateHelper();
    }

    private void supportRotationCheck() {
        PackageManager pm =
                InstrumentationRegistry.getInstrumentation().getContext().getPackageManager();
        boolean supportsRotation = pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_PORTRAIT)
                && pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_LANDSCAPE);
        final boolean isFixedToUserRotation =
                "enabled".equals(SystemUtil.runShellCommand(FIXED_TO_USER_ROTATION_COMMAND).trim());
        Assume.assumeTrue(supportsRotation && !isFixedToUserRotation);
    }

    @After
    public void teardown() {
        if (mOrientationSession != null) {
            mOrientationSession.close();
        }
    }

    @Test
    public void testOnBufferTransformHintChangedListener() throws InterruptedException {
        supportRotationCheck();

        final int[] transformHintResult = new int[2];
        final CountDownLatch[] firstCallback = new CountDownLatch[1];
        final CountDownLatch[] secondCallback = new CountDownLatch[1];
        try (ActivityScenario<HandleConfigurationActivity> scenario =
                     ActivityScenario.launch(HandleConfigurationActivity.class)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                mWmState.computeState();
                assumeFalse("Skipping test: display area is ignoring orientation request",
                        mWmState.isTaskDisplayAreaIgnoringOrientationRequest(
                                activity.getComponentName()));
                int requestedOrientation = getRequestedOrientation(activity);
                TransformHintListener listener = new TransformHintListener(activity,
                        requestedOrientation, hint -> transformHintResult[0] = hint);
                firstCallback[0] = listener.latch;
                activity.getWindow().getRootSurfaceControl()
                        .addOnBufferTransformHintChangedListener(listener);
                setRequestedOrientation(activity, requestedOrientation);
            });
            // Check we get a callback since the orientation has changed and we expect transform
            // hint to change.
            Assert.assertTrue(firstCallback[0].await(3, TimeUnit.SECONDS));

            // Check the callback value matches the call to get the transform hint.
            scenario.onActivity(activity -> Assert.assertEquals(transformHintResult[0],
                    activity.getWindow().getRootSurfaceControl().getBufferTransformHint()));

            scenario.onActivity(activity -> {
                int requestedOrientation = getRequestedOrientation(activity);
                TransformHintListener listener = new TransformHintListener(activity,
                        requestedOrientation, hint -> transformHintResult[1] = hint);
                secondCallback[0] = listener.latch;
                activity.getWindow().getRootSurfaceControl()
                        .addOnBufferTransformHintChangedListener(listener);
                setRequestedOrientation(activity, requestedOrientation);
            });
            // Check we get a callback since the orientation has changed and we expect transform
            // hint to change.
            Assert.assertTrue(secondCallback[0].await(3, TimeUnit.SECONDS));

            // Check the callback value matches the call to get the transform hint.
            scenario.onActivity(activity -> Assert.assertEquals(transformHintResult[1],
                    activity.getWindow().getRootSurfaceControl().getBufferTransformHint()));
        }

        // If the app orientation was changed, we should get a different transform hint
        Assert.assertNotEquals(transformHintResult[0], transformHintResult[1]);
    }

    private int getRequestedOrientation(Activity activity) {
        int currentOrientation = activity.getResources().getConfiguration().orientation;
        return currentOrientation == ORIENTATION_LANDSCAPE ? ORIENTATION_PORTRAIT
                : ORIENTATION_LANDSCAPE;
    }

    private void setRequestedOrientation(Activity activity,
            /* @Configuration.Orientation */ int requestedOrientation) {
        /* @ActivityInfo.ScreenOrientation */
        Log.d(TAG, "setRequestedOrientation: requestedOrientation=" + requestedOrientation);
        int screenOrientation =
                requestedOrientation == ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        activity.setRequestedOrientation(screenOrientation);
    }

    @Test
    public void testOnBufferTransformHintChangesFromLandToSea() throws InterruptedException {
        supportRotationCheck();

        final int[] transformHintResult = new int[2];
        final CountDownLatch[] firstCallback = new CountDownLatch[1];
        final CountDownLatch[] secondCallback = new CountDownLatch[1];
        try (ActivityScenario<HandleConfigurationActivity> scenario =
                     ActivityScenario.launch(HandleConfigurationActivity.class)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                mWmState.computeState();
                assumeFalse("Skipping test: display area is ignoring orientation request",
                        mWmState.isTaskDisplayAreaIgnoringOrientationRequest(
                                activity.getComponentName()));
                if (activity.getResources().getConfiguration().orientation
                        == ORIENTATION_LANDSCAPE) {
                    return;
                }
                TransformHintListener listener = new TransformHintListener(activity,
                        ORIENTATION_LANDSCAPE, hint -> transformHintResult[0] = hint);
                firstCallback[0] = listener.latch;
                activity.getWindow().getRootSurfaceControl()
                        .addOnBufferTransformHintChangedListener(listener);
                setRequestedOrientation(activity, ORIENTATION_LANDSCAPE);
            });

            // If the device is already in landscape, do nothing.
            if (firstCallback[0] != null) {
                Assert.assertTrue(firstCallback[0].await(3, TimeUnit.SECONDS));
                scenario.onActivity(activity -> Assert.assertEquals(transformHintResult[0],
                    activity.getWindow().getRootSurfaceControl().getBufferTransformHint()));
            }

            scenario.onActivity(activity -> {
                TransformHintListener listener = new TransformHintListener(activity,
                        ORIENTATION_LANDSCAPE, hint -> transformHintResult[1] = hint);
                secondCallback[0] = listener.latch;
                activity.getWindow().getRootSurfaceControl()
                        .addOnBufferTransformHintChangedListener(listener);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            });
            // Check we get a callback since the orientation has changed and we expect transform
            // hint to change.
            Assert.assertTrue(secondCallback[0].await(3, TimeUnit.SECONDS));

            // Check the callback value matches the call to get the transform hint.
            scenario.onActivity(activity -> Assert.assertEquals(transformHintResult[1],
                    activity.getWindow().getRootSurfaceControl().getBufferTransformHint()));
        }

        // If the app orientation was changed, we should get a different transform hint
        Assert.assertNotEquals(transformHintResult[0], transformHintResult[1]);
    }

    private static class GreenAnchorViewWithInsets extends View {
        SurfaceControl mSurfaceControl;
        final Surface mSurface;

        private final Rect mChildBoundingInsets;

        private final CountDownLatch mDrawCompleteLatch = new CountDownLatch(1);

        GreenAnchorViewWithInsets(Context c, Rect insets) {
            super(c, null, 0, 0);
            mSurfaceControl = new SurfaceControl.Builder()
                    .setName("SurfaceAnchorView")
                    .setBufferSize(100, 100)
                    .build();
            mSurface = new Surface(mSurfaceControl);
            Canvas canvas = mSurface.lockHardwareCanvas();
            canvas.drawColor(Color.GREEN);
            mSurface.unlockCanvasAndPost(canvas);
            mChildBoundingInsets = insets;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            SurfaceControl.Transaction t =
                    getRootSurfaceControl().buildReparentTransaction(mSurfaceControl);

            getRootSurfaceControl().setChildBoundingInsets(mChildBoundingInsets);
            t.setLayer(mSurfaceControl, 1)
                    .setVisibility(mSurfaceControl, true)
                    .apply();

            t.addTransactionCommittedListener(Runnable::run, mDrawCompleteLatch::countDown);
            getRootSurfaceControl().applyTransactionOnDraw(t);
        }

        @Override
        protected void onDetachedFromWindow() {
            new SurfaceControl.Transaction().reparent(mSurfaceControl, null).apply();
            mSurfaceControl.release();
            mSurface.release();

            super.onDetachedFromWindow();
        }

        public void waitForDrawn() {
            try {
                assertTrue("Failed to wait for frame to draw",
                        mDrawCompleteLatch.await(WAIT_TIMEOUT_S, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail();
            }
        }
    }

    @Test
    public void testCropWithChildBoundingInsets() throws Throwable {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            final GreenAnchorViewWithInsets[] view = new GreenAnchorViewWithInsets[1];
            final Activity[] activity = new Activity[1];
            scenario.onActivity(a -> {
                activity[0] = a;
                FrameLayout parentLayout = a.getParentLayout();
                GreenAnchorViewWithInsets anchorView = new GreenAnchorViewWithInsets(a,
                        new Rect(0, 10, 0, 0));
                parentLayout.addView(anchorView,
                        new FrameLayout.LayoutParams(100, 100, Gravity.LEFT | Gravity.TOP));

                view[0] = anchorView;
                countDownLatch.countDown();
            });
            assertTrue("Failed to wait for activity to start",
                    countDownLatch.await(WAIT_TIMEOUT_S, TimeUnit.SECONDS));

            view[0].waitForDrawn();
            // Do not include system insets because the child SC is not layed out in the system
            // insets
            validateScreenshot(mName, activity[0],
                    new BitmapPixelChecker(Color.GREEN, new Rect(0, 10, 100, 100)),
                    9000 /* expectedMatchingPixels */, Insets.NONE);
        }
    }
}
