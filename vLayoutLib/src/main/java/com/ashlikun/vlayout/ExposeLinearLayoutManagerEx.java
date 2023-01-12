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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 此类用于公开layoutChunk方法，不应在其他任何地方使用。从技术上讲，它只是一个有效的类，其中没有功能
 *
 * @author villadora
 * @since 1.0.0
 */


/**
 * A {@link RecyclerView.LayoutManager} implementation 它提供了
 * 功能类似于 {@link android.widget.ListView}.
 */
class ExposeLinearLayoutManagerEx extends LinearLayoutManager {

    private static final String TAG = "ExposeLLManagerEx";

    private static final boolean DEBUG = false;

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    public static final int INVALID_OFFSET = Integer.MIN_VALUE;


    /**
     * 在尝试查找下一个要关注的视图时，LayoutManager不会尝试滚动更多
     * 大于此因子乘以列表的总空间。如果布局是垂直的，则总空间为
     * 高度减去填充，如果布局为水平，则总空间为宽度减去填充。
     */
    private static final float MAX_SCROLL_FACTOR = 0.33f;

    /**
     * 保持临时布局状态的帮助程序类。
     * 布局完成后，它不会保持状态，但我们仍保留一个引用以供重用
     * 相同的对象。
     */
    protected LayoutState mLayoutState;

    /**
     * 根据方向进行许多计算。为了保持干净，此界面
     * 帮助 {@link LinearLayoutManager} 做出这些决定。
     * 基于 {@link #mOrientation}, 实现是在
     * {@link #ensureLayoutStateExpose} 方法.
     */
    private OrientationHelperEx mOrientationHelper;

    /**
     * 我们需要跟踪这一点，以便在当前位置发生变化时忽略它。
     */
    private boolean mLastStackFromEnd;


    /**
     * 这将保留LayoutManager应如何开始布局视图的最终值。
     * 它是通过检查{@link getReverseLayout（）}和View的布局方向来计算的。
     * {@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)} 正在运行。
     */
    private boolean mShouldReverseLayoutExpose = false;

    /**
     * 当LayoutManager需要滚动到某个位置时，它会设置此变量并请求一个布局，该布局将检查此变量并相应地重新布局。
     */
    private int mCurrentPendingScrollPosition = RecyclerView.NO_POSITION;

    /**
     * 用于在以下情况下保持偏移值 {@link #scrollToPositionWithOffset(int, int)} 被调用.
     */
    private int mPendingScrollPositionOffset = INVALID_OFFSET;


    protected Bundle mCurrentPendingSavedState = null;

    /**
     * 重新使用的变量在重新布局时保留锚点信息。锚点位置和坐标定义了布局时LLM的参考点。
     */
    private final AnchorInfo mAnchorInfo;

    private final ChildHelperWrapper mChildHelperWrapper;

    private final Method mEnsureLayoutStateMethod;

    protected int recycleOffset;

    /**
     * 创建垂直LinearLayoutManager
     *
     * @param context 当前上下文将用于访问资源。
     */
    public ExposeLinearLayoutManagerEx(Context context) {
        this(context, VERTICAL, false);
    }


    /**
     * @param context       当前上下文将用于访问资源。
     * @param orientation   布局方向。应为{@link HORIGHT}或{@link VERTICAL}。
     * @param reverseLayout 当设置为true时，从结束到开始的布局。
     */
    public ExposeLinearLayoutManagerEx(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        mAnchorInfo = new AnchorInfo();
        setOrientation(orientation);
        setReverseLayout(reverseLayout);
        mChildHelperWrapper = new ChildHelperWrapper(this);


        try {
            mEnsureLayoutStateMethod = LinearLayoutManager.class.getDeclaredMethod("ensureLayoutState");
            mEnsureLayoutStateMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            // FIXME 未来
            Method setItemPrefetchEnabledMethod = RecyclerView.LayoutManager.class
                    .getDeclaredMethod("setItemPrefetchEnabled", boolean.class);
            if (setItemPrefetchEnabledMethod != null) {
                setItemPrefetchEnabledMethod.invoke(this, false);
            }
        } catch (NoSuchMethodException e) {
            /** 这个方法是在25.1.0中添加的，官方版本仍然有bug，请参见
             * https://code.google.com/p/android/issues/detail?can=2&start=0&num=100&q=&colspec=ID%20Status%20Priority%20Owner%20Summary%20Stars%20Reporter%20Opened&groupby=&sort=&id=230295
             **/
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }
//        setItemPrefetchEnabled(false);
    }


    @Override
    public Parcelable onSaveInstanceState() {
        if (mCurrentPendingSavedState != null) {
            return new Bundle(mCurrentPendingSavedState);
        }
        Bundle state = new Bundle();
        if (getChildCount() > 0) {
            boolean didLayoutFromEnd = mLastStackFromEnd ^ mShouldReverseLayoutExpose;
            state.putBoolean("AnchorLayoutFromEnd", didLayoutFromEnd);
            if (didLayoutFromEnd) {
                final View refChild = getChildClosestToEndExpose();
                state.putInt("AnchorOffset", mOrientationHelper.getEndAfterPadding() -
                        mOrientationHelper.getDecoratedEnd(refChild));
                state.putInt("AnchorPosition", getPosition(refChild));
            } else {
                final View refChild = getChildClosestToStartExpose();
                state.putInt("AnchorPosition", getPosition(refChild));
                state.putInt("AnchorOffset", mOrientationHelper.getDecoratedStart(refChild) -
                        mOrientationHelper.getStartAfterPadding());
            }
        } else {
            state.putInt("AnchorPosition", RecyclerView.NO_POSITION);
        }
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            mCurrentPendingSavedState = (Bundle) state;
            requestLayout();
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "loaded saved state");
            }
        } else if (VLayoutUtils.isDebug) {
            Log.d(TAG, "invalid saved state class");
        }
    }

    /**
     * 设置布局的方向。 {@link LinearLayoutManager}
     * 将尽力保持滚动位置。
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        mOrientationHelper = null;
    }

    public void setRecycleOffset(int recycleOffset) {
        this.recycleOffset = recycleOffset;
    }

    /**
     * 计算视图布局顺序。（例如，从结束到开始或从开始到结束）自动应用RTL布局支持。因此，如果布局为RTL
     * {@link #getReverseLayout()} is {@code true},元素将从左侧开始布置。
     */
    private void myResolveShouldLayoutReverse() {
        // A==B是相同的结果，但我们宁愿保持可读性
        if (getOrientation() == RecyclerView.VERTICAL || !isLayoutRTL()) {
            mShouldReverseLayoutExpose = getReverseLayout();
        } else {
            mShouldReverseLayoutExpose = !getReverseLayout();
        }
    }


    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final int direction = targetPosition < firstChildPos != mShouldReverseLayoutExpose ? -1 : 1;
        if (getOrientation() == RecyclerView.HORIZONTAL) {
            return new PointF(direction, 0);
        } else {
            return new PointF(0, direction);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        // 布局算法：
        // 1） 通过检查子项和其他变量，找到锚点坐标和锚点项位置。
        // 2） 向起点填充，从底部堆叠
        // 3） 向端填充，从顶部堆叠
        // 4） 滚动以满足从底部堆叠的要求。
        // 创建布局状态
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "is pre layout:" + state.isPreLayout());
        }
        if (mCurrentPendingSavedState != null && mCurrentPendingSavedState.getInt("AnchorPosition") >= 0) {
            mCurrentPendingScrollPosition = mCurrentPendingSavedState.getInt("AnchorPosition");
        }

        ensureLayoutStateExpose();
        mLayoutState.mRecycle = false;
        // resolve layout direction
        myResolveShouldLayoutReverse();

        mAnchorInfo.reset();
        mAnchorInfo.mLayoutFromEnd = mShouldReverseLayoutExpose ^ getStackFromEnd();
        // calculate anchor position and coordinate
        updateAnchorInfoForLayoutExpose(state, mAnchorInfo);


        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "Anchor info:" + mAnchorInfo);
        }

        // LLM可能决定为“额外”像素布局项目，以考虑滚动目标、缓存或预测动画。
        int extraForStart;
        int extraForEnd;
        final int extra = getExtraLayoutSpace(state);
        boolean before = state.getTargetScrollPosition() < mAnchorInfo.mPosition;
        if (before == mShouldReverseLayoutExpose) {
            extraForEnd = extra;
            extraForStart = 0;
        } else {
            extraForStart = extra;
            extraForEnd = 0;
        }
        extraForStart += mOrientationHelper.getStartAfterPadding();
        extraForEnd += mOrientationHelper.getEndPadding();
        if (state.isPreLayout() && mCurrentPendingScrollPosition != RecyclerView.NO_POSITION &&
                mPendingScrollPositionOffset != INVALID_OFFSET) {
            // 如果孩子是可见的，并且我们要移动它，我们应该在相反的方向上布置额外的项目，以确保新的项目很好地动画化，而不是仅仅淡出
            final View existing = findViewByPosition(mCurrentPendingScrollPosition);
            if (existing != null) {
                final int current;
                final int upcomingOffset;
                if (mShouldReverseLayoutExpose) {
                    current = mOrientationHelper.getEndAfterPadding() -
                            mOrientationHelper.getDecoratedEnd(existing);
                    upcomingOffset = current - mPendingScrollPositionOffset;
                } else {
                    current = mOrientationHelper.getDecoratedStart(existing)
                            - mOrientationHelper.getStartAfterPadding();
                    upcomingOffset = mPendingScrollPositionOffset - current;
                }
                if (upcomingOffset > 0) {
                    extraForStart += upcomingOffset;
                } else {
                    extraForEnd -= upcomingOffset;
                }
            }
        }
        int startOffset;
        int endOffset;
        onAnchorReady(state, mAnchorInfo);
        detachAndScrapAttachedViews(recycler);
        mLayoutState.mIsPreLayout = state.isPreLayout();
        mLayoutState.mOnRefresLayout = true;
        if (mAnchorInfo.mLayoutFromEnd) {
            // fill towards start
            updateLayoutStateToFillStartExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForStart;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
            if (mLayoutState.mAvailable > 0) {
                extraForEnd += mLayoutState.mAvailable;
            }
            // fill towards end
            updateLayoutStateToFillEndExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForEnd;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;
        } else {
            // fill towards end
            updateLayoutStateToFillEndExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForEnd;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;
            if (mLayoutState.mAvailable > 0) {
                extraForStart += mLayoutState.mAvailable;
            }
            // fill towards start
            updateLayoutStateToFillStartExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForStart;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
        }

        // 更改可能会导致UI上的空白，请尝试修复它们。
        // TODO 如果stackFromEndreverseLayoutRTL值都没有
        // 改变
        if (getChildCount() > 0) {
            // because layout from end may be changed by scroll to position
            // we re-calculate it.
            // find which side we should check for gaps.
            if (mShouldReverseLayoutExpose ^ getStackFromEnd()) {
                int fixOffset = fixLayoutEndGapExpose(endOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutStartGapExpose(startOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            } else {
                int fixOffset = fixLayoutStartGapExpose(startOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutEndGapExpose(endOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            }
        }
        layoutForPredictiveAnimationsExpose(recycler, state, startOffset, endOffset);
        if (!state.isPreLayout()) {
            mCurrentPendingScrollPosition = RecyclerView.NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
            mOrientationHelper.onLayoutComplete();
        }
        mLastStackFromEnd = getStackFromEnd();
        mCurrentPendingSavedState = null; // we don't need this anymore
        if (VLayoutUtils.isDebug) {
            validateChildOrderExpose();
        }
    }

    /**
     * 确定锚点位置时调用的方法。扩展类可以相应地设置，甚至在必要时更新锚点信息。
     *
     * @param state
     * @param anchorInfo 简单的数据结构，为下一个布局保留锚点信息
     */
    public void onAnchorReady(RecyclerView.State state, AnchorInfo anchorInfo) {
    }


    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        mRecyclerView = null;
    }

    @Override
    public int findFirstVisibleItemPosition() {
        ensureLayoutStateExpose();
        return super.findFirstVisibleItemPosition();
    }


    @Override
    public int findLastVisibleItemPosition() {
        ensureLayoutStateExpose();
        try {
            return super.findLastVisibleItemPosition();
        } catch (Exception e) {
            Log.d("LastItem", "itemCount: " + getItemCount());
            Log.d("LastItem", "childCount: " + getChildCount());
            Log.d("LastItem", "child: " + getChildAt(getChildCount() - 1));
            Log.d("LastItem", "RV childCount: " + mRecyclerView.getChildCount());
            Log.d("LastItem", "RV child: " + mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1));
            throw e;
        }
    }

    private View myFindReferenceChildClosestToEnd(RecyclerView.State state) {
        return this.mShouldReverseLayoutExpose ? this.myFindFirstReferenceChild(state.getItemCount()) : this.myFindLastReferenceChild(state.getItemCount());
    }

    private View myFindReferenceChildClosestToStart(RecyclerView.State state) {
        return this.mShouldReverseLayoutExpose ? this.myFindLastReferenceChild(state.getItemCount()) : this.myFindFirstReferenceChild(state.getItemCount());
    }


    private View myFindFirstReferenceChild(int itemCount) {
        return this.findReferenceChildInternal(0, this.getChildCount(), itemCount);
    }

    private View myFindLastReferenceChild(int itemCount) {
        return this.findReferenceChildInternal(this.getChildCount() - 1, -1, itemCount);
    }


    private View findReferenceChildInternal(int start, int end, int itemCount) {
        this.ensureLayoutStateExpose();
        View invalidMatch = null;
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;

        for (int i = start; i != end; i += diff) {
            View view = this.getChildAt(i);
            int position = this.getPosition(view);
            if (position >= 0 && position < itemCount) {
                if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else {
                    if (this.mOrientationHelper.getDecoratedStart(view) < boundsEnd && this.mOrientationHelper.getDecoratedEnd(view) >= boundsStart) {
                        return view;
                    }

                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                }
            }
        }

        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    /**
     * 如有必要，为预测性动画布局新项目
     */
    private void layoutForPredictiveAnimationsExpose(RecyclerView.Recycler recycler,
                                                     RecyclerView.State state, int startOffset, int endOffset) {
        // 如果有我们没有布局的废弃子对象，我们需要找到它们的位置，并相应地布局它们，以便动画可以按预期工作。
        // 如果添加了新视图或现有视图展开并将另一个视图推出边界，则可能会出现这种情况。
        if (!state.willRunPredictiveAnimations() || getChildCount() == 0 || state.isPreLayout()
                || !supportsPredictiveItemAnimations()) {
            return;
        }

        // 为了简化逻辑，我们计算子级的大小并调用fill.
        int scrapExtraStart = 0, scrapExtraEnd = 0;
        final List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        final int scrapSize = scrapList.size();
        final int firstChildPos = getPosition(getChildAt(0));
        for (int i = 0; i < scrapSize; i++) {
            RecyclerView.ViewHolder scrap = scrapList.get(i);
            final int position = scrap.getLayoutPosition();
            final int direction = position < firstChildPos != mShouldReverseLayoutExpose
                    ? LayoutState.LAYOUT_START : LayoutState.LAYOUT_END;
            if (direction == LayoutState.LAYOUT_START) {
                scrapExtraStart += mOrientationHelper.getDecoratedMeasurement(scrap.itemView);
            } else {
                scrapExtraEnd += mOrientationHelper.getDecoratedMeasurement(scrap.itemView);
            }
        }

        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "for unused scrap, decided to add " + scrapExtraStart
                    + " towards start and " + scrapExtraEnd + " towards end");
        }
        mLayoutState.mScrapList = scrapList;
        if (scrapExtraStart > 0) {
            View anchor = getChildClosestToStartExpose();
            updateLayoutStateToFillStartExpose(getPosition(anchor), startOffset);
            mLayoutState.mExtra = scrapExtraStart;
            mLayoutState.mAvailable = 0;
            mLayoutState.mCurrentPosition += mShouldReverseLayoutExpose ? 1 : -1;
            mLayoutState.mOnRefresLayout = true;
            fill(recycler, mLayoutState, state, false);
        }

        if (scrapExtraEnd > 0) {
            View anchor = getChildClosestToEndExpose();
            updateLayoutStateToFillEndExpose(getPosition(anchor), endOffset);
            mLayoutState.mExtra = scrapExtraEnd;
            mLayoutState.mAvailable = 0;
            mLayoutState.mCurrentPosition += mShouldReverseLayoutExpose ? -1 : 1;
            mLayoutState.mOnRefresLayout = true;
            fill(recycler, mLayoutState, state, false);
        }
        mLayoutState.mScrapList = null;
    }

    private void updateAnchorInfoForLayoutExpose(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (updateAnchorFromPendingDataExpose(state, anchorInfo)) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "updated anchor info from pending information");
            }
            return;
        }

        if (updateAnchorFromChildrenExpose(state, anchorInfo)) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "updated anchor info from existing children");
            }
            return;
        }
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "deciding anchor info for fresh state");
        }
        anchorInfo.assignCoordinateFromPadding();
        anchorInfo.mPosition = getStackFromEnd() ? state.getItemCount() - 1 : 0;
    }

    /**
     * 从现有视图中查找锚点子级。大多数情况下，这是最接近起点或终点的视图，具有有效位置（例如，未删除）。
     * <p/>
     * 如果孩子有专注力，就优先考虑。
     */
    private boolean updateAnchorFromChildrenExpose(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (getChildCount() == 0) {
            return false;
        }
        View focused = getFocusedChild();
        if (focused != null && anchorInfo.assignFromViewIfValid(focused, state)) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "decided anchor child from focused view");
            }
            return true;
        }

        if (mLastStackFromEnd != getStackFromEnd()) {
            return false;
        }


        final View referenceChild = anchorInfo.mLayoutFromEnd ?
                myFindReferenceChildClosestToEnd(state)
                : myFindReferenceChildClosestToStart(state);


        if (referenceChild != null) {
            anchorInfo.assignFromView(referenceChild);
            // 如果在一次过程中删除了所有可见视图，则引用子级可能超出范围。
            // 如果是这种情况，请将其偏移回0，以便使用这些预布局子项。
            if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
                // 验证此子级至少部分可见。如果没有，请将其偏移以开始
                final boolean notVisible =
                        mOrientationHelper.getDecoratedStart(referenceChild) >= mOrientationHelper
                                .getEndAfterPadding()
                                || mOrientationHelper.getDecoratedEnd(referenceChild)
                                < mOrientationHelper.getStartAfterPadding();
                if (notVisible) {
                    anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd
                            ? mOrientationHelper.getEndAfterPadding()
                            : mOrientationHelper.getStartAfterPadding();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 如果存在挂起的滚动位置或保存的状态，则根据该数据更新锚点信息并返回true
     */
    private boolean updateAnchorFromPendingDataExpose(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (state.isPreLayout() || mCurrentPendingScrollPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        // 验证滚动位置
        if (mCurrentPendingScrollPosition < 0 || mCurrentPendingScrollPosition >= state.getItemCount()) {
            mCurrentPendingScrollPosition = RecyclerView.NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
            if (VLayoutUtils.isDebug) {
                Log.e(TAG, "ignoring invalid scroll position " + mCurrentPendingScrollPosition);
            }
            return false;
        }

        // 如果子级可见，请尝试将其设置为引用子级，并确保其完全可见。
        // 如果子对象不可见，请根据其虚拟位置将其对齐。
        anchorInfo.mPosition = mCurrentPendingScrollPosition;
        if (mCurrentPendingSavedState != null && mCurrentPendingSavedState.getInt("AnchorPosition") >= 0) {
            // 锚点偏移取决于孩子的布局。这里，我们根据当前视图边界更新它
            anchorInfo.mLayoutFromEnd = mCurrentPendingSavedState.getBoolean("AnchorLayoutFromEnd");
            if (anchorInfo.mLayoutFromEnd) {
                anchorInfo.mCoordinate = mOrientationHelper.getEndAfterPadding() -
                        mCurrentPendingSavedState.getInt("AnchorOffset");
            } else {
                anchorInfo.mCoordinate = mOrientationHelper.getStartAfterPadding() +
                        mCurrentPendingSavedState.getInt("AnchorOffset");
            }
            return true;
        }

        if (mPendingScrollPositionOffset == INVALID_OFFSET) {
            View child = findViewByPosition(mCurrentPendingScrollPosition);
            if (child != null) {
                final int childSize = mOrientationHelper.getDecoratedMeasurement(child);
                if (childSize > mOrientationHelper.getTotalSpace()) {
                    // 项目不适合。根据布局方向固定
                    anchorInfo.assignCoordinateFromPadding();
                    return true;
                }
                final int startGap = mOrientationHelper.getDecoratedStart(child)
                        - mOrientationHelper.getStartAfterPadding();
                if (startGap < 0) {
                    anchorInfo.mCoordinate = mOrientationHelper.getStartAfterPadding();
                    anchorInfo.mLayoutFromEnd = false;
                    return true;
                }
                final int endGap = mOrientationHelper.getEndAfterPadding() -
                        mOrientationHelper.getDecoratedEnd(child);
                if (endGap < 0) {
                    anchorInfo.mCoordinate = mOrientationHelper.getEndAfterPadding();
                    anchorInfo.mLayoutFromEnd = true;
                    return true;
                }
                anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd
                        ? (mOrientationHelper.getDecoratedEnd(child) + mOrientationHelper
                        .getTotalSpaceChange())
                        : mOrientationHelper.getDecoratedStart(child);
            } else { // 项目不可见。
                if (getChildCount() > 0) {
                    // 获得任何孩子的位置，无所谓
                    int pos = getPosition(getChildAt(0));
                    anchorInfo.mLayoutFromEnd = mCurrentPendingScrollPosition < pos
                            == mShouldReverseLayoutExpose;
                }
                anchorInfo.assignCoordinateFromPadding();
            }
            return true;
        }
        // 从端点值覆盖布局以实现一致性
        anchorInfo.mLayoutFromEnd = mShouldReverseLayoutExpose;
        if (mShouldReverseLayoutExpose) {
            anchorInfo.mCoordinate = mOrientationHelper.getEndAfterPadding() -
                    mPendingScrollPositionOffset;
        } else {
            anchorInfo.mCoordinate = mOrientationHelper.getStartAfterPadding() +
                    mPendingScrollPositionOffset;
        }
        return true;
    }

    /**
     * @return 子项的最终抵销金额
     */
    private int fixLayoutEndGapExpose(int endOffset, RecyclerView.Recycler recycler,
                                      RecyclerView.State state, boolean canOffsetChildren) {
        int gap = mOrientationHelper.getEndAfterPadding() - endOffset;
        int fixOffset = 0;
        if (gap > 0) {
            fixOffset = -scrollInternalBy(-gap, recycler, state);
        } else {
            return 0; // nothing to fix
        }
        // 根据滚动量移动偏移
        endOffset += fixOffset;
        if (canOffsetChildren) {
            // re-calculate gap, see if we could fix it
            gap = mOrientationHelper.getEndAfterPadding() - endOffset;
            if (gap > 0) {
                mOrientationHelper.offsetChildren(gap);
                return gap + fixOffset;
            }
        }
        return fixOffset;
    }

    /**
     * @return 子项的最终抵销金额
     */
    private int fixLayoutStartGapExpose(int startOffset, RecyclerView.Recycler recycler,
                                        RecyclerView.State state, boolean canOffsetChildren) {
        int gap = startOffset - mOrientationHelper.getStartAfterPadding();
        int fixOffset = 0;
        if (gap > 0) {
            // 看看我们是否应该弥补这个差距。
            fixOffset = -scrollInternalBy(gap, recycler, state);
        } else {
            return 0; // 无需修复
        }
        startOffset += fixOffset;
        if (canOffsetChildren) {
            // 重新计算差距，看看我们能否解决
            gap = startOffset - mOrientationHelper.getStartAfterPadding();
            if (gap > 0) {
                mOrientationHelper.offsetChildren(-gap);
                return fixOffset - gap;
            }
        }
        return fixOffset;
    }

    private void updateLayoutStateToFillEndExpose(AnchorInfo anchorInfo) {
        updateLayoutStateToFillEndExpose(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillEndExpose(int itemPosition, int offset) {
        mLayoutState.mAvailable = mOrientationHelper.getEndAfterPadding() - offset;
        mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_HEAD :
                LayoutState.ITEM_DIRECTION_TAIL;
        mLayoutState.mCurrentPosition = itemPosition;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mOffset = offset;
        mLayoutState.mScrollingOffset = LayoutState.SCOLLING_OFFSET_NaN;
    }

    private void updateLayoutStateToFillStartExpose(AnchorInfo anchorInfo) {
        updateLayoutStateToFillStartExpose(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillStartExpose(int itemPosition, int offset) {
        mLayoutState.mAvailable = offset - mOrientationHelper.getStartAfterPadding();
        mLayoutState.mCurrentPosition = itemPosition;
        mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_TAIL :
                LayoutState.ITEM_DIRECTION_HEAD;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
        mLayoutState.mOffset = offset;
        mLayoutState.mScrollingOffset = LayoutState.SCOLLING_OFFSET_NaN;

    }

    private Object[] emptyArgs = new Object[0];

    protected void ensureLayoutStateExpose() {
        if (mLayoutState == null) {
            mLayoutState = new LayoutState();
        }

        if (mOrientationHelper == null) {
            mOrientationHelper = OrientationHelperEx.createOrientationHelper(this, getOrientation());
        }

        try {
            mEnsureLayoutStateMethod.invoke(this, emptyArgs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>滚动RecyclerView以使位置可见。</p>
     * <p/>
     * <p>RecyclerView将滚动使目标位置可见所需的最小量。如果您正在寻找与
     * {@link android.widget.ListView#setSelection(int)} or
     * {@link android.widget.ListView#setSelectionFromTop(int, int)}, use
     * {@link #scrollToPositionWithOffset(int, int)}.</p>
     * <p/>
     * <p>请注意，在下一次布局调用之前，滚动位置更改不会反映出来。</p>
     *
     * @param position 滚动到此适配器位置
     * @see #scrollToPositionWithOffset(int, int)
     */
    @Override
    public void scrollToPosition(int position) {
        mCurrentPendingScrollPosition = position;
        mPendingScrollPositionOffset = INVALID_OFFSET;
        if (mCurrentPendingSavedState != null) {
            mCurrentPendingSavedState.putInt("AnchorPosition", RecyclerView.NO_POSITION);
        }
        requestLayout();
    }

    /**
     * 滚动到指定的适配器位置，该位置与解析的布局起点具有给定的偏移量。解析的布局开始取决于 {@link #getReverseLayout()},
     * {@link ViewCompat#getLayoutDirection(View)} and {@link #getStackFromEnd()}.
     * <p/>
     * 例如，如果布局为{@link #VERTICAL} and {@link #getStackFromEnd()} 是真的，正在调用
     * <code>scrollToPositionWithOffset(10, 20)</code> 将布局为
     * <code>item[10]</code>的底部比RecyclerView的底部高20像素。
     * <p/>
     * 请注意，在下一次布局调用之前，滚动位置更改不会反映出来。
     * <p/>
     * <p/>
     * 如果您只是想让某个位置可见，请使用 {@link #scrollToPosition(int)}.
     *
     * @param position 引用项的索引（从0开始）。
     * @param offset   项目视图的开始边缘和RecyclerView的开始边缘之间的距离（以像素为单位）。
     * @see #setReverseLayout(boolean)
     * @see #scrollToPosition(int)
     */
    @Override
    public void scrollToPositionWithOffset(int position, int offset) {
        mCurrentPendingScrollPosition = position;
        mPendingScrollPositionOffset = offset;
        if (mCurrentPendingSavedState != null) {
            mCurrentPendingSavedState.putInt("AnchorPosition", RecyclerView.NO_POSITION);
        }
        requestLayout();
    }

    protected void updateLayoutStateExpose(int layoutDirection, int requiredSpace,
                                           boolean canUseExistingSpace, RecyclerView.State state) {
        mLayoutState.mExtra = getExtraLayoutSpace(state);
        mLayoutState.mLayoutDirection = layoutDirection;
        int fastScrollSpace;
        if (layoutDirection == LayoutState.LAYOUT_END) {
            mLayoutState.mExtra += mOrientationHelper.getEndPadding();
            // 把第一个孩子带到我们要去的方向
            final View child = getChildClosestToEndExpose();
            // 我们穿越孩子们的方向
            mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_HEAD
                    : LayoutState.ITEM_DIRECTION_TAIL;
            mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
            mLayoutState.mOffset = mOrientationHelper.getDecoratedEnd(child) + computeAlignOffset(child, true, false);
            // 计算在不添加新子项的情况下可以滚动多少（与布局无关）
            fastScrollSpace = mLayoutState.mOffset
                    - mOrientationHelper.getEndAfterPadding();

        } else {
            final View child = getChildClosestToStartExpose();
            mLayoutState.mExtra += mOrientationHelper.getStartAfterPadding();
            mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_TAIL
                    : LayoutState.ITEM_DIRECTION_HEAD;
            mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;

            mLayoutState.mOffset = mOrientationHelper.getDecoratedStart(child) + computeAlignOffset(child, false, false);
            fastScrollSpace = -mLayoutState.mOffset
                    + mOrientationHelper.getStartAfterPadding();
        }
        mLayoutState.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            mLayoutState.mAvailable -= fastScrollSpace;
        }
        mLayoutState.mScrollingOffset = fastScrollSpace;
    }

    /**
     * 在滚动期间填充视图时调整对齐偏移，或在从锚点布局时获取边距
     *
     * @param child
     * @param isLayoutEnd
     * @return
     */
    protected int computeAlignOffset(View child, boolean isLayoutEnd, boolean useAnchor) {
        return 0;
    }

    /**
     * 在滚动期间填充视图时调整对齐偏移，或在从锚点布局时获取边距
     *
     * @param position
     * @param isLayoutEnd
     * @return
     */
    protected int computeAlignOffset(int position, boolean isLayoutEnd, boolean useAnchor) {
        return 0;
    }

    public boolean isEnableMarginOverLap() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        if (getOrientation() == RecyclerView.VERTICAL) {
            return 0;
        }
        return scrollInternalBy(dx, recycler, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        if (getOrientation() == RecyclerView.HORIZONTAL) {
            return 0;
        }
        return scrollInternalBy(dy, recycler, state);
    }

    /**
     * 内部处理滚动事件，包括水平和垂直
     *
     * @param dy       将滚动的像素
     * @param recycler 回收者持有回收视图
     * @param state    当前{@link RecyclerView} 状态，保持是否在preLayout等中。
     * @return 实际滚动的像素，它可能与{@code dy}不同，就像到达视图边缘时一样
     */
    protected int scrollInternalBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }


        // 指示此过程中是否需要回收，滚动时为true，布局时为false
        mLayoutState.mRecycle = true;
        ensureLayoutStateExpose();
        final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDy = Math.abs(dy);
        updateLayoutStateExpose(layoutDirection, absDy, true, state);
        final int freeScroll = mLayoutState.mScrollingOffset;

        mLayoutState.mOnRefresLayout = false;

        final int consumed = freeScroll + fill(recycler, mLayoutState, state, false);
        if (consumed < 0) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "Don't have any more elements to scroll");
            }
            return 0;
        }
        final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
        mOrientationHelper.offsetChildren(-scrolled);
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "scroll req: " + dy + " scrolled: " + scrolled);
        }

        return scrolled;
    }

    @Override
    public void assertNotInLayoutOrScroll(String message) {
        if (mCurrentPendingSavedState == null) {
            super.assertNotInLayoutOrScroll(message);
        }
    }

    /**
     * 在给定索引之间循环使用子项。
     *
     * @param startIndex inclusive
     * @param endIndex   exclusive
     */
    protected void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "Recycling " + Math.abs(startIndex - endIndex) + " items");
        }
        if (endIndex > startIndex) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = startIndex; i > endIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    /**
     * 回收滚动到布局末尾后超出边界的视图。
     *
     * @param recycler 的回收器实例 {@link RecyclerView}
     * @param dt       这可用于向可视区域添加额外的填充。这是用来
     *                 检测滚动后将超出边界的子对象，而不实际移动它们。
     */
    private void recycleViewsFromStartExpose(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "Called recycle from start with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
            return;
        }
        // 忽略填充，ViewGroup可能不会剪辑子对象。
        final int limit = dt;
        final int childCount = getChildCount();
        if (mShouldReverseLayoutExpose) {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedEnd(child) + recycleOffset > limit) {// stop here
                    recycleChildren(recycler, childCount - 1, i);
                    return;
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedEnd(child) + recycleOffset > limit) {// stop here
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        }
    }


    /**
     * 回收滚动到布局开始位置后超出边界的视图。
     *
     * @param recycler 的回收器实例 {@link RecyclerView}
     * @param dt       这可用于向可视区域添加额外的填充。这用于检测滚动后将超出边界的子对象，而不实际移动它们。
     */
    private void recycleViewsFromEndExpose(RecyclerView.Recycler recycler, int dt) {
        final int childCount = getChildCount();
        if (dt < 0) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "Called recycle from end with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
            return;
        }
        final int limit = mOrientationHelper.getEnd() - dt;
        if (mShouldReverseLayoutExpose) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedStart(child) - recycleOffset < limit) {// stop here
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        } else {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedStart(child) - recycleOffset < limit) {// stop here
                    recycleChildren(recycler, childCount - 1, i);
                    return;
                }
            }
        }

    }

    /**
     * 根据当前布局方向调用适当回收方法的Helper方法
     *
     * @param recycler    连接到RecyclerView的当前回收器
     * @param layoutState 当前布局状态。现在，这个对象不会改变，但我们可以考虑将它移出这个视图，以便现在作为参数传递，而不是访问{@link #mLayoutState}
     * @see #recycleViewsFromStartExpose(RecyclerView.Recycler, int)
     * @see #recycleViewsFromEndExpose(RecyclerView.Recycler, int)
     * @see LinearLayoutManager.LayoutState#mLayoutDirection
     */
    private void recycleByLayoutStateExpose(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (!layoutState.mRecycle) {
            return;
        }
        if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            recycleViewsFromEndExpose(recycler, layoutState.mScrollingOffset);
        } else {
            recycleViewsFromStartExpose(recycler, layoutState.mScrollingOffset);
        }
    }

    private com.ashlikun.vlayout.layout.LayoutChunkResult layoutChunkResultCache
            = new com.ashlikun.vlayout.layout.LayoutChunkResult();

    /**
     * 魔法功能：）。填充由layoutState定义的给定布局。这与 {@link LinearLayoutManager}
     * 并且几乎没有变化，可以作为助手类公开提供。
     *
     * @param recycler        连接到RecyclerView的当前回收器
     * @param layoutState     关于如何填充可用空间的配置。
     * @param state           RecyclerView传递的用于控制滚动步骤的上下文。
     * @param stopOnFocusable 如果为true，填充将在第一个可聚焦的新子对象中停止
     * @return 它添加的像素数。用于scoll函数。
     */
    protected int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
                       RecyclerView.State state, boolean stopOnFocusable) {
        // 我们应该设置的最大偏移量是mFastScroll+可用
        final int start = layoutState.mAvailable;
        if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
            // TODO ugly bug fix. should not happen
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutStateExpose(recycler, layoutState);
        }
        int remainingSpace = layoutState.mAvailable + layoutState.mExtra + recycleOffset;
        while (remainingSpace > 0 && layoutState.hasMore(state)) {
            layoutChunkResultCache.resetInternal();
            layoutChunk(recycler, state, layoutState, layoutChunkResultCache);
            if (layoutChunkResultCache.mFinished) {
                break;
            }
            layoutState.mOffset += layoutChunkResultCache.mConsumed * layoutState.mLayoutDirection;
            /**
             * 如果：layoutChunk没有请求忽略，或者我们正在布局废弃子项，或者我们没有进行预布局，则使用可用空间
             */
            if (!layoutChunkResultCache.mIgnoreConsumed || mLayoutState.mScrapList != null
                    || !state.isPreLayout()) {
                layoutState.mAvailable -= layoutChunkResultCache.mConsumed;
                // 我们保留一个单独的剩余空间，因为可用空间对回收很重要
                remainingSpace -= layoutChunkResultCache.mConsumed;
            }

            if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResultCache.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleByLayoutStateExpose(recycler, layoutState);
            }
            if (stopOnFocusable && layoutChunkResultCache.mFocusable) {
                break;
            }
        }
        if (VLayoutUtils.isDebug) {
            validateChildOrderExpose();
        }
        return start - layoutState.mAvailable;
    }

    protected void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
                               LayoutState layoutState, com.ashlikun.vlayout.layout.LayoutChunkResult result) {
        View view = layoutState.next(recycler);
        if (view == null) {
            if (DEBUG && layoutState.mScrapList == null) {
                throw new RuntimeException("received null view when unexpected");
            }
            // 如果我们在废料中布局视图，这可能会返回null，这意味着没有更多的项目要布局。
            result.mFinished = true;
            return;
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutState.mScrapList == null) {
            // 在剪贴簿中找不到
            if (mShouldReverseLayoutExpose == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addView(view);
            } else {
                addView(view, 0);
            }
        } else {
            if (mShouldReverseLayoutExpose == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addDisappearingView(view);
            } else {
                addDisappearingView(view, 0);
            }
        }
        measureChildWithMargins(view, 0, 0);
        result.mConsumed = mOrientationHelper.getDecoratedMeasurement(view);
        int left, top, right, bottom;
        if (getOrientation() == RecyclerView.VERTICAL) {
            if (isLayoutRTL()) {
                right = getWidth() - getPaddingRight();
                left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = getPaddingLeft();
                right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
            }

            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                bottom = layoutState.mOffset;
                top = layoutState.mOffset - result.mConsumed;
            } else {
                top = layoutState.mOffset;
                bottom = layoutState.mOffset + result.mConsumed;
            }
        } else {
            top = getPaddingTop();
            bottom = top + mOrientationHelper.getDecoratedMeasurementInOther(view);

            // 此布局过程是从开始到结束还是反向
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                right = layoutState.mOffset;
                left = layoutState.mOffset - result.mConsumed;
            } else {
                left = layoutState.mOffset;
                right = layoutState.mOffset + result.mConsumed;
            }
        }
        // 我们使用View的边界框（包括装饰和边距）计算所有内容。为了计算正确的布局位置，我们减去边距。
        layoutDecorated(view, left + params.leftMargin, top + params.topMargin,
                right - params.rightMargin, bottom - params.bottomMargin);
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "laid out child at position " + getPosition(view) + ", with l:"
                    + (left + params.leftMargin) + ", t:" + (top + params.topMargin) + ", r:"
                    + (right - params.rightMargin) + ", b:" + (bottom - params.bottomMargin));
        }
        // 如果未删除或更改视图，则占用可用空间
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }
        result.mFocusable = view.isFocusable();
    }

    /**
     * 将focusDirection转换为方向。
     *
     * @param focusDirection One of {@link View#FOCUS_UP}, {@link View#FOCUS_DOWN},
     *                       {@link View#FOCUS_LEFT}, {@link View#FOCUS_RIGHT},
     *                       {@link View#FOCUS_BACKWARD}, {@link View#FOCUS_FORWARD}
     *                       or 0 for not applicable
     * @return {@link LayoutState#LAYOUT_START} or {@link LayoutState#LAYOUT_END} if focus direction
     * is applicable to current state, {@link LayoutState#INVALID_LAYOUT} otherwise.
     */
    private int convertFocusDirectionToLayoutDirectionExpose(int focusDirection) {
        int orientation = getOrientation();
        switch (focusDirection) {
            case View.FOCUS_BACKWARD:
                return LayoutState.LAYOUT_START;
            case View.FOCUS_FORWARD:
                return LayoutState.LAYOUT_END;
            case View.FOCUS_UP:
                return orientation == VERTICAL ? LayoutState.LAYOUT_START
                        : LayoutState.INVALID_LAYOUT;
            case View.FOCUS_DOWN:
                return orientation == VERTICAL ? LayoutState.LAYOUT_END
                        : LayoutState.INVALID_LAYOUT;
            case View.FOCUS_LEFT:
                return orientation == HORIZONTAL ? LayoutState.LAYOUT_START
                        : LayoutState.INVALID_LAYOUT;
            case View.FOCUS_RIGHT:
                return orientation == HORIZONTAL ? LayoutState.LAYOUT_END
                        : LayoutState.INVALID_LAYOUT;
            default:
                if (VLayoutUtils.isDebug) {
                    Log.d(TAG, "Unknown focus request:" + focusDirection);
                }
                return LayoutState.INVALID_LAYOUT;
        }

    }

    /**
     * 找到孩子的便捷方法。呼叫者应该检查它是否有足够的孩子。
     *
     * @return 从用户的角度看，子项关闭到布局的开始。
     */
    private View getChildClosestToStartExpose() {
        return getChildAt(mShouldReverseLayoutExpose ? getChildCount() - 1 : 0);
    }

    /**
     * 找到孩子接近终点的便捷方法。呼叫者应该检查它是否有足够的孩子。
     *
     * @return 从用户的角度看，子项接近布局的末尾。
     */
    private View getChildClosestToEndExpose() {
        return getChildAt(mShouldReverseLayoutExpose ? 0 : getChildCount() - 1);
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection,
                                    RecyclerView.Recycler recycler, RecyclerView.State state) {
        myResolveShouldLayoutReverse();
        if (getChildCount() == 0) {
            return null;
        }

        final int layoutDir = convertFocusDirectionToLayoutDirectionExpose(focusDirection);
        if (layoutDir == LayoutState.INVALID_LAYOUT) {
            return null;
        }

        View referenceChild = null;
        if (layoutDir == LayoutState.LAYOUT_START) {
            referenceChild = myFindReferenceChildClosestToStart(state);
        } else {
            referenceChild = myFindReferenceChildClosestToEnd(state);

        }

        if (referenceChild == null) {
            if (VLayoutUtils.isDebug) {
                Log.d(TAG,
                        "Cannot find a child with a valid position to be used for focus search.");
            }
            return null;
        }
        ensureLayoutStateExpose();
        final int maxScroll = (int) (MAX_SCROLL_FACTOR * mOrientationHelper.getTotalSpace());
        updateLayoutStateExpose(layoutDir, maxScroll, false, state);
        mLayoutState.mScrollingOffset = LayoutState.SCOLLING_OFFSET_NaN;
        mLayoutState.mRecycle = false;
        mLayoutState.mOnRefresLayout = false;
        fill(recycler, mLayoutState, state, true);
        final View nextFocus;
        if (layoutDir == LayoutState.LAYOUT_START) {
            nextFocus = getChildClosestToStartExpose();
        } else {
            nextFocus = getChildClosestToEndExpose();
        }
        if (nextFocus == referenceChild || !nextFocus.isFocusable()) {
            return null;
        }
        return nextFocus;
    }

    /**
     * 用于调试。将子级的内部表示形式记录到默认记录器。
     */
    private void logChildren() {
        Log.d(TAG, "internal representation of views on the screen");
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Log.d(TAG, "item " + getPosition(child) + ", coord:"
                    + mOrientationHelper.getDecoratedStart(child));
        }
        Log.d(TAG, "==============");
    }

    /**
     * 用于调试。验证子视图的布局顺序是否正确。这很重要，因为算法的其余部分依赖于此约束。
     * <p/>
     * 在默认布局中，子级0应最接近屏幕位置0，最后一个子级应最接近位置WIDTH或HEIGHT。
     * 在反向布局中，最后一个孩子应该靠近屏幕位置0，第一个孩子应该最接近位置WIDTH或HEIGHT
     */
    private void validateChildOrderExpose() {
        Log.d(TAG, "validating child count " + getChildCount());
        if (getChildCount() < 1) {
            return;
        }
        int lastPos = getPosition(getChildAt(0));
        int lastScreenLoc = mOrientationHelper.getDecoratedStart(getChildAt(0));
        if (mShouldReverseLayoutExpose) {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int pos = getPosition(child);
                int screenLoc = mOrientationHelper.getDecoratedStart(child);
                if (pos < lastPos) {
                    logChildren();
                    throw new RuntimeException("detected invalid position. loc invalid? " +
                            (screenLoc < lastScreenLoc));
                }
                if (screenLoc > lastScreenLoc) {
                    logChildren();
                    throw new RuntimeException("detected invalid location");
                }
            }
        } else {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int pos = getPosition(child);
                int screenLoc = mOrientationHelper.getDecoratedStart(child);
                if (pos < lastPos) {
                    logChildren();
                    throw new RuntimeException("detected invalid position. loc invalid? " +
                            (screenLoc < lastScreenLoc));
                }
                if (screenLoc < lastScreenLoc) {
                    logChildren();
                    throw new RuntimeException("detected invalid location");
                }
            }
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return mCurrentPendingSavedState == null && mLastStackFromEnd == getStackFromEnd();
    }

    //==================================
    // 扩展方法
    //==================================
    protected void addHiddenView(View view, boolean head) {
        int index = head ? 0 : -1;
        addView(view, index);
        mChildHelperWrapper.hide(view);
    }

    protected void hideView(View view) {
        mChildHelperWrapper.hide(view);
    }

    protected void showView(View view) {
        mChildHelperWrapper.show(view);
    }

    protected View findHiddenView(int position) {
        return mChildHelperWrapper.findHiddenNonRemovedView(position, RecyclerView.INVALID_TYPE);
    }

    protected boolean isHidden(View view) {
        return mChildHelperWrapper.isHidden(view);
    }

    static final int FLAG_INVALID = 1 << 2;

    static final int FLAG_UPDATED = 1 << 1;

    private static Field vhField = null;
    private static Method vhSetFlags = null;

    protected static boolean isViewHolderUpdated(RecyclerView.ViewHolder holder) {
        return new ViewHolderWrapper(holder).requireUpdated();
    }

    protected static void attachViewHolder(RecyclerView.LayoutParams params, RecyclerView.ViewHolder holder) {
        try {

            if (vhField == null) {
                vhField = RecyclerView.LayoutParams.class.getDeclaredField("mViewHolder");
            }

            vhField.setAccessible(true);
            vhField.set(params, holder);

            if (vhSetFlags == null) {
                vhSetFlags = RecyclerView.ViewHolder.class.getDeclaredMethod("setFlags", int.class, int.class);
                vhSetFlags.setAccessible(true);
            }

            vhSetFlags.invoke(holder, FLAG_INVALID, FLAG_INVALID);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * 在{LayoutManager}填充空白空间时保持临时状态的帮助程序类。
     */
    public static class LayoutState {

        private Method vhIsRemoved = null;

        final static String TAG = "_ExposeLLayoutManager#LayoutState";

        public final static int LAYOUT_START = -1;

        public final static int LAYOUT_END = 1;

        final static int INVALID_LAYOUT = Integer.MIN_VALUE;

        public final static int ITEM_DIRECTION_HEAD = -1;

        public final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        public boolean mOnRefresLayout = false;

        /**
         * 在某些情况下，我们可能不想回收儿童（例如布局）
         */
        public boolean mRecycle = true;

        /**
         * 布局应开始的像素偏移
         */
        public int mOffset;

        /**
         * 我们应该在布局方向上填充的像素数。
         */
        public int mAvailable;

        /**
         * 获取下一项的适配器上的当前位置。
         */
        public int mCurrentPosition;

        /**
         * 定义数据适配器的遍历方向。应该是 {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        public int mItemDirection;

        /**
         * 定义填充布局的方向。应该是 {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        public int mLayoutDirection;

        /**
         * 在滚动状态下构造LayoutState时使用。它应该设置为无需创建新视图即可进行的滚动量。这是高效视图回收所需的设置。
         */
        public int mScrollingOffset;

        /**
         * 如果要预先布局尚不可见的项目，请使用。与 {@link #mAvailable} 回收时 {@link #mExtra} 不考虑避免可见的回收 children.
         */
        public int mExtra = 0;


        /**
         * 当布局为固定滚动时使用
         */
        public int mFixOffset = 0;

        /**
         * 等于 {@link RecyclerView.State#isPreLayout()}. 在使用废料时，如果该值设置为true，我们将跳过删除的视图，因为它们不应在布局后步骤中进行布局。
         */
        public boolean mIsPreLayout = false;

        /**
         * 当LLM需要布局特定视图时，它会设置此列表，在这种情况下，LayoutState将仅返回此列表中的视图，如果找不到项，则返回null。
         */
        public List<RecyclerView.ViewHolder> mScrapList = null;

        public LayoutState() {
            try {
                vhIsRemoved = RecyclerView.ViewHolder.class.getDeclaredMethod("isRemoved");
                vhIsRemoved.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }


        /**
         * @return 如果数据适配器中有更多项，则为true
         */
        public boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }

        /**
         * 获取我们应该布局的下一个元素的视图。还将当前项目索引更新为下一个项目，基于 {@link #mItemDirection}
         *
         * @return 我们应该布局的下一个元素。
         */
        public View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextFromLimitedList();
            }
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        /**
         * 返回有限列表中的下一项。
         * <p/>
         * 找到有效VH后，将当前项目位置设置为VH.itemPosition+mItemDirection
         *
         * @return 查看当前位置或方向中的项目是否存在（如果不为空）。
         */
        private View nextFromLimitedList() {
            int size = mScrapList.size();
            RecyclerView.ViewHolder closest = null;
            int closestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                RecyclerView.ViewHolder viewHolder = mScrapList.get(i);
                if (!mIsPreLayout) {
                    boolean isRemoved = false;
                    try {
                        isRemoved = (boolean) vhIsRemoved.invoke(viewHolder);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (!mIsPreLayout && isRemoved) {
                        continue;
                    }
                }
                final int distance = (viewHolder.getLayoutPosition() - mCurrentPosition) * mItemDirection;
                if (distance < 0) {
                    continue; // 项目不在当前方向
                }
                if (distance < closestDistance) {
                    closest = viewHolder;
                    closestDistance = distance;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            if (closest != null) {
                mCurrentPosition = closest.getLayoutPosition() + mItemDirection;
                return closest.itemView;
            }
            return null;
        }

        @SuppressLint("LongLogTag")
        void log() {
            Log.d(TAG, "avail:" + mAvailable + ", ind:" + mCurrentPosition + ", dir:" +
                    mItemDirection + ", offset:" + mOffset + ", layoutDir:" + mLayoutDirection);
        }
    }

    /**
     * 保存锚点信息的简单数据类
     */
    protected class AnchorInfo {
        public int mPosition;
        public int mCoordinate;
        public boolean mLayoutFromEnd;

        void reset() {
            mPosition = RecyclerView.NO_POSITION;
            mCoordinate = INVALID_OFFSET;
            mLayoutFromEnd = false;
        }

        /**
         * 根据当前layoutFromEnd值从RecyclerView的填充中指定锚点坐标
         */
        void assignCoordinateFromPadding() {
            mCoordinate = mLayoutFromEnd
                    ? mOrientationHelper.getEndAfterPadding()
                    : mOrientationHelper.getStartAfterPadding();
        }

        @Override
        public String toString() {
            return "AnchorInfo{" +
                    "mPosition=" + mPosition +
                    ", mCoordinate=" + mCoordinate +
                    ", mLayoutFromEnd=" + mLayoutFromEnd +
                    '}';
        }

        /**
         * 如果所提供的视图作为引用子对象有效，则从该视图中分配锚点位置信息。
         */
        public boolean assignFromViewIfValid(View child, RecyclerView.State state) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (!lp.isItemRemoved() && lp.getViewLayoutPosition() >= 0
                    && lp.getViewLayoutPosition() < state.getItemCount()) {
                assignFromView(child);
                return true;
            }
            return false;
        }

        public void assignFromView(View child) {
            if (mLayoutFromEnd) {
                mCoordinate = mOrientationHelper.getDecoratedEnd(child) + computeAlignOffset(child, mLayoutFromEnd, true) +
                        mOrientationHelper.getTotalSpaceChange();
                if (VLayoutUtils.isDebug) {
                    Log.d(TAG, "1 mLayoutFromEnd " + mLayoutFromEnd + " mOrientationHelper.getDecoratedEnd(child) "
                            + mOrientationHelper.getDecoratedEnd(child) + " computeAlignOffset(child, mLayoutFromEnd, true) " + computeAlignOffset(child, mLayoutFromEnd, true));
                }
            } else {
                mCoordinate = mOrientationHelper.getDecoratedStart(child) + computeAlignOffset(child, mLayoutFromEnd, true);
                if (VLayoutUtils.isDebug) {
                    Log.d(TAG, "2 mLayoutFromEnd " + mLayoutFromEnd + " mOrientationHelper.getDecoratedStart(child) "
                            + mOrientationHelper.getDecoratedStart(child) + " computeAlignOffset(child, mLayoutFromEnd, true) " + computeAlignOffset(child, mLayoutFromEnd, true));
                }
            }

            mPosition = getPosition(child);
            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "position " + mPosition + " mCoordinate " + mCoordinate);
            }
        }
    }


    static class ViewHolderWrapper {
        private RecyclerView.ViewHolder mHolder;

        private static Method mShouldIgnore;
        private static Method mIsInvalid;
        private static Method mIsRemoved;
        private static Method mIsChanged;
        private static Method mSetFlags;


        static {
            try {
                mShouldIgnore = RecyclerView.ViewHolder.class.getDeclaredMethod("shouldIgnore");
                mShouldIgnore.setAccessible(true);
                mIsInvalid = RecyclerView.ViewHolder.class.getDeclaredMethod("isInvalid");
                mIsInvalid.setAccessible(true);
                mIsRemoved = RecyclerView.ViewHolder.class.getDeclaredMethod("isRemoved");
                mIsRemoved.setAccessible(true);

                mSetFlags = RecyclerView.ViewHolder.class.getDeclaredMethod("setFlags", int.class, int.class);
                mSetFlags.setAccessible(true);

                try {
                    mIsChanged = RecyclerView.ViewHolder.class.getDeclaredMethod("isChanged");
                } catch (NoSuchMethodException e) {
                    mIsChanged = RecyclerView.ViewHolder.class.getDeclaredMethod("isUpdated");
                }

                mIsChanged.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }


        public static void setFlags(RecyclerView.ViewHolder viewHolder, int flags, int mask) {
            try {
                mSetFlags.invoke(viewHolder, flags, mask);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        public ViewHolderWrapper(RecyclerView.ViewHolder holder) {
            this.mHolder = holder;

        }

        boolean isInvalid() {
            if (mIsInvalid == null) {
                return true;
            }
            try {
                return (boolean) mIsInvalid.invoke(mHolder);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        boolean isRemoved() {
            if (mIsRemoved == null) {
                return true;
            }
            try {
                return (boolean) mIsRemoved.invoke(mHolder);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        boolean isChanged() {
            if (mIsChanged == null) {
                return true;
            }
            try {
                return (boolean) mIsChanged.invoke(mHolder);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        void setFlags(int flags, int mask) {
            try {
                mSetFlags.invoke(mHolder, flags, mask);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }


        public boolean requireUpdated() {
            return isInvalid() || isRemoved() || isChanged();
        }


    }

    class ChildHelperWrapper {
        private Object mInnerChildHelper;

        private Method mHideMethod;

        private Method mFindHiddenNonRemovedViewMethod;

        /**
         * 从25.2.0开始，也许更早，该方法将参数从两个减少到一个
         */
        private Method mFindHiddenNonRemovedViewMethod25;

        private Method mIsHideMethod;

        private Field mHiddenViewField;

        private Object mInnerBucket;

        private Method mClearMethod;

        private Field mChildHelperField;

        private List mInnerHiddenView;

        private RecyclerView.LayoutManager mLayoutManager;

        private Object[] args = new Object[1];

        void ensureChildHelper() {
            try {
                if (mInnerChildHelper == null) {
                    mInnerChildHelper = mChildHelperField.get(mLayoutManager);
                    if (mInnerChildHelper == null) {
                        return;
                    }

                    Class<?> helperClz = mInnerChildHelper.getClass();
                    mHideMethod = helperClz.getDeclaredMethod("hide", View.class);
                    mHideMethod.setAccessible(true);
                    try {
                        mFindHiddenNonRemovedViewMethod = helperClz.getDeclaredMethod("findHiddenNonRemovedView", int.class, int.class);
                        mFindHiddenNonRemovedViewMethod.setAccessible(true);
                    } catch (NoSuchMethodException nsme) {
                        mFindHiddenNonRemovedViewMethod25 = helperClz.getDeclaredMethod("findHiddenNonRemovedView", int.class);
                        mFindHiddenNonRemovedViewMethod25.setAccessible(true);
                    }
                    mIsHideMethod = helperClz.getDeclaredMethod("isHidden", View.class);
                    mIsHideMethod.setAccessible(true);

                    Field bucketField = helperClz.getDeclaredField("mBucket");
                    bucketField.setAccessible(true);

                    mInnerBucket = bucketField.get(mInnerChildHelper);
                    mClearMethod = mInnerBucket.getClass().getDeclaredMethod("clear", int.class);
                    mClearMethod.setAccessible(true);

                    mHiddenViewField = helperClz.getDeclaredField("mHiddenViews");
                    mHiddenViewField.setAccessible(true);
                    mInnerHiddenView = (List) mHiddenViewField.get(mInnerChildHelper);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        ChildHelperWrapper(RecyclerView.LayoutManager layoutManager) {
            this.mLayoutManager = layoutManager;
            try {
                mChildHelperField = RecyclerView.LayoutManager.class.getDeclaredField("mChildHelper");
                mChildHelperField.setAccessible(true);
                ensureChildHelper();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void hide(View view) {
            try {
                ensureChildHelper();
                if (mInnerHiddenView.indexOf(view) < 0) {
                    args[0] = view;
                    mHideMethod.invoke(mInnerChildHelper, args);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void show(View view) {
            try {
                ensureChildHelper();
                int index = mRecyclerView.indexOfChild(view);
                args[0] = Integer.valueOf(index);
                mClearMethod.invoke(mInnerBucket, args);
                if (mInnerHiddenView != null) {
                    mInnerHiddenView.remove(view);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        View findHiddenNonRemovedView(int position, int type) {
            try {
                ensureChildHelper();
                if (mFindHiddenNonRemovedViewMethod != null) {
                    return (View) mFindHiddenNonRemovedViewMethod.invoke(mInnerChildHelper, position, RecyclerView.INVALID_TYPE);
                } else if (mFindHiddenNonRemovedViewMethod25 != null) {
                    return (View) mFindHiddenNonRemovedViewMethod25.invoke(mInnerChildHelper, position);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        boolean isHidden(View view) {
            try {
                ensureChildHelper();
                args[0] = view;
                return (boolean) mIsHideMethod.invoke(mInnerChildHelper, args);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

    }

}

