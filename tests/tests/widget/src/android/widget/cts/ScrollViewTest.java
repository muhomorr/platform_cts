/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.widget.cts;

import static android.widget.cts.util.StretchEdgeUtil.dragAndHoldExecute;
import static android.widget.cts.util.StretchEdgeUtil.fling;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.cts.util.NoReleaseEdgeEffect;
import android.widget.cts.util.StretchEdgeUtil;
import android.widget.cts.util.TestUtils;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.PollingCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

/**
 * Test {@link ScrollView}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ScrollViewTest {
    // view dpi constants. Must match those defined in scroll_view layout
    private static final int ITEM_WIDTH_DPI  = 250;
    private static final int ITEM_HEIGHT_DPI = 100;
    private static final int ITEM_COUNT  = 15;
    private static final int PAGE_WIDTH_DPI  = 100;
    private static final int PAGE_HEIGHT_DPI = 100;
    private static final int TOLERANCE = 2;

    private int mItemWidth;
    private int mItemHeight;
    private int mPageWidth;
    private int mPageHeight;
    private int mScrollBottom;
    private int mScrollRight;

    private Instrumentation mInstrumentation;
    private Activity mActivity;
    private ScrollView mScrollViewRegular;
    private ScrollView mScrollViewCustom;
    private MyScrollView mScrollViewCustomEmpty;
    private MyScrollView mScrollViewStretch;

    private SurfaceView mSurfaceView;
    private float mDurationScale = 1f;

    @Rule
    public ActivityTestRule<ScrollViewCtsActivity> mActivityRule =
            new ActivityTestRule<>(ScrollViewCtsActivity.class);

    @Before
    public void setup() {
        mDurationScale = ValueAnimator.getDurationScale();
        ValueAnimator.setDurationScale(1f);
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityRule.getActivity();
        mScrollViewRegular = (ScrollView) mActivity.findViewById(R.id.scroll_view_regular);
        mScrollViewCustom = (ScrollView) mActivity.findViewById(R.id.scroll_view_custom);
        mScrollViewCustomEmpty = (MyScrollView) mActivity.findViewById(
                R.id.scroll_view_custom_empty);
        mScrollViewStretch = (MyScrollView) mActivity.findViewById(R.id.scroll_view_stretch);
        mSurfaceView = mActivity.findViewById(R.id.surfaceview_stretch_target);

        // calculate pixel positions from dpi constants.
        mItemWidth = TestUtils.dpToPx(mActivity, ITEM_WIDTH_DPI);
        mItemHeight = TestUtils.dpToPx(mActivity, ITEM_HEIGHT_DPI);
        mPageWidth = TestUtils.dpToPx(mActivity, PAGE_WIDTH_DPI);
        mPageHeight = TestUtils.dpToPx(mActivity, PAGE_HEIGHT_DPI);

        mScrollBottom = mItemHeight * ITEM_COUNT - mPageHeight;
        mScrollRight = mItemWidth - mPageWidth;
    }

    @After
    public void teardown() {
        ValueAnimator.setDurationScale(mDurationScale);
    }

    @Test
    public void testConstructor() {
        XmlPullParser parser = mActivity.getResources().getLayout(R.layout.scrollview_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new ScrollView(mActivity);

        new ScrollView(mActivity, attrs);

        new ScrollView(mActivity, attrs, 0);
    }

    @UiThreadTest
    @Test
    public void testGetMaxScrollAmount() {
        // the value is half of total layout height
        mScrollViewRegular.layout(0, 0, 100, 200);
        assertEquals((200 - 0) / 2, mScrollViewRegular.getMaxScrollAmount());

        mScrollViewRegular.layout(0, 0, 150, 100);
        assertEquals((100 - 0) / 2, mScrollViewRegular.getMaxScrollAmount());
    }

    @UiThreadTest
    @Test
    public void testAddView() {
        TextView child0 = new TextView(mActivity);
        mScrollViewRegular.addView(child0);
        assertSame(child0, mScrollViewRegular.getChildAt(0));

        assertEquals(1, mScrollViewRegular.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child1);
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, mScrollViewRegular.getChildCount());
    }

    @UiThreadTest
    @Test
    public void testAddViewWithIndex() {
        TextView child0 = new TextView(mActivity);
        mScrollViewRegular.addView(child0, 0);
        assertSame(child0, mScrollViewRegular.getChildAt(0));

        assertEquals(1, mScrollViewRegular.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child1, 1);
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, mScrollViewRegular.getChildCount());

        mScrollViewRegular.removeAllViews();
        mScrollViewRegular = new ScrollView(mActivity);
        mScrollViewRegular.addView(child0, -1);
        assertSame(child0, mScrollViewRegular.getChildAt(0));

        assertEquals(1, mScrollViewRegular.getChildCount());
        child1 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child1, -1);
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, mScrollViewRegular.getChildCount());

        mScrollViewRegular.removeAllViews();
        mScrollViewRegular = new ScrollView(mActivity);
        try {
            mScrollViewRegular.addView(child0, 1);
            fail("ScrollView can host only one direct child");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @UiThreadTest
    @Test
    public void testAddViewWithLayoutParams() {
        TextView child0 = new TextView(mActivity);
        mScrollViewRegular.addView(child0, new ViewGroup.LayoutParams(200, 100));
        assertSame(child0, mScrollViewRegular.getChildAt(0));
        assertEquals(200, child0.getLayoutParams().width);
        assertEquals(100, child0.getLayoutParams().height);

        assertEquals(1, mScrollViewRegular.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child1, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, mScrollViewRegular.getChildCount());

        mScrollViewRegular.removeAllViews();
        mScrollViewRegular = new ScrollView(mActivity);
        child0 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child0, null);
            fail("The LayoutParams should not be null!");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @UiThreadTest
    @Test
    public void testAddViewWithIndexAndLayoutParams() {
        TextView child0 = new TextView(mActivity);
        mScrollViewRegular.addView(child0, 0, new ViewGroup.LayoutParams(200, 100));
        assertSame(child0, mScrollViewRegular.getChildAt(0));
        assertEquals(200, child0.getLayoutParams().width);
        assertEquals(100, child0.getLayoutParams().height);

        assertEquals(1, mScrollViewRegular.getChildCount());
        TextView child1 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child1, 0, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, mScrollViewRegular.getChildCount());

        mScrollViewRegular.removeAllViews();
        child0 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child0, null);
            fail("The LayoutParams should not be null!");
        } catch (NullPointerException e) {
            // expected
        }

        mScrollViewRegular.removeAllViews();
        mScrollViewRegular.addView(child0, -1, new ViewGroup.LayoutParams(300, 150));
        assertSame(child0, mScrollViewRegular.getChildAt(0));
        assertEquals(300, child0.getLayoutParams().width);
        assertEquals(150, child0.getLayoutParams().height);

        assertEquals(1, mScrollViewRegular.getChildCount());
        child1 = new TextView(mActivity);
        try {
            mScrollViewRegular.addView(child1, -1, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals(1, mScrollViewRegular.getChildCount());

        mScrollViewRegular.removeAllViews();
        try {
            mScrollViewRegular.addView(child0, 1, new ViewGroup.LayoutParams(200, 100));
            fail("ScrollView can host only one direct child");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @UiThreadTest
    @Test
    public void testAccessFillViewport() {
        assertFalse(mScrollViewRegular.isFillViewport());
        mScrollViewRegular.layout(0, 0, 100, 100);
        assertFalse(mScrollViewRegular.isLayoutRequested());

        mScrollViewRegular.setFillViewport(false);
        assertFalse(mScrollViewRegular.isFillViewport());
        assertFalse(mScrollViewRegular.isLayoutRequested());

        mScrollViewRegular.setFillViewport(true);
        assertTrue(mScrollViewRegular.isFillViewport());
        assertTrue(mScrollViewRegular.isLayoutRequested());

        mScrollViewRegular.layout(0, 0, 100, 100);
        assertFalse(mScrollViewRegular.isLayoutRequested());

        mScrollViewRegular.setFillViewport(false);
        assertFalse(mScrollViewRegular.isFillViewport());
        assertTrue(mScrollViewRegular.isLayoutRequested());
    }

    @Test
    public void testAccessSmoothScrollingEnabled() throws Throwable {
        assertTrue(mScrollViewCustom.isSmoothScrollingEnabled());

        // scroll immediately
        mScrollViewCustom.setSmoothScrollingEnabled(false);
        assertFalse(mScrollViewCustom.isSmoothScrollingEnabled());

        mActivityRule.runOnUiThread(() -> mScrollViewCustom.fullScroll(View.FOCUS_DOWN));

        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY(), TOLERANCE);

        mActivityRule.runOnUiThread(() -> mScrollViewCustom.fullScroll(View.FOCUS_UP));
        assertEquals(0, mScrollViewCustom.getScrollY());

        // smooth scroll
        mScrollViewCustom.setSmoothScrollingEnabled(true);
        assertTrue(mScrollViewCustom.isSmoothScrollingEnabled());

        mActivityRule.runOnUiThread(() -> mScrollViewCustom.fullScroll(View.FOCUS_DOWN));
        pollingCheckSmoothScrolling(0, 0, 0, mScrollBottom);
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY(), TOLERANCE);

        mActivityRule.runOnUiThread(() -> mScrollViewCustom.fullScroll(View.FOCUS_UP));
        pollingCheckSmoothScrolling(0, 0, mScrollBottom, 0);
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testMeasureChild() {
        MyView child = new MyView(mActivity);
        child.setBackgroundDrawable(null);
        child.setPadding(0, 0, 0, 0);
        child.setMinimumHeight(30);
        child.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        child.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));

        assertEquals(100, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());

        mScrollViewCustomEmpty.measureChild(child,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));

        assertEquals(30, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());
    }

    @UiThreadTest
    @Test
    public void testMeasureChildWithMargins() {
        MyView child = new MyView(mActivity);
        child.setBackgroundDrawable(null);
        child.setPadding(0, 0, 0, 0);
        child.setMinimumHeight(30);
        child.setLayoutParams(new ViewGroup.MarginLayoutParams(100, 100));
        child.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));

        assertEquals(100, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());

        mScrollViewCustomEmpty.measureChildWithMargins(child,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY), 5,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY), 5);

        assertEquals(30, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());
    }

    @UiThreadTest
    @Test
    public void testMeasureSpecs() {
        MyView child = spy(new MyView(mActivity));
        mScrollViewCustomEmpty.addView(child);

        mScrollViewCustomEmpty.measureChild(child,
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));
        verify(child).onMeasure(
                eq(MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY)),
                eq(MeasureSpec.makeMeasureSpec(100, MeasureSpec.UNSPECIFIED)));
    }

    @UiThreadTest
    @Test
    public void testMeasureSpecsWithPadding() {
        MyView child = spy(new MyView(mActivity));
        mScrollViewCustomEmpty.setPadding(3, 5, 7, 11);
        mScrollViewCustomEmpty.addView(child);

        mScrollViewCustomEmpty.measureChild(child,
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));
        verify(child).onMeasure(
                eq(MeasureSpec.makeMeasureSpec(140, MeasureSpec.EXACTLY)),
                eq(MeasureSpec.makeMeasureSpec(84, MeasureSpec.UNSPECIFIED)));
    }

    @UiThreadTest
    @Test
    public void testMeasureSpecsWithMargins() {
        MyView child = spy(new MyView(mActivity));
        mScrollViewCustomEmpty.addView(child);

        mScrollViewCustomEmpty.measureChildWithMargins(child,
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY), 20,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY), 15);
        verify(child).onMeasure(
                eq(MeasureSpec.makeMeasureSpec(130, MeasureSpec.EXACTLY)),
                eq(MeasureSpec.makeMeasureSpec(85, MeasureSpec.UNSPECIFIED)));
    }

    @UiThreadTest
    @Test
    public void testMeasureSpecsWithMarginsAndPadding() {
        MyView child = spy(new MyView(mActivity));
        mScrollViewCustomEmpty.setPadding(3, 5, 7, 11);
        mScrollViewCustomEmpty.addView(child);

        mScrollViewCustomEmpty.measureChildWithMargins(child,
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY), 20,
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY), 15);
        verify(child).onMeasure(
                eq(MeasureSpec.makeMeasureSpec(120, MeasureSpec.EXACTLY)),
                eq(MeasureSpec.makeMeasureSpec(69, MeasureSpec.UNSPECIFIED)));
    }

    @UiThreadTest
    @Test
    public void testMeasureSpecsWithMarginsAndNoHintWidth() {
        MyView child = spy(new MyView(mActivity));
        mScrollViewCustomEmpty.addView(child);

        mScrollViewCustomEmpty.measureChildWithMargins(child,
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY), 20,
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 15);
        verify(child).onMeasure(
                eq(MeasureSpec.makeMeasureSpec(130, MeasureSpec.EXACTLY)),
                eq(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)));
    }

    @UiThreadTest
    @Test
    public void testFillViewport() {
        mScrollViewRegular.setFillViewport(true);

        MyView child = new MyView(mActivity);
        child.setLayoutParams(new ViewGroup.LayoutParams(100, 100));

        mScrollViewRegular.addView(child);
        mScrollViewRegular.measure(
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY));

        assertEquals(150, child.getMeasuredHeight());
        assertEquals(100, child.getMeasuredWidth());

        mScrollViewRegular.layout(0, 0, 100, 150);
        assertEquals(0, child.getTop());
    }

    @UiThreadTest
    @Test
    public void testFillViewportWithScrollViewPadding() {
        mScrollViewRegular.setFillViewport(true);
        mScrollViewRegular.setPadding(3, 10, 5, 7);

        MyView child = new MyView(mActivity);
        child.setLayoutParams(new ViewGroup.LayoutParams(10,10));
        child.setDesiredHeight(30);

        mScrollViewRegular.addView(child);
        mScrollViewRegular.measure(
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY));

        assertEquals(133, child.getMeasuredHeight());
        assertEquals(10, child.getMeasuredWidth());

        mScrollViewRegular.layout(0, 0, 100, 150);
        assertEquals(10, child.getTop());
    }

    @UiThreadTest
    @Test
    public void testFillViewportWithChildMargins() {
        mScrollViewRegular.setFillViewport(true);

        MyView child = new MyView(mActivity);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(10, 10);
        lp.leftMargin = 3;
        lp.topMargin = 10;
        lp.rightMargin = 5;
        lp.bottomMargin = 7;
        child.setDesiredHeight(30);
        child.setLayoutParams(lp);

        mScrollViewRegular.addView(child);
        mScrollViewRegular.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY));

        assertEquals(133, child.getMeasuredHeight());
        assertEquals(10, child.getMeasuredWidth());

        mScrollViewRegular.layout(0, 0, 100, 150);
        assertEquals(10, child.getTop());
    }

    @UiThreadTest
    @Test
    public void testFillViewportWithScrollViewPaddingAlreadyFills() {
        mScrollViewRegular.setFillViewport(true);
        mScrollViewRegular.setPadding(3, 10, 5, 7);

        MyView child = new MyView(mActivity);
        child.setDesiredHeight(175);

        mScrollViewRegular.addView(child);
        mScrollViewRegular.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY));


        assertEquals(92, child.getMeasuredWidth());
        assertEquals(175, child.getMeasuredHeight());

        mScrollViewRegular.layout(0, 0, 100, 150);
        assertEquals(10, child.getTop());
    }

    @UiThreadTest
    @Test
    public void testFillViewportWithChildMarginsAlreadyFills() {
        mScrollViewRegular.setFillViewport(true);
        MyView child = new MyView(mActivity);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        lp.leftMargin = 3;
        lp.topMargin = 10;
        lp.rightMargin = 5;
        lp.bottomMargin = 7;
        child.setLayoutParams(lp);
        child.setDesiredHeight(175);

        mScrollViewRegular.addView(child);
        mScrollViewRegular.measure(MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(150, MeasureSpec.EXACTLY));

        assertEquals(92, child.getMeasuredWidth());
        assertEquals(175, child.getMeasuredHeight());

        mScrollViewRegular.layout(0, 0, 100, 150);
        assertEquals(10, child.getTop());
    }

    @UiThreadTest
    @Test
    public void testPageScroll() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollViewCustom.getScrollY());

        assertTrue(mScrollViewCustom.pageScroll(View.FOCUS_DOWN));
        assertEquals(mPageHeight, mScrollViewCustom.getScrollY(), TOLERANCE);

        assertTrue(mScrollViewCustom.pageScroll(View.FOCUS_DOWN));
        assertEquals(mPageHeight * 2, mScrollViewCustom.getScrollY(), TOLERANCE);

        mScrollViewCustom.scrollTo(mPageWidth, mScrollBottom);
        assertFalse(mScrollViewCustom.pageScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY(), TOLERANCE);

        assertTrue(mScrollViewCustom.pageScroll(View.FOCUS_UP));
        assertEquals(mScrollBottom - mPageHeight, mScrollViewCustom.getScrollY(), TOLERANCE);

        assertTrue(mScrollViewCustom.pageScroll(View.FOCUS_UP));
        assertEquals(mScrollBottom -mPageHeight * 2, mScrollViewCustom.getScrollY(), TOLERANCE);

        mScrollViewCustom.scrollTo(mPageWidth, 0);
        assertFalse(mScrollViewCustom.pageScroll(View.FOCUS_UP));
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testFullScroll() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollViewCustom.getScrollY());

        assertTrue(mScrollViewCustom.fullScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        assertFalse(mScrollViewCustom.fullScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        assertTrue(mScrollViewCustom.fullScroll(View.FOCUS_UP));
        assertEquals(0, mScrollViewCustom.getScrollY());

        assertFalse(mScrollViewCustom.fullScroll(View.FOCUS_UP));
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testArrowScroll() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollViewCustom.getScrollY());

        int y = mScrollViewCustom.getScrollY();
        while (mScrollBottom != y) {
            assertTrue(mScrollViewCustom.arrowScroll(View.FOCUS_DOWN));
            assertTrue(y <= mScrollViewCustom.getScrollY());
            y = mScrollViewCustom.getScrollY();
        }

        assertFalse(mScrollViewCustom.arrowScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        y = mScrollViewCustom.getScrollY();
        while (0 != y) {
            assertTrue(mScrollViewCustom.arrowScroll(View.FOCUS_UP));
            assertTrue(y >= mScrollViewCustom.getScrollY());
            y = mScrollViewCustom.getScrollY();
        }

        assertFalse(mScrollViewCustom.arrowScroll(View.FOCUS_UP));
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testKeyPageUpDownScroll() {
        final KeyEvent pageDownDownEvent =
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN);
        final KeyEvent pageDownUpEvent =
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN);

        final KeyEvent pageUpDownEvent =
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP);
        final KeyEvent pageUpUpEvent =
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP);

        mScrollViewCustom.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollViewCustom.getScrollY());

        int y = mScrollViewCustom.getScrollY();
        while (mScrollBottom != y) {
            assertTrue(mScrollViewCustom.dispatchKeyEvent(pageDownDownEvent));
            assertFalse(mScrollViewCustom.dispatchKeyEvent(pageDownUpEvent));

            assertTrue(y <= mScrollViewCustom.getScrollY());
            y = mScrollViewCustom.getScrollY();
        }

        assertFalse(mScrollViewCustom.arrowScroll(View.FOCUS_DOWN));
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        y = mScrollViewCustom.getScrollY();
        while (0 != y) {
            assertTrue(mScrollViewCustom.dispatchKeyEvent(pageUpDownEvent));
            assertFalse(mScrollViewCustom.dispatchKeyEvent(pageUpUpEvent));

            assertTrue(y >= mScrollViewCustom.getScrollY());
            y = mScrollViewCustom.getScrollY();
        }

        assertFalse(mScrollViewCustom.arrowScroll(View.FOCUS_UP));
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testKeyHomeEndScroll() {
        final KeyEvent endDownEvent =
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END);
        final KeyEvent endUpEvent =
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END);

        final KeyEvent homeDownEvent =
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME);
        final KeyEvent homeUpEvent =
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME);

        mScrollViewCustom.setSmoothScrollingEnabled(false);
        assertEquals(0, mScrollViewCustom.getScrollY());

        // Send END KeyEvent
        assertTrue(mScrollViewCustom.dispatchKeyEvent(endDownEvent));
        assertFalse(mScrollViewCustom.dispatchKeyEvent(endUpEvent));

        // End key should scroll until end of the content.
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        // Send HOME KeyEvent
        assertTrue(mScrollViewCustom.dispatchKeyEvent(homeDownEvent));
        assertFalse(mScrollViewCustom.dispatchKeyEvent(homeUpEvent));

        // HOME key should scroll up to top.
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @Test
    public void testSmoothScrollBy() throws Throwable {
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(0, mScrollViewCustom.getScrollY());

        mActivityRule.runOnUiThread(
                () -> mScrollViewCustom.smoothScrollBy(mScrollRight, mScrollBottom));
        // smoothScrollBy doesn't scroll in X
        pollingCheckSmoothScrolling(0, 0, 0, mScrollBottom);
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        mActivityRule.runOnUiThread(
                () -> mScrollViewCustom.smoothScrollBy(-mScrollRight, -mScrollBottom));
        pollingCheckSmoothScrolling(mScrollRight, 0, mScrollBottom, 0);
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(0, mScrollViewCustom.getScrollY());
    }

    @Test
    public void testSmoothScrollTo() throws Throwable {
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(0, mScrollViewCustom.getScrollY());

        mActivityRule.runOnUiThread(
                () -> mScrollViewCustom.smoothScrollTo(mScrollRight, mScrollBottom));
        // smoothScrollTo doesn't scroll in X
        pollingCheckSmoothScrolling(0, 0, 0, mScrollBottom);
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());

        mActivityRule.runOnUiThread(
                () -> mScrollViewCustom.smoothScrollTo(mPageWidth, mPageHeight));
        pollingCheckSmoothScrolling(0, 0, mScrollBottom, mPageHeight);
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(mPageHeight, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testComputeScrollDeltaToGetChildRectOnScreen() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);
        int edge = mScrollViewCustom.getVerticalFadingEdgeLength();

        // Rect's height is smaller than scroll view
        Rect rect = new Rect(0, 0, 0, 0);
        assertEquals(0,
                ((MyScrollView) mScrollViewCustom).computeScrollDeltaToGetChildRectOnScreen(rect));

        rect = new Rect(0, edge, 0, mPageHeight);
        assertEquals(0,
                ((MyScrollView) mScrollViewCustom).computeScrollDeltaToGetChildRectOnScreen(rect));

        mScrollViewCustom.scrollTo(0, 0);
        rect = new Rect(0, edge + 1, 0, mPageHeight);
        assertEquals(edge,
                ((MyScrollView) mScrollViewCustom).computeScrollDeltaToGetChildRectOnScreen(rect));
    }

    @UiThreadTest
    @Test
    public void testComputeVerticalScrollRange() {
        assertTrue(mScrollViewCustom.getChildCount() > 0);
        assertEquals(mItemHeight * ITEM_COUNT,
                ((MyScrollView) mScrollViewCustom).computeVerticalScrollRange(), TOLERANCE);

        MyScrollView myScrollView = new MyScrollView(mActivity);
        assertEquals(0, myScrollView.getChildCount());
        assertEquals(0, myScrollView.computeVerticalScrollRange());
    }

    @UiThreadTest
    @Test
    public void testRequestChildFocus() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);

        View firstChild = mScrollViewCustom.findViewById(R.id.first_child);
        View lastChild = mScrollViewCustom.findViewById(R.id.last_child);
        firstChild.requestFocus();

        int scrollY = mScrollViewCustom.getScrollY();
        mScrollViewCustom.requestChildFocus(lastChild, lastChild);
        // check scrolling to the child which wants focus
        assertTrue(mScrollViewCustom.getScrollY() > scrollY);

        scrollY = mScrollViewCustom.getScrollY();
        mScrollViewCustom.requestChildFocus(firstChild, firstChild);
        // check scrolling to the child which wants focus
        assertTrue(mScrollViewCustom.getScrollY() < scrollY);
    }

    @UiThreadTest
    @Test
    public void testRequestChildRectangleOnScreen() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);
        mScrollViewCustom.setVerticalFadingEdgeEnabled(true);
        int edge = mScrollViewCustom.getVerticalFadingEdgeLength();

        View child = mScrollViewCustom.findViewById(R.id.first_child);
        int orgRectSize = (int)(10 * mActivity.getResources().getDisplayMetrics().density);
        final Rect originalRect = new Rect(0, 0, orgRectSize, orgRectSize);
        final Rect newRect = new Rect(mItemWidth - orgRectSize, mItemHeight - orgRectSize,
                mItemWidth, mItemHeight);

        assertFalse(mScrollViewCustom.requestChildRectangleOnScreen(child, originalRect, true));
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(0, mScrollViewCustom.getScrollY());

        assertTrue(mScrollViewCustom.requestChildRectangleOnScreen(child, newRect, true));
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(edge, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testRequestLayout() {
        mScrollViewCustom.requestLayout();

        assertTrue(mScrollViewCustom.isLayoutRequested());
    }

    @Test
    public void testFling() throws Throwable {
        mScrollViewCustom.setSmoothScrollingEnabled(true);
        assertEquals(0, mScrollViewCustom.getScrollY());

        // fling towards bottom
        mActivityRule.runOnUiThread(() -> mScrollViewCustom.fling(2000));
        pollingCheckFling(0, true);

        final int currentY = mScrollViewCustom.getScrollY();
        // fling towards top
        mActivityRule.runOnUiThread(() -> mScrollViewCustom.fling(-2000));
        pollingCheckFling(currentY, false);
    }

    @UiThreadTest
    @Test
    public void testScrollTo() {
        mScrollViewCustom.setSmoothScrollingEnabled(false);

        mScrollViewCustom.scrollTo(10, 10);
        assertEquals(10, mScrollViewCustom.getScrollY());
        assertEquals(10, mScrollViewCustom.getScrollX());

        mScrollViewCustom.scrollTo(mPageWidth, mPageHeight);
        assertEquals(mPageHeight, mScrollViewCustom.getScrollY());
        assertEquals(mPageWidth, mScrollViewCustom.getScrollX());

        mScrollViewCustom.scrollTo(mScrollRight, mScrollBottom);
        assertEquals(mScrollBottom, mScrollViewCustom.getScrollY());
        assertEquals(mScrollRight, mScrollViewCustom.getScrollX());

        // reach the top and left
        mScrollViewCustom.scrollTo(-10, -10);
        assertEquals(0, mScrollViewCustom.getScrollY());
        assertEquals(0, mScrollViewCustom.getScrollX());
    }

    @UiThreadTest
    @Test
    public void testGetVerticalFadingEdgeStrengths() {
        MyScrollView myScrollViewCustom = (MyScrollView) mScrollViewCustom;

        assertTrue(myScrollViewCustom.getChildCount() > 0);
        assertTrue(myScrollViewCustom.getTopFadingEdgeStrength() <= 1.0f);
        assertTrue(myScrollViewCustom.getTopFadingEdgeStrength() >= 0.0f);
        assertTrue(myScrollViewCustom.getBottomFadingEdgeStrength() <= 1.0f);
        assertTrue(myScrollViewCustom.getBottomFadingEdgeStrength() >= 0.0f);

        MyScrollView myScrollView = new MyScrollView(mActivity);
        assertEquals(0, myScrollView.getChildCount());
        assertTrue(myScrollView.getTopFadingEdgeStrength() <= 1.0f);
        assertTrue(myScrollView.getTopFadingEdgeStrength() >= 0.0f);
        assertTrue(myScrollView.getBottomFadingEdgeStrength() <= 1.0f);
        assertTrue(myScrollView.getBottomFadingEdgeStrength() >= 0.0f);
    }

    @Test
    public void testScrollDescendant() throws Throwable {
        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(0, mScrollViewCustom.getScrollY());

        View lastChild = mScrollViewCustom.findViewById(R.id.last_child);
        int lastChildTop = (ITEM_COUNT - 1) * mItemHeight;

        mActivityRule.runOnUiThread(() -> mScrollViewCustom.scrollToDescendant(lastChild));
        // smoothScrollBy doesn't scroll in X
        pollingCheckSmoothScrolling(0, 0, 0, lastChildTop);

        assertEquals(0, mScrollViewCustom.getScrollX());
        assertEquals(lastChildTop, mScrollViewCustom.getScrollY());
    }

    @UiThreadTest
    @Test
    public void testEdgeEffectColors() {
        int defaultColor = new EdgeEffect(mScrollViewRegular.getContext()).getColor();
        assertEquals(mScrollViewRegular.getTopEdgeEffectColor(), defaultColor);
        assertEquals(mScrollViewRegular.getBottomEdgeEffectColor(), defaultColor);

        mScrollViewRegular.setEdgeEffectColor(Color.BLUE);
        assertEquals(mScrollViewRegular.getTopEdgeEffectColor(), Color.BLUE);
        assertEquals(mScrollViewRegular.getBottomEdgeEffectColor(), Color.BLUE);

        mScrollViewRegular.setTopEdgeEffectColor(Color.RED);
        assertEquals(mScrollViewRegular.getTopEdgeEffectColor(), Color.RED);
        assertEquals(mScrollViewRegular.getBottomEdgeEffectColor(), Color.BLUE);

        mScrollViewRegular.setBottomEdgeEffectColor(Color.GREEN);
        assertEquals(mScrollViewRegular.getTopEdgeEffectColor(), Color.RED);
        assertEquals(mScrollViewRegular.getBottomEdgeEffectColor(), Color.GREEN);
    }

    @Test
    public void testStretchAtTop() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowTop = edgeEffect;
        assertTrue(StretchEdgeUtil.dragStretches(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                300
        ));
    }

    @Test
    public void testStretchAtTopAndCatch() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowTop = edgeEffect;
        assertTrue(StretchEdgeUtil.dragAndHoldKeepsStretch(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                3000
        ));
    }

    /**
     * Verify that the SurfaceView is at the correct location during the overscroll stretch
     */
    @Test
    public void testSurfaceViewStretchAtEnd() throws Throwable {
        showOnlyStretch();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceView surfaceView = mActivity.findViewById(R.id.surfaceview_stretch_target);
                surfaceView.setBackgroundColor(Color.RED);
                surfaceView.setVisibility(View.VISIBLE);
            }
        });

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
        });

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);

        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;

        StretchEdgeUtil.dragAndHoldExecute(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                -3000,
                null,
                () -> {
                    int[] coords = new int[2];
                    mScrollViewStretch.getLocationInWindow(coords);
                    Bitmap screenshot = getInstrumentation().getUiAutomation().takeScreenshot(
                            mActivityRule.getActivity().getWindow());

                    assertEquals(Color.RED, screenshot.getPixel(
                            coords[0] + mScrollViewStretch.getWidth() / 2,
                            coords[1] + mScrollViewStretch.getHeight() / 2));
                }
        );
    }

    @Test
    public void testSurfaceViewStretchAtEndWithScale() throws Throwable {
        showOnlyStretch();

        float scaleY = 2.0f;

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceView surfaceView = mActivity.findViewById(R.id.surfaceview_stretch_target);
                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(@NonNull SurfaceHolder holder) {
                        // no-op
                    }

                    @Override
                    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                            int height) {
                        surfaceView.setScaleY(scaleY);
                        surfaceView.setPivotX(width / 2f);
                        surfaceView.setPivotY(height);
                        Surface surface = holder.getSurface();
                        Paint paint = new Paint();
                        Canvas canvas = surface.lockHardwareCanvas();
                        paint.setColor(Color.RED);
                        canvas.drawRect(0f, 0f, width / 2f, height / 2f, paint);
                        paint.setColor(Color.YELLOW);
                        canvas.drawRect(width / 2f, 0f, width, height / 2f, paint);
                        paint.setColor(Color.BLUE);
                        canvas.drawRect(0f, height / 2f, width / 2f, height, paint);
                        paint.setColor(Color.BLACK);
                        canvas.drawRect(width / 2f, height / 2f, width, height, paint);
                        surface.unlockCanvasAndPost(canvas);
                    }

                    @Override
                    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                        // no-op
                    }
                });
                surfaceView.setVisibility(View.VISIBLE);

            }
        });

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the end
            mScrollViewStretch.scrollToEnd();
        });

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);

        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;

        StretchEdgeUtil.dragAndHoldExecute(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                -3000,
                null,
                () -> {
                    int[] coords = new int[2];
                    mScrollViewStretch.getLocationInWindow(coords);
                    Bitmap screenshot = getInstrumentation().getUiAutomation().takeScreenshot(
                            mActivityRule.getActivity().getWindow());

                    int containerWidth = mScrollViewStretch.getWidth();
                    int containerHeight = mScrollViewStretch.getHeight();

                    int topLeftColor = screenshot.getPixel(
                            coords[0] + containerWidth / 4,
                            coords[1] + (containerHeight / 4) + 3
                    );

                    int topRightColor = screenshot.getPixel(
                            coords[0] + containerWidth / 2 + containerWidth / 4,
                            coords[1] + (containerHeight / 4) + 3
                    );

                    int bottomLeftColor = screenshot.getPixel(
                            coords[0] + containerWidth / 4,
                            coords[1] + containerHeight / 2 + containerHeight / 4
                    );

                    int bottomRightColor = screenshot.getPixel(
                            coords[0] + containerWidth / 2 + containerWidth / 4,
                            coords[1] + containerHeight / 2 + containerHeight / 4
                    );
                    assertEquals(Color.RED, topLeftColor);
                    assertEquals(Color.YELLOW, topRightColor);
                    assertEquals(Color.BLUE, bottomLeftColor);
                    assertEquals(Color.BLACK, bottomRightColor);
                }
        );
    }

    @LargeTest
    @Test
    public void testRequestDisallowInterceptTouchEventNotCalled() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        InterceptView interceptView = mActivity.findViewById(R.id.wrapped_stretch);

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowTop = edgeEffect;

        dragAndHoldExecute(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                300,
                () -> interceptView.requestDisallowInterceptCalled = false,
                null
        );

        mActivityRule.runOnUiThread(
                () -> assertFalse(interceptView.requestDisallowInterceptCalled)
        );
    }

    @Test
    public void testStretchAtBottom() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
        });

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;
        assertTrue(StretchEdgeUtil.dragStretches(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                -300
        ));
    }

    @Test
    public void testStretchAtBottomAndCatch() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
        });

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;
        assertTrue(StretchEdgeUtil.dragAndHoldKeepsStretch(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                -300
        ));
    }

    @Test
    public void testFlingWhileStretchedTop() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        CaptureOnAbsorbEdgeEffect edgeEffect = new CaptureOnAbsorbEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowTop = edgeEffect;
        fling(mActivityRule, mScrollViewStretch, 0, 300);
        assertTrue(edgeEffect.onAbsorbVelocity > 0);
    }

    @Test
    public void testFlingWhileStretchedBottom() throws Throwable {
        // Make sure that the scroll view we care about is on screen and at the top:
        showOnlyStretch();

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
        });

        CaptureOnAbsorbEdgeEffect edgeEffect = new CaptureOnAbsorbEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;
        fling(mActivityRule, mScrollViewStretch, 0, -300);
        assertTrue(edgeEffect.onAbsorbVelocity > 0);
    }

    @Test
    public void scrollFromRotaryStretchesTop() throws Throwable {
        showOnlyStretch();

        CaptureOnReleaseEdgeEffect edgeEffect = new CaptureOnReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowTop = edgeEffect;

        mActivityRule.runOnUiThread(() -> {
            assertTrue(mScrollViewStretch.dispatchGenericMotionEvent(
                    createScrollEvent(2f, InputDevice.SOURCE_ROTARY_ENCODER)));
            assertFalse(edgeEffect.isFinished());
            assertTrue(edgeEffect.getDistance() > 0f);
            assertTrue(edgeEffect.onReleaseCalled);
        });
    }

    @Test
    public void scrollFromMouseDoesNotStretchTop() throws Throwable {
        showOnlyStretch();

        CaptureOnReleaseEdgeEffect edgeEffect = new CaptureOnReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowTop = edgeEffect;

        mActivityRule.runOnUiThread(() -> {
            assertFalse(mScrollViewStretch.dispatchGenericMotionEvent(
                    createScrollEvent(2f, InputDevice.SOURCE_MOUSE)));
            assertTrue(edgeEffect.isFinished());
            assertFalse(edgeEffect.onReleaseCalled);
        });
    }

    @Test
    public void scrollFromRotaryStretchesBottom() throws Throwable {
        showOnlyStretch();

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
        });

        CaptureOnReleaseEdgeEffect edgeEffect = new CaptureOnReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;

        mActivityRule.runOnUiThread(() -> {
            assertTrue(mScrollViewStretch.dispatchGenericMotionEvent(
                    createScrollEvent(-2f, InputDevice.SOURCE_ROTARY_ENCODER)));
            assertFalse(edgeEffect.isFinished());
            assertTrue(edgeEffect.getDistance() > 0f);
            assertTrue(edgeEffect.onReleaseCalled);
        });
    }

    @Test
    public void scrollFromMouseDoesNotStretchBottom() throws Throwable {
        showOnlyStretch();

        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
            assertEquals(210, mScrollViewStretch.getScrollY());
        });

        CaptureOnReleaseEdgeEffect edgeEffect = new CaptureOnReleaseEdgeEffect(mActivity);
        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;

        mActivityRule.runOnUiThread(() -> {
            assertFalse(mScrollViewStretch.dispatchGenericMotionEvent(
                    createScrollEvent(-2f, InputDevice.SOURCE_MOUSE)));
            assertTrue(edgeEffect.isFinished());
            assertFalse(edgeEffect.onReleaseCalled);
        });
    }

    @Test
    public void flingUpWhileStretchedAtTop() throws Throwable {
        showOnlyStretch();
        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);

        mScrollViewStretch.mEdgeGlowTop = edgeEffect;

        StretchEdgeUtil.dragAndHoldExecute(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                3000,
                null,
                () -> assertNotEquals(0f, edgeEffect.getDistance())
        );

        mActivityRule.runOnUiThread(() -> {
            edgeEffect.setOnReleaseCalled(false);
            assertEquals(0, mScrollViewStretch.getScrollY());
            mScrollViewStretch.fling(10000);
            assertFalse(edgeEffect.getOnReleaseCalled());
            assertNotEquals(0f, edgeEffect.getDistance());
            assertEquals(0, mScrollViewStretch.getScrollY());
        });

        PollingCheck.waitFor(1000L, () -> edgeEffect.getDistance() == 0);
        PollingCheck.waitFor(1000L, () -> mScrollViewStretch.getScrollY() != 0);
    }

    @Test
    public void flingDownWhileStretchedAtBottom() throws Throwable {
        showOnlyStretch();
        mActivityRule.runOnUiThread(() -> {
            // Scroll all the way to the bottom
            mScrollViewStretch.scrollToEnd();
        });

        NoReleaseEdgeEffect edgeEffect = new NoReleaseEdgeEffect(mActivity);

        mScrollViewStretch.mEdgeGlowBottom = edgeEffect;

        StretchEdgeUtil.dragAndHoldExecute(
                mActivityRule,
                mScrollViewStretch,
                edgeEffect,
                0,
                -3000,
                null,
                () -> assertNotEquals(0f, edgeEffect.getDistance())
        );

        mActivityRule.runOnUiThread(() -> {
            edgeEffect.setOnReleaseCalled(false);
            assertEquals(210, mScrollViewStretch.getScrollY());
            mScrollViewStretch.fling(-10000);
            assertFalse(edgeEffect.getOnReleaseCalled());
            assertNotEquals(0f, edgeEffect.getDistance());
            assertEquals(210, mScrollViewStretch.getScrollY());
        });

        PollingCheck.waitFor(1000L, () -> edgeEffect.getDistance() == 0);
        PollingCheck.waitFor(1000L, () -> mScrollViewStretch.getScrollY() != 210);
    }

    private MotionEvent createScrollEvent(float scrollAmount, int source) {
        MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        int axis = source == InputDevice.SOURCE_ROTARY_ENCODER ? MotionEvent.AXIS_SCROLL
                : MotionEvent.AXIS_VSCROLL;
        pointerCoords.setAxisValue(axis, scrollAmount);

        return MotionEvent.obtain(
                0, /* downTime */
                0, /* eventTime */
                MotionEvent.ACTION_SCROLL, /* action */
                1, /* pointerCount */
                new MotionEvent.PointerProperties[] { pointerProperties },
                new MotionEvent.PointerCoords[] { pointerCoords },
                0, /* metaState */
                0, /* buttonState */
                0f, /* xPrecision */
                0f, /* yPrecision */
                0, /* deviceId */
                0, /* edgeFlags */
                source, /* source */
                0 /* flags */
        );
    }

    private void showOnlyStretch() throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            mScrollViewCustom.setVisibility(View.GONE);
            mScrollViewCustomEmpty.setVisibility(View.GONE);
            mScrollViewRegular.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.GONE);
        });
    }

    private boolean isInRange(int current, int from, int to) {
        if (from < to) {
            return current >= from && current <= to;
        }
        return current <= from && current >= to;
    }

    private void pollingCheckSmoothScrolling(final int fromX, final int toX,
            final int fromY, final int toY) {

        if (fromX == toX && fromY == toY) {
            return;
        }

        if (fromY != toY) {
            PollingCheck.waitFor(() -> isInRange(mScrollViewCustom.getScrollY(), fromY, toY));
        }

        if (fromX != toX) {
            PollingCheck.waitFor(() -> isInRange(mScrollViewCustom.getScrollX(), fromX, toX));
        }

        PollingCheck.waitFor(
                () -> toX == mScrollViewCustom.getScrollX()
                        && toY == mScrollViewCustom.getScrollY());
    }

    private void pollingCheckFling(final int startPosition, final boolean movingDown) {
        PollingCheck.waitFor(() -> {
            if (movingDown) {
                return mScrollViewCustom.getScrollY() > startPosition;
            }
            return mScrollViewCustom.getScrollY() < startPosition;
        });

        final int[] previousScrollY = new int[] { mScrollViewCustom.getScrollY() };
        PollingCheck.waitFor(() -> {
            if (mScrollViewCustom.getScrollY() == previousScrollY[0]) {
                return true;
            } else {
                previousScrollY[0] = mScrollViewCustom.getScrollY();
                return false;
            }
        });
    }

    public static class MyView extends View {
        // measure in this height if set
        private Integer mDesiredHeight;
        public MyView(Context context) {
            super(context);
        }

        public void setDesiredHeight(Integer desiredHeight) {
            mDesiredHeight = desiredHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (mDesiredHeight != null) {
                int mode = MeasureSpec.getMode(heightMeasureSpec);
                int size = MeasureSpec.getSize(heightMeasureSpec);
                int newHeight = size;
                if (mode == MeasureSpec.AT_MOST) {
                    newHeight = Math.max(size, mDesiredHeight);
                } else if (mode == MeasureSpec.UNSPECIFIED) {
                    newHeight = mDesiredHeight;
                }
                setMeasuredDimension(getMeasuredWidth(), newHeight);
            }
        }
    }

    public static class MyScrollView extends ScrollView {
        public MyScrollView(Context context) {
            super(context);
        }

        public MyScrollView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
            return super.computeScrollDeltaToGetChildRectOnScreen(rect);
        }

        @Override
        protected int computeVerticalScrollRange() {
            return super.computeVerticalScrollRange();
        }

        @Override
        protected float getBottomFadingEdgeStrength() {
            return super.getBottomFadingEdgeStrength();
        }

        @Override
        protected float getTopFadingEdgeStrength() {
            return super.getTopFadingEdgeStrength();
        }

        @Override
        protected void measureChild(View c, int pWidthMeasureSpec, int pHeightMeasureSpec) {
            super.measureChild(c, pWidthMeasureSpec, pHeightMeasureSpec);
        }

        @Override
        protected void measureChildWithMargins(View child, int parentWidthMeasureSpec,
                int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
            super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed,
                    parentHeightMeasureSpec, heightUsed);
        }

        public void scrollToEnd() {
            scrollTo(0, computeVerticalScrollRange());
        }
    }

    public static class InterceptView extends FrameLayout {
        public boolean requestDisallowInterceptCalled = false;

        public InterceptView(Context context) {
            super(context);
        }

        public InterceptView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public InterceptView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            requestDisallowInterceptCalled = true;
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    public static class CaptureOnAbsorbEdgeEffect extends EdgeEffect {
        public int onAbsorbVelocity;

        public CaptureOnAbsorbEdgeEffect(Context context) {
            super(context);
        }

        @Override
        public void onAbsorb(int velocity) {
            onAbsorbVelocity = velocity;
            super.onAbsorb(velocity);
        }
    }

    public static class CaptureOnReleaseEdgeEffect extends EdgeEffect {
        public boolean onReleaseCalled;

        public CaptureOnReleaseEdgeEffect(Context context) {
            super(context);
        }

        @Override
        public void onRelease() {
            onReleaseCalled = true;
            super.onRelease();
        }
    }
}
