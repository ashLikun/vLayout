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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;

import androidx.recyclerview.widget.RecyclerView;

import com.ashlikun.vlayout.LayoutManagerHelper;
import com.ashlikun.vlayout.OrientationHelperEx;
import com.ashlikun.vlayout.VirtualLayoutManager;


/**
 * 将定位为固定位置的LayoutHelper。出现和消失能够
 * 通过动画传输。
 * 动画示例：
 *<blockquote>
 * <pre>
 *     {@code
 * setFixViewAnimatorHelper(new FixViewAnimatorHelper() {
 * @Override
 * public ViewPropertyAnimator onGetFixViewAppearAnimator(View fixView) {
 * int height = fixView.getMeasuredHeight();
 * fixView.setTranslationY(-height);
 * return fixView.animate().translationYBy(height).alpha(1.0f).setDuration(500);
 * }
 *
 * @Override
 * public ViewPropertyAnimator onGetFixViewDisappearAnimator(View fixView) {
 * int height = fixView.getMeasuredHeight();
 * return fixView.animate().translationYBy(-height).alpha(0.0f).setDuration(500);
 * }
 * });
 *     }
 * </pre>
 *</blockquote>
 */
public class FixLayoutHelper extends FixAreaLayoutHelper {

    private static final String TAG = "FixLayoutHelper";

    public static final int TOP_LEFT = 0;

    public static final int TOP_RIGHT = 1;

    public static final int BOTTOM_LEFT = 2;

    public static final int BOTTOM_RIGHT = 3;

    private int mPos = -1;

    private int mAlignType = TOP_LEFT;

    protected int mX = 0;

    protected int mY = 0;

    private boolean mSketchMeasure = false;

    protected View mFixView = null;

    protected boolean mDoNormalHandle = false;

    private boolean mShouldDrawn = true;

    private boolean isAddFixViewImmediately = false;

    private boolean isRemoveFixViewImmediately = true;

    private FixViewAppearAnimatorListener
            mFixViewAppearAnimatorListener = new FixViewAppearAnimatorListener();

    private FixViewDisappearAnimatorListener
            mFixViewDisappearAnimatorListener = new FixViewDisappearAnimatorListener();

    public FixLayoutHelper(int x, int y) {
        this(TOP_LEFT, x, y);
    }

    public FixLayoutHelper(int alignType, int x, int y) {
        this.mAlignType = alignType;
        this.mX = x;
        this.mY = y;
        setItemCount(1);
    }

    @Override
    public void setItemCount(int itemCount) {
        if (itemCount > 0) {
            super.setItemCount(1);
        } else {
            super.setItemCount(0);
        }
    }


    /**
     * FixLayoutHelper中的边距被禁用
     */
    @Override
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
    }


    public void setX(int x) {
        this.mX = x;
    }

    public void setY(int y) {
        this.mY = y;
    }

    public void setAlignType(int alignType) {
        this.mAlignType = alignType;
    }


    public void setSketchMeasure(boolean sketchMeasure) {
        mSketchMeasure = sketchMeasure;
    }

    /**
     * 仅使用启动，不应使用此测量值
     *
     * @param start 此layoutHelper处理的项目的位置
     * @param end   将被忽略 {@link ScrollFixLayoutHelper}
     */
    @Override
    public void onRangeChange(int start, int end) {
        this.mPos = start;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
            VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result,
            final LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        if (!mShouldDrawn) {
            layoutState.skipCurrentPosition();
            return;
        }

        // 在当前位置查找视图
        View view = mFixView;
        if (view == null) {
            view = layoutState.next(recycler);
        } else {
            layoutState.skipCurrentPosition();
        }

        if (view == null) {
            result.mFinished = true;
            return;
        }

        mDoNormalHandle = state.isPreLayout();

        if (mDoNormalHandle) {
            // in PreLayout do normal layout
            helper.addChildView(layoutState, view);
        }

        mFixView = view;

        doMeasureAndLayout(view, helper);

        result.mConsumed = 0;
        result.mIgnoreConsumed = true;

        handleStateOnResult(result, view);

    }

    @Override
    public boolean requireLayoutView() {
        return false;
    }

    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
            LayoutManagerHelper helper) {
        super.beforeLayout(recycler, state, helper);

        if (mFixView != null && helper.isViewHolderUpdated(mFixView)) {
            // 回收视图以供以后使用
            helper.removeChildView(mFixView);
            recycler.recycleView(mFixView);
            mFixView = null;
            isAddFixViewImmediately = true;
        }

        mDoNormalHandle = false;
    }

    @Override
    public void afterLayout(final RecyclerView.Recycler recycler, RecyclerView.State state,
            int startPosition, int endPosition, int scrolled,
            final LayoutManagerHelper helper) {
        super.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);

        // 如果mPos为负数，则禁用
        if (mPos < 0) {
            return;
        }

        if (mDoNormalHandle && state.isPreLayout()) {
            if (mFixView != null) {
//                Log.d(TAG, "after layout doNormal removeView");
                helper.removeChildView(mFixView);
                recycler.recycleView(mFixView);
                isAddFixViewImmediately = false;
            }

            mFixView = null;
            return;
        }

        // 不在正常流量中
        if (shouldBeDraw(helper, startPosition, endPosition, scrolled)) {
            mShouldDrawn = true;
            if (mFixView != null) {
                // 如果屏幕上未显示，则已在layoutViews阶段捕获
                if (mFixView.getParent() == null) {
                    addFixViewWithAnimator(helper, mFixView);
                } else {
                    // helper.removeChildView(mFixView);
                    helper.addFixedView(mFixView);
                    isRemoveFixViewImmediately = false;
                }
            } else {
                Runnable action = new Runnable() {
                    @Override
                    public void run() {
                        mFixView = recycler.getViewForPosition(mPos);
                        doMeasureAndLayout(mFixView, helper);
                        if (isAddFixViewImmediately) {
                            helper.addFixedView(mFixView);
                            isRemoveFixViewImmediately = false;
                        } else {
                            addFixViewWithAnimator(helper, mFixView);
                        }
                    }
                };
                if (mFixViewDisappearAnimatorListener.isAnimating()) {
                    mFixViewDisappearAnimatorListener.withEndAction(action);
                } else {
                    action.run();
                }
            }
        } else {
            mShouldDrawn = false;
            if (mFixView != null) {
                removeFixViewWithAnimator(recycler, helper, mFixView);
                mFixView = null;
            }
        }

    }

    private void addFixViewWithAnimator(LayoutManagerHelper layoutManagerHelper, View fixView) {
        if (mFixViewAnimatorHelper != null) {
            ViewPropertyAnimator animator = mFixViewAnimatorHelper
                    .onGetFixViewAppearAnimator(fixView);
            if (animator != null) {
                fixView.setVisibility(View.INVISIBLE);
                layoutManagerHelper.addFixedView(fixView);
                mFixViewAppearAnimatorListener.bindAction(layoutManagerHelper, fixView);
                animator.setListener(mFixViewAppearAnimatorListener).start();
            } else {
                layoutManagerHelper.addFixedView(fixView);
            }
        } else {
            layoutManagerHelper.addFixedView(fixView);
        }
        isRemoveFixViewImmediately = false;
    }

    private void removeFixViewWithAnimator(RecyclerView.Recycler recycler,
            LayoutManagerHelper layoutManagerHelper, View fixView) {
        if (!isRemoveFixViewImmediately && mFixViewAnimatorHelper != null) {
            ViewPropertyAnimator animator = mFixViewAnimatorHelper
                    .onGetFixViewDisappearAnimator(fixView);
            if (animator != null) {
                mFixViewDisappearAnimatorListener
                        .bindAction(recycler, layoutManagerHelper, fixView);
                animator.setListener(mFixViewDisappearAnimatorListener).start();
                isAddFixViewImmediately = false;
            } else {
                layoutManagerHelper.removeChildView(fixView);
                recycler.recycleView(fixView);
                isAddFixViewImmediately = false;
            }
        } else {
            layoutManagerHelper.removeChildView(fixView);
            recycler.recycleView(fixView);
            isAddFixViewImmediately = false;
        }

    }

    /**
     * 决定是否应显示视图
     *
     * @param helper layoutManagerHelper
     * @param startPosition RecyclerView中的第一个可见位置
     * @param endPosition   RecyclerView中最后一个可见位置
     * @param scrolled      在此滚动期间将滚动多少像素，0在
     *                      布局
     * @return 是否应显示当前layoutHelper中的视图
     */
    protected boolean shouldBeDraw(LayoutManagerHelper helper, int startPosition, int endPosition, int scrolled) {
        return true;
    }

    @Override
    public View getFixedView() {
        return mFixView;
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        if (mFixView != null) {
            helper.removeChildView(mFixView);
            helper.recycleView(mFixView);
            mFixView.animate().cancel();
            mFixView = null;
            isAddFixViewImmediately = false;
        }
    }

    private void doMeasureAndLayout(View view, LayoutManagerHelper helper) {
        if (view == null || helper == null) {
            return;
        }

        final VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view
                .getLayoutParams();

        final OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            final int widthSpec = helper.getChildMeasureSpec(
                    helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(),
                    params.width >= 0 ? params.width
                            : ((mSketchMeasure && layoutInVertical) ? LayoutParams.MATCH_PARENT
                                    : LayoutParams.WRAP_CONTENT), false);
            int heightSpec;
            if (!Float.isNaN(params.mAspectRatio) && params.mAspectRatio > 0) {
                heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper
                                .getPaddingBottom(),
                        (int) (View.MeasureSpec.getSize(widthSpec) / params.mAspectRatio + 0.5f),
                        false);
            } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
                heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper
                                .getPaddingBottom(),
                        (int) (View.MeasureSpec.getSize(widthSpec) / mAspectRatio + 0.5f),
                        false);
            } else {
                heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper
                                .getPaddingBottom(),
                        params.height >= 0 ? params.height
                                : ((mSketchMeasure && !layoutInVertical) ? LayoutParams.MATCH_PARENT
                                        : LayoutParams.WRAP_CONTENT),
                        false);
            }

            // do measurement
            helper.measureChildWithMargins(view, widthSpec, heightSpec);
        } else {
            final int heightSpec = helper.getChildMeasureSpec(
                    helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                            params.height >= 0 ? params.height
                            : ((mSketchMeasure && !layoutInVertical) ? LayoutParams.MATCH_PARENT
                                    : LayoutParams.WRAP_CONTENT), false);
            int widthSpec;

            if (!Float.isNaN(params.mAspectRatio) && params.mAspectRatio > 0) {
                widthSpec = helper.getChildMeasureSpec(
                        helper.getContentWidth() - helper.getPaddingLeft() - helper
                                .getPaddingRight(),
                        (int) (View.MeasureSpec.getSize(heightSpec) * params.mAspectRatio + 0.5f),
                        false);
            } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
                widthSpec = helper.getChildMeasureSpec(
                        helper.getContentWidth() - helper.getPaddingLeft() - helper
                                .getPaddingRight(),
                        (int) (View.MeasureSpec.getSize(heightSpec) * mAspectRatio + 0.5f),
                        false);
            } else {
                widthSpec = helper.getChildMeasureSpec(
                        helper.getContentWidth() - helper.getPaddingLeft() - helper
                                .getPaddingRight(),
                        params.width >= 0 ? params.width
                                : ((mSketchMeasure && layoutInVertical) ? LayoutParams.MATCH_PARENT
                                        : LayoutParams.WRAP_CONTENT),
                        false);
            }


            // do measurement
            helper.measureChildWithMargins(view, widthSpec, heightSpec);
        }

        int left, top, right, bottom;
        if (mAlignType == TOP_RIGHT) {
            top = helper.getPaddingTop() + mY + mAdjuster.top;
            right = helper.getContentWidth() - helper.getPaddingRight() - mX - mAdjuster.right;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            bottom = top + params.topMargin + params.bottomMargin + view.getMeasuredHeight();
        } else if (mAlignType == BOTTOM_LEFT) {
            left = helper.getPaddingLeft() + mX + mAdjuster.left;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY - mAdjuster.bottom;
            right = left + params.leftMargin + params.rightMargin + view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else if (mAlignType == BOTTOM_RIGHT) {
            right = helper.getContentWidth() - helper.getPaddingRight() - mX - mAdjuster.right;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY - mAdjuster.bottom;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else {
            // TOP_LEFT
            left = helper.getPaddingLeft() + mX + mAdjuster.left;
            top = helper.getPaddingTop() + mY + mAdjuster.top;
            right = left + (layoutInVertical ? orientationHelper
                    .getDecoratedMeasurementInOther(view)
                    : orientationHelper.getDecoratedMeasurement(view));
            bottom = top + (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view)
                    : orientationHelper.getDecoratedMeasurementInOther(view));
        }

        layoutChildWithMargin(view, left, top, right, bottom, helper);
    }

    private static class FixViewAppearAnimatorListener extends AnimatorListenerAdapter {

        private LayoutManagerHelper mLayoutManagerHelper;

        private View mFixView;

        public void bindAction(LayoutManagerHelper layoutManagerHelper, View fixView) {
            mLayoutManagerHelper = layoutManagerHelper;
            mFixView = fixView;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mFixView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }
    }

    private static class FixViewDisappearAnimatorListener extends AnimatorListenerAdapter {

        private boolean isAnimating;

        private RecyclerView.Recycler mRecycler;

        private LayoutManagerHelper mLayoutManagerHelper;

        private View mFixView;

        private Runnable mEndAction;

        public void bindAction(RecyclerView.Recycler recycler,
                LayoutManagerHelper layoutManagerHelper, View fixView) {
            isAnimating = true;
            mRecycler = recycler;
            mLayoutManagerHelper = layoutManagerHelper;
            mFixView = fixView;
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mLayoutManagerHelper.removeChildView(mFixView);
            mRecycler.recycleView(mFixView);
            isAnimating = false;
            if (mEndAction != null) {
                mEndAction.run();
                mEndAction = null;
            }
        }

        public boolean isAnimating() {
            return isAnimating;
        }

        public void withEndAction(Runnable action) {
            this.mEndAction = action;
        }

    }

}