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
package com.android.compatibility.common.util;

public class AmUtils {
    private static final String TAG = "CtsAmUtils";

    private static final String DUMPSYS_ACTIVITY_PROCESSES = "dumpsys activity --proto processes";

    public static int STANDBY_BUCKET_DOES_NOT_EXIST = -1;

    private AmUtils() {
    }

    /** Run "adb shell am make-uid-idle PACKAGE" */
    public static void runMakeUidIdle(String packageName) {
        SystemUtil.runShellCommandForNoOutput("am make-uid-idle " + packageName);
    }

    /** Run "adb shell am kill PACKAGE" */
    public static void runKill(String packageName) throws Exception {
        runKill(packageName, false /* wait */);
    }

    public static void runKill(String packageName, boolean wait) throws Exception {
        SystemUtil.runShellCommandForNoOutput("am kill --user cur " + packageName);

        if (!wait) {
            return;
        }

        TestUtils.waitUntil("package process was not killed:" + packageName,
                () -> !isProcessRunning(packageName));
    }

    private static boolean isProcessRunning(String packageName) {
        final String output = SystemUtil.runShellCommand("ps -A -o NAME");
        String[] packages = output.split("\\n");
        for (int i = packages.length -1; i >=0; --i) {
            if (packages[i].equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /** Run "adb shell am set-standby-bucket" */
    public static void setStandbyBucket(String packageName, int value) {
        SystemUtil.runShellCommandForNoOutput("am set-standby-bucket " + packageName
                + " " + value);
    }

    /**
     * Run "adb shell am get-standby-bucket",
     * return #STANDBY_BUCKET_DOES_NOT_EXIST for invalid packages
     * */
    public static int getStandbyBucket(String packageName) {
        final String value = SystemUtil.runShellCommand("am get-standby-bucket " + packageName);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException nfe) {
            return STANDBY_BUCKET_DOES_NOT_EXIST;
        }
    }

    /** Wait until all broad queues are idle. */
    public static void waitForBroadcastIdle() {
        SystemUtil.runCommandAndPrintOnLogcat(TAG, "am wait-for-broadcast-idle");
    }

    /** Wait until all broad queues have passed barrier. */
    public static void waitForBroadcastBarrier() {
        SystemUtil.runCommandAndPrintOnLogcat(TAG, "am wait-for-broadcast-barrier");
    }
}
