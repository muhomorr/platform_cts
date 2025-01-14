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

package android.credentials.cts.unittests.service;

import static com.google.common.truth.Truth.assertThat;

import static org.testng.Assert.assertThrows;

import android.app.slice.Slice;
import android.credentials.cts.unittests.TestUtils;
import android.net.Uri;
import android.platform.test.annotations.AppModeFull;
import android.service.credentials.RemoteEntry;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@SmallTest
@AppModeFull(reason = "unit test")
@RunWith(AndroidJUnit4.class)
public class RemoteEntryTest {

    private final Slice mSlice = new Slice.Builder(Uri.parse("foo://bar"), null).addText(
            "some text", null, List.of(Slice.HINT_TITLE)).build();

    @Test
    public void testConstructor_nullSlice() {
        assertThrows(NullPointerException.class, () -> new RemoteEntry(null));
    }

    @Test
    public void testConstructor() {
        final RemoteEntry entry = new RemoteEntry(mSlice);
        assertThat(entry.getSlice()).isSameInstanceAs(mSlice);
    }

    @Test
    public void testWriteToParcel() {
        final RemoteEntry entry1 = new RemoteEntry(mSlice);

        final RemoteEntry entry2 = TestUtils.cloneParcelable(entry1);
        assertThat(entry2.getSlice()).isEqualTo(entry2.getSlice());
    }
}
