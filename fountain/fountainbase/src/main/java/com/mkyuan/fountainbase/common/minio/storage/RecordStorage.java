package com.mkyuan.fountainbase.common.minio.storage;

import java.io.IOException;

public interface RecordStorage {

	String upload(String group, String fileName, String contentType, byte[] fileData) throws IOException;

	byte[] download(String filePath) throws IOException;
	
	void delete(String filePath) throws IOException;

}