/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.telephony.cts;

import static androidx.test.InstrumentationRegistry.getContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TtsSpan;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class PhoneNumberUtilsTest {
    private static final int MIN_MATCH = 7;

    // mPhoneNumber ~ "+17005550020", length == 7.
    private byte[] mPhoneNumber = { (byte) 0x91, (byte) 0x71, (byte) 0x00, (byte) 0x55,
            (byte) 0x05, (byte) 0x20, (byte) 0xF0 };

    private int mOldMinMatch;

    @Before
    public void setUp() throws Exception {
        mOldMinMatch = PhoneNumberUtils.getMinMatchForTest();
        PhoneNumberUtils.setMinMatchForTest(MIN_MATCH);
    }

    @After
    public void tearDown() throws Exception {
        PhoneNumberUtils.setMinMatchForTest(mOldMinMatch);
    }

    @Test
    public void testExtractMethods() {

        // Test extractNetworkPortion
        assertNull(PhoneNumberUtils.extractNetworkPortion(null));
        assertEquals("+17005554141", PhoneNumberUtils.extractNetworkPortion("+17005554141"));
        assertEquals("+17005554141*#N",
                PhoneNumberUtils.extractNetworkPortion("+1 (700).555-4141*#N"));
        assertEquals("170055541", PhoneNumberUtils.extractNetworkPortion(
                String.format("1 (700).555-41%c1234", PhoneNumberUtils.PAUSE)));
        assertEquals("**21**17005554141#",
                PhoneNumberUtils.extractNetworkPortion("**21**+17005554141#"));

        // Test extractPostDialPortion
        assertNull(PhoneNumberUtils.extractPostDialPortion(null));
        assertEquals("", PhoneNumberUtils.extractPostDialPortion("+17005554141"));
        assertEquals(String.format("%c1234", PhoneNumberUtils.PAUSE),
                PhoneNumberUtils.extractPostDialPortion(
                String.format("+1 (700).555-41NN%c1234", PhoneNumberUtils.PAUSE)));
        assertEquals(String.format("%c1234", PhoneNumberUtils.WAIT),
                PhoneNumberUtils.extractPostDialPortion(
                String.format("+1 (700).555-41NN%c1234", PhoneNumberUtils.WAIT)));
        assertEquals(String.format("%c1234%c%cN", PhoneNumberUtils.WAIT, PhoneNumberUtils.PAUSE,
                PhoneNumberUtils.WAIT), PhoneNumberUtils
                .extractPostDialPortion(
                        String.format("+1 (700).555-41NN%c1-2.34 %c%cN", PhoneNumberUtils.WAIT,
                                PhoneNumberUtils.PAUSE,
                                PhoneNumberUtils.WAIT)));
        assertEquals("example", PhoneNumberUtils.getUsernameFromUriNumber("example@example.com"));
    }

    @Test
    public void testCallMethods() {
        // Test calledPartyBCDToString
        assertEquals("+17005550020", PhoneNumberUtils.calledPartyBCDToString(mPhoneNumber, 0, 7));

        // Test toCallerIDMinMatch
        assertNull(PhoneNumberUtils.toCallerIDMinMatch(null));
//        assertEquals("1414555", PhoneNumberUtils.toCallerIDMinMatch("17005554141"));
//        assertEquals("1414555", PhoneNumberUtils.toCallerIDMinMatch("1-700-555-4141"));
//        assertEquals("1414555", PhoneNumberUtils.toCallerIDMinMatch("1-700-555-4141,1234"));
//        assertEquals("1414555", PhoneNumberUtils.toCallerIDMinMatch("1-700-555-4141;1234"));
//        assertEquals("NN14555", PhoneNumberUtils.toCallerIDMinMatch("1-700-555-41NN"));
        assertEquals("", PhoneNumberUtils.toCallerIDMinMatch(""));
        assertEquals("0032", PhoneNumberUtils.toCallerIDMinMatch("2300"));
        assertEquals("0032+", PhoneNumberUtils.toCallerIDMinMatch("+2300"));
        assertEquals("#130#*", PhoneNumberUtils.toCallerIDMinMatch("*#031#"));

        // Test networkPortionToCalledPartyBCD, calledPartyBCDToString
        byte[] bRet = PhoneNumberUtils.networkPortionToCalledPartyBCD("+17005550020");
        assertEquals(mPhoneNumber.length, bRet.length);
        for (int i = 0; i < mPhoneNumber.length; i++) {
            assertEquals(mPhoneNumber[i], bRet[i]);
        }
        bRet = PhoneNumberUtils.networkPortionToCalledPartyBCD("7005550020");
        assertEquals("7005550020", PhoneNumberUtils.calledPartyBCDToString(bRet, 0, bRet.length));

        // Test calledPartyBCDFragmentToString
        assertEquals("1917005550020", PhoneNumberUtils.calledPartyBCDFragmentToString(mPhoneNumber,
                0, 7));

        // Test networkPortionToCalledPartyBCDWithLength
        bRet = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength("+17005550020");
        assertEquals(mPhoneNumber.length + 1, bRet.length);
        for (int i = 0; i < mPhoneNumber.length; i++) {
            assertEquals(mPhoneNumber[i], bRet[i + 1]);
        }

        // Test numberToCalledPartyBCD
        bRet = PhoneNumberUtils.numberToCalledPartyBCD("+17005550020");
        assertEquals(mPhoneNumber.length, bRet.length);
        for (int i = 0; i < mPhoneNumber.length; i++) {
            assertEquals(mPhoneNumber[i], bRet[i]);
        }
    }

    @Test
    public void testGetMethods() throws RemoteException {
        // Test getStrippedReversed
        assertNull(PhoneNumberUtils.getStrippedReversed(null));
        assertEquals("14145550071", PhoneNumberUtils.getStrippedReversed("1-700-555-4141"));
        assertEquals("14145550071", PhoneNumberUtils.getStrippedReversed("1-700-555-4141,1234"));
        assertEquals("14145550071", PhoneNumberUtils.getStrippedReversed("1-700-555-4141;1234"));
        assertEquals("NN145550071", PhoneNumberUtils.getStrippedReversed("1-700-555-41NN"));
        assertEquals("", PhoneNumberUtils.getStrippedReversed(""));
        assertEquals("#130#*+", PhoneNumberUtils.getStrippedReversed("+*#031#"));

        // Test getFormatTypeForLocale
        int formatType = PhoneNumberUtils.getFormatTypeForLocale(Locale.CHINA);
        assertEquals(PhoneNumberUtils.FORMAT_UNKNOWN, formatType);
        formatType = PhoneNumberUtils.getFormatTypeForLocale(Locale.US);
        assertEquals(PhoneNumberUtils.FORMAT_NANP, formatType);
        formatType = PhoneNumberUtils.getFormatTypeForLocale(Locale.JAPAN);
        assertEquals(PhoneNumberUtils.FORMAT_JAPAN, formatType);

        // Test getNumberFromIntent, query nothing, return null.
        Intent intent = new Intent();
        intent.setData(Contacts.People.CONTENT_URI);
        Context context = getContext();
        assertNull(PhoneNumberUtils.getNumberFromIntent(intent, context));

        intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+18005555555"));
        assertEquals("+18005555555", PhoneNumberUtils.getNumberFromIntent(intent, getContext()));

        ContentResolver cr = getContext().getContentResolver();
        Uri personRecord = null;
        Uri phoneRecord = null;
        try {
            // insert a contact with phone number
            ContentValues values = new ContentValues();
            values.put(People.NAME, "CTS test contact");
            personRecord = cr.insert(People.CONTENT_URI, values);
            Uri phoneUri = Uri.withAppendedPath(personRecord, People.Phones.CONTENT_DIRECTORY);
            values.clear();
            values.put(People.Phones.TYPE, People.Phones.TYPE_HOME);
            values.put(People.Phones.NUMBER, "+18005552871");
            phoneRecord = cr.insert(phoneUri, values);

            intent = new Intent(Intent.ACTION_DIAL, phoneRecord);
            assertEquals("+18005552871",
                    PhoneNumberUtils.getNumberFromIntent(intent, getContext()));
        } finally {
            if (personRecord != null) {
                cr.delete(personRecord, null, null);
            }
            if (phoneRecord != null) {
                cr.delete(phoneRecord, null, null);
            }
        }
    }

    @Test
    public void testFormatMethods() {
        // Test formatNanpNumber
        SpannableStringBuilder builderNumber = new SpannableStringBuilder();
        builderNumber.append("8005551212");
        PhoneNumberUtils.formatNanpNumber(builderNumber);
        assertEquals("800-555-1212", builderNumber.toString());
        builderNumber.clear();
        builderNumber.append("800555121");
        PhoneNumberUtils.formatNanpNumber(builderNumber);
        assertEquals("800-555-121", builderNumber.toString());
        builderNumber.clear();
        builderNumber.append("555-1212");
        PhoneNumberUtils.formatNanpNumber(builderNumber);
        assertEquals("555-1212", builderNumber.toString());
        builderNumber.clear();
        builderNumber.append("180055512");
        PhoneNumberUtils.formatNanpNumber(builderNumber);
        assertEquals("1-800-555-12", builderNumber.toString());
        builderNumber.clear();
        builderNumber.append("+180055512");
        PhoneNumberUtils.formatNanpNumber(builderNumber);
        assertEquals("+1-800-555-12", builderNumber.toString());

        // Test convertKeypadLettersToDigits
        assertEquals("1-800-4664-411", PhoneNumberUtils
                .convertKeypadLettersToDigits("1-800-GOOG-411"));
        assertEquals("1-800-466-4411", PhoneNumberUtils
                .convertKeypadLettersToDigits("1-800-466-4411"));
        assertEquals("222-333-444-555-666-7777-888-9999", PhoneNumberUtils
                .convertKeypadLettersToDigits("ABC-DEF-GHI-JKL-MNO-PQRS-TUV-WXYZ"));
        assertEquals("222-333-444-555-666-7777-888-9999", PhoneNumberUtils
                .convertKeypadLettersToDigits("abc-def-ghi-jkl-mno-pqrs-tuv-wxyz"));
        assertEquals("(800) 222-3334", PhoneNumberUtils
                .convertKeypadLettersToDigits("(800) ABC-DEFG"));

        // Test stringFromStringAndTOA
        assertNull(PhoneNumberUtils.stringFromStringAndTOA(null, 1));
        assertEquals("+888888888", PhoneNumberUtils.stringFromStringAndTOA("888888888",
                PhoneNumberUtils.TOA_International));

        // Test formatJapaneseNumber
        Editable jpEditNumber = Editable.Factory.getInstance().newEditable("0377777777");
        PhoneNumberUtils.formatJapaneseNumber(jpEditNumber);
        assertEquals("03-7777-7777", jpEditNumber.toString());
        jpEditNumber = Editable.Factory.getInstance().newEditable("09077777777");
        PhoneNumberUtils.formatJapaneseNumber(jpEditNumber);
        assertEquals("090-7777-7777", jpEditNumber.toString());
        jpEditNumber = Editable.Factory.getInstance().newEditable("0120777777");
        PhoneNumberUtils.formatJapaneseNumber(jpEditNumber);
        assertEquals("0120-777-777", jpEditNumber.toString());
        jpEditNumber = Editable.Factory.getInstance().newEditable("+81377777777");
        PhoneNumberUtils.formatJapaneseNumber(jpEditNumber);
        assertEquals("+81-3-7777-7777", jpEditNumber.toString());
        jpEditNumber = Editable.Factory.getInstance().newEditable("+819077777777");
        PhoneNumberUtils.formatJapaneseNumber(jpEditNumber);
        assertEquals("+81-90-7777-7777", jpEditNumber.toString());

        // Test formatNumber(String). Only numbers begin with +1 or +81 can be formatted.
        assertEquals("+1-888-888-888", PhoneNumberUtils.formatNumber("+1888888888"));
        // Test formatNumber(Editable, int)
        Editable editNumber = Editable.Factory.getInstance().newEditable("0377777777");
        PhoneNumberUtils.formatNumber(editNumber, PhoneNumberUtils.FORMAT_UNKNOWN);
        assertEquals("0377777777", editNumber.toString());
        editNumber = Editable.Factory.getInstance().newEditable("+177777777");
        PhoneNumberUtils.formatNumber(editNumber, PhoneNumberUtils.FORMAT_UNKNOWN);
        assertEquals("+1-777-777-77", editNumber.toString());
        editNumber = Editable.Factory.getInstance().newEditable("+8177777777");
        PhoneNumberUtils.formatNumber(editNumber, PhoneNumberUtils.FORMAT_UNKNOWN);
        assertEquals("+81-77-777-777", editNumber.toString());

        // Test stripSeparators
        assertEquals("+188888888", PhoneNumberUtils.stripSeparators("+188-888-888"));

        // Test toaFromString
        assertEquals(PhoneNumberUtils.TOA_International, PhoneNumberUtils
                .toaFromString("+88888888"));
        assertEquals(PhoneNumberUtils.TOA_Unknown, PhoneNumberUtils.toaFromString("88888888"));
    }

    @Test
    public void testJudgeMethods() {
        // Test is12Key, isDialable, isISODigit, isReallyDialable, isStartsPostDial
        for (char c = '0'; c <= '9'; c++) {
            assertTrue(PhoneNumberUtils.is12Key(c));
            assertTrue(PhoneNumberUtils.isDialable(c));
            assertTrue(PhoneNumberUtils.isISODigit(c));
            assertTrue(PhoneNumberUtils.isNonSeparator(c));
            assertTrue(PhoneNumberUtils.isReallyDialable(c));
        }
        char c = '*';
        assertTrue(PhoneNumberUtils.is12Key(c));
        assertTrue(PhoneNumberUtils.isDialable(c));
        assertTrue(PhoneNumberUtils.isNonSeparator(c));
        assertTrue(PhoneNumberUtils.isReallyDialable(c));
        c = '#';
        assertTrue(PhoneNumberUtils.is12Key(c));
        assertTrue(PhoneNumberUtils.isDialable(c));
        assertTrue(PhoneNumberUtils.isNonSeparator(c));
        assertTrue(PhoneNumberUtils.isReallyDialable(c));
        c = '$';
        assertFalse(PhoneNumberUtils.is12Key(c));
        assertFalse(PhoneNumberUtils.isDialable(c));
        assertFalse(PhoneNumberUtils.isDialable(c));
        c = '+';
        assertTrue(PhoneNumberUtils.isDialable(c));
        assertFalse(PhoneNumberUtils.isISODigit(c));
        assertTrue(PhoneNumberUtils.isNonSeparator(c));
        assertTrue(PhoneNumberUtils.isReallyDialable(c));
        c = PhoneNumberUtils.WILD;
        assertTrue(PhoneNumberUtils.isDialable(c));
        assertTrue(PhoneNumberUtils.isNonSeparator(c));
        assertFalse(PhoneNumberUtils.isReallyDialable(c));
        c = PhoneNumberUtils.WAIT;
        assertTrue(PhoneNumberUtils.isNonSeparator(c));
        assertTrue(PhoneNumberUtils.isStartsPostDial(c));
        c = PhoneNumberUtils.PAUSE;
        assertTrue(PhoneNumberUtils.isNonSeparator(c));
        assertTrue(PhoneNumberUtils.isStartsPostDial(c));
        c = '8';
        assertFalse(PhoneNumberUtils.isStartsPostDial(c));

        // Test isEmergencyNumber, now only know US emergency number
        TelephonyManager tm = (TelephonyManager)getContext().getSystemService(
                 Context.TELEPHONY_SERVICE);
        // Test isEmergencyNumber, now only know US emergency number
        if ("US".equals(tm.getSimCountryIso())) {
            assertTrue(PhoneNumberUtils.isEmergencyNumber("911"));
            assertFalse(PhoneNumberUtils.isEmergencyNumber("119"));
        }

        // Test isGlobalPhoneNumber
        assertTrue(PhoneNumberUtils.isGlobalPhoneNumber("+17005554141"));
        assertFalse(PhoneNumberUtils.isGlobalPhoneNumber("android"));

        // Test isWellFormedSmsAddress
        assertTrue(PhoneNumberUtils.isWellFormedSmsAddress("+17005554141"));
        assertFalse(PhoneNumberUtils.isWellFormedSmsAddress("android"));

        // Test isUriNumber
        assertTrue(PhoneNumberUtils.isUriNumber("example@example.com"));
        assertFalse(PhoneNumberUtils.isUriNumber("+18005555555"));

        // Test isVoicemailNumber -- this is closely tied to the SIM so we'll just test some basic
        // cases
        assertFalse(PhoneNumberUtils.isVoiceMailNumber(getContext(),
                SubscriptionManager.getDefaultSubscriptionId(), null));
        assertFalse(PhoneNumberUtils.isVoiceMailNumber(getContext(),
                SubscriptionManager.getDefaultSubscriptionId(), ""));

    }

    @Test
    public void testGetPhoneTtsSpan() {
        // Setup: phone number without a country code. Lets keep coverage minimal to avoid
        // exercising the underlying PhoneNumberUtil or constraining localization changes.
        String phoneNumber = "6512223333";
        // Execute
        TtsSpan ttsSpan = PhoneNumberUtils.createTtsSpan(phoneNumber);
        // Verify: the created TtsSpan contains the phone number.
        assertEquals("6512223333", ttsSpan.getArgs().get(TtsSpan.ARG_NUMBER_PARTS));
    }

    @Test
    public void testAddPhoneTtsSpan() {
        // Setup: phone number without a country code. Lets keep coverage minimal to avoid
        // exercising the underlying PhoneNumberUtil or constraining localization changes.
        Spannable spannable = new SpannableString("Hello 6502223333");
        // Execute
        PhoneNumberUtils.addTtsSpan(spannable, 5, spannable.length());
        // Verify: the Spannable is annotated with a TtsSpan in the correct location.
        TtsSpan[] ttsSpans = spannable.getSpans(5, spannable.length() - 1, TtsSpan.class);
        assertEquals(1, ttsSpans.length);
        assertEquals("6502223333", ttsSpans[0].getArgs().get(TtsSpan.ARG_NUMBER_PARTS));
    }

    @Test
    public void testGetPhoneTtsSpannable() {
        // Setup: phone number without a country code. Lets keep coverage minimal to avoid
        // exercising the underlying PhoneNumberUtil or constraining localization changes.
        CharSequence phoneNumber = "6512223333";
        // Execute
        Spannable spannable = (Spannable) PhoneNumberUtils.createTtsSpannable(phoneNumber);
        // Verify: returned char sequence contains a TtsSpan with the phone number in it
        TtsSpan[] ttsSpans = spannable.getSpans(0, spannable.length() - 1, TtsSpan.class);
        assertEquals(1, ttsSpans.length);
        assertEquals("6512223333", ttsSpans[0].getArgs().get(TtsSpan.ARG_NUMBER_PARTS));
    }

    @Test
    public void testFormatNumberToE164() {
        assertNull(PhoneNumberUtils.formatNumber("invalid#", "US"));
        assertNull(PhoneNumberUtils.formatNumberToE164("1234567", "US"));

        assertEquals("+18004664114", PhoneNumberUtils.formatNumberToE164("800-GOOG-114", "US"));
        assertEquals("+16502910000", PhoneNumberUtils.formatNumberToE164("650 2910000", "US"));
        assertEquals("+12023458246", PhoneNumberUtils.formatNumberToE164("(202)345-8246", "US"));
        assertEquals("+812023458246", PhoneNumberUtils.formatNumberToE164("202-345-8246", "JP"));
    }

    @Test
    public void testFormatNumberToE164_countryCodeLowerCase() {
        assertNull(PhoneNumberUtils.formatNumber("invalid#", "us"));
        assertNull(PhoneNumberUtils.formatNumberToE164("1234567", "us"));

        assertEquals("+18004664114", PhoneNumberUtils.formatNumberToE164("800-GOOG-114", "us"));
        assertEquals("+16502910000", PhoneNumberUtils.formatNumberToE164("650 2910000", "us"));
        assertEquals("+12023458246", PhoneNumberUtils.formatNumberToE164("(202)345-8246", "us"));
        assertEquals("+812023458246", PhoneNumberUtils.formatNumberToE164("202-345-8246", "jp"));
    }

    @Test
    public void testAreSamePhoneNumber() {
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("abcd", "bcde", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("1-800-flowers", "800-flowers", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("1-800-flowers", "1-800-abcdefg", "us"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("999", "999", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("123456789", "923456789", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("123456789", "0123456789", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "650 253 0000", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "1-650-253-0000", "us"));

        //TODO: Change the expected result to false after libphonenumber improvement
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "11-650-253-0000", "us"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "0-650-253-0000", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("555-4141", "+1-700-555-4141", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+1650-253-0000", "6502530000", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("001650-253-0000", "6502530000", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("0111650-253-0000", "6502530000", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+19012345678", "+819012345678", "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("008001231234", "8001231234", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+66811234567", "166811234567", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("080-1234-5678", "+819012345678", "us"));

        //TODO: Change the expected result to false after libphonenumber improvement
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("011 11 7005554141", "+17005554141", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+44 207 792 3490", "00 207 792 3490",
                "us"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("16610001234", "6610001234", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("550-450-3605", "+14504503605", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("550-450-3605", "+15404503605", "us"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("550-450-3605", "+15514503605", "us"));

        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+31771234567", "0771234567", "jp"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "+819012345678", "jp"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "90-1234-5678", "jp"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "080-1234-5678", "jp"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "190-1234-5678", "jp"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "890-1234-5678", "jp"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("080-1234-5678", "+819012345678", "jp"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("290-1234-5678", "+819012345678", "jp"));

        //TODO: Change the expected result to false after libphonenumber improvement
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+79161234567", "89161234567", "ru"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+33123456789", "0123456789", "fr"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+31771234567", "0771234567", "nl"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+593(800)123-1234", "8001231234", "ec"));
    }

    @Test
    public void testAreSamePhoneNumber_countryCodeUpperCase() {
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("abcd", "bcde", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("1-800-flowers", "800-flowers", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("1-800-flowers", "1-800-abcdefg", "US"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("999", "999", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("123456789", "923456789", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("123456789", "0123456789", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "650 253 0000", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "1-650-253-0000", "US"));

        //TODO: Change the expected result to false after libphonenumber improvement
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "11-650-253-0000", "US"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("650-253-0000", "0-650-253-0000", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("555-4141", "+1-700-555-4141", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+1650-253-0000", "6502530000", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("001650-253-0000", "6502530000", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("0111650-253-0000", "6502530000", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+19012345678", "+819012345678", "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("008001231234", "8001231234", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+66811234567", "166811234567", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("080-1234-5678", "+819012345678", "US"));

        //TODO: Change the expected result to false after libphonenumber improvement
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("011 11 7005554141", "+17005554141", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+44 207 792 3490", "00 207 792 3490",
                "US"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("16610001234", "6610001234", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("550-450-3605", "+14504503605", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("550-450-3605", "+15404503605", "US"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("550-450-3605", "+15514503605", "US"));

        assertFalse(PhoneNumberUtils.areSamePhoneNumber("+31771234567", "0771234567", "JP"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "+819012345678", "JP"));
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "90-1234-5678", "JP"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "080-1234-5678", "JP"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "190-1234-5678", "JP"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("090-1234-5678", "890-1234-5678", "JP"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("080-1234-5678", "+819012345678", "JP"));
        assertFalse(PhoneNumberUtils.areSamePhoneNumber("290-1234-5678", "+819012345678", "JP"));

        //TODO: Change the expected result to false after libphonenumber improvement
        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+79161234567", "89161234567", "RU"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+33123456789", "0123456789", "FR"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+31771234567", "0771234567", "NL"));

        assertTrue(PhoneNumberUtils.areSamePhoneNumber("+593(800)123-1234", "8001231234", "EC"));
    }
}
