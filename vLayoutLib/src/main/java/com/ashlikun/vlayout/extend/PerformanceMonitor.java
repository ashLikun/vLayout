package com.ashlikun.vlayout.extend;

import android.view.View;

import androidx.annotation.Keep;

/**
 * 在度量和布局期间添加回调，帮助您监视视图的性能。设计为类而不是接口能够在将来扩展api。
 */
public class PerformanceMonitor {

    /**
     * 记录开始时间
     * @param phase
     * @param viewType
     */
    @Keep
    public void recordStart(String phase, String viewType) {

    }

    /**
     * 记录结束时间
     * @param phase
     * @param viewType
     */
    @Keep
    public void recordEnd(String phase, String viewType) {

    }

    /**
     * 记录开始时间
     * @param phase
     * @param view
     */
    @Keep
    public void recordStart(String phase, View view) {

    }

    /**
     * 记录结束时间
     * @param phase
     * @param view
     */
    @Keep
    public void recordEnd(String phase, View view) {

    }

}
