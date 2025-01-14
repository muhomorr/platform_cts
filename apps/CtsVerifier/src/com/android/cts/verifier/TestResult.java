/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier;

import static com.android.cts.verifier.TestListActivity.sCurrentDisplayMode;
import static com.android.cts.verifier.TestListAdapter.removeTestNameSuffix;
import static com.android.cts.verifier.TestListAdapter.setTestNameSuffix;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.android.compatibility.common.util.ReportLog;
import com.android.cts.verifier.TestListActivity.DisplayMode;

/**
 * Object representing the result of a test activity like whether it succeeded or failed.
 * Use {@link #setPassedResult(Activity, String, String)} or
 * {@link #setFailedResult(Activity, String, String)} from a test activity like you would
 * {@link Activity#setResult(int)} so that {@link TestListActivity}
 * will persist the test result and update its adapter and thus the list view.
 */
public class TestResult {

    private static final String TAG = TestResult.class.getSimpleName();

    public static final int TEST_RESULT_NOT_EXECUTED = 0;
    public static final int TEST_RESULT_PASSED = 1;
    public static final int TEST_RESULT_FAILED = 2;
    public static final String TEST_START_TIME = "start_time";

    private static final String TEST_NAME = "name";
    private static final String TEST_RESULT = "result";
    private static final String TEST_DETAILS = "details";
    private static final String TEST_METRICS = "metrics";
    private static final String TEST_HISTORY_COLLECTION = "historyCollection";

    private final String mName;
    private final int mResult;
    private final String mDetails;
    private final ReportLog mReportLog;
    private final TestResultHistoryCollection mHistoryCollection;

    /** Sets the test activity's result to pass. */
    public static void setPassedResult(Activity activity, String testId, String testDetails) {
        setPassedResult(activity, testId, testDetails, null /*reportLog*/);
    }

    /** Sets the test activity's result to pass including a test report log result. */
    public static void setPassedResult(Activity activity, String testId, String testDetails,
            ReportLog reportLog) {
        setPassedResult(activity, testId, testDetails, reportLog, null /*history*/);
    }

    /** Sets the test activity's result to pass including a test report log result and history. */
    public static void setPassedResult(Activity activity, String testId, String testDetails,
            ReportLog reportLog, TestResultHistoryCollection historyCollection) {
        Log.i(TAG, "setPassedResult(activity=" + activity + ", testId=" + testId
                + ", testDetails=" + testDetails);

        // We store results here straight into the content provider so it can be fetched by the
        // CTSInteractive host
        TestResultsProvider.setTestResult(
                activity, testId, 1, testDetails, reportLog, historyCollection,
                null);

        activity.setResult(Activity.RESULT_OK, createResult(activity, TEST_RESULT_PASSED, testId,
                testDetails, reportLog, historyCollection));
    }

    /** Sets the test activity's result to failed. */
    public static void setFailedResult(Activity activity, String testId, String testDetails) {
        setFailedResult(activity, testId, testDetails, null /*reportLog*/);
    }

    /** Sets the test activity's result to failed including a test report log result. */
    public static void setFailedResult(Activity activity, String testId, String testDetails,
            ReportLog reportLog) {
        setFailedResult(activity, testId, testDetails, reportLog, null /*history*/);
    }

    /** Sets the test activity's result to failed including a test report log result and history. */
    public static void setFailedResult(Activity activity, String testId, String testDetails,
            ReportLog reportLog, TestResultHistoryCollection historyCollection) {
        Log.e(TAG, "setFailedResult(activity=" + activity + ", testId=" + testId
                + ", testDetails=" + testDetails);

        // We store results here straight into the content provider so it can be fetched by the
        // CTSInteractive host
        TestResultsProvider.setTestResult(
                activity, testId, 2, testDetails, reportLog, historyCollection,
                null);

        activity.setResult(Activity.RESULT_OK, createResult(activity, TEST_RESULT_FAILED, testId,
                testDetails, reportLog, historyCollection));
    }

    public static Intent createResult(Activity activity, int testResult, String testName,
            String testDetails, ReportLog reportLog, TestResultHistoryCollection historyCollection) {
        Intent activityIntent = activity.getIntent();
        Intent data = new Intent(activity, activity.getClass());
        if (activityIntent.hasExtra(TEST_START_TIME)) {
            data.putExtra(TEST_START_TIME, activityIntent.getLongExtra(TEST_START_TIME, 0));
        }
        addResultData(data, testResult, testName, testDetails, reportLog, historyCollection);
        return data;
    }

    public static void addResultData(Intent intent, int testResult, String testName,
            String testDetails, ReportLog reportLog, TestResultHistoryCollection historyCollection) {
        testName = setTestNameSuffix(sCurrentDisplayMode, testName);
        intent.putExtra(TEST_NAME, testName);
        intent.putExtra(TEST_RESULT, testResult);
        intent.putExtra(TEST_DETAILS, testDetails);
        intent.putExtra(TEST_METRICS, reportLog);
        intent.putExtra(TEST_HISTORY_COLLECTION, historyCollection);
    }

    /**
     * Convert the test activity's result into a {@link TestResult}. Only meant to be used by
     * {@link TestListActivity}.
     */
    static TestResult fromActivityResult(int resultCode, Intent data) {
        String name = setTestNameSuffix(sCurrentDisplayMode, data.getStringExtra(TEST_NAME));
        return fromActivityResultWithTestName(resultCode, data, name);
    }

    /**
     * Convert the test activity's result into a {@link TestResult} with the given test name.
     * Only meant to be used by {@link TestResult}.
     */
    static TestResult fromActivityResultWithTestName(
            int resultCode, Intent data, String name) {
        int result = data.getIntExtra(TEST_RESULT, TEST_RESULT_NOT_EXECUTED);
        String details = data.getStringExtra(TEST_DETAILS);
        ReportLog reportLog = (ReportLog) data.getSerializableExtra(TEST_METRICS);
        TestResultHistoryCollection historyCollection =
                (TestResultHistoryCollection) data.getSerializableExtra(TEST_HISTORY_COLLECTION);
        return new TestResult(name, result, details, reportLog, historyCollection);
    }

    /**
     * Convert the test activity's result into a {@link TestResult} with the given display mode.
     * Only meant to be used by {@link TestListActivity}.
     */
    static TestResult fromActivityResultWithDisplayMode(
            int resultCode, Intent data, String mode) {
        if (mode.equalsIgnoreCase(DisplayMode.UNFOLDED.toString())) {
            return fromActivityResultWithTestName(
                    resultCode,
                    data,
                    removeTestNameSuffix(mode, data.getStringExtra(TEST_NAME))
            );
        }
        return fromActivityResultWithTestName(
                resultCode,
                data,
                setTestNameSuffix(mode, data.getStringExtra(TEST_NAME))
        );
    }

    private TestResult(
            String name, int result, String details, ReportLog reportLog,
            TestResultHistoryCollection historyCollection) {
        this.mName = name;
        this.mResult = result;
        this.mDetails = details;
        this.mReportLog = reportLog;
        this.mHistoryCollection =
            historyCollection == null ? new TestResultHistoryCollection() : historyCollection;
    }

    /** Return the name of the test like "com.android.cts.verifier.foo.FooTest" */
    public String getName() {
        return mName;
    }

    /** Return integer test result. See test result constants. */
    public int getResult() {
        return mResult;
    }

    /** Return null or string containing test output. */
    public String getDetails() {
        return mDetails;
    }

    /** @return the {@link ReportLog} or null if not set */
    public ReportLog getReportLog() {
        return mReportLog;
    }

    /** @return the {@link TestResultHistoryCollection} containing test history */
    public TestResultHistoryCollection getHistoryCollection() {
        return mHistoryCollection;
    }
}
