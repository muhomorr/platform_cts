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

package com.android.bedstead.harrier;

import org.junit.rules.TestRule;

/** A @Rule used on device by Harrier. */
public abstract class HarrierRule implements TestRule {
    /** Sets that we should skip tearing down between tests. */
    abstract void setSkipTestTeardown(boolean skipTestTeardown);
    /** Sets that we are using the BedsteadJUnit4 test runner. */
    abstract void setUsingBedsteadJUnit4(boolean usingBedsteadJUnit4);
    /** Queries if the current device is using headless system user mode. */
    abstract boolean isHeadlessSystemUserMode();
}
