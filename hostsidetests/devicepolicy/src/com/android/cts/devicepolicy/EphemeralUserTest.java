/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for ephemeral users and profiles.
 */
public final class EphemeralUserTest extends BaseDevicePolicyTest {

    @Override
    protected void assumeTestEnabled() throws Exception {
        assumeCanCreateAdditionalUsers(1);
    }

    @Override
    public void tearDown() throws Exception {
        removeTestUsers();

        super.tearDown();
    }

    /** The user should have the ephemeral flag set if it was created as ephemeral. */
    @Test
    public void testCreateEphemeralUser() throws Exception {
        int userId = createUser(FLAG_EPHEMERAL);
        int flags = getUserFlags(userId);
        assertTrue("ephemeral flag must be set", FLAG_EPHEMERAL == (flags & FLAG_EPHEMERAL));
    }

    /** The user should not have the ephemeral flag set if it was not created as ephemeral. */
    @Test
    public void testCreateLongLivedUser() throws Exception {
        int userId = createUser();
        int flags = getUserFlags(userId);
        assertTrue("ephemeral flag must not be set", 0 == (flags & FLAG_EPHEMERAL));
    }

    /**
     * Ephemeral user should be automatically removed after it is stopped.
     */
    @Test
    public void testRemoveEphemeralOnStop() throws Exception {
        int userId = createUser(FLAG_EPHEMERAL);
        startUser(userId);
        assertTrue("ephemeral user must exists after start", listUsers().contains(userId));
        stopUser(userId);
        assertFalse("ephemeral user must be removed after stop", listUsers().contains(userId));
    }

    /**
     * The guest should be automatically created ephemeral when the ephemeral-guest feature is set
     * and not ephemeral when the feature is not set.
     */
    @Test
    public void testEphemeralGuestFeature() throws Exception {
        // Create a guest user.
        int userId = createUser(FLAG_GUEST);
        int flags = getUserFlags(userId);
        if (getGuestUsersEphemeral()) {
            // Check the guest was automatically created ephemeral.
            assertTrue("ephemeral flag must be set for guest",
                    FLAG_EPHEMERAL == (flags & FLAG_EPHEMERAL));
        } else {
            // The guest should not be ephemeral.
            assertTrue("ephemeral flag must not be set for guest", 0 == (flags & FLAG_EPHEMERAL));
        }
    }

    private boolean getGuestUsersEphemeral() throws Exception {
        String commandOutput = getDevice().executeShellCommand("dumpsys user");
        String[] outputLines = commandOutput.split("\n");
        for (String line : outputLines) {
            String[] lineParts = line.split(":");
            if (lineParts.length == 2 && lineParts[0].trim().equals("All guests ephemeral")) {
                return Boolean.parseBoolean(lineParts[1].trim());
            }
        }
        return false;
    }
}
