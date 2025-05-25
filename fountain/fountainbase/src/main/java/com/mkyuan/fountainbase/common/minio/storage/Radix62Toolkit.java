package com.mkyuan.fountainbase.common.minio.storage;

import org.apache.commons.lang3.StringUtils;

//断链编码关键类
// @formatter:off
/**
 * 格式如下:<br/>
 * ---------------------------------------
 * integer   |   companyId  |   value   
 * ---------------------------------------
 * 10radix   |              -            
 * --------------------------------------- → {companyId的长度:10进制}{[companyId:62进制][真实编码:62进制]}
 *    -      |          62radix          
 * ---------------------------------------
 *               3R30DP                  ↓
 * ---------------------------------------
 *     3     |          R30DP            ↓
 * ---------------------------------------
 *     3     |          101123456        ↓ 3:代表编码前三位为companyId
 * ---------------------------------------
 *     3     |    101       |   123456   ↓
 * ---------------------------------------
 */
//@formatter:on
public class Radix62Toolkit {

	// 62 进制种子
	private static final String SEEDS = "uGVE7cRgxazlHfNiq40pYJUjb2y5OwPQe9LoCWXktdAhK8n1mIsD3FrBvM6SZT";
	private static final long VALUE_LIMIT = 99999999999999L;// 最大的序列码值 ,99万亿

	private static long toRadixLong(String value) {
		long num = 0;
		int index;
		for (int i = 0; i < value.length(); i++) {
			index = SEEDS.indexOf(value.charAt(i));
			num += (long) (index * (Math.pow(62, value.length() - i - 1)));
		}
		return num;
	}

	private static String toRadixString(long value) {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int v = (int) (value % 62);
			sb.append(SEEDS.charAt(v));
			value = value / 62;
			if (value == 0) {
				break;
			}
		}
		return sb.reverse().toString();
	}

	public static RadixLong decode(String value) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException("参数有误");
		}
		//int companyLength = ((int) value.charAt(0)) - 47;
		try {
			long target = toRadixLong(value);
			String targetString = String.valueOf(target);
			//String companyString = targetString.substring(0, companyLength - 1);
			//String valueString = targetString.substring(companyLength - 1);

			RadixLong result = new RadixLong();
			result.setValue(Long.parseLong(targetString));
			//result.setCompany(Integer.parseInt(companyString));
			return result;
		} catch (Exception e) {
			throw new IllegalArgumentException("解码出错: "+e.getMessage(),e);
		}
	}

	public static String encode(long value) {
		if (value > VALUE_LIMIT) {
			// 超出最大限制
			throw new IllegalArgumentException("value超出最大限制");
		}
		return toRadixString(value);

	}

	public static class RadixLong {
		private int company;
		private long value;

		public int getCompany() {
			return company;
		}

		public void setCompany(int company) {
			this.company = company;
		}

		public long getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}

	public static void main(String[] args) {
		System.err.println(toRadixString(1793570460275027969L));
//		String value = encode(101, 123456L);
//		System.err.println(value);
//		RadixLong rl = decode(value);
//		System.err.println(rl.getCompany());
//		System.err.println(rl.getValue());
//		System.out.println(toRadixLong("R30DP"));
	}

}