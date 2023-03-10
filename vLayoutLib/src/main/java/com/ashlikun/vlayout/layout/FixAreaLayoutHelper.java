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

import android.view.View;
import android.view.ViewPropertyAnimator;

import com.ashlikun.vlayout.LayoutManagerHelper;

/**
 * 将定位为固定位置的LayoutHelper
 */
public abstract class FixAreaLayoutHelper extends BaseLayoutHelper {
    protected com.ashlikun.vlayout.layout.FixAreaAdjuster mAdjuster = com.ashlikun.vlayout.layout.FixAreaAdjuster.mDefaultAdjuster;

    protected FixViewAnimatorHelper mFixViewAnimatorHelper;

    public void setAdjuster(FixAreaAdjuster adjuster) {
        this.mAdjuster = adjuster;
    }

    public void setFixViewAnimatorHelper(
            FixViewAnimatorHelper fixViewAnimatorHelper) {
        mFixViewAnimatorHelper = fixViewAnimatorHelper;
    }

    @Override
    public void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper) {

    }

    @Override
    public boolean isFixLayout() {
        return true;
    }

    public interface FixViewAnimatorHelper {

        ViewPropertyAnimator onGetFixViewAppearAnimator(View fixView);

        ViewPropertyAnimator onGetFixViewDisappearAnimator(View fixView);

    }

    @Override
    public int computeMarginStart(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }

    @Override
    public int computeMarginEnd(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }

    @Override
    public int computePaddingStart(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }

    @Override
    public int computePaddingEnd(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }
}
