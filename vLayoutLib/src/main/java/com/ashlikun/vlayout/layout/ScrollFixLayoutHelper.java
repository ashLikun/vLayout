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

import com.ashlikun.vlayout.LayoutManagerHelper;

/**
 * 绝对布局，仅在滚动到“”位置后显示，它将基于leftMargintopMarginrightMarginbottomMargin布局视图。
 */
public class ScrollFixLayoutHelper extends FixLayoutHelper {
    public static final int SHOW_ALWAYS = 0;
    public static final int SHOW_ON_ENTER = 1;
    public static final int SHOW_ON_LEAVE = 2;


    private int mShowType = SHOW_ALWAYS;

    public ScrollFixLayoutHelper(int x, int y) {
        this(TOP_LEFT, x, y);
    }

    public ScrollFixLayoutHelper(int alignType, int x, int y) {
        super(alignType, x, y);
    }

    public void setShowType(int showType) {
        this.mShowType = showType;
    }

    public int getShowType() {
        return mShowType;
    }

    @Override
    protected boolean shouldBeDraw(LayoutManagerHelper helper, int startPosition, int endPosition, int scrolled) {
        switch (mShowType) {
            case SHOW_ON_ENTER:
                // when previous item is entering
                return endPosition >= getRange().getLower() - 1;
            case SHOW_ON_LEAVE:
                // show on leave from top
                // when next item is the first one in screen
                return startPosition >= getRange().getLower() + 1;
            case SHOW_ALWAYS:
            default:
                // default is always
                return true;
        }

    }

}
