package com.mkyuan.fountainbase.common.minio.storage;

import org.apache.commons.lang3.StringUtils;

public enum RecordStorageChannel {

	MinIO("minio", "MinIO");

	private final String value;
	private final String label;

	RecordStorageChannel(String value, String label) {
		this.value = value;
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public String getLabel() {
		return label;
	}

	public static RecordStorageChannel parseByValue(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		for (RecordStorageChannel channel : RecordStorageChannel.values()) {
			if (StringUtils.equals(channel.getValue(), value)) {
				return channel;
			}
		}
		return null;
	}

}