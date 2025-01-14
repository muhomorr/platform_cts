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
package com.android.cts.devicepolicy;

import static com.android.cts.devicepolicy.DeviceAdminFeaturesCheckerRule.FEATURE_MANAGED_USERS;

import com.android.cts.devicepolicy.DeviceAdminFeaturesCheckerRule.RequiresAdditionalFeatures;

/**
 * Tests the DPC transfer functionality for profile owner. Testing is done by having two test DPCs,
 * CtsTransferOwnerOutgoingApp and CtsTransferOwnerIncomingApp. The former is the current DPC
 * and the latter will be the new DPC after transfer. In order to run the tests from the correct
 * process, first we setup some policies in the client side in CtsTransferOwnerOutgoingApp and then
 * we verify the policies are still there in CtsTransferOwnerIncomingApp.
 */
// We need managed users to be supported in order to create a profile of the user owner.
@RequiresAdditionalFeatures({FEATURE_MANAGED_USERS})
public final class MixedProfileOwnerHostSideTransferTest extends
        DeviceAndProfileOwnerHostSideTransferTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        int profileOwnerUserId = setupManagedProfile(TRANSFER_OWNER_OUTGOING_APK,
                TRANSFER_OWNER_OUTGOING_TEST_RECEIVER);
        if (profileOwnerUserId != -1) {
            setupTestParameters(profileOwnerUserId, TRANSFER_PROFILE_OWNER_OUTGOING_TEST,
                    TRANSFER_PROFILE_OWNER_INCOMING_TEST);
            installAppAsUser(TRANSFER_OWNER_INCOMING_APK, mUserId);
        }
    }
}
