package com.mkyuan.fountainbase.office;

import java.io.IOException;

/**
 * 文件解析器
 */
public interface FileResolver {

    FileBean resolve(String fileName, byte[] fileData) throws IOException;


}
