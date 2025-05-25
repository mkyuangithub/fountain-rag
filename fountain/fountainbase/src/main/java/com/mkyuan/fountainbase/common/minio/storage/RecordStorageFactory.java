package com.mkyuan.fountainbase.common.minio.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class RecordStorageFactory {

	private static final RecordStorageChannel DEFAULT_CHANNEL = RecordStorageChannel.MinIO;

	@Autowired
	private ApplicationContext applicationContext;
	
	public RecordStorageChannel getDefaultChannel() {
		return DEFAULT_CHANNEL;
	}

	public RecordStorage getStorage(RecordStorageChannel channel) {
		if (null == channel) {
			channel = DEFAULT_CHANNEL;
		}
		String beanName = String.format("RecordStorage_%s", channel.getValue());
		RecordStorage recordStorage = this.applicationContext.getBean(beanName, RecordStorage.class);
		return recordStorage;
	}

}