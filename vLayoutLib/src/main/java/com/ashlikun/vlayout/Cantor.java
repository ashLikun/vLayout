package com.ashlikun.vlayout;

public class Cantor {

    /**
     * @param k1
     * @param k2
     * @return k1和k2的康托尔对
     */
    public static long getCantor(long k1, long k2) {
        return (k1 + k2) * (k1 + k2 + 1) / 2 + k2;
    }

    /**
     * 与原点编号k1和k2相反的康托尔对，k1存储在结果[0]中，k2存储在数据[1]中
     * @param cantor 计算的康托尔数
     * @param result 用于存储输出值的数组
     */
    public static void reverseCantor(long cantor, long[] result) {
        if (result == null || result.length < 2) {
            result = new long[2];
        }
        // 反康托尔函数
        long w = (long) (Math.floor(Math.sqrt(8 * cantor + 1) - 1) / 2);
        long t = (w * w + w) / 2;

        long k2 = cantor - t;
        long k1 = w - k2;
        result[0] = k1;
        result[1] = k2;
    }

}
