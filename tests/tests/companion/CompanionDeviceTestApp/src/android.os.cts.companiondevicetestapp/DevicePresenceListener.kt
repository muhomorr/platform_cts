/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.os.cts.companiondevicetestapp

import android.companion.CompanionDeviceService

class DevicePresenceListener : CompanionDeviceService() {

    override fun onDeviceAppeared(address: String) {
        toast("Device appeared: $address")
    }

    override fun onDeviceDisappeared(address: String) {
        toast("Device disappeared: $address")
    }
}
