/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.InstrumentationRegistry;

import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

public final class CtsMouseUtil {

    // TODO(b/272376728): make it an instance object instead
    private static final UserHelper sUserHelper = new UserHelper(
            InstrumentationRegistry.getInstrumentation().getTargetContext());

    private CtsMouseUtil() {}

    public static View.OnHoverListener installHoverListener(View view) {
        return installHoverListener(view, true);
    }

    public static View.OnHoverListener installHoverListener(View view, boolean result) {
        final View.OnHoverListener mockListener = mock(View.OnHoverListener.class);
        view.setOnHoverListener((v, event) -> {
            // Clone the event to work around event instance reuse in the framework.
            mockListener.onHover(v, MotionEvent.obtain(event));
            return result;
        });
        return mockListener;
    }

    public static void clearHoverListener(View view) {
        view.setOnHoverListener(null);
    }

    public static MotionEvent obtainMouseEvent(int action, View anchor, int offsetX, int offsetY) {
        final long eventTime = SystemClock.uptimeMillis();
        final int[] screenPos = new int[2];
        anchor.getLocationOnScreen(screenPos);
        final int x = screenPos[0] + offsetX;
        final int y = screenPos[1] + offsetY;
        MotionEvent event = MotionEvent.obtain(eventTime, eventTime, action, x, y, 0);
        sUserHelper.injectDisplayIdIfNeeded(event);
        event.setSource(InputDevice.SOURCE_MOUSE);
        return event;
    }

    /**
     * Emulates a hover move on a point relative to the top-left corner of the passed {@link View}.
     * Offset parameters are used to compute the final screen coordinates of the tap point.
     *
     * @param instrumentation the instrumentation used to run the test
     * @param anchor the anchor view to determine the tap location on the screen
     * @param offsetX extra X offset for the move
     * @param offsetY extra Y offset for the move
     */
    public static void emulateHoverOnView(Instrumentation instrumentation, View anchor, int offsetX,
            int offsetY) {
        final long downTime = SystemClock.uptimeMillis();
        final int[] screenPos = new int[2];
        anchor.getLocationOnScreen(screenPos);
        final int x = screenPos[0] + offsetX;
        final int y = screenPos[1] + offsetY;
        injectHoverEvent(instrumentation, downTime, x, y);
    }

    private static void injectHoverEvent(Instrumentation instrumentation, long downTime,
            int xOnScreen, int yOnScreen) {
        MotionEvent event = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_HOVER_MOVE,
                xOnScreen, yOnScreen, 0);
        sUserHelper.injectDisplayIdIfNeeded(event);
        event.setSource(InputDevice.SOURCE_MOUSE);
        instrumentation.sendPointerSync(event);
        event.recycle();
    }

    public static class ActionMatcher implements ArgumentMatcher<MotionEvent> {
        private final int mAction;

        public ActionMatcher(int action) {
            mAction = action;
        }

        @Override
        public boolean matches(MotionEvent actual) {
            return actual.getAction() == mAction;
        }

        @Override
        public String toString() {
            return "action=" + MotionEvent.actionToString(mAction);
        }
    }

    public static class PositionMatcher extends ActionMatcher {
        private final int mX;
        private final int mY;

        public PositionMatcher(int action, int x, int y) {
            super(action);
            mX = x;
            mY = y;
        }

        @Override
        public boolean matches(MotionEvent actual) {
            return super.matches(actual)
                    && Math.round(actual.getX()) == mX
                    && Math.round(actual.getY()) == mY;
        }

        @Override
        public String toString() {
            return super.toString() + "@(" + mX + "," + mY + ")";
        }
    }

    public static void verifyEnterMove(View.OnHoverListener listener, View view, int moveCount) {
        final InOrder inOrder = inOrder(listener);
        verifyEnterMoveInternal(listener, view, moveCount, inOrder);
        inOrder.verifyNoMoreInteractions();
    }

    public static void verifyEnterMoveExit(
            View.OnHoverListener listener, View view, int moveCount) {
        final InOrder inOrder = inOrder(listener);
        verifyEnterMoveInternal(listener, view, moveCount, inOrder);
        inOrder.verify(listener, times(1)).onHover(eq(view),
                argThat(new ActionMatcher(MotionEvent.ACTION_HOVER_EXIT)));
        inOrder.verifyNoMoreInteractions();
    }

    private static void verifyEnterMoveInternal(
            View.OnHoverListener listener, View view, int moveCount, InOrder inOrder) {
        inOrder.verify(listener, times(1)).onHover(eq(view),
                argThat(new ActionMatcher(MotionEvent.ACTION_HOVER_ENTER)));
        inOrder.verify(listener, times(moveCount)).onHover(eq(view),
                argThat(new ActionMatcher(MotionEvent.ACTION_HOVER_MOVE)));
    }
}

