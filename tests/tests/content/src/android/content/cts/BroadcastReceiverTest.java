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

package android.content.cts;

import static android.content.Context.RECEIVER_EXPORTED;

import android.app.BroadcastOptions;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.BroadcastReceiver.PendingResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.platform.test.annotations.AppModeFull;
import android.test.ActivityInstrumentationTestCase2;

import androidx.test.InstrumentationRegistry;

import com.android.compatibility.common.util.CddTest;
import com.android.server.am.nano.ActivityManagerServiceDumpBroadcastsProto;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link BroadcastReceiver}.
 * TODO:  integrate the existing tests.
 */
public class BroadcastReceiverTest extends ActivityInstrumentationTestCase2<MockActivity> {
    private static final int RESULT_INITIAL_CODE = 1;
    private static final String RESULT_INITIAL_DATA = "initial data";

    private static final int RESULT_INTERNAL_FINAL_CODE = 7;
    private static final String RESULT_INTERNAL_FINAL_DATA = "internal final data";

    private static final String ACTION_BROADCAST_INTERNAL =
            "android.content.cts.BroadcastReceiverTest.BROADCAST_INTERNAL";
    private static final String ACTION_BROADCAST_MOCKTEST =
            "android.content.cts.BroadcastReceiverTest.BROADCAST_MOCKTEST";
    private static final String ACTION_TEST_NOT_EXPORTED =
            "android.content.cts.BroadcastReceiverTest.TEST_NOT_EXPORTED";
    private static final String ACTION_BROADCAST_TESTABORT =
            "android.content.cts.BroadcastReceiverTest.BROADCAST_TESTABORT";
    private static final String ACTION_BROADCAST_DISABLED =
            "android.content.cts.BroadcastReceiverTest.BROADCAST_DISABLED";
    private static final String TEST_PACKAGE_NAME = "android.content.cts";

    private static final String SIGNATURE_PERMISSION = "android.content.cts.SIGNATURE_PERMISSION";

    private static final long SEND_BROADCAST_TIMEOUT = 15000;
    private static final long START_SERVICE_TIMEOUT  = 3000;

    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(1);
    private static final ComponentName DISABLEABLE_RECEIVER =
            new ComponentName("android.content.cts",
                    "android.content.cts.MockReceiverDisableable");

    public BroadcastReceiverTest() {
        super(TEST_PACKAGE_NAME, MockActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testConstructor() {
        new MockReceiverInternal();
    }

    public void testAccessDebugUnregister() {
        MockReceiverInternal mockReceiver = new MockReceiverInternal();
        assertFalse(mockReceiver.getDebugUnregister());

        mockReceiver.setDebugUnregister(true);
        assertTrue(mockReceiver.getDebugUnregister());

        mockReceiver.setDebugUnregister(false);
        assertFalse(mockReceiver.getDebugUnregister());
    }

    public void testSetOrderedHint() {
        MockReceiverInternal mockReceiver = new MockReceiverInternal();

        /*
         * Let's just test to make sure the method doesn't fail for this one.
         * It's marked as "for internal use".
         */
        mockReceiver.setOrderedHint(true);
        mockReceiver.setOrderedHint(false);
    }

    private class MockReceiverInternal extends BroadcastReceiver  {
        protected boolean mCalledOnReceive = false;
        private IBinder mIBinder;

        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            mCalledOnReceive = true;
            Intent serviceIntent = new Intent(context, MockService.class);
            mIBinder = peekService(context, serviceIntent);
            notifyAll();
        }

        public boolean hasCalledOnReceive() {
            return mCalledOnReceive;
        }

        public void reset() {
            mCalledOnReceive = false;
        }

        public synchronized void waitForReceiver(long timeout)
                throws InterruptedException {
            if (!mCalledOnReceive) {
                wait(timeout);
            }
            assertTrue(mCalledOnReceive);
        }

        public synchronized boolean waitForReceiverNoException(long timeout)
                throws InterruptedException {
            if (!mCalledOnReceive) {
                wait(timeout);
            }
            return mCalledOnReceive;
        }

        public IBinder getIBinder() {
            return mIBinder;
        }
    }

    private class MockReceiverInternalOrder extends MockReceiverInternal  {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            setResultCode(RESULT_INTERNAL_FINAL_CODE);
            setResultData(RESULT_INTERNAL_FINAL_DATA);

            super.onReceive(context, intent);
        }
    }

    private class MockReceiverInternalVerifyUncalled extends MockReceiverInternal {
        final int mExpectedInitialCode;

        public MockReceiverInternalVerifyUncalled(int initialCode) {
            mExpectedInitialCode = initialCode;
        }

        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            // only update to the expected final values if we're still in the
            // initial conditions.  The intermediate receiver would have
            // updated the result code if it [inappropriately] ran.
            if (getResultCode() == mExpectedInitialCode) {
                setResultCode(RESULT_INTERNAL_FINAL_CODE);
            }

            super.onReceive(context, intent);
        }
    }

    public static class TestNonExportedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            COUNT_DOWN_LATCH.countDown();
        }
    }

    public static class MockAsyncReceiver extends BroadcastReceiver {
        public CompletableFuture<PendingResult> pendingResult = new CompletableFuture<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            pendingResult.complete(goAsync());
        }
    }

    public void testOnReceive () throws InterruptedException {
        final MockActivity activity = getActivity();

        MockReceiverInternal internalReceiver = new MockReceiverInternal();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BROADCAST_INTERNAL);
        activity.registerReceiver(internalReceiver, filter, RECEIVER_EXPORTED);

        assertEquals(0, internalReceiver.getResultCode());
        assertEquals(null, internalReceiver.getResultData());
        assertEquals(null, internalReceiver.getResultExtras(false));

        activity.sendBroadcast(new Intent(ACTION_BROADCAST_INTERNAL)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND));
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        activity.unregisterReceiver(internalReceiver);
    }

    @AppModeFull
    public void testManifestReceiverPackage() throws InterruptedException {
        MockReceiverInternal internalReceiver = new MockReceiverInternal();

        Bundle map = new Bundle();
        map.putString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY,
                MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE);
        map.putString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY,
                MockReceiver.RESULT_EXTRAS_REMOVE_VALUE);
        getInstrumentation().getContext().sendOrderedBroadcast(
                new Intent(ACTION_BROADCAST_MOCKTEST)
                        .setPackage(TEST_PACKAGE_NAME).addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                null, internalReceiver,
                null, RESULT_INITIAL_CODE, RESULT_INITIAL_DATA, map);
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        // These are set by MockReceiver.
        assertEquals(MockReceiver.RESULT_CODE, internalReceiver.getResultCode());
        assertEquals(MockReceiver.RESULT_DATA, internalReceiver.getResultData());

        Bundle resultExtras = internalReceiver.getResultExtras(false);
        assertEquals(MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY));
        assertEquals(MockReceiver.RESULT_EXTRAS_ADD_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_ADD_KEY));
        assertNull(resultExtras.getString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY));
    }

    @AppModeFull
    public void testManifestReceiverComponent() throws InterruptedException {
        MockReceiverInternal internalReceiver = new MockReceiverInternal();

        Bundle map = new Bundle();
        map.putString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY,
                MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE);
        map.putString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY,
                MockReceiver.RESULT_EXTRAS_REMOVE_VALUE);
        getInstrumentation().getContext().sendOrderedBroadcast(
                new Intent(ACTION_BROADCAST_MOCKTEST)
                        .setClass(getActivity(), MockReceiver.class)
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                null, internalReceiver,
                null, RESULT_INITIAL_CODE, RESULT_INITIAL_DATA, map);
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        // These are set by MockReceiver.
        assertEquals(MockReceiver.RESULT_CODE, internalReceiver.getResultCode());
        assertEquals(MockReceiver.RESULT_DATA, internalReceiver.getResultData());

        Bundle resultExtras = internalReceiver.getResultExtras(false);
        assertEquals(MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY));
        assertEquals(MockReceiver.RESULT_EXTRAS_ADD_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_ADD_KEY));
        assertNull(resultExtras.getString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY));
    }

    @AppModeFull
    public void testManifestReceiverPermission() throws InterruptedException {
        MockReceiverInternal internalReceiver = new MockReceiverInternal();

        Bundle map = new Bundle();
        map.putString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY,
                MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE);
        map.putString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY,
                MockReceiver.RESULT_EXTRAS_REMOVE_VALUE);
        getInstrumentation().getContext().sendOrderedBroadcast(
                new Intent(ACTION_BROADCAST_MOCKTEST)
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                SIGNATURE_PERMISSION, internalReceiver,
                null, RESULT_INITIAL_CODE, RESULT_INITIAL_DATA, map);
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        // These are set by MockReceiver.
        assertEquals(MockReceiver.RESULT_CODE, internalReceiver.getResultCode());
        assertEquals(MockReceiver.RESULT_DATA, internalReceiver.getResultData());

        Bundle resultExtras = internalReceiver.getResultExtras(false);
        assertEquals(MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY));
        assertEquals(MockReceiver.RESULT_EXTRAS_ADD_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_ADD_KEY));
        assertNull(resultExtras.getString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY));
    }

    public void testNoManifestReceiver() throws InterruptedException {
        MockReceiverInternal internalReceiver = new MockReceiverInternal();

        Bundle map = new Bundle();
        map.putString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY,
                MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE);
        map.putString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY,
                MockReceiver.RESULT_EXTRAS_REMOVE_VALUE);
        getInstrumentation().getContext().sendOrderedBroadcast(
                new Intent(ACTION_BROADCAST_MOCKTEST).addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                null, internalReceiver,
                null, RESULT_INITIAL_CODE, RESULT_INITIAL_DATA, map);
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        // The MockReceiver should not have run, so we should still have the initial result.
        assertEquals(RESULT_INITIAL_CODE, internalReceiver.getResultCode());
        assertEquals(RESULT_INITIAL_DATA, internalReceiver.getResultData());

        Bundle resultExtras = internalReceiver.getResultExtras(false);
        assertEquals(MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY));
        assertNull(resultExtras.getString(MockReceiver.RESULT_EXTRAS_ADD_KEY));
        assertEquals(MockReceiver.RESULT_EXTRAS_REMOVE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY));
    }

    @AppModeFull
    public void testAbortBroadcast() throws InterruptedException {
        MockReceiverInternalOrder internalOrderReceiver = new MockReceiverInternalOrder();

        assertEquals(0, internalOrderReceiver.getResultCode());
        assertNull(internalOrderReceiver.getResultData());
        assertNull(internalOrderReceiver.getResultExtras(false));

        Bundle map = new Bundle();
        map.putString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY,
                MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE);
        map.putString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY,
                MockReceiver.RESULT_EXTRAS_REMOVE_VALUE);
        // The order of the receiver is:
        // MockReceiverFirst --> MockReceiverAbort --> MockReceiver --> internalOrderReceiver.
        // And MockReceiver is the receiver which will be aborted.
        getInstrumentation().getContext().sendOrderedBroadcast(
                new Intent(ACTION_BROADCAST_TESTABORT)
                        .setPackage(TEST_PACKAGE_NAME).addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                null, internalOrderReceiver,
                null, RESULT_INITIAL_CODE, RESULT_INITIAL_DATA, map);
        internalOrderReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        assertEquals(RESULT_INTERNAL_FINAL_CODE, internalOrderReceiver.getResultCode());
        assertEquals(RESULT_INTERNAL_FINAL_DATA, internalOrderReceiver.getResultData());
        Bundle resultExtras = internalOrderReceiver.getResultExtras(false);
        assertEquals(MockReceiver.RESULT_EXTRAS_INVARIABLE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_INVARIABLE_KEY));
        assertEquals(MockReceiver.RESULT_EXTRAS_REMOVE_VALUE,
                resultExtras.getString(MockReceiver.RESULT_EXTRAS_REMOVE_KEY));
        assertEquals(MockReceiverFirst.RESULT_EXTRAS_FIRST_VALUE,
                resultExtras.getString(MockReceiverFirst.RESULT_EXTRAS_FIRST_KEY));
        assertEquals(MockReceiverAbort.RESULT_EXTRAS_ABORT_VALUE,
                resultExtras.getString(MockReceiverAbort.RESULT_EXTRAS_ABORT_KEY));
    }

    public void testReceiverNotExported() throws Exception {
        final Context context = getInstrumentation().getContext();
        context.sendBroadcast(new Intent(ACTION_TEST_NOT_EXPORTED));
        try {
            assertFalse(COUNT_DOWN_LATCH.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail("interrupted");
        }
    }

    public void testDisabledBroadcastReceiver() throws Exception {
        final Context context = getInstrumentation().getContext();
        PackageManager pm = context.getPackageManager();

        MockReceiverInternalVerifyUncalled lastReceiver =
                new MockReceiverInternalVerifyUncalled(RESULT_INITIAL_CODE);
        assertEquals(0, lastReceiver.getResultCode());

        pm.setComponentEnabledSetting(DISABLEABLE_RECEIVER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        context.sendOrderedBroadcast(
                new Intent(ACTION_BROADCAST_DISABLED).addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
                null, lastReceiver,
                null, RESULT_INITIAL_CODE, RESULT_INITIAL_DATA, new Bundle());
        lastReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);

        assertEquals(RESULT_INTERNAL_FINAL_CODE, lastReceiver.getResultCode());
    }

    public void testPeekService() throws InterruptedException {
        final MockActivity activity = getActivity();

        MockReceiverInternal internalReceiver = new MockReceiverInternal();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BROADCAST_INTERNAL);
        activity.registerReceiver(internalReceiver, filter, RECEIVER_EXPORTED);

        activity.sendBroadcast(new Intent(ACTION_BROADCAST_INTERNAL)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND));
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);
        assertNull(internalReceiver.getIBinder());

        Intent intent = new Intent(activity, MockService.class);
        MyServiceConnection msc = new MyServiceConnection();
        assertTrue(activity.bindService(intent, msc, Service.BIND_AUTO_CREATE));
        assertTrue(msc.waitForService(START_SERVICE_TIMEOUT));

        internalReceiver.reset();
        activity.sendBroadcast(new Intent(ACTION_BROADCAST_INTERNAL)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND));
        internalReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);
        assertNotNull(internalReceiver.getIBinder());
        activity.unbindService(msc);
        activity.stopService(intent);
        activity.unregisterReceiver(internalReceiver);
    }

    public void testAsync() throws Exception {
        final MockActivity activity = getActivity();

        final MockAsyncReceiver asyncReceiver = new MockAsyncReceiver();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BROADCAST_INTERNAL);
        activity.registerReceiver(asyncReceiver, filter, RECEIVER_EXPORTED);

        final Intent intent = new Intent(ACTION_BROADCAST_INTERNAL)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        final MockReceiverInternal resultReceiver = new MockReceiverInternal();
        activity.sendOrderedBroadcast(intent, null, resultReceiver, null, 24, null, null);

        final PendingResult res = asyncReceiver.pendingResult.get(SEND_BROADCAST_TIMEOUT,
                TimeUnit.MILLISECONDS);
        res.setResultCode(42);
        res.finish();

        resultReceiver.waitForReceiver(SEND_BROADCAST_TIMEOUT);
        assertEquals(42, resultReceiver.getResultCode());
    }

    public void testNewPhotoBroadcast_notReceived() throws InterruptedException {
        final MockActivity activity = getActivity();
        MockReceiverInternal internalReceiver = new MockReceiverInternal();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Camera.ACTION_NEW_PICTURE);
        activity.registerReceiver(internalReceiver, filter, RECEIVER_EXPORTED);
        assertFalse(internalReceiver.waitForReceiverNoException(SEND_BROADCAST_TIMEOUT));
    }

    public void testNewVideoBroadcast_notReceived() throws InterruptedException {
        final MockActivity activity = getActivity();
        MockReceiverInternal internalReceiver = new MockReceiverInternal();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Camera.ACTION_NEW_VIDEO);
        activity.registerReceiver(internalReceiver, filter, RECEIVER_EXPORTED);
        assertFalse(internalReceiver.waitForReceiverNoException(SEND_BROADCAST_TIMEOUT));
    }

    /**
     * Starting in {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE}, the
     * developer-visible behavior of {@link BroadcastReceiver} has been revised
     * to support features like
     * {@link BroadcastOptions#DEFERRAL_POLICY_UNTIL_ACTIVE}.
     * <p>
     * Even though the OS provides both a "modern" and "legacy" implementation
     * that can be temporarily switched via a feature flag to aid debugging and
     * triage, all devices must ship with the "modern" stack enabled to match
     * documented developer expectations.
     */
    @CddTest(requirements = {"3.5/C-0-2"})
    public void testModern() throws Exception {
        final ActivityManagerServiceDumpBroadcastsProto dump = dumpBroadcasts();
        final String msg = "Devices must ship with the modern broadcast queue "
                + "that has updated behaviors and APIs that developers rely upon";

        assertEquals(msg, 1, dump.broadcastQueue.length);
        assertEquals(msg, "modern", dump.broadcastQueue[0].queueName);
    }

    private static ActivityManagerServiceDumpBroadcastsProto dumpBroadcasts() throws Exception {
        return ActivityManagerServiceDumpBroadcastsProto
                .parseFrom(executeShellCommand("dumpsys activity --proto broadcasts"));
    }

    private static byte[] executeShellCommand(String cmd) throws Exception {
        ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand(cmd);
        try (FileInputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FileUtils.copy(in, out);
            return out.toByteArray();
        }
    }

    static class MyServiceConnection implements ServiceConnection {
        private boolean serviceConnected;

        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            serviceConnected = true;
            notifyAll();
        }

        public synchronized void onServiceDisconnected(ComponentName name) {
        }

        public synchronized boolean waitForService(long timeout) {
            if (!serviceConnected) {
                try {
                    wait(timeout);
                } catch (InterruptedException ignored) {
                    // ignored
                }
            }
            return serviceConnected;
        }
    }
}
