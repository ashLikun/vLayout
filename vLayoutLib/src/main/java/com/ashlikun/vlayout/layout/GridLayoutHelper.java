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

package com.ashlikun.vlayout.layout;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.LayoutManagerHelper;
import com.ashlikun.vlayout.OrientationHelperEx;
import com.ashlikun.vlayout.VLayoutUtils;
import com.ashlikun.vlayout.VirtualLayoutManager;
import com.ashlikun.vlayout.VirtualLayoutManager.LayoutParams;
import com.ashlikun.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import java.util.Arrays;


/**
 * LayoutHelper提供GridLayout。与{@link ColumnLayoutHelper}的区别在于，此layoutHelper可以一行一行地布局和回收子视图。
 *
 * @author villadora
 * @since 1.0.0
 */
public class GridLayoutHelper extends BaseLayoutHelper {
    private static final String TAG = "GridLayoutHelper";


    private int mSpanCount = 4;

    private int mSizePerSpan = 0;


    private int mTotalSize = 0;

    private boolean mIsAutoExpand = true;

    private boolean mIgnoreExtra = false;

    @NonNull
    private SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();

    private int mVGap = 0;
    private int mHGap = 0;


    private float[] mWeights = new float[0];


    private View[] mSet;

    /**
     * 存储每个跨度的索引
     */
    private int[] mSpanIndices;

    /**
     * {@link mWeights}不为空时存储每个跨度的大小
     */
    private int[] mSpanCols;

    /**
     * @param spanCount 网格中的列行数，必须大于0
     */
    public GridLayoutHelper(int spanCount) {
        this(spanCount, -1, -1);
    }

    /**
     * @param spanCount 网格中的列行数，必须大于0
     * @param itemCount 此layoutHelper中的项目数
     */
    public GridLayoutHelper(int spanCount, int itemCount) {
        this(spanCount, itemCount, 0);
    }

    public GridLayoutHelper(int spanCount, int itemCount, int gap) {
        this(spanCount, itemCount, gap, gap);
    }

    /**
     * @param spanCount 网格中的列行数，必须大于0
     * @param itemCount 此layoutHelper中的项目数
     * @param vGap      垂直间隙
     * @param hGap      水平间距
     */
    public GridLayoutHelper(int spanCount, int itemCount, int vGap, int hGap) {
        setSpanCount(spanCount);
        mSpanSizeLookup.setSpanIndexCacheEnabled(true);

        setItemCount(itemCount);
        setVGap(vGap);
        setHGap(hGap);
    }


    public void setWeights(float[] weights) {
        if (weights != null) {
            this.mWeights = Arrays.copyOf(weights, weights.length);
        } else {
            this.mWeights = new float[0];
        }
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        if (spanSizeLookup != null) {
            // TODO: 处理反向布局？
            spanSizeLookup.setStartPosition(mSpanSizeLookup.getStartPosition());

            this.mSpanSizeLookup = spanSizeLookup;
        }
    }

    public void setAutoExpand(boolean isAutoExpand) {
        this.mIsAutoExpand = isAutoExpand;
    }

    public void setIgnoreExtra(boolean ignoreExtra) {
        this.mIgnoreExtra = ignoreExtra;
    }


    /**
     * 设置网格的SpanCount
     *
     * @param spanCount 网格列编号，必须大于0。否则将引发{@link IllegalArgumentException}
     */
    public void setSpanCount(int spanCount) {
        if (spanCount == mSpanCount) {
            return;
        }
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided "
                    + spanCount);
        }
        mSpanCount = spanCount;
        mSpanSizeLookup.invalidateSpanIndexCache();

        ensureSpanCount();
    }

    public int getVGap() {
        return mVGap;
    }

    public int getHGap() {
        return mHGap;
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    /**
     * @param start 此layoutHelper处理的项目的开始位置
     * @param end   由这个layoutHelper处理的项目的结束位置，如果 end<start ，它将抛出 {@link IllegalArgumentException}
     */
    @Override
    public void onRangeChange(int start, int end) {
        mSpanSizeLookup.setStartPosition(start);
        mSpanSizeLookup.invalidateSpanIndexCache();
    }


    public void setGap(int gap) {
        setVGap(gap);
        setHGap(gap);
    }

    public void setVGap(int vGap) {
        if (vGap < 0) {
            vGap = 0;
        }
        this.mVGap = vGap;
    }

    public void setHGap(int hGap) {
        if (hGap < 0) {
            hGap = 0;
        }
        this.mHGap = hGap;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        boolean isStartLine = false, isEndLine = false;
        final int currentPosition = layoutState.getCurrentPosition();
        final boolean isOverLapMargin = helper.isEnableMarginOverLap();

        final int itemDirection = layoutState.getItemDirection();
        final boolean layingOutInPrimaryDirection =
                itemDirection == LayoutStateWrapper.ITEM_DIRECTION_TAIL;

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (layoutInVertical) {
            mTotalSize = helper.getContentWidth() - helper.getPaddingRight() - helper.getPaddingLeft() - getHorizontalMargin() - getHorizontalPadding();
            mSizePerSpan = (int) ((mTotalSize - (mSpanCount - 1) * mHGap) * 1.0f / mSpanCount + 0.5f);
        } else {
            mTotalSize = helper.getContentHeight() - helper.getPaddingBottom() - helper.getPaddingTop() - getVerticalMargin() - getVerticalPadding();
            mSizePerSpan = (int) ((mTotalSize - (mSpanCount - 1) * mVGap) * 1.0f / mSpanCount + 0.5f);
        }


        int count = 0;
        int consumedSpanCount = 0;
        int remainingSpan = mSpanCount;


        ensureSpanCount();


        if (!layingOutInPrimaryDirection) {
            // fill the remaining spacing this row
            int itemSpanIndex = getSpanIndex(recycler, state, layoutState.getCurrentPosition());
            int itemSpanSize = getSpanSize(recycler, state, layoutState.getCurrentPosition());


            remainingSpan = itemSpanIndex + itemSpanSize;

            // should find the last element of this row
            if (itemSpanIndex != mSpanCount - 1) {
                int index = layoutState.getCurrentPosition();
                int revRemainingSpan = mSpanCount - remainingSpan;
                while (count < mSpanCount && revRemainingSpan > 0) {
                    // go reverse direction to find views fill current row
                    index -= itemDirection;
                    if (isOutOfRange(index)) {
                        break;
                    }
                    final int spanSize = getSpanSize(recycler, state, index);
                    if (spanSize > mSpanCount) {
                        throw new IllegalArgumentException("Item at position " + index + " requires " +
                                spanSize + " spans but GridLayoutManager has only " + mSpanCount
                                + " spans.");
                    }

                    View view = layoutState.retrieve(recycler, index);
                    if (view == null) {
                        break;
                    }

                    if (!isStartLine) {
                        isStartLine = helper.getReverseLayout() ? index == getRange().getUpper() : index == getRange().getLower();
                    }

                    if (!isEndLine) {
                        isEndLine = helper.getReverseLayout() ? index == getRange().getLower() : index == getRange().getUpper();
                    }

                    revRemainingSpan -= spanSize;
                    if (revRemainingSpan < 0) {
                        break;
                    }


                    consumedSpanCount += spanSize;
                    mSet[count] = view;
                    count++;
                }

                if (count > 0) {
                    // reverse array
                    int s = 0, e = count - 1;
                    while (s < e) {
                        View temp = mSet[s];
                        mSet[s] = mSet[e];
                        mSet[e] = temp;
                        s++;
                        e--;
                    }
                }
            }
        }

        while (count < mSpanCount && layoutState.hasMore(state) && remainingSpan > 0) {
            int pos = layoutState.getCurrentPosition();
            if (isOutOfRange(pos)) {
                if (VLayoutUtils.isDebug) {
                    Log.d(TAG, "pos [" + pos + "] is out of range");
                }
                break;
            }

            final int spanSize = getSpanSize(recycler, state, pos);
            if (spanSize > mSpanCount) {
                throw new IllegalArgumentException("Item at position " + pos + " requires " +
                        spanSize + " spans but GridLayoutManager has only " + mSpanCount
                        + " spans.");
            }
            remainingSpan -= spanSize;
            if (remainingSpan < 0) {
                break; // 项不适合此行或列
            }

            View view = layoutState.next(recycler);
            if (view == null) {
                break;
            }

            if (!isStartLine) {
                isStartLine = helper.getReverseLayout() ? pos == getRange().getUpper().intValue() : pos == getRange().getLower().intValue();
            }

            if (!isEndLine) {
                isEndLine = helper.getReverseLayout() ? pos == getRange().getLower().intValue() : pos == getRange().getUpper().intValue();
            }

            consumedSpanCount += spanSize;
            mSet[count] = view;
            count++;
        }


        if (count == 0) {
            return;
        }

        int maxSize = 0;


        // 我们应该在计算项目装饰偏移量之前分配跨度
        assignSpans(recycler, state, count, consumedSpanCount, layingOutInPrimaryDirection, helper);

        if (remainingSpan > 0 && (count == consumedSpanCount) && mIsAutoExpand) {
            //autoExpand仅在每个单元格占用一个跨度时支持。
            if (layoutInVertical) {
                mSizePerSpan = (mTotalSize - (count - 1) * mHGap) / count;
            } else {
                mSizePerSpan = (mTotalSize - (count - 1) * mVGap) / count;
            }
        } else if (!layingOutInPrimaryDirection && remainingSpan == 0 && (count == consumedSpanCount) && mIsAutoExpand) {
            //autoExpand仅在每个单元格占用一个跨度时支持。
            if (layoutInVertical) {
                mSizePerSpan = (mTotalSize - (count - 1) * mHGap) / count;
            } else {
                mSizePerSpan = (mTotalSize - (count - 1) * mVGap) / count;
            }
        }


        boolean weighted = false;
        if (mWeights != null && mWeights.length > 0) {
            weighted = true;
            int totalSpace;
            if (layoutInVertical) {
                totalSpace = mTotalSize - (count - 1) * mHGap;
            } else {
                totalSpace = mTotalSize - (count - 1) * mVGap;
            }

            // 用重量百分比计算宽度

            int eqCnt = 0, remainingSpace = totalSpace;
            int colCnt = (remainingSpan > 0 && mIsAutoExpand) ? count : mSpanCount;
            for (int i = 0; i < colCnt; i++) {
                if (i < mWeights.length && !Float.isNaN(mWeights[i]) && mWeights[i] >= 0) {
                    float weight = mWeights[i];
                    mSpanCols[i] = (int) (weight * 1.0f / 100 * totalSpace + 0.5f);
                    remainingSpace -= mSpanCols[i];
                } else {
                    eqCnt++;
                    mSpanCols[i] = -1;
                }
            }

            if (eqCnt > 0) {
                int eqLength = remainingSpace / eqCnt;
                for (int i = 0; i < colCnt; i++) {
                    if (mSpanCols[i] < 0) {
                        mSpanCols[i] = eqLength;
                    }
                }
            }
        }


        for (int i = 0; i < count; i++) {
            View view = mSet[i];
            helper.addChildView(layoutState, view, layingOutInPrimaryDirection ? -1 : 0);

            int spanSize = getSpanSize(recycler, state, helper.getPosition(view)), spec;
            if (weighted) {
                final int index = mSpanIndices[i];
                int spanLength = 0;
                for (int j = 0; j < spanSize; j++) {
                    spanLength += mSpanCols[j + index];
                }

                spec = View.MeasureSpec.makeMeasureSpec(Math.max(0, spanLength), View.MeasureSpec.EXACTLY);
            } else {
                spec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan * spanSize +
                                Math.max(0, spanSize - 1) * (layoutInVertical ? mHGap : mVGap),
                        View.MeasureSpec.EXACTLY);
            }
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();

            if (helper.getOrientation() == VERTICAL) {
                helper.measureChildWithMargins(view, spec, getMainDirSpec(lp.height, mTotalSize,
                        View.MeasureSpec.getSize(spec), lp.mAspectRatio));
            } else {
                helper.measureChildWithMargins(view,
                        getMainDirSpec(lp.width, mTotalSize, View.MeasureSpec.getSize(spec),
                                lp.mAspectRatio), View.MeasureSpec.getSize(spec));
            }
            final int size = orientationHelper.getDecoratedMeasurement(view);
            if (size > maxSize) {
                maxSize = size;
            }
        }

        // 必须重新测量未测量maxSize的视图
        final int maxMeasureSpec = getMainDirSpec(maxSize, mTotalSize, 0, Float.NaN);
        for (int i = 0; i < count; i++) {
            final View view = mSet[i];
            if (orientationHelper.getDecoratedMeasurement(view) != maxSize) {
                int spanSize = getSpanSize(recycler, state, helper.getPosition(view)), spec;
                if (weighted) {
                    final int index = mSpanIndices[i];
                    int spanLength = 0;
                    for (int j = 0; j < spanSize; j++) {
                        spanLength += mSpanCols[j + index];
                    }

                    spec = View.MeasureSpec.makeMeasureSpec(Math.max(0, spanLength), View.MeasureSpec.EXACTLY);
                } else {
                    spec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan * spanSize +
                                    Math.max(0, spanSize - 1) * (layoutInVertical ? mHGap : mVGap),
                            View.MeasureSpec.EXACTLY);
                }

                if (helper.getOrientation() == VERTICAL) {
                    helper.measureChildWithMargins(view, spec, maxMeasureSpec);
                } else {
                    helper.measureChildWithMargins(view, maxMeasureSpec, spec);
                }
            }
        }

        int startSpace = 0, endSpace = 0;

        if (isStartLine) {
            startSpace = computeStartSpace(helper, layoutInVertical, !helper.getReverseLayout(), isOverLapMargin);
        }

        if (isEndLine) {
            endSpace = computeEndSpace(helper, layoutInVertical, !helper.getReverseLayout(), isOverLapMargin);
        }


        result.mConsumed = maxSize + startSpace + endSpace;

        final boolean layoutStart = layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START;
        if (!mLayoutWithAnchor && (!isEndLine || !layoutStart) && (!isStartLine || layoutStart)) {
            result.mConsumed += (layoutInVertical ? mVGap : mHGap);
        }


        int left = 0, right = 0, top = 0, bottom = 0;
        if (layoutInVertical) {
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                bottom = layoutState.getOffset() - endSpace - ((mLayoutWithAnchor || isEndLine) ? 0 : mVGap);
                top = bottom - maxSize;
            } else {
                top = layoutState.getOffset() + startSpace + ((mLayoutWithAnchor || isStartLine) ? 0 : mVGap);
                bottom = top + maxSize;
            }
        } else {
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - endSpace - (mLayoutWithAnchor || isEndLine ? 0 : mHGap);
                left = right - maxSize;
            } else {
                left = layoutState.getOffset() + startSpace + (mLayoutWithAnchor || isStartLine ? 0 : mHGap);
                right = left + maxSize;
            }
        }

        for (int i = 0; i < count; i++) {
            View view = mSet[i];
            final int index = mSpanIndices[i];

            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (layoutInVertical) {
                if (weighted) {
                    left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft;
                    for (int j = 0; j < index; j++) {
                        left += mSpanCols[j] + mHGap;
                    }
                } else {
                    left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft + mSizePerSpan * index + index * mHGap;
                }

                right = left + orientationHelper.getDecoratedMeasurementInOther(view);
            } else {

                if (weighted) {
                    top = helper.getPaddingTop() + mMarginTop + mPaddingTop;
                    for (int j = 0; j < index; j++) {
                        top += mSpanCols[j] + mVGap;
                    }
                } else {
                    top = helper.getPaddingTop() + mMarginTop + mPaddingTop
                            + mSizePerSpan * index + index * mVGap;
                }

                bottom = top + orientationHelper.getDecoratedMeasurementInOther(view);
            }

            if (VLayoutUtils.isDebug) {
                Log.d(TAG, "layout item in position: " + params.getViewLayoutPosition() + " with text " + ((TextView) view).getText() + " with SpanIndex: " + index + " into (" +
                        left + ", " + top + ", " + right + ", " + bottom + " )");
            }

            // 我们使用View的边界框（包括装饰和边距）计算所有内容
            // 为了计算正确的布局位置，我们减去边距。
            // 由汇丰于20160907修改，已扣除利润
            layoutChildWithMargin(view, left, top, right, bottom, helper);

            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }

            result.mFocusable |= view.isFocusable();
        }


        mLayoutWithAnchor = false;
        Arrays.fill(mSet, null);
        Arrays.fill(mSpanIndices, 0);
        Arrays.fill(mSpanCols, 0);
    }


    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                return layoutInVertical ? mMarginBottom + mPaddingBottom : mMarginRight + mPaddingRight;
            }
        } else {
            if (offset == 0) {
                return layoutInVertical ? -mMarginTop - mPaddingTop : -mMarginLeft - mPaddingLeft;
            }
        }

        return super.computeAlignOffset(offset, isLayoutEnd, useAnchor, helper);
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public void onItemsChanged(LayoutManagerHelper helper) {
        super.onItemsChanged(helper);
        mSpanSizeLookup.invalidateSpanIndexCache();
    }

    private static final int MAIN_DIR_SPEC =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    private int getMainDirSpec(int dim, int otherSize, int viewSize, float viewAspectRatio) {
        if (!Float.isNaN(viewAspectRatio) && viewAspectRatio > 0 && viewSize > 0) {
            return View.MeasureSpec.makeMeasureSpec((int) (viewSize / viewAspectRatio + 0.5f), View.MeasureSpec.EXACTLY);
        } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
            return View.MeasureSpec.makeMeasureSpec((int) (otherSize / mAspectRatio + 0.5f), View.MeasureSpec.EXACTLY);
        } else if (dim < 0) {
            return MAIN_DIR_SPEC;
        } else {
            return View.MeasureSpec.makeMeasureSpec(dim, View.MeasureSpec.EXACTLY);
        }
    }


    private void ensureSpanCount() {

        if (mSet == null || mSet.length != mSpanCount) {
            mSet = new View[mSpanCount];
        }

        if (mSpanIndices == null || mSpanIndices.length != mSpanCount) {
            mSpanIndices = new int[mSpanCount];
        }

        if (mSpanCols == null || mSpanCols.length != mSpanCount) {
            mSpanCols = new int[mSpanCount];
        }
    }


    private boolean mLayoutWithAnchor = false;

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            int span = mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, mSpanCount);
            if (anchorInfo.layoutFromEnd) {
                while (span < mSpanCount - 1 && anchorInfo.position < getRange().getUpper()) {
                    anchorInfo.position++;
                    span = mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, mSpanCount);
                }
            } else {
                while (span > 0 && anchorInfo.position > 0) {
                    anchorInfo.position--;
                    span = mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, mSpanCount);
                }
            }

            mLayoutWithAnchor = true;

/*
            if (anchorInfo.position == getRange().getLower() || anchorInfo.position == getRange().getUpper()) {
                return;
            }

            boolean layoutInVertical = helper.getOrientation() == VERTICAL;
            if (anchorInfo.layoutFromEnd) {
                anchorInfo.coordinate += layoutInVertical ? mVGap : mHGap;
            } else {
                anchorInfo.coordinate -= layoutInVertical ? mVGap : mHGap;
            }
 */

        }
    }


    private int getSpanIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return mSpanSizeLookup.getCachedSpanIndex(pos, mSpanCount);
        }

        final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            return 0;
        }
        return mSpanSizeLookup.getCachedSpanIndex(adapterPosition, mSpanCount);
    }


    private int getSpanSize(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return mSpanSizeLookup.getSpanSize(pos);
        }

        final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            return 0;
        }

        return mSpanSizeLookup.getSpanSize(adapterPosition);
    }

    private void assignSpans(RecyclerView.Recycler recycler, RecyclerView.State state, int count,
                             int consumedSpanCount, boolean layingOutInPrimaryDirection, LayoutManagerHelper helper) {
        int span, spanDiff, start, end, diff;
        // make sure we traverse from min position to max position
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }

        if (helper.getOrientation() == VERTICAL && helper.isDoLayoutRTL()) { // start from last span
            span = consumedSpanCount - 1;
            spanDiff = -1;
        } else {
            span = 0;
            spanDiff = 1;
        }

        for (int i = start; i != end; i += diff) {
            View view = mSet[i];
            int spanSize = getSpanSize(recycler, state, helper.getPosition(view));
            if (spanDiff == -1 && spanSize > 1) {
                mSpanIndices[i] = span - (spanSize - 1);
            } else {
                mSpanIndices[i] = span;
            }
            span += spanDiff * spanSize;
        }
    }


    static final class DefaultSpanSizeLookup extends SpanSizeLookup {

        @Override
        public int getSpanSize(int position) {
            return 1;
        }

        @Override
        public int getSpanIndex(int span, int spanCount) {
            return (span - mStartPosition) % spanCount;
        }
    }


    public static abstract class SpanSizeLookup {

        final SparseIntArray mSpanIndexCache = new SparseIntArray();

        private boolean mCacheSpanIndices = false;

        int mStartPosition = 0;

        /**
         * 返回项目在<code>位置<code>处占用的跨度数。
         *
         * @param position 项目的适配器位置
         * @return 项目在提供位置占用的跨距数
         */
        abstract public int getSpanSize(int position);

        /**
         * 设置是否缓存{@link #getSpanIndex(int, int)}方法的结果。默认情况下，不会缓存这些值。如果不覆盖
         * {@link #getSpanIndex(int, int)},为了获得更好的性能，应该将此设置为true。
         *
         * @param cacheSpanIndices 是否应缓存getSpanIndex的结果。
         */
        public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
            mCacheSpanIndices = cacheSpanIndices;
        }

        public void setStartPosition(int startPosition) {
            this.mStartPosition = startPosition;
        }

        public int getStartPosition() {
            return this.mStartPosition;
        }

        /**
         * 清除跨度索引缓存。当适配器发生更改时，GridLayoutManager会自动调用此方法。
         */
        public void invalidateSpanIndexCache() {
            mSpanIndexCache.clear();
        }

        /**
         * 返回的结果 {@link #getSpanIndex(int, int)} 方法是否缓存。
         *
         * @return 如果结果为 true {@link #getSpanIndex(int, int)} 缓存。
         */
        public boolean isSpanIndexCacheEnabled() {
            return mCacheSpanIndices;
        }

        int getCachedSpanIndex(int position, int spanCount) {
            if (!mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            final int existing = mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            final int value = getSpanIndex(position, spanCount);
            mSpanIndexCache.put(position, value);
            return value;
        }

        /**
         * 返回所提供位置的最终跨度索引。
         * <p/>
         * 如果您有一种更快的方法来计算项目的跨度索引，则应重写此方法。否则，应启用span索引缓存
         * ({@link #setSpanIndexCacheEnabled(boolean)}) 以获得更好的性能。禁用缓存时，默认实现将遍历从0到
         * <code>position</code>.启用缓存时，它会根据最近缓存的
         * 之前的值 <code>position</code>.
         * <p/>
         * 如果重写此方法，则需要确保它与
         * {@link #getSpanSize(int)}.GridLayoutManager未为调用此方法
         * 每个项目。仅对参考项和其余项调用
         * 基于参考项指定给跨度。例如，不能指定
         * 当跨度1为空时，将位置设置为跨度2。
         * <p/>
         * 请注意，跨度偏移始终以0开始，不受RTL影响。
         *
         * @param position  项目的位置
         * @param spanCount 网格中的跨度总数
         * @return 项目的最终跨度位置。应介于0（含）和
         * <code>spanCount</code>(exclusive)
         */
        public int getSpanIndex(int position, int spanCount) {
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0; // quick return for full-span items
            }
            int span = 0;
            int startPos = mStartPosition;
            // 如果启用了缓存，请尝试跳转
            if (mCacheSpanIndices && mSpanIndexCache.size() > 0) {
                int prevKey = findReferenceIndexFromCache(position);
                if (prevKey >= 0) {
                    span = mSpanIndexCache.get(prevKey) + getSpanSize(prevKey);
                    startPos = prevKey + 1;
                }
            }
            for (int i = startPos; i < position; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                } else if (span > spanCount) {
                    // 不适合，移动到下一行列
                    span = size;
                }
            }
            if (span + positionSpanSize <= spanCount) {
                return span;
            }
            return 0;
        }

        int findReferenceIndexFromCache(int position) {
            int lo = 0;
            int hi = mSpanIndexCache.size() - 1;

            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;
                final int midVal = mSpanIndexCache.keyAt(mid);
                if (midVal < position) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            int index = lo - 1;
            if (index >= 0 && index < mSpanIndexCache.size()) {
                return mSpanIndexCache.keyAt(index);
            }
            return -1;
        }

        /**
         * 返回此位置所属组的索引。
         * <p/>
         * 例如，如果网格有3列，每个项目占用1个跨度，则项目1的跨度组索引将为0，项目5将为1。
         *
         * @param adapterPosition 适配器中的位置
         * @param spanCount       网格中的跨度总数
         * @return 在给定适配器位置包含项的范围组的索引
         */
        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int span = 0;
            int group = 0;
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = 0; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    // did not fit, moving to next row / column
                    span = size;
                    group++;
                }
            }
            if (span + positionSpanSize > spanCount) {
                group++;
            }
            return group;
        }
    }
}
