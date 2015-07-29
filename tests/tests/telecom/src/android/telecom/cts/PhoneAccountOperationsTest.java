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

import static android.telecom.cts.TestUtils.*;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.test.InstrumentationTestCase;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


/**
 * Verifies some of the PhoneAccount registration related operations.
 */
public class PhoneAccountOperationsTest extends InstrumentationTestCase {
    public static final PhoneAccountHandle TEST_PHONE_ACCOUNT_HANDLE =
            new PhoneAccountHandle(new ComponentName(PACKAGE, COMPONENT), ACCOUNT_ID);

    public static final PhoneAccount TEST_SIM_PHONE_ACCOUNT = PhoneAccount.builder(
            TEST_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
            .setHighlightColor(Color.RED)
            .setShortDescription(ACCOUNT_LABEL)
            .setSupportedUriSchemes(Arrays.asList("tel"))
            .build();

    public static final PhoneAccount TEST_NO_SIM_PHONE_ACCOUNT = PhoneAccount.builder(
            TEST_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
            .setAddress(Uri.parse("tel:555-TEST"))
            .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setHighlightColor(Color.RED)
            .setShortDescription(ACCOUNT_LABEL)
            .setSupportedUriSchemes(Arrays.asList("tel"))
            .build();

    private Context mContext;
    private TelecomManager mTelecomManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        if (!TestUtils.shouldTestTelecom(mContext)) {
            return;
        }
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    protected void tearDown() throws Exception {
        mTelecomManager.unregisterPhoneAccount(TEST_PHONE_ACCOUNT_HANDLE);
        PhoneAccount retrievedPhoneAccount = mTelecomManager.getPhoneAccount(
                TEST_PHONE_ACCOUNT_HANDLE);
        assertNull("Test account not deregistered.", retrievedPhoneAccount);
        super.tearDown();
    }

    public void testRegisterPhoneAccount_correctlyThrowsSecurityException() throws Exception {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        try {
            mTelecomManager.registerPhoneAccount(TEST_SIM_PHONE_ACCOUNT);
            fail("TelecomManager.registerPhoneAccount should throw SecurityException if "
                    + "not a system app.");
        } catch (SecurityException e) {
            assertTrue("Unexpected security exception.", (e.getMessage().indexOf(
                    "android.permission.REGISTER_SIM_SUBSCRIPTION") >= 0));
        }
    }

    public void testRegisterPhoneAccount_NotEnabledAutomatically() throws Exception {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        mTelecomManager.registerPhoneAccount(TEST_NO_SIM_PHONE_ACCOUNT);
        PhoneAccount retrievedPhoneAccount = mTelecomManager.getPhoneAccount(
                TEST_PHONE_ACCOUNT_HANDLE);
        assertNotNull("Failed to retrieve test account.", retrievedPhoneAccount);
        assertFalse("Phone account should not be automatically enabled.",
                retrievedPhoneAccount.isEnabled());
    }

    public void testRegisterPhoneAccount_DisallowEnable() throws Exception {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        Method setIsEnabled = null;
        PhoneAccount.Builder phoneAccountBuilder = PhoneAccount.builder(
                TEST_PHONE_ACCOUNT_HANDLE, ACCOUNT_LABEL)
                .setAddress(Uri.parse("tel:555-TEST"))
                .setSubscriptionAddress(Uri.parse("tel:555-TEST"))
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .setHighlightColor(Color.RED)
                .setShortDescription(ACCOUNT_LABEL)
                .setSupportedUriSchemes(Arrays.asList("tel"));
        try {
            setIsEnabled = PhoneAccount.Builder.class.getDeclaredMethod(
                    "setIsEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            fail("Failed to find setIsEnabled method.");
        }
        setIsEnabled.invoke(phoneAccountBuilder, true);
        final PhoneAccount phoneAccount  = phoneAccountBuilder.build();
        mTelecomManager.registerPhoneAccount(phoneAccount);
        PhoneAccount retrievedPhoneAccount = mTelecomManager.getPhoneAccount(
                TEST_PHONE_ACCOUNT_HANDLE);
        assertNotNull("Failed to retrieve test account.", retrievedPhoneAccount);
        assertFalse("3rd party app cannot enable its own phone account.",
                retrievedPhoneAccount.isEnabled());
    }

    public void testRegisterPhoneAccount_ListEnabledAccounts() throws Exception {
        if (!shouldTestTelecom(mContext)) {
            return;
        }
        mTelecomManager.registerPhoneAccount(TEST_NO_SIM_PHONE_ACCOUNT);
        final List<PhoneAccountHandle> oldAccounts = mTelecomManager.getCallCapablePhoneAccounts();
        final int oldAccountsListSize = oldAccounts.size();
        if (oldAccountsListSize > 0) {
            assertFalse("Enabled Phone accounts should not contain the test account.",
                    oldAccounts.contains(TEST_PHONE_ACCOUNT_HANDLE));
        }
        TestUtils.enablePhoneAccount(getInstrumentation(), TEST_PHONE_ACCOUNT_HANDLE);
        final List<PhoneAccountHandle> newAccounts = mTelecomManager.getCallCapablePhoneAccounts();
        assertNotNull("No enabled Phone account found.", newAccounts);
        assertEquals("1 new enabled Phone account expected.", newAccounts.size(),
                oldAccountsListSize+1);
        assertTrue("Enabled Phone accounts do not contain the test account.",
                newAccounts.contains(TEST_PHONE_ACCOUNT_HANDLE));
    }

}
