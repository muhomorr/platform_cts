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

package android.media.session.cts;

import static android.media.cts.MediaSessionTestHelperConstants.FLAG_CREATE_MEDIA_SESSION;
import static android.media.cts.MediaSessionTestHelperConstants.FLAG_CREATE_MEDIA_SESSION2;
import static android.media.cts.MediaSessionTestHelperConstants.FLAG_SET_MEDIA_SESSION_ACTIVE;
import static android.media.cts.MediaSessionTestHelperConstants.MEDIA_SESSION_TEST_HELPER_APK;
import static android.media.cts.MediaSessionTestHelperConstants.MEDIA_SESSION_TEST_HELPER_PKG;

import android.media.cts.BaseMultiUserTest;
import android.media.cts.MediaSessionTestHelperConstants;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.AppModeInstant;
import android.platform.test.annotations.RequiresDevice;

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.RunUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Host-side test for the media session manager that installs and runs device-side tests after the
 * proper device setup.
 * <p>Corresponding device-side tests are written in the {@link #DEVICE_SIDE_TEST_CLASS}
 * which is in the {@link #DEVICE_SIDE_TEST_APK}.
 */
public class MediaSessionManagerHostTest extends BaseMultiUserTest {
    /**
     * Package name of the device-side tests.
     */
    private static final String DEVICE_SIDE_TEST_PKG = "android.media.session.cts";
    /**
     * Package file name (.apk) for the device-side tests.
     */
    private static final String DEVICE_SIDE_TEST_APK = "CtsMediaSessionHostTestApp.apk";
    /**
     * Fully qualified class name for the device-side tests.
     */
    private static final String DEVICE_SIDE_TEST_CLASS =
            "android.media.session.cts.MediaSessionManagerTest";

    private static final int TIMEOUT_MS = 1000;

    private final List<Integer> mNotificationListeners = new ArrayList<>();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mNotificationListeners.clear();
    }

    @Override
    public void tearDown() throws Exception {
        getDevice().uninstallPackage(MEDIA_SESSION_TEST_HELPER_PKG);
        getDevice().uninstallPackage(DEVICE_SIDE_TEST_PKG);
        for (int userId : mNotificationListeners) {
            setAllowGetActiveSessionForTest(false, userId);
        }
        super.tearDown();
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with the primary user.
     */
    @AppModeInstant
    @RequiresDevice
    public void testGetActiveSessionsInstant_primaryUser() throws Exception {
        testGetActiveSessions_primaryUser(true);
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with the primary user.
     */
    @AppModeFull
    @RequiresDevice
    public void testGetActiveSessionsFull_primaryUser() throws Exception {
        testGetActiveSessions_primaryUser(false);
    }

    private void testGetActiveSessions_primaryUser(boolean instant) throws Exception {
        int mainUserId = getDevice().getMainUserId();

        setAllowGetActiveSessionForTest(true, mainUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, mainUserId, instant);
        runTest("testGetActiveSessions_noMediaSessionFromMediaSessionTestHelper");

        installAppAsUser(
                MEDIA_SESSION_TEST_HELPER_APK, MEDIA_SESSION_TEST_HELPER_PKG, mainUserId, false);
        sendControlCommand(mainUserId, FLAG_CREATE_MEDIA_SESSION);
        runTest("testGetActiveSessions_noMediaSessionFromMediaSessionTestHelper");

        sendControlCommand(mainUserId, FLAG_SET_MEDIA_SESSION_ACTIVE);
        runTest("testGetActiveSessions_hasMediaSessionFromMediaSessionTestHelper");
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with additional users.
     */
    @AppModeInstant
    @RequiresDevice
    public void testGetActiveSessionsInstant_additionalUser() throws Exception {
        testGetActiveSessions_additionalUser(true);
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with additional users.
     */
    @AppModeFull
    @RequiresDevice
    public void testGetActiveSessionsFull_additionalUser() throws Exception {
        testGetActiveSessions_additionalUser(false);
    }

    private void testGetActiveSessions_additionalUser(boolean instant) throws Exception {
        if (!canCreateAdditionalUsers(1)) {
            CLog.logAndDisplay(LogLevel.INFO,
                    "Cannot create a new user. Skipping multi-user test cases.");
            return;
        }

        // Test if another user can get the session.
        int newUser = createAndStartUser();
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, newUser, instant);
        setAllowGetActiveSessionForTest(true, newUser);
        runTestAsUser("testGetActiveSessions_noMediaSession", newUser);
        removeUser(newUser);
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with restricted profiles.
     */
    @AppModeInstant
    @RequiresDevice
    public void testGetActiveSessionsInstant_restrictedProfiles() throws Exception {
        testGetActiveSessions_restrictedProfiles(true);
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with restricted profiles.
     */
    @AppModeFull
    @RequiresDevice
    public void testGetActiveSessionsFull_restrictedProfiles() throws Exception {
        testGetActiveSessions_restrictedProfiles(false);
    }

    private void testGetActiveSessions_restrictedProfiles(boolean instant)
            throws Exception {
        if (!canCreateAdditionalUsers(1)) {
            CLog.logAndDisplay(LogLevel.INFO,
                    "Cannot create a new user. Skipping multi-user test cases.");
            return;
        }

        // Test if another restricted profile can get the session.
        // Remove the created user first not to exceed system's user number limit.
        // Restricted profile's parent must be the primary user (the system user).
        int newUser = createAndStartRestrictedProfile(getDevice().getPrimaryUserId());
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, newUser, instant);
        setAllowGetActiveSessionForTest(true, newUser);
        runTestAsUser("testGetActiveSessions_noMediaSession", newUser);
        removeUser(newUser);
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with managed profiles.
     */
    @AppModeInstant
    @RequiresDevice
    public void testGetActiveSessionsInstant_managedProfiles() throws Exception {
        testGetActiveSessions_managedProfiles(true);
    }

    /**
     * Tests {@link MediaSessionManager#getActiveSessions} with managed profiles.
     */
    @AppModeFull
    @RequiresDevice
    public void testGetActiveSessionsFull_managedProfiles() throws Exception {
        testGetActiveSessions_managedProfiles(false);
    }

    private void testGetActiveSessions_managedProfiles(boolean instant)
            throws Exception {
        if (!hasDeviceFeature("android.software.managed_users")) {
            CLog.logAndDisplay(LogLevel.INFO,
                    "Device doesn't support managed profiles. Test won't run.");
            return;
        }

        // Test if another managed profile can get the session.
        // Remove the created user first not to exceed system's user number limit.
        // Managed profile's parent must not be the primary user (in the context of this test, we
        // use the main user).
        int newUser = createAndStartManagedProfile(getDevice().getMainUserId());
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, newUser, instant);
        setAllowGetActiveSessionForTest(true, newUser);
        runTestAsUser("testGetActiveSessions_noMediaSession", newUser);
        removeUser(newUser);
    }

    @AppModeFull
    @RequiresDevice
    public void testGetActiveSessions_noSession2() throws Exception {
        int mainUserId = getDevice().getMainUserId();

        setAllowGetActiveSessionForTest(true, mainUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, mainUserId, false);
        runTest("testGetActiveSessions_noMediaSessionFromMediaSessionTestHelper");

        installAppAsUser(
                MEDIA_SESSION_TEST_HELPER_APK, MEDIA_SESSION_TEST_HELPER_PKG, mainUserId, false);
        sendControlCommand(mainUserId, FLAG_CREATE_MEDIA_SESSION2);

        // Wait for a second for framework to recognize media session2.
        RunUtil.getDefault().sleep(TIMEOUT_MS);
        runTest("testGetActiveSessions_noMediaSessionFromMediaSessionTestHelper");
    }

    @AppModeFull
    @RequiresDevice
    public void testGetActiveSessions_withSession2() throws Exception {
        int mainUserId = getDevice().getMainUserId();

        setAllowGetActiveSessionForTest(true, mainUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, mainUserId, false);
        runTest("testGetActiveSessions_noMediaSessionFromMediaSessionTestHelper");

        installAppAsUser(
                MEDIA_SESSION_TEST_HELPER_APK, MEDIA_SESSION_TEST_HELPER_PKG, mainUserId, false);
        sendControlCommand(
                mainUserId,
                FLAG_CREATE_MEDIA_SESSION
                        | FLAG_CREATE_MEDIA_SESSION2
                        | FLAG_SET_MEDIA_SESSION_ACTIVE);

        // Wait for a second for framework to recognize media session2.
        RunUtil.getDefault().sleep(TIMEOUT_MS);

        runTest("testGetActiveSessions_hasMediaSessionFromMediaSessionTestHelper");
    }

    @AppModeFull
    @RequiresDevice
    public void testOnMediaKeyEventSessionChangedListener() throws Exception {
        int mainUserId = getDevice().getMainUserId();

        setAllowGetActiveSessionForTest(true, mainUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, mainUserId, false);
        runTest("testOnMediaKeyEventSessionChangedListener");
    }

    @AppModeFull
    @RequiresDevice
    public void testOnMediaKeyEventSessionChangedListener_whenSessionIsReleased() throws Exception {
        int mainUserId = getDevice().getMainUserId();

        setAllowGetActiveSessionForTest(true, mainUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, mainUserId, false);
        runTest("testOnMediaKeyEventSessionChangedListener_whenSessionIsReleased");
    }

    @AppModeFull
    @RequiresDevice
    public void testIsTrusted_withEnabledNotificationListener_returnsTrue() throws Exception {
        if (!canCreateAdditionalUsers(1)) {
            CLog.logAndDisplay(LogLevel.INFO,
                    "Cannot create a new user. Skipping multi-user test cases.");
            return;
        }

        int newUserId = createAndStartUser();
        setAllowGetActiveSessionForTest(true, newUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, newUserId, false);
        runTestAsUser("testIsTrusted_returnsTrue", newUserId);
    }

    @AppModeFull
    @RequiresDevice
    public void testIsTrusted_withoutEnabledNotificationListener_returnsFalse()
            throws Exception {
        if (!canCreateAdditionalUsers(1)) {
            CLog.logAndDisplay(LogLevel.INFO,
                    "Cannot create a new user. Skipping multi-user test cases.");
            return;
        }

        int newUserId = createAndStartUser();
        setAllowGetActiveSessionForTest(false, newUserId);
        installAppAsUser(DEVICE_SIDE_TEST_APK, DEVICE_SIDE_TEST_PKG, newUserId, false);
        runTestAsUser("testIsTrusted_returnsFalse", newUserId);
    }

    private void runTest(String testMethodName) throws DeviceNotAvailableException {
        runTestAsUser(testMethodName, getDevice().getMainUserId());
    }

    private void runTestAsUser(String testMethodName, int userId)
            throws DeviceNotAvailableException {
        runDeviceTests(DEVICE_SIDE_TEST_PKG, DEVICE_SIDE_TEST_CLASS, testMethodName, userId);
    }

    /**
     * Sets to allow or disallow the {@link #DEVICE_SIDE_TEST_CLASS}
     * to call {@link MediaSessionManager#getActiveSessions} for testing.
     * <p>{@link MediaSessionManager#getActiveSessions} bypasses the permission check if the
     * caller is the enabled notification listener. This method uses the behavior by allowing
     * this class as the notification listener service.
     * <p>Note that the device-side test {@link android.media.cts.MediaSessionManagerTest} already
     * covers the test for failing {@link MediaSessionManager#getActiveSessions} without the
     * permission nor the notification listener.
     */
    private void setAllowGetActiveSessionForTest(boolean allow, int userId) throws Exception {
        String notificationListener = DEVICE_SIDE_TEST_PKG + "/" + DEVICE_SIDE_TEST_CLASS;
        String command = "cmd notification "
                + ((allow) ? "allow_listener " : "disallow_listener ")
                + notificationListener + " " + userId;
        executeShellCommand(command);
        if (allow) {
            mNotificationListeners.add(userId);
        }
    }

    private void sendControlCommand(int userId, int flag) throws Exception {
        executeShellCommand(MediaSessionTestHelperConstants.buildControlCommand(userId, flag));
    }
}
