/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.telecom.cts;

import android.app.Instrumentation;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;

import androidx.test.InstrumentationRegistry;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class TestUtils {
    static final String TAG = "TelecomCTSTests";
    static final boolean HAS_TELECOM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    static final long WAIT_FOR_STATE_CHANGE_TIMEOUT_MS = 10000;
    static final long WAIT_FOR_CALL_ADDED_TIMEOUT_S = 15;
    static final long WAIT_FOR_STATE_CHANGE_TIMEOUT_CALLBACK = 50;
    static final long WAIT_FOR_PHONE_STATE_LISTENER_REGISTERED_TIMEOUT_S = 15;
    static final long WAIT_FOR_PHONE_STATE_LISTENER_CALLBACK_TIMEOUT_S = 15;
    static final boolean HAS_BLUETOOTH = hasBluetoothFeature();
    static final BluetoothDevice BLUETOOTH_DEVICE1 = makeBluetoothDevice("00:00:00:00:00:01");
    static final BluetoothDevice BLUETOOTH_DEVICE2 = makeBluetoothDevice("00:00:00:00:00:02");

    // Non-final to allow modification by tests not in this package (e.g. permission-related
    // tests in the Telecom2 test package.
    public static String PACKAGE = "android.telecom.cts";
    public static String SELF_MANAGED_PACKAGE = "android.telecom.cts.selfmanagedcstestappone";
    public static final String TEST_URI_SCHEME = "foobuzz";
    public static final String COMPONENT = "android.telecom.cts.CtsConnectionService";
    public static final String INCALL_COMPONENT = "android.telecom.cts/.MockInCallService";
    public static final String SELF_MANAGED_COMPONENT =
            "android.telecom.cts.CtsSelfManagedConnectionService";
    public static final String SELF_MANAGED_COMPONENT_1 =
            "android.telecom.cts.selfmanagedcstestappone.CtsSelfManagedConnectionServiceOne";
    public static final String REMOTE_COMPONENT = "android.telecom.cts.CtsRemoteConnectionService";
    public static final String ACCOUNT_ID_1 = "xtstest_CALL_PROVIDER_ID_1";
    public static final String ACCOUNT_ID_2 = "xtstest_CALL_PROVIDER_ID_2";
    public static final String ACCOUNT_ID_SIM = "sim_acct";
    public static final String ACCOUNT_ID_EMERGENCY = "xtstest_CALL_PROVIDER_EMERGENCY";
    public static final String EXTRA_PHONE_NUMBER = "android.telecom.cts.extra.PHONE_NUMBER";
    public static final ComponentName TELECOM_CTS_COMPONENT_NAME = new ComponentName(
            TestUtils.PACKAGE, TestUtils.COMPONENT);
    public static final PhoneAccountHandle TEST_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), ACCOUNT_ID_1);
    public static final PhoneAccountHandle TEST_SIM_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), ACCOUNT_ID_SIM);
    public static final PhoneAccountHandle TEST_PHONE_ACCOUNT_HANDLE_2 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), ACCOUNT_ID_2);
    public static final PhoneAccountHandle TEST_EMERGENCY_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), ACCOUNT_ID_EMERGENCY);
    public static final String DEFAULT_TEST_ACCOUNT_1_ID = "ctstest_DEFAULT_TEST_ID_1";
    public static final String DEFAULT_TEST_ACCOUNT_2_ID = "ctstest_DEFAULT_TEST_ID_2";
    public static final PhoneAccountHandle TEST_DEFAULT_PHONE_ACCOUNT_HANDLE_1 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT),
                    DEFAULT_TEST_ACCOUNT_1_ID);
    public static final PhoneAccountHandle TEST_DEFAULT_PHONE_ACCOUNT_HANDLE_2 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT),
                    DEFAULT_TEST_ACCOUNT_2_ID);
    public static final PhoneAccountHandle TEST_HANDOVER_SRC_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), "handoverFrom");
    public static final PhoneAccountHandle TEST_HANDOVER_DEST_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, SELF_MANAGED_COMPONENT),
                    "handoverTo");
    public static final String REMOTE_ACCOUNT_ID = "xtstest_REMOTE_CALL_PROVIDER_ID";
    public static final String SELF_MANAGED_ACCOUNT_ID_1 = "ctstest_SELF_MANAGED_ID_1";
    public static final PhoneAccountHandle TEST_SELF_MANAGED_HANDLE_1 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, SELF_MANAGED_COMPONENT),
                    SELF_MANAGED_ACCOUNT_ID_1);
    public static final String SELF_MANAGED_ACCOUNT_ID_2 = "ctstest_SELF_MANAGED_ID_2";
    public static final PhoneAccountHandle TEST_SELF_MANAGED_HANDLE_2 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, SELF_MANAGED_COMPONENT),
                    SELF_MANAGED_ACCOUNT_ID_2);
    public static final String SELF_MANAGED_ACCOUNT_ID_3 = "ctstest_SELF_MANAGED_ID_3";
    public static final PhoneAccountHandle TEST_SELF_MANAGED_HANDLE_3 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, SELF_MANAGED_COMPONENT),
                    SELF_MANAGED_ACCOUNT_ID_3);
    public static final String SELF_MANAGED_ACCOUNT_ID_4 = "ctstest_SELF_MANAGED_ID_4";
    public static final PhoneAccountHandle TEST_SELF_MANAGED_HANDLE_4 =
            new PhoneAccountHandle(new ComponentName(PACKAGE, SELF_MANAGED_COMPONENT),
                    SELF_MANAGED_ACCOUNT_ID_4);
    public static final String SELF_MANAGED_CS_1_ACCOUNT_ID_1 = "ctstest_SELF_MANAGED_CS_1_ID_1";
    public static final String SELF_MANAGED_CS_1_ACCOUNT_ID_3 = "ctstest_SELF_MANAGED_CS_1_ID_3";
    public static final PhoneAccountHandle TEST_SELF_MANAGED_CS_1_HANDLE_1 =
            new PhoneAccountHandle(
                    new ComponentName(SELF_MANAGED_PACKAGE, SELF_MANAGED_COMPONENT_1),
                    SELF_MANAGED_CS_1_ACCOUNT_ID_1);
    public static final PhoneAccountHandle TEST_SELF_MANAGED_CS_1_HANDLE_3 =
            new PhoneAccountHandle(
                    new ComponentName(SELF_MANAGED_PACKAGE, SELF_MANAGED_COMPONENT_1),
                    SELF_MANAGED_CS_1_ACCOUNT_ID_3);

    public static final String ACCOUNT_LABEL = "CTSConnectionService";
    public static final String SIM_ACCOUNT_LABEL = "CTSConnectionServiceSim";
    public static final PhoneAccount TEST_PHONE_ACCOUNT = PhoneAccount.builder(
            TEST_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER |
                    PhoneAccount.CAPABILITY_VIDEO_CALLING |
                    PhoneAccount.CAPABILITY_RTT |
                    PhoneAccount.CAPABILITY_CONNECTION_MANAGER |
                    PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS |
                    PhoneAccount.CAPABILITY_ADHOC_CONFERENCE_CALLING)
            .setHighlightColor(Color.RED)
            .setShortDescription(ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .build();

    public static final PhoneAccount TEST_PHONE_ACCOUNT_THAT_HANDLES_CONTENT_SCHEME =
            PhoneAccount.builder(
                            TEST_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
                    .setAddress(Uri.parse("tel:555-TEST"))
                    .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER |
                            PhoneAccount.CAPABILITY_VIDEO_CALLING |
                            PhoneAccount.CAPABILITY_RTT |
                            PhoneAccount.CAPABILITY_CONNECTION_MANAGER |
                            PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS |
                            PhoneAccount.CAPABILITY_ADHOC_CONFERENCE_CALLING)
                    .setHighlightColor(Color.RED)
                    .setShortDescription(ACCOUNT_LABEL)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
                    .addSupportedUriScheme("content")
                    .build();

    public static final PhoneAccount TEST_SIM_PHONE_ACCOUNT = PhoneAccount.builder(
            TEST_SIM_PHONE_ACCOUNT_HANDLE, SIM_ACCOUNT_LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER |
                    PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
            .setHighlightColor(Color.RED)
            .setShortDescription(SIM_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .build();

    public static final PhoneAccount TEST_PHONE_ACCOUNT_2 = PhoneAccount.builder(
            TEST_PHONE_ACCOUNT_HANDLE_2, ACCOUNT_LABEL + "2")
            .setAddress(Uri.parse("tel:555-TEST2"))
            .setSubscriptionAddress(Uri.parse("tel:555-TEST2"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER |
                    PhoneAccount.CAPABILITY_VIDEO_CALLING |
                    PhoneAccount.CAPABILITY_RTT |
                    PhoneAccount.CAPABILITY_CONNECTION_MANAGER)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .build();

    public static final PhoneAccount TEST_DEFAULT_PHONE_ACCOUNT_1 = PhoneAccount.builder(
            TEST_DEFAULT_PHONE_ACCOUNT_HANDLE_1, "Default Test 1")
            .setAddress(Uri.parse("foobuzz:testuri1"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setHighlightColor(Color.RED)
            .setShortDescription("Default Test 1")
            .addSupportedUriScheme(TEST_URI_SCHEME)
            .build();
    public static final PhoneAccount TEST_DEFAULT_PHONE_ACCOUNT_2 = PhoneAccount.builder(
            TEST_DEFAULT_PHONE_ACCOUNT_HANDLE_2, "Default Test 2")
            .setAddress(Uri.parse("foobuzz:testuri2"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setHighlightColor(Color.RED)
            .setShortDescription("Default Test 2")
            .addSupportedUriScheme(TEST_URI_SCHEME)
            .build();
    private static final Bundle SUPPORTS_HANDOVER_FROM_EXTRAS = new Bundle();
    private static final Bundle SUPPORTS_HANDOVER_TO_EXTRAS = new Bundle();
    static {
        SUPPORTS_HANDOVER_FROM_EXTRAS.putBoolean(PhoneAccount.EXTRA_SUPPORTS_HANDOVER_FROM, true);
        SUPPORTS_HANDOVER_TO_EXTRAS.putBoolean(PhoneAccount.EXTRA_SUPPORTS_HANDOVER_TO, true);
    }
    public static final PhoneAccount TEST_PHONE_ACCOUNT_HANDOVER_SRC = PhoneAccount.builder(
            TEST_HANDOVER_SRC_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setExtras(SUPPORTS_HANDOVER_FROM_EXTRAS)
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .build();
    public static final PhoneAccount TEST_PHONE_ACCOUNT_HANDOVER_DEST = PhoneAccount.builder(
            TEST_HANDOVER_DEST_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setExtras(SUPPORTS_HANDOVER_TO_EXTRAS)
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .setHighlightColor(Color.MAGENTA)
            .setShortDescription(ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_VOICEMAIL)
            .build();
    public static final String REMOTE_ACCOUNT_LABEL = "CTSRemoteConnectionService";
    public static final String SELF_MANAGED_ACCOUNT_LABEL = "android.telecom.cts";
    public static final PhoneAccount TEST_SELF_MANAGED_PHONE_ACCOUNT_3 = PhoneAccount.builder(
            TEST_SELF_MANAGED_HANDLE_3, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.fromParts(TEST_URI_SCHEME, "test@test.com", null))
            .setSubscriptionAddress(Uri.fromParts(TEST_URI_SCHEME, "test@test.com", null))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED |
                    PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING |
                    PhoneAccount.CAPABILITY_VIDEO_CALLING)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(TEST_URI_SCHEME)
            .build();
    public static final Bundle SELF_MANAGED_ACCOUNT_1_EXTRAS;
    static {
        SELF_MANAGED_ACCOUNT_1_EXTRAS = new Bundle();
        SELF_MANAGED_ACCOUNT_1_EXTRAS.putBoolean(
                PhoneAccount.EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE, false);
    }
    public static final Bundle SELF_MANAGED_ACCOUNT_2_EXTRAS;
    static {
        SELF_MANAGED_ACCOUNT_2_EXTRAS = new Bundle();
        SELF_MANAGED_ACCOUNT_2_EXTRAS.putBoolean(PhoneAccount.EXTRA_LOG_SELF_MANAGED_CALLS, true);
    }
    public static final Bundle SELF_MANAGED_ACCOUNT_4_EXTRAS;
    static {
        SELF_MANAGED_ACCOUNT_4_EXTRAS = new Bundle();
        SELF_MANAGED_ACCOUNT_4_EXTRAS.putBoolean(
                PhoneAccount.EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE, true);
    }
    public static final Bundle SELF_MANAGED_CS_1_ACCOUNT_1_EXTRAS;
    static {
        SELF_MANAGED_CS_1_ACCOUNT_1_EXTRAS = new Bundle();
        SELF_MANAGED_CS_1_ACCOUNT_1_EXTRAS.putBoolean(
                PhoneAccount.EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE, true);
    }
    public static final Bundle SELF_MANAGED_CS_1_ACCOUNT_3_EXTRAS;
    static {
        SELF_MANAGED_CS_1_ACCOUNT_3_EXTRAS = new Bundle();
        SELF_MANAGED_CS_1_ACCOUNT_3_EXTRAS.putBoolean(
                PhoneAccount.EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE, false);
    }

    public static final PhoneAccount TEST_SELF_MANAGED_PHONE_ACCOUNT_2 = PhoneAccount.builder(
            TEST_SELF_MANAGED_HANDLE_2, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.parse("sip:test@test.com"))
            .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED |
                    PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING |
                    PhoneAccount.CAPABILITY_VIDEO_CALLING)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .setExtras(SELF_MANAGED_ACCOUNT_2_EXTRAS)
            .build();
    public static final PhoneAccount TEST_SELF_MANAGED_PHONE_ACCOUNT_1 = PhoneAccount.builder(
            TEST_SELF_MANAGED_HANDLE_1, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.parse("sip:test@test.com"))
            .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED |
                    PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING |
                    PhoneAccount.CAPABILITY_VIDEO_CALLING)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .setExtras(SELF_MANAGED_ACCOUNT_1_EXTRAS)
            .build();
    public static final PhoneAccount TEST_SELF_MANAGED_PHONE_ACCOUNT_4 = PhoneAccount.builder(
            TEST_SELF_MANAGED_HANDLE_4, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.parse("sip:test@test.com"))
            .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED |
                    PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING |
                    PhoneAccount.CAPABILITY_VIDEO_CALLING)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .setExtras(SELF_MANAGED_ACCOUNT_4_EXTRAS)
            .build();
    public static final PhoneAccount TEST_SELF_MANAGED_CS_1_PHONE_ACCOUNT_1 = PhoneAccount.builder(
            TEST_SELF_MANAGED_CS_1_HANDLE_1, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.parse("sip:test@test.com"))
            .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED
                    | PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING
                    | PhoneAccount.CAPABILITY_VIDEO_CALLING)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .setExtras(SELF_MANAGED_CS_1_ACCOUNT_1_EXTRAS)
            .build();
    public static final PhoneAccount TEST_SELF_MANAGED_CS_1_PHONE_ACCOUNT_2 = PhoneAccount.builder(
            TEST_SELF_MANAGED_CS_1_HANDLE_1, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.parse("sip:test@test.com"))
            .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .setExtras(SELF_MANAGED_CS_1_ACCOUNT_1_EXTRAS)
            .build();
    public static final PhoneAccount TEST_SELF_MANAGED_CS_1_PHONE_ACCOUNT_3 = PhoneAccount.builder(
            TEST_SELF_MANAGED_CS_1_HANDLE_3, SELF_MANAGED_ACCOUNT_LABEL)
            .setAddress(Uri.parse("sip:test@test.com"))
            .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
            .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
            .setHighlightColor(Color.BLUE)
            .setShortDescription(SELF_MANAGED_ACCOUNT_LABEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .setExtras(SELF_MANAGED_CS_1_ACCOUNT_3_EXTRAS)
            .build();

    /**
     * See {@link TelecomManager#ENABLE_GET_CALL_STATE_PERMISSION_PROTECTION}
     */
    public static final String ENABLE_GET_CALL_STATE_PERMISSION_PROTECTION_STRING =
            "ENABLE_GET_CALL_STATE_PERMISSION_PROTECTION ";

    /**
     * See {@link TelecomManager#ENABLE_GET_PHONE_ACCOUNT_PERMISSION_PROTECTION}
     */
    public static final String ENABLE_GET_PHONE_ACCOUNT_PERMISSION_PROTECTION_STRING =
            "ENABLE_GET_PHONE_ACCOUNT_PERMISSION_PROTECTION ";

    private static final String COMMAND_SET_CALL_DIAGNOSTIC_SERVICE =
            "telecom set-call-diagnostic-service ";

    private static final String COMMAND_SET_DEFAULT_DIALER = "telecom set-default-dialer ";

    private static final String COMMAND_GET_DEFAULT_DIALER = "telecom get-default-dialer";

    private static final String COMMAND_SET_SYSTEM_DIALER = "telecom set-system-dialer ";

    private static final String COMMAND_GET_SYSTEM_DIALER = "telecom get-system-dialer";

    private static final String COMMAND_ENABLE = "telecom set-phone-account-enabled ";

    private static final String COMMAND_DISABLE = "telecom set-phone-account-disabled ";

    private static final String COMMAND_SET_ACCT_SUGGESTION =
            "telecom set-phone-acct-suggestion-component ";

    private static final String COMMAND_REGISTER_SIM = "telecom register-sim-phone-account ";

    private static final String COMMAND_SET_DEFAULT_PHONE_ACCOUNT =
            "telecom set-user-selected-outgoing-phone-account ";

    private static final String COMMAND_WAIT_ON_HANDLERS = "telecom wait-on-handlers";

    private static final String COMMAND_ADD_TEST_EMERGENCY_NUMBER =
            "cmd phone emergency-number-test-mode -a ";

    private static final String COMMAND_REMOVE_TEST_EMERGENCY_NUMBER =
            "cmd phone emergency-number-test-mode -r ";

    private static final String COMMAND_CLEAR_TEST_EMERGENCY_NUMBERS =
            "cmd phone emergency-number-test-mode -c";

    private static final String COMMAND_SET_TEST_EMERGENCY_PHONE_ACCOUNT_PACKAGE_NAME_FILTER =
            "telecom set-test-emergency-phone-account-package-filter ";

    private static final String COMMAND_AM_COMPAT = "am compat ";

    public static final String MERGE_CALLER_NAME = "calls-merged";
    public static final String SWAP_CALLER_NAME = "calls-swapped";

    public static boolean shouldTestTelecom(Context context) {
        if (!HAS_TELECOM) {
            return false;
        }
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELECOM);
    }

    public static boolean hasTelephonyFeature(Context context) {
        final PackageManager pm = context.getPackageManager();
        return (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && pm.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_CALLING));
    }

    public static String setCallDiagnosticService(Instrumentation instrumentation,
            String packageName)
            throws Exception {
        return executeShellCommand(instrumentation, COMMAND_SET_CALL_DIAGNOSTIC_SERVICE
                + packageName);
    }

    public static String setDefaultDialer(Instrumentation instrumentation, String packageName)
            throws Exception {
        return executeShellCommand(instrumentation, COMMAND_SET_DEFAULT_DIALER + packageName);
    }

    public static String setSystemDialerOverride(Instrumentation instrumentation) throws Exception {
        return executeShellCommand(instrumentation, COMMAND_SET_SYSTEM_DIALER + INCALL_COMPONENT);
    }

    public static String clearSystemDialerOverride(
            Instrumentation instrumentation) throws Exception {
        return executeShellCommand(instrumentation, COMMAND_SET_SYSTEM_DIALER + "default");
    }

    public static String setCtsPhoneAccountSuggestionService(Instrumentation instrumentation,
            ComponentName componentName) throws Exception {
        return executeShellCommand(instrumentation,
                COMMAND_SET_ACCT_SUGGESTION
                        + (componentName == null ? "" : componentName.flattenToString()));
    }

    public static String getDefaultDialer(Instrumentation instrumentation) throws Exception {
        return executeShellCommand(instrumentation, COMMAND_GET_DEFAULT_DIALER);
    }

    public static String getSystemDialer(Instrumentation instrumentation) throws Exception {
        return executeShellCommand(instrumentation, COMMAND_GET_SYSTEM_DIALER);
    }

    public static void enablePhoneAccount(Instrumentation instrumentation,
            PhoneAccountHandle handle) throws Exception {
        final ComponentName component = handle.getComponentName();
        final long currentUserSerial = getCurrentUserSerialNumber(instrumentation);
        executeShellCommand(instrumentation, COMMAND_ENABLE
                + component.getPackageName() + "/" + component.getClassName() + " "
                + handle.getId() + " " + currentUserSerial);
    }

    public static void disablePhoneAccount(Instrumentation instrumentation,
            PhoneAccountHandle handle) throws Exception {
        final ComponentName component = handle.getComponentName();
        final long currentUserSerial = getCurrentUserSerialNumber(instrumentation);
        executeShellCommand(instrumentation, COMMAND_DISABLE
                + component.getPackageName() + "/" + component.getClassName() + " "
                + handle.getId() + " " + currentUserSerial);
    }

    public static void registerSimPhoneAccount(Instrumentation instrumentation,
            PhoneAccountHandle handle, String label, String address) throws Exception {
        final ComponentName component = handle.getComponentName();
        final long currentUserSerial = getCurrentUserSerialNumber(instrumentation);
        executeShellCommand(instrumentation, COMMAND_REGISTER_SIM
                + component.getPackageName() + "/" + component.getClassName() + " "
                + handle.getId() + " " + currentUserSerial + " " + label + " " + address);
    }

    public static void registerEmergencyPhoneAccount(Instrumentation instrumentation,
            PhoneAccountHandle handle, String label, String address) throws Exception {
        final ComponentName component = handle.getComponentName();
        final long currentUserSerial = getCurrentUserSerialNumber(instrumentation);
        executeShellCommand(instrumentation, COMMAND_REGISTER_SIM  + "-e "
                + component.getPackageName() + "/" + component.getClassName() + " "
                + handle.getId() + " " + currentUserSerial + " " + label + " " + address);
    }

    public static void setDefaultOutgoingPhoneAccount(Instrumentation instrumentation,
            PhoneAccountHandle handle) throws Exception {
        if (handle != null) {
            final ComponentName component = handle.getComponentName();
            final long currentUserSerial = getCurrentUserSerialNumber(instrumentation);
            executeShellCommand(instrumentation, COMMAND_SET_DEFAULT_PHONE_ACCOUNT
                    + component.getPackageName() + "/" + component.getClassName() + " "
                    + handle.getId() + " " + currentUserSerial);
        } else {
            executeShellCommand(instrumentation, COMMAND_SET_DEFAULT_PHONE_ACCOUNT);
        }
    }

    public static void waitOnAllHandlers(Instrumentation instrumentation) {
        try {
            executeShellCommand(instrumentation, COMMAND_WAIT_ON_HANDLERS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void waitOnLocalMainLooper(long timeoutMs) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final CountDownLatch lock = new CountDownLatch(1);
        mainHandler.post(lock::countDown);
        while (lock.getCount() > 0) {
            try {
                lock.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    public static void addTestEmergencyNumber(Instrumentation instr,
            String testNumber) throws Exception {
        executeShellCommand(instr, COMMAND_ADD_TEST_EMERGENCY_NUMBER + testNumber);
    }

    public static void removeTestEmergencyNumber(Instrumentation instr,
            String number) throws Exception {
        executeShellCommand(instr, COMMAND_REMOVE_TEST_EMERGENCY_NUMBER + number);
    }

    public static void clearTestEmergencyNumbers(Instrumentation instr) throws Exception {
        executeShellCommand(instr, COMMAND_CLEAR_TEST_EMERGENCY_NUMBERS);
    }

    public static void setTestEmergencyPhoneAccountPackageFilter(Instrumentation instr,
            Context context) throws Exception {
        executeShellCommand(instr, COMMAND_SET_TEST_EMERGENCY_PHONE_ACCOUNT_PACKAGE_NAME_FILTER
                + context.getPackageName());
    }

    public static void clearTestEmergencyPhoneAccountPackageFilter(
            Instrumentation instr) throws Exception {
        executeShellCommand(instr, COMMAND_SET_TEST_EMERGENCY_PHONE_ACCOUNT_PACKAGE_NAME_FILTER);
    }

    public static void enableCompatCommand(Instrumentation instr,
            String commandName) throws Exception {
        String cmd = COMMAND_AM_COMPAT + "enable  --no-kill " + commandName + PACKAGE;
        executeShellCommand(instr, cmd);
    }

    public static void disableCompatCommand(Instrumentation instr,
            String commandName) throws Exception {
        String cmd = COMMAND_AM_COMPAT + "disable  --no-kill " + commandName + PACKAGE;
        executeShellCommand(instr, cmd);
    }

    public static void resetCompatCommand(Instrumentation instr,
            String commandName) throws Exception {
        String cmd = COMMAND_AM_COMPAT + "reset  --no-kill " + commandName + PACKAGE;
        executeShellCommand(instr, cmd);
    }

    /**
     * Executes the given shell command and returns the output in a string. Note that even
     * if we don't care about the output, we have to read the stream completely to make the
     * command execute.
     */
    public static String executeShellCommand(Instrumentation instrumentation,
            String command) throws Exception {
        final ParcelFileDescriptor pfd =
                instrumentation.getUiAutomation().executeShellCommand(command);
        BufferedReader br = null;
        try (InputStream in = new FileInputStream(pfd.getFileDescriptor())) {
            br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String str = null;
            StringBuilder out = new StringBuilder();
            while ((str = br.readLine()) != null) {
                out.append(str);
            }
            return out.toString();
        } finally {
            if (br != null) {
                closeQuietly(br);
            }
            closeQuietly(pfd);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Waits for the {@link CountDownLatch} to count down to 0 and then returns without reseting
     * the latch.
     * @param lock the latch that the system will wait on.
     * @return true if the latch was released successfully, false if the latch timed out before
     * resetting.
     */
    public static boolean waitForLatchCountDown(CountDownLatch lock) {
        if (lock == null) {
            return false;
        }

        boolean success;
        try {
            success = lock.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            return false;
        }

        return success;
    }

    /**
     * Waits for the {@link CountDownLatch} to count down to 0 and then returns a new reset latch.
     * @param lock The lock that will await a countDown to 0.
     * @return a new reset {@link CountDownLatch} if the lock successfully counted down to 0 or
     * null if the operation timed out.
     */
    public static CountDownLatch waitForLock(CountDownLatch lock) {
        boolean success = waitForLatchCountDown(lock);
        if (success) {
            return new CountDownLatch(1);
        } else {
            return null;
        }
    }

    /**
     * Adds a new incoming call.
     *
     * @param instrumentation the Instrumentation, used for shell command execution.
     * @param telecomManager the TelecomManager.
     * @param handle the PhoneAccountHandle associated with the call.
     * @param address the incoming address.
     * @return the new self-managed incoming call.
     */
    public static void addIncomingCall(Instrumentation instrumentation,
                                       TelecomManager telecomManager, PhoneAccountHandle handle,
                                       Uri address) {

        // Inform telecom of new incoming self-managed connection.
        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, address);
        telecomManager.addNewIncomingCall(handle, extras);

        // Wait for Telecom to finish creating the new connection.
        try {
            waitOnAllHandlers(instrumentation);
        } catch (Exception e) {
            TestCase.fail("Failed to wait on handlers");
        }
    }
    public static boolean hasBluetoothFeature() {
        try {
            return InstrumentationRegistry.getContext().getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean hasAutomotiveFeature() {
        try {
            return InstrumentationRegistry.getContext().getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
        } catch (Exception e) {
            return false;
        }
    }
    public static BluetoothDevice makeBluetoothDevice(String address) {
        if (!HAS_BLUETOOTH) return null;
        Parcel p1 = Parcel.obtain();
        p1.writeString(address);
        p1.setDataPosition(0);
        BluetoothDevice device = BluetoothDevice.CREATOR.createFromParcel(p1);
        p1.recycle();
        return device;
    }

    /**
     * Places a new outgoing call.
     *
     * @param telecomManager the TelecomManager.
     * @param handle the PhoneAccountHandle associated with the call.
     * @param address outgoing call address.
     * @return the new self-managed outgoing call.
     */
    public static void placeOutgoingCall(Instrumentation instrumentation,
                                          TelecomManager telecomManager, PhoneAccountHandle handle,
                                          Uri address) {
        placeOutgoingCall(instrumentation, telecomManager, handle, address,
                VideoProfile.STATE_AUDIO_ONLY);
    }

    /**
     * Places a new outgoing call.
     *
     * @param telecomManager the TelecomManager.
     * @param handle the PhoneAccountHandle associated with the call.
     * @param address outgoing call address.
     * @return the new self-managed outgoing call.
     */
    public static void placeOutgoingCall(Instrumentation instrumentation,
                                          TelecomManager telecomManager, PhoneAccountHandle handle,
                                          Uri address, int videoState) {
        // Inform telecom of new incoming self-managed connection.
        Bundle extras = new Bundle();
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);

        if (!VideoProfile.isAudioOnly(videoState)) {
            extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, videoState);
        }

        telecomManager.placeCall(address, extras);

        // Wait for Telecom to finish creating the new connection.
        try {
            waitOnAllHandlers(instrumentation);
        } catch (Exception e) {
            TestCase.fail("Failed to wait on handlers");
        }
    }

    /**
     * Waits for a new SelfManagedConnection with the given address to be added.
     * @param address The expected address.
     * @return The SelfManagedConnection found.
     */
    public static SelfManagedConnection waitForAndGetConnection(Uri address) {
        // Wait for creation of the new connection.
        if (!CtsSelfManagedConnectionService.waitForBinding()) {
            TestCase.fail("Could not bind to Self-Managed ConnectionService");
        }
        CtsSelfManagedConnectionService connectionService =
                CtsSelfManagedConnectionService.getConnectionService();
        TestCase.assertTrue(connectionService.waitForUpdate(
                CtsSelfManagedConnectionService.CONNECTION_CREATED_LOCK));

        Optional<SelfManagedConnection> connectionOptional = connectionService.getConnections()
                .stream()
                .filter(connection -> address.equals(connection.getAddress()))
                .findFirst();
        assert(connectionOptional.isPresent());
        return connectionOptional.get();
    }

    /**
     * Utility class used to track the number of times a callback was invoked, and the arguments it
     * was invoked with. This class is prefixed Invoke rather than the more typical Call for
     * disambiguation purposes.
     */
    public static final class InvokeCounter {
        private final String mName;
        private final Object mLock = new Object();
        private final ArrayList<Object[]> mInvokeArgs = new ArrayList<>();

        private int mInvokeCount;

        public InvokeCounter(String callbackName) {
            mName = callbackName;
        }

        public void invoke(Object... args) {
            synchronized (mLock) {
                mInvokeCount++;
                mInvokeArgs.add(args);
                mLock.notifyAll();
            }
        }

        public Object[] getArgs(int index) {
            synchronized (mLock) {
                return mInvokeArgs.get(index);
            }
        }

        public int getInvokeCount() {
            synchronized (mLock) {
                return mInvokeCount;
            }
        }

        public void waitForCount(int count) {
            waitForCount(count, WAIT_FOR_STATE_CHANGE_TIMEOUT_MS);
        }

        public void waitForCount(int count, long timeoutMillis) {
            waitForCount(count, timeoutMillis, null);
        }

        public void waitForCount(long timeoutMillis) {
             synchronized (mLock) {
             try {
                  mLock.wait(timeoutMillis);
             }catch (InterruptedException ex) {
                  ex.printStackTrace();
             }
           }
        }

        public void waitForCount(int count, long timeoutMillis, String message) {
            synchronized (mLock) {
                final long startTimeMillis = SystemClock.uptimeMillis();
                while (mInvokeCount < count) {
                    try {
                        final long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
                        final long remainingTimeMillis = timeoutMillis - elapsedTimeMillis;
                        if (remainingTimeMillis <= 0) {
                            if (message != null) {
                                TestCase.fail(message);
                            } else {
                                TestCase.fail(String.format("Expected %s to be called %d times.",
                                        mName, count));
                            }
                        }
                        mLock.wait(timeoutMillis);
                    } catch (InterruptedException ie) {
                        /* ignore */
                    }
                }
            }
        }

        /**
         * Waits for a predicate to return {@code true} within the specified timeout.  Uses the
         * {@link #mLock} for this {@link InvokeCounter} to eliminate the need to perform busy-wait.
         * @param predicate The predicate.
         * @param timeoutMillis The timeout.
         */
        public void waitForPredicate(Predicate predicate, long timeoutMillis) {
            synchronized (mLock) {
                long startTimeMillis = SystemClock.uptimeMillis();
                long elapsedTimeMillis = 0;
                long remainingTimeMillis = timeoutMillis;
                Object foundValue = null;
                boolean wasFound = false;
                do {
                    try {
                        mLock.wait(timeoutMillis);
                        foundValue = (mInvokeArgs.get(mInvokeArgs.size()-1))[0];
                        wasFound = predicate.test(foundValue);
                        elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
                        remainingTimeMillis = timeoutMillis - elapsedTimeMillis;
                    } catch (InterruptedException ie) {
                        /* ignore */
                    }
                } while (!wasFound && remainingTimeMillis > 0);
                if (wasFound) {
                    return;
                } else if (remainingTimeMillis <= 0) {
                    TestCase.fail("Expected value not found within time limit");
                }
            }
        }

        public void clearArgs() {
            synchronized (mLock) {
                mInvokeArgs.clear();
            }
        }

        public void reset() {
            synchronized (mLock) {
                clearArgs();
                mInvokeCount = 0;
            }
        }
    }

    private static long getCurrentUserSerialNumber(Instrumentation instrumentation) {
        UserManager userManager =
                instrumentation.getContext().getSystemService(UserManager.class);
        return userManager.getSerialNumberForUser(Process.myUserHandle());
    }



    public static Uri insertContact(ContentResolver contentResolver, String phoneNumber)
            throws Exception {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "test_type")
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "test_name")
                .build());
        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "test")
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .withYieldAllowed(true)
                .build());
        return contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)[0].uri;
    }

    public static int deleteContact(ContentResolver contentResolver, Uri deleteUri) {
        return contentResolver.delete(deleteUri, null, null);
    }

    /**
     * Generates random phone accounts.
     * @param seed random seed to use for random UUIDs; passed in for determinism.
     * @param count How many phone accounts to use.
     * @return Random phone accounts.
     */
    public static ArrayList<PhoneAccount> generateRandomPhoneAccounts(long seed, int count,
            String packageName, String component) {
        Random random = new Random(seed);
        ArrayList<PhoneAccount> accounts = new ArrayList<>();
        for (int ix = 0; ix < count; ix++) {
            PhoneAccountHandle handle = new PhoneAccountHandle(
                    new ComponentName(packageName, component), getRandomUuid(random).toString());
            PhoneAccount acct = new PhoneAccount.Builder(handle, "TelecommTests")
                    .setAddress(Uri.parse("sip:test@test.com"))
                    .setSubscriptionAddress(Uri.parse("sip:test@test.com"))
                    .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED
                            | PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING
                            | PhoneAccount.CAPABILITY_VIDEO_CALLING)
                    .setHighlightColor(Color.BLUE)
                    .setShortDescription(TestUtils.SELF_MANAGED_ACCOUNT_LABEL)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
                    .setExtras(TestUtils.SELF_MANAGED_ACCOUNT_1_EXTRAS)
                    .build();
            accounts.add(acct);
        }
        return accounts;
    }

    /**
     * Returns a random UUID based on the passed in Random generator.
     * @param random Random generator.
     * @return The UUID.
     */
    public static UUID getRandomUuid(Random random) {
        byte[] array = new byte[16];
        random.nextBytes(array);
        return UUID.nameUUIDFromBytes(array);
    }

    public static PhoneAccountHandle makePhoneAccountHandle(String id) {
        return new PhoneAccountHandle(TELECOM_CTS_COMPONENT_NAME, id);
    }


}
