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

import static com.ashlikun.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.layout.LayoutChunkResult;

import java.util.LinkedList;
import java.util.List;

/**
 * 用于处理不同布局的帮助程序类 {@link VirtualLayoutManager}
 */

public abstract class LayoutHelper {

    public static final Range<Integer> RANGE_EMPTY = Range.create(-1, -1);

    /**
     * 此layoutHelper的范围，用EMPTY初始化
     */
    @NonNull
    Range<Integer> mRange = RANGE_EMPTY;

    int mZIndex = 0;


    /**
     * 这个位置应该用这个来处理吗 {@link LayoutHelper}
     *
     * @param position 没有偏移的位置，这是 {@link VirtualLayoutManager}
     * @return 如果{@link getRange()}返回的范围内的位置为true
     */
    public boolean isOutOfRange(int position) {
        return !mRange.contains(position);
    }


    /**
     * 设置将由此layoutHelper处理的项目范围。开始位置必须大于结束位置，否则 {@link IllegalArgumentException} 将被抛出
     *
     * @param start 此layoutHelper处理的项目的开始位置
     * @param end   由这个layoutHelper处理的项目的结束位置，如果end＜start，它将抛出 {@link IllegalArgumentException}
     * @throws MismatchChildCountException 当（start-end）不等于itemCount时
     */
    public void setRange(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end should be larger or equeal then start position");
        }

        if (start == -1 && end == -1) {
            this.mRange = RANGE_EMPTY;
            onRangeChange(start, end);
            return;
        }

        if ((end - start + 1) != getItemCount()) {
            throw new MismatchChildCountException("ItemCount mismatch when range: " + mRange.toString() + " childCount: " + getItemCount());
        }

        if (start == mRange.getUpper() && end == mRange.getLower()) {
            // no change
            return;
        }

        this.mRange = Range.create(start, end);
        onRangeChange(start, end);
    }

    /**
     * 范围更改时将调用此方法
     *
     * @param start 此layoutHelper处理的项目的开始位置
     * @param end   由这个layoutHelper处理的项目的结束位置，如果end＜start，它将抛出 {@link IllegalArgumentException}
     */
    public void onRangeChange(final int start, final int end) {

    }

    /**
     * 返回电流范围
     *
     * @return 整数的范围
     */
    @NonNull
    public final Range<Integer> getRange() {
        return mRange;
    }


    /**
     * 有机会检查和更改所选的anchorInfo
     *
     * @param state      当前{@linkRecyclerView} 的状态
     * @param anchorInfo 选定的主持人信息
     * @param helper
     */
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {

    }

    /**
     * 当滚动状态更改时调用此方法
     *
     * @param state         RecyclerView的新滚动状态
     * @param startPosition
     * @param endPosition
     */
    public void onScrollStateChanged(int state, int startPosition, int endPosition, LayoutManagerHelper helper) {

    }


    /**
     * 将附加到父RecyclerView的所有子视图沿水平轴偏移dx像素。
     *
     * @param dx 要偏移的像素
     */
    public void onOffsetChildrenHorizontal(int dx, LayoutManagerHelper helper) {

    }

    /**
     * 将附加到父RecyclerView的所有子视图沿垂直轴偏移dy像素。
     *
     * @param dy 要偏移的像素
     */
    public void onOffsetChildrenVertical(int dy, LayoutManagerHelper helper) {

    }

    /**
     * 获取此的zIndex {@link LayoutHelper}
     *
     * @return z当前layoutHelper的索引
     */
    public int getZIndex() {
        return mZIndex;
    }

    /**
     * 实验属性，设置此{@link LayoutHelper}的zIndex，它并不意味着视图的z索引。它只是在线性流中重新排序layoutHelpers。当前不要使用它。
     *
     * @param zIndex
     */
    public void setZIndex(int zIndex) {
        this.mZIndex = zIndex;
    }

    /**
     * 获取固定在某个位置的视图
     *
     * @return
     */
    @Nullable
    public View getFixedView() {
        return null;
    }


    @NonNull
    protected final List<View> mOffFlowViews = new LinkedList<>();

    /**
     * 获取超出正常流布局的视图
     *
     * @return 视图列表
     */
    @NonNull
    public List<View> getOffFlowViews() {
        return mOffFlowViews;
    }

    /**
     * 告诉LayoutManager是否可以回收子项，recycleChild范围为（startIndex，endIndex）
     *
     * @param childPos   循环子索引
     * @param startIndex 将回收子项的开始索引
     * @param endIndex   子级的结束索引将被回收
     * @param helper     类型的助手 {@link LayoutManagerHelper}
     * @param fromStart  是否从一开始就回收儿童
     * @return 孩子是否在 <code>childPos</code> 可回收利用
     */
    public boolean isRecyclable(int childPos, int startIndex, int endIndex, LayoutManagerHelper helper, boolean fromStart) {
        return true;
    }

    /**
     * 返回子项计数
     *
     * @return 孩子的数量
     */
    public abstract int getItemCount();

    /**
     * 设置项目计数
     *
     * @param itemCount 这个layoutHelper中有多少个孩子
     */
    public abstract void setItemCount(int itemCount);

    public abstract void doLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                  LayoutStateWrapper layoutState, LayoutChunkResult result,
                                  LayoutManagerHelper helper);

    public void onRefreshLayout(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {

    }

    /**
     * 在之前调用 <code>doLayout</code>
     *
     * @param recycler recycler
     * @param state    RecyclerView的状态
     * @param helper   LayoutManagerHelper处理视图
     */
    public abstract void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                      LayoutManagerHelper helper);

    /**
     * 在之后调用 <code>doLayout</code>
     *
     * @param recycler      recycler
     * @param state         RecyclerView的状态
     * @param startPosition firstVisiblePosition in {@link RecyclerView}
     * @param endPosition   lastVisiblePosition in {@link RecyclerView}
     * @param scrolled      如果在滚动过程中发生布局，则滚动多少偏移量
     * @param helper        LayoutManagerHelper
     */
    public abstract void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                     int startPosition, int endPosition, int scrolled,
                                     LayoutManagerHelper helper);

    /**
     * 运行以调整layoutHelper的背景区域
     *
     * @param startPosition
     * @param endPosition
     * @param helper
     */
    public abstract void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper);

    public void onItemsChanged(LayoutManagerHelper helper) {

    }

    /**
     * 当此layoutHelper将从LayoutManager中删除时调用，请在此处释放视图和其他资源
     *
     * @param helper LayoutManagerHelper
     */
    public abstract void clear(LayoutManagerHelper helper);

    /**
     * 是否需要后台布局视图
     *
     * @return 如果需要layoutView，则为true
     */
    public abstract boolean requireLayoutView();

    /**
     * 将属性绑定到 <code>layoutView</code>
     *
     * @param layoutView 生成layoutView作为后台视图
     */
    public abstract void bindLayoutView(View layoutView);

    public abstract boolean isFixLayout();

    /**
     * 在布局子项时获取布局之间的边距<code>offset</code>
     * 或在滚动期间计算对齐线的偏移
     *
     * @param offset      当前layoutHelper中锚定子项的偏移量，例如，0表示第一项
     * @param isLayoutEnd 布局过程是结束还是开始，true表示它将从头到尾放置视图
     * @param useAnchor   是为滚动还是为锚点重置计算偏移
     * @param helper      视图布局辅助对象
     * @return 额外的偏移量必须在{@link VirtualLayoutManager}
     */
    public abstract int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor,
                                           LayoutManagerHelper helper);

    public abstract int computeMarginStart(int offset, boolean isLayoutEnd, boolean useAnchor,
                                           LayoutManagerHelper helper);

    public abstract int computeMarginEnd(int offset, boolean isLayoutEnd, boolean useAnchor,
                                         LayoutManagerHelper helper);

    public abstract int computePaddingStart(int offset, boolean isLayoutEnd, boolean useAnchor,
                                            LayoutManagerHelper helper);

    public abstract int computePaddingEnd(int offset, boolean isLayoutEnd, boolean useAnchor,
                                          LayoutManagerHelper helper);

    public void onSaveState(final Bundle bundle) {

    }

    public void onRestoreInstanceState(final Bundle bundle) {

    }

}
