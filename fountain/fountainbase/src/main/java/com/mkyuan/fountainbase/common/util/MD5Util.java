package com.mkyuan.fountainbase.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.security.SecureRandom;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.DigestUtils;
public class MD5Util {
    private static final SecureRandom secureRandom = new SecureRandom();
    private final static Logger logger = LogManager.getLogger(MD5Util.class);
    public static String generateToken(String loginId, String signature) {
        // 生成16位随机盐值
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        String saltStr = Base64.getEncoder().encodeToString(salt);

        // 获取当前时间戳
        long timestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

        // 组合所有元素
        // 使用commons-lang2的StringUtils.join
        String tokenSource = StringUtils.join(new String[]{
                loginId,
                signature,
                String.valueOf(timestamp),
                saltStr
        }, ":");  // 使用冒号作为分隔符

        // 使用MD5进行加密（也可以选择其他加密算法如SHA-256）
        String myToken = DigestUtils.md5DigestAsHex(tokenSource.getBytes());
        return myToken;
    }
    public static String getMD5(String input)  {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error(">>>>>>getMD5 error->{}",e.getMessage());
            return "";
        }
    }
    public static String getMD5(String input, String loginId) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 先更新loginId
            md.update(loginId.getBytes(StandardCharsets.UTF_8));
            // 再更新input
            md.update(input.getBytes(StandardCharsets.UTF_8));

            byte[] messageDigest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getMD5(byte[] data, String loginId) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 先更新loginId的字节
            md.update(loginId.getBytes(StandardCharsets.UTF_8));
            // 再更新文件数据
            md.update(data);

            byte[] hash = md.digest();

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(">>>>>>MD5计算失败", e);
        }
    }
    public static String getMD5(byte[] data) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(">>>>>>MD5计算失败", e);
        }
    }

    public static boolean isValid(String md5a, String md5b) {
        return md5a.equals(md5b);
    }
}
