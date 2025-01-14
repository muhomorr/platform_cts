/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.cts.content;

import static com.android.cts.content.Utils.ALWAYS_SYNCABLE_AUTHORITY;
import static com.android.cts.content.Utils.SYNC_TIMEOUT_MILLIS;
import static com.android.cts.content.Utils.allowSyncAdapterRunInBackgroundAndDataInBackground;
import static com.android.cts.content.Utils.disallowSyncAdapterRunInBackgroundAndDataInBackground;
import static com.android.cts.content.Utils.getUiDevice;
import static com.android.cts.content.Utils.hasDataConnection;
import static com.android.cts.content.Utils.hasNotificationSupport;
import static com.android.cts.content.Utils.isWatch;
import static com.android.cts.content.Utils.requestSync;
import static com.android.cts.content.Utils.withAccount;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.ActivityManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.res.Configuration;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

/**
 * Tests whether a sync adapter can access accounts.
 */
@RunWith(AndroidJUnit4.class)
public class CtsSyncAccountAccessOtherCertTestCases {
    private static final long UI_TIMEOUT_MILLIS = 5000; // 5 sec
    private static final String LOG_TAG =
            CtsSyncAccountAccessOtherCertTestCases.class.getSimpleName();

    private static final Pattern PERMISSION_REQUESTED = Pattern.compile(
            "Permission Requested.*|Permission requested.*");
    private static final Pattern ALLOW_SYNC = Pattern.compile("ALLOW|Allow");
    private static final String OPEN_NOTIFICATION_WATCH = "Open";

    @Rule
    public final TestRule mFlakyTestRule = new FlakyTestRule(3);

    @Rule
    public final ActivityTestRule<StubActivity> activity = new ActivityTestRule(StubActivity.class);

    @Before
    public void setUp() throws Exception {
        allowSyncAdapterRunInBackgroundAndDataInBackground();
    }

    @After
    public void tearDown() throws Exception {
        disallowSyncAdapterRunInBackgroundAndDataInBackground();
    }

    /*
    @Ignore("In some cases test cannot scroll through notifications to find permission request "
            + "b/147410068")
     */
    @Test
    public void testAccountAccess_otherCertAsAuthenticatorCanNotSeeAccount() throws Exception {
        assumeTrue(hasDataConnection());
        assumeTrue(hasNotificationSupport());
        assumeFalse(isRunningInVR());
        assumeFalse(isWatch());

        // If running in a test harness the Account Manager never denies access to an account. Hence
        // the permission request will not trigger. b/72114924
        assumeFalse(ActivityManager.isRunningInTestHarness());

        try (AutoCloseable ignored = withAccount(activity.getActivity())) {
            AbstractThreadedSyncAdapter adapter = AlwaysSyncableSyncService.getInstance(
                    activity.getActivity()).setNewDelegate();

            SyncRequest request = requestSync(ALWAYS_SYNCABLE_AUTHORITY);
            Log.i(LOG_TAG, "Sync requested " + request);

            Thread.sleep(SYNC_TIMEOUT_MILLIS);
            verify(adapter, never()).onPerformSync(any(), any(), any(), any(), any());
            Log.i(LOG_TAG, "Did not get onPerformSync");

            UiDevice uiDevice = getUiDevice();
            if (isWatch()) {
                UiObject2 notification = findPermissionNotificationInStream(uiDevice);
                notification.click();
                UiObject2 openButton = uiDevice.wait(
                        Until.findObject(By.text(OPEN_NOTIFICATION_WATCH)), UI_TIMEOUT_MILLIS);
                if (openButton != null) {
                    // older sysui may not have the "open" button
                    openButton.click();
                }
            } else {
                uiDevice.openNotification();
                int scrollUps = 0;

                while (true) {
                    try {
                        UiObject2 permissionRequest = uiDevice.wait(
                                Until.findObject(By.text(PERMISSION_REQUESTED)), UI_TIMEOUT_MILLIS);

                        permissionRequest.click();
                        break;
                    } catch (Throwable t) {
                        if (scrollUps < 10) {
                            // The notification we search for is below the fold, scroll to find it
                            scrollNotifications();
                            scrollUps++;
                            continue;
                        }

                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        getUiDevice().dumpWindowHierarchy(os);

                        Log.w(LOG_TAG, "Window hierarchy:");
                        for (String line : os.toString("UTF-8").split("\n")) {
                            Log.w(LOG_TAG, line);

                            // Do not overwhelm logging
                            Thread.sleep(10);
                        }

                        throw t;
                    }
                }
            }

            uiDevice.wait(Until.findObject(By.text(ALLOW_SYNC)), UI_TIMEOUT_MILLIS).click();

            ContentResolver.requestSync(request);

            verify(adapter, timeout(SYNC_TIMEOUT_MILLIS)).onPerformSync(any(), any(), any(), any(),
                    any());
            Log.i(LOG_TAG, "Got onPerformSync");
        }
    }

    private UiObject2 findPermissionNotificationInStream(UiDevice uiDevice) {
        uiDevice.pressHome();
        swipeUp(uiDevice);
        if (uiDevice.hasObject(By.text(PERMISSION_REQUESTED))) {
          return uiDevice.findObject(By.text(PERMISSION_REQUESTED));
        }
        for (int i = 0; i < 100; i++) {
          if (!swipeUp(uiDevice)) {
            // We have reached the end of the stream and not found the target.
            break;
          }
          if (uiDevice.hasObject(By.text(PERMISSION_REQUESTED))) {
            return uiDevice.findObject(By.text(PERMISSION_REQUESTED));
          }
        }
        return null;
    }

    private boolean swipeUp(UiDevice uiDevice) {
        int width = uiDevice.getDisplayWidth();
        int height = uiDevice.getDisplayHeight();
        return uiDevice.swipe(
            width / 2 /* startX */,
            height / 2 /* startY */,
            width / 2 /* endX */,
            1 /* endY */,
            50 /* numberOfSteps */);
    }

    private boolean scrollNotifications() {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        if (!scrollable.exists()) {
            return false;
        }
        try {
            return scrollable.scrollForward(50);
        } catch (UiObjectNotFoundException e) {
            return false;
        }
    }

    private boolean isRunningInVR() {
        final Context context = InstrumentationRegistry.getTargetContext();
        return ((context.getResources().getConfiguration().uiMode &
                 Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_VR_HEADSET);
    }
}
