package com.mkyuan.fountainbase.common.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jodd.util.StringUtil;

public class EncryptUtil {
	private final static Logger logger = LoggerFactory.getLogger(EncryptUtil.class);

	/**
	 * 解密
	 *
	 */
	public static String decrypt(String encryptTxt, String secretKey) throws Exception {
		try {
			byte[] iv = new byte[16]; // 创建一个全0的IV
			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
			if (StringUtil.isBlank(secretKey)) {
				logger.error(">>>>>>解密操作时->密钥不得为空");
				return null;
			}
			byte[] keyBytes = secretKey.getBytes("UTF-8");
			SecretKeySpec skey = new SecretKeySpec(keyBytes, "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skey, ivParameterSpec);

			byte[] decodeBase64 = Base64.getDecoder().decode(encryptTxt);
			byte[] original = cipher.doFinal(decodeBase64);
			return new String(original, "UTF-8");

		} catch (Exception e) {
			//logger.info(">>>>>>解密出错: {}", e.getMessage(), e);
			throw new Exception(">>>>>>解密出错: " + e.getMessage(), e);
		}
	}
	public static String decrypt_safeencode(String encryptTxt, String secretKey) throws Exception {
		try {
			byte[] iv = new byte[16]; // 创建一个全0的IV
			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

			if (StringUtil.isBlank(secretKey)) {
				logger.error(">>>>>>解密操作时->密钥不得为空");
				return null;
			}

			byte[] keyBytes = secretKey.getBytes("UTF-8");
			SecretKeySpec skey = new SecretKeySpec(keyBytes, "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skey, ivParameterSpec);

			// 修改这里：使用 URL 安全的 Base64 解码器
			byte[] decodeBase64 = Base64.getUrlDecoder().decode(encryptTxt);
			byte[] original = cipher.doFinal(decodeBase64);
			return new String(original, "UTF-8");

		} catch (Exception e) {
			throw new Exception(">>>>>>解密出错: " + e.getMessage(), e);
		}
	}
	/**
	 * 加密
	 * 
	 * @param plainTxt
	 * @param secretKey
	 * @return
	 */
	public static String encrypt(String plainTxt, String secretKey) throws Exception {
		try {
			if (StringUtil.isBlank(secretKey)) {
				logger.error(">>>>>>加密操作时->密钥不得为空");
				return null;
			}
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

			byte[] iv = new byte[16]; // 创建一个全0的IV
			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
			byte[] encrypted = cipher.doFinal(plainTxt.getBytes());
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			//logger.info(">>>>>>加密出错: {}", e.getMessage(), e);
			throw new Exception(">>>>>>加密出错: " + e.getMessage(), e);
		}
	}

	public static void main(String[] args) throws Exception {
		String txt = "%71rVZ,<`]02";
		String encryptTxt = encrypt(txt, "1234567890123456");
		String decryptTxt = decrypt("Mkv1hFVdCZG6TeVJLMjmIA==", "1234567890123456");
		System.out.println(">>>>>>加密后的文本->" + encryptTxt);
		System.out.println(">>>>>>解密后的文本->" + decryptTxt);

	}
}
