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
package com.android.cts.devicepolicy;

import android.platform.test.annotations.FlakyTest;

import org.junit.Test;

/**
 * BaseDeviceAdminHostSideTest for device admin targeting API level 23.
 */
public final class DeviceAdminHostSideTestApi23 extends BaseDeviceAdminHostSideTest {
    @Override
    protected int getTargetApiVersion() {
        return 23;
    }

    /**
     * Device admin with no BIND_DEVICE_ADMIN can still be activated, if the target SDK <= 23.
     */
    @FlakyTest
    @Test
    public void testAdminWithNoProtection() throws Exception {
        installAppAsUser(getDeviceAdminApkFileName(), mUserId);
        try {
            setDeviceAdmin(getUnprotectedAdminReceiverComponent(), mUserId);
        } finally {
            runTests(getDeviceAdminApkPackage(), "ClearDeviceAdminWithNoProtectionTest");
        }
    }
}
