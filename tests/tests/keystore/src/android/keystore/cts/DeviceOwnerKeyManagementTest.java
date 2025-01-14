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

package android.keystore.cts;

import static android.app.admin.DevicePolicyManager.ID_TYPE_BASE_INFO;
import static android.app.admin.DevicePolicyManager.ID_TYPE_IMEI;
import static android.app.admin.DevicePolicyManager.ID_TYPE_MEID;
import static android.app.admin.DevicePolicyManager.ID_TYPE_SERIAL;

import static com.google.android.attestation.ParsedAttestationRecord.createParsedAttestationRecord;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.keystore.cts.util.TestUtils;
import android.os.Build;
import android.os.SystemProperties;
import android.security.AttestedKeyPair;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.bedstead.deviceadminapp.DeviceAdminApp;
import com.android.bedstead.harrier.DeviceState;
import com.android.bedstead.harrier.annotations.RequireFeature;
import com.android.bedstead.harrier.annotations.RequireRunOnSystemUser;
import com.android.bedstead.nene.TestApis;
import com.android.bedstead.nene.devicepolicy.DeviceOwner;
import com.android.bedstead.nene.permissions.PermissionContext;
import com.android.compatibility.common.util.ApiTest;

import com.google.android.attestation.ParsedAttestationRecord;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class DeviceOwnerKeyManagementTest {
    private static final Context sContext = TestApis.context().instrumentedContext();
    private static final DevicePolicyManager sDevicePolicyManager =
            sContext.getSystemService(DevicePolicyManager.class);
    private static final ComponentName DEVICE_ADMIN_COMPONENT_NAME =
            DeviceAdminApp.deviceAdminComponentName(sContext);

    @ClassRule @Rule
    public static final DeviceState sDeviceState = new DeviceState();

    private static class SupportedKeyAlgorithm {
        public final String keyAlgorithm;
        public final String signatureAlgorithm;
        public final String[] signaturePaddingSchemes;

        SupportedKeyAlgorithm(
                String keyAlgorithm, String signatureAlgorithm,
                String[] signaturePaddingSchemes) {
            this.keyAlgorithm = keyAlgorithm;
            this.signatureAlgorithm = signatureAlgorithm;
            this.signaturePaddingSchemes = signaturePaddingSchemes;
        }
    }

    private static final SupportedKeyAlgorithm[] SUPPORTED_KEY_ALGORITHMS =
        new SupportedKeyAlgorithm[] {
            new SupportedKeyAlgorithm(KeyProperties.KEY_ALGORITHM_RSA, "SHA256withRSA",
                    new String[] {KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                            KeyProperties.SIGNATURE_PADDING_RSA_PKCS1}),
            new SupportedKeyAlgorithm(KeyProperties.KEY_ALGORITHM_EC, "SHA256withECDSA", null)
        };

    byte[] signDataWithKey(String algoIdentifier, PrivateKey privateKey) throws Exception {
        byte[] data = new String("hello").getBytes();
        Signature sign = Signature.getInstance(algoIdentifier);
        sign.initSign(privateKey);
        sign.update(data);
        return sign.sign();
    }

    void verifySignature(String algoIdentifier, PublicKey publicKey, byte[] signature)
            throws Exception {
        byte[] data = new String("hello").getBytes();
        Signature verify = Signature.getInstance(algoIdentifier);
        verify.initVerify(publicKey);
        verify.update(data);
        assertThat(verify.verify(signature)).isTrue();
    }

    void verifySignatureOverData(String algoIdentifier, KeyPair keyPair) throws Exception {
        verifySignature(algoIdentifier, keyPair.getPublic(),
                signDataWithKey(algoIdentifier, keyPair.getPrivate()));
    }

    int getDeviceFirstSdkLevel() {
        return SystemProperties.getInt("ro.board.first_api_level", 0);
    }

    private void validateDeviceIdAttestationData(Certificate leaf,
                                                 String expectedSerial,
                                                 String expectedImei,
                                                 String expectedMeid,
                                                 String expectedSecondImei)
            throws CertificateParsingException {
        Attestation attestationRecord = Attestation.loadFromCertificate((X509Certificate) leaf);
        AuthorizationList teeAttestation = attestationRecord.getTeeEnforced();
        assertThat(teeAttestation).isNotNull();
        final String platformReportedBrand =
                TestUtils.isPropertyEmptyOrUnknown(Build.BRAND_FOR_ATTESTATION)
                ? Build.BRAND : Build.BRAND_FOR_ATTESTATION;
        assertThat(teeAttestation.getBrand()).isEqualTo(platformReportedBrand);
        final String platformReportedDevice =
                TestUtils.isPropertyEmptyOrUnknown(Build.DEVICE_FOR_ATTESTATION)
                        ? Build.DEVICE : Build.DEVICE_FOR_ATTESTATION;
        assertThat(teeAttestation.getDevice()).isEqualTo(platformReportedDevice);
        final String platformReportedProduct =
                TestUtils.isPropertyEmptyOrUnknown(Build.PRODUCT_FOR_ATTESTATION)
                ? Build.PRODUCT : Build.PRODUCT_FOR_ATTESTATION;
        assertThat(teeAttestation.getProduct()).isEqualTo(platformReportedProduct);
        final String platformReportedManufacturer =
                TestUtils.isPropertyEmptyOrUnknown(Build.MANUFACTURER_FOR_ATTESTATION)
                        ? Build.MANUFACTURER : Build.MANUFACTURER_FOR_ATTESTATION;
        assertThat(teeAttestation.getManufacturer()).isEqualTo(platformReportedManufacturer);
        final String platformReportedModel =
                TestUtils.isPropertyEmptyOrUnknown(Build.MODEL_FOR_ATTESTATION)
                ? Build.MODEL : Build.MODEL_FOR_ATTESTATION;
        assertThat(teeAttestation.getModel()).isEqualTo(platformReportedModel);
        assertThat(teeAttestation.getSerialNumber()).isEqualTo(expectedSerial);
        assertThat(teeAttestation.getImei()).isEqualTo(expectedImei);
        assertThat(teeAttestation.getMeid()).isEqualTo(expectedMeid);

        validateSecondImei(teeAttestation.getSecondImei(), expectedSecondImei);
    }

    private void validateSecondImei(String attestedSecondImei, String expectedSecondImei) {
        /**
         * Test attestation support for 2nd IMEI:
         * * Attestation of 2nd IMEI (if present on the device) is required for devices shipping
         *   with VSR-U (device's first SDK level U and above).
         * * KeyMint v3 implementations on devices that shipped with earlier VSR, MAY support
         *   attesting to the 2nd IMEI. In that case, if the 2nd IMEI tag is included in the
         *   attestation record, it must match what the platform provided.
         * * Other KeyMint implementations must not include anything in this tag.
         */
        final boolean isKeyMintV3 = TestUtils.getFeatureVersionKeystore(sContext) >= 300;
        final boolean emptySecondImei = TextUtils.isEmpty(expectedSecondImei);
        final boolean deviceShippedWithKeyMint3 =
                getDeviceFirstSdkLevel() >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

        if (!isKeyMintV3) {
            // Earlier versions of KeyMint must not attest to second IMEI values as they are not
            // allowed to emit an attestation extension version that includes it.
            assertThat(attestedSecondImei).isNull();
        } else if (emptySecondImei) {
            // Device doesn't have a second IMEI, so none should be included in the attestation
            // extension.
            assertThat(attestedSecondImei).isNull();
        } else if (deviceShippedWithKeyMint3) {
            // The device has a second IMEI and should attest to it.
            assertThat(attestedSecondImei).isEqualTo(expectedSecondImei);
        } else {
            // Device has KeyMint 3, but originally shipped with an earlier KeyMint and
            // may not have provisioned the second IMEI as an attestation ID.
            // It does not have to support attesting to the second IMEI, but if there is something
            // in the attestation record, it must match the platform-provided second IMEI.
            if (!TextUtils.isEmpty(attestedSecondImei)) {
                assertThat(attestedSecondImei).isEqualTo(expectedSecondImei);
            }
        }
    }

    private void validateDeviceIdAttestationDataUsingExtLib(Certificate leaf,
                                                            String expectedSerial,
                                                            String expectedImei,
                                                            String expectedMeid,
                                                            String expectedSecondImei)
            throws CertificateParsingException, IOException {
        ParsedAttestationRecord parsedAttestationRecord =
                createParsedAttestationRecord((X509Certificate) leaf);

        com.google.android.attestation.AuthorizationList teeAttestation =
                parsedAttestationRecord.teeEnforced;

        assertThat(teeAttestation).isNotNull();
        final String platformReportedBrand =
                TestUtils.isPropertyEmptyOrUnknown(Build.BRAND_FOR_ATTESTATION)
                ? Build.BRAND : Build.BRAND_FOR_ATTESTATION;
        assertThat(new String(teeAttestation.attestationIdBrand.get()))
                .isEqualTo(platformReportedBrand);
        final String platformReportedDevice =
                TestUtils.isPropertyEmptyOrUnknown(Build.DEVICE_FOR_ATTESTATION)
                        ? Build.DEVICE : Build.DEVICE_FOR_ATTESTATION;
        assertThat(new String(teeAttestation.attestationIdDevice.get()))
                .isEqualTo(platformReportedDevice);
        final String platformReportedProduct =
                TestUtils.isPropertyEmptyOrUnknown(Build.PRODUCT_FOR_ATTESTATION)
                ? Build.PRODUCT : Build.PRODUCT_FOR_ATTESTATION;
        assertThat(new String(teeAttestation.attestationIdProduct.get()))
                .isEqualTo(platformReportedProduct);
        final String platformReportedManufacturer =
                TestUtils.isPropertyEmptyOrUnknown(Build.MANUFACTURER_FOR_ATTESTATION)
                        ? Build.MANUFACTURER : Build.MANUFACTURER_FOR_ATTESTATION;
        assertThat(new String(teeAttestation.attestationIdManufacturer.get()))
                .isEqualTo(platformReportedManufacturer);
        final String platformReportedModel =
                TestUtils.isPropertyEmptyOrUnknown(Build.MODEL_FOR_ATTESTATION)
                ? Build.MODEL : Build.MODEL_FOR_ATTESTATION;
        assertThat(new String(teeAttestation.attestationIdModel.get()))
                .isEqualTo(platformReportedModel);

        assertThat(!TextUtils.isEmpty(expectedSerial))
                .isEqualTo(teeAttestation.attestationIdSerial.isPresent());
        if (!TextUtils.isEmpty(expectedSerial)) {
            assertThat(new String(teeAttestation.attestationIdSerial.get()))
                    .isEqualTo(expectedSerial);
        }
        assertThat(!TextUtils.isEmpty(expectedImei))
                .isEqualTo(teeAttestation.attestationIdImei.isPresent());
        if (!TextUtils.isEmpty(expectedImei)) {
            assertThat(new String(teeAttestation.attestationIdImei.get()))
                    .isEqualTo(expectedImei);
        }
        assertThat(!TextUtils.isEmpty(expectedMeid))
                .isEqualTo(teeAttestation.attestationIdMeid.isPresent());
        if (!TextUtils.isEmpty(expectedMeid)) {
            assertThat(new String(teeAttestation.attestationIdMeid.get()))
                    .isEqualTo(expectedMeid);
        }
        // TODO: Second IMEI parsing is not supported by external library yet,
        //  hence skipping for now.
        /* validateSecondImei(new String(teeAttestation.attestationIdSecondImei.get()),
                expectedSecondImei); */
    }

    private void validateAttestationRecord(List<Certificate> attestation, byte[] providedChallenge)
            throws CertificateParsingException {
        assertThat(attestation).isNotNull();
        assertThat(attestation.size()).isGreaterThan(1);
        X509Certificate leaf = (X509Certificate) attestation.get(0);
        Attestation attestationRecord = Attestation.loadFromCertificate(leaf);
        assertThat(attestationRecord.getAttestationChallenge()).isEqualTo(providedChallenge);
    }

    private void validateSignatureChain(List<Certificate> chain, PublicKey leafKey)
            throws GeneralSecurityException {
        X509Certificate leaf = (X509Certificate) chain.get(0);
        PublicKey keyFromCert = leaf.getPublicKey();
        assertThat(keyFromCert.getEncoded()).isEqualTo(leafKey.getEncoded());
        // Check that the certificate chain is valid.
        for (int i = 1; i < chain.size(); i++) {
            X509Certificate intermediate = (X509Certificate) chain.get(i);
            PublicKey intermediateKey = intermediate.getPublicKey();
            leaf.verify(intermediateKey);
            leaf = intermediate;
        }

        // leaf is now the root, verify the root is self-signed.
        PublicKey rootKey = leaf.getPublicKey();
        leaf.verify(rootKey);
    }

    private boolean isDeviceIdAttestationSupported() {
        return sDevicePolicyManager.isDeviceIdAttestationSupported();
    }

    private boolean isDeviceIdAttestationRequested(int deviceIdAttestationFlags) {
        return deviceIdAttestationFlags != 0;
    }

    /**
     * Generates a key using DevicePolicyManager.generateKeyPair using the given key algorithm,
     * then test signing and verifying using generated key.
     * If {@code signaturePaddings} is not null, it is added to the key parameters specification.
     * Returns the Attestation leaf certificate.
     */
    private Certificate generateKeyAndCheckAttestation(
            String keyAlgorithm, String signatureAlgorithm,
            String[] signaturePaddings, boolean useStrongBox,
            int deviceIdAttestationFlags) throws Exception {
        final String alias =
                String.format("com.android.test.attested-%s", keyAlgorithm.toLowerCase());
        byte[] attestationChallenge = new byte[] {0x01, 0x02, 0x03};
        try {
            KeyGenParameterSpec.Builder specBuilder =  new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAttestationChallenge(attestationChallenge)
                    .setIsStrongBoxBacked(useStrongBox);
            if (signaturePaddings != null) {
                specBuilder.setSignaturePaddings(signaturePaddings);
            }

            KeyGenParameterSpec spec = specBuilder.build();
            AttestedKeyPair generated = sDevicePolicyManager.generateKeyPair(
                    DEVICE_ADMIN_COMPONENT_NAME, keyAlgorithm, spec, deviceIdAttestationFlags);
            // If Device ID attestation was requested, check it succeeded if and only if device ID
            // attestation is supported.
            if (isDeviceIdAttestationRequested(deviceIdAttestationFlags)) {
                if (generated == null) {
                    assertWithMessage(
                        String.format(
                            "The device failed ID attestation, despite declaring it support for"
                            + "the feature. This is a hardware-related failure that should be "
                            + "analyzed by the OEM. The failure was for algorithm %s with flags"
                            + " %s.",
                            keyAlgorithm, deviceIdAttestationFlags))
                        .that(isDeviceIdAttestationSupported())
                        .isFalse();
                    return null;
                } else {
                    assertWithMessage(
                        String.format(
                            "The device declares it does not support ID attestation yet it "
                            + "produced a valid ID attestation record (for algorithm  %s, flags"
                            + " %s). This is a device configuration issue that should be "
                            + "analyzed by the OEM first",
                            keyAlgorithm, deviceIdAttestationFlags))
                        .that(isDeviceIdAttestationSupported())
                        .isTrue();
                }
            } else {
                assertWithMessage(
                    String.format(
                        "The device failed to generate a key with attestation that does not"
                        + " include Device ID attestation. This is a hardware failure that "
                        + "is usually caused by attestation keys not being provisioned on "
                        + "the device, and the OEM needs to analyze the underlying cause."
                        + " Algorithm used: %s",
                        keyAlgorithm))
                    .that(generated)
                    .isNotNull();
            }
            final KeyPair keyPair = generated.getKeyPair();
            verifySignatureOverData(signatureAlgorithm, keyPair);
            List<Certificate> attestation = generated.getAttestationRecord();
            validateAttestationRecord(attestation, attestationChallenge);
            validateSignatureChain(attestation, keyPair.getPublic());
            return attestation.get(0);
        } catch (UnsupportedOperationException ex) {
            assertWithMessage(
                    String.format(
                            "Unexpected failure while generating key %s with ID flags %d: %s"
                            + "\nIn case of AOSP/GSI builds, system provided properties could be"
                            + " different from provisioned properties in KeyMaster/KeyMint. In"
                            + " such cases, make sure attestation specific properties"
                            + " (Build.*_FOR_ATTESTATION) are configured correctly.",
                            keyAlgorithm, deviceIdAttestationFlags, ex))
                    .that(
                            isDeviceIdAttestationRequested(deviceIdAttestationFlags)
                                    && !isDeviceIdAttestationSupported())
                    .isTrue();
            return null;
        } finally {
            assertThat(sDevicePolicyManager.removeKeyPair(DEVICE_ADMIN_COMPONENT_NAME, alias))
                    .isTrue();
        }
    }

    public void assertAllVariantsOfDeviceIdAttestation(boolean useStrongBox) throws Exception {
        List<Integer> modesToTest = new ArrayList<Integer>();
        String imei = null;
        String secondImei = null;
        String meid = null;
        // All devices must support at least basic device information attestation as well as serial
        // number attestation. Although attestation of unique device ids are only callable by
        // device owner.
        modesToTest.add(ID_TYPE_BASE_INFO);
        modesToTest.add(ID_TYPE_SERIAL);
        // Get IMEI and MEID of the device.
        try (PermissionContext c = TestApis.permissions().withPermission(
                "android.permission.READ_PHONE_STATE")) {
            TelephonyManager telephonyService = sContext.getSystemService(TelephonyManager.class);
            assertWithMessage("Need to be able to read device identifiers")
                    .that(telephonyService)
                    .isNotNull();
            imei = telephonyService.getImei(0);
            secondImei = telephonyService.getImei(1);
            meid = telephonyService.getMeid(0);
            // If the device has a valid IMEI it must support attestation for it.
            if (imei != null) {
                modesToTest.add(ID_TYPE_IMEI);
            }
            // Same for MEID
            if (meid != null) {
                modesToTest.add(ID_TYPE_MEID);
            }
            int numCombinations = 1 << modesToTest.size();
            for (int i = 1; i < numCombinations; i++) {
                // Set the bits in devIdOpt to be passed into generateKeyPair according to the
                // current modes tested.
                int devIdOpt = 0;
                for (int j = 0; j < modesToTest.size(); j++) {
                    if ((i & (1 << j)) != 0) {
                        devIdOpt = devIdOpt | modesToTest.get(j);
                    }
                }
                try {
                    // Now run the test with all supported key algorithms
                    for (SupportedKeyAlgorithm supportedKey: SUPPORTED_KEY_ALGORITHMS) {
                        Certificate attestation = generateKeyAndCheckAttestation(
                                supportedKey.keyAlgorithm, supportedKey.signatureAlgorithm,
                                supportedKey.signaturePaddingSchemes, useStrongBox, devIdOpt);
                        // generateKeyAndCheckAttestation should return null if device ID
                        // attestation is not supported. Simply continue to test the next
                        // combination.
                        if (attestation == null && !isDeviceIdAttestationSupported()) {
                            continue;
                        }
                        assertWithMessage(
                                String.format(
                                        "Attestation should be valid for key %s with attestation"
                                                + " modes  %s",
                                        supportedKey.keyAlgorithm, devIdOpt))
                                .that(attestation)
                                .isNotNull();
                        // Set the expected values for serial, IMEI and MEID depending on whether
                        // attestation for them was requested.
                        String expectedSerial = null;
                        if ((devIdOpt & ID_TYPE_SERIAL) != 0) {
                            expectedSerial = Build.getSerial();
                        }
                        String expectedImei = null;
                        if ((devIdOpt & ID_TYPE_IMEI) != 0) {
                            expectedImei = imei;
                        }
                        String expectedMeid = null;
                        if ((devIdOpt & ID_TYPE_MEID) != 0) {
                            expectedMeid = meid;
                        }
                        String expectedSecondImei = null;
                        if ((devIdOpt & ID_TYPE_IMEI) != 0) {
                            expectedSecondImei = secondImei;
                        }
                        validateDeviceIdAttestationData(attestation, expectedSerial,
                                expectedImei, expectedMeid, expectedSecondImei);
                        // Validate attestation record using external library. As above validation
                        // is successful external library validation should also pass.
                        validateDeviceIdAttestationDataUsingExtLib(attestation, expectedSerial,
                                expectedImei, expectedMeid, expectedSecondImei);
                    }
                } catch (UnsupportedOperationException expected) {
                    // Make sure the test only fails if the device is not meant to support Device
                    // ID attestation.
                    assertThat(isDeviceIdAttestationSupported()).isFalse();
                } catch (StrongBoxUnavailableException expected) {
                    // This exception must only be thrown if StrongBox attestation was requested.
                    assertThat(useStrongBox && !hasStrongBox()).isTrue();
                }
            }
        }
    }

    @Test
    @ApiTest(apis = {"android.app.admin.DevicePolicyManager#generateKeyPair",
            "android.app.admin.DevicePolicyManager#ID_TYPE_IMEI",
            "android.app.admin.DevicePolicyManager#ID_TYPE_MEID",
            "android.app.admin.DevicePolicyManager#ID_TYPE_SERIAL"})
    @RequireRunOnSystemUser
    @RequireFeature(PackageManager.FEATURE_DEVICE_ADMIN)
    @RequireFeature(PackageManager.FEATURE_DEVICE_ID_ATTESTATION)
    public void testAllVariationsOfDeviceIdAttestation() throws Exception {
        try (DeviceOwner o = TestApis.devicePolicy().setDeviceOwner(DEVICE_ADMIN_COMPONENT_NAME)) {
            assertAllVariantsOfDeviceIdAttestation(false /* useStrongBox */);
        }
    }

    @Test
    @ApiTest(apis = {"android.app.admin.DevicePolicyManager#generateKeyPair",
            "android.app.admin.DevicePolicyManager#ID_TYPE_IMEI",
            "android.app.admin.DevicePolicyManager#ID_TYPE_MEID",
            "android.app.admin.DevicePolicyManager#ID_TYPE_SERIAL"})
    @RequireRunOnSystemUser
    @RequireFeature(PackageManager.FEATURE_DEVICE_ADMIN)
    @RequireFeature(PackageManager.FEATURE_DEVICE_ID_ATTESTATION)
    public void testAllVariationsOfDeviceIdAttestationUsingStrongBox() throws Exception {
        try (DeviceOwner o = TestApis.devicePolicy().setDeviceOwner(DEVICE_ADMIN_COMPONENT_NAME)) {
            assertAllVariantsOfDeviceIdAttestation(true  /* useStrongBox */);
        }
    }

    boolean hasStrongBox() {
        return sContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE);
    }
}
