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

package com.ashlikun.vlayout.extend;

import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.io.Closeable;

/**
 * 包装原始的RecycledViewPool以提供视图上的销毁回调，
 * 还提供包装另一个ViewPool。此ViewPool不支持多线程。
 * 回收器的默认最大尺寸为原始尺寸的5，
 * 您也可以修改为更大的尺寸以满足您的需要。
 */
public final class InnerRecycledViewPool extends RecyclerView.RecycledViewPool {

    private static final String TAG = "InnerRecycledViewPool";

    private static int DEFAULT_MAX_SIZE = 20;

    /*
     * Wrapped InnerPool
     */
    private RecyclerView.RecycledViewPool mInnerPool;


    private SparseIntArray mScrapLength = new SparseIntArray();
    private SparseIntArray mMaxScrap = new SparseIntArray();

    /**
     * 包装现有池
     *
     * @param pool
     */
    public InnerRecycledViewPool(RecyclerView.RecycledViewPool pool) {
        this.mInnerPool = pool;
    }


    public InnerRecycledViewPool() {
        this(new RecyclerView.RecycledViewPool());
    }

    @Override
    public void clear() {
        for (int i = 0, size = mScrapLength.size(); i < size; i++) {
            int viewType = mScrapLength.keyAt(i);
            RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
            while (holder != null) {
                destroyViewHolder(holder);
                holder = mInnerPool.getRecycledView(viewType);
            }
        }

        mScrapLength.clear();
        super.clear();
    }

    @Override
    public void setMaxRecycledViews(int viewType, int max) {
        // 当viewType被更改时，由于无法获取包装池中的项目，请销毁viewType的所有项目
        RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
        while (holder != null) {
            destroyViewHolder(holder);
            holder = mInnerPool.getRecycledView(viewType);
        }

        // change maxRecycledViews
        this.mMaxScrap.put(viewType, max);
        this.mScrapLength.put(viewType, 0);
        mInnerPool.setMaxRecycledViews(viewType, max);
    }

    @Override
    public RecyclerView.ViewHolder getRecycledView(int viewType) {
        RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
        if (holder != null) {
            int scrapHeapSize = mScrapLength.indexOfKey(viewType) >= 0 ? this.mScrapLength.get(viewType) : 0;
            if (scrapHeapSize > 0) {
                mScrapLength.put(viewType, scrapHeapSize - 1);
            }
        }

        return holder;
    }


    /**
     * 获取当前池中的所有项目大小
     * @return ViewPool中项目的大小
     */
    public int size() {
        int count = 0;

        for (int i = 0, size = mScrapLength.size(); i < size; i++) {
            int val = mScrapLength.valueAt(i);
            count += val;
        }

        return count;
    }

    /**
     * 这将仅在UI线程中运行
     *
     * @param scrap 将回收的ViewHolder废料
     */
    @Override
    public void putRecycledView(RecyclerView.ViewHolder scrap) {
        int viewType = scrap.getItemViewType();

        if (mMaxScrap.indexOfKey(viewType) < 0) {
            // does't contains this viewType, initial scrap list
            mMaxScrap.put(viewType, DEFAULT_MAX_SIZE);
            setMaxRecycledViews(viewType, DEFAULT_MAX_SIZE);
        }

        // get current heap size
        int scrapHeapSize = mScrapLength.indexOfKey(viewType) >= 0 ? this.mScrapLength.get(viewType) : 0;

        if (this.mMaxScrap.get(viewType) > scrapHeapSize) {
            // if exceed current heap size
            mInnerPool.putRecycledView(scrap);
            mScrapLength.put(viewType, scrapHeapSize + 1);
        } else {
            // destroy viewHolder
            destroyViewHolder(scrap);
        }
    }


    private void destroyViewHolder(RecyclerView.ViewHolder holder) {
        View view = holder.itemView;
        // if view inherits {@link Closeable}, cal close method
        if (view instanceof Closeable) {
            try {
                ((Closeable) view).close();
            } catch (Exception e) {
                Log.w(TAG, Log.getStackTraceString(e), e);
            }
        }

        if (holder instanceof Closeable) {
            try {
                ((Closeable) holder).close();
            } catch (Exception e) {
                Log.w(TAG, Log.getStackTraceString(e), e);
            }
        }
    }

    public void setDefaultMaxSize(int maxSize) {
        DEFAULT_MAX_SIZE = maxSize;
    }
}
