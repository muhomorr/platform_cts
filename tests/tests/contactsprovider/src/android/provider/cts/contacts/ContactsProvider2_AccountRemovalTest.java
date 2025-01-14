/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package android.provider.cts.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.SimContacts;
import android.provider.cts.contacts.DatabaseAsserts.ContactIdPair;
import android.provider.cts.contacts.account.StaticAccountAuthenticator;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.compatibility.common.util.CddTest;
import com.android.compatibility.common.util.SystemUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MediumTest
public class ContactsProvider2_AccountRemovalTest extends AndroidTestCase {

    private static long ASYNC_TIMEOUT_LIMIT_MS = 1000 * 60 * 1; // 3 minutes
    private static long SLEEP_BETWEEN_POLL_MS = 1000 * 10; // 10 seconds

    private static int NOT_MERGED = -1;

    // Not re-using StaticAcountAuthenticator.ACCOUNT_1 because this test may break
    // other tests running when the account is removed.  No other tests should use the following
    // accounts.
    private static final Account ACCT_1 = new Account("cp removal acct 1",
            StaticAccountAuthenticator.TYPE);
    private static final Account ACCT_2 = new Account("cp removal acct 2",
            StaticAccountAuthenticator.TYPE);

    // Using unique account name and types because these tests may break or be broken by
    // other tests running.  No other tests should use the following accounts.
    private static final String SIM_ACCT_NAME_1 = "cp removal sim acct name 1";
    private static final String SIM_ACCT_TYPE_1 = "cp removal sim acct type 1";
    private static final String SIM_ACCT_NAME_2 = "cp removal sim acct name 2";
    private static final String SIM_ACCT_TYPE_2 = "cp removal sim acct type 2";
    private static final String SIM_ACCT_NAME_3 = "cp removal sim acct name 3";
    private static final String SIM_ACCT_TYPE_3 = "cp removal sim acct type 3";

    private static final int SIM_SLOT_0 = 0;
    private static final int SIM_SLOT_1 = 1;

    private ContentResolver mResolver;
    private AccountManager mAccountManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
        mAccountManager = AccountManager.get(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // Reset SIM accounts
        SystemUtil.runWithShellPermissionIdentity(() -> {
            SimContacts.removeSimAccounts(mResolver, SIM_SLOT_0);
            SimContacts.removeSimAccounts(mResolver, SIM_SLOT_1);
        });
    }

    public void testAccountRemoval_deletesContacts() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        mAccountManager.addAccountExplicitly(ACCT_2, null, null);
        ArrayList<ContactIdPair> acc1Ids = createContacts(ACCT_1, 5);
        ArrayList<ContactIdPair> acc2Ids = createContacts(ACCT_2, 15);

        mAccountManager.removeAccount(ACCT_2, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc2Ids);

        mAccountManager.removeAccount(ACCT_1, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc1Ids);
    }

    public void testAccountRemoval_hasDeleteLogsForContacts() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        mAccountManager.addAccountExplicitly(ACCT_2, null, null);
        ArrayList<ContactIdPair> acc1Ids = createContacts(ACCT_1, 5);
        ArrayList<ContactIdPair> acc2Ids = createContacts(ACCT_2, 15);

        long start = System.currentTimeMillis();
        mAccountManager.removeAccount(ACCT_2, null, null);
        assertContactsInDeleteLogEventually(start, acc2Ids);

        start = System.currentTimeMillis();
        mAccountManager.removeAccount(ACCT_1, null, null);
        assertContactsInDeleteLogEventually(start, acc1Ids);
    }

    /**
     * Contact has merged raw contacts from a single account.  Contact should be deleted upon
     * account removal.
     */
    public void testAccountRemovalWithMergedContact_deletesContacts() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        List<ContactIdPair> idList = createAndAssertMergedContact(ACCT_1, ACCT_1);
        mAccountManager.removeAccount(ACCT_1, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), idList);
    }

    /**
     * Contacts saved to the local account should not be removed when accounts change.
     *
     * <p>The local account is a special case that is not added to
     * {@link AccountManager}. Normally raw contacts with an
     * {@link android.provider.ContactsContract.RawContacts#ACCOUNT_NAME} and
     * {@link android.provider.ContactsContract.RawContacts#ACCOUNT_TYPE} that do not correspond
     * to an added account will be removed but this should not be done for the local account.
     */
    @CddTest(requirement="3.18/C-1-4")
    public void testAccountRemoval_doesNotDeleteLocalAccountContacts() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        mAccountManager.addAccountExplicitly(ACCT_2, null, null);
        ArrayList<ContactIdPair> acc1Ids = createContacts(ACCT_1, 5);
        ArrayList<ContactIdPair> acc2Ids = createContacts(ACCT_2, 15);

        String name = ContactsContract.RawContacts.getLocalAccountName(mContext);
        String type = ContactsContract.RawContacts.getLocalAccountType(mContext);
        Account deviceAccount = name != null && type != null ? new Account(name, type) : null;
        long deviceRawContactId = RawContactUtil
                .createRawContactWithAutoGeneratedName(mResolver, deviceAccount);
        // Remove accounts and wait for deletion of their contacts to finish before verifying
        // the existence of the device contacts
        mAccountManager.removeAccount(ACCT_2, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc2Ids);
        mAccountManager.removeAccount(ACCT_1, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc1Ids);

        assertTrue(RawContactUtil.rawContactExistsById(mResolver, deviceRawContactId));
    }

    /**
     * Contacts saved to a SIM account that was added through
     * {@link SimContacts#addSimAccount(ContentResolver, String, String, int, int)}
     * should not be deleted.
     *
     * <p>These SIM accounts are special cases that are not added to
     * {@link AccountManager}. Normally raw contacts with an
     * {@link android.provider.ContactsContract.RawContacts#ACCOUNT_NAME} and
     * {@link android.provider.ContactsContract.RawContacts#ACCOUNT_TYPE} that do not correspond
     * to an added account will be removed but this should not be done for these SIM accounts.
     */
    public void testAccountRemoval_doesNotDeleteSimAccountContacts() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        mAccountManager.addAccountExplicitly(ACCT_2, null, null);
        SystemUtil.runWithShellPermissionIdentity(() -> {
            SimContacts.addSimAccount(mResolver, SIM_ACCT_NAME_1, SIM_ACCT_TYPE_1, SIM_SLOT_0,
                    ContactsContract.SimAccount.ADN_EF_TYPE);
        });
        ArrayList<ContactIdPair> acc1Ids = createContacts(ACCT_1, 5);
        ArrayList<ContactIdPair> acc2Ids = createContacts(ACCT_2, 15);
        long simRawContactId = RawContactUtil
                .createRawContactWithAutoGeneratedName(mResolver,
                        new Account(SIM_ACCT_NAME_1, SIM_ACCT_TYPE_1));

        // Remove accounts and wait for deletion of their contacts to finish before verifying
        // the existence of the SIM contacts
        mAccountManager.removeAccount(ACCT_2, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc2Ids);
        mAccountManager.removeAccount(ACCT_1, null, null);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc1Ids);

        assertTrue(RawContactUtil.rawContactExistsById(mResolver, simRawContactId));
    }

    /**
     * Contacts saved to a SIM account that was added through
     * {@link SimContacts#addSimAccount(ContentResolver, String, String, int, int)}
     * should be deleted when {@link SimContacts#removeSimAccounts(ContentResolver, int)} is called.
     * Only SIM accounts from the sim slot index should be removed.
     */
    public void testRemoveSimAccount_deleteSimAccountContacts() {
        SystemUtil.runWithShellPermissionIdentity(() -> {
            SimContacts.addSimAccount(mResolver, SIM_ACCT_NAME_1, SIM_ACCT_TYPE_1, SIM_SLOT_0,
                    ContactsContract.SimAccount.ADN_EF_TYPE);
            SimContacts.addSimAccount(mResolver, SIM_ACCT_NAME_2, SIM_ACCT_TYPE_2, SIM_SLOT_0,
                    ContactsContract.SimAccount.SDN_EF_TYPE);
            SimContacts.addSimAccount(mResolver, SIM_ACCT_NAME_3, SIM_ACCT_TYPE_3, SIM_SLOT_1,
                    ContactsContract.SimAccount.ADN_EF_TYPE);
        });


        ArrayList<ContactIdPair> acc1Ids = createContacts(
                new Account(SIM_ACCT_NAME_1, SIM_ACCT_TYPE_1), 5);
        ArrayList<ContactIdPair> acc2Ids = createContacts(
                new Account(SIM_ACCT_NAME_2, SIM_ACCT_TYPE_2), 5);


        long secondSimSlotRawContactId = RawContactUtil
                .createRawContactWithAutoGeneratedName(mResolver,
                        new Account(SIM_ACCT_NAME_3, SIM_ACCT_TYPE_3));

        SystemUtil.runWithShellPermissionIdentity(() -> {
            SimContacts.removeSimAccounts(mResolver, SIM_SLOT_0);
        });
        // Wait for deletion of the contacts in the removed account to finish before verifying
        // the existence of the device contacts
        assertContactsDeletedEventually(System.currentTimeMillis(), acc1Ids);
        assertContactsDeletedEventually(System.currentTimeMillis(), acc2Ids);

        // Sim contacts in a different slot should remain
        assertTrue(RawContactUtil.rawContactExistsById(mResolver, secondSimSlotRawContactId));
    }

    /**
     * Contact has merged raw contacts from different accounts. Contact should not be deleted when
     * one account is removed.  But contact should have last updated timestamp updated.
     */
    public void testAccountRemovalWithMergedContact_doesNotDeleteContactAndTimestampUpdated() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        mAccountManager.addAccountExplicitly(ACCT_2, null, null);
        List<ContactIdPair> idList = createAndAssertMergedContact(ACCT_1, ACCT_2);
        long contactId = idList.get(0).mContactId;

        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, contactId);
        long start = System.currentTimeMillis();
        mAccountManager.removeAccount(ACCT_1, null, null);

        while (ContactUtil.queryContactLastUpdatedTimestamp(mResolver, contactId) == baseTime) {
            assertWithinTimeoutLimit(start,
                    "Contact " + contactId + " last updated timestamp has not been updated.");
            SystemClock.sleep(SLEEP_BETWEEN_POLL_MS);
        }
        mAccountManager.removeAccount(ACCT_2, null, null);
    }

    public void testAccountRemovalWithMergedContact_hasDeleteLogsForContacts() {
        mAccountManager.addAccountExplicitly(ACCT_1, null, null);
        List<ContactIdPair> idList = createAndAssertMergedContact(ACCT_1, ACCT_1);
        long start = System.currentTimeMillis();
        mAccountManager.removeAccount(ACCT_1, null, null);
        assertContactsInDeleteLogEventually(start, idList);
    }

    private List<ContactIdPair> createAndAssertMergedContact(Account acct, Account acct2) {
        ContactIdPair ids1 = DatabaseAsserts.assertAndCreateContactWithName(mResolver, acct,
                "merge me");
        DataUtil.insertPhoneNumber(mResolver, ids1.mRawContactId, "555-5555");

        ContactIdPair ids2 = DatabaseAsserts.assertAndCreateContactWithName(mResolver, acct2,
                "merge me");
        DataUtil.insertPhoneNumber(mResolver, ids2.mRawContactId, "555-5555");

        // Check merge before continuing. Merge process is async.
        long mergedContactId = assertMerged(System.currentTimeMillis(), ids1.mRawContactId,
                ids2.mRawContactId);

        // Update the contact id to the newly merged contact id.
        ids1.mContactId = mergedContactId;
        ids2.mContactId = mergedContactId;

        return Arrays.asList(ids1, ids2);
    }

    private long assertMerged(long start, long rawContactId, long rawContactId2) {
        long contactId = NOT_MERGED;
        while (contactId == NOT_MERGED) {
            assertWithinTimeoutLimit(start,
                    "Raw contact " + rawContactId + " and " + rawContactId2 + " are not merged.");

            SystemClock.sleep(SLEEP_BETWEEN_POLL_MS);
            contactId = checkMerged(rawContactId, rawContactId2);
        }
        return contactId;
    }

    private long checkMerged(long rawContactId, long rawContactId2) {
        long contactId = RawContactUtil.queryContactIdByRawContactId(mResolver, rawContactId);
        long contactId2 = RawContactUtil.queryContactIdByRawContactId(mResolver, rawContactId2);
        if (contactId == contactId2) {
            return contactId;
        }
        return NOT_MERGED;
    }

    private void assertContactsInDeleteLogEventually(long start, List<ContactIdPair> idList) {
        // Can not use newArrayList() because the version that accepts size is missing.
        ArrayList<ContactIdPair> remaining = new ArrayList<ContactIdPair>(idList.size());
        remaining.addAll(idList);
        while (!remaining.isEmpty()) {
            // Account cleanup is asynchronous, wait a bit before checking.
            SystemClock.sleep(SLEEP_BETWEEN_POLL_MS);
            assertWithinTimeoutLimit(start, "Contacts " + Arrays.toString(remaining.toArray()) +
                    " are not in delete log after account removal.");

            // Need a second list to remove since we can't remove from the list while iterating.
            ArrayList<ContactIdPair> toBeRemoved = new ArrayList<>();
            for (ContactIdPair ids : remaining) {
                long deletedTime = DeletedContactUtil.queryDeletedTimestampForContactId(mResolver,
                        ids.mContactId);
                if (deletedTime != CommonDatabaseUtils.NOT_FOUND) {
                    toBeRemoved.add(ids);
                    assertTrue("Deleted contact was found in delete log but insert time is before"
                            + " start time", deletedTime > start);
                }
            }
            remaining.removeAll(toBeRemoved);
        }

        // All contacts in delete log.  Pass.
    }

    /**
     * Polls every so often to see if all contacts have been deleted.  If not deleted in the
     * pre-defined threshold, fails.
     */
    private void assertContactsDeletedEventually(long start, List<ContactIdPair> idList) {
        // Can not use newArrayList() because the version that accepts size is missing.
        ArrayList<ContactIdPair> remaining = new ArrayList<ContactIdPair>(idList.size());
        remaining.addAll(idList);
        while (!remaining.isEmpty()) {
            // Account cleanup is asynchronous, wait a bit before checking.
            SystemClock.sleep(SLEEP_BETWEEN_POLL_MS);
            assertWithinTimeoutLimit(start, "Contacts have not been deleted after account"
                    + " removal.");

            ArrayList<ContactIdPair> toBeRemoved = new ArrayList<>();
            for (ContactIdPair ids : remaining) {
                if (!RawContactUtil.rawContactExistsById(mResolver, ids.mRawContactId)) {
                    toBeRemoved.add(ids);
                }
            }
            remaining.removeAll(toBeRemoved);
        }

        // All contacts deleted.  Pass.
    }

    private void assertWithinTimeoutLimit(long start, String message) {
        long now = System.currentTimeMillis();
        long elapsed = now - start;
        if (elapsed > ASYNC_TIMEOUT_LIMIT_MS) {
            fail(elapsed + "ms has elapsed. The limit is " + ASYNC_TIMEOUT_LIMIT_MS + "ms. " +
                    message);
        }
    }

    /**
     * Creates a given number of contacts for an account.
     */
    private ArrayList<ContactIdPair> createContacts(Account account, int numContacts) {
        ArrayList<ContactIdPair> accountIds = new ArrayList<>();
        for (int i = 0; i < numContacts; i++) {
            accountIds.add(DatabaseAsserts.assertAndCreateContact(mResolver, account));
        }
        return accountIds;
    }
}
