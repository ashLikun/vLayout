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

import com.ashlikun.vlayout.LayoutHelper;
import com.ashlikun.vlayout.LayoutManagerHelper;

/**
 * {@link LayoutHelper} 提供边距和填充支持。
 */
public abstract class MarginLayoutHelper extends LayoutHelper {

    protected int mPaddingLeft;
    protected int mPaddingRight;
    protected int mPaddingTop;
    protected int mPaddingBottom;

    protected int mMarginLeft;
    protected int mMarginRight;
    protected int mMarginTop;
    protected int mMarginBottom;


    /**
     * 设置此layoutHelper的填充
     * @param leftPadding left padding
     * @param topPadding top padding
     * @param rightPadding right padding
     * @param bottomPadding bottom padding
     */
    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        mPaddingLeft = leftPadding;
        mPaddingRight = rightPadding;
        mPaddingTop = topPadding;
        mPaddingBottom = bottomPadding;
    }

    /**
     * 设置此layoutHelper的边距
     *
     * @param leftMargin left margin
     * @param topMargin top margin
     * @param rightMargin right margin
     * @param bottomMargin bottom margin
     */
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        this.mMarginLeft = leftMargin;
        this.mMarginTop = topMargin;
        this.mMarginRight = rightMargin;
        this.mMarginBottom = bottomMargin;
    }

    /**
     * 使用定位视图开始新布局时计算对齐偏移
     *
     * @param offset      当前layoutHelper中锚定子项的偏移量，例如，0表示第一项
     * @param isLayoutEnd 布局过程是结束还是开始，true表示它将从头到尾放置视图
     * @param useAnchor   是为滚动还是为锚点重置计算偏移
     * @param helper      视图布局辅助对象
     * @return 开始定位视图的像素偏移
     */
    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }

    @Override
    public int computeMarginStart(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mMarginTop;
        } else {
            return mMarginLeft;
        }
    }

    @Override
    public int computeMarginEnd(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mMarginBottom;
        } else {
            return mMarginRight;
        }
    }

    @Override
    public int computePaddingStart(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mPaddingTop;
        } else {
            return mPaddingLeft;
        }
    }

    @Override
    public int computePaddingEnd(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mPaddingBottom;
        } else {
            return mPaddingRight;
        }
    }

    /**
     *获取水平维度的总边距
     *
     * @return
     */
    public int getHorizontalMargin() {
        return mMarginLeft + mMarginRight;
    }

    /**
     * 获取垂直维度的总边距
     *
     * @return
     */
    public int getVerticalMargin() {
        return mMarginTop + mMarginBottom;
    }

    /**
     * %1 (% 2) Get total population in horizontal dimension
     * @return
     */
    public int getHorizontalPadding() {
        return mPaddingLeft + mPaddingRight;
    }

    /**
     * 获取垂直维度中的总填充
     * @return
     */
    public int getVerticalPadding() {
        return mPaddingTop + mPaddingBottom;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public int getPaddingRight() {
        return mPaddingRight;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public int getMarginLeft() {
        return mMarginLeft;
    }

    public int getMarginRight() {
        return mMarginRight;
    }

    public int getMarginTop() {
        return mMarginTop;
    }

    public int getMarginBottom() {
        return mMarginBottom;
    }

    public void setPaddingLeft(int paddingLeft) {
        mPaddingLeft = paddingLeft;
    }

    public void setPaddingRight(int paddingRight) {
        mPaddingRight = paddingRight;
    }

    public void setPaddingTop(int paddingTop) {
        mPaddingTop = paddingTop;
    }

    public void setPaddingBottom(int paddingBottom) {
        mPaddingBottom = paddingBottom;
    }

    public void setMarginLeft(int marginLeft) {
        mMarginLeft = marginLeft;
    }

    public void setMarginRight(int marginRight) {
        mMarginRight = marginRight;
    }

    public void setMarginTop(int marginTop) {
        mMarginTop = marginTop;
    }

    public void setMarginBottom(int marginBottom) {
        mMarginBottom = marginBottom;
    }
}

