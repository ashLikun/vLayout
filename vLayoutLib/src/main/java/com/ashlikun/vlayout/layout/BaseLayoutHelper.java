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

import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.LayoutHelper;
import com.ashlikun.vlayout.LayoutManagerHelper;
import com.ashlikun.vlayout.OrientationHelperEx;
import com.ashlikun.vlayout.R;
import com.ashlikun.vlayout.VLayoutUtils;
import com.ashlikun.vlayout.VirtualLayoutManager;
import com.ashlikun.vlayout.VirtualLayoutManager.LayoutStateWrapper;

/**
 * {@link LayoutHelper} 提供基本方法
 */
public abstract class BaseLayoutHelper extends MarginLayoutHelper {

    private static final String TAG = "BaseLayoutHelper";


    protected Rect mLayoutRegion = new Rect();

    View mLayoutView;

    Drawable mBackground;

    float mAspectRatio = Float.NaN;

    public BaseLayoutHelper() {

    }

    @Override
    public boolean isFixLayout() {
        return false;
    }


    public Drawable getBackground() {
        return mBackground;
    }

    public void setBackground(Drawable background) {
        this.mBackground = background;
    }

    /**
     * LayoutView的颜色
     */
    public void setBgColor(int bgColor) {
        setBackground(new ColorDrawable(bgColor));
    }

    public void setAspectRatio(float aspectRatio) {
        this.mAspectRatio = aspectRatio;
    }

    public float getAspectRatio() {
        return mAspectRatio;
    }

    private int mItemCount = 0;

    /**
     * 当前布局中的项数
     *
     * @return 子视图数
     */
    @Override
    public int getItemCount() {
        return mItemCount;
    }

    @Override
    public void setItemCount(int itemCount) {
        this.mItemCount = itemCount;
    }

    /**
     * 检索下一个视图并将其添加到布局中，这是为了确保视图按顺序添加
     *
     * @param recycler    回收者生成视图
     * @param layoutState 当前布局状态
     * @param helper      添加视图的助手
     * @param result      块结果，告诉layoutManager布局过程是否结束
     * @return 下一个要渲染的视图，如果没有更多视图可用，则为空
     */
    @Nullable
    public final View nextView(RecyclerView.Recycler recycler, LayoutStateWrapper layoutState, LayoutManagerHelper helper, LayoutChunkResult result) {
        View view = layoutState.next(recycler);
        if (view == null) {
            // if we are laying out views in scrap, this may return null which means there is
            // no more items to layout.
            if (VLayoutUtils.isDebug && !layoutState.hasScrapList()) {
                throw new RuntimeException("received null view when unexpected");
            }
            // if there is no more views can be retrieved, this layout process is finished
            result.mFinished = true;
            return null;
        }

        helper.addChildView(layoutState, view);
        return view;
    }


    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                             LayoutManagerHelper helper) {
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "call beforeLayout() on " + this.getClass().getSimpleName());
        }


        if (requireLayoutView()) {
            if (mLayoutView != null) {
                // TODO: recycle LayoutView
                // helper.detachChildView(mLayoutView);
            }
        } else {
            // 如果不需要layoutView，请将其删除
            if (mLayoutView != null) {
                if (mLayoutViewUnBindListener != null) {
                    mLayoutViewUnBindListener.onUnbind(mLayoutView, this);
                }
                helper.removeChildView(mLayoutView);
                mLayoutView = null;
            }
        }
    }

    /**
     * 告诉滚动值是否有效，如果无效，则表示这是一个没有滚动的布局处理
     *
     * @param scrolled 滚动多少像素的值
     * @return true表示在滚动过程期间，false表示在布局过程期间。
     */
    protected boolean isValidScrolled(int scrolled) {
        return scrolled != Integer.MAX_VALUE && scrolled != Integer.MIN_VALUE;
    }


    @Override
    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                            int startPosition, int endPosition, int scrolled,
                            LayoutManagerHelper helper) {
        if (VLayoutUtils.isDebug) {
            Log.d(TAG, "call afterLayout() on " + this.getClass().getSimpleName());
        }


        if (requireLayoutView()) {
            if (isValidScrolled(scrolled) && mLayoutView != null) {
                // initial layout do reset
                mLayoutRegion.union(mLayoutView.getLeft(), mLayoutView.getTop(), mLayoutView.getRight(), mLayoutView.getBottom());
            }


            if (!mLayoutRegion.isEmpty()) {
                if (isValidScrolled(scrolled)) {
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                        mLayoutRegion.offset(0, -scrolled);
                    } else {
                        mLayoutRegion.offset(-scrolled, 0);
                    }
                }
                int contentWidth = helper.getContentWidth();
                int contentHeight = helper.getContentHeight();
                if (helper.getOrientation() == VirtualLayoutManager.VERTICAL ?
                        mLayoutRegion.intersects(0, -contentHeight / 4, contentWidth, contentHeight + contentHeight / 4) :
                        mLayoutRegion.intersects(-contentWidth / 4, 0, contentWidth + contentWidth / 4, contentHeight)) {

                    if (mLayoutView == null) {
                        mLayoutView = helper.generateLayoutView();
                        helper.addOffFlowView(mLayoutView, true);
                    }
                    //finally fix layoutRegion's height and with here to avoid visual blank
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                        mLayoutRegion.left = helper.getPaddingLeft() + mMarginLeft;
                        mLayoutRegion.right = helper.getContentWidth() - helper.getPaddingRight() - mMarginRight;
                    } else {
                        mLayoutRegion.top = helper.getPaddingTop() + mMarginTop;
                        mLayoutRegion.bottom = helper.getContentHeight() - helper.getPaddingBottom() - mMarginBottom;
                    }

                    bindLayoutView(mLayoutView);
                    return;
                } else {
                    mLayoutRegion.set(0, 0, 0, 0);
                    if (mLayoutView != null) {
                        mLayoutView.layout(0, 0, 0, 0);
                    }
                }
            }
        }

        if (mLayoutView != null) {
            if (mLayoutViewUnBindListener != null) {
                mLayoutViewUnBindListener.onUnbind(mLayoutView, this);
            }
            helper.removeChildView(mLayoutView);
            mLayoutView = null;
        }

    }

    @Override
    public void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper) {
        if (requireLayoutView()) {
            View refer = null;
            Rect tempRect = new Rect();
            final OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();
            for (int i = 0; i < helper.getChildCount(); i++) {
                refer = helper.getChildAt(i);
                int anchorPos = helper.getPosition(refer);
                if (getRange().contains(anchorPos)) {
                    if (refer.getVisibility() == View.GONE) {
                        tempRect.setEmpty();
                    } else {
                        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                                refer.getLayoutParams();
                        if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                            tempRect.union(helper.getDecoratedLeft(refer) - params.leftMargin,
                                    orientationHelper.getDecoratedStart(refer),
                                    helper.getDecoratedRight(refer) + params.rightMargin,
                                    orientationHelper.getDecoratedEnd(refer));
                        } else {
                            tempRect.union(orientationHelper.getDecoratedStart(refer),
                                    helper.getDecoratedTop(refer) - params.topMargin, orientationHelper.getDecoratedEnd(refer),
                                    helper.getDecoratedBottom(refer) + params.bottomMargin);
                        }
                    }
                }
            }
            if (!tempRect.isEmpty()) {
                mLayoutRegion.set(tempRect.left - mPaddingLeft, tempRect.top - mPaddingTop,
                        tempRect.right + mPaddingRight, tempRect.bottom + mPaddingBottom);
            } else {
                mLayoutRegion.setEmpty();
            }
            if (mLayoutView != null) {
                mLayoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
            }
        }
    }

    /**
     * 当{@link LayoutHelper}被删除时调用执行框架定义的默认清理作业
     *
     * @param helper LayoutManagerHelper
     */
    @Override
    public final void clear(LayoutManagerHelper helper) {
        // remove LayoutViews if there is one
        if (mLayoutView != null) {
            if (mLayoutViewUnBindListener != null) {
                mLayoutViewUnBindListener.onUnbind(mLayoutView, this);
            }
            helper.removeChildView(mLayoutView);
            mLayoutView = null;
        }

        // 呼叫用户定义
        onClear(helper);
    }

    /**
     * 当 {@link LayoutHelper} 被删除时调用，执行干净的自定义作业
     *
     * @param helper
     */
    protected void onClear(LayoutManagerHelper helper) {

    }

    /**
     * @return
     */
    @Override
    public boolean requireLayoutView() {
        return mBackground != null || mLayoutViewBindListener != null;
    }

    public abstract void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                                     LayoutStateWrapper layoutState, LayoutChunkResult result,
                                     LayoutManagerHelper helper);


    @Override
    public void doLayout(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        layoutViews(recycler, state, layoutState, result, helper);
    }

    /**
     * Helper函数，它可以布局子对象，也可以更新layoutRegion，但它不会考虑布局中的边距，因此如果您将边距应用于layoutView，则需要注意边距
     *
     * @param child  child that will be laid
     * @param left   left position
     * @param top    top position
     * @param right  right position
     * @param bottom bottom position
     * @param helper layoutManagerHelper，帮助孩子
     */
    protected void layoutChildWithMargin(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper) {
        layoutChildWithMargin(child, left, top, right, bottom, helper, false);
    }

    protected void layoutChildWithMargin(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
        helper.layoutChildWithMargins(child, left, top, right, bottom);
        if (requireLayoutView()) {
            if (addLayoutRegionWithMargin) {
                mLayoutRegion
                        .union(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginTop,
                                right + mPaddingRight + mMarginRight,
                                bottom + mPaddingBottom + mMarginBottom);
            } else {
                mLayoutRegion.union(left - mPaddingLeft, top - mPaddingTop, right + mPaddingRight, bottom + mPaddingBottom);
            }
        }

    }

    /**
     * 帮助程序函数，用于布局子对象并更新layoutRegion
     *
     * @param child  即将分娩的孩子
     * @param left   left position
     * @param top    top position
     * @param right  right position
     * @param bottom bottom position
     * @param helper layoutManagerHelper，帮助孩子
     */
    protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper) {
        layoutChild(child, left, top, right, bottom, helper, false);
    }

    protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
        helper.layoutChild(child, left, top, right, bottom);
        if (requireLayoutView()) {
            if (addLayoutRegionWithMargin) {
                mLayoutRegion
                        .union(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginTop,
                                right + mPaddingRight + mMarginRight,
                                bottom + mPaddingBottom + mMarginBottom);
            } else {
                mLayoutRegion.union(left - mPaddingLeft, top - mPaddingTop, right + mPaddingRight, bottom + mPaddingBottom);
            }
        }

    }

    /**
     * 侦听器处理LayoutView，如bgImage
     */
    public interface LayoutViewBindListener {
        void onBind(View layoutView, BaseLayoutHelper baseLayoutHelper);
    }

    /**
     * 侦听器处理LayoutView，如bgImage
     */
    public interface LayoutViewUnBindListener {
        void onUnbind(View layoutView, BaseLayoutHelper baseLayoutHelper);
    }


    public interface LayoutViewHelper {

        /**
         * 通过维护layoutView和图像url之间的映射或设置要查看的唯一标记来实现它。这取决于你的选择。
         *
         * @param layoutView 视图已准备好与图像绑定
         *                   * @param id layoutView的标识符
         */
        void onBindViewSuccess(View layoutView, String id);
    }

    private LayoutViewUnBindListener mLayoutViewUnBindListener;

    private LayoutViewBindListener mLayoutViewBindListener;

    /**
     * 帮助者决定是否调用 {@link LayoutViewBindListener#onBind(View, BaseLayoutHelper)}.
     * 这里有一个性能问题： {@link LayoutViewBindListener#onBind(View, BaseLayoutHelper)} is called during layout phase,
     * 当将图像绑定到视图树时，会导致视图树重新布局  {@link LayoutViewBindListener#onBind(View, BaseLayoutHelper)} 将被调用。
     * 用户应该提供足够的信息来告诉LayoutHelper图像是否绑定成功。若图像已成功绑定，则不再发生死循环。
     * <p>
     * 当然，你可以自己处理这个逻辑，而忽略这个助手。
     */
    public static class DefaultLayoutViewHelper implements LayoutViewBindListener, LayoutViewUnBindListener, LayoutViewHelper {

        private final LayoutViewBindListener mLayoutViewBindListener;

        private final LayoutViewUnBindListener mLayoutViewUnBindListener;

        public DefaultLayoutViewHelper(
                LayoutViewBindListener layoutViewBindListener,
                LayoutViewUnBindListener layoutViewUnBindListener) {
            mLayoutViewBindListener = layoutViewBindListener;
            mLayoutViewUnBindListener = layoutViewUnBindListener;
        }

        @Override
        public void onBindViewSuccess(View layoutView, String id) {
            layoutView.setTag(R.id.tag_layout_helper_bg, id);
        }

        @Override
        public void onBind(View layoutView, BaseLayoutHelper baseLayoutHelper) {
            if (layoutView.getTag(R.id.tag_layout_helper_bg) == null) {
                if (mLayoutViewBindListener != null) {
                    mLayoutViewBindListener.onBind(layoutView, baseLayoutHelper);
                }
            }
        }

        @Override
        public void onUnbind(View layoutView, BaseLayoutHelper baseLayoutHelper) {
            if (mLayoutViewUnBindListener != null) {
                mLayoutViewUnBindListener.onUnbind(layoutView, baseLayoutHelper);
            }
            layoutView.setTag(R.id.tag_layout_helper_bg, null);
        }
    }

    public void setLayoutViewHelper(DefaultLayoutViewHelper layoutViewHelper) {
        mLayoutViewBindListener = layoutViewHelper;
        mLayoutViewUnBindListener = layoutViewHelper;
    }

    /**
     * 更好地使用{@link #setLayoutViewHelper(DefaultLayoutViewHelper)}
     *
     * @param bindListener
     */
    public void setLayoutViewBindListener(LayoutViewBindListener bindListener) {
        mLayoutViewBindListener = bindListener;
    }

    /**
     * 更好地使用 {@link #setLayoutViewHelper(DefaultLayoutViewHelper)}
     *
     * @param layoutViewUnBindListener
     */
    public void setLayoutViewUnBindListener(
            LayoutViewUnBindListener layoutViewUnBindListener) {
        mLayoutViewUnBindListener = layoutViewUnBindListener;
    }

    @Override
    public void bindLayoutView(@NonNull final View layoutView) {
        layoutView.measure(View.MeasureSpec.makeMeasureSpec(mLayoutRegion.width(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mLayoutRegion.height(), View.MeasureSpec.EXACTLY));
        layoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
        layoutView.setBackground(mBackground);

        if (mLayoutViewBindListener != null) {
            mLayoutViewBindListener.onBind(layoutView, this);
        }

        // 重置区域矩形
        mLayoutRegion.set(0, 0, 0, 0);
    }

    protected void handleStateOnResult(LayoutChunkResult result, View view) {
        if (view == null) {
            return;
        }

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

        // 如果未删除或更改视图，则占用可用空间
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }

        // 搜索聚焦视图时使用
        result.mFocusable = result.mFocusable || view.isFocusable();

    }

    /**
     * 处理视图焦点状态的帮助程序方法
     *
     * @param result
     * @param views
     */
    protected void handleStateOnResult(LayoutChunkResult result, View[] views) {
        if (views == null) {
            return;
        }

        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view == null) {
                continue;
            }
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

            // 如果未删除或更改视图，则占用可用空间
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }

            // 搜索聚焦视图时使用
            result.mFocusable = result.mFocusable || view.isFocusable();

            if (result.mFocusable && result.mIgnoreConsumed) {
                break;
            }
        }
    }

    protected int computeStartSpace(LayoutManagerHelper helper, boolean layoutInVertical, boolean isLayoutEnd, boolean isOverLapMargin) {
        int startSpace = 0;
        LayoutHelper lastLayoutHelper = null;
        if (helper instanceof VirtualLayoutManager) {
            lastLayoutHelper = ((VirtualLayoutManager) helper).findNeighbourNonfixLayoutHelper(this, isLayoutEnd);
        }
        MarginLayoutHelper lastMarginLayoutHelper = null;

        if (lastLayoutHelper != null && lastLayoutHelper instanceof MarginLayoutHelper) {
            lastMarginLayoutHelper = (MarginLayoutHelper) lastLayoutHelper;
        }
        if (lastLayoutHelper == this) {
            return 0;
        }

        if (!isOverLapMargin) {
            startSpace = layoutInVertical ? mMarginTop + mPaddingTop : mMarginLeft + mPaddingLeft;
        } else {
            int offset = 0;

            if (lastMarginLayoutHelper == null) {
                offset = layoutInVertical ? mMarginTop + mPaddingTop : mMarginLeft + mPaddingLeft;
            } else {
                offset = layoutInVertical
                        ? (isLayoutEnd ? calGap(lastMarginLayoutHelper.mMarginBottom, mMarginTop) : calGap(lastMarginLayoutHelper.mMarginTop, mMarginBottom))
                        : (isLayoutEnd ? calGap(lastMarginLayoutHelper.mMarginRight, mMarginLeft) : calGap(lastMarginLayoutHelper.mMarginLeft, mMarginRight));
            }
            //Log.e("huang", "computeStartSpace offset: " + offset + ", isLayoutEnd: " + isLayoutEnd + ", " + this);
            startSpace += layoutInVertical
                    ? (isLayoutEnd ? mPaddingTop : mPaddingBottom)
                    : (isLayoutEnd ? mPaddingLeft : mPaddingRight);

            startSpace += offset;
        }
        return startSpace;
    }

    protected int computeEndSpace(LayoutManagerHelper helper, boolean layoutInVertical, boolean isLayoutEnd, boolean isOverLapMargin) {
        int endSpace = layoutInVertical
                ? mMarginBottom + mPaddingBottom : mMarginLeft + mPaddingLeft;
        //Log.e("huang", "computeEndSpace offset: " + endSpace + ", isLayoutEnd: " + isLayoutEnd + ", " + this);
        //Log.e("huang", "===================\n\n");
        return endSpace;
    }

    private int calGap(int gap, int currGap) {
        if (gap < currGap) {
            return currGap - gap;
        } else {
            return 0;
        }
    }
}
