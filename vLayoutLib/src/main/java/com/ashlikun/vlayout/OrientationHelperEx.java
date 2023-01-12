package com.ashlikun.vlayout;

import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

abstract public class OrientationHelperEx {

    private static final int INVALID_SIZE = Integer.MIN_VALUE;

    protected final com.ashlikun.vlayout.ExposeLinearLayoutManagerEx mLayoutManager;

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;

    public static final int VERTICAL = LinearLayout.VERTICAL;

    private int mLastTotalSpace = INVALID_SIZE;

    private OrientationHelperEx(com.ashlikun.vlayout.ExposeLinearLayoutManagerEx layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * 如果状态为NOT预布局，则在onLayout方法完成后调用此方法。此方法记录诸如布局边界之类的信息，这些信息可能在下一次布局计算中有用。
     */
    public void onLayoutComplete() {
        mLastTotalSpace = getTotalSpace();
    }

    /**
     * 返回上一次布局过程与当前布局过程之间的布局空间更改。
     * <p>
     * 确保在LayoutManager的{@link RecyclerView.LayoutManager#onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)}方法结束时调用{@link #onLayoutComplete()}。
     *
     * @return 当前总空间与上一布局的总空间之间的差异。
     * @see #onLayoutComplete()
     */
    public int getTotalSpaceChange() {
        return INVALID_SIZE == mLastTotalSpace ? 0 : getTotalSpace() - mLastTotalSpace;
    }

    /**
     * 返回视图的开头，包括其装饰和边距。
     * <p>
     * 例如，对于水平辅助对象，如果视图的左侧位于像素20处，具有2px左装饰和3px左边距，则返回值将为15px。
     *
     * @param view 要检查的视图元素
     * @return 元素的第一个像素
     * @see #getDecoratedEnd(View)
     */
    public abstract int getDecoratedStart(View view);

    /**
     * 返回视图的结尾，包括其装饰和边距。
     * <p>
     * 例如，对于水平辅助对象，如果视图的右侧位于像素200处，具有2px右装饰和3px右边距，则返回值将为205。
     *
     * @param view 要检查的视图元素
     * @return 元素的最后一个像素
     * @see #getDecoratedStart(View)
     */
    public abstract int getDecoratedEnd(View view);

    /**
     * 返回此视图在当前方向上占用的空间，包括装饰和边距。
     *
     * @param view 要检查的视图元素
     * @return 此视图占用的总空间
     * @see #getDecoratedMeasurementInOther(View)
     */
    public abstract int getDecoratedMeasurement(View view);

    /**
     * 返回此视图在垂直方向上占用的空间，包括装饰和边距。
     *
     * @param view 要检查的视图元素
     * @return 此视图在与当前视图垂直的方向上占用的总空间
     * @see #getDecoratedMeasurement(View)
     */
    public abstract int getDecoratedMeasurementInOther(View view);

    /**
     * 返回添加起始填充后布局的起始位置。
     *
     * @return 我们能画的第一个像素。
     */
    public abstract int getStartAfterPadding();

    /**
     * 返回删除结束填充后布局的结束位置。
     *
     * @return 此布局的结束边界。
     */
    public abstract int getEndAfterPadding();

    /**
     * 返回布局的结束位置，不考虑填充。
     *
     * @return 此布局的结束边界，不考虑填充。
     */
    public abstract int getEnd();

    /**
     * 按给定数量抵消所有子项的位置。
     *
     * @param amount 要添加到每个子级布局参数的值
     */
    public abstract void offsetChildren(int amount);

    /**
     * 返回布局的总空间。这个数字是
     * {@link #getEndAfterPadding()} and {@link #getStartAfterPadding()}.
     *
     * @return 布局子项的总空间
     */
    public abstract int getTotalSpace();

    /**
     * 沿此方向偏移子对象。
     *
     * @param view   View to offset
     * @param offset offset amount
     */
    public abstract void offsetChild(View view, int offset);

    /**
     * 返回布局末尾的填充。对于水平辅助对象，这是右侧填充，对于垂直辅助对象，则是底部填充。此方法不检查布局是否为RTL。
     *
     * @return The padding at the end of the layout.
     */
    public abstract int getEndPadding();

    /**
     * 为给定LayoutManager和方向创建OrientationHelper。
     *
     * @param layoutManager 要附加到的LayoutManager
     * @param orientation   所需方向。应该是 {@link #HORIZONTAL} or {@link #VERTICAL}
     * @return 新的OrientationHelper
     */
    public static OrientationHelperEx createOrientationHelper(
            com.ashlikun.vlayout.ExposeLinearLayoutManagerEx layoutManager, int orientation) {
        switch (orientation) {
            case HORIZONTAL:
                return createHorizontalHelper(layoutManager);
            case VERTICAL:
                return createVerticalHelper(layoutManager);
        }
        throw new IllegalArgumentException("invalid orientation");
    }

    /**
     * 为给定LayoutManager创建水平OrientationHelper。
     *
     * @param layoutManager 要附加到的LayoutManager。
     * @return 新的OrientationHelper
     */
    public static OrientationHelperEx createHorizontalHelper(
            com.ashlikun.vlayout.ExposeLinearLayoutManagerEx layoutManager) {
        return new OrientationHelperEx(layoutManager) {
            @Override
            public int getEndAfterPadding() {
                return mLayoutManager.getWidth() - mLayoutManager.getPaddingRight();
            }

            @Override
            public int getEnd() {
                return mLayoutManager.getWidth();
            }

            @Override
            public void offsetChildren(int amount) {
                mLayoutManager.offsetChildrenHorizontal(amount);
            }

            @Override
            public int getStartAfterPadding() {
                return mLayoutManager.getPaddingLeft();
            }

            @Override
            public int getDecoratedMeasurement(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin
                        + params.rightMargin : mLayoutManager.getDecoratedMeasuredWidth(view);
            }

            @Override
            public int getDecoratedMeasurementInOther(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
            }

            @Override
            public int getDecoratedEnd(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedRight(view)
                        + params.rightMargin : mLayoutManager.getDecoratedRight(view);
            }

            @Override
            public int getDecoratedStart(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedLeft(view)
                        - params.leftMargin : mLayoutManager.getDecoratedLeft(view);
            }

            @Override
            public int getTotalSpace() {
                return mLayoutManager.getWidth() - mLayoutManager.getPaddingLeft()
                        - mLayoutManager.getPaddingRight();
            }

            @Override
            public void offsetChild(View view, int offset) {
                view.offsetLeftAndRight(offset);
            }

            @Override
            public int getEndPadding() {
                return mLayoutManager.getPaddingRight();
            }
        };
    }

    /**
     * 为给定LayoutManager创建垂直OrientationHelper。
     *
     * @param layoutManager 要附加到的LayoutManager。
     * @return 新的OrientationHelper
     */
    public static OrientationHelperEx createVerticalHelper(com.ashlikun.vlayout.ExposeLinearLayoutManagerEx layoutManager) {
        return new OrientationHelperEx(layoutManager) {
            @Override
            public int getEndAfterPadding() {
                return mLayoutManager.getHeight() - mLayoutManager.getPaddingBottom();
            }

            @Override
            public int getEnd() {
                return mLayoutManager.getHeight();
            }

            @Override
            public void offsetChildren(int amount) {
                mLayoutManager.offsetChildrenVertical(amount);
            }

            @Override
            public int getStartAfterPadding() {
                return mLayoutManager.getPaddingTop();
            }

            @Override
            public int getDecoratedMeasurement(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin
                        + params.bottomMargin : mLayoutManager.getDecoratedMeasuredHeight(view);
            }

            @Override
            public int getDecoratedMeasurementInOther(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin;
            }

            @Override
            public int getDecoratedEnd(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedBottom(view)
                        + params.bottomMargin : mLayoutManager.getDecoratedBottom(view);
            }

            @Override
            public int getDecoratedStart(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                        view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedTop(view) - params.topMargin
                        : mLayoutManager.getDecoratedTop(view);
            }

            @Override
            public int getTotalSpace() {
                return mLayoutManager.getHeight() - mLayoutManager.getPaddingTop()
                        - mLayoutManager.getPaddingBottom();
            }

            @Override
            public void offsetChild(View view, int offset) {
                view.offsetTopAndBottom(offset);
            }

            @Override
            public int getEndPadding() {
                return mLayoutManager.getPaddingBottom();
            }
        };
    }
}
