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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link com.ashlikun.vlayout.LayoutHelperFinder} 的一个实现，它按位置查找layoutHelper
 */
public class RangeLayoutHelperFinder extends LayoutHelperFinder {

    @NonNull
    private List<LayoutHelperItem> mLayoutHelperItems = new LinkedList<>();

    @NonNull
    private List<com.ashlikun.vlayout.LayoutHelper> mLayoutHelpers = new LinkedList<>();

    @NonNull
    private List<com.ashlikun.vlayout.LayoutHelper> mReverseLayoutHelpers = new LinkedList<>();

    private LayoutHelperItem[] mSortedLayoutHelpers = null;

    @NonNull
    private Comparator<LayoutHelperItem> mLayoutHelperItemComparator = new Comparator<LayoutHelperItem>() {
        @Override
        public int compare(LayoutHelperItem lhs, LayoutHelperItem rhs) {
            return lhs.getStartPosition() - rhs.getStartPosition();
        }
    };

    @Override
    protected List<com.ashlikun.vlayout.LayoutHelper> reverse() {
        return mReverseLayoutHelpers;
    }

    /**
     * @param layouts layoutHelpers处理
     */
    @Override
    public void setLayouts(@Nullable List<com.ashlikun.vlayout.LayoutHelper> layouts) {
        mLayoutHelpers.clear();
        mReverseLayoutHelpers.clear();
        mLayoutHelperItems.clear();
        if (layouts != null) {
            ListIterator<com.ashlikun.vlayout.LayoutHelper> iterator = layouts.listIterator();
            com.ashlikun.vlayout.LayoutHelper helper = null;
            while (iterator.hasNext()) {
                helper = iterator.next();
                mLayoutHelpers.add(helper);
                mLayoutHelperItems.add(new LayoutHelperItem(helper));
            }

            while (iterator.hasPrevious()) {
                mReverseLayoutHelpers.add(iterator.previous());
            }

            // Collections.sort(mLayoutHelperItems, mLayoutHelperItemComparator);
            mSortedLayoutHelpers = mLayoutHelperItems.toArray(new LayoutHelperItem[mLayoutHelperItems.size()]);
            Arrays.sort(mSortedLayoutHelpers, mLayoutHelperItemComparator);
        }
    }

    @NonNull
    @Override
    protected List<com.ashlikun.vlayout.LayoutHelper> getLayoutHelpers() {
        return mLayoutHelpers;
    }

    @Nullable
    @Override
    public com.ashlikun.vlayout.LayoutHelper getLayoutHelper(int position) {
        if (mSortedLayoutHelpers == null || mSortedLayoutHelpers.length == 0) {
            return null;
        }
        final int count = mSortedLayoutHelpers.length;

        int s = 0, e = count - 1, m;
        LayoutHelperItem rs = null;
        // 二进制搜索范围
        while (s <= e) {
            m = (s + e) / 2;
            rs = mSortedLayoutHelpers[m];
            if (rs.getStartPosition() > position) {
                e = m - 1;
            } else if (rs.getEndPosition() < position) {
                s = m + 1;
            } else if (rs.getStartPosition() <= position && rs.getEndPosition() >= position) {
                break;
            }

            rs = null;
        }

        return rs == null ? null : rs.layoutHelper;
    }

    static class LayoutHelperItem {

        LayoutHelperItem(com.ashlikun.vlayout.LayoutHelper helper) {
            this.layoutHelper = helper;
        }

        LayoutHelper layoutHelper;

        public int getStartPosition() {
            return layoutHelper.getRange().getLower();
        }

        public int getEndPosition() {
            return layoutHelper.getRange().getUpper();
        }

    }
}
