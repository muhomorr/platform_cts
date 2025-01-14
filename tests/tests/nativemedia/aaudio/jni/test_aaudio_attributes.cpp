/*
 * Copyright (C) 2018 The Android Open Source Project
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

// Test AAudio attributes such as Usage, ContentType and InputPreset.

#include <stdio.h>
#include <unistd.h>

#include <aaudio/AAudio.h>
#include <gtest/gtest.h>

#include "utils.h"

constexpr int64_t kNanosPerSecond = 1000000000;
constexpr int kNumFrames = 256;
constexpr int kChannelCount = 2;

constexpr int32_t DONT_SET = -1000;
constexpr const char *DONT_SET_STR = "don't set";

#define IS_SPATIALIZED_FALSE (AAUDIO_UNSPECIFIED + 1)
#define IS_SPATIALIZED_TRUE  (AAUDIO_UNSPECIFIED + 2)

static void checkAttributes(aaudio_performance_mode_t perfMode,
                            aaudio_usage_t usage,
                            aaudio_content_type_t contentType,
                            aaudio_spatialization_behavior_t spatializationBehavior = DONT_SET,
                            int isContentSpatialized = DONT_SET,
                            aaudio_input_preset_t preset = DONT_SET,
                            aaudio_allowed_capture_policy_t capturePolicy = DONT_SET,
                            int privacyMode = DONT_SET,
                            aaudio_direction_t direction = AAUDIO_DIRECTION_OUTPUT,
                            const char *packageName = DONT_SET_STR,
                            const char *attributionTag = DONT_SET_STR) {
    if (direction == AAUDIO_DIRECTION_INPUT
            && !deviceSupportsFeature(FEATURE_RECORDING)) return;
    else if (direction == AAUDIO_DIRECTION_OUTPUT
            && !deviceSupportsFeature(FEATURE_PLAYBACK)) return;

    std::unique_ptr<float[]> buffer(new float[kNumFrames * kChannelCount]);

    AAudioStreamBuilder *aaudioBuilder = nullptr;
    AAudioStream *aaudioStream = nullptr;

    // Use an AAudioStreamBuilder to contain requested parameters.
    ASSERT_EQ(AAUDIO_OK, AAudio_createStreamBuilder(&aaudioBuilder));

    // Request stream properties.
    AAudioStreamBuilder_setPerformanceMode(aaudioBuilder, perfMode);
    AAudioStreamBuilder_setDirection(aaudioBuilder, direction);

    // Set the attribute in the builder.
    if (usage != DONT_SET) {
        AAudioStreamBuilder_setUsage(aaudioBuilder, usage);
    }
    if (contentType != DONT_SET) {
        AAudioStreamBuilder_setContentType(aaudioBuilder, contentType);
    }
    if (spatializationBehavior != DONT_SET) {
        AAudioStreamBuilder_setSpatializationBehavior(aaudioBuilder, spatializationBehavior);
    }
    if (isContentSpatialized != DONT_SET) {
        AAudioStreamBuilder_setIsContentSpatialized(aaudioBuilder,
                                                    isContentSpatialized == IS_SPATIALIZED_TRUE);
    }
    if (preset != DONT_SET) {
        AAudioStreamBuilder_setInputPreset(aaudioBuilder, preset);
    }
    if (capturePolicy != DONT_SET) {
        AAudioStreamBuilder_setAllowedCapturePolicy(aaudioBuilder, capturePolicy);
    }
    if (privacyMode != DONT_SET) {
        AAudioStreamBuilder_setPrivacySensitive(aaudioBuilder, (bool)privacyMode);
    }
    if (packageName != DONT_SET_STR) {
        AAudioStreamBuilder_setPackageName(aaudioBuilder, packageName);
    }
    if (attributionTag != DONT_SET_STR) {
        AAudioStreamBuilder_setAttributionTag(aaudioBuilder, attributionTag);
    }

    // Create an AAudioStream using the Builder.
    ASSERT_EQ(AAUDIO_OK, AAudioStreamBuilder_openStream(aaudioBuilder, &aaudioStream));
    AAudioStreamBuilder_delete(aaudioBuilder);

    // Make sure we get the same attributes back from the stream.
    aaudio_usage_t expectedUsage =
            (usage == DONT_SET || usage == AAUDIO_UNSPECIFIED)
            ? AAUDIO_USAGE_MEDIA // default
            : usage;
    EXPECT_EQ(expectedUsage, AAudioStream_getUsage(aaudioStream));

    aaudio_content_type_t expectedContentType =
            (contentType == DONT_SET || contentType == AAUDIO_UNSPECIFIED)
            ? AAUDIO_CONTENT_TYPE_MUSIC // default
            : contentType;
    EXPECT_EQ(expectedContentType, AAudioStream_getContentType(aaudioStream));

    if (perfMode == AAUDIO_PERFORMANCE_MODE_NONE) {
        aaudio_spatialization_behavior_t expectedBehavior =
                (spatializationBehavior == DONT_SET || spatializationBehavior == AAUDIO_UNSPECIFIED)
                ? AAUDIO_SPATIALIZATION_BEHAVIOR_AUTO // default
                : spatializationBehavior;
        EXPECT_EQ(expectedBehavior, AAudioStream_getSpatializationBehavior(aaudioStream));

        bool expectedIsContentSpatialized =
                (isContentSpatialized == DONT_SET)
                ? false //default
                : isContentSpatialized == IS_SPATIALIZED_TRUE;
        EXPECT_EQ(expectedIsContentSpatialized, AAudioStream_isContentSpatialized(aaudioStream));
    }

    aaudio_input_preset_t expectedPreset =
            (preset == DONT_SET || preset == AAUDIO_UNSPECIFIED)
            ? AAUDIO_INPUT_PRESET_VOICE_RECOGNITION // default
            : preset;
    EXPECT_EQ(expectedPreset, AAudioStream_getInputPreset(aaudioStream));

    aaudio_allowed_capture_policy_t expectedCapturePolicy =
            (capturePolicy == DONT_SET || capturePolicy == AAUDIO_UNSPECIFIED)
            ? AAUDIO_ALLOW_CAPTURE_BY_ALL // default
            : capturePolicy;
    EXPECT_EQ(expectedCapturePolicy, AAudioStream_getAllowedCapturePolicy(aaudioStream));

    bool expectedPrivacyMode =
            (privacyMode == DONT_SET) ?
                ((preset == AAUDIO_INPUT_PRESET_VOICE_COMMUNICATION
                    || preset == AAUDIO_INPUT_PRESET_CAMCORDER) ? true : false) :
                privacyMode;
    EXPECT_EQ(expectedPrivacyMode, AAudioStream_isPrivacySensitive(aaudioStream));

    EXPECT_EQ(AAUDIO_OK, AAudioStream_requestStart(aaudioStream));

    if (direction == AAUDIO_DIRECTION_INPUT) {
        EXPECT_EQ(kNumFrames,
                  AAudioStream_read(aaudioStream, buffer.get(), kNumFrames, kNanosPerSecond));
    } else {
        EXPECT_EQ(kNumFrames,
                  AAudioStream_write(aaudioStream, buffer.get(), kNumFrames, kNanosPerSecond));
    }

    EXPECT_EQ(AAUDIO_OK, AAudioStream_requestStop(aaudioStream));

    EXPECT_EQ(AAUDIO_OK, AAudioStream_close(aaudioStream));
}

static const aaudio_usage_t sUsages[] = {
    DONT_SET,
    AAUDIO_UNSPECIFIED,
    AAUDIO_USAGE_MEDIA,
    AAUDIO_USAGE_VOICE_COMMUNICATION,
    AAUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING,
    AAUDIO_USAGE_ALARM,
    AAUDIO_USAGE_NOTIFICATION,
    AAUDIO_USAGE_NOTIFICATION_RINGTONE,
    AAUDIO_USAGE_NOTIFICATION_EVENT,
    AAUDIO_USAGE_ASSISTANCE_ACCESSIBILITY,
    AAUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
    AAUDIO_USAGE_ASSISTANCE_SONIFICATION,
    AAUDIO_USAGE_GAME,
    AAUDIO_USAGE_ASSISTANT,
};

static const aaudio_usage_t sSystemUsages[] = {
    AAUDIO_SYSTEM_USAGE_EMERGENCY,
    AAUDIO_SYSTEM_USAGE_SAFETY,
    AAUDIO_SYSTEM_USAGE_VEHICLE_STATUS,
    AAUDIO_SYSTEM_USAGE_ANNOUNCEMENT
};

static const aaudio_content_type_t sContentypes[] = {
    DONT_SET,
    AAUDIO_UNSPECIFIED,
    AAUDIO_CONTENT_TYPE_SPEECH,
    AAUDIO_CONTENT_TYPE_MUSIC,
    AAUDIO_CONTENT_TYPE_MOVIE,
    AAUDIO_CONTENT_TYPE_SONIFICATION
};

static const aaudio_spatialization_behavior_t sSpatializationBehavior[] = {
    DONT_SET,
    AAUDIO_UNSPECIFIED,
    AAUDIO_SPATIALIZATION_BEHAVIOR_AUTO,
    AAUDIO_SPATIALIZATION_BEHAVIOR_NEVER
};

static const int sIsContentSpatialized[] = {
    DONT_SET,
    IS_SPATIALIZED_TRUE,
    IS_SPATIALIZED_FALSE
};

static const aaudio_input_preset_t sInputPresets[] = {
    DONT_SET,
    AAUDIO_UNSPECIFIED,
    AAUDIO_INPUT_PRESET_GENERIC,
    AAUDIO_INPUT_PRESET_CAMCORDER,
    AAUDIO_INPUT_PRESET_VOICE_RECOGNITION,
    AAUDIO_INPUT_PRESET_VOICE_COMMUNICATION,
    AAUDIO_INPUT_PRESET_UNPROCESSED,
    AAUDIO_INPUT_PRESET_VOICE_PERFORMANCE,
};

static const aaudio_input_preset_t sAllowCapturePolicies[] = {
    DONT_SET,
    AAUDIO_UNSPECIFIED,
    AAUDIO_ALLOW_CAPTURE_BY_ALL,
    AAUDIO_ALLOW_CAPTURE_BY_SYSTEM,
    AAUDIO_ALLOW_CAPTURE_BY_NONE,
};

static const int sPrivacyModes[] = {
    DONT_SET,
    false,
    true,
};

static const char *sPackageNames[] = {
    DONT_SET_STR,
    "android.nativemedia.aaudio",
};

static const char *sAttributionTags[] = {
    DONT_SET_STR,
    "validTag",
    NULL,
};

static void checkAttributesUsage(aaudio_performance_mode_t perfMode) {
    for (aaudio_usage_t usage : sUsages) {
        // There can be a race condition when switching between devices,
        // which can cause an unexpected disconnection of the stream.
        usleep(500 * 1000); // wait for previous stream to completely close
        checkAttributes(perfMode, usage, DONT_SET);
    }
    usleep(500 * 1000); // wait for previous stream to completely close
}

static void checkAttributesContentType(aaudio_performance_mode_t perfMode) {
    for (aaudio_content_type_t contentType : sContentypes) {
        checkAttributes(perfMode, DONT_SET, contentType);
    }
}

static void checkAttributesSpatializationBehavior(aaudio_performance_mode_t perfMode) {
    for (aaudio_spatialization_behavior_t behavior : sSpatializationBehavior) {
        checkAttributes(perfMode,
                        DONT_SET, // usage
                        DONT_SET, // content type
                        behavior);
    }
}

static void checkAttributesIsContentSpatialized(aaudio_performance_mode_t perfMode) {
    for (int spatialized : sIsContentSpatialized) {
        checkAttributes(perfMode,
                        DONT_SET, // usage
                        DONT_SET, // content type
                        DONT_SET, // spatialization behavior
                        spatialized);
    }
}

static void checkAttributesInputPreset(aaudio_performance_mode_t perfMode) {
    for (aaudio_input_preset_t inputPreset : sInputPresets) {
        checkAttributes(perfMode,
                        DONT_SET,
                        DONT_SET,
                        DONT_SET, // spatialization behavior
                        DONT_SET, // is content spatialized
                        inputPreset,
                        DONT_SET,
                        DONT_SET,
                        AAUDIO_DIRECTION_INPUT);
    }
}

static void checkAttributesAllowedCapturePolicy(aaudio_performance_mode_t perfMode) {
    for (aaudio_allowed_capture_policy_t policy : sAllowCapturePolicies) {
        checkAttributes(perfMode,
                        DONT_SET,
                        DONT_SET,
                        DONT_SET, // spatialization behavior
                        DONT_SET, // is content spatialized
                        DONT_SET,
                        policy);
    }
}

static void checkAttributesPrivacySensitive(aaudio_performance_mode_t perfMode) {
    for (int privacyMode : sPrivacyModes) {
        checkAttributes(perfMode,
                        DONT_SET,
                        DONT_SET,
                        DONT_SET, // spatialization behavior
                        DONT_SET, // is content spatialized
                        DONT_SET,
                        DONT_SET,
                        privacyMode,
                        AAUDIO_DIRECTION_INPUT);
    }
}

class AAudioTestAttributes : public AAudioCtsBase {
};

TEST_F(AAudioTestAttributes, package_name) {
    for (const char *packageName : sPackageNames) {
        checkAttributes(AAUDIO_PERFORMANCE_MODE_NONE,
                        DONT_SET,
                        DONT_SET,
                        DONT_SET, // spatialization behavior
                        DONT_SET, // is content spatialized
                        DONT_SET,
                        DONT_SET,
                        DONT_SET,
                        AAUDIO_DIRECTION_INPUT,
                        packageName);
    }
}

TEST_F(AAudioTestAttributes, low_latency_package_name) {
    for (const char *packageName : sPackageNames) {
        checkAttributes(AAUDIO_PERFORMANCE_MODE_LOW_LATENCY,
                        DONT_SET,
                        DONT_SET,
                        DONT_SET, // spatialization behavior
                        DONT_SET, // is content spatialized
                        DONT_SET,
                        DONT_SET,
                        DONT_SET,
                        AAUDIO_DIRECTION_INPUT,
                        packageName);
    }
}

TEST_F(AAudioTestAttributes, attribution_tag) {
    for (const char *attributionTag : sAttributionTags) {
        checkAttributes(AAUDIO_PERFORMANCE_MODE_NONE,
                        DONT_SET,
                        DONT_SET,
                        DONT_SET, // spatialization behavior
                        DONT_SET, // is content spatialized
                        DONT_SET,
                        DONT_SET,
                        DONT_SET,
                        AAUDIO_DIRECTION_INPUT,
                        DONT_SET_STR,
                        attributionTag);
    }
}

TEST_F(AAudioTestAttributes, aaudio_usage_perfnone) {
    checkAttributesUsage(AAUDIO_PERFORMANCE_MODE_NONE);
}

TEST_F(AAudioTestAttributes, aaudio_content_type_perfnone) {
    checkAttributesContentType(AAUDIO_PERFORMANCE_MODE_NONE);
}

TEST_F(AAudioTestAttributes, aaudio_spatialization_behavior_perfnone) {
    checkAttributesSpatializationBehavior(AAUDIO_PERFORMANCE_MODE_NONE);
}

TEST_F(AAudioTestAttributes, aaudio_is_content_spatialized_perfnone) {
    checkAttributesIsContentSpatialized(AAUDIO_PERFORMANCE_MODE_NONE);
}

TEST_F(AAudioTestAttributes, aaudio_input_preset_perfnone) {
    checkAttributesInputPreset(AAUDIO_PERFORMANCE_MODE_NONE);
}

TEST_F(AAudioTestAttributes, aaudio_allowed_capture_policy_perfnone) {
    checkAttributesAllowedCapturePolicy(AAUDIO_PERFORMANCE_MODE_NONE);
}

TEST_F(AAudioTestAttributes, aaudio_usage_lowlat) {
    checkAttributesUsage(AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
}

TEST_F(AAudioTestAttributes, aaudio_content_type_lowlat) {
    checkAttributesContentType(AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
}

TEST_F(AAudioTestAttributes, aaudio_input_preset_lowlat) {
    checkAttributesInputPreset(AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
}

TEST_F(AAudioTestAttributes, aaudio_allowed_capture_policy_lowlat) {
    checkAttributesAllowedCapturePolicy(AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
}

TEST_F(AAudioTestAttributes, aaudio_system_usages_rejected) {
    for (aaudio_usage_t systemUsage : sSystemUsages) {
        AAudioStreamBuilder *aaudioBuilder = nullptr;
        AAudioStream *aaudioStream = nullptr;

        // Use an AAudioStreamBuilder to contain requested parameters.
        ASSERT_EQ(AAUDIO_OK, AAudio_createStreamBuilder(&aaudioBuilder));

        AAudioStreamBuilder_setUsage(aaudioBuilder, systemUsage);

        aaudio_result_t result = AAudioStreamBuilder_openStream(aaudioBuilder, &aaudioStream);

        // Get failed status when trying to create an AAudioStream using the Builder. There are two
        // potential failures: one if the device doesn't support the system usage, and the  other
        // if it does but this test doesn't have the MODIFY_AUDIO_ROUTING permission required to
        // use it.
        ASSERT_TRUE(result == AAUDIO_ERROR_ILLEGAL_ARGUMENT
                || result == AAUDIO_ERROR_INTERNAL);
        AAudioStreamBuilder_delete(aaudioBuilder);
    }
}

TEST_F(AAudioTestAttributes, aaudio_allowed_privacy_sensitive_lowlat) {
    checkAttributesPrivacySensitive(AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
}
