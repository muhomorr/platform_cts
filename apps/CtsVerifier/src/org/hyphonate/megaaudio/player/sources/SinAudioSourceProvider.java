/*
 * Copyright 2020 The Android Open Source Project
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
package org.hyphonate.megaaudio.player.sources;

import org.hyphonate.megaaudio.player.AudioSource;
import org.hyphonate.megaaudio.player.AudioSourceProvider;
import org.hyphonate.megaaudio.player.NativeAudioSource;

public class SinAudioSourceProvider implements AudioSourceProvider {
    // Cache sources
    AudioSource mJavaSource;
    NativeAudioSource mNativeSource;

    @Override
    public AudioSource getJavaSource() {
        return mJavaSource != null ? mJavaSource : (mJavaSource = new SinAudioSource());
    }

    @Override
    public NativeAudioSource getNativeSource() {
        return mNativeSource != null
                ? mNativeSource
                : (mNativeSource = new NativeAudioSource(allocNativeSource()));
    }

    private native long allocNativeSource();
}
