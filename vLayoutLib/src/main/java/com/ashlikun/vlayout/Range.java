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

import java.util.Objects;

/**
 * 范围对象
 */
public final class Range<T extends Comparable<? super T>> {

    /**
     * 创建一个新的不可变范围。
     * 端点是{@code [lower, upper]};即范围是有界的。{@code lower}必须是{@link Comparable#compareTo 小于或等于}{@ccode upper}。
     *
     * @param lower 下端点（含）
     * @param upper 上端点（含）
     * @throws NullPointerException 如果{@code lower}或{@code upper}为{@code null}
     */
    public Range(@NonNull final T lower, @NonNull final T upper) {
        if (lower == null) {
            throw new IllegalArgumentException("lower must not be null");
        }

        if (upper == null) {
            throw new IllegalArgumentException("upper must not be null");
        }

        mLower = lower;
        mUpper = upper;

        if (lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException("lower must be less than or equal to upper");
        }
    }

    /**
     * 创建一个新的不可变范围，并推断参数类型。
     * 端点是{@code[lower，upper]}；即范围是有界的。{@code lower}必须是{@link Comparable#compareTo 小于或等于} {@ccode upper}。
     * </p>
     *
     * @param lower 下端点（含）
     * @param upper 上端点（含）
     * @throws NullPointerException 如果{@code lower}或{@code upper}为{@code null}
     */
    public static <T extends Comparable<? super T>> Range<T> create(final T lower, final T upper) {
        return new Range<T>(lower, upper);
    }

    /**
     * 获取下端点。
     *
     * @return a non-{@code null} {@code T} reference
     */
    public T getLower() {
        return mLower;
    }

    /**
     * 获取上端点。
     *
     * @return a non-{@code null} {@code T} reference
     */
    public T getUpper() {
        return mUpper;
    }

    /**
     * 检查{@code值}是否在此范围内。
     * 如果{@code>=}是下端点＜i＞，而＜i＞{@code<=}为上端点（使用{@linkComparable}接口），则该值被视为在此范围内
     *
     * @param value a non-{@code null} {@code T} reference
     * @return {@code true}如果值在此包含范围内，则{@ccode false}否则
     * @throws NullPointerException 如果{@code value}为{@ccode null}
     */
    public boolean contains(@NonNull T value) {
        if (value == null)
            throw new IllegalArgumentException("value must not be null");

        boolean gteLower = value.compareTo(mLower) >= 0;
        boolean lteUpper = value.compareTo(mUpper) <= 0;

        return gteLower && lteUpper;
    }

    /**
     * 检查另一个｛@code范围｝是否在此范围内。
     * <p>如果某个范围的两个端点都在此范围内，则该范围被视为在此范围内。</p>
     *
     * @param range a non-{@code null} {@code T} reference
     * @return ｛@code true｝如果范围在此包含范围内，则｛@ccode false｝否则
     * @throws NullPointerException if {@code range} was {@code null}
     */
    public boolean contains(@NonNull Range<T> range) {
        if (range == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        boolean gteLower = range.mLower.compareTo(mLower) >= 0;
        boolean lteUpper = range.mUpper.compareTo(mUpper) <= 0;

        return gteLower && lteUpper;
    }

    /**
     * 比较两个范围是否相等。
     * <p>当且仅当下端点和上端点都相等时，范围被视为相等。</p>
     *
     * @return ｛@code true｝如果范围相等，则｛@ccode false｝否则
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (obj instanceof Range) {
            Range other = (Range) obj;
            return mLower.equals(other.mLower) && mUpper.equals(other.mUpper);
        }
        return false;
    }

    /**
     * 将｛@code value｝限制在此范围内。
     * 如果值在此范围内，则返回该值。否则，如果它比下端点｛@code＜｝，则返回下端点，否则返回上端点。使用｛@link Comparable｝接口执行比较。
     *
     * @param value a non-{@code null} {@code T} reference
     * @return ｛@code value｝被限制在此范围内。
     */
    public T clamp(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        if (value.compareTo(mLower) < 0) {
            return mLower;
        } else if (value.compareTo(mUpper) > 0) {
            return mUpper;
        } else {
            return value;
        }
    }

    /**
     * 返回此范围与另一个｛@code范围｝的交集。
     * <p>
     * E.g. if a {@code <} b {@code <} c {@code <} d, the
     * intersection of [a, c] and [b, d] ranges is [b, c].
     * As the endpoints are object references, there is no guarantee
     * which specific endpoint reference is used from the input ranges:</p>
     * <p>
     * E.g. if a {@code ==} a' {@code <} b {@code <} c, the
     * intersection of [a, b] and [a', c] ranges could be either
     * [a, b] or ['a, b], where [a, b] could be either the exact
     * input range, or a newly created range with the same endpoints.</p>
     *
     * @param range a non-{@code null} {@code Range<T>} reference
     * @return the intersection of this range and the other range.
     * @throws NullPointerException     if {@code range} was {@code null}
     * @throws IllegalArgumentException if the ranges are disjoint.
     */
    public Range<T> intersect(Range<T> range) {
        if (range == null) {
            throw new IllegalArgumentException("range must not be null");
        }

        int cmpLower = range.mLower.compareTo(mLower);
        int cmpUpper = range.mUpper.compareTo(mUpper);

        if (cmpLower <= 0 && cmpUpper >= 0) {
            // range includes this
            return this;
        } else if (cmpLower >= 0 && cmpUpper <= 0) {
            // this inludes range
            return range;
        } else {
            return Range.create(
                    cmpLower <= 0 ? mLower : range.mLower,
                    cmpUpper >= 0 ? mUpper : range.mUpper);
        }
    }

    /**
     * 返回此范围与包含范围的交集
     * 由指定 {@code [lower, upper]}.
     * <p>
     * See {@link #intersect(Range)} 有关详细信息.</p>
     *
     * @param lower a non-{@code null} {@code T} reference
     * @param upper a non-{@code null} {@code T} reference
     * @return the intersection of this range and the other range
     * @throws NullPointerException     if {@code lower} or {@code upper} was {@code null}
     * @throws IllegalArgumentException 如果范围不相交。
     */
    public Range<T> intersect(T lower, T upper) {
        if (lower == null) {
            throw new IllegalArgumentException("lower must not be null");
        }

        if (upper == null) {
            throw new IllegalArgumentException("upper must not be null");
        }

        int cmpLower = lower.compareTo(mLower);
        int cmpUpper = upper.compareTo(mUpper);

        if (cmpLower <= 0 && cmpUpper >= 0) {
            // [lower, upper] 包括这个
            return this;
        } else {
            return Range.create(
                    cmpLower <= 0 ? mLower : lower,
                    cmpUpper >= 0 ? mUpper : upper);
        }
    }

    /**
     * 返回包含此范围和另一个范围的最小范围 {@code range}.
     * <p>
     * E.g. if a {@code <} b {@code <} c {@code <} d, the
     * extension of [a, c] and [b, d] ranges is [a, d].
     * As the endpoints are object references, there is no guarantee
     * which specific endpoint reference is used from the input ranges:</p>
     * <p>
     * E.g. if a {@code ==} a' {@code <} b {@code <} c, the
     * extension of [a, b] and [a', c] ranges could be either
     * [a, c] or ['a, c], where ['a, c] could be either the exact
     * input range, or a newly created range with the same endpoints.</p>
     *
     * @param range a non-{@code null} {@code Range<T>} reference
     * @return the extension of this range and the other range.
     * @throws NullPointerException if {@code range} was {@code null}
     */
    public Range<T> extend(Range<T> range) {
        if (range == null) {
            throw new IllegalArgumentException("range must not be null");
        }

        int cmpLower = range.mLower.compareTo(mLower);
        int cmpUpper = range.mUpper.compareTo(mUpper);

        if (cmpLower <= 0 && cmpUpper >= 0) {
            // other includes this
            return range;
        } else if (cmpLower >= 0 && cmpUpper <= 0) {
            // this inludes other
            return this;
        } else {
            return Range.create(
                    cmpLower >= 0 ? mLower : range.mLower,
                    cmpUpper <= 0 ? mUpper : range.mUpper);
        }
    }

    /**
     * 返回包含此范围的最小范围以及 {@code [lower, upper]}.
     * <p>
     * See {@link #extend(Range)} 了解更多详情。</p>
     *
     * @param lower a non-{@code null} {@code T} reference
     * @param upper a non-{@code null} {@code T} reference
     * @return the extension of this range and the other range.
     * @throws NullPointerException if {@code lower} or {@code
     *                              upper} was {@code null}
     */
    public Range<T> extend(T lower, T upper) {
        if (lower == null) {
            throw new IllegalArgumentException("lower must not be null");
        }

        if (upper == null) {
            throw new IllegalArgumentException("upper must not be null");
        }

        int cmpLower = lower.compareTo(mLower);
        int cmpUpper = upper.compareTo(mUpper);

        if (cmpLower >= 0 && cmpUpper <= 0) {
            // 这包括其他
            return this;
        } else {
            return Range.create(
                    cmpLower >= 0 ? mLower : lower,
                    cmpUpper <= 0 ? mUpper : upper);
        }
    }

    /**
     * 返回包含此范围和 {@code value}.
     * <p>
     * See {@link #extend(Range)} 有关详细信息，此方法等效于 {@code extend(Range.create(value, value))}.</p>
     *
     * @param value a non-{@code null} {@code T} reference
     * @return 此范围的扩展和值。
     * @throws NullPointerException if {@code value} was {@code null}
     */
    public Range<T> extend(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return extend(value, value);
    }

    /**
     * 以字符串表示形式返回范围 {@code "[lower, upper]"}.
     *
     * @return 范围的字符串表示
     */
    @Override
    public String toString() {
        return String.format("[%s, %s]", mLower, mUpper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLower, mUpper);
    }

    private final T mLower;
    private final T mUpper;
}
