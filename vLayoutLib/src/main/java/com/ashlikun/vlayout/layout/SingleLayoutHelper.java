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

import static com.ashlikun.vlayout.VirtualLayoutManager.VERTICAL;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.LayoutManagerHelper;
import com.ashlikun.vlayout.OrientationHelperEx;
import com.ashlikun.vlayout.VirtualLayoutManager;

/**
 * LayoutHelper仅包含一个视图
 */
public class SingleLayoutHelper extends ColumnLayoutHelper {

    private int mPos = -1;

    public SingleLayoutHelper() {
        setItemCount(1);
    }


    @Override
    public void setItemCount(int itemCount) {
        if (itemCount > 0) {
            super.setItemCount(1);
        } else {
            super.setItemCount(0);
        }
    }

    /**
     * 仅使用启动，不应使用此测量值
     *
     * @param start 此layoutHelper处理的项目的位置
     * @param end  将被忽略 {@link SingleLayoutHelper}
     */
    @Override
    public void onRangeChange(int start, int end) {
        this.mPos = start;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // 到达此布局的结尾
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        View view = layoutState.next(recycler);

        if (view == null) {
            result.mFinished = true;
            return;
        }


        helper.addChildView(layoutState, view);
        final VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        int parentWidth = helper.getContentWidth() - helper.getPaddingLeft() - helper
                .getPaddingRight() - getHorizontalMargin() - getHorizontalPadding();
        int parentHeight = helper.getContentHeight() - helper.getPaddingTop() - helper
                .getPaddingBottom() - getVerticalMargin() - getVerticalPadding();

        if (!Float.isNaN(mAspectRatio)) {
            if (layoutInVertical) {
                parentHeight = (int) (parentWidth / mAspectRatio + 0.5f);
            } else {
                parentWidth = (int) (parentHeight * mAspectRatio + 0.5f);
            }
        }

        if (layoutInVertical) {
            final int widthSpec = helper.getChildMeasureSpec(parentWidth,
                     Float.isNaN(mAspectRatio) ? params.width : parentWidth, !layoutInVertical && Float.isNaN(mAspectRatio));
            final int heightSpec = helper.getChildMeasureSpec(parentHeight,
                    Float.isNaN(params.mAspectRatio) ? (Float.isNaN(mAspectRatio) ? params.height : parentHeight) : (int) (
                            parentWidth / params.mAspectRatio + 0.5f), layoutInVertical && Float.isNaN(mAspectRatio));

            // do测量
            helper.measureChildWithMargins(view, widthSpec, heightSpec);
        } else {
            final int widthSpec = helper.getChildMeasureSpec(parentWidth,
                    Float.isNaN(params.mAspectRatio) ? (Float.isNaN(mAspectRatio) ? params.width : parentWidth) : (int) (
                            parentHeight * params.mAspectRatio + 0.5f), !layoutInVertical && Float.isNaN(mAspectRatio));
            final int heightSpec = helper.getChildMeasureSpec(parentHeight,
                     Float.isNaN(mAspectRatio) ? params.height : parentHeight, layoutInVertical && Float.isNaN(mAspectRatio));

            // do测量
            helper.measureChildWithMargins(view, widthSpec, heightSpec);
        }

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        result.mConsumed = orientationHelper.getDecoratedMeasurement(view);

        // do layout
        int left, top, right, bottom;
        if (layoutInVertical) {
            int viewWidth = orientationHelper.getDecoratedMeasurementInOther(view);
            int available = parentWidth - viewWidth;
            if (available < 0) {
                available = 0;
            }

            left = mMarginLeft + mPaddingLeft + helper.getPaddingLeft() + available / 2;
            right = helper.getContentWidth() - mMarginRight - mPaddingRight - helper.getPaddingRight() - available / 2;


            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                bottom = layoutState.getOffset() - mMarginBottom - mPaddingBottom;
                top = bottom - result.mConsumed;
            } else {
                top = layoutState.getOffset() + mMarginTop + mPaddingTop;
                bottom = top + result.mConsumed;
            }
        } else {
            int viewHeight = orientationHelper.getDecoratedMeasurementInOther(view);
            int available = parentHeight - viewHeight;
            if (available < 0) {
                available = 0;
            }

            top = helper.getPaddingTop() + mMarginTop + mPaddingTop + available / 2;
            bottom = helper.getContentHeight() - -mMarginBottom - mPaddingBottom - helper.getPaddingBottom() - available / 2;

            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - mMarginRight - mPaddingRight;
                left = right - result.mConsumed;
            } else {
                left = layoutState.getOffset() + mMarginLeft + mPaddingLeft;
                right = left + result.mConsumed;
            }
        }

        if (layoutInVertical) {
            result.mConsumed += getVerticalMargin() + getVerticalPadding();
        } else {
            result.mConsumed += getHorizontalMargin() + getHorizontalPadding();
        }

        layoutChildWithMargin(view, left, top, right, bottom, helper);
    }

}
