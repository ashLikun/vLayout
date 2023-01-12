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
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.ashlikun.vlayout.extend.InnerRecycledViewPool;

/**
 * PagerAdapter，使用RecycledPool，用于嵌套ViewPager。
 */
public abstract class RecyclablePagerAdapter<VH extends RecyclerView.ViewHolder> extends PagerAdapter {

    private RecyclerView.Adapter<VH> mAdapter;

    private InnerRecycledViewPool mRecycledViewPool;


    public RecyclablePagerAdapter(RecyclerView.Adapter<VH> adapter, RecyclerView.RecycledViewPool pool) {
        this.mAdapter = adapter;
        if (pool instanceof InnerRecycledViewPool) {
            this.mRecycledViewPool = (InnerRecycledViewPool) pool;
        } else {
            this.mRecycledViewPool = new InnerRecycledViewPool(pool);
        }
    }

    @Override
    public abstract int getCount();

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return o instanceof RecyclerView.ViewHolder && (((RecyclerView.ViewHolder) o).itemView == view);
    }

    /**
     * 从位置获取视图
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int itemViewType = getItemViewType(position);
        RecyclerView.ViewHolder holder = mRecycledViewPool.getRecycledView(itemViewType);

        if (holder == null) {
            holder = mAdapter.createViewHolder(container, itemViewType);
        }


        onBindViewHolder((VH) holder, position);
        //当一个页面中有多个嵌套的ViewPager时，itemViews的layoutParam将被重用，
        //因此layoutParam的属性（例如widthFactor和position）也将被重用，
        //而这些属性在重用期间应重置为默认值。
        //考虑到ViewPager.LayoutParams有一些无法在外部修改的内部属性，我们在这里提供了一个新的实例
        ViewPager.LayoutParams layoutParams = new ViewPager.LayoutParams();
        if (holder.itemView.getLayoutParams() != null) {
            layoutParams.width = holder.itemView.getLayoutParams().width;
            layoutParams.height = holder.itemView.getLayoutParams().height;
        }

        container.addView(holder.itemView, layoutParams);

        return holder;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof RecyclerView.ViewHolder) {
            RecyclerView.ViewHolder holder = (RecyclerView.ViewHolder) object;
            container.removeView(holder.itemView);
            mRecycledViewPool.putRecycledView(holder);
        }
    }


    public abstract void onBindViewHolder(VH viewHolder, int position);

    public abstract int getItemViewType(int position);
}


