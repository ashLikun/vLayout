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

import java.util.List;

/**
 * LayoutHelperFinder提供LayoutHelper的存储库
 */
public abstract class LayoutHelperFinder {

    /**
     * 将布局放入取景器
     */
    abstract void setLayouts(@Nullable List<com.ashlikun.vlayout.LayoutHelper> layouts);

    /**
     * 在给定位置获取layoutHelper
     */
    @Nullable
    public abstract com.ashlikun.vlayout.LayoutHelper getLayoutHelper(int position);

    /**
     * 获取所有layoutHelper
     *
     * @return
     */
    @NonNull
    protected abstract List<com.ashlikun.vlayout.LayoutHelper> getLayoutHelpers();

    /**
     * 获取按相反顺序排列的layoutHelpers
     *
     * @return
     */
    protected abstract List<LayoutHelper> reverse();

}
