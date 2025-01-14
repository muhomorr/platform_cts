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

package com.android.cts.graphics.framerateoverride;

import android.compat.cts.CompatChangeGatingTestCase;
import android.view.Display;

import com.google.common.collect.ImmutableSet;

/**
 * Tests for frame rate override and the behavior of {@link Display#getRefreshRate()} and
 * {@link Display.Mode#getRefreshRate()} Api.
 */
public class FrameRateOverrideHostTest extends CompatChangeGatingTestCase {

    protected static final String TEST_APK = "CtsHostsideFrameRateOverrideTestsApp.apk";
    protected static final String TEST_PKG = "com.android.cts.graphics.framerateoverride";

    // See b/170503758 for more details
    private static final long DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID = 170503758;

    @Override
    protected void setUp() throws Exception {
        installPackage(TEST_APK, true);
        // add device config to enable game mode
        runCommand("device_config put game_overlay " + TEST_PKG + " mode=2:mode=3");
    }

    @Override
    protected void tearDown() throws Exception {
        // remove device config
        runCommand("device_config delete game_overlay " + TEST_PKG);
        uninstallPackage(TEST_PKG, true);
    }

    public void testGameModeBackpressureDisplayModeReturnsPhysicalRefreshRateEnabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeBackpressure",
                /*enabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID),
                /*disabledChanges*/
                ImmutableSet.of());
    }

    public void testGameModeBackpressureDisplayModeReturnsPhysicalRefreshRateDisabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeBackpressure",
                /*enabledChanges*/
                ImmutableSet.of(),
                /*disabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID));
    }

    public void testGameModeChoreographerDisplayModeReturnsPhysicalRefreshRateEnabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeChoreographer",
                /*enabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID),
                /*disabledChanges*/
                ImmutableSet.of());
    }

    public void testGameModeChoreographerDisplayModeReturnsPhysicalRefreshRateDisabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeChoreographer",
                /*enabledChanges*/
                ImmutableSet.of(),
                /*disabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID));
    }

    public void testGameModeDisplayGetRefreshRateDisplayModeReturnsPhysicalRefreshRateEnabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeDisplayGetRefreshRate",
                /*enabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID),
                /*disabledChanges*/
                ImmutableSet.of());
    }

    public void testGameModeDisplayGetRefreshRateDisplayModeReturnsPhysicalRefreshRateDisabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeDisplayGetRefreshRate",
                /*enabledChanges*/
                ImmutableSet.of(),
                /*disabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID));
    }

    public void testGameModeDisplayModeGetRefreshRateDisplayModeReturnsPhysicalRefreshRateEnabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeDisplayModeGetRefreshRateDisplayModeReturnsPhysicalRefreshRateEnabled",
                /*enabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID),
                /*disabledChanges*/
                ImmutableSet.of());
    }

    public void testGameModeDisplayModeGetRefreshRateDisplayModeReturnsPhysicalRefreshRateDisabled()
            throws Exception {
        runDeviceCompatTest(TEST_PKG, ".FrameRateOverrideTest",
                "testGameModeDisplayModeGetRefreshRateDisplayModeReturnsPhysicalRefreshRateDisabled",
                /*enabledChanges*/
                ImmutableSet.of(),
                /*disabledChanges*/
                ImmutableSet.of(DISPLAY_MODE_RETURNS_PHYSICAL_REFRESH_RATE_CHANGEID));
    }
}
