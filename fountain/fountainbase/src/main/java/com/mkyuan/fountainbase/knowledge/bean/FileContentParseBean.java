package com.mkyuan.fountainbase.knowledge.bean;

import java.io.Serializable;

public class FileContentParseBean implements Serializable {

    private int type=0; //2-excel
    private String fileContent="";
    private String fileId="0l";

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }


}
