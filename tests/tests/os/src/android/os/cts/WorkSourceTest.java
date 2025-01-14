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
 * limitations under the License.
 */
package android.os.cts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.WorkSource;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.ApiTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class WorkSourceTest {
    private WorkSource wsNew(int uid) {
        return new WorkSource(uid);
    }

    private WorkSource wsNew(int[] uids) {
        WorkSource ws = new WorkSource();
        for (int i=0; i<uids.length; i++) {
            ws.add(uids[i]);
        }
        checkWorkSource("Constructed", ws, uids);
        return ws;
    }

    private WorkSource wsNew(int[] uids, String[] names) {
        WorkSource ws = new WorkSource();
        for (int i=0; i<uids.length; i++) {
            ws.add(uids[i], names[i]);
        }
        checkWorkSource("Constructed", ws, uids, names);
        return ws;
    }

    private WorkSource wsAddReturningNewbs(WorkSource ws, WorkSource other) {
        return ws.addReturningNewbs(other);
    }

    private WorkSource[] wsSetReturningDiffs(WorkSource ws, WorkSource other) {
        return ws.setReturningDiffs(other);
    }

    private void printArrays(StringBuilder sb, int[] uids, String[] names) {
        sb.append("{ ");
        for (int i=0; i<uids.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(uids[i]);
            if (names != null) {
                sb.append(" ");
                sb.append(names[i]);
            }
        }
        sb.append(" }");
    }

    private void failWorkSource(String op, WorkSource ws, int[] uids) {
        StringBuilder sb = new StringBuilder();
        sb.append(op);
        sb.append(": Expected: ");
        printArrays(sb, uids, null);
        sb.append(", got: ");
        sb.append(ws);
        fail(sb.toString());
    }

    private void failWorkSource(String op, WorkSource ws, int[] uids, String[] names) {
        StringBuilder sb = new StringBuilder();
        sb.append(op);
        sb.append(": Expected: ");
        printArrays(sb, uids, names);
        sb.append(", got: ");
        sb.append(ws);
        fail(sb.toString());
    }

    private void checkWorkSource(String op, WorkSource ws, int[] uids) {
        if (ws == null || uids == null) {
            if (ws != null) {
                fail(op + ": WorkSource is not null " + ws +", but expected null");
            }
            if (uids != null) {
                fail(op + "WorkSource is null, but expected non-null: " + Arrays.toString(uids));
            }
            return;
        }
        if (ws.size() != uids.length) {
            failWorkSource(op, ws, uids);
        }
        for (int i=0; i<uids.length; i++) {
            if (uids[i] != ws.getUid(i)) {
                failWorkSource(op, ws, uids);
            }
        }
    }

    private void checkWorkSource(String op, WorkSource ws, int[] uids, String[] names) {
        if (ws == null || uids == null) {
            if (ws != null) {
                fail(op + ": WorkSource is not null " + ws +", but expected null");
            }
            if (uids != null) {
                fail(op + "WorkSource is null, but expected non-null: " + Arrays.toString(uids));
            }
            return;
        }
        if (ws.size() != uids.length) {
            failWorkSource(op, ws, uids, names);
        }
        for (int i=0; i<uids.length; i++) {
            if (uids[i] != ws.getUid(i) || !names[i].equals(ws.getPackageName(i))) {
                failWorkSource(op, ws, uids, names);
            }
        }
    }

    @Test
    public void testConstructEmpty() {
        checkWorkSource("Empty", new WorkSource(), new int[] { });
    }

    @Test
    public void testConstructSingle() throws Exception {
        checkWorkSource("Single 1", wsNew(1), new int[] { 1 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testAddRawOrdered() throws Exception {
        WorkSource ws = wsNew(1);
        ws.add(2);
        checkWorkSource("First", ws, new int[] { 1 , 2 });
        ws.add(20);
        checkWorkSource("Second", ws, new int[] { 1 , 2, 20 });
        ws.add(100);
        checkWorkSource("Third", ws, new int[] { 1, 2, 20, 100 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testAddRawRevOrdered() throws Exception {
        WorkSource ws = wsNew(100);
        ws.add(20);
        checkWorkSource("First", ws, new int[] { 20, 100 });
        ws.add(2);
        checkWorkSource("Second", ws, new int[] { 2, 20, 100 });
        ws.add(1);
        checkWorkSource("Third", ws, new int[] { 1, 2, 20, 100 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testAddRawUnordered() throws Exception {
        WorkSource ws = wsNew(10);
        ws.add(2);
        checkWorkSource("First", ws, new int[] { 2, 10 });
        ws.add(5);
        checkWorkSource("Second", ws, new int[] { 2, 5, 10 });
        ws.add(1);
        checkWorkSource("Third", ws, new int[] { 1, 2, 5, 10 });
        ws.add(100);
        checkWorkSource("Fourth", ws, new int[] { 1, 2, 5, 10, 100 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testAddWsOrdered() throws Exception {
        WorkSource ws = wsNew(1);
        ws.add(wsNew(2));
        checkWorkSource("First", ws, new int[] { 1 , 2 });
        ws.add(wsNew(20));
        checkWorkSource("Second", ws, new int[] { 1 , 2, 20 });
        ws.add(wsNew(100));
        checkWorkSource("Third", ws, new int[] { 1 , 2, 20, 100 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testAddWsRevOrdered() throws Exception {
        WorkSource ws = wsNew(100);
        ws.add(wsNew(20));
        checkWorkSource("First", ws, new int[] { 20, 100 });
        ws.add(wsNew(2));
        checkWorkSource("Second", ws, new int[] { 2, 20, 100 });
        ws.add(wsNew(1));
        checkWorkSource("Third", ws, new int[] { 1, 2, 20, 100 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testAddWsUnordered() throws Exception {
        WorkSource ws = wsNew(10);
        ws.add(wsNew(2));
        checkWorkSource("First", ws, new int[] { 2, 10 });
        ws.add(wsNew(5));
        checkWorkSource("Second", ws, new int[] { 2, 5, 10 });
        ws.add(wsNew(1));
        checkWorkSource("Third", ws, new int[] { 1, 2, 5, 10 });
        ws.add(wsNew(100));
        checkWorkSource("Fourth", ws, new int[] { 1, 2, 5, 10, 100 });
    }

    private void doTestCombineTwoUids(int[] lhs, int[] rhs, int[] expected, int[] newbs,
            int[] gones) throws Exception {
        WorkSource ws1 = wsNew(lhs);
        WorkSource ws2 = wsNew(rhs);
        ws1.add(ws2);
        checkWorkSource("Add result", ws1, expected);
        ws1 = wsNew(lhs);
        WorkSource wsNewbs = wsAddReturningNewbs(ws1, ws2);
        checkWorkSource("AddReturning result", ws1, expected);
        checkWorkSource("Newbs", wsNewbs, newbs);
        ws1 = wsNew(lhs);
        WorkSource[] res = wsSetReturningDiffs(ws1, ws2);
        checkWorkSource("SetReturning result", ws1, rhs);
        checkWorkSource("Newbs", res[0], newbs);
        checkWorkSource("Gones", res[1], gones);
    }

    private int[] makeRepeatingIntArray(String[] stringarray, int value) {
        if (stringarray == null) {
            return null;
        }
        int[] res = new int[stringarray.length];
        for (int i=0; i<stringarray.length; i++) {
            res[i] = value;
        }
        return res;
    }

    private void doTestCombineTwoNames(String[] lhsnames, String[] rhsnames,
            String[] expectednames, String[] newbnames,
            String[] gonenames) throws Exception {
        int[] lhs = makeRepeatingIntArray(lhsnames, 0);
        int[] rhs = makeRepeatingIntArray(rhsnames, 0);
        int[] expected = makeRepeatingIntArray(expectednames, 0);
        int[] newbs = makeRepeatingIntArray(newbnames, 0);
        int[] gones = makeRepeatingIntArray(gonenames, 0);
        doTestCombineTwoUidsNames(lhs, lhsnames, rhs, rhsnames, expected, expectednames,
                newbs, newbnames, gones, gonenames);
    }

    private void doTestCombineTwoUidsNames(int[] lhs, String[] lhsnames, int[] rhs, String[] rhsnames,
            int[] expected, String[] expectednames, int[] newbs, String[] newbnames,
            int[] gones, String[] gonenames) throws Exception {
        WorkSource ws1 = wsNew(lhs, lhsnames);
        WorkSource ws2 = wsNew(rhs, rhsnames);
        ws1.add(ws2);
        checkWorkSource("Add result", ws1, expected, expectednames);
        ws1 = wsNew(lhs, lhsnames);
        WorkSource wsNewbs = wsAddReturningNewbs(ws1, ws2);
        checkWorkSource("AddReturning result", ws1, expected, expectednames);
        checkWorkSource("Newbs", wsNewbs, newbs, newbnames);
        ws1 = wsNew(lhs, lhsnames);
        WorkSource[] res = wsSetReturningDiffs(ws1, ws2);
        checkWorkSource("SetReturning result", ws1, rhs, rhsnames);
        checkWorkSource("Newbs", res[0], newbs, newbnames);
        checkWorkSource("Gones", res[1], gones, gonenames);
    }

    private String[] makeRepeatingStringArray(int[] intarray, String value) {
        if (intarray == null) {
            return null;
        }
        String[] res = new String[intarray.length];
        for (int i=0; i<intarray.length; i++) {
            res[i] = value;
        }
        return res;
    }

    private String[] makeStringArray(int[] intarray) {
        if (intarray == null) {
            return null;
        }
        String[] res = new String[intarray.length];
        for (int i=0; i<intarray.length; i++) {
            res[i] = Character.toString((char)('A' + intarray[i]));
        }
        return res;
    }

    private void doTestCombineTwo(int[] lhs, int[] rhs, int[] expected, int[] newbs,
            int[] gones) throws Exception {
        doTestCombineTwoUids(lhs, rhs, expected, newbs, gones);
        doTestCombineTwoUidsNames(lhs, makeRepeatingStringArray(lhs, "A"),
                rhs, makeRepeatingStringArray(rhs, "A"),
                expected, makeRepeatingStringArray(expected, "A"),
                newbs, makeRepeatingStringArray(newbs, "A"),
                gones, makeRepeatingStringArray(gones, "A"));
        doTestCombineTwoNames(makeStringArray(lhs), makeStringArray(rhs),
                makeStringArray(expected), makeStringArray(newbs), makeStringArray(gones));
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiFront() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 15, 16 },
                new int[] { 1, 2, 10, 15, 16, 20, 30, 40 },
                new int[] { 1, 2, 15, 16 },
                new int[] { 10, 20, 30, 40 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiMiddle() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 12, 14, 22 },
                new int[] { 10, 12, 14, 20, 22, 30, 40 },
                new int[] { 12, 14, 22 },
                new int[] { 10, 20, 30, 40 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiEnd() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 35, 45, 50 },
                new int[] { 10, 20, 30, 35, 40, 45, 50 },
                new int[] { 35, 45, 50 },
                new int[] { 10, 20, 30, 40 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiFull() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 15, 35, 50 },
                new int[] { 1, 2, 10, 15, 20, 30, 35, 40, 50 },
                new int[] { 1, 2, 15, 35, 50 },
                new int[] { 10, 20, 30, 40 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiSame() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 10, 20, 30 },
                new int[] { 10, 20, 30, 40 },
                null,
                new int[] { 40 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiSomeSame() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 30, 40 },
                new int[] { 1, 10, 20, 30, 40 },
                new int[] { 1 },
                new int[] { 10, 20 });
    }

    @ApiTest(apis = {"android.os.WorkSource#add"})
    @Test
    public void testCombineMultiSomeSameUidsNames() throws Exception {
        doTestCombineTwoUidsNames(
                new int[]    { 10,  10,  20,  30,  30,  30,  40 },
                new String[] { "A", "B", "A", "A", "B", "C", "A" },
                new int[]    { 1,   30,  40,  50 },
                new String[] { "A", "A", "B", "A" },
                new int[]    { 1,   10,  10,  20,  30,  30,  30,  40,  40,  50 },
                new String[] { "A", "A", "B", "A", "A", "B", "C", "A", "B", "A" },
                new int[]    { 1,   40,  50 },
                new String[] { "A", "B", "A" },
                new int[]    { 10,  10,  20,  30,  30,  40 },
                new String[] { "A", "B", "A", "B", "C", "A" });
    }

    private void doTestRemoveUids(int[] lhs, int[] rhs, int[] result, boolean diff) throws Exception {
        WorkSource ws1 = wsNew(lhs);
        WorkSource ws2 = wsNew(rhs);
        boolean diffres = ws1.remove(ws2);
        if (diffres != diff) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected diff ");
            sb.append(diff);
            sb.append(" but got ");
            sb.append(diffres);
            sb.append(" when removing ");
            sb.append(ws2);
            sb.append(" from ");
            sb.append(ws1);
            fail(sb.toString());
        }
        checkWorkSource("Remove", ws1, result);
    }

    private void doTestRemoveNames(String[] lhsnames, String[] rhsnames,
            String[] resultnames, boolean diff) throws Exception {
        int[] lhs = makeRepeatingIntArray(lhsnames, 0);
        int[] rhs = makeRepeatingIntArray(rhsnames, 0);
        int[] result = makeRepeatingIntArray(resultnames, 0);
        WorkSource ws1 = wsNew(lhs, lhsnames);
        WorkSource ws2 = wsNew(rhs, rhsnames);
        boolean diffres = ws1.remove(ws2);
        if (diffres != diff) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected diff ");
            sb.append(diff);
            sb.append(" but got ");
            sb.append(diffres);
            sb.append(" when removing ");
            sb.append(ws2);
            sb.append(" from ");
            sb.append(ws1);
            fail(sb.toString());
        }
        checkWorkSource("Remove", ws1, result, resultnames);
    }

    private void doTestRemoveUidsNames(int[] lhs, String[] lhsnames, int[] rhs, String[] rhsnames,
            int[] result, String[] resultnames, boolean diff) throws Exception {
        WorkSource ws1 = wsNew(lhs, lhsnames);
        WorkSource ws2 = wsNew(rhs, rhsnames);
        boolean diffres = ws1.remove(ws2);
        if (diffres != diff) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected diff ");
            sb.append(diff);
            sb.append(" but got ");
            sb.append(diffres);
            sb.append(" when removing ");
            sb.append(ws2);
            sb.append(" from ");
            sb.append(ws1);
            fail(sb.toString());
        }
        checkWorkSource("Remove", ws1, result, resultnames);
    }

    private void doTestRemove(int[] lhs, int[] rhs, int[] result, boolean diff) throws Exception {
        doTestRemoveUids(lhs, rhs, result, diff);
        doTestRemoveUidsNames(lhs, makeRepeatingStringArray(lhs, "A"),
                rhs, makeRepeatingStringArray(rhs, "A"),
                result, makeRepeatingStringArray(result, "A"),
                diff);
        doTestRemoveNames(makeStringArray(lhs), makeStringArray(rhs),
                makeStringArray(result), diff);
    }

    @ApiTest(apis = {"android.os.WorkSource#remove"})
    @Test
    public void testRemoveNone() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 35, 50 },
                new int[] { 10, 20, 30, 40 },
                false);
    }

    @ApiTest(apis = {"android.os.WorkSource#remove"})
    @Test
    public void testRemoveMultiFront() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 10, 30 },
                new int[] { 20, 40 },
                true);
    }

    @ApiTest(apis = {"android.os.WorkSource#remove"})
    @Test
    public void testRemoveMultiMiddle() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 20, 30 },
                new int[] { 10, 40 },
                true);
    }

    @ApiTest(apis = {"android.os.WorkSource#remove"})
    @Test
    public void testRemoveMultiEnd() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 30, 40, 50 },
                new int[] { 10, 20 },
                true);
    }

    @ApiTest(apis = {"android.os.WorkSource#remove"})
    @Test
    public void testRemoveMultiFull() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 20, 25, 35, 40 },
                new int[] { 10, 30 },
                true);
    }

    @ApiTest(apis = {"android.os.WorkSource#remove"})
    @Test
    public void testRemoveMultiAll() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 10, 20, 30, 40 },
                new int[] { },
                true);
    }

    @ApiTest(apis = {"android.os.WorkSource#isEmpty"})
    @Test
    public void testIsEmptyByDefault() {
        WorkSource ws = new WorkSource();
        assertTrue("isEmpty false for empty WorkSource", ws.isEmpty());
    }

    @ApiTest(apis = {"android.os.WorkSource#clear"})
    @Test
    public void testIsEmptyOnClear() {
        WorkSource ws = wsNew(new int[] {1, 2, 3}, new String[] {"a", "aa", "aaa"});
        assertFalse(ws.isEmpty());
        ws.clear();
        assertTrue(ws.isEmpty());
    }

    @ApiTest(apis = {"android.os.WorkSource#withoutNames"})
    @Test
    public void testWithoutNames() {
        WorkSource ws = wsNew(
                new int[] {10, 12, 12, 15, 15, 17},
                new String[] {"a", "b", "c", "d", "e", "f"});
        WorkSource wsWithoutNames = ws.withoutNames();

        int[] expectedUids = new int[] {10, 12, 15, 17};
        if (expectedUids.length != wsWithoutNames.size()) {
            failWorkSource("withoutNames", wsWithoutNames, expectedUids);
        }
        for (int i = 0; i < expectedUids.length; i++) {
            if (wsWithoutNames.getUid(i) != expectedUids[i]) {
                failWorkSource("withoutNames", wsWithoutNames, expectedUids);
            }
            if (wsWithoutNames.getPackageName(i) != null) {
                fail("Name " + wsWithoutNames.getPackageName(i) + " found at i = " + i);
            }
        }
        try {
            wsWithoutNames.add(50, "name");
            fail("Added name to unnamed worksource");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
