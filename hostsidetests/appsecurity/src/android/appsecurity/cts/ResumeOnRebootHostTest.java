/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.appsecurity.cts;

import static android.appsecurity.cts.Utils.waitForBootCompleted;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import com.android.compatibility.common.util.ApiLevelUtil;
import com.android.compatibility.common.util.HostSideTestUtils;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.RunUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Set of tests that verify behavior of Resume on Reboot, if supported.
 * <p>
 * Note that these tests drive PIN setup manually instead of relying on device
 * administrators, which are not supported by all devices.
 */
@RunWith(DeviceJUnit4ClassRunner.class)
public class ResumeOnRebootHostTest extends BaseHostJUnit4Test {
    private static final String TAG = "ResumeOnRebootTest";

    private static final String PKG = "com.android.cts.encryptionapp";
    private static final String CLASS = PKG + ".EncryptionAppTest";
    private static final String APK = "CtsEncryptionApp.apk";

    private static final String OTHER_APK = "CtsSplitApp.apk";
    private static final String OTHER_PKG = "com.android.cts.splitapp";

    private static final String FEATURE_REBOOT_ESCROW = "feature:android.hardware.reboot_escrow";
    private static final String FEATURE_DEVICE_ADMIN = "feature:android.software.device_admin";
    private static final String FEATURE_SECURE_LOCK_SCREEN =
            "feature:android.software.secure_lock_screen";

    private static final long SHUTDOWN_TIME_MS = TimeUnit.SECONDS.toMillis(30);
    private static final int USER_SYSTEM = 0;

    private static final int USER_SWITCH_TIMEOUT_SECONDS = 10;
    private static final long USER_SWITCH_WAIT = TimeUnit.SECONDS.toMillis(10);
    private static final int UNLOCK_BROADCAST_WAIT_SECONDS = 10;

    private boolean mSupportsMultiUser;

    @Rule(order = 0)
    public BootCountTrackerRule mBootCountTrackingRule = new BootCountTrackerRule(this, 0);

    @Rule(order = 1)
    public NormalizeScreenStateRule mNoDozeRule = new NormalizeScreenStateRule(this);

    @Before
    public void setUp() throws Exception {
        assertNotNull(getAbi());
        assertNotNull(getBuild());

        mSupportsMultiUser = getDevice().getMaxNumberOfUsersSupported() > 1;

        setScreenStayOnValue(true);
        removeTestPackages();
        deviceDisableDeviceConfigSync();
        deviceSetupServerBasedParameter();
    }

    @After
    public void tearDown() throws Exception {
        removeTestPackages();
        deviceCleanupServerBasedParameter();
        deviceRestoreDeviceConfigSync();
        setScreenStayOnValue(false);
    }

    @Test
    public void resumeOnReboot_ManagedProfile_Success() throws Exception {
        assumeTrue("Device isn't at least S or has no lock screen", isSupportedSDevice());
        assumeTrue("Device does not support file-based encryption", supportFileBasedEncryption());

        if (!getDevice().hasFeature("android.software.managed_users")) {
            CLog.v(TAG, "Device doesn't support managed users; skipping test");
            return;
        }

        int[] users = Utils.prepareSingleUser(getDevice());
        int initialUser = users[0];

        int managedUserId = createManagedProfile(initialUser);

        try {
            // Set up test app and secure lock screens
            installTestPackages();

            deviceSetup(initialUser);
            deviceRequestLskf();
            deviceLock(initialUser);
            deviceEnterLskf(initialUser);
            deviceRebootAndApply();

            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", initialUser);
            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", managedUserId);
        } finally {
            stopUserAsync(managedUserId);
            removeUser(managedUserId);

            // Remove secure lock screens and tear down test app
            runDeviceTestsAsUser("testTearDown", initialUser);

            deviceClearLskf();
        }
    }

    @Test
    public void resumeOnReboot_TwoUsers_SingleUserUnlock_Success() throws Exception {
        assumeTrue("Device isn't at least S or has no lock screen", isSupportedSDevice());
        assumeTrue("Device does not support file-based encryption", supportFileBasedEncryption());

        if (!mSupportsMultiUser) {
            CLog.v(TAG, "Device doesn't support multi-user; skipping test");
            return;
        }

        int[] users = Utils.prepareMultipleUsers(getDevice(), 2);
        int initialUser = users[0];
        int secondaryUser = users[1];

        try {
            // Set up test app and secure lock screens
            installTestPackages();

            switchUser(secondaryUser);
            deviceSetup(secondaryUser);

            switchUser(initialUser);
            deviceSetup(initialUser);

            deviceRequestLskf();

            deviceLock(initialUser);
            deviceEnterLskf(initialUser);

            deviceRebootAndApply();

            // Try to start early to calm down broadcast storms.
            getDevice().startUser(secondaryUser);

            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", initialUser);

            switchUser(secondaryUser);
            runDeviceTestsAsUser("testVerifyLockedAndDismiss", secondaryUser);
        } finally {
            // Remove secure lock screens and tear down test app
            switchUser(secondaryUser);
            runDeviceTestsAsUser("testTearDown", secondaryUser);
            switchUser(initialUser);
            runDeviceTestsAsUser("testTearDown", initialUser);

            deviceClearLskf();
        }
    }

    @Test
    public void resumeOnReboot_TwoUsers_BothUserUnlock_Success() throws Exception {
        assumeTrue("Device isn't at least S or has no lock screen", isSupportedSDevice());
        assumeTrue("Device does not support file-based encryption", supportFileBasedEncryption());

        if (!mSupportsMultiUser) {
            CLog.v(TAG, "Device doesn't support multi-user; skipping test");
            return;
        }

        int[] users = Utils.prepareMultipleUsers(getDevice(), 2);
        int initialUser = users[0];
        int secondaryUser = users[1];

        try {
            installTestPackages();

            switchUser(secondaryUser);
            deviceSetup(secondaryUser);

            switchUser(initialUser);
            deviceSetup(initialUser);

            deviceRequestLskf();

            deviceLock(initialUser);
            deviceEnterLskf(initialUser);

            switchUser(secondaryUser);
            deviceEnterLskf(secondaryUser);

            deviceRebootAndApply();

            // Try to start early to calm down broadcast storms.
            getDevice().startUser(secondaryUser);

            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", initialUser);

            switchUser(secondaryUser);
            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", secondaryUser);
        } finally {
            // Remove secure lock screens and tear down test app
            switchUser(secondaryUser);
            runDeviceTestsAsUser("testTearDown", secondaryUser);
            switchUser(initialUser);
            runDeviceTestsAsUser("testTearDown", initialUser);

            deviceClearLskf();
        }
    }

    @Test
    public void resumeOnReboot_SingleUser_ServerBased_Success() throws Exception {
        assumeTrue("Device isn't at least S or has no lock screen", isSupportedSDevice());
        assumeTrue("Device does not support file-based encryption", supportFileBasedEncryption());

        int[] users = Utils.prepareSingleUser(getDevice());
        int initialUser = users[0];

        try {
            installTestPackages();

            deviceSetup(initialUser);
            deviceRequestLskf();
            deviceLock(initialUser);
            deviceEnterLskf(initialUser);
            deviceRebootAndApply();

            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", initialUser);
            runDeviceTestsAsUser("testCheckServiceInteraction", initialUser);
        } finally {
            // Remove secure lock screens and tear down test app
            runDeviceTestsAsUser("testTearDown", initialUser);

            deviceClearLskf();
        }
    }

    @Test
    public void resumeOnReboot_SingleUser_MultiClient_ClientASuccess() throws Exception {
        assumeTrue("Device isn't at least S or has no lock screen", isSupportedSDevice());
        assumeTrue("Device does not support file-based encryption", supportFileBasedEncryption());

        int[] users = Utils.prepareSingleUser(getDevice());
        int initialUser = users[0];

        final String clientA = "ClientA";
        final String clientB = "ClientB";
        try {
            installTestPackages();

            deviceSetup(initialUser);
            deviceRequestLskf(clientA);
            deviceRequestLskf(clientB);

            deviceLock(initialUser);
            deviceEnterLskf(initialUser);

            // Client B's clear shouldn't affect client A's preparation.
            deviceClearLskf(clientB);
            deviceRebootAndApply(clientA);

            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", initialUser);
            runDeviceTestsAsUser("testCheckServiceInteraction", initialUser);
        } finally {
            // Remove secure lock screens and tear down test app
            runDeviceTestsAsUser("testTearDown", initialUser);

            deviceClearLskf();
        }
    }

    @Test
    public void resumeOnReboot_SingleUser_MultiClient_ClientBSuccess() throws Exception {
        assumeTrue("Device isn't at least S or has no lock screen", isSupportedSDevice());
        assumeTrue("Device does not support file-based encryption", supportFileBasedEncryption());

        int[] users = Utils.prepareSingleUser(getDevice());
        int initialUser = users[0];

        final String clientA = "ClientA";
        final String clientB = "ClientB";
        try {
            installTestPackages();

            deviceSetup(initialUser);
            deviceRequestLskf(clientA);

            deviceLock(initialUser);
            deviceEnterLskf(initialUser);

            // Both clients have prepared
            deviceRequestLskf(clientB);
            deviceRebootAndApply(clientB);

            runDeviceTestsAsUser("testVerifyUnlockedAndDismiss", initialUser);
            runDeviceTestsAsUser("testCheckServiceInteraction", initialUser);
        } finally {
            // Remove secure lock screens and tear down test app
            runDeviceTestsAsUser("testTearDown", initialUser);

            deviceClearLskf();
        }
    }

    private void deviceDisableDeviceConfigSync() throws Exception {
        getDevice().executeShellCommand("device_config set_sync_disabled_for_tests persistent");
        String res = getDevice().executeShellCommand("device_config get_sync_disabled_for_tests");
        if (res == null || !res.contains("persistent")) {
            CLog.w(TAG, "Could not disable device config for test");
        }
    }

    private void deviceRestoreDeviceConfigSync() throws Exception {
        getDevice().executeShellCommand("device_config set_sync_disabled_for_tests none");
    }


    private void deviceSetupServerBasedParameter() throws Exception {
        getDevice().executeShellCommand("device_config put ota server_based_ror_enabled true");
        String res = getDevice().executeShellCommand(
                "device_config get ota server_based_ror_enabled");
        if (res == null || !res.contains("true")) {
            fail("could not set up server based ror");
        }

        getDevice().executeShellCommand(
                "cmd lock_settings set-resume-on-reboot-provider-package " + PKG);
    }

    private void deviceCleanupServerBasedParameter() throws Exception {
        getDevice().executeShellCommand("device_config put ota server_based_ror_enabled false");
        String res = getDevice().executeShellCommand(
                "device_config get ota server_based_ror_enabled");
        if (res == null || !res.contains("false")) {
            fail("could not clean up server based ror");
        }

        getDevice().executeShellCommand(
                "cmd lock_settings set-resume-on-reboot-provider-package ");
    }

    private void deviceSetup(int userId) throws Exception {
        // To receive boot broadcasts, kick our other app out of stopped state
        getDevice().executeShellCommand("am start -a android.intent.action.MAIN"
                + " --user " + userId
                + " -c android.intent.category.LAUNCHER com.android.cts.splitapp/.MyActivity");

        // Give enough time for PackageManager to persist stopped state
        RunUtil.getDefault().sleep(15000);

        runDeviceTestsAsUser("testSetUp", userId);

        // Give enough time for vold to update keys
        RunUtil.getDefault().sleep(15000);
    }

    private void deviceRequestLskf() throws Exception {
        deviceRequestLskf(PKG);
    }

    private void deviceRequestLskf(String clientName) throws Exception {
        String res = getDevice().executeShellCommand("cmd recovery request-lskf " + clientName);
        if (res == null || !res.contains("success")) {
            fail("could not set up recovery request-lskf");
        }
    }

    private void deviceClearLskf() throws Exception {
        deviceClearLskf(PKG);
    }

    private void deviceClearLskf(String clientName) throws Exception {
        String res = getDevice().executeShellCommand("cmd recovery clear-lskf " + clientName);
        if (res == null || !res.contains("success")) {
            fail("could not clear-lskf");
        }
    }

    private void deviceLock(int userId) throws Exception {
        int retriesLeft = 3;
        boolean retry = false;
        do {
            if (retry) {
                CLog.i("Retrying to summon lockscreen...");
                RunUtil.getDefault().sleep(500);
            }
            runDeviceTestsAsUser("testLockScreen", userId);
            retry = !LockScreenInspector.newInstance(getDevice()).isDisplayedAndNotOccluded();
        } while (retriesLeft-- > 0 && retry);

        if (retry) {
            CLog.e("Could not summon lockscreen...");
            fail("Device could not be locked");
        }
    }

    private void deviceEnterLskf(int userId) throws Exception {
        runDeviceTestsAsUser("testUnlockScreen", userId);
    }

    private void verifyLskfCaptured(String clientName) throws Exception {
        HostSideTestUtils.waitUntil("Lskf isn't captured after "
                        + UNLOCK_BROADCAST_WAIT_SECONDS + " seconds for " + clientName,
                UNLOCK_BROADCAST_WAIT_SECONDS, () -> isLskfCapturedForClient(clientName));
    }

    private boolean isLskfCapturedForClient(String clientName) throws Exception {
        Pattern pattern = Pattern.compile(".*LSKF capture status: (\\w+)");
        String status = getDevice().executeShellCommand(
                "cmd recovery is-lskf-captured " + clientName);
        Matcher matcher = pattern.matcher(status);
        if (!matcher.find()) {
            CLog.i(TAG, "is-lskf-captured isn't implemented on build, assuming captured");
            return true;
        }

        return "true".equalsIgnoreCase(matcher.group(1));
    }

    private void deviceRebootAndApply() throws Exception {
        deviceRebootAndApply(PKG);
    }

    private void deviceRebootAndApply(String clientName) throws Exception {
        verifyLskfCaptured(clientName);
        mBootCountTrackingRule.increaseExpectedBootCountDifference(1);

        String res = executeShellCommandWithLogging(
                "cmd recovery reboot-and-apply " + clientName + " cts-test");
        if (res != null && res.contains("Reboot and apply status: failure")) {
            fail("could not call reboot-and-apply");
        }

        getDevice().waitForDeviceNotAvailable(SHUTDOWN_TIME_MS);
        getDevice().waitForDeviceOnline(120000);

        waitForBootCompleted(getDevice());
    }

    private void installTestPackages() throws Exception {
        new InstallMultiple().addFile(APK).run();
        new InstallMultiple().addFile(OTHER_APK).run();
    }

    private void removeTestPackages() throws DeviceNotAvailableException {
        getDevice().uninstallPackage(PKG);
        getDevice().uninstallPackage(OTHER_PKG);
    }

    private ArrayList<Integer> listUsers() throws DeviceNotAvailableException {
        return getDevice().listUsers();
    }

    /**
     * Calls switch-user, but without trying to dismiss the keyguard.
     */
    private void switchUser(int userId) throws Exception {
        getDevice().switchUser(userId);
        HostSideTestUtils.waitUntil("Could not switch users", USER_SWITCH_TIMEOUT_SECONDS,
                () -> getDevice().getCurrentUser() == userId);
        RunUtil.getDefault().sleep(USER_SWITCH_WAIT);
    }

    private void stopUserAsync(int userId) throws Exception {
        executeShellCommandWithLogging("am stop-user -f " + userId);
    }

    private void removeUser(int userId) throws Exception {
        if (listUsers().contains(userId) && userId != USER_SYSTEM) {
            // Don't log output, as tests sometimes set no debug user restriction, which
            // causes this to fail, we should still continue and remove the user.
            String stopUserCommand = "am stop-user -w -f " + userId;
            CLog.d("stopping and removing user " + userId);
            getDevice().executeShellCommand(stopUserCommand);
            // Ephemeral users may have already been removed after being stopped.
            if (listUsers().contains(userId)) {
                assertThat("Couldn't remove user", getDevice().removeUser(userId), is(true));
            }
        }
    }

    private int createManagedProfile(int parentUserId) throws DeviceNotAvailableException {
        String commandOutput = getCreateManagedProfileCommandOutput(parentUserId);
        return getUserIdFromCreateUserCommandOutput(commandOutput);
    }

    private int getUserIdFromCreateUserCommandOutput(String commandOutput) {
        // Extract the id of the new user.
        String[] tokens = commandOutput.split("\\s+");
        assertThat(commandOutput + " expected to have format \"Success: {USER_ID}\"",
                tokens.length, greaterThan(0));
        assertThat("Command output should start with \"Success\"" + commandOutput, tokens[0],
                is("Success:"));
        return Integer.parseInt(tokens[tokens.length - 1]);
    }

    private String getCreateManagedProfileCommandOutput(int parentUserId)
            throws DeviceNotAvailableException {
        String command = "pm create-user --profileOf " + parentUserId + " --managed "
                + "TestProfile_" + System.currentTimeMillis();
        CLog.d("Starting command " + command);
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.d("Output for command " + command + ": " + commandOutput);
        return commandOutput;
    }

    private void runDeviceTestsAsUser(String testMethodName, int userId)
            throws DeviceNotAvailableException {
        Utils.runDeviceTestsAsCurrentUser(getDevice(), PKG, CLASS, testMethodName);
    }

    private boolean isSupportedSDevice() throws Exception {
        // The following tests targets API level >= S.
        boolean isAtleastS = ApiLevelUtil.isAfter(getDevice(), 30 /* BUILD.VERSION_CODES.R */)
                || ApiLevelUtil.codenameEquals(getDevice(), "S");

        return isAtleastS && getDevice().hasFeature(FEATURE_SECURE_LOCK_SCREEN);
    }

    private boolean supportFileBasedEncryption() throws Exception {
        return "file".equals(getDevice().getProperty("ro.crypto.type"));
    }

    private class InstallMultiple extends BaseInstallMultiple<InstallMultiple> {
        public InstallMultiple() {
            super(getDevice(), getBuild(), getAbi());
        }
    }

    private String executeShellCommandWithLogging(String command)
            throws DeviceNotAvailableException {
        CLog.d("Starting command: " + command);
        String result = getDevice().executeShellCommand(command);
        CLog.d("Output for command \"" + command + "\": " + result);
        return result;
    }

    private void setScreenStayOnValue(boolean value) throws DeviceNotAvailableException {
        getDevice().executeShellCommand("svc power stayon " + value);
    }
}
