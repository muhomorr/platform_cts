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

package android.autofillservice.cts;

import static android.autofillservice.cts.activities.GridActivity.ID_L1C1;
import static android.autofillservice.cts.activities.GridActivity.ID_L1C2;
import static android.autofillservice.cts.testcore.Helper.assertEqualsIgnoreSession;
import static android.autofillservice.cts.testcore.Helper.assertTextIsSanitized;
import static android.autofillservice.cts.testcore.Helper.findNodeByResourceId;
import static android.service.autofill.SaveInfo.SAVE_DATA_TYPE_GENERIC;

import static com.google.common.truth.Truth.assertThat;

import static org.testng.Assert.assertThrows;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.autofillservice.cts.activities.GridActivity.FillExpectation;
import android.autofillservice.cts.commontests.AbstractGridActivityTestCase;
import android.autofillservice.cts.testcore.CannedFillResponse;
import android.autofillservice.cts.testcore.CannedFillResponse.CannedDataset;
import android.autofillservice.cts.testcore.InstrumentedAutoFillService.FillRequest;
import android.autofillservice.cts.testcore.InstrumentedAutoFillService.SaveRequest;
import android.service.autofill.FillContext;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.widget.EditText;

import androidx.test.uiautomator.UiObject2;

import org.junit.Test;

import java.util.List;

/**
 * Test cases for the cases where the autofill id of a view is changed by the app.
 */
public class MutableAutofillIdTest extends AbstractGridActivityTestCase {

    private static final String TAG = "MutableAutofillIdTest";

    @Test
    public void testDatasetPickerIsNotShownAfterViewIsSwappedOut() throws Exception {
        enableService();

        final EditText field1 = mActivity.getCell(1, 1);
        final AutofillId oldIdField1 = field1.getAutofillId();

        // Prepare response
        final CannedFillResponse response1 = new CannedFillResponse.Builder()
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_L1C1, "l1c1", createPresentation("l1c1"))
                        .setField(ID_L1C2, "l1c2", createPresentation("l1c2"))
                        .build())
                .build();
        sReplier.addResponse(response1);

        // Trigger autofill on 1st view.
        focusCell(1, 1);
        final FillRequest fillRequest1 = sReplier.getNextFillRequest();
        final ViewNode node1Request1 = assertTextIsSanitized(fillRequest1.structure, ID_L1C1);
        assertEqualsIgnoreSession(node1Request1.getAutofillId(), oldIdField1);
        mUiBot.assertDatasets("l1c1");

        // Make sure 2nd field shows picker
        focusCell(1, 2);
        mUiBot.assertDatasets("l1c2");

        // Now change id of 1st view
        final AutofillId newIdField1 = mActivity.getAutofillManager().getNextAutofillId();

        // TODO: move to an autofill unit test class for View
        // Make sure view has to be detached first
        assertThrows(IllegalStateException.class, () -> field1.setAutofillId(newIdField1));

        // Change id
        mActivity.removeCell(1, 1);

        // TODO: move to an autofill unit test class for View
        // Also assert it does not accept virtual ids
        assertThrows(IllegalStateException.class,
                () -> field1.setAutofillId(new AutofillId(newIdField1, 42)));

        field1.setAutofillId(newIdField1);
        assertThat(field1.getAutofillId()).isEqualTo(newIdField1);

        Log.d(TAG, "Changed id of " + ID_L1C1 + " from " + oldIdField1 + " to " + newIdField1);

        // Trigger another request because 1st view has a different id. Service will ignore it now.
        final CannedFillResponse response2 = new CannedFillResponse.Builder()
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_L1C2, "l1c2", createPresentation("l1c2"))
                        .build())
                .build();
        sReplier.addResponse(response2);

        // Re-add the cell before triggering autofill on it
        mActivity.addCell(1, 1, field1);
        mActivity.focusCell(1, 1);

        final FillRequest fillRequest2 = sReplier.getNextFillRequest();
        final ViewNode node1Request2 = assertTextIsSanitized(fillRequest2.structure, ID_L1C1);
        // Make sure node has new id.
        assertEqualsIgnoreSession(node1Request2.getAutofillId(), newIdField1);
        mUiBot.assertNoDatasets();

        // Make sure 2nd field shows picker
        focusCell(1, 2);
        final UiObject2 datasetPicker = mUiBot.assertDatasets("l1c2");

        // Now autofill
        final FillExpectation expectation = mActivity.expectAutofill()
                .onCell(1, 2, "l1c2");
        mUiBot.selectDataset(datasetPicker, "l1c2");
        expectation.assertAutoFilled();
    }

    @Test
    public void testViewGoneDuringAutofillCanStillBeFilled() throws Exception {
        enableService();

        final EditText field1 = mActivity.getCell(1, 1);
        final AutofillId oldIdField1 = field1.getAutofillId();

        // Prepare response
        final CannedFillResponse response1 = new CannedFillResponse.Builder()
                .addDataset(new CannedDataset.Builder()
                        .setField(ID_L1C1, "l1c1", createPresentation("l1c1"))
                        .setField(ID_L1C2, "l1c2", createPresentation("l1c2"))
                        .build())
                .build();
        sReplier.addResponse(response1);

        // Trigger autofill on 1st view.
        focusCell(1, 1);
        final FillRequest fillRequest1 = sReplier.getNextFillRequest();
        final ViewNode node1Request1 = assertTextIsSanitized(fillRequest1.structure, ID_L1C1);
        assertEqualsIgnoreSession(node1Request1.getAutofillId(), oldIdField1);
        mUiBot.assertDatasets("l1c1");

        // Make sure 2nd field shows picker
        focusCell(1, 2);
        final UiObject2 picker1 = mUiBot.assertDatasets("l1c2");

        // Now change id of 1st view
        final AutofillId newIdField1 = mActivity.getAutofillManager().getNextAutofillId();

        // Change id
        mActivity.removeCell(1, 1);
        field1.setAutofillId(newIdField1);
        assertEqualsIgnoreSession(field1.getAutofillId(), newIdField1);
        Log.d(TAG, "Changed id of " + ID_L1C1 + " from " + oldIdField1 + " to " + newIdField1);

        // Autofill it - just 2nd field should be autofilled
        final FillExpectation expectation1 = mActivity.expectAutofill().onCell(1, 2, "l1c2");
        mUiBot.selectDataset(picker1, "l1c2");
        expectation1.assertAutoFilled();

        // Re-add the cell before triggering autofill on it
        field1.setAutofillId(oldIdField1);
        mActivity.addCell(1, 1, field1);

        // Tap 1st field again - it should be autofillable
        mActivity.focusCell(1, 1);
        final UiObject2 picker2 = mUiBot.assertDatasets("l1c1");

        // Now autofill again
        final FillExpectation expectation2 = mActivity.expectAutofill().onCell(1, 1, "l1c1");
        mUiBot.selectDataset(picker2, "l1c1");
        expectation2.assertAutoFilled();
    }

    @Test
    public void testSave_serviceIgnoresNewId() throws Exception {
        saveWhenIdChanged(true);
    }

    @Test
    public void testSave_serviceExpectingOldId() throws Exception {
        saveWhenIdChanged(false);
    }

    private void saveWhenIdChanged(boolean serviceIgnoresNewId) throws Exception {
        enableService();

        final EditText field1 = mActivity.getCell(1, 1);
        final AutofillId oldIdField1 = field1.getAutofillId();

        // Prepare response
        final CannedFillResponse response1 = new CannedFillResponse.Builder()
                .setRequiredSavableIds(SAVE_DATA_TYPE_GENERIC, ID_L1C1, ID_L1C2)
                .build();
        sReplier.addResponse(response1);

        // Trigger autofill on 1st view.
        mActivity.focusCell(1, 1); // No window change because it's not showing dataset picker.
        final FillRequest fillRequest1 = sReplier.getNextFillRequest();
        final ViewNode node1Request1 = assertTextIsSanitized(fillRequest1.structure, ID_L1C1);
        assertEqualsIgnoreSession(node1Request1.getAutofillId(), oldIdField1);
        mUiBot.assertNoDatasetsEver();

        // Make sure 2nd field doesn't trigger a new request
        mActivity.focusCell(1, 2); // No window change because it's not showing dataset picker.
        mUiBot.assertNoDatasetsEver();

        // Now change 1st view value...
        mActivity.setText(1, 1, "OLD");
        // ...and its id
        final AutofillId newIdField1 = mActivity.getAutofillManager().getNextAutofillId();
        mActivity.removeCell(1, 1);
        field1.setAutofillId(newIdField1);
        assertThat(field1.getAutofillId()).isEqualTo(newIdField1);
        Log.d(TAG, "Changed id of " + ID_L1C1 + " from " + oldIdField1 + " to " + newIdField1);

        // Trigger another request because 1st view has a different id...
        final CannedFillResponse.Builder response2 = new CannedFillResponse.Builder();
        if (serviceIgnoresNewId) {
            // ... and service will ignore it now.
            response2.setRequiredSavableIds(SAVE_DATA_TYPE_GENERIC, ID_L1C2);
        } else {
            // ..but service is still expecting the old id.
            response2.setRequiredSavableIds(SAVE_DATA_TYPE_GENERIC, ID_L1C1, ID_L1C2);
        }
        sReplier.addResponse(response2.build());

        mActivity.addCell(1, 1, field1);
        mActivity.focusCell(1, 1); // No window change because it's not showing dataset picker.
        final FillRequest fillRequest2 = sReplier.getNextFillRequest();
        final ViewNode node1Request2 = assertTextIsSanitized(fillRequest2.structure, ID_L1C1);
        // Make sure node has new id.
        assertEqualsIgnoreSession(node1Request2.getAutofillId(), newIdField1);
        mUiBot.assertNoDatasetsEver();

        // Now triggers save
        mActivity.setText(1, 1, "NEW");
        mActivity.setText(1, 2, "NOD2");
        mActivity.save();

        mUiBot.saveForAutofill(true, SAVE_DATA_TYPE_GENERIC);
        final SaveRequest saveRequest = sReplier.getNextSaveRequest();
        final List<FillContext> contexts = saveRequest.contexts;
        assertThat(contexts).hasSize(2);

        // Assert 1st context
        final AssistStructure structure1 = contexts.get(0).getStructure();

        final ViewNode newNode1Context1 = findNodeByResourceId(structure1, ID_L1C1);
        assertThat(newNode1Context1).isNotNull();
        assertThat(newNode1Context1.getText().toString()).isEqualTo("OLD");

        final ViewNode node2Context1 = findNodeByResourceId(structure1, ID_L1C2);
        assertThat(node2Context1).isNotNull();
        assertThat(node2Context1.getText().toString()).isEqualTo("NOD2");

        // Assert 2nd context
        final AssistStructure structure2 = contexts.get(1).getStructure();

        final ViewNode newNode1Context2 = findNodeByResourceId(structure2, ID_L1C1);
        assertThat(newNode1Context2).isNotNull();
        assertThat(newNode1Context2.getText().toString()).isEqualTo("NEW");

        final ViewNode node2Context2 = findNodeByResourceId(structure2, ID_L1C2);
        assertThat(node2Context2).isNotNull();
        assertThat(node2Context2.getText().toString()).isEqualTo("NOD2");
    }
}
