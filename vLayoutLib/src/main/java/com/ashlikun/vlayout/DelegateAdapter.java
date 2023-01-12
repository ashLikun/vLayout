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

import static androidx.recyclerview.widget.RecyclerView.NO_ID;

import android.util.Pair;
import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 适配器将其责任委托给子适配器
 *
 * @author villadora
 * @since 1.0.0
 */
public class DelegateAdapter extends VirtualLayoutAdapter<RecyclerView.ViewHolder> {

    @Nullable
    private AtomicInteger mIndexGen;

    private int mIndex = 0;

    private final boolean mHasConsistItemType;

    private SparseArray<Adapter> mItemTypeAry = new SparseArray<>();

    @NonNull
    private final List<Pair<AdapterDataObserver, Adapter>> mAdapters = new ArrayList<>();

    private int mTotal = 0;

    private final SparseArray<Pair<AdapterDataObserver, Adapter>> mIndexAry = new SparseArray<>();

    private long[] cantorReverse = new long[2];

    /**
     * 代理适配器合并多个子适配器，默认为线程不安全
     *
     * @param layoutManager layoutManager
     */
    public DelegateAdapter(VirtualLayoutManager layoutManager) {
        this(layoutManager, false, false);
    }

    /**
     * @param layoutManager      layoutManager
     * @param hasConsistItemType 子适配器itemType是否一致
     */
    public DelegateAdapter(VirtualLayoutManager layoutManager, boolean hasConsistItemType) {
        this(layoutManager, hasConsistItemType, false);
    }

    /**
     * @param layoutManager      layoutManager
     * @param hasConsistItemType 子适配器itemType是否一致
     * @param threadSafe         告诉适配器是否线程安全
     */
    DelegateAdapter(VirtualLayoutManager layoutManager, boolean hasConsistItemType, boolean threadSafe) {
        super(layoutManager);
        if (threadSafe) {
            mIndexGen = new AtomicInteger(0);
        }

        mHasConsistItemType = hasConsistItemType;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (mHasConsistItemType) {
            Adapter adapter = mItemTypeAry.get(viewType);
            if (adapter != null) {
                return adapter.onCreateViewHolder(parent, viewType);
            }

            return null;
        }


        // 反康托尔函数
        com.ashlikun.vlayout.Cantor.reverseCantor(viewType, cantorReverse);

        int index = (int)cantorReverse[1];
        int subItemType = (int)cantorReverse[0];

        Adapter adapter = findAdapterByIndex(index);
        if (adapter == null) {
            return null;
        }

        return adapter.onCreateViewHolder(parent, subItemType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
        if (pair == null) {
            return;
        }
        pair.second.onBindViewHolder(holder, position - pair.first.mStartPosition);
        pair.second.onBindViewHolderWithOffset(holder, position - pair.first.mStartPosition, position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
        if (pair == null) {
            return;
        }
        pair.second.onBindViewHolder(holder, position - pair.first.mStartPosition, payloads);
        pair.second.onBindViewHolderWithOffset(holder, position - pair.first.mStartPosition, position, payloads);

    }

    @Override
    public int getItemCount() {
        return mTotal;
    }

    /**
     * 委派适配器返回的itemType的大整数可能导致失败
     *
     * @param position 项目位置
     * @return 整数表示项视图类型
     */
    @Override
    public int getItemViewType(int position) {
        Pair<AdapterDataObserver, Adapter> p = findAdapterByPosition(position);
        if (p == null) {
            return RecyclerView.INVALID_TYPE;
        }

        int subItemType = p.second.getItemViewType(position - p.first.mStartPosition);

        if (subItemType < 0) {
            // 负整数，无效，仅返回
            return subItemType;
        }

        if (mHasConsistItemType) {
            mItemTypeAry.put(subItemType, p.second);
            return subItemType;
        }


        int index = p.first.mIndex;

        return (int) com.ashlikun.vlayout.Cantor.getCantor(subItemType, index);
    }


    @Override
    public long getItemId(int position) {
        Pair<AdapterDataObserver, Adapter> p = findAdapterByPosition(position);

        if (p == null) {
            return NO_ID;
        }

        long itemId = p.second.getItemId(position - p.first.mStartPosition);

        if (itemId < 0) {
            return NO_ID;
        }

        int index = p.first.mIndex;
        /*
         * 现在我们有一个配对函数的问题，我们使用cantor配对函数作为itemId。
         */
        return Cantor.getCantor(index, itemId);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        // do nothing
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        int position = holder.getLayoutPosition();
        if (position >= 0) {
            Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
            if (pair != null) {
                pair.second.onViewRecycled(holder);
            }
        }
    }


    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getLayoutPosition();
        if (position >= 0) {
            Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
            if (pair != null) {
                pair.second.onViewAttachedToWindow(holder);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getLayoutPosition();
        if (position >= 0) {
            Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
            if (pair != null) {
                pair.second.onViewDetachedFromWindow(holder);
            }
        }
    }


    /**
     * 不能将layoutHelpers设置为委派适配器
     */
    @Deprecated
    @Override
    public void setLayoutHelpers(List<LayoutHelper> helpers) {
        throw new UnsupportedOperationException("DelegateAdapter doesn't support setLayoutHelpers directly");
    }


    public void setAdapters(@Nullable List<Adapter> adapters) {
        clear();

        if (adapters == null) {
            adapters = Collections.emptyList();
        }

        List<LayoutHelper> helpers = new LinkedList<>();

        boolean hasStableIds = true;
        mTotal = 0;

        Pair<AdapterDataObserver, Adapter> pair;
        for (Adapter adapter : adapters) {
            // 每个适配器都有一个唯一的索引id
            AdapterDataObserver observer = new AdapterDataObserver(mTotal, mIndexGen == null ? mIndex++ : mIndexGen.incrementAndGet());
            adapter.registerAdapterDataObserver(observer);
            hasStableIds = hasStableIds && adapter.hasStableIds();
            LayoutHelper helper = adapter.onCreateLayoutHelper();

            helper.setItemCount(adapter.getItemCount());
            mTotal += helper.getItemCount();
            helpers.add(helper);
            pair = Pair.create(observer, adapter);
            mIndexAry.put(observer.mIndex, pair);
            mAdapters.add(pair);
        }

        if (!hasObservers()) {
            super.setHasStableIds(hasStableIds);
        }
        super.setLayoutHelpers(helpers);
    }

    /**
     * 在中添加适配器<code>position</code>
     *
     * @param position 添加适配器的索引
     * @param adapters adapters
     */
    public void addAdapters(int position, @Nullable List<Adapter> adapters) {
        if (adapters == null || adapters.size() == 0) {
            return;
        }
        if (position < 0) {
            position = 0;
        }

        if (position > mAdapters.size()) {
            position = mAdapters.size();
        }

        List<Adapter> newAdapter = new ArrayList<>();
        Iterator<Pair<AdapterDataObserver, Adapter>> itr = mAdapters.iterator();
        while (itr.hasNext()) {
            Pair<AdapterDataObserver, Adapter> pair = itr.next();
            Adapter theOrigin = pair.second;
            newAdapter.add(theOrigin);
        }
        for (Adapter adapter : adapters) {
            newAdapter.add(position, adapter);
            position++;
        }
        setAdapters(newAdapter);
    }

    /**
     * 将适配器附加到末端
     *
     * @param adapters 将附加适配器
     */
    public void addAdapters(@Nullable List<Adapter> adapters) {
        addAdapters(mAdapters.size(), adapters);
    }

    public void addAdapter(int position, @Nullable Adapter adapter) {
        addAdapters(position, Collections.singletonList(adapter));
    }

    public void addAdapter(@Nullable Adapter adapter) {
        addAdapters(Collections.singletonList(adapter));
    }

    public void removeFirstAdapter() {
        if (mAdapters != null && !mAdapters.isEmpty()) {
            Adapter targetAdatper = mAdapters.get(0).second;
            removeAdapter(targetAdatper);
        }
    }

    public void removeLastAdapter() {
        if (mAdapters != null && !mAdapters.isEmpty()) {
            Adapter targetAdatper = mAdapters.get(mAdapters.size() - 1).second;
            removeAdapter(targetAdatper);
        }
    }

    public void removeAdapter(int adapterIndex) {
        if (adapterIndex >= 0 && adapterIndex < mAdapters.size()) {
            Adapter targetAdatper = mAdapters.get(adapterIndex).second;
            removeAdapter(targetAdatper);
        }
    }

    public void removeAdapter(@Nullable Adapter targetAdapter) {
        if (targetAdapter == null) {
            return;
        }
        removeAdapters(Collections.singletonList(targetAdapter));
    }

    public void removeAdapters(@Nullable List<Adapter> targetAdapters) {
        if (targetAdapters == null || targetAdapters.isEmpty()) {
            return;
        }
        List<LayoutHelper> helpers = new LinkedList<>(super.getLayoutHelpers());
        for (int i = 0, size = targetAdapters.size(); i < size; i++) {
            Adapter one = targetAdapters.get(i);
            Iterator<Pair<AdapterDataObserver, Adapter>> itr = mAdapters.iterator();
            while (itr.hasNext()) {
                Pair<AdapterDataObserver, Adapter> pair = itr.next();
                Adapter theOther = pair.second;
                if (theOther.equals(one)) {
                    theOther.unregisterAdapterDataObserver(pair.first);
                    final int position = findAdapterPositionByIndex(pair.first.mIndex);
                    if (position >= 0 && position < helpers.size()) {
                        helpers.remove(position);
                    }
                    itr.remove();
                    break;
                }
            }
        }

        List<Adapter> newAdapter = new ArrayList<>();
        Iterator<Pair<AdapterDataObserver, Adapter>> itr = mAdapters.iterator();
        while (itr.hasNext()) {
            newAdapter.add(itr.next().second);
        }
        setAdapters(newAdapter);
    }

    public void clear() {
        mTotal = 0;
        mIndex = 0;
        if (mIndexGen != null) {
            mIndexGen.set(0);
        }
        mLayoutManager.setLayoutHelpers(null);

        for (Pair<AdapterDataObserver, Adapter> p : mAdapters) {
            p.second.unregisterAdapterDataObserver(p.first);
        }


        mItemTypeAry.clear();
        mAdapters.clear();
        mIndexAry.clear();
    }

    public int getAdaptersCount() {
        return mAdapters == null ? 0 : mAdapters.size();
    }

    /**
     * @param absoultePosition
     * @return 子适配器中的相对位置除以DelegateAdapter中的绝对位置。如果未找到子适配器，则返回-1。
     */
    public int findOffsetPosition(int absoultePosition) {
        Pair<AdapterDataObserver, Adapter> p = findAdapterByPosition(absoultePosition);
        if (p == null) {
            return -1;
        }
        int subAdapterPosition = absoultePosition - p.first.mStartPosition;
        return subAdapterPosition;
    }

    @Nullable
    public Pair<AdapterDataObserver, Adapter> findAdapterByPosition(int position) {
        final int count = mAdapters.size();
        if (count == 0) {
            return null;
        }

        int s = 0, e = count - 1, m;
        Pair<AdapterDataObserver, Adapter> rs = null;

        // 二进制搜索范围
        while (s <= e) {
            m = (s + e) / 2;
            rs = mAdapters.get(m);
            int endPosition = rs.first.mStartPosition + rs.second.getItemCount() - 1;

            if (rs.first.mStartPosition > position) {
                e = m - 1;
            } else if (endPosition < position) {
                s = m + 1;
            } else if (rs.first.mStartPosition <= position && endPosition >= position) {
                break;
            }

            rs = null;
        }

        return rs;
    }


    public int findAdapterPositionByIndex(int index) {
        Pair<AdapterDataObserver, Adapter> rs = mIndexAry.get(index);
        return rs == null ? -1 : mAdapters.indexOf(rs);
    }

    public Adapter findAdapterByIndex(int index) {
        Pair<AdapterDataObserver, Adapter> rs = mIndexAry.get(index);
        return rs.second;
    }

    protected class AdapterDataObserver extends RecyclerView.AdapterDataObserver {
        int mStartPosition;

        int mIndex = -1;

        public AdapterDataObserver(int startPosition, int index) {
            this.mStartPosition = startPosition;
            this.mIndex = index;
        }

        public void updateStartPositionAndIndex(int startPosition, int index) {
            this.mStartPosition = startPosition;
            this.mIndex = index;
        }

        public int getStartPosition() {
            return mStartPosition;
        }

        public int getIndex() {
            return mIndex;
        }

        private boolean updateLayoutHelper() {
            if (mIndex < 0) {
                return false;
            }

            final int idx = findAdapterPositionByIndex(mIndex);
            if (idx < 0) {
                return false;
            }

            Pair<AdapterDataObserver, Adapter> p = mAdapters.get(idx);
            List<LayoutHelper> helpers = new LinkedList<>(getLayoutHelpers());
            LayoutHelper helper = helpers.get(idx);

            if (helper.getItemCount() != p.second.getItemCount()) {
                // 如果itemCount已更改；
                helper.setItemCount(p.second.getItemCount());

                mTotal = mStartPosition + p.second.getItemCount();

                for (int i = idx + 1; i < mAdapters.size(); i++) {
                    Pair<AdapterDataObserver, Adapter> pair = mAdapters.get(i);
                    // 更新以下适配器的startPosition
                    pair.first.mStartPosition = mTotal;
                    mTotal += pair.second.getItemCount();
                }

                // 将助手设置为刷新范围
                DelegateAdapter.super.setLayoutHelpers(helpers);
            }
            return true;
        }

        @Override
        public void onChanged() {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeRemoved(mStartPosition + positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeInserted(mStartPosition + positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemMoved(mStartPosition + fromPosition, mStartPosition + toPosition);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeChanged(mStartPosition + positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeChanged(mStartPosition + positionStart, itemCount, payload);
        }
    }


    public static abstract class Adapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
        public abstract LayoutHelper onCreateLayoutHelper();

        protected void onBindViewHolderWithOffset(VH holder, int position, int offsetTotal) {

        }

        protected void onBindViewHolderWithOffset(VH holder, int position, int offsetTotal, List<Object> payloads) {
            onBindViewHolderWithOffset(holder, position, offsetTotal);
        }
    }

}
