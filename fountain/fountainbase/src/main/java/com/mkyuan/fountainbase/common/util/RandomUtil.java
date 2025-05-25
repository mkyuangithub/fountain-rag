package com.mkyuan.fountainbase.common.util;

import java.util.Random;

public class RandomUtil {
    public static long getRandomLong() {
        // 使用时间戳加随机数的方式生成id
        long timestamp = System.currentTimeMillis();
        long nanoTime = System.nanoTime() & 0xFFFFFF; // 取纳秒时间的后24位

        // 生成9位随机数 (100000000 到 999999999)
        int random = 100000000 + new Random().nextInt(900000000);

        // 使用 & 运算确保结果为正数
        long id = (((timestamp & 0x1FFFFFFFL) << 32) | (nanoTime << 8) | (random & 0xFF)) & Long.MAX_VALUE;

        return id;
    }
    public static long getRandomShortLong() { //不超过99999999999999L
        // 上限设置为 99999999999999L (14位数)
        final long MAX_VALUE = 99999999999999L;

        // 获取纳秒时间的后32位
        long nanoTime = System.nanoTime() & 0xFFFFFFFFL;
        // 获取一个随机数
        long random = new Random().nextInt(10000) & 0xFFFFL;

        // 将纳秒时间和随机数组合
        long combined = (nanoTime << 16) | random;
        // 确保结果为正数且不超过上限
        long result = Math.abs(combined % MAX_VALUE);

        return result == 0 ? 1 : result; // 确保不会返回0
    }
}
