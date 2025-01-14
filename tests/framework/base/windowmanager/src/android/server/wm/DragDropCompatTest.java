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

package android.server.wm;

import static android.server.wm.ShellCommandHelper.executeShellCommand;

import android.platform.test.annotations.Presubmit;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@Presubmit
@RunWith(AndroidJUnit4.class)
public class DragDropCompatTest extends DragDropTest {
    static final String TAG = "DragDropCompatTest";
    static final String PACKAGE_NAME = "android.server.wm.cts";

    @Before
    public void setUp() throws InterruptedException {
        executeShellCommand("am compat enable --no-kill DOWNSCALED" + " " + PACKAGE_NAME);
        executeShellCommand("am compat enable --no-kill DOWNSCALE_50" + " " + PACKAGE_NAME);
        mInvCompatScale = 1 / 0.5f;
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        mInvCompatScale = 1.0f;
        executeShellCommand("am compat disable --no-kill DOWNSCALED " + PACKAGE_NAME);
        executeShellCommand("am compat disable --no-kill DOWNSCALE_50" + " " + PACKAGE_NAME);
    }
}
