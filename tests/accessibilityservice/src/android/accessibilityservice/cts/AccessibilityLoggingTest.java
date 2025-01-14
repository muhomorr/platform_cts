/**
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.accessibilityservice.cts;

import static android.app.AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE;

import static junit.framework.Assert.assertTrue;

import android.accessibility.cts.common.AccessibilityDumpOnFailureRule;
import android.content.Context;
import android.platform.test.annotations.Presubmit;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.AppOpsUtils;
import com.android.compatibility.common.util.CddTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@Presubmit
@CddTest(requirements = {"3.10/C-1-1,C-1-2"})
public class AccessibilityLoggingTest {

    @Rule
    public final AccessibilityDumpOnFailureRule mDumpOnFailureRule =
            new AccessibilityDumpOnFailureRule();

    /**
     * Tests that new accessibility services are logged by the system.
     */
    @Test
    public void testServiceLogged() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        String packageName = context.getPackageName();

        // There are accessibility services defined in this test package, and this fact must be
        // logged.
        assertTrue("Accessibility service was bound, but this wasn't logged by app ops",
                AppOpsUtils.allowedOperationLogged(packageName, OPSTR_BIND_ACCESSIBILITY_SERVICE));
    }
}
