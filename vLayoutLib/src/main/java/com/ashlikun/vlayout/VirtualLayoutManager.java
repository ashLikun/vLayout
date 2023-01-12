/*
 * MIT License
 *
 * Copyright (c) 2016 Alibaba Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ashlikun.vlayout;

import android.content.Context;
import android.graphics.Rect;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.extend.LayoutManagerCanScrollListener;
import com.ashlikun.vlayout.extend.PerformanceMonitor;
import com.ashlikun.vlayout.extend.ViewLifeCycleHelper;
import com.ashlikun.vlayout.extend.ViewLifeCycleListener;
import com.ashlikun.vlayout.layout.BaseLayoutHelper;
import com.ashlikun.vlayout.layout.DefaultLayoutHelper;
import com.ashlikun.vlayout.layout.FixAreaAdjuster;
import com.ashlikun.vlayout.layout.FixAreaLayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A {@link androidx.recyclerview.widget.RecyclerView.LayoutManager}它为实际视图提供了虚拟布局。
 * NOTE: 它将为recyclerview更改 {@link androidx.recyclerview.widget.RecyclerView.RecycledViewPool}。
 */

public class VirtualLayoutManager extends com.ashlikun.vlayout.ExposeLinearLayoutManagerEx implements LayoutManagerHelper {
    protected static final String TAG = "VirtualLayoutManager";

    private static final String PHASE_MEASURE = "measure";
    private static final String PHASE_LAYOUT = "layout";
    private static final String TRACE_LAYOUT = "VLM onLayoutChildren";
    private static final String TRACE_SCROLL = "VLM scroll";

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;


    protected com.ashlikun.vlayout.OrientationHelperEx mOrientationHelper;
    protected com.ashlikun.vlayout.OrientationHelperEx mSecondaryOrientationHelper;

    private RecyclerView mRecyclerView;

    private boolean mNoScrolling = false;

    private boolean mNestedScrolling = false;

    private boolean mCanScrollHorizontally;

    private boolean mCanScrollVertically;

    private LayoutManagerCanScrollListener layoutManagerCanScrollListener;

    private boolean mEnableMarginOverlapping = false;

    private int mMaxMeasureSize = -1;

    private PerformanceMonitor mPerformanceMonitor;

    private ViewLifeCycleHelper mViewLifeCycleHelper;

    private Comparator<Pair<com.ashlikun.vlayout.Range<Integer>, Integer>> mRangeComparator = new Comparator<Pair<com.ashlikun.vlayout.Range<Integer>, Integer>>() {
        @Override
        public int compare(Pair<com.ashlikun.vlayout.Range<Integer>, Integer> a, Pair<com.ashlikun.vlayout.Range<Integer>, Integer> b) {
            if (a == null && b == null) {
                return 0;
            }
            if (a == null) {
                return -1;
            }
            if (b == null) {
                return 1;
            }

            com.ashlikun.vlayout.Range<Integer> lr = a.first;
            com.ashlikun.vlayout.Range<Integer> rr = b.first;

            return lr.getLower() - rr.getLower();
        }
    };

    public VirtualLayoutManager(@NonNull final Context context) {
        this(context, VERTICAL);
    }

    /**
     * @param context     Context
     * @param orientation 布局方向。应该是 {@link #HORIZONTAL} or {@link
     *                    #VERTICAL}.
     */
    public VirtualLayoutManager(@NonNull final Context context, int orientation) {
        this(context, orientation, false);
    }

    /**
     * @param context       当前上下文将用于访问资源。
     * @param orientation   布局方向。应该是 {@link #HORIZONTAL} or {@link
     *                      #VERTICAL}.
     * @param reverseLayout 是否应反转数据
     */
    public VirtualLayoutManager(@NonNull final Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        this.mOrientationHelper = com.ashlikun.vlayout.OrientationHelperEx.createOrientationHelper(this, orientation);
        this.mSecondaryOrientationHelper = com.ashlikun.vlayout.OrientationHelperEx.createOrientationHelper(this, orientation == VERTICAL ? HORIZONTAL : VERTICAL);
        this.mCanScrollVertically = super.canScrollVertically();
        this.mCanScrollHorizontally = super.canScrollHorizontally();
        setHelperFinder(new RangeLayoutHelperFinder());
    }

    public void setPerformanceMonitor(PerformanceMonitor performanceMonitor) {
        mPerformanceMonitor = performanceMonitor;
    }

    public void setNoScrolling(boolean noScrolling) {
        this.mNoScrolling = noScrolling;
        mSpaceMeasured = false;
        mMeasuredFullSpace = 0;
        mSpaceMeasuring = false;
    }

    public void setCanScrollVertically(boolean canScrollVertically) {
        this.mCanScrollVertically = canScrollVertically;
    }

    public void setCanScrollHorizontally(boolean canScrollHorizontally) {
        this.mCanScrollHorizontally = canScrollHorizontally;
    }

    public void setLayoutManagerCanScrollListener(LayoutManagerCanScrollListener layoutManagerCanScrollListener) {
        this.layoutManagerCanScrollListener = layoutManagerCanScrollListener;
    }

    public void setNestedScrolling(boolean nestedScrolling) {
        setNestedScrolling(nestedScrolling, -1);
    }

    public void setNestedScrolling(boolean nestedScrolling, int maxMeasureSize) {
        this.mNestedScrolling = nestedScrolling;
        mSpaceMeasuring = mSpaceMeasured = false;
        mMeasuredFullSpace = 0;
    }

    private com.ashlikun.vlayout.LayoutHelperFinder mHelperFinder;

    public void setHelperFinder(@NonNull final LayoutHelperFinder finder) {
        //noinspection ConstantConditions
        if (finder == null) {
            throw new IllegalArgumentException("finder is null");
        }

        List<com.ashlikun.vlayout.LayoutHelper> helpers = new LinkedList<>();
        if (this.mHelperFinder != null) {
            List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
            Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
            com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
            while (iterator.hasNext()) {
                layoutHelper = iterator.next();
                helpers.add(layoutHelper);

            }
        }

        this.mHelperFinder = finder;
        if (helpers.size() > 0) {
            this.mHelperFinder.setLayouts(helpers);
        }

        mSpaceMeasured = false;
        requestLayout();
    }

    private FixAreaAdjuster mFixAreaAdjustor = FixAreaAdjuster.mDefaultAdjuster;

    public void setFixOffset(int left, int top, int right, int bottom) {
        mFixAreaAdjustor = new FixAreaAdjuster(left, top, right, bottom);
    }


    /**
     * Temp hashMap
     */
    private HashMap<Integer, com.ashlikun.vlayout.LayoutHelper> newHelpersSet = new HashMap<>();
    private HashMap<Integer, com.ashlikun.vlayout.LayoutHelper> oldHelpersSet = new HashMap<>();

    private BaseLayoutHelper.LayoutViewBindListener mLayoutViewBindListener;

    /**
     * 更新layoutHelpers，数据更改将导致layoutHelper更改
     *
     * @param helpers layoutHelper组
     */
    public void setLayoutHelpers(@Nullable List<com.ashlikun.vlayout.LayoutHelper> helpers) {
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> it0 = layoutHelpers.iterator();
        while (it0.hasNext()) {
            com.ashlikun.vlayout.LayoutHelper helper = it0.next();
            oldHelpersSet.put(System.identityHashCode(helper), helper);
        }

        // 设置范围
        if (helpers != null) {
            int start = 0;
            Iterator<com.ashlikun.vlayout.LayoutHelper> it1 = helpers.iterator();
            while (it1.hasNext()) {
                com.ashlikun.vlayout.LayoutHelper helper = it1.next();
                if (helper instanceof FixAreaLayoutHelper) {
                    ((FixAreaLayoutHelper) helper).setAdjuster(mFixAreaAdjustor);
                }

                if (helper instanceof BaseLayoutHelper && mLayoutViewBindListener != null) {
                    ((BaseLayoutHelper) helper).setLayoutViewBindListener(mLayoutViewBindListener);
                }


                if (helper.getItemCount() > 0) {
                    helper.setRange(start, start + helper.getItemCount() - 1);
                } else {
                    helper.setRange(-1, -1);
                }

                start += helper.getItemCount();
            }
        }

        this.mHelperFinder.setLayouts(helpers);

        layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        while (iterator.hasNext()) {
            com.ashlikun.vlayout.LayoutHelper layoutHelper = iterator.next();
            newHelpersSet.put(System.identityHashCode(layoutHelper), layoutHelper);
        }

        for (Iterator<Map.Entry<Integer, com.ashlikun.vlayout.LayoutHelper>> it = oldHelpersSet.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, com.ashlikun.vlayout.LayoutHelper> entry = it.next();
            Integer key = entry.getKey();
            if (newHelpersSet.containsKey(key)) {
                newHelpersSet.remove(key);
                it.remove();
            }
        }


        for (com.ashlikun.vlayout.LayoutHelper helper : oldHelpersSet.values()) {
            helper.clear(this);
        }

        if (!oldHelpersSet.isEmpty() || !newHelpersSet.isEmpty()) {
            mSpaceMeasured = false;
        }

        oldHelpersSet.clear();
        newHelpersSet.clear();
        requestLayout();
    }


    @NonNull
    public List<com.ashlikun.vlayout.LayoutHelper> getLayoutHelpers() {
        return this.mHelperFinder.getLayoutHelpers();
    }

    public void setEnableMarginOverlapping(boolean enableMarginOverlapping) {
        mEnableMarginOverlapping = enableMarginOverlapping;
    }

    @Override
    public boolean isEnableMarginOverLap() {
        return mEnableMarginOverlapping;
    }

    /**
     * 要么是 {@link #HORIZONTAL} or {@link #VERTICAL}
     *
     * @return 此布局管理器的方向
     */
    @Override
    public int getOrientation() {
        return super.getOrientation();
    }

    @Override
    public void setOrientation(int orientation) {
        this.mOrientationHelper = com.ashlikun.vlayout.OrientationHelperEx.createOrientationHelper(this, orientation);
        super.setOrientation(orientation);
    }

    /**
     * VirtualLayoutManager不支持reverseLayout。它将被禁用，直到所有LayoutHelper支持它。
     */
    @Override
    public void setReverseLayout(boolean reverseLayout) {
        if (reverseLayout) {
            throw new UnsupportedOperationException(
                    "VirtualLayoutManager does not support reverse layout in current version.");
        }

        super.setReverseLayout(false);
    }

    /**
     * VirtualLayoutManager不支持stackFromEnd。它被禁用，直到所有layoutHelper都支持它。
     * {@link #setReverseLayout(boolean)}.
     */
    @Override
    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException(
                    "VirtualLayoutManager does not support stack from end.");
        }
        super.setStackFromEnd(false);
    }


    private AnchorInfoWrapper mTempAnchorInfoWrapper = new AnchorInfoWrapper();

    @Override
    public void onAnchorReady(RecyclerView.State state, AnchorInfo anchorInfo) {
        super.onAnchorReady(state, anchorInfo);

        boolean changed = true;
        while (changed) {
            mTempAnchorInfoWrapper.position = anchorInfo.mPosition;
            mTempAnchorInfoWrapper.coordinate = anchorInfo.mCoordinate;
            mTempAnchorInfoWrapper.layoutFromEnd = anchorInfo.mLayoutFromEnd;
            com.ashlikun.vlayout.LayoutHelper layoutHelper = mHelperFinder.getLayoutHelper(anchorInfo.mPosition);
            if (layoutHelper != null) {
                layoutHelper.checkAnchorInfo(state, mTempAnchorInfoWrapper, this);
            }

            if (mTempAnchorInfoWrapper.position == anchorInfo.mPosition) {
                changed = false;
            } else {
                anchorInfo.mPosition = mTempAnchorInfoWrapper.position;
            }

            anchorInfo.mCoordinate = mTempAnchorInfoWrapper.coordinate;
        }


        mTempAnchorInfoWrapper.position = anchorInfo.mPosition;
        mTempAnchorInfoWrapper.coordinate = anchorInfo.mCoordinate;
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            layoutHelper.onRefreshLayout(state, mTempAnchorInfoWrapper, this);
        }
    }

    public com.ashlikun.vlayout.LayoutHelper findNeighbourNonfixLayoutHelper(com.ashlikun.vlayout.LayoutHelper layoutHelper, boolean isLayoutEnd) {
        if (layoutHelper == null) {
            return null;
        }
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        int index = layoutHelpers.indexOf(layoutHelper);
        if (index == -1) {
            return null;
        }
        int next = isLayoutEnd ? index - 1 : index + 1;
        if (next >= 0 && next < layoutHelpers.size()) {
            com.ashlikun.vlayout.LayoutHelper helper = layoutHelpers.get(next);
            if (helper != null) {
                if (helper.isFixLayout()) {
                    return null;
                } else {
                    return helper;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected int computeAlignOffset(View child, boolean isLayoutEnd, boolean useAnchor) {
        return computeAlignOffset(getPosition(child), isLayoutEnd, useAnchor);
    }

    @Override
    protected int computeAlignOffset(int position, boolean isLayoutEnd, boolean useAnchor) {
        if (position != RecyclerView.NO_POSITION) {
            com.ashlikun.vlayout.LayoutHelper helper = mHelperFinder.getLayoutHelper(position);

            if (helper != null) {
                return helper.computeAlignOffset(position - helper.getRange().getLower(),
                        isLayoutEnd, useAnchor, this);
            }
        }

        return 0;
    }

    public int obtainExtraMargin(View child, boolean isLayoutEnd) {
        return obtainExtraMargin(child, isLayoutEnd, true);
    }

    public int obtainExtraMargin(View child, boolean isLayoutEnd, boolean useAnchor) {
        if (child != null) {
            return computeAlignOffset(child, isLayoutEnd, useAnchor);
        }

        return 0;
    }

    private int mNested = 0;


    private void runPreLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (mNested == 0) {
            List<com.ashlikun.vlayout.LayoutHelper> reverseLayoutHelpers = mHelperFinder.reverse();
            Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = reverseLayoutHelpers.iterator();
            com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
            while (iterator.hasNext()) {
                layoutHelper = iterator.next();
                layoutHelper.beforeLayout(recycler, state, this);
            }
        }

        mNested++;
    }

    private void runPostLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int scrolled) {
        mNested--;
        if (mNested <= 0) {
            mNested = 0;
            final int startPosition = findFirstVisibleItemPosition();
            final int endPosition = findLastVisibleItemPosition();
            List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
            Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
            com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
            while (iterator.hasNext()) {
                layoutHelper = iterator.next();
                try {
                    layoutHelper.afterLayout(recycler, state, startPosition, endPosition, scrolled, this);
                } catch (Exception e) {
                    if (VLayoutUtils.isDebug) {
                        throw e;
                    }
                }
            }

            if (null != mViewLifeCycleHelper) {
                mViewLifeCycleHelper.checkViewStatusInScreen();
            }
        }
    }

    public void runAdjustLayout() {
        final int startPosition = findFirstVisibleItemPosition();
        final com.ashlikun.vlayout.LayoutHelper firstLayoutHelper = mHelperFinder.getLayoutHelper(startPosition);
        final int endPosition = findLastVisibleItemPosition();
        final com.ashlikun.vlayout.LayoutHelper lastLayoutHelper = mHelperFinder.getLayoutHelper(endPosition);
        List<com.ashlikun.vlayout.LayoutHelper> totalLayoutHelpers = mHelperFinder.getLayoutHelpers();
        final int start = totalLayoutHelpers.indexOf(firstLayoutHelper);
        final int end = totalLayoutHelpers.indexOf(lastLayoutHelper);
        for (int i = start; i <= end; i++) {
            try {
                totalLayoutHelpers.get(i).adjustLayout(startPosition, endPosition, this);
            } catch (Exception e) {
                if (VLayoutUtils.isDebug) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        Trace.beginSection(TRACE_LAYOUT);

        if (mNoScrolling && state.didStructureChange()) {
            mSpaceMeasured = false;
            mSpaceMeasuring = true;
        }


        runPreLayout(recycler, state);

        try {
            super.onLayoutChildren(recycler, state);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // MaX_VALUE 表示滚动偏移无效-无滚动
            runPostLayout(recycler, state, Integer.MAX_VALUE); // hack表示其初始布局
        }


        if ((mNestedScrolling || mNoScrolling) && mSpaceMeasuring) {
            // 需要测量，也需要测量
            mSpaceMeasured = true;
            // get last child
            int childCount = getChildCount();
            View lastChild = getChildAt(childCount - 1);
            if (lastChild != null) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) lastChild.getLayoutParams();
                // 找到最后一个子视图的结尾
                mMeasuredFullSpace = getDecoratedBottom(lastChild) + params.bottomMargin + computeAlignOffset(lastChild, true, false);

                if (mRecyclerView != null && mNestedScrolling) {
                    ViewParent parent = mRecyclerView.getParent();
                    if (parent instanceof View) {
                        // 确保全空间是测量的空间和父母身高的最小值
                        mMeasuredFullSpace = Math.min(mMeasuredFullSpace, ((View) parent).getMeasuredHeight());
                    }
                }
            }
            mSpaceMeasuring = false;
            if (mRecyclerView != null && getItemCount() > 0) {
                // relayout
                mRecyclerView.post(() -> {
                    // post relayout
                    if (mRecyclerView != null) {
                        mRecyclerView.requestLayout();
                    }
                });
            }
        }

        Trace.endSection();
    }

    /**
     * 滚动的输入方法
     * {@inheritDoc}
     */
    @Override
    protected int scrollInternalBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        Trace.beginSection(TRACE_SCROLL);

        runPreLayout(recycler, state);

        int scrolled = 0;
        try {
            if (!mNoScrolling) {
                scrolled = super.scrollInternalBy(dy, recycler, state);
            } else {
                if (getChildCount() == 0 || dy == 0) {
                    return 0;
                }

                mLayoutState.mRecycle = true;
                ensureLayoutStateExpose();
                final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
                final int absDy = Math.abs(dy);
                updateLayoutStateExpose(layoutDirection, absDy, true, state);
                final int freeScroll = mLayoutState.mScrollingOffset;

                final int consumed = freeScroll + fill(recycler, mLayoutState, state, false);
                if (consumed < 0) {
                    return 0;
                }
                scrolled = absDy > consumed ? layoutDirection * consumed : dy;
            }
        } catch (Exception e) {
            Log.w(TAG, Log.getStackTraceString(e), e);
            if (VLayoutUtils.isDebug) {
                throw e;
            }

        } finally {
            runPostLayout(recycler, state, scrolled);
        }

        Trace.endSection();

        return scrolled;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        int startPosition = findFirstVisibleItemPosition();
        int endPosition = findLastVisibleItemPosition();
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            layoutHelper.onScrollStateChanged(state, startPosition, endPosition, this);
        }
    }

    @Override
    public void offsetChildrenHorizontal(int dx) {
        super.offsetChildrenHorizontal(dx);

        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            layoutHelper.onOffsetChildrenHorizontal(dx, this);

        }
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        super.offsetChildrenVertical(dy);
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            layoutHelper.onOffsetChildrenVertical(dy, this);
        }

        if (null != mViewLifeCycleHelper) {
            mViewLifeCycleHelper.checkViewStatusInScreen();
        }
    }

    public void setViewLifeCycleListener(@NonNull ViewLifeCycleListener viewLifeCycleListener) {
        if (null == viewLifeCycleListener) {
            throw new IllegalArgumentException("ViewLifeCycleListener should not be null!");
        }

        mViewLifeCycleHelper = new ViewLifeCycleHelper(this, viewLifeCycleListener);
    }

    public int getVirtualLayoutDirection() {
        return mLayoutState.mLayoutDirection;
    }

    private LayoutStateWrapper mTempLayoutStateWrapper = new LayoutStateWrapper();

    private List<Pair<com.ashlikun.vlayout.Range<Integer>, Integer>> mRangeLengths = new ArrayList<>();

    @Nullable
    private int findRangeLength(@NonNull final com.ashlikun.vlayout.Range<Integer> range) {
        final int count = mRangeLengths.size();
        if (count == 0) {
            return -1;
        }

        int s = 0, e = count - 1, m = -1;
        Pair<com.ashlikun.vlayout.Range<Integer>, Integer> rs = null;

        // 二进制搜索范围
        while (s <= e) {
            m = (s + e) / 2;
            rs = mRangeLengths.get(m);

            com.ashlikun.vlayout.Range<Integer> r = rs.first;
            if (r == null) {
                rs = null;
                break;
            }

            if (r.contains(range.getLower()) || r.contains(range.getUpper()) || range.contains(r)) {
                break;
            } else if (r.getLower() > range.getUpper()) {
                e = m - 1;
            } else if (r.getUpper() < range.getLower()) {
                s = m + 1;
            }

            rs = null;
        }

        return rs == null ? -1 : m;
    }


    @Override
    protected void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState, com.ashlikun.vlayout.layout.LayoutChunkResult result) {
        final int position = layoutState.mCurrentPosition;
        mTempLayoutStateWrapper.mLayoutState = layoutState;
        com.ashlikun.vlayout.LayoutHelper layoutHelper = mHelperFinder == null ? null : mHelperFinder.getLayoutHelper(position);
        if (layoutHelper == null) {
            layoutHelper = mDefaultLayoutHelper;
        }

        layoutHelper.doLayout(recycler, state, mTempLayoutStateWrapper, result, this);


        mTempLayoutStateWrapper.mLayoutState = null;


        // no item consumed
        if (layoutState.mCurrentPosition == position) {
            if (VLayoutUtils.isDebug) {
                Log.w(TAG, "layoutHelper[" + layoutHelper.getClass().getSimpleName() + "@" + layoutHelper.toString() + "] consumes no item!");
            }
            // 因未消耗项目而中断
            result.mFinished = true;
        } else {
            // 更新每个布局Chuck通道中消耗的高度
            final int positionAfterLayout = layoutState.mCurrentPosition - layoutState.mItemDirection;
            final int consumed = result.mIgnoreConsumed ? 0 : result.mConsumed;

            // TODO: 支持reverseLayout时发生更改
            com.ashlikun.vlayout.Range<Integer> range = new com.ashlikun.vlayout.Range<>(Math.min(position, positionAfterLayout), Math.max(position, positionAfterLayout));

            final int idx = findRangeLength(range);
            if (idx >= 0) {
                Pair<com.ashlikun.vlayout.Range<Integer>, Integer> pair = mRangeLengths.get(idx);
                if (pair != null && pair.first.equals(range) && pair.second == consumed) {
                    return;
                }

                mRangeLengths.remove(idx);
            }

            mRangeLengths.add(Pair.create(range, consumed));
            Collections.sort(mRangeLengths, mRangeComparator);
        }
    }


    /**
     * 返回与顶部相关的当前位置，仅在从顶部滚动时有效
     *
     * @return 从当前位置到RecycledView原始顶部的偏移
     */
    public int getOffsetToStart() {
        if (getChildCount() == 0) {
            return -1;
        }

        final View view = getChildAt(0);

        if (view == null) {
            //例如，在某些情况下，在结束活动破坏时调用此方法可能会导致npe
            return -1;
        }

        int position = getPosition(view);
        final int idx = findRangeLength(com.ashlikun.vlayout.Range.create(position, position));
        if (idx < 0 || idx >= mRangeLengths.size()) {
            return -1;
        }

        int offset = -mOrientationHelper.getDecoratedStart(view);
        for (int i = 0; i < idx; i++) {
            Pair<Range<Integer>, Integer> pair = mRangeLengths.get(i);
            if (pair != null) {
                offset += pair.second;
            }
        }

        return offset;
    }


    private static com.ashlikun.vlayout.LayoutHelper DEFAULT_LAYOUT_HELPER = new DefaultLayoutHelper();

    private com.ashlikun.vlayout.LayoutHelper mDefaultLayoutHelper = DEFAULT_LAYOUT_HELPER;

    /**
     * 更改默认LayoutHelper
     *
     * @param layoutHelper 默认layoutHelper应用于没有指定layoutHelpers的项目，它不应为空
     */
    private void setDefaultLayoutHelper(@NonNull final com.ashlikun.vlayout.LayoutHelper layoutHelper) {
        //noinspection ConstantConditions
        if (layoutHelper == null) {
            throw new IllegalArgumentException("layoutHelper should not be null");
        }

        this.mDefaultLayoutHelper = layoutHelper;
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
    }


    @Override
    public void scrollToPositionWithOffset(int position, int offset) {
        super.scrollToPositionWithOffset(position, offset);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        super.smoothScrollToPosition(recyclerView, state, position);
    }


    @Override
    public boolean supportsPredictiveItemAnimations() {
        return mCurrentPendingSavedState == null;
    }


    /**
     * Do updates when items change
     *
     * @param recyclerView  recyclerView that belong to
     * @param positionStart start position that items changed
     * @param itemCount     number of items that changed
     */
    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            layoutHelper.onItemsChanged(this);
        }

        // setLayoutHelpers(mHelperFinder.getLayoutHelpers());
    }


    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof RecyclerView.LayoutParams) {
            return new LayoutParams((RecyclerView.LayoutParams) lp);
        } else if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new InflateLayoutParams(c, attrs);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
    }


    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);

        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            layoutHelper.clear(this);
        }

        mRecyclerView = null;
    }


    public static class LayoutParams extends RecyclerView.LayoutParams {
        public static final int INVALIDE_SIZE = Integer.MIN_VALUE;


        public int zIndex = 0;

        public float mAspectRatio = Float.NaN;

        private int mOriginWidth = INVALIDE_SIZE;
        private int mOriginHeight = INVALIDE_SIZE;


        public void storeOriginWidth() {
            if (mOriginWidth == INVALIDE_SIZE) {
                mOriginWidth = width;
            }
        }

        public void storeOriginHeight() {
            if (mOriginHeight == INVALIDE_SIZE) {
                mOriginHeight = height;
            }
        }

        public void restoreOriginWidth() {
            if (mOriginWidth != INVALIDE_SIZE) {
                width = mOriginWidth;
            }
        }

        public void restoreOriginHeight() {
            if (mOriginHeight != INVALIDE_SIZE) {
                height = mOriginHeight;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

    }

    public static class InflateLayoutParams extends LayoutParams {

        public InflateLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
    }


    public static class AnchorInfoWrapper {

        public int position;

        public int coordinate;

        public boolean layoutFromEnd;

        AnchorInfoWrapper() {

        }

    }


    public static class LayoutStateWrapper {
        public final static int LAYOUT_START = -1;

        public final static int LAYOUT_END = 1;

        final static int INVALID_LAYOUT = Integer.MIN_VALUE;

        public final static int ITEM_DIRECTION_HEAD = -1;

        public final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        private LayoutState mLayoutState;

        LayoutStateWrapper() {

        }

        LayoutStateWrapper(LayoutState layoutState) {
            this.mLayoutState = layoutState;
        }


        public int getOffset() {
            return mLayoutState.mOffset;
        }

        public int getCurrentPosition() {
            return mLayoutState.mCurrentPosition;
        }

        public boolean hasScrapList() {
            return mLayoutState.mScrapList != null;
        }

        public void skipCurrentPosition() {
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
        }

        /**
         * 在某些情况下，我们可能不想回收儿童（例如布局）
         */
        public boolean isRecycle() {
            return mLayoutState.mRecycle;
        }


        /**
         * 这 {@link #layoutChunk(RecyclerView.Recycler, RecyclerView.State, LayoutState, com.ashlikun.vlayout.layout.LayoutChunkResult)}通行证处于布局或滚动状态
         */
        public boolean isRefreshLayout() {
            return mLayoutState.mOnRefresLayout;
        }

        /**
         * 我们应该在布局方向上填充的像素数。
         */
        public int getAvailable() {
            return mLayoutState.mAvailable;
        }


        /**
         * 定义数据适配器的遍历方向。应该是 {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        public int getItemDirection() {
            return mLayoutState.mItemDirection;
        }

        /**
         * 定义填充布局的方向。应该是 {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        public int getLayoutDirection() {
            return mLayoutState.mLayoutDirection;
        }

        /**
         * 在滚动状态下构造LayoutState时使用。它应该设置为无需创建新视图即可进行的滚动量。这是高效视图回收所需的设置。
         */
        public int getScrollingOffset() {
            return mLayoutState.mScrollingOffset;
        }

        /**
         * 如果要预先布局尚不可见的项目，请使用。与{@link #getAvailable()}的区别在于，在回收时，
         * 不考虑为{@link #getExtra()} 设置距离以避免回收可见的子对象。
         */
        public int getExtra() {
            return mLayoutState.mExtra;
        }

        /**
         * 等于{@link RecyclerView.State#isPreLayout()}.当消耗废料时，如果此值设置为true，我们将跳过删除的视图，因为它们不应在布局后步骤中进行布局。
         */
        public boolean isPreLayout() {
            return mLayoutState.mIsPreLayout;
        }


        public boolean hasMore(RecyclerView.State state) {
            return mLayoutState.hasMore(state);
        }

        public View next(RecyclerView.Recycler recycler) {
            View next = mLayoutState.next(recycler);
            // 集合回收器
            return next;
        }

        public View retrieve(RecyclerView.Recycler recycler, int position) {
            int originPosition = mLayoutState.mCurrentPosition;
            mLayoutState.mCurrentPosition = position;
            View view = next(recycler);
            mLayoutState.mCurrentPosition = originPosition;
            return view;
        }
    }


    private static class LayoutViewHolder extends RecyclerView.ViewHolder {

        public LayoutViewHolder(View itemView) {
            super(itemView);
        }

    }


    public List<View> getFixedViews() {
        if (mRecyclerView == null) {
            return Collections.emptyList();
        }

        // TODO: support zIndex?
        List<View> views = new LinkedList<>();
        List<com.ashlikun.vlayout.LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        Iterator<com.ashlikun.vlayout.LayoutHelper> iterator = layoutHelpers.iterator();
        com.ashlikun.vlayout.LayoutHelper layoutHelper = null;
        while (iterator.hasNext()) {
            layoutHelper = iterator.next();
            View fixedView = layoutHelper.getFixedView();
            if (fixedView != null) {
                views.add(fixedView);
            }
        }

        return views;
    }


    private com.ashlikun.vlayout.LayoutViewFactory mLayoutViewFatory = new com.ashlikun.vlayout.LayoutViewFactory() {
        @Override
        public View generateLayoutView(@NonNull Context context) {
            return new LayoutView(context);
        }
    };

    /**
     * 设置LayoutView Factory，以便可以替换LayoutHelpers的LayoutView
     */
    public void setLayoutViewFactory(@NonNull final LayoutViewFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory should not be null");
        }
        mLayoutViewFatory = factory;
    }

    @Override
    public final View generateLayoutView() {
        if (mRecyclerView == null) {
            return null;
        }

        View layoutView = mLayoutViewFatory.generateLayoutView(mRecyclerView.getContext());
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        attachViewHolder(params, new LayoutViewHolder(layoutView));

        layoutView.setLayoutParams(params);
        return layoutView;
    }


    @Override
    public void addChildView(View view, int index) {
        super.addView(view, index);
    }


    @Override
    public void moveView(int fromIndex, int toIndex) {
        super.moveView(fromIndex, toIndex);
    }

    @Override
    public void addChildView(LayoutStateWrapper layoutState, View view) {
        addChildView(layoutState, view, layoutState.getItemDirection() == LayoutStateWrapper.ITEM_DIRECTION_TAIL ? -1 : 0);
    }


    @Override
    public void addChildView(LayoutStateWrapper layoutState, View view, int index) {
        showView(view);

        if (!layoutState.hasScrapList()) {
            //在剪贴簿中找不到
            addView(view, index);
        } else {
            addDisappearingView(view, index);
        }
    }

    @Override
    public void addOffFlowView(View view, boolean head) {
        showView(view);
        addHiddenView(view, head);

    }

    @Override
    public void addBackgroundView(View view, boolean head) {
        showView(view);
        int index = head ? 0 : -1;
        addView(view, index);
    }

    @Override
    public void addFixedView(View view) {
        //removeChildView(view);
        //mFixedContainer.addView(view);
        addOffFlowView(view, false);
    }

    @Override
    public void hideView(View view) {
        super.hideView(view);
    }

    @Override
    public void showView(View view) {
        super.showView(view);
    }

    @Override
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public RecyclerView.ViewHolder getChildViewHolder(View view) {
        if (mRecyclerView != null) {
            return mRecyclerView.getChildViewHolder(view);
        }
        return null;
    }

    @Override
    public boolean isViewHolderUpdated(View view) {
        RecyclerView.ViewHolder holder = getChildViewHolder(view);
        return holder == null || isViewHolderUpdated(holder);

    }

    @Override
    public void removeChildView(View child) {
        removeView(child);
    }

    @Override
    public com.ashlikun.vlayout.OrientationHelperEx getMainOrientationHelper() {
        return mOrientationHelper;
    }

    @Override
    public OrientationHelperEx getSecondaryOrientationHelper() {
        return mSecondaryOrientationHelper;
    }

    @Override
    public void measureChild(View child, int widthSpec, int heightSpec) {
        measureChildWithDecorations(child, widthSpec, heightSpec);
    }

    @Override
    public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
        measureChildWithDecorationsAndMargin(child, widthUsed, heightUsed);
    }

    @Override
    public int getChildMeasureSpec(int parentSize, int size, boolean canScroll) {
        return getChildMeasureSpec(parentSize, 0, size, canScroll);
    }


    @Override
    public boolean canScrollHorizontally() {
        boolean ret = true;
        if (layoutManagerCanScrollListener != null) {
            ret = ret && layoutManagerCanScrollListener.canScrollHorizontally();
        }
        return mCanScrollHorizontally && !mNoScrolling && ret;
    }

    @Override
    public boolean canScrollVertically() {
        boolean ret = true;
        if (layoutManagerCanScrollListener != null) {
            ret = ret && layoutManagerCanScrollListener.canScrollVertically();
        }
        return mCanScrollVertically && !mNoScrolling && ret;
    }

    @Override
    public void layoutChildWithMargins(View child, int left, int top, int right, int bottom) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_LAYOUT, child);
        }
        layoutDecorated(child, left + lp.leftMargin, top + lp.topMargin,
                right - lp.rightMargin, bottom - lp.bottomMargin);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_LAYOUT, child);
        }
    }

    @Override
    public void layoutChild(View child, int left, int top, int right, int bottom) {
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_LAYOUT, child);
        }
        layoutDecorated(child, left, top,
                right, bottom);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_LAYOUT, child);
        }
    }

    @Override
    protected void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }

        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "Recycling " + Math.abs(startIndex - endIndex) + " items");
        }

        if (endIndex > startIndex) {

            View endView = getChildAt(endIndex - 1);
            View startView = getChildAt(startIndex);

            int startPos = getPosition(startView);
            int endPos = getPosition(endView);

            int idx = startIndex;

            for (int i = startIndex; i < endIndex; i++) {
                View v = getChildAt(idx);
                int pos = getPosition(v);
                if (pos != RecyclerView.NO_POSITION) {
                    com.ashlikun.vlayout.LayoutHelper layoutHelper = mHelperFinder.getLayoutHelper(pos);
                    if (layoutHelper == null || layoutHelper.isRecyclable(pos, startPos, endPos, this, true)) {
                        removeAndRecycleViewAt(idx, recycler);
                    } else {
                        idx++;
                    }
                } else {
                    removeAndRecycleViewAt(idx, recycler);
                }
            }
        } else {

            View endView = getChildAt(startIndex);
            View startView = getChildAt(endIndex + 1);

            int startPos = getPosition(startView);
            int endPos = getPosition(endView);

            for (int i = startIndex; i > endIndex; i--) {
                View v = getChildAt(i);
                int pos = getPosition(v);
                if (pos != RecyclerView.NO_POSITION) {
                    com.ashlikun.vlayout.LayoutHelper layoutHelper = mHelperFinder.getLayoutHelper(pos);
                    if (layoutHelper == null || layoutHelper.isRecyclable(pos, startPos, endPos, this, false)) {
                        removeAndRecycleViewAt(i, recycler);
                    }
                } else {
                    removeAndRecycleViewAt(i, recycler);
                }
            }
        }
    }


    @Override
    public void detachAndScrapAttachedViews(RecyclerView.Recycler recycler) {
        int childCount = this.getChildCount();

        for (int i = childCount - 1; i >= 0; --i) {
            View v = this.getChildAt(i);
            RecyclerView.ViewHolder holder = getChildViewHolder(v);
            if (holder instanceof CacheViewHolder && ((CacheViewHolder) holder).needCached()) {
                // 标记不无效，忽略DataSetChange（），使ViewHolder自身保持数据
                ViewHolderWrapper.setFlags(holder, 0, FLAG_INVALID | FLAG_UPDATED);
            }
        }


        super.detachAndScrapAttachedViews(recycler);
    }

    @Override
    public void detachAndScrapViewAt(int index, RecyclerView.Recycler recycler) {
        View child = getChildAt(index);
        RecyclerView.ViewHolder holder = getChildViewHolder(child);
        if (holder instanceof CacheViewHolder && ((CacheViewHolder) holder).needCached()) {
            // mark not invalid
            ViewHolderWrapper.setFlags(holder, 0, FLAG_INVALID);
        }

        super.detachAndScrapViewAt(index, recycler);
    }

    @Override
    public void detachAndScrapView(View child, RecyclerView.Recycler recycler) {
        super.detachAndScrapView(child, recycler);
    }

    public interface CacheViewHolder {
        boolean needCached();
    }

    @Override
    public int getContentWidth() {
        return super.getWidth();
    }

    @Override
    public int getContentHeight() {
        return super.getHeight();
    }

    @Override
    public boolean isDoLayoutRTL() {
        return isLayoutRTL();
    }

    private Rect mDecorInsets = new Rect();

    private void measureChildWithDecorations(View child, int widthSpec, int heightSpec) {
        calculateItemDecorationsForChild(child, mDecorInsets);
        widthSpec = updateSpecWithExtra(widthSpec, mDecorInsets.left, mDecorInsets.right);
        heightSpec = updateSpecWithExtra(heightSpec, mDecorInsets.top, mDecorInsets.bottom);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_MEASURE, child);
        }
        child.measure(widthSpec, heightSpec);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_MEASURE, child);
        }
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        calculateItemDecorationsForChild(child, mDecorInsets);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();

        if (getOrientation() == RecyclerView.VERTICAL) {
            widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + mDecorInsets.left,
                    lp.rightMargin + mDecorInsets.right);
        }
        if (getOrientation() == RecyclerView.HORIZONTAL) {
            heightSpec = updateSpecWithExtra(heightSpec, mDecorInsets.top,
                    mDecorInsets.bottom);
        }
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_MEASURE, child);
        }
        child.measure(widthSpec, heightSpec);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_MEASURE, child);
        }
    }

    /**
     * 使用插图更新度量规范
     */
    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            int size = View.MeasureSpec.getSize(spec);
            if (size - startInset - endInset < 0) {
                return View.MeasureSpec.makeMeasureSpec(0, mode);
            } else {
                return View.MeasureSpec.makeMeasureSpec(
                        View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
            }
        }
        return spec;
    }


    @Override
    public View findViewByPosition(int position) {
        View view = super.findViewByPosition(position);
        if (view != null && getPosition(view) == position) {
            return view;
        }

        for (int i = 0; i < getChildCount(); i++) {
            view = getChildAt(i);
            if (view != null && getPosition(view) == position) {
                return view;
            }
        }

        return null;
    }


    @Override
    public void recycleView(View view) {
        if (mRecyclerView != null) {
            ViewParent parent = view.getParent();
            if (parent != null && parent == mRecyclerView) {
                RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                mRecyclerView.getRecycledViewPool().putRecycledView(holder);
            }
        }
    }

    @Override
    public LayoutHelper findLayoutHelperByPosition(int position) {
        return mHelperFinder.getLayoutHelper(position);
    }




    /*
     * 扩展到完整显示视图
     */


    // 当设置为不滚动时，最大大小应该有限制
    private static final int MAX_NO_SCROLLING_SIZE = Integer.MAX_VALUE >> 4;

    private boolean mSpaceMeasured = false;

    private int mMeasuredFullSpace = 0;

    private boolean mSpaceMeasuring = false;


    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
        if (!mNoScrolling && !mNestedScrolling) {

            super.onMeasure(recycler, state, widthSpec, heightSpec);
            return;
        }


        int initialSize = MAX_NO_SCROLLING_SIZE;

        if (mRecyclerView != null && mNestedScrolling) {
            if (mMaxMeasureSize > 0) {
                initialSize = mMaxMeasureSize;
            } else {
                ViewParent parent = mRecyclerView.getParent();
                if (parent instanceof View) {
                    initialSize = ((View) parent).getMeasuredHeight();
                }
            }
        }

        int measuredSize = mSpaceMeasured ? mMeasuredFullSpace : initialSize;

        if (mNoScrolling) {
            mSpaceMeasuring = !mSpaceMeasured;

            if (getChildCount() > 0 || getChildCount() != getItemCount()) {
                View lastChild = getChildAt(getChildCount() - 1);

                int bottom = mMeasuredFullSpace;
                if (lastChild != null) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) lastChild.getLayoutParams();
                    bottom = getDecoratedBottom(lastChild) + params.bottomMargin + computeAlignOffset(lastChild, true, false);
                }

                if (getChildCount() != getItemCount() || (lastChild != null && bottom != mMeasuredFullSpace)) {
                    measuredSize = MAX_NO_SCROLLING_SIZE;
                    mSpaceMeasured = false;
                    mSpaceMeasuring = true;
                }
            } else if (getItemCount() == 0) {
                measuredSize = 0;
                mSpaceMeasured = true;
                mSpaceMeasuring = false;
            }
        }


        if (getOrientation() == RecyclerView.VERTICAL) {
            super.onMeasure(recycler, state, widthSpec, View.MeasureSpec.makeMeasureSpec(measuredSize, View.MeasureSpec.AT_MOST));
        } else {
            super.onMeasure(recycler, state, View.MeasureSpec.makeMeasureSpec(measuredSize, View.MeasureSpec.AT_MOST), heightSpec);
        }
    }
}
