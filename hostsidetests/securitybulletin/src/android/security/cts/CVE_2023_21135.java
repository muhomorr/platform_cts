/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.security.cts;

import static org.junit.Assume.assumeNoException;

import android.platform.test.annotations.AsbSecurityTest;

import com.android.sts.common.tradefed.testtype.NonRootSecurityTestCase;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DeviceJUnit4ClassRunner.class)
public class CVE_2023_21135 extends NonRootSecurityTestCase {

    // b/260570119
    // Vulnerable module : Settings.apk
    @AsbSecurityTest(cveBugId = 260570119)
    @Test
    public void testPocCVE_2023_21135() {
        try {
            final String testPkg = "android.security.cts.CVE_2023_21135";
            installPackage("CVE-2023-21135.apk");

            runDeviceTests(testPkg, testPkg + ".DeviceTest", "testNotificationPermission");
        } catch (Exception e) {
            assumeNoException(e);
        } finally {
            try {
                getDevice().executeShellCommand("input keyevent KEYCODE_HOME");
            } catch (Exception ignored) {
                // ignore the exceptions
            }
        }
    }
}
