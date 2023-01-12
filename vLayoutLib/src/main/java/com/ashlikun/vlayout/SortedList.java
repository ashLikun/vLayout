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

import java.lang.reflect.Array;


/**
 * 一种排序列表实现，它可以保持项目有序，并通知列表中的更改
 * 这样它可以绑定到{@link androidx.recyclerview.widget.RecyclerView.Adapter }
 * <p>
 * 它使用{@link Callback#compare(Object, Object)}方法对项目进行排序，并使用二进制搜索来检索项目。如果项目的排序标准可能会更改，请确保在编辑它们时调用适当的方法，以避免数据不一致。
 * <p>
 * 您可以通过{@link Callback}参数控制项目的顺序和更改通知。
 */
public class SortedList<T> {

    /**
     * {@link #indexOf(Object)}在列表中找不到项目时使用。
     */
    public static final int INVALID_POSITION = -1;

    private static final int MIN_CAPACITY = 10;
    private static final int CAPACITY_GROWTH = MIN_CAPACITY;
    private static final int INSERTION = 1;
    private static final int DELETION = 1 << 1;
    private static final int LOOKUP = 1 << 2;
    T[] mData;

    /**
     * 控制SortedList行为并在发生更改时得到通知的回调实例。
     */
    private Callback mCallback;

    private BatchedCallback mBatchedCallback;

    private int mSize;
    private final Class<T> mTClass;

    /**
     * 创建T类型的新SortedList。
     *
     * @param klass    SortedList内容的类。
     * @param callback 控制SortedList行为的回调。
     */
    public SortedList(Class<T> klass, Callback<T> callback) {
        this(klass, callback, MIN_CAPACITY);
    }

    /**
     * 创建T类型的新SortedList。
     *
     * @param klass           SortedList内容的类。
     * @param callback        控制SortedList行为的回调。
     * @param initialCapacity 存放物品的初始容量。
     */
    public SortedList(Class<T> klass, Callback<T> callback, int initialCapacity) {
        mTClass = klass;
        mData = (T[]) Array.newInstance(klass, initialCapacity);
        mCallback = callback;
        mSize = 0;
    }

    /**
     * 列表中的项目数。
     *
     * @return 列表中的项目数。
     */
    public int size() {
        return mSize;
    }

    /**
     * 将给定项添加到列表中。如果这是一个新项目，则SortedList调用
     * {@link Callback#onInserted(int, int)}.
     * 如果列表中已存在该项目，且其排序条件未更改，则将替换为现有项目。排序列表使用
     * {@link Callback#areItemsTheSame(Object, Object)}检查两个项是否为同一项，并使用{@link Callback#areContentsTheSame(Object, Object)}
     * 决定是否应调用{@link Callback#onChanged(int, int)}。在这两种情况下，它总是删除对旧项的引用，并将新项放入后备数组，
     * 即使{@link Callback#areContentsTheSame(Object, Object)}返回false。
     * <p>
     * 如果更改了项目的排序标准，则SortedList将无法在列表中找到其重复项，这将导致列表中的项目重复。如果需要更新列表中已存在的项的排序条件，
     * 请使用{@link #updateItemAt(int, Object)}。在更新对象之前，可以使用{@link #indexOf(Object)}查找项的索引。
     *
     * @param item 要添加到列表中的项目。
     * @return 新添加项目的索引。
     * @see {@link Callback#compare(Object, Object)}
     * @see {@link Callback#areItemsTheSame(Object, Object)}
     * @see {@link Callback#areContentsTheSame(Object, Object)}}
     */
    public int add(T item) {
        return add(item, true);
    }

    /**
     * 在调用此方法到调用{@link #endBatchedUpdates()}之间发生的批处理适配器更新。例如，如果在循环中添加多个项并将它们放入连续索引中，
     * 则SortedList只使用正确的项计数调用{@link Callback#onInserted(int, int)}一次。如果某个事件不能与前一个事件合并，则会立即将前一事件分派给回调。
     * 运行数据更新后，<b>必须<b>调用 {@link #endBatchedUpdates()}，它将向当前回调分派任何延迟的数据更改事件。
     * <p>
     * 示例实现可能如下所示：
     * <pre>
     *     mSortedList.beginBatchedUpdates();
     *     try {
     *         mSortedList.add(item1)
     *         mSortedList.add(item2)
     *         mSortedList.remove(item3)
     *         ...
     *     } finally {
     *         mSortedList.endBatchedUpdates();
     *     }
     * </pre>
     * <p>
     * 您可以使用扩展{@link BatchedCallback}的Callback，而不是使用此方法来批处理调用。在这种情况下，
     * 您必须确保在完成数据更改后立即手动调用{@link BatchedCallback#dispatchLastEvent()}。如果不这样做，可能会导致回调数据不一致。
     * <p>
     * 如果{@link BatchedCallback}实例中的当前回调，则调用此方法无效。
     */
    public void beginBatchedUpdates() {
        if (mCallback instanceof BatchedCallback) {
            return;
        }
        if (mBatchedCallback == null) {
            mBatchedCallback = new BatchedCallback(mCallback);
        }
        mCallback = mBatchedCallback;
    }

    /**
     * 结束更新事务并将任何剩余事件分派给回调。
     */
    public void endBatchedUpdates() {
        if (mCallback instanceof BatchedCallback) {
            ((BatchedCallback) mCallback).dispatchLastEvent();
        }
        if (mCallback == mBatchedCallback) {
            mCallback = mBatchedCallback.mWrappedCallback;
        }
    }

    private int add(T item, boolean notify) {
        int index = findIndexOf(item, INSERTION);
        if (index == INVALID_POSITION) {
            index = 0;
        } else if (index < mSize) {
            T existing = mData[index];
            if (mCallback.areItemsTheSame(existing, item)) {
                if (mCallback.areContentsTheSame(existing, item)) {
                    //no change but still replace the item
                    mData[index] = item;
                    return index;
                } else {
                    mData[index] = item;
                    mCallback.onChanged(index, 1);
                    return index;
                }
            }
        }
        addToData(index, item);
        if (notify) {
            mCallback.onInserted(index, 1);
        }
        return index;
    }

    /**
     * 从列表中删除提供的项并调用 {@link Callback#onRemoved(int, int)}.
     *
     * @param item 要从列表中删除的项目。
     * @return 如果项已删除，则为True；如果列表中找不到项，则为false.
     */
    public boolean remove(T item) {
        return remove(item, true);
    }

    /**
     * 删除给定索引处的项并调用{@link Callback#onRemoved(int, int)}.
     *
     * @param index 要删除的项的索引。
     * @return 删除的项目。
     */
    public T removeItemAt(int index) {
        T item = get(index);
        removeItemAtIndex(index, true);
        return item;
    }

    private boolean remove(T item, boolean notify) {
        int index = findIndexOf(item, DELETION);
        if (index == INVALID_POSITION) {
            return false;
        }
        removeItemAtIndex(index, notify);
        return true;
    }

    private void removeItemAtIndex(int index, boolean notify) {
        System.arraycopy(mData, index + 1, mData, index, mSize - index - 1);
        mSize--;
        mData[mSize] = null;
        if (notify) {
            mCallback.onRemoved(index, 1);
        }
    }

    /**
     * 更新给定索引处的项，并在必要时调用{@link Callback#onChanged(int, int)}和或{@link Callback#onMoved(int, int)}。
     * <p>
     * 如果需要更改现有项，使其在列表中的位置可能会更改，则可以使用此方法。
     * <p>
     * 如果新对象是其他对象(<code>get(index) != item</code>) and
     * {@link Callback#areContentsTheSame(Object, Object)} 回报 <code>true</code>, SortedList避免调用
     * {@link Callback#onChanged(int, int)} 否则它会调用
     * {@link Callback#onChanged(int, int)}.
     * <p>
     * 如果项目的新位置与提供的<code>index<code>不同，则排序列表
     * calls {@link Callback#onMoved(int, int)}.
     *
     * @param index 要替换的项目的索引
     * @param item  要替换给定索引中的项的项。
     * @see #add(Object)
     */
    public void updateItemAt(int index, T item) {
        final T existing = get(index);
        // 如果返回相同的对象，则假定已更改
        boolean contentsChanged = existing == item || !mCallback.areContentsTheSame(existing, item);
        if (existing != item) {
            // 不同的项目，我们可以使用比较，并且可以避免查找
            final int cmp = mCallback.compare(existing, item);
            if (cmp == 0) {
                mData[index] = item;
                if (contentsChanged) {
                    mCallback.onChanged(index, 1);
                }
                return;
            }
        }
        if (contentsChanged) {
            mCallback.onChanged(index, 1);
        }
        // TODO 这在一次传递中完成，以避免两次移位。
        removeItemAtIndex(index, false);
        int newIndex = add(item, false);
        if (index != newIndex) {
            mCallback.onMoved(index, newIndex);
        }
    }

    /**
     * 此方法可用于重新计算项在给定索引处的位置，而不触发 {@link Callback#onChanged(int, int)}回调。
     * <p>
     * 如果您正在编辑列表中的对象，使其在列表中的位置可能会更改，但不想触发onChange动画，则可以使用此方法重新定位它。
     * 如果项目更改位置，SortedList将调用 {@link Callback#onMoved(int, int)}，而不调用 {@link Callback#onChanged(int, int)}。
     * <p>
     * 示例用法如下：
     * <pre>
     *     final int position = mSortedList.indexOf(item);
     *     item.incrementPriority(); // assume items are sorted by priority
     *     mSortedList.recalculatePositionOfItemAt(position);
     * </pre>
     * 在上面的示例中，由于项目的排序条件已更改，mSortedList.indexOf（item）将无法找到该项目。这就是为什么上面的代码首先
     * 在编辑项目之前获取位置，对其进行编辑，并通知SortedList应重新定位项目。
     *
     * @param index 应重新计算其位置的项的当前索引。
     * @see #updateItemAt(int, Object)
     * @see #add(Object)
     */
    public void recalculatePositionOfItemAt(int index) {
        // TODO 可以改进
        final T item = get(index);
        removeItemAtIndex(index, false);
        int newIndex = add(item, false);
        if (index != newIndex) {
            mCallback.onMoved(index, newIndex);
        }
    }

    /**
     * 返回给定索引处的项。
     *
     * @param index 要检索的项的索引。
     * @return 给定索引处的项。
     * @throws IndexOutOfBoundsException 如果所提供的索引是负数或大于列表的大小。
     */
    public T get(int index) throws IndexOutOfBoundsException {
        if (index >= mSize || index < 0) {
            throw new IndexOutOfBoundsException("Asked to get item at " + index + " but size is "
                    + mSize);
        }
        return mData[index];
    }

    /**
     * 返回所提供项的位置。
     *
     * @param item 要查询位置的项目。
     * @return 所提供项目的位置，如果项目不在列表中，则为 {@link #INVALID_POSITION} 。
     */
    public int indexOf(T item) {
        return findIndexOf(item, LOOKUP);
    }

    private int findIndexOf(T item, int reason) {
        int left = 0;
        int right = mSize;
        while (left < right) {
            final int middle = (left + right) / 2;
            T myItem = mData[middle];
            final int cmp = mCallback.compare(myItem, item);
            if (cmp < 0) {
                left = middle + 1;
            } else if (cmp == 0) {
                if (mCallback.areItemsTheSame(myItem, item)) {
                    return middle;
                } else {
                    int exact = linearEqualitySearch(item, middle, left, right);
                    if (reason == INSERTION) {
                        return exact == INVALID_POSITION ? middle : exact;
                    } else {
                        return exact;
                    }
                }
            } else {
                right = middle;
            }
        }
        return reason == INSERTION ? left : INVALID_POSITION;
    }

    private int linearEqualitySearch(T item, int middle, int left, int right) {
        // go left
        for (int next = middle - 1; next >= left; next--) {
            T nextItem = mData[next];
            int cmp = mCallback.compare(nextItem, item);
            if (cmp != 0) {
                break;
            }
            if (mCallback.areItemsTheSame(nextItem, item)) {
                return next;
            }
        }
        for (int next = middle + 1; next < right; next++) {
            T nextItem = mData[next];
            int cmp = mCallback.compare(nextItem, item);
            if (cmp != 0) {
                break;
            }
            if (mCallback.areItemsTheSame(nextItem, item)) {
                return next;
            }
        }
        return INVALID_POSITION;
    }

    private void addToData(int index, T item) {
        if (index > mSize) {
            throw new IndexOutOfBoundsException(
                    "cannot add item to " + index + " because size is " + mSize);
        }
        if (mSize == mData.length) {
            // we are at the limit enlarge
            T[] newData = (T[]) Array.newInstance(mTClass, mData.length + CAPACITY_GROWTH);
            System.arraycopy(mData, 0, newData, 0, index);
            newData[index] = item;
            System.arraycopy(mData, index, newData, index + 1, mSize - index);
            mData = newData;
        } else {
            // just shift, we fit
            System.arraycopy(mData, index, mData, index + 1, mSize - index);
            mData[index] = item;
        }
        mSize++;
    }

    /**
     * 控制 {@link SortedList}行为的类。
     * <p>
     * 它定义了项目应如何排序以及重复项应如何处理。
     * <p>
     * SortedList调用该类的回调方法，以通知有关基础数据的更改。
     */
    public static abstract class Callback<T2> {

        /**
         * 类似于{@link java.util.Comparator#compare(Object, Object)}，应该比较两者并返回它们的排序方式。
         *
         * @param o1 要比较的第一个对象。
         * @param o2 要比较的第二个对象。
         * @return 负整数、零或正整数，因为第一个参数小于、等于或大于第二个参数。
         */
        abstract public int compare(T2 o1, T2 o2);

        /**
         * 在给定位置插入项时由SortedList调用。
         *
         * @param position 新项目的位置。
         * @param count    已添加的项目数。
         */
        abstract public void onInserted(int position, int count);

        /**
         * 从给定位置移除项时由SortedList调用。
         *
         * @param position 已删除项目的位置。
         * @param count    已删除的项目数。
         */
        abstract public void onRemoved(int position, int count);

        /**
         * 当项目更改其在列表中的位置时，由SortedList调用。
         *
         * @param fromPosition 移动前项目的上一个位置。
         * @param toPosition   项目的新位置。
         */
        abstract public void onMoved(int fromPosition, int toPosition);

        /**
         * 在更新给定位置的项时由SortedList调用。
         *
         * @param position 已更新项目的位置。
         * @param count    已更改的项目数。
         */
        abstract public void onChanged(int position, int count);

        /**
         * 当SortedList想要检查两个项目是否具有相同的数据时，由它调用。SortedList使用此信息决定是否应调用{@link #onChanged(int, int)}。
         * <p>
         * SortedList使用此方法检查相等性，而不是{@link Object#equals(Object)}因此
         * 您可以根据UI更改其行为。
         * <p>
         * 例如，如果您将SortedList与{@link androidx.recyclerview.widget.RecyclerView.Adapter
         * RecyclerView.Adapter}一起使用，则应该
         * 返回项目的视觉表示是否相同。
         *
         * @param oldItem 对象的先前表示。
         * @param newItem 替换上一个对象的新对象。
         * @return 如果项目的内容相同，则为True；如果项目不同，则为false。
         */
        abstract public boolean areContentsTheSame(T2 oldItem, T2 newItem);

        /**
         * 由SortedList调用，以决定两个对象是否表示相同的Item。
         * <p>
         * 例如，如果您的项具有唯一ID，则此方法应检查它们的相等性。
         *
         * @param item1 要检查的第一项。
         * @param item2 要检查的第二项。
         * @return 如果两个项目表示相同的对象，则为True；如果它们不同，则为false。
         */
        abstract public boolean areItemsTheSame(T2 item1, T2 item2);
    }

    /**
     * 一种回调实现，可以批量通知SortedList调度的事件。
     * <p>
     * 如果您想对一个SortedList执行多个操作，但不想逐个分派每个事件，这可能会导致性能问题，那么这个类很有用。
     * <p>
     * 例如，如果要将多个项目添加到SortedList，则BatchedCallback调用将单个 <code>onInserted(index, 1)</code> 回调一个
     * <code>onInserted(index, N)</code> 如果项目被添加到连续索引中。此更改可以帮助RecyclerView更轻松地解决更改。
     * <p>
     * 如果SortedList中的连续更改不适合批处理，则BatchingCallback会在检测到这种情况后立即分派这些更改。在完成对SortedList的编辑后，<b>must</b> always call {@link BatchedCallback#dispatchLastEvent()} 来刷新对Callback的所有更改。
     */
    public static class BatchedCallback<T2> extends Callback<T2> {

        private final Callback<T2> mWrappedCallback;
        static final int TYPE_NONE = 0;
        static final int TYPE_ADD = 1;
        static final int TYPE_REMOVE = 2;
        static final int TYPE_CHANGE = 3;
        static final int TYPE_MOVE = 4;

        int mLastEventType = TYPE_NONE;
        int mLastEventPosition = -1;
        int mLastEventCount = -1;

        /**
         * 创建包装所提供回调的新BatchedCallback。
         *
         * @param wrappedCallback 应接收数据更改回调的回调。来自SortedList的其他方法调用（例如 {@link #compare(Object, Object)})直接转发到此回调。
         */
        public BatchedCallback(Callback<T2> wrappedCallback) {
            mWrappedCallback = wrappedCallback;
        }

        @Override
        public int compare(T2 o1, T2 o2) {
            return mWrappedCallback.compare(o1, o2);
        }

        @Override
        public void onInserted(int position, int count) {
            if (mLastEventType == TYPE_ADD && position >= mLastEventPosition
                    && position <= mLastEventPosition + mLastEventCount) {
                mLastEventCount += count;
                mLastEventPosition = Math.min(position, mLastEventPosition);
                return;
            }
            dispatchLastEvent();
            mLastEventPosition = position;
            mLastEventCount = count;
            mLastEventType = TYPE_ADD;
        }

        @Override
        public void onRemoved(int position, int count) {
            if (mLastEventType == TYPE_REMOVE && mLastEventPosition == position) {
                mLastEventCount += count;
                return;
            }
            dispatchLastEvent();
            mLastEventPosition = position;
            mLastEventCount = count;
            mLastEventType = TYPE_REMOVE;
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            dispatchLastEvent();//moves are not merged
            mWrappedCallback.onMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count) {
            if (mLastEventType == TYPE_CHANGE &&
                    !(position > mLastEventPosition + mLastEventCount
                            || position + count < mLastEventPosition)) {
                // take potential overlap into account
                int previousEnd = mLastEventPosition + mLastEventCount;
                mLastEventPosition = Math.min(position, mLastEventPosition);
                mLastEventCount = Math.max(previousEnd, position + count) - mLastEventPosition;
                return;
            }
            dispatchLastEvent();
            mLastEventPosition = position;
            mLastEventCount = count;
            mLastEventType = TYPE_CHANGE;
        }

        @Override
        public boolean areContentsTheSame(T2 oldItem, T2 newItem) {
            return mWrappedCallback.areContentsTheSame(oldItem, newItem);
        }

        @Override
        public boolean areItemsTheSame(T2 item1, T2 item2) {
            return mWrappedCallback.areItemsTheSame(item1, item2);
        }


        /**
         * 此方法将任何挂起的事件通知分派给包装回调。
         * You <b>must</b> 在完成SortedList的编辑后，请始终调用此方法。
         */
        public void dispatchLastEvent() {
            if (mLastEventType == TYPE_NONE) {
                return;
            }
            switch (mLastEventType) {
                case TYPE_ADD:
                    mWrappedCallback.onInserted(mLastEventPosition, mLastEventCount);
                    break;
                case TYPE_REMOVE:
                    mWrappedCallback.onRemoved(mLastEventPosition, mLastEventCount);
                    break;
                case TYPE_CHANGE:
                    mWrappedCallback.onChanged(mLastEventPosition, mLastEventCount);
                    break;
            }
            mLastEventType = TYPE_NONE;
        }
    }
}

