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

import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * LayoutManagerHelper，为 {@link com.ashlikun.vlayout.LayoutHelper}
 */
public interface LayoutManagerHelper {

    /**
     * 它可以被缓存和重用，layoutHelper负责确保属性正确
     *
     * @return 默认布局视图
     */
    View generateLayoutView();

    /**
     * 获取当前子项计数，无隐藏和固定子项
     *
     * @return 正常流中的子项总数，不包括固定子项和隐藏子项
     */
    int getChildCount();

    /**
     * 获取可见子项
     *
     * @param index 获取子项的索引
     * @return 子视图，如果没有则为空。
     */
    @Nullable
    View getChildAt(int index);

    /**
     * 将视图添加到定义的索引
     *
     * @param view  添加的视图
     * @param index 要添加子项的索引
     */
    void addChildView(View view, int index);

    /**
     * 将视图添加到指定的索引，当需要动画时
     *
     * @param layoutState 要执行动画检查的当前布局状态
     * @param view        将添加视图
     * @param index       要添加子项的索引
     */
    void addChildView(VirtualLayoutManager.LayoutStateWrapper layoutState, View view, int index);

    /**
     * 根据layoutState状态将视图添加到headtail
     *
     * @param layoutState 要执行动画检查的当前布局状态
     * @param view        将添加视图
     */
    void addChildView(VirtualLayoutManager.LayoutStateWrapper layoutState, View view);

    /**
     * 从容器中删除视图
     *
     * @param view 视图将被删除
     */
    void removeChildView(View view);

    /**
     * 告诉视图的数据绑定是否已更新，如果为true，则表示需要重新绑定
     */
    boolean isViewHolderUpdated(View view);

    /**
     * 将视图添加到正常流之外，这意味着它不会在getChildAt中被忽略，但仍然可以滚动内容，但可以通过{@link #findViewByPosition(int)} 按位置查找
     *
     * @param view 将添加视图
     * @param head 无论是添加到头部还是尾部
     */
    void addOffFlowView(View view, boolean head);

    /**
     * 在正常流之外添加视图，这意味着它不会在getChildAt中被忽略，但仍然可以滚动内容，但可以通过 {@link #findViewByPosition(int)}.
     * 与{@link #addOffFlowView(View, boolean)}的区别在于，此方法不隐藏视图，它用于添加重叠的背景视图。
     *
     * @param view 将添加视图
     * @param head 无论是添加到头部还是尾部
     */
    void addBackgroundView(View view, boolean head);

    /**
     * 将视图添加到覆盖在正常图层上的固定图层。getChildAt找不到它，也会滚动内容。只能通过{@link #findViewByPosition(int)}按位置获取
     *
     * @param view 固定视图
     */
    void addFixedView(View view);

    /**
     * 将视图标记为隐藏，它将显示在屏幕上，但无法通过{@link RecyclerView.LayoutManager#getChildCount()} and {@link androidx.recyclerview.widget.RecyclerView.LayoutManager#getChildAt(int)}访问
     *
     * @param view
     */
    void hideView(View view);

    /**
     * 将隐藏视图标记为重新显示，以便您可以从 {@link androidx.recyclerview.widget.RecyclerView.LayoutManager#getChildAt(int)}
     *
     * @param view
     */
    void showView(View view);

    /**
     * Get {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} 用于RecyclerView中的视图
     *
     * @param child
     * @return
     */
    RecyclerView.ViewHolder getChildViewHolder(View child);

    /**
     * 获取当前容器回收器视图
     *
     * @return
     */
    RecyclerView getRecyclerView();

    /**
     * 通过项目位置查找视图 {@param position}
     *
     * @param position 视图关联的项目的位置
     * @return 找到的视图，否则为空。
     */
    @Nullable
    View findViewByPosition(int position);


    /**
     * MainOrientationHelper
     *
     * @return
     */
    OrientationHelperEx getMainOrientationHelper();

    /**
     * 辅助方向上的OrientationHelper
     *
     * @return
     */
    OrientationHelperEx getSecondaryOrientationHelper();

    /**
     * 用装饰品测量儿童视图，使用此项测量儿童
     */
    void measureChild(View view, int widthSpec, int heightSpec);

    /**
     * 测量带有边距和装饰的子视图，使用此项测量子视图
     */
    void measureChildWithMargins(View child, int widthUsed, int heightUsed);


    /**
     * 使用边距和装饰布置子视图。
     */
    void layoutChildWithMargins(View view, int left, int top, int right, int bottom);

    /**
     * 布置带有装饰但没有边距的子视图。
     */
    void layoutChild(View view, int left, int top, int right, int bottom);

    /**
     * 获取measureSize的快速助手
     *
     * @param canScroll 可以向这个方向滚动的位置
     */
    int getChildMeasureSpec(int parentSize, int size, boolean canScroll);

    /*
     * 属性助手
     */

    /**
     * 查找视图的项目位置，而不是RecyclerView中的视图索引
     */
    int getPosition(View view);

    int getOrientation();

    int getPaddingTop();

    int getPaddingBottom();

    int getPaddingRight();

    int getPaddingLeft();

    int getContentWidth();

    int getContentHeight();

    boolean isDoLayoutRTL();

    boolean getReverseLayout();

    /**
     * 将子项回收回recycledPool
     *
     * @param child 视图将被回收
     */
    void recycleView(View child);

    /**
     * 返回特定位置的布局助手
     */
    LayoutHelper findLayoutHelperByPosition(int position);

    /**
     * 返回layoutManager中的第一个可见项目位置
     */
    int findFirstVisibleItemPosition();

    /**
     * 返回layoutManager中最后一个可见项目位置
     */
    int findLastVisibleItemPosition();

    /**
     * @return 在vlayout中，为了使项目或布局助手之间的边距重叠，我们支持同级之间的垂直和水平边距重叠
     */
    boolean isEnableMarginOverLap();

    int getDecoratedLeft(View child);

    int getDecoratedTop(View child);

    int getDecoratedRight(View child);

    int getDecoratedBottom(View child);
}

