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

package android.car.cts.builtin;

import android.app.Instrumentation;
import android.car.test.AbstractExpectableTestCase;
import android.car.test.ApiCheckerRule;
import android.content.Context;

import androidx.test.InstrumentationRegistry;

import org.junit.Rule;

/**
 * Base class for all tests, provides most common functionalities.
 */
public abstract class AbstractCarBuiltinTestCase extends AbstractExpectableTestCase {

    @Rule
    public final ApiCheckerRule mApiCheckerRule = new ApiCheckerRule.Builder()
            .disableApiRequirementsCheck().build();

    protected final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    protected final Context mContext = mInstrumentation.getContext();
}
