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

package com.android.bedstead.harrier.annotations;

import static com.android.bedstead.harrier.annotations.AnnotationRunPrecedence.REQUIRE_RUN_ON_PRECEDENCE;
import static com.android.bedstead.nene.packages.CommonPackages.FEATURE_DEVICE_ADMIN;
import static com.android.bedstead.nene.types.OptionalBoolean.ANY;
import static com.android.bedstead.nene.types.OptionalBoolean.TRUE;

import com.android.bedstead.harrier.annotations.enterprise.AdditionalQueryParameters;
import com.android.bedstead.harrier.annotations.meta.RequireRunOnProfileAnnotation;
import com.android.bedstead.nene.types.OptionalBoolean;
import com.android.queryable.annotations.Query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark that a test method should run within a work profile.
 *
 * <p>Your test configuration should be such that this test is only run where a work profile is
 * created and the test is being run within that user.
 *
 * <p>Optionally, you can guarantee that these methods do not run outside of a work
 * profile by using {@code Devicestate}.
 *
 * <p>This annotation by default opts a test into multi-user presubmit. New tests should also be
 * annotated {@link Postsubmit} until they are shown to meet the multi-user presubmit requirements.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RequireRunOnProfileAnnotation(value = "android.os.usertype.profile.MANAGED",
        hasProfileOwner = true)
@RequireFeature(FEATURE_DEVICE_ADMIN)
public @interface RequireRunOnWorkProfile {
    OptionalBoolean installInstrumentedAppInParent() default ANY;

    String DEFAULT_KEY = "profileOwner";

    /**
     * The key used to identify the profile owner.
     *
     * <p>This can be used with {@link AdditionalQueryParameters} to modify the requirements for
     * the DPC. */
    String dpcKey() default DEFAULT_KEY;

    /**
     * Requirements for the Profile Owner.
     *
     * <p>Defaults to the default version of RemoteDPC.
     */
    Query dpc() default @Query();

    /**
     * Whether the profile owner's DPC should be returned by calls to {@code Devicestate#dpc()}.
     *
     * <p>Only one device policy controller per test should be marked as primary.
     */
    boolean dpcIsPrimary() default false;

    /** Whether the work profile device will be in COPE mode. */
    boolean isOrganizationOwned() default false;

    /**
     * Affiliation ids to be set for the profile owner.
     */
    String[] affiliationIds() default {};

    /**
     * Should we ensure that we are switched to the parent of the profile.
     *
     * <p>ANY will be treated as TRUE if no other annotation has forced a switch.
     */
    OptionalBoolean switchedToParentUser() default TRUE;

    /**
     * Weight sets the order that annotations will be resolved.
     *
     * <p>Annotations with a lower weight will be resolved before annotations with a higher weight.
     *
     * <p>If there is an order requirement between annotations, ensure that the weight of the
     * annotation which must be resolved first is lower than the one which must be resolved later.
     *
     * <p>Weight can be set to a {@link AnnotationRunPrecedence} constant, or to any {@link int}.
     */
    int weight() default REQUIRE_RUN_ON_PRECEDENCE;
}
