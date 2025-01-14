/**
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.accessibilityservice.cts.utils;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.both;

import android.app.UiAutomation;
import android.app.UiAutomation.AccessibilityEventFilter;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.NonNull;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Utility class for creating AccessibilityEventFilters
 */
public class AccessibilityEventFilterUtils {
    public static AccessibilityEventFilter filterForEventType(int eventType) {
        return (new AccessibilityEventTypeMatcher(eventType))::matches;
    }

    public static AccessibilityEventFilter filterWindowContentChangedWithChangeTypes(int changes) {
        return (both(new AccessibilityEventTypeMatcher(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)).and(
                        new ContentChangesMatcher(changes)))::matches;
    }

    public static AccessibilityEventFilter filterWindowsChangedWithChangeTypes(int changes) {
        return (both(new AccessibilityEventTypeMatcher(AccessibilityEvent.TYPE_WINDOWS_CHANGED))
                        .and(new WindowChangesMatcher(changes)))::matches;
    }

    public static AccessibilityEventFilter filterForEventTypeWithResource(int eventType,
            String ResourceName) {
        TypeSafeMatcher<AccessibilityEvent> matchResourceName = new PropertyMatcher<>(
                ResourceName, "Resource name",
                (event, expect) -> event.getSource() != null
                        && event.getSource().getViewIdResourceName().equals(expect));
        return (both(new AccessibilityEventTypeMatcher(eventType)).and(matchResourceName))::matches;
    }

    public static AccessibilityEventFilter filterForEventTypeWithAction(int eventType, int action) {
        TypeSafeMatcher<AccessibilityEvent> matchAction =
                new PropertyMatcher<>(
                        action, "Action", (event, expect) -> event.getAction() == action);
        return (both(new AccessibilityEventTypeMatcher(eventType)).and(matchAction))::matches;
    }

    public static AccessibilityEventFilter filterWindowsChangeTypesAndWindowTitle(
            @NonNull UiAutomation uiAutomation, int changeTypes, @NonNull String title) {
        return allOf(new AccessibilityEventTypeMatcher(AccessibilityEvent.TYPE_WINDOWS_CHANGED),
                new WindowChangesMatcher(changeTypes),
                new WindowTitleMatcher(uiAutomation, title))::matches;
    }

    public static AccessibilityEventFilter filterWindowsChangTypesAndWindowId(int windowId,
            int changeTypes) {
        return allOf(new AccessibilityEventTypeMatcher(AccessibilityEvent.TYPE_WINDOWS_CHANGED),
                new WindowChangesMatcher(changeTypes),
                new WindowIdMatcher(windowId))::matches;
    }

    /**
     * Creates an {@link AccessibilityEventFilter} that returns {@code true} once all the given
     * filters return {@code true} for any event.
     * Each given filters are invoked on every AccessibilityEvent until it returns {@code true}.
     * After all filters return {@code true} once, the created filter returns {@code true} forever.
     */
    public static AccessibilityEventFilter filterWaitForAll(AccessibilityEventFilter... filters) {
        return new AccessibilityEventFilter() {
            private final List<AccessibilityEventFilter> mUnresolved =
                    new LinkedList<>(Arrays.asList(filters));

            @Override
            public boolean accept(AccessibilityEvent event) {
                mUnresolved.removeIf(filter -> filter.accept(event));
                return mUnresolved.isEmpty();
            }
        };
    }

    /**
     * Returns a matcher for a display id from getDisplayId().
     * @param displayId the display id to match.
     * @return a matcher for comparing display ids.
     */
    public static TypeSafeMatcher<AccessibilityEvent> matcherForDisplayId(int displayId) {
        final TypeSafeMatcher<AccessibilityEvent> matchAction =
                new PropertyMatcher<>(
                        displayId, "Display id",
                        (event, expect) -> event.getDisplayId() == displayId);
        return matchAction;
    }

    /**
     * Returns a matcher for a class name from getClassName().
     * @param className the class name to match.
     * @return a matcher for comparing class names.
     */
    public static TypeSafeMatcher<AccessibilityEvent> matcherForClassName(CharSequence className) {
        final TypeSafeMatcher<AccessibilityEvent> matchAction =
                new PropertyMatcher<>(
                        className, "Class name",
                        (event, expect) -> event.getClassName().equals(className));
        return matchAction;
    }

    /**
     * Returns a matcher for the first text instance from getText().
     * @param text the text to match.
     * @return a matcher for comparing first text instances.
     */
    public static TypeSafeMatcher<AccessibilityEvent> matcherForFirstText(CharSequence text) {
        final TypeSafeMatcher<AccessibilityEvent> matchAction =
                new PropertyMatcher<>(
                        text, "Text",
                        (event, expect) -> {
                            if (event.getText() != null && event.getText().size() > 0) {
                                return event.getText().get(0).equals(text);
                            }
                            return false;
                        });
        return matchAction;
    }

    public static class AccessibilityEventTypeMatcher extends TypeSafeMatcher<AccessibilityEvent> {
        private int mType;

        public AccessibilityEventTypeMatcher(int type) {
            super();
            mType = type;
        }

        @Override
        protected boolean matchesSafely(AccessibilityEvent event) {
            return event.getEventType() == mType;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Matching to type " + mType);
        }
    }

    public static class WindowChangesMatcher extends TypeSafeMatcher<AccessibilityEvent> {
        private int mWindowChanges;

        public WindowChangesMatcher(int windowChanges) {
            super();
            mWindowChanges = windowChanges;
        }

        @Override
        protected boolean matchesSafely(AccessibilityEvent event) {
            return (event.getWindowChanges() & mWindowChanges) == mWindowChanges;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("With window change type " + mWindowChanges);
        }
    }

    public static class ContentChangesMatcher extends TypeSafeMatcher<AccessibilityEvent> {
        private int mContentChanges;

        public ContentChangesMatcher(int contentChanges) {
            super();
            mContentChanges = contentChanges;
        }

        @Override
        protected boolean matchesSafely(AccessibilityEvent event) {
            return (event.getContentChangeTypes() & mContentChanges) == mContentChanges;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("With content change type " + mContentChanges);
        }
    }

    public static class PropertyMatcher<T> extends TypeSafeMatcher<AccessibilityEvent> {
        private T mProperty;
        private String mDescription;
        private BiPredicate<AccessibilityEvent, T> mComparator;

        public PropertyMatcher(T property, String description,
                BiPredicate<AccessibilityEvent, T> comparator) {
            super();
            mProperty = property;
            mDescription = description;
            mComparator = comparator;
        }

        @Override
        protected boolean matchesSafely(AccessibilityEvent event) {
            return mComparator.test(event, mProperty);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Matching to " + mDescription + " " + mProperty.toString());
        }
    }

    public static class WindowTitleMatcher extends TypeSafeMatcher<AccessibilityEvent> {
        private final UiAutomation mUiAutomation;
        private final String mTitle;

        public WindowTitleMatcher(@NonNull UiAutomation uiAutomation, @NonNull String title) {
            super();
            mUiAutomation = uiAutomation;
            mTitle = title;
        }

        @Override
        protected boolean matchesSafely(AccessibilityEvent event) {
            final List<AccessibilityWindowInfo> windows = mUiAutomation.getWindows();
            final int eventWindowId = event.getWindowId();
            for (AccessibilityWindowInfo info : windows) {
                if (eventWindowId == info.getId() && mTitle.equals(info.getTitle())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("With window title " + mTitle);
        }
    }

    public static class WindowIdMatcher extends TypeSafeMatcher<AccessibilityEvent> {
        private int mWindowId;

        public WindowIdMatcher(int windowId) {
            super();
            mWindowId = windowId;
        }

        @Override
        protected boolean matchesSafely(AccessibilityEvent event) {
            return event.getWindowId() == mWindowId;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("With window Id " + mWindowId);
        }
    }
}
