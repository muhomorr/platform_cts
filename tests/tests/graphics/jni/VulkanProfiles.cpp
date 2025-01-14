/*
 * Copyright 2023 The Android Open Source Project
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
 *
 */

#define LOG_TAG "VulkanFeaturesTest"

#ifndef VK_USE_PLATFORM_ANDROID_KHR
#define VK_USE_PLATFORM_ANDROID_KHR
#endif

#include <cstddef>
#include <cstring>
#include <cstdint>
#include <cmath>
#include <vector>
#include <algorithm>
#include "VulkanProfiles.h"

#include <android/log.h>
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)


namespace detail {


VPAPI_ATTR bool isMultiple(double source, double multiple) {
    double mod = std::fmod(source, multiple);
    return std::abs(mod) < 0.0001;
}

VPAPI_ATTR bool isPowerOfTwo(double source) {
    double mod = std::fmod(source, 1.0);
    if (std::abs(mod) >= 0.0001) return false;

    std::uint64_t value = static_cast<std::uint64_t>(std::abs(source));
    return !(value & (value - static_cast<std::uint64_t>(1)));
}

using PFN_vpStructFiller = void(*)(VkBaseOutStructure* p);
using PFN_vpStructComparator = bool(*)(VkBaseOutStructure* p);
using PFN_vpStructChainerCb =  void(*)(VkBaseOutStructure* p, void* pUser);
using PFN_vpStructChainer = void(*)(VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb);

struct VpFeatureDesc {
    PFN_vpStructFiller              pfnFiller;
    PFN_vpStructComparator          pfnComparator;
    PFN_vpStructChainer             pfnChainer;
};

struct VpPropertyDesc {
    PFN_vpStructFiller              pfnFiller;
    PFN_vpStructComparator          pfnComparator;
    PFN_vpStructChainer             pfnChainer;
};

struct VpQueueFamilyDesc {
    PFN_vpStructFiller              pfnFiller;
    PFN_vpStructComparator          pfnComparator;
};

struct VpFormatDesc {
    VkFormat                        format;
    PFN_vpStructFiller              pfnFiller;
    PFN_vpStructComparator          pfnComparator;
};

struct VpStructChainerDesc {
    PFN_vpStructChainer             pfnFeature;
    PFN_vpStructChainer             pfnProperty;
    PFN_vpStructChainer             pfnQueueFamily;
    PFN_vpStructChainer             pfnFormat;
};

struct VpProfileDesc {
    VpProfileProperties             props;
    uint32_t                        minApiVersion;

    const VkExtensionProperties*    pInstanceExtensions;
    uint32_t                        instanceExtensionCount;

    const VkExtensionProperties*    pDeviceExtensions;
    uint32_t                        deviceExtensionCount;

    const VpProfileProperties*      pFallbacks;
    uint32_t                        fallbackCount;

    const VkStructureType*          pFeatureStructTypes;
    uint32_t                        featureStructTypeCount;
    VpFeatureDesc                   feature;

    const VkStructureType*          pPropertyStructTypes;
    uint32_t                        propertyStructTypeCount;
    VpPropertyDesc                  property;

    const VkStructureType*          pQueueFamilyStructTypes;
    uint32_t                        queueFamilyStructTypeCount;
    const VpQueueFamilyDesc*        pQueueFamilies;
    uint32_t                        queueFamilyCount;

    const VkStructureType*          pFormatStructTypes;
    uint32_t                        formatStructTypeCount;
    const VpFormatDesc*             pFormats;
    uint32_t                        formatCount;

    VpStructChainerDesc             chainers;
};

template <typename T>
VPAPI_ATTR bool vpCheckFlags(const T& actual, const uint64_t expected) {
    return (actual & expected) == expected;
}

#ifdef VP_ANDROID_baseline_2021
namespace VP_ANDROID_BASELINE_2021 {

static const VkExtensionProperties instanceExtensions[] = {
    VkExtensionProperties{ VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_ANDROID_SURFACE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_FENCE_CAPABILITIES_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_SURFACE_EXTENSION_NAME, 1 },
};

static const VkExtensionProperties deviceExtensions[] = {
    VkExtensionProperties{ VK_GOOGLE_DISPLAY_TIMING_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_DESCRIPTOR_UPDATE_TEMPLATE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_FENCE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_FENCE_FD_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_INCREMENTAL_PRESENT_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_MAINTENANCE_1_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_STORAGE_BUFFER_STORAGE_CLASS_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_SWAPCHAIN_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_VARIABLE_POINTERS_EXTENSION_NAME, 1 },
};

static const VkStructureType featureStructTypes[] = {
    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR,
};

static const VkStructureType propertyStructTypes[] = {
    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR,
};

static const VkStructureType formatStructTypes[] = {
    VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR,
    VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3_KHR,
};

static const VpFeatureDesc featureDesc = {
    [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR: {
                    VkPhysicalDeviceFeatures2KHR* s = static_cast<VkPhysicalDeviceFeatures2KHR*>(static_cast<void*>(p));
                    s->features.depthBiasClamp = VK_TRUE;
                    s->features.fragmentStoresAndAtomics = VK_TRUE;
                    s->features.fullDrawIndexUint32 = VK_TRUE;
                    s->features.imageCubeArray = VK_TRUE;
                    s->features.independentBlend = VK_TRUE;
                    s->features.robustBufferAccess = VK_TRUE;
                    s->features.sampleRateShading = VK_TRUE;
                    s->features.shaderSampledImageArrayDynamicIndexing = VK_TRUE;
                    s->features.shaderStorageImageArrayDynamicIndexing = VK_TRUE;
                    s->features.shaderUniformBufferArrayDynamicIndexing = VK_TRUE;
                    s->features.textureCompressionASTC_LDR = VK_TRUE;
                    s->features.textureCompressionETC2 = VK_TRUE;
                } break;
                default: break;
            }
    },
    [](VkBaseOutStructure* p) -> bool {
        bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR: {
                    VkPhysicalDeviceFeatures2KHR* s = static_cast<VkPhysicalDeviceFeatures2KHR*>(static_cast<void*>(p));
                    ret = ret && (s->features.depthBiasClamp == VK_TRUE);
                    ret = ret && (s->features.fragmentStoresAndAtomics == VK_TRUE);
                    ret = ret && (s->features.fullDrawIndexUint32 == VK_TRUE);
                    ret = ret && (s->features.imageCubeArray == VK_TRUE);
                    ret = ret && (s->features.independentBlend == VK_TRUE);
                    ret = ret && (s->features.robustBufferAccess == VK_TRUE);
                    ret = ret && (s->features.sampleRateShading == VK_TRUE);
                    ret = ret && (s->features.shaderSampledImageArrayDynamicIndexing == VK_TRUE);
                    ret = ret && (s->features.shaderStorageImageArrayDynamicIndexing == VK_TRUE);
                    ret = ret && (s->features.shaderUniformBufferArrayDynamicIndexing == VK_TRUE);
                    ret = ret && (s->features.textureCompressionASTC_LDR == VK_TRUE);
                    ret = ret && (s->features.textureCompressionETC2 == VK_TRUE);
                } break;
                default: break;
            }
        return ret;
    },
    nullptr
};

static const VpPropertyDesc propertyDesc = {
    [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR: {
                    VkPhysicalDeviceProperties2KHR* s = static_cast<VkPhysicalDeviceProperties2KHR*>(static_cast<void*>(p));
                    s->properties.limits.discreteQueuePriorities = 2;
                    s->properties.limits.framebufferColorSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.framebufferDepthSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.framebufferNoAttachmentsSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.framebufferStencilSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.maxBoundDescriptorSets = 4;
                    s->properties.limits.maxColorAttachments = 4;
                    s->properties.limits.maxComputeSharedMemorySize = 16384;
                    s->properties.limits.maxComputeWorkGroupCount[0] = 65535;
                    s->properties.limits.maxComputeWorkGroupCount[1] = 65535;
                    s->properties.limits.maxComputeWorkGroupCount[2] = 65535;
                    s->properties.limits.maxComputeWorkGroupInvocations = 128;
                    s->properties.limits.maxComputeWorkGroupSize[0] = 128;
                    s->properties.limits.maxComputeWorkGroupSize[1] = 128;
                    s->properties.limits.maxComputeWorkGroupSize[2] = 64;
                    s->properties.limits.maxDescriptorSetInputAttachments = 4;
                    s->properties.limits.maxDescriptorSetSampledImages = 48;
                    s->properties.limits.maxDescriptorSetSamplers = 48;
                    s->properties.limits.maxDescriptorSetStorageBuffers = 24;
                    s->properties.limits.maxDescriptorSetStorageBuffersDynamic = 4;
                    s->properties.limits.maxDescriptorSetStorageImages = 12;
                    s->properties.limits.maxDescriptorSetUniformBuffers = 36;
                    s->properties.limits.maxDescriptorSetUniformBuffersDynamic = 8;
                    s->properties.limits.maxDrawIndexedIndexValue = 4294967295;
                    s->properties.limits.maxDrawIndirectCount = 1;
                    s->properties.limits.maxFragmentCombinedOutputResources = 8;
                    s->properties.limits.maxFragmentInputComponents = 64;
                    s->properties.limits.maxFragmentOutputAttachments = 4;
                    s->properties.limits.maxFramebufferHeight = 4096;
                    s->properties.limits.maxFramebufferLayers = 256;
                    s->properties.limits.maxFramebufferWidth = 4096;
                    s->properties.limits.maxImageArrayLayers = 256;
                    s->properties.limits.maxImageDimension1D = 4096;
                    s->properties.limits.maxImageDimension2D = 4096;
                    s->properties.limits.maxImageDimension3D = 512;
                    s->properties.limits.maxImageDimensionCube = 4096;
                    s->properties.limits.maxInterpolationOffset = 0.4375f;
                    s->properties.limits.maxMemoryAllocationCount = 4096;
                    s->properties.limits.maxPerStageDescriptorInputAttachments = 4;
                    s->properties.limits.maxPerStageDescriptorSampledImages = 16;
                    s->properties.limits.maxPerStageDescriptorSamplers = 16;
                    s->properties.limits.maxPerStageDescriptorStorageBuffers = 4;
                    s->properties.limits.maxPerStageDescriptorStorageImages = 4;
                    s->properties.limits.maxPerStageDescriptorUniformBuffers = 12;
                    s->properties.limits.maxPerStageResources = 44;
                    s->properties.limits.maxPushConstantsSize = 128;
                    s->properties.limits.maxSampleMaskWords = 1;
                    s->properties.limits.maxSamplerAllocationCount = 4000;
                    s->properties.limits.maxSamplerAnisotropy = 1.0f;
                    s->properties.limits.maxSamplerLodBias = 2.0f;
                    s->properties.limits.maxStorageBufferRange = 134217728;
                    s->properties.limits.maxTexelBufferElements = 65536;
                    s->properties.limits.maxTexelOffset = 7;
                    s->properties.limits.maxUniformBufferRange = 16384;
                    s->properties.limits.maxVertexInputAttributeOffset = 2047;
                    s->properties.limits.maxVertexInputAttributes = 16;
                    s->properties.limits.maxVertexInputBindingStride = 2048;
                    s->properties.limits.maxVertexInputBindings = 16;
                    s->properties.limits.maxVertexOutputComponents = 64;
                    s->properties.limits.maxViewportDimensions[0] = 4096;
                    s->properties.limits.maxViewportDimensions[1] = 4096;
                    s->properties.limits.maxViewports = 1;
                    s->properties.limits.minInterpolationOffset = -0.5f;
                    s->properties.limits.minMemoryMapAlignment = 4096;
                    s->properties.limits.minStorageBufferOffsetAlignment = 256;
                    s->properties.limits.minTexelBufferOffsetAlignment = 256;
                    s->properties.limits.minTexelOffset = -8;
                    s->properties.limits.minUniformBufferOffsetAlignment = 256;
                    s->properties.limits.mipmapPrecisionBits = 4;
                    s->properties.limits.pointSizeGranularity = 1;
                    s->properties.limits.sampledImageColorSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.sampledImageDepthSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.sampledImageIntegerSampleCounts = (VK_SAMPLE_COUNT_1_BIT);
                    s->properties.limits.sampledImageStencilSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.standardSampleLocations = VK_TRUE;
                    s->properties.limits.storageImageSampleCounts = (VK_SAMPLE_COUNT_1_BIT);
                    s->properties.limits.subPixelInterpolationOffsetBits = 4;
                    s->properties.limits.subPixelPrecisionBits = 4;
                    s->properties.limits.subTexelPrecisionBits = 4;
                    s->properties.limits.viewportBoundsRange[0] = -8192;
                    s->properties.limits.viewportBoundsRange[1] = 8191;
                } break;
                default: break;
            }
    },
    [](VkBaseOutStructure* p) -> bool {
        bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR: {
                    VkPhysicalDeviceProperties2KHR* s = static_cast<VkPhysicalDeviceProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (s->properties.limits.discreteQueuePriorities >= 2);
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferColorSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferDepthSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferNoAttachmentsSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferStencilSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (s->properties.limits.maxBoundDescriptorSets >= 4);
                    ret = ret && (s->properties.limits.maxColorAttachments >= 4);
                    ret = ret && (s->properties.limits.maxComputeSharedMemorySize >= 16384);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupCount[0] >= 65535);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupCount[1] >= 65535);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupCount[2] >= 65535);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupInvocations >= 128);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupSize[0] >= 128);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupSize[1] >= 128);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupSize[2] >= 64);
                    ret = ret && (s->properties.limits.maxDescriptorSetInputAttachments >= 4);
                    ret = ret && (s->properties.limits.maxDescriptorSetSampledImages >= 48);
                    ret = ret && (s->properties.limits.maxDescriptorSetSamplers >= 48);
                    ret = ret && (s->properties.limits.maxDescriptorSetStorageBuffers >= 24);
                    ret = ret && (s->properties.limits.maxDescriptorSetStorageBuffersDynamic >= 4);
                    ret = ret && (s->properties.limits.maxDescriptorSetStorageImages >= 12);
                    ret = ret && (s->properties.limits.maxDescriptorSetUniformBuffers >= 36);
                    ret = ret && (s->properties.limits.maxDescriptorSetUniformBuffersDynamic >= 8);
                    ret = ret && (s->properties.limits.maxDrawIndexedIndexValue >= 4294967295);
                    ret = ret && (s->properties.limits.maxDrawIndirectCount >= 1);
                    ret = ret && (s->properties.limits.maxFragmentCombinedOutputResources >= 8);
                    ret = ret && (s->properties.limits.maxFragmentInputComponents >= 64);
                    ret = ret && (s->properties.limits.maxFragmentOutputAttachments >= 4);
                    ret = ret && (s->properties.limits.maxFramebufferHeight >= 4096);
                    ret = ret && (s->properties.limits.maxFramebufferLayers >= 256);
                    ret = ret && (s->properties.limits.maxFramebufferWidth >= 4096);
                    ret = ret && (s->properties.limits.maxImageArrayLayers >= 256);
                    ret = ret && (s->properties.limits.maxImageDimension1D >= 4096);
                    ret = ret && (s->properties.limits.maxImageDimension2D >= 4096);
                    ret = ret && (s->properties.limits.maxImageDimension3D >= 512);
                    ret = ret && (s->properties.limits.maxImageDimensionCube >= 4096);
                    ret = ret && (s->properties.limits.maxInterpolationOffset >= 0.4375);
                    ret = ret && (s->properties.limits.maxMemoryAllocationCount >= 4096);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorInputAttachments >= 4);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorSampledImages >= 16);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorSamplers >= 16);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorStorageBuffers >= 4);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorStorageImages >= 4);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorUniformBuffers >= 12);
                    ret = ret && (s->properties.limits.maxPerStageResources >= 44);
                    ret = ret && (s->properties.limits.maxPushConstantsSize >= 128);
                    ret = ret && (s->properties.limits.maxSampleMaskWords >= 1);
                    ret = ret && (s->properties.limits.maxSamplerAllocationCount >= 4000);
                    ret = ret && (s->properties.limits.maxSamplerAnisotropy >= 1.0);
                    ret = ret && (s->properties.limits.maxSamplerLodBias >= 2.0);
                    ret = ret && (s->properties.limits.maxStorageBufferRange >= 134217728);
                    ret = ret && (s->properties.limits.maxTexelBufferElements >= 65536);
                    ret = ret && (s->properties.limits.maxTexelOffset >= 7);
                    ret = ret && (s->properties.limits.maxUniformBufferRange >= 16384);
                    ret = ret && (s->properties.limits.maxVertexInputAttributeOffset >= 2047);
                    ret = ret && (s->properties.limits.maxVertexInputAttributes >= 16);
                    ret = ret && (s->properties.limits.maxVertexInputBindingStride >= 2048);
                    ret = ret && (s->properties.limits.maxVertexInputBindings >= 16);
                    ret = ret && (s->properties.limits.maxVertexOutputComponents >= 64);
                    ret = ret && (s->properties.limits.maxViewportDimensions[0] >= 4096);
                    ret = ret && (s->properties.limits.maxViewportDimensions[1] >= 4096);
                    ret = ret && (s->properties.limits.maxViewports >= 1);
                    ret = ret && (s->properties.limits.minInterpolationOffset <= -0.5);
                    ret = ret && (s->properties.limits.minMemoryMapAlignment <= 4096);
                    ret = ret && ((s->properties.limits.minMemoryMapAlignment & (s->properties.limits.minMemoryMapAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.minStorageBufferOffsetAlignment <= 256);
                    ret = ret && ((s->properties.limits.minStorageBufferOffsetAlignment & (s->properties.limits.minStorageBufferOffsetAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.minTexelBufferOffsetAlignment <= 256);
                    ret = ret && ((s->properties.limits.minTexelBufferOffsetAlignment & (s->properties.limits.minTexelBufferOffsetAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.minTexelOffset <= -8);
                    ret = ret && (s->properties.limits.minUniformBufferOffsetAlignment <= 256);
                    ret = ret && ((s->properties.limits.minUniformBufferOffsetAlignment & (s->properties.limits.minUniformBufferOffsetAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.mipmapPrecisionBits >= 4);
                    ret = ret && (s->properties.limits.pointSizeGranularity <= 1);
                    ret = ret && (isMultiple(1, s->properties.limits.pointSizeGranularity));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageColorSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageDepthSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageIntegerSampleCounts, (VK_SAMPLE_COUNT_1_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageStencilSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (s->properties.limits.standardSampleLocations == VK_TRUE);
                    ret = ret && (vpCheckFlags(s->properties.limits.storageImageSampleCounts, (VK_SAMPLE_COUNT_1_BIT)));
                    ret = ret && (s->properties.limits.subPixelInterpolationOffsetBits >= 4);
                    ret = ret && (s->properties.limits.subPixelPrecisionBits >= 4);
                    ret = ret && (s->properties.limits.subTexelPrecisionBits >= 4);
                    ret = ret && (s->properties.limits.viewportBoundsRange[0] <= -8192);
                    ret = ret && (s->properties.limits.viewportBoundsRange[1] >= 8191);
                } break;
                default: break;
            }
        return ret;
    },
    nullptr
};

static const VpFormatDesc formatDesc[] = {
    {
        VK_FORMAT_A1R5G5B5_UNORM_PACK16,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A2B10G10R10_UINT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A2B10G10R10_UNORM_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_SINT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_SNORM_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_SRGB_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_UINT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_UNORM_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x10_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x10_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x6_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x6_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x10_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x10_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x12_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x12_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_4x4_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_4x4_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x4_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x4_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x6_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x6_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x6_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x6_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B10G11R11_UFLOAT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B4G4R4A4_UNORM_PACK16,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B8G8R8A8_SRGB,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B8G8R8A8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_D16_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_D32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_E5B9G9R9_UFLOAT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11G11_SNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11G11_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11_SNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A1_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A1_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32B32A32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32B32A32_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32B32A32_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R5G6B5_UNORM_PACK16,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_SRGB,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
};

static const VpStructChainerDesc chainerDesc = {
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        p->pNext = static_cast<VkBaseOutStructure*>(static_cast<void*>(nullptr));
        pfnCb(p, pUser);
    },
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        p->pNext = static_cast<VkBaseOutStructure*>(static_cast<void*>(nullptr));
        pfnCb(p, pUser);
    },
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        pfnCb(p, pUser);
    },
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        VkFormatProperties3KHR formatProperties3KHR;
        formatProperties3KHR.sType = VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3_KHR;
        formatProperties3KHR.pNext = nullptr;
        p->pNext = static_cast<VkBaseOutStructure*>(static_cast<void*>(&formatProperties3KHR));
        pfnCb(p, pUser);
    },
};

} // namespace VP_ANDROID_BASELINE_2021
#endif

#ifdef VP_ANDROID_baseline_cpu_only_2021
namespace VP_ANDROID_BASELINE_CPU_ONLY_2021 {

static const VkExtensionProperties instanceExtensions[] = {
    VkExtensionProperties{ VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_ANDROID_SURFACE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_FENCE_CAPABILITIES_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_MEMORY_CAPABILITIES_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_SEMAPHORE_CAPABILITIES_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_SURFACE_EXTENSION_NAME, 1 },
};

static const VkExtensionProperties deviceExtensions[] = {
    VkExtensionProperties{ VK_KHR_DEDICATED_ALLOCATION_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_DESCRIPTOR_UPDATE_TEMPLATE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_FENCE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_MEMORY_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_SEMAPHORE_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_GET_MEMORY_REQUIREMENTS_2_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_INCREMENTAL_PRESENT_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_MAINTENANCE_1_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_STORAGE_BUFFER_STORAGE_CLASS_EXTENSION_NAME, 1 },
    VkExtensionProperties{ VK_KHR_SWAPCHAIN_EXTENSION_NAME, 1 },
};

static const VkStructureType featureStructTypes[] = {
    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR,
};

static const VkStructureType propertyStructTypes[] = {
    VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR,
};

static const VkStructureType formatStructTypes[] = {
    VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR,
    VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3_KHR,
};

static const VpFeatureDesc featureDesc = {
    [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR: {
                    VkPhysicalDeviceFeatures2KHR* s = static_cast<VkPhysicalDeviceFeatures2KHR*>(static_cast<void*>(p));
                    s->features.depthBiasClamp = VK_TRUE;
                    s->features.fragmentStoresAndAtomics = VK_TRUE;
                    s->features.fullDrawIndexUint32 = VK_TRUE;
                    s->features.imageCubeArray = VK_TRUE;
                    s->features.independentBlend = VK_TRUE;
                    s->features.robustBufferAccess = VK_TRUE;
                    s->features.sampleRateShading = VK_TRUE;
                    s->features.shaderSampledImageArrayDynamicIndexing = VK_TRUE;
                    s->features.shaderStorageImageArrayDynamicIndexing = VK_TRUE;
                    s->features.shaderUniformBufferArrayDynamicIndexing = VK_TRUE;
                    s->features.textureCompressionASTC_LDR = VK_TRUE;
                    s->features.textureCompressionETC2 = VK_TRUE;
                } break;
                default: break;
            }
    },
    [](VkBaseOutStructure* p) -> bool {
        bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR: {
                    VkPhysicalDeviceFeatures2KHR* s = static_cast<VkPhysicalDeviceFeatures2KHR*>(static_cast<void*>(p));
                    ret = ret && (s->features.depthBiasClamp == VK_TRUE);
                    ret = ret && (s->features.fragmentStoresAndAtomics == VK_TRUE);
                    ret = ret && (s->features.fullDrawIndexUint32 == VK_TRUE);
                    ret = ret && (s->features.imageCubeArray == VK_TRUE);
                    ret = ret && (s->features.independentBlend == VK_TRUE);
                    ret = ret && (s->features.robustBufferAccess == VK_TRUE);
                    ret = ret && (s->features.sampleRateShading == VK_TRUE);
                    ret = ret && (s->features.shaderSampledImageArrayDynamicIndexing == VK_TRUE);
                    ret = ret && (s->features.shaderStorageImageArrayDynamicIndexing == VK_TRUE);
                    ret = ret && (s->features.shaderUniformBufferArrayDynamicIndexing == VK_TRUE);
                    ret = ret && (s->features.textureCompressionASTC_LDR == VK_TRUE);
                    ret = ret && (s->features.textureCompressionETC2 == VK_TRUE);
                } break;
                default: break;
            }
        return ret;
    },
    nullptr
};

static const VpPropertyDesc propertyDesc = {
    [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR: {
                    VkPhysicalDeviceProperties2KHR* s = static_cast<VkPhysicalDeviceProperties2KHR*>(static_cast<void*>(p));
                    s->properties.limits.discreteQueuePriorities = 2;
                    s->properties.limits.framebufferColorSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.framebufferDepthSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.framebufferNoAttachmentsSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.framebufferStencilSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.maxBoundDescriptorSets = 4;
                    s->properties.limits.maxColorAttachments = 4;
                    s->properties.limits.maxComputeSharedMemorySize = 16384;
                    s->properties.limits.maxComputeWorkGroupCount[0] = 65535;
                    s->properties.limits.maxComputeWorkGroupCount[1] = 65535;
                    s->properties.limits.maxComputeWorkGroupCount[2] = 65535;
                    s->properties.limits.maxComputeWorkGroupInvocations = 128;
                    s->properties.limits.maxComputeWorkGroupSize[0] = 128;
                    s->properties.limits.maxComputeWorkGroupSize[1] = 128;
                    s->properties.limits.maxComputeWorkGroupSize[2] = 64;
                    s->properties.limits.maxDescriptorSetInputAttachments = 4;
                    s->properties.limits.maxDescriptorSetSampledImages = 48;
                    s->properties.limits.maxDescriptorSetSamplers = 48;
                    s->properties.limits.maxDescriptorSetStorageBuffers = 24;
                    s->properties.limits.maxDescriptorSetStorageBuffersDynamic = 4;
                    s->properties.limits.maxDescriptorSetStorageImages = 12;
                    s->properties.limits.maxDescriptorSetUniformBuffers = 36;
                    s->properties.limits.maxDescriptorSetUniformBuffersDynamic = 8;
                    s->properties.limits.maxDrawIndexedIndexValue = 4294967295;
                    s->properties.limits.maxDrawIndirectCount = 1;
                    s->properties.limits.maxFragmentCombinedOutputResources = 8;
                    s->properties.limits.maxFragmentInputComponents = 64;
                    s->properties.limits.maxFragmentOutputAttachments = 4;
                    s->properties.limits.maxFramebufferHeight = 4096;
                    s->properties.limits.maxFramebufferLayers = 256;
                    s->properties.limits.maxFramebufferWidth = 4096;
                    s->properties.limits.maxImageArrayLayers = 256;
                    s->properties.limits.maxImageDimension1D = 4096;
                    s->properties.limits.maxImageDimension2D = 4096;
                    s->properties.limits.maxImageDimension3D = 512;
                    s->properties.limits.maxImageDimensionCube = 4096;
                    s->properties.limits.maxInterpolationOffset = 0.4375f;
                    s->properties.limits.maxMemoryAllocationCount = 4096;
                    s->properties.limits.maxPerStageDescriptorInputAttachments = 4;
                    s->properties.limits.maxPerStageDescriptorSampledImages = 16;
                    s->properties.limits.maxPerStageDescriptorSamplers = 16;
                    s->properties.limits.maxPerStageDescriptorStorageBuffers = 4;
                    s->properties.limits.maxPerStageDescriptorStorageImages = 4;
                    s->properties.limits.maxPerStageDescriptorUniformBuffers = 12;
                    s->properties.limits.maxPerStageResources = 44;
                    s->properties.limits.maxPushConstantsSize = 128;
                    s->properties.limits.maxSampleMaskWords = 1;
                    s->properties.limits.maxSamplerAllocationCount = 4000;
                    s->properties.limits.maxSamplerAnisotropy = 1.0f;
                    s->properties.limits.maxSamplerLodBias = 2.0f;
                    s->properties.limits.maxStorageBufferRange = 134217728;
                    s->properties.limits.maxTexelBufferElements = 65536;
                    s->properties.limits.maxTexelOffset = 7;
                    s->properties.limits.maxUniformBufferRange = 16384;
                    s->properties.limits.maxVertexInputAttributeOffset = 2047;
                    s->properties.limits.maxVertexInputAttributes = 16;
                    s->properties.limits.maxVertexInputBindingStride = 2048;
                    s->properties.limits.maxVertexInputBindings = 16;
                    s->properties.limits.maxVertexOutputComponents = 64;
                    s->properties.limits.maxViewportDimensions[0] = 4096;
                    s->properties.limits.maxViewportDimensions[1] = 4096;
                    s->properties.limits.maxViewports = 1;
                    s->properties.limits.minInterpolationOffset = -0.5f;
                    s->properties.limits.minMemoryMapAlignment = 4096;
                    s->properties.limits.minStorageBufferOffsetAlignment = 256;
                    s->properties.limits.minTexelBufferOffsetAlignment = 256;
                    s->properties.limits.minTexelOffset = -8;
                    s->properties.limits.minUniformBufferOffsetAlignment = 256;
                    s->properties.limits.mipmapPrecisionBits = 4;
                    s->properties.limits.sampledImageColorSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.sampledImageDepthSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.sampledImageIntegerSampleCounts = (VK_SAMPLE_COUNT_1_BIT);
                    s->properties.limits.sampledImageStencilSampleCounts = (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT);
                    s->properties.limits.standardSampleLocations = VK_TRUE;
                    s->properties.limits.storageImageSampleCounts = (VK_SAMPLE_COUNT_1_BIT);
                    s->properties.limits.subPixelInterpolationOffsetBits = 4;
                    s->properties.limits.subPixelPrecisionBits = 4;
                    s->properties.limits.subTexelPrecisionBits = 4;
                    s->properties.limits.viewportBoundsRange[0] = -8192;
                    s->properties.limits.viewportBoundsRange[1] = 8191;
                } break;
                default: break;
            }
    },
    [](VkBaseOutStructure* p) -> bool {
        bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR: {
                    VkPhysicalDeviceProperties2KHR* s = static_cast<VkPhysicalDeviceProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (s->properties.limits.discreteQueuePriorities >= 2);
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferColorSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferDepthSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferNoAttachmentsSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.framebufferStencilSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (s->properties.limits.maxBoundDescriptorSets >= 4);
                    ret = ret && (s->properties.limits.maxColorAttachments >= 4);
                    ret = ret && (s->properties.limits.maxComputeSharedMemorySize >= 16384);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupCount[0] >= 65535);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupCount[1] >= 65535);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupCount[2] >= 65535);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupInvocations >= 128);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupSize[0] >= 128);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupSize[1] >= 128);
                    ret = ret && (s->properties.limits.maxComputeWorkGroupSize[2] >= 64);
                    ret = ret && (s->properties.limits.maxDescriptorSetInputAttachments >= 4);
                    ret = ret && (s->properties.limits.maxDescriptorSetSampledImages >= 48);
                    ret = ret && (s->properties.limits.maxDescriptorSetSamplers >= 48);
                    ret = ret && (s->properties.limits.maxDescriptorSetStorageBuffers >= 24);
                    ret = ret && (s->properties.limits.maxDescriptorSetStorageBuffersDynamic >= 4);
                    ret = ret && (s->properties.limits.maxDescriptorSetStorageImages >= 12);
                    ret = ret && (s->properties.limits.maxDescriptorSetUniformBuffers >= 36);
                    ret = ret && (s->properties.limits.maxDescriptorSetUniformBuffersDynamic >= 8);
                    ret = ret && (s->properties.limits.maxDrawIndexedIndexValue >= 4294967295);
                    ret = ret && (s->properties.limits.maxDrawIndirectCount >= 1);
                    ret = ret && (s->properties.limits.maxFragmentCombinedOutputResources >= 8);
                    ret = ret && (s->properties.limits.maxFragmentInputComponents >= 64);
                    ret = ret && (s->properties.limits.maxFragmentOutputAttachments >= 4);
                    ret = ret && (s->properties.limits.maxFramebufferHeight >= 4096);
                    ret = ret && (s->properties.limits.maxFramebufferLayers >= 256);
                    ret = ret && (s->properties.limits.maxFramebufferWidth >= 4096);
                    ret = ret && (s->properties.limits.maxImageArrayLayers >= 256);
                    ret = ret && (s->properties.limits.maxImageDimension1D >= 4096);
                    ret = ret && (s->properties.limits.maxImageDimension2D >= 4096);
                    ret = ret && (s->properties.limits.maxImageDimension3D >= 512);
                    ret = ret && (s->properties.limits.maxImageDimensionCube >= 4096);
                    ret = ret && (s->properties.limits.maxInterpolationOffset >= 0.4375);
                    ret = ret && (s->properties.limits.maxMemoryAllocationCount >= 4096);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorInputAttachments >= 4);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorSampledImages >= 16);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorSamplers >= 16);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorStorageBuffers >= 4);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorStorageImages >= 4);
                    ret = ret && (s->properties.limits.maxPerStageDescriptorUniformBuffers >= 12);
                    ret = ret && (s->properties.limits.maxPerStageResources >= 44);
                    ret = ret && (s->properties.limits.maxPushConstantsSize >= 128);
                    ret = ret && (s->properties.limits.maxSampleMaskWords >= 1);
                    ret = ret && (s->properties.limits.maxSamplerAllocationCount >= 4000);
                    ret = ret && (s->properties.limits.maxSamplerAnisotropy >= 1.0);
                    ret = ret && (s->properties.limits.maxSamplerLodBias >= 2.0);
                    ret = ret && (s->properties.limits.maxStorageBufferRange >= 134217728);
                    ret = ret && (s->properties.limits.maxTexelBufferElements >= 65536);
                    ret = ret && (s->properties.limits.maxTexelOffset >= 7);
                    ret = ret && (s->properties.limits.maxUniformBufferRange >= 16384);
                    ret = ret && (s->properties.limits.maxVertexInputAttributeOffset >= 2047);
                    ret = ret && (s->properties.limits.maxVertexInputAttributes >= 16);
                    ret = ret && (s->properties.limits.maxVertexInputBindingStride >= 2048);
                    ret = ret && (s->properties.limits.maxVertexInputBindings >= 16);
                    ret = ret && (s->properties.limits.maxVertexOutputComponents >= 64);
                    ret = ret && (s->properties.limits.maxViewportDimensions[0] >= 4096);
                    ret = ret && (s->properties.limits.maxViewportDimensions[1] >= 4096);
                    ret = ret && (s->properties.limits.maxViewports >= 1);
                    ret = ret && (s->properties.limits.minInterpolationOffset <= -0.5);
                    ret = ret && (s->properties.limits.minMemoryMapAlignment <= 4096);
                    ret = ret && ((s->properties.limits.minMemoryMapAlignment & (s->properties.limits.minMemoryMapAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.minStorageBufferOffsetAlignment <= 256);
                    ret = ret && ((s->properties.limits.minStorageBufferOffsetAlignment & (s->properties.limits.minStorageBufferOffsetAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.minTexelBufferOffsetAlignment <= 256);
                    ret = ret && ((s->properties.limits.minTexelBufferOffsetAlignment & (s->properties.limits.minTexelBufferOffsetAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.minTexelOffset <= -8);
                    ret = ret && (s->properties.limits.minUniformBufferOffsetAlignment <= 256);
                    ret = ret && ((s->properties.limits.minUniformBufferOffsetAlignment & (s->properties.limits.minUniformBufferOffsetAlignment - 1)) == 0);
                    ret = ret && (s->properties.limits.mipmapPrecisionBits >= 4);
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageColorSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageDepthSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageIntegerSampleCounts, (VK_SAMPLE_COUNT_1_BIT)));
                    ret = ret && (vpCheckFlags(s->properties.limits.sampledImageStencilSampleCounts, (VK_SAMPLE_COUNT_1_BIT | VK_SAMPLE_COUNT_4_BIT)));
                    ret = ret && (s->properties.limits.standardSampleLocations == VK_TRUE);
                    ret = ret && (vpCheckFlags(s->properties.limits.storageImageSampleCounts, (VK_SAMPLE_COUNT_1_BIT)));
                    ret = ret && (s->properties.limits.subPixelInterpolationOffsetBits >= 4);
                    ret = ret && (s->properties.limits.subPixelPrecisionBits >= 4);
                    ret = ret && (s->properties.limits.subTexelPrecisionBits >= 4);
                    ret = ret && (s->properties.limits.viewportBoundsRange[0] <= -8192);
                    ret = ret && (s->properties.limits.viewportBoundsRange[1] >= 8191);
                } break;
                default: break;
            }
        return ret;
    },
    nullptr
};

static const VpFormatDesc formatDesc[] = {
    {
        VK_FORMAT_A1R5G5B5_UNORM_PACK16,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A2B10G10R10_UINT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A2B10G10R10_UNORM_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_SINT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_SNORM_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_SRGB_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_UINT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_A8B8G8R8_UNORM_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x10_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x10_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x6_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x6_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_10x8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x10_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x10_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x12_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_12x12_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_4x4_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_4x4_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x4_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x4_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_5x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x6_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_6x6_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x5_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x5_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x6_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x6_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ASTC_8x8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B10G11R11_UFLOAT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B4G4R4A4_UNORM_PACK16,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B8G8R8A8_SRGB,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_B8G8R8A8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_D16_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_D32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_E5B9G9R9_UFLOAT_PACK32,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11G11_SNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11G11_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11_SNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_EAC_R11_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A1_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A1_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8A8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8_SRGB_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16B16A16_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16G16_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R16_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32B32A32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32B32A32_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32B32A32_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32G32_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32_SFLOAT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R32_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R5G6B5_UNORM_PACK16,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_SRGB,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8B8A8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8G8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_SINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_SNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_UINT,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
    {
        VK_FORMAT_R8_UNORM,
        [](VkBaseOutStructure* p) {
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    s->formatProperties.bufferFeatures = (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);
                    s->formatProperties.linearTilingFeatures = (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                    s->formatProperties.optimalTilingFeatures = (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT);
                } break;
                default: break;
            }
        },
        [](VkBaseOutStructure* p) -> bool {
            bool ret = true;
            switch (p->sType) {
                case VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR: {
                    VkFormatProperties2KHR* s = static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p));
                    ret = ret && (vpCheckFlags(s->formatProperties.bufferFeatures, (VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT | VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.linearTilingFeatures, (VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                    ret = ret && (vpCheckFlags(s->formatProperties.optimalTilingFeatures, (VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT | VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT | VK_FORMAT_FEATURE_BLIT_SRC_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT | VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT | VK_FORMAT_FEATURE_TRANSFER_SRC_BIT | VK_FORMAT_FEATURE_TRANSFER_DST_BIT)));
                } break;
                default: break;
            }
            return ret;
        }
    },
};

static const VpStructChainerDesc chainerDesc = {
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        p->pNext = static_cast<VkBaseOutStructure*>(static_cast<void*>(nullptr));
        pfnCb(p, pUser);
    },
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        p->pNext = static_cast<VkBaseOutStructure*>(static_cast<void*>(nullptr));
        pfnCb(p, pUser);
    },
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        pfnCb(p, pUser);
    },
    [](VkBaseOutStructure* p, void* pUser, PFN_vpStructChainerCb pfnCb) {
        VkFormatProperties3KHR formatProperties3KHR;
        formatProperties3KHR.sType = VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3_KHR;
        formatProperties3KHR.pNext = nullptr;
        p->pNext = static_cast<VkBaseOutStructure*>(static_cast<void*>(&formatProperties3KHR));
        pfnCb(p, pUser);
    },
};

} // namespace VP_ANDROID_BASELINE_CPU_ONLY_2021
#endif

static const VpProfileDesc vpProfiles[] = {
#ifdef VP_ANDROID_baseline_2021
    VpProfileDesc{
        VpProfileProperties{ VP_ANDROID_BASELINE_2021_NAME, VP_ANDROID_BASELINE_2021_SPEC_VERSION },
        VP_ANDROID_BASELINE_2021_MIN_API_VERSION,
        &VP_ANDROID_BASELINE_2021::instanceExtensions[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_2021::instanceExtensions) / sizeof(VP_ANDROID_BASELINE_2021::instanceExtensions[0])),
        &VP_ANDROID_BASELINE_2021::deviceExtensions[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_2021::deviceExtensions) / sizeof(VP_ANDROID_BASELINE_2021::deviceExtensions[0])),
        nullptr, 0,
        &VP_ANDROID_BASELINE_2021::featureStructTypes[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_2021::featureStructTypes) / sizeof(VP_ANDROID_BASELINE_2021::featureStructTypes[0])),
        VP_ANDROID_BASELINE_2021::featureDesc,
        &VP_ANDROID_BASELINE_2021::propertyStructTypes[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_2021::propertyStructTypes) / sizeof(VP_ANDROID_BASELINE_2021::propertyStructTypes[0])),
        VP_ANDROID_BASELINE_2021::propertyDesc,
        nullptr, 0,
        nullptr, 0,
        &VP_ANDROID_BASELINE_2021::formatStructTypes[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_2021::formatStructTypes) / sizeof(VP_ANDROID_BASELINE_2021::formatStructTypes[0])),
        &VP_ANDROID_BASELINE_2021::formatDesc[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_2021::formatDesc) / sizeof(VP_ANDROID_BASELINE_2021::formatDesc[0])),
        VP_ANDROID_BASELINE_2021::chainerDesc,
    },
#endif
#ifdef VP_ANDROID_baseline_cpu_only_2021
    VpProfileDesc{
        VpProfileProperties{ VP_ANDROID_BASELINE_CPU_ONLY_2021_NAME, VP_ANDROID_BASELINE_CPU_ONLY_2021_SPEC_VERSION },
        VP_ANDROID_BASELINE_CPU_ONLY_2021_MIN_API_VERSION,
        &VP_ANDROID_BASELINE_CPU_ONLY_2021::instanceExtensions[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::instanceExtensions) / sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::instanceExtensions[0])),
        &VP_ANDROID_BASELINE_CPU_ONLY_2021::deviceExtensions[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::deviceExtensions) / sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::deviceExtensions[0])),
        nullptr, 0,
        &VP_ANDROID_BASELINE_CPU_ONLY_2021::featureStructTypes[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::featureStructTypes) / sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::featureStructTypes[0])),
        VP_ANDROID_BASELINE_CPU_ONLY_2021::featureDesc,
        &VP_ANDROID_BASELINE_CPU_ONLY_2021::propertyStructTypes[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::propertyStructTypes) / sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::propertyStructTypes[0])),
        VP_ANDROID_BASELINE_CPU_ONLY_2021::propertyDesc,
        nullptr, 0,
        nullptr, 0,
        &VP_ANDROID_BASELINE_CPU_ONLY_2021::formatStructTypes[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::formatStructTypes) / sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::formatStructTypes[0])),
        &VP_ANDROID_BASELINE_CPU_ONLY_2021::formatDesc[0], static_cast<uint32_t>(sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::formatDesc) / sizeof(VP_ANDROID_BASELINE_CPU_ONLY_2021::formatDesc[0])),
        VP_ANDROID_BASELINE_CPU_ONLY_2021::chainerDesc,
    },
#endif
};
static const uint32_t vpProfileCount = static_cast<uint32_t>(sizeof(vpProfiles) / sizeof(vpProfiles[0]));

VPAPI_ATTR const VpProfileDesc* vpGetProfileDesc(const char profileName[VP_MAX_PROFILE_NAME_SIZE]) {
    for (uint32_t i = 0; i < vpProfileCount; ++i) {
        if (strncmp(vpProfiles[i].props.profileName, profileName, VP_MAX_PROFILE_NAME_SIZE) == 0) return &vpProfiles[i];
    }
    return nullptr;
}

VPAPI_ATTR bool vpCheckVersion(uint32_t actual, uint32_t expected) {
    uint32_t actualMajor = VK_API_VERSION_MAJOR(actual);
    uint32_t actualMinor = VK_API_VERSION_MINOR(actual);
    uint32_t expectedMajor = VK_API_VERSION_MAJOR(expected);
    uint32_t expectedMinor = VK_API_VERSION_MINOR(expected);
    return actualMajor > expectedMajor || (actualMajor == expectedMajor && actualMinor >= expectedMinor);
}

VPAPI_ATTR bool vpCheckExtension(const VkExtensionProperties *supportedProperties, size_t supportedSize,
                                 const char *requestedExtension) {
    bool found = false;
    for (size_t i = 0, n = supportedSize; i < n; ++i) {
        if (strcmp(supportedProperties[i].extensionName, requestedExtension) == 0) {
            found = true;
            // Drivers don't actually update their spec version, so we cannot rely on this
            // if (supportedProperties[i].specVersion >= expectedVersion) found = true;
        }
    }
    return found;
}

VPAPI_ATTR void vpGetExtensions(uint32_t requestedExtensionCount, const char *const *ppRequestedExtensionNames,
                                uint32_t profileExtensionCount, const VkExtensionProperties *pProfileExtensionProperties,
                                std::vector<const char *> &extensions, bool merge, bool override) {
    if (override) {
        for (uint32_t i = 0; i < requestedExtensionCount; ++i) {
            extensions.push_back(ppRequestedExtensionNames[i]);
        }
    } else {
        for (uint32_t i = 0; i < profileExtensionCount; ++i) {
            extensions.push_back(pProfileExtensionProperties[i].extensionName);
        }

        if (merge) {
            for (uint32_t i = 0; i < requestedExtensionCount; ++i) {
                if (vpCheckExtension(pProfileExtensionProperties, profileExtensionCount, ppRequestedExtensionNames[i])) {
                    continue;
                }
                extensions.push_back(ppRequestedExtensionNames[i]);
            }
        }
    }
}

VPAPI_ATTR const void* vpGetStructure(const void* pNext, VkStructureType type) {
    const VkBaseOutStructure *p = static_cast<const VkBaseOutStructure*>(pNext);
    while (p != nullptr) {
        if (p->sType == type) return p;
        p = p->pNext;
    }
    return nullptr;
}

VPAPI_ATTR void* vpGetStructure(void* pNext, VkStructureType type) {
    VkBaseOutStructure *p = static_cast<VkBaseOutStructure*>(pNext);
    while (p != nullptr) {
        if (p->sType == type) return p;
        p = p->pNext;
    }
    return nullptr;
}

} // namespace detail

VPAPI_ATTR VkResult vpGetProfiles(uint32_t *pPropertyCount, VpProfileProperties *pProperties) {
    VkResult result = VK_SUCCESS;

    if (pProperties == nullptr) {
        *pPropertyCount = detail::vpProfileCount;
    } else {
        if (*pPropertyCount < detail::vpProfileCount) {
            result = VK_INCOMPLETE;
        } else {
            *pPropertyCount = detail::vpProfileCount;
        }
        for (uint32_t i = 0; i < *pPropertyCount; ++i) {
            pProperties[i] = detail::vpProfiles[i].props;
        }
    }
    return result;
}

VPAPI_ATTR VkResult vpGetProfileFallbacks(const VpProfileProperties *pProfile, uint32_t *pPropertyCount, VpProfileProperties *pProperties) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pProperties == nullptr) {
        *pPropertyCount = pDesc->fallbackCount;
    } else {
        if (*pPropertyCount < pDesc->fallbackCount) {
            result = VK_INCOMPLETE;
        } else {
            *pPropertyCount = pDesc->fallbackCount;
        }
        for (uint32_t i = 0; i < *pPropertyCount; ++i) {
            pProperties[i] = pDesc->pFallbacks[i];
        }
    }
    return result;
}

VPAPI_ATTR VkResult vpGetInstanceProfileSupport(const char *pLayerName, const VpProfileProperties *pProfile, VkBool32 *pSupported) {
    VkResult result = VK_SUCCESS;

    uint32_t apiVersion = VK_MAKE_VERSION(1, 0, 0);
    static PFN_vkEnumerateInstanceVersion pfnEnumerateInstanceVersion =
        (PFN_vkEnumerateInstanceVersion)vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion");
    if (pfnEnumerateInstanceVersion != nullptr) {
        result = pfnEnumerateInstanceVersion(&apiVersion);
        if (result != VK_SUCCESS) {
            return result;
        }
    }

    uint32_t extCount = 0;
    result = vkEnumerateInstanceExtensionProperties(pLayerName, &extCount, nullptr);
    if (result != VK_SUCCESS) {
        return result;
    }
    std::vector<VkExtensionProperties> ext(extCount);
    result = vkEnumerateInstanceExtensionProperties(pLayerName, &extCount, ext.data());
    if (result != VK_SUCCESS) {
        return result;
    }

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    *pSupported = VK_TRUE;

    if (pDesc->props.specVersion < pProfile->specVersion) {
        *pSupported = VK_FALSE;
    }

    if (!detail::vpCheckVersion(apiVersion, pDesc->minApiVersion)) {
        *pSupported = VK_FALSE;
    }

    for (uint32_t i = 0; i < pDesc->instanceExtensionCount; ++i) {
        if (!detail::vpCheckExtension(ext.data(), ext.size(),
            pDesc->pInstanceExtensions[i].extensionName)) {
            *pSupported = VK_FALSE;
        }
    }

    // We require VK_KHR_get_physical_device_properties2 if we are on Vulkan 1.0
    if (apiVersion < VK_API_VERSION_1_1) {
        bool foundGPDP2 = false;
        for (size_t i = 0; i < ext.size(); ++i) {
            if (strcmp(ext[i].extensionName, VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME) == 0) {
                foundGPDP2 = true;
                break;
            }
        }
        if (!foundGPDP2) {
            *pSupported = VK_FALSE;
        }
    }

    return result;
}

VPAPI_ATTR VkResult vpCreateInstance(const VpInstanceCreateInfo *pCreateInfo,
                                     const VkAllocationCallbacks *pAllocator, VkInstance *pInstance) {
    VkInstanceCreateInfo createInfo;
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    VkApplicationInfo appInfo;
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    std::vector<const char*> extensions;
    VkInstanceCreateInfo* pInstanceCreateInfo = nullptr;

    if (pCreateInfo != nullptr && pCreateInfo->pCreateInfo != nullptr) {
        createInfo = *pCreateInfo->pCreateInfo;
        pInstanceCreateInfo = &createInfo;

        const detail::VpProfileDesc* pDesc = nullptr;
        if (pCreateInfo->pProfile != nullptr) {
            pDesc = detail::vpGetProfileDesc(pCreateInfo->pProfile->profileName);
            if (pDesc == nullptr) return VK_ERROR_UNKNOWN;
        }

        if (createInfo.pApplicationInfo == nullptr) {
            appInfo.apiVersion = pDesc->minApiVersion;
            createInfo.pApplicationInfo = &appInfo;
        }

        if (pDesc != nullptr && pDesc->pInstanceExtensions != nullptr) {
            bool merge = (pCreateInfo->flags & VP_INSTANCE_CREATE_MERGE_EXTENSIONS_BIT) != 0;
            bool override = (pCreateInfo->flags & VP_INSTANCE_CREATE_OVERRIDE_EXTENSIONS_BIT) != 0;

            if (!merge && !override && pCreateInfo->pCreateInfo->enabledExtensionCount > 0) {
                // If neither merge nor override is used then the application must not specify its
                // own extensions
                return VK_ERROR_UNKNOWN;
            }

            detail::vpGetExtensions(pCreateInfo->pCreateInfo->enabledExtensionCount,
                                    pCreateInfo->pCreateInfo->ppEnabledExtensionNames,
                                    pDesc->instanceExtensionCount,
                                    pDesc->pInstanceExtensions,
                                    extensions, merge, override);
            {
                bool foundPortEnum = false;
                for (size_t i = 0; i < extensions.size(); ++i) {
                    if (strcmp(extensions[i], VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME) == 0) {
                        foundPortEnum = true;
                        break;
                    }
                }
                if (foundPortEnum) {
                    createInfo.flags |= VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
                }
            }

            // Need to include VK_KHR_get_physical_device_properties2 if we are on Vulkan 1.0
            if (createInfo.pApplicationInfo->apiVersion < VK_API_VERSION_1_1) {
                bool foundGPDP2 = false;
                for (size_t i = 0; i < extensions.size(); ++i) {
                    if (strcmp(extensions[i], VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME) == 0) {
                        foundGPDP2 = true;
                        break;
                    }
                }
                if (!foundGPDP2) {
                    extensions.push_back(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
                }
            }

            createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
            createInfo.ppEnabledExtensionNames = extensions.data();
        }
    }

    return vkCreateInstance(pInstanceCreateInfo, pAllocator, pInstance);
}

VPAPI_ATTR VkResult vpGetPhysicalDeviceProfileSupport(VkInstance instance, VkPhysicalDevice physicalDevice,
                                                      const VpProfileProperties *pProfile, VkBool32 *pSupported) {
    VkResult result = VK_SUCCESS;

    uint32_t extCount = 0;
    result = vkEnumerateDeviceExtensionProperties(physicalDevice, nullptr, &extCount, nullptr);
    if (result != VK_SUCCESS) {
        return result;
    }
    std::vector<VkExtensionProperties> ext;
    if (extCount > 0) {
        ext.resize(extCount);
    }
    result = vkEnumerateDeviceExtensionProperties(physicalDevice, nullptr, &extCount, ext.data());
    if (result != VK_SUCCESS) {
        return result;
    }

    // Workaround old loader bug where count could be smaller on the second call to vkEnumerateDeviceExtensionProperties
    if (extCount > 0) {
        ext.resize(extCount);
    }

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    struct GPDP2EntryPoints {
        PFN_vkGetPhysicalDeviceFeatures2KHR                 pfnGetPhysicalDeviceFeatures2;
        PFN_vkGetPhysicalDeviceProperties2KHR               pfnGetPhysicalDeviceProperties2;
        PFN_vkGetPhysicalDeviceFormatProperties2KHR         pfnGetPhysicalDeviceFormatProperties2;
        PFN_vkGetPhysicalDeviceQueueFamilyProperties2KHR    pfnGetPhysicalDeviceQueueFamilyProperties2;
    };

    struct UserData {
        VkPhysicalDevice                    physicalDevice;
        const detail::VpProfileDesc*        pDesc;
        GPDP2EntryPoints                    gpdp2;
        uint32_t                            index;
        uint32_t                            count;
        detail::PFN_vpStructChainerCb       pfnCb;
        bool                                supported;
    } userData;
    userData.physicalDevice = physicalDevice;
    userData.pDesc = pDesc;

    // Attempt to load core versions of the GPDP2 entry points
    userData.gpdp2.pfnGetPhysicalDeviceFeatures2 =
        (PFN_vkGetPhysicalDeviceFeatures2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFeatures2");
    userData.gpdp2.pfnGetPhysicalDeviceProperties2 =
        (PFN_vkGetPhysicalDeviceProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceProperties2");
    userData.gpdp2.pfnGetPhysicalDeviceFormatProperties2 =
        (PFN_vkGetPhysicalDeviceFormatProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFormatProperties2");
    userData.gpdp2.pfnGetPhysicalDeviceQueueFamilyProperties2 =
        (PFN_vkGetPhysicalDeviceQueueFamilyProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceQueueFamilyProperties2");

    // If not successful, try to load KHR variant
    if (userData.gpdp2.pfnGetPhysicalDeviceFeatures2 == nullptr) {
        userData.gpdp2.pfnGetPhysicalDeviceFeatures2 =
            (PFN_vkGetPhysicalDeviceFeatures2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFeatures2KHR");
        userData.gpdp2.pfnGetPhysicalDeviceProperties2 =
            (PFN_vkGetPhysicalDeviceProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceProperties2KHR");
        userData.gpdp2.pfnGetPhysicalDeviceFormatProperties2 =
            (PFN_vkGetPhysicalDeviceFormatProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFormatProperties2KHR");
        userData.gpdp2.pfnGetPhysicalDeviceQueueFamilyProperties2 =
            (PFN_vkGetPhysicalDeviceQueueFamilyProperties2KHR)vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceQueueFamilyProperties2KHR");
    }

    if (userData.gpdp2.pfnGetPhysicalDeviceFeatures2 == nullptr ||
        userData.gpdp2.pfnGetPhysicalDeviceProperties2 == nullptr ||
        userData.gpdp2.pfnGetPhysicalDeviceFormatProperties2 == nullptr ||
        userData.gpdp2.pfnGetPhysicalDeviceQueueFamilyProperties2 == nullptr) {
        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    *pSupported = VK_TRUE;

    if (pDesc->props.specVersion < pProfile->specVersion) {
        *pSupported = VK_FALSE;
    }

    {
        VkPhysicalDeviceProperties props{};
        vkGetPhysicalDeviceProperties(physicalDevice, &props);
        if (!detail::vpCheckVersion(props.apiVersion, pDesc->minApiVersion)) {
            *pSupported = VK_FALSE;
        }
    }

    for (uint32_t i = 0; i < pDesc->deviceExtensionCount; ++i) {
        if (!detail::vpCheckExtension(ext.data(), ext.size(),
            pDesc->pDeviceExtensions[i].extensionName)) {
            *pSupported = VK_FALSE;
        }
    }

    {
        VkPhysicalDeviceFeatures2KHR features;
        features.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR;
        pDesc->chainers.pfnFeature(static_cast<VkBaseOutStructure*>(static_cast<void*>(&features)), &userData,
            [](VkBaseOutStructure* p, void* pUser) {
                UserData* pUserData = static_cast<UserData*>(pUser);
                pUserData->gpdp2.pfnGetPhysicalDeviceFeatures2(pUserData->physicalDevice,
                                                               static_cast<VkPhysicalDeviceFeatures2KHR*>(static_cast<void*>(p)));
                pUserData->supported = true;
                while (p != nullptr) {
                    if (!pUserData->pDesc->feature.pfnComparator(p)) {
                        pUserData->supported = false;
                    }
                    p = p->pNext;
                }
            }
        );
        if (!userData.supported) {
            *pSupported = VK_FALSE;
        }
    }

    {
        VkPhysicalDeviceProperties2KHR props;
        props.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR;
        pDesc->chainers.pfnProperty(static_cast<VkBaseOutStructure*>(static_cast<void*>(&props)), &userData,
            [](VkBaseOutStructure* p, void* pUser) {
                UserData* pUserData = static_cast<UserData*>(pUser);
                pUserData->gpdp2.pfnGetPhysicalDeviceProperties2(pUserData->physicalDevice,
                                                                 static_cast<VkPhysicalDeviceProperties2KHR*>(static_cast<void*>(p)));
                pUserData->supported = true;
                while (p != nullptr) {
                    if (!pUserData->pDesc->property.pfnComparator(p)) {
                        pUserData->supported = false;
                    }
                    p = p->pNext;
                }
            }
        );
        if (!userData.supported) {
            *pSupported = VK_FALSE;
        }
    }

    for (uint32_t i = 0; i < pDesc->formatCount; ++i) {
        userData.index = i;
        VkFormatProperties2KHR props;
        props.sType = VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR;
        pDesc->chainers.pfnFormat(static_cast<VkBaseOutStructure*>(static_cast<void*>(&props)), &userData,
            [](VkBaseOutStructure* p, void* pUser) {
                UserData* pUserData = static_cast<UserData*>(pUser);
                pUserData->gpdp2.pfnGetPhysicalDeviceFormatProperties2(pUserData->physicalDevice,
                                                                       pUserData->pDesc->pFormats[pUserData->index].format,
                                                                       static_cast<VkFormatProperties2KHR*>(static_cast<void*>(p)));
                pUserData->supported = true;
                while (p != nullptr) {
                    if (!pUserData->pDesc->pFormats[pUserData->index].pfnComparator(p)) {
                        pUserData->supported = false;
                    }
                    p = p->pNext;
                }
            }
        );
        if (!userData.supported) {
            *pSupported = VK_FALSE;
        }
    }

    {
        userData.gpdp2.pfnGetPhysicalDeviceQueueFamilyProperties2(physicalDevice, &userData.count, nullptr);
        VkQueueFamilyProperties2KHR property;
        property.sType = VK_STRUCTURE_TYPE_QUEUE_FAMILY_PROPERTIES_2_KHR;
        std::vector<VkQueueFamilyProperties2KHR> props(userData.count, property);
        userData.index = 0;

        detail::PFN_vpStructChainerCb callback = [](VkBaseOutStructure* p, void* pUser) {
            UserData* pUserData = static_cast<UserData*>(pUser);
            VkQueueFamilyProperties2KHR* pProps = static_cast<VkQueueFamilyProperties2KHR*>(static_cast<void*>(p));
            if (++pUserData->index < pUserData->count) {
                pUserData->pDesc->chainers.pfnQueueFamily(static_cast<VkBaseOutStructure*>(static_cast<void*>(++pProps)),
                                                          pUser, pUserData->pfnCb);
            } else {
                pProps -= pUserData->count - 1;
                pUserData->gpdp2.pfnGetPhysicalDeviceQueueFamilyProperties2(pUserData->physicalDevice,
                                                                            &pUserData->count,
                                                                            pProps);
                pUserData->supported = true;

                // Check first that each queue family defined is supported by the device
                for (uint32_t i = 0; i < pUserData->pDesc->queueFamilyCount; ++i) {
                    bool found = false;
                    for (uint32_t j = 0; j < pUserData->count; ++j) {
                        bool propsMatch = true;
                        p = static_cast<VkBaseOutStructure*>(static_cast<void*>(&pProps[j]));
                        while (p != nullptr) {
                            if (!pUserData->pDesc->pQueueFamilies[i].pfnComparator(p)) {
                                propsMatch = false;
                                break;
                            }
                            p = p->pNext;
                        }
                        if (propsMatch) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        pUserData->supported = false;
                        return;
                    }
                }

                // Then check each permutation to ensure that while order of the queue families
                // doesn't matter, each queue family property criteria is matched with a separate
                // queue family of the actual device
                std::vector<uint32_t> permutation(pUserData->count);
                for (uint32_t i = 0; i < pUserData->count; ++i) {
                    permutation[i] = i;
                }
                bool found = false;
                do {
                    bool propsMatch = true;
                    for (uint32_t i = 0; i < pUserData->pDesc->queueFamilyCount && propsMatch; ++i) {
                        p = static_cast<VkBaseOutStructure*>(static_cast<void*>(&pProps[permutation[i]]));
                        while (p != nullptr) {
                            if (!pUserData->pDesc->pQueueFamilies[i].pfnComparator(p)) {
                                propsMatch = false;
                                break;
                            }
                            p = p->pNext;
                        }
                    }
                    if (propsMatch) {
                        found = true;
                        break;
                    }
                } while (std::next_permutation(permutation.begin(), permutation.end()));

                if (!found) {
                    pUserData->supported = false;
                }
            }
        };
        userData.pfnCb = callback;

        if (userData.count >= userData.pDesc->queueFamilyCount) {
            pDesc->chainers.pfnQueueFamily(static_cast<VkBaseOutStructure*>(static_cast<void*>(props.data())), &userData, callback);
            if (!userData.supported) {
                *pSupported = VK_FALSE;
            }
        } else {
            *pSupported = VK_FALSE;
        }
    }

    return result;
}

VPAPI_ATTR VkResult vpCreateDevice(VkPhysicalDevice physicalDevice, const VpDeviceCreateInfo *pCreateInfo,
                                   const VkAllocationCallbacks *pAllocator, VkDevice *pDevice) {
    if (physicalDevice == VK_NULL_HANDLE || pCreateInfo == nullptr || pDevice == nullptr) {
        return vkCreateDevice(physicalDevice, pCreateInfo == nullptr ? nullptr : pCreateInfo->pCreateInfo, pAllocator, pDevice);
    }

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pCreateInfo->pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    struct UserData {
        VkPhysicalDevice                physicalDevice;
        const detail::VpProfileDesc*    pDesc;
        const VpDeviceCreateInfo*       pCreateInfo;
        const VkAllocationCallbacks*    pAllocator;
        VkDevice*                       pDevice;
        VkResult                        result;
    } userData;
    userData.physicalDevice = physicalDevice;
    userData.pDesc = pDesc;
    userData.pCreateInfo = pCreateInfo;
    userData.pAllocator = pAllocator;
    userData.pDevice = pDevice;

    VkPhysicalDeviceFeatures2KHR features;
    features.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR;
    pDesc->chainers.pfnFeature(static_cast<VkBaseOutStructure*>(static_cast<void*>(&features)), &userData,
        [](VkBaseOutStructure* p, void* pUser) {
            UserData* pUserData = static_cast<UserData*>(pUser);
            const detail::VpProfileDesc* pDesc = pUserData->pDesc;
            const VpDeviceCreateInfo* pCreateInfo = pUserData->pCreateInfo;

            bool merge = (pCreateInfo->flags & VP_DEVICE_CREATE_MERGE_EXTENSIONS_BIT) != 0;
            bool override = (pCreateInfo->flags & VP_DEVICE_CREATE_OVERRIDE_EXTENSIONS_BIT) != 0;

            if (!merge && !override && pCreateInfo->pCreateInfo->enabledExtensionCount > 0) {
                // If neither merge nor override is used then the application must not specify its
                // own extensions
                pUserData->result = VK_ERROR_UNKNOWN;
                return;
            }

            std::vector<const char*> extensions;
            detail::vpGetExtensions(pCreateInfo->pCreateInfo->enabledExtensionCount,
                                    pCreateInfo->pCreateInfo->ppEnabledExtensionNames,
                                    pDesc->deviceExtensionCount,
                                    pDesc->pDeviceExtensions,
                                    extensions, merge, override);

            VkBaseOutStructure profileStructList;
            profileStructList.pNext = p;
            VkPhysicalDeviceFeatures2KHR* pFeatures = static_cast<VkPhysicalDeviceFeatures2KHR*>(static_cast<void*>(p));
            if (pDesc->feature.pfnFiller != nullptr) {
                while (p != nullptr) {
                    pDesc->feature.pfnFiller(p);
                    p = p->pNext;
                }
            }

            if (pCreateInfo->pCreateInfo->pEnabledFeatures != nullptr) {
                pFeatures->features = *pCreateInfo->pCreateInfo->pEnabledFeatures;
            }

            if (pCreateInfo->flags & VP_DEVICE_CREATE_DISABLE_ROBUST_BUFFER_ACCESS_BIT) {
                pFeatures->features.robustBufferAccess = VK_FALSE;
            }

#ifdef VK_EXT_robustness2
            VkPhysicalDeviceRobustness2FeaturesEXT* pRobustness2FeaturesEXT = static_cast<VkPhysicalDeviceRobustness2FeaturesEXT*>(
                detail::vpGetStructure(pFeatures, VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ROBUSTNESS_2_FEATURES_EXT));
            if (pRobustness2FeaturesEXT != nullptr) {
                if (pCreateInfo->flags & VP_DEVICE_CREATE_DISABLE_ROBUST_BUFFER_ACCESS_BIT) {
                    pRobustness2FeaturesEXT->robustBufferAccess2 = VK_FALSE;
                }
                if (pCreateInfo->flags & VP_DEVICE_CREATE_DISABLE_ROBUST_IMAGE_ACCESS_BIT) {
                    pRobustness2FeaturesEXT->robustImageAccess2 = VK_FALSE;
                }
            }
#endif

#ifdef VK_EXT_image_robustness
            VkPhysicalDeviceImageRobustnessFeaturesEXT* pImageRobustnessFeaturesEXT = static_cast<VkPhysicalDeviceImageRobustnessFeaturesEXT*>(
                detail::vpGetStructure(pFeatures, VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_ROBUSTNESS_FEATURES_EXT));
            if (pImageRobustnessFeaturesEXT != nullptr && (pCreateInfo->flags & VP_DEVICE_CREATE_DISABLE_ROBUST_IMAGE_ACCESS_BIT)) {
                pImageRobustnessFeaturesEXT->robustImageAccess = VK_FALSE;
            }
#endif

#ifdef VK_VERSION_1_3
            VkPhysicalDeviceVulkan13Features* pVulkan13Features = static_cast<VkPhysicalDeviceVulkan13Features*>(
                detail::vpGetStructure(pFeatures, VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES));
            if (pVulkan13Features != nullptr && (pCreateInfo->flags & VP_DEVICE_CREATE_DISABLE_ROBUST_IMAGE_ACCESS_BIT)) {
                pVulkan13Features->robustImageAccess = VK_FALSE;
            }
#endif

            VkBaseOutStructure* pNext = static_cast<VkBaseOutStructure*>(const_cast<void*>(pCreateInfo->pCreateInfo->pNext));
            if ((pCreateInfo->flags & VP_DEVICE_CREATE_OVERRIDE_ALL_FEATURES_BIT) == 0) {
                for (uint32_t i = 0; i < pDesc->featureStructTypeCount; ++i) {
                    const void* pRequested = detail::vpGetStructure(pNext, pDesc->pFeatureStructTypes[i]);
                    if (pRequested == nullptr) {
                        VkBaseOutStructure* pPrevStruct = &profileStructList;
                        VkBaseOutStructure* pCurrStruct = pPrevStruct->pNext;
                        while (pCurrStruct->sType != pDesc->pFeatureStructTypes[i]) {
                            pPrevStruct = pCurrStruct;
                            pCurrStruct = pCurrStruct->pNext;
                        }
                        pPrevStruct->pNext = pCurrStruct->pNext;
                        pCurrStruct->pNext = pNext;
                        pNext = pCurrStruct;
                    } else
                    if ((pCreateInfo->flags & VP_DEVICE_CREATE_OVERRIDE_FEATURES_BIT) == 0) {
                        // If override is not used then the application must not specify its
                        // own feature structure for anything that the profile defines
                        pUserData->result = VK_ERROR_UNKNOWN;
                        return;
                    }
                }
            }

            VkDeviceCreateInfo createInfo;
            createInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
            createInfo.pNext = pNext;
            createInfo.queueCreateInfoCount = pCreateInfo->pCreateInfo->queueCreateInfoCount;
            createInfo.pQueueCreateInfos = pCreateInfo->pCreateInfo->pQueueCreateInfos;
            createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
            createInfo.ppEnabledExtensionNames = extensions.data();
            if (pCreateInfo->flags & VP_DEVICE_CREATE_OVERRIDE_ALL_FEATURES_BIT) {
                createInfo.pEnabledFeatures = pCreateInfo->pCreateInfo->pEnabledFeatures;
            }
            pUserData->result = vkCreateDevice(pUserData->physicalDevice, &createInfo, pUserData->pAllocator, pUserData->pDevice);
        }
    );

    return userData.result;
}

VPAPI_ATTR VkResult vpGetProfileInstanceExtensionProperties(const VpProfileProperties *pProfile, uint32_t *pPropertyCount,
                                                            VkExtensionProperties *pProperties) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pProperties == nullptr) {
        *pPropertyCount = pDesc->instanceExtensionCount;
    } else {
        if (*pPropertyCount < pDesc->instanceExtensionCount) {
            result = VK_INCOMPLETE;
        } else {
            *pPropertyCount = pDesc->instanceExtensionCount;
        }
        for (uint32_t i = 0; i < *pPropertyCount; ++i) {
            pProperties[i] = pDesc->pInstanceExtensions[i];
        }
    }
    return result;
}

VPAPI_ATTR VkResult vpGetProfileDeviceExtensionProperties(const VpProfileProperties *pProfile, uint32_t *pPropertyCount,
                                                          VkExtensionProperties *pProperties) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pProperties == nullptr) {
        *pPropertyCount = pDesc->deviceExtensionCount;
    } else {
        if (*pPropertyCount < pDesc->deviceExtensionCount) {
            result = VK_INCOMPLETE;
        } else {
            *pPropertyCount = pDesc->deviceExtensionCount;
        }
        for (uint32_t i = 0; i < *pPropertyCount; ++i) {
            pProperties[i] = pDesc->pDeviceExtensions[i];
        }
    }
    return result;
}

VPAPI_ATTR void vpGetProfileFeatures(const VpProfileProperties *pProfile, void *pNext) {
    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc != nullptr && pDesc->feature.pfnFiller != nullptr) {
        VkBaseOutStructure* p = static_cast<VkBaseOutStructure*>(pNext);
        while (p != nullptr) {
            pDesc->feature.pfnFiller(p);
            p = p->pNext;
        }
    }
}

VPAPI_ATTR VkResult vpGetProfileFeatureStructureTypes(const VpProfileProperties *pProfile, uint32_t *pStructureTypeCount,
                                                      VkStructureType *pStructureTypes) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pStructureTypes == nullptr) {
        *pStructureTypeCount = pDesc->featureStructTypeCount;
    } else {
        if (*pStructureTypeCount < pDesc->featureStructTypeCount) {
            result = VK_INCOMPLETE;
        } else {
            *pStructureTypeCount = pDesc->featureStructTypeCount;
        }
        for (uint32_t i = 0; i < *pStructureTypeCount; ++i) {
            pStructureTypes[i] = pDesc->pFeatureStructTypes[i];
        }
    }
    return result;
}

VPAPI_ATTR void vpGetProfileProperties(const VpProfileProperties *pProfile, void *pNext) {
    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc != nullptr && pDesc->property.pfnFiller != nullptr) {
        VkBaseOutStructure* p = static_cast<VkBaseOutStructure*>(pNext);
        while (p != nullptr) {
            pDesc->property.pfnFiller(p);
            p = p->pNext;
        }
    }
}

VPAPI_ATTR VkResult vpGetProfilePropertyStructureTypes(const VpProfileProperties *pProfile, uint32_t *pStructureTypeCount,
                                                       VkStructureType *pStructureTypes) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pStructureTypes == nullptr) {
        *pStructureTypeCount = pDesc->propertyStructTypeCount;
    } else {
        if (*pStructureTypeCount < pDesc->propertyStructTypeCount) {
            result = VK_INCOMPLETE;
        } else {
            *pStructureTypeCount = pDesc->propertyStructTypeCount;
        }
        for (uint32_t i = 0; i < *pStructureTypeCount; ++i) {
            pStructureTypes[i] = pDesc->pPropertyStructTypes[i];
        }
    }
    return result;
}

VPAPI_ATTR VkResult vpGetProfileQueueFamilyProperties(const VpProfileProperties *pProfile, uint32_t *pPropertyCount,
                                                      VkQueueFamilyProperties2KHR *pProperties) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pProperties == nullptr) {
        *pPropertyCount = pDesc->queueFamilyCount;
    } else {
        if (*pPropertyCount < pDesc->queueFamilyCount) {
            result = VK_INCOMPLETE;
        } else {
            *pPropertyCount = pDesc->queueFamilyCount;
        }
        for (uint32_t i = 0; i < *pPropertyCount; ++i) {
            VkBaseOutStructure* p = static_cast<VkBaseOutStructure*>(static_cast<void*>(&pProperties[i]));
            while (p != nullptr) {
                pDesc->pQueueFamilies[i].pfnFiller(p);
                p = p->pNext;
            }
        }
    }
    return result;
}

VPAPI_ATTR VkResult vpGetProfileQueueFamilyStructureTypes(const VpProfileProperties *pProfile, uint32_t *pStructureTypeCount,
                                                          VkStructureType *pStructureTypes) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pStructureTypes == nullptr) {
        *pStructureTypeCount = pDesc->queueFamilyStructTypeCount;
    } else {
        if (*pStructureTypeCount < pDesc->queueFamilyStructTypeCount) {
            result = VK_INCOMPLETE;
        } else {
            *pStructureTypeCount = pDesc->queueFamilyStructTypeCount;
        }
        for (uint32_t i = 0; i < *pStructureTypeCount; ++i) {
            pStructureTypes[i] = pDesc->pQueueFamilyStructTypes[i];
        }
    }
    return result;
}

VPAPI_ATTR VkResult vpGetProfileFormats(const VpProfileProperties *pProfile, uint32_t *pFormatCount, VkFormat *pFormats) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pFormats == nullptr) {
        *pFormatCount = pDesc->formatCount;
    } else {
        if (*pFormatCount < pDesc->formatCount) {
            result = VK_INCOMPLETE;
        } else {
            *pFormatCount = pDesc->formatCount;
        }
        for (uint32_t i = 0; i < *pFormatCount; ++i) {
            pFormats[i] = pDesc->pFormats[i].format;
        }
    }
    return result;
}

VPAPI_ATTR void vpGetProfileFormatProperties(const VpProfileProperties *pProfile, VkFormat format, void *pNext) {
    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return;

    for (uint32_t i = 0; i < pDesc->formatCount; ++i) {
        if (pDesc->pFormats[i].format == format) {
            VkBaseOutStructure* p = static_cast<VkBaseOutStructure*>(static_cast<void*>(pNext));
            while (p != nullptr) {
                pDesc->pFormats[i].pfnFiller(p);
                p = p->pNext;
            }
#if defined(VK_VERSION_1_3) || defined(VK_KHR_format_feature_flags2)
            VkFormatProperties2KHR* fp2 = static_cast<VkFormatProperties2KHR*>(
                detail::vpGetStructure(pNext, VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR));
            VkFormatProperties3KHR* fp3 = static_cast<VkFormatProperties3KHR*>(
                detail::vpGetStructure(pNext, VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3_KHR));
            if (fp3 != nullptr) {
                VkFormatProperties2KHR fp;
                fp.sType = VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2_KHR;
                pDesc->pFormats[i].pfnFiller(static_cast<VkBaseOutStructure*>(static_cast<void*>(&fp)));
                fp3->linearTilingFeatures = static_cast<VkFormatFeatureFlags2KHR>(fp3->linearTilingFeatures | fp.formatProperties.linearTilingFeatures);
                fp3->optimalTilingFeatures = static_cast<VkFormatFeatureFlags2KHR>(fp3->optimalTilingFeatures | fp.formatProperties.optimalTilingFeatures);
                fp3->bufferFeatures = static_cast<VkFormatFeatureFlags2KHR>(fp3->bufferFeatures | fp.formatProperties.bufferFeatures);
            }
            if (fp2 != nullptr) {
                VkFormatProperties3KHR fp;
                fp.sType = VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3_KHR;
                pDesc->pFormats[i].pfnFiller(static_cast<VkBaseOutStructure*>(static_cast<void*>(&fp)));
                fp2->formatProperties.linearTilingFeatures = static_cast<VkFormatFeatureFlags>(fp2->formatProperties.linearTilingFeatures | fp.linearTilingFeatures);
                fp2->formatProperties.optimalTilingFeatures = static_cast<VkFormatFeatureFlags>(fp2->formatProperties.optimalTilingFeatures | fp.optimalTilingFeatures);
                fp2->formatProperties.bufferFeatures = static_cast<VkFormatFeatureFlags>(fp2->formatProperties.bufferFeatures | fp.bufferFeatures);
            }
#endif
        }
    }
}

VPAPI_ATTR VkResult vpGetProfileFormatStructureTypes(const VpProfileProperties *pProfile, uint32_t *pStructureTypeCount,
                                                     VkStructureType *pStructureTypes) {
    VkResult result = VK_SUCCESS;

    const detail::VpProfileDesc* pDesc = detail::vpGetProfileDesc(pProfile->profileName);
    if (pDesc == nullptr) return VK_ERROR_UNKNOWN;

    if (pStructureTypes == nullptr) {
        *pStructureTypeCount = pDesc->formatStructTypeCount;
    } else {
        if (*pStructureTypeCount < pDesc->formatStructTypeCount) {
            result = VK_INCOMPLETE;
        } else {
            *pStructureTypeCount = pDesc->formatStructTypeCount;
        }
        for (uint32_t i = 0; i < *pStructureTypeCount; ++i) {
            pStructureTypes[i] = pDesc->pFormatStructTypes[i];
        }
    }
    return result;
}
