package com.mkyuan.fountainbase.vectordb.bean.pointinfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Payload implements Serializable {

    private String bizId = "";
    private String prodName = "";
    private String ledgerTypeId = "-1";
    private String fileName = "";
    private String fileId = "-1";
    private String ledgerTypeText = "";

    private double unitPrice=0.0;
    private String category="";
    private List<String> prodLabel=new ArrayList<>();

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getProdLabel() {
        return prodLabel;
    }

    public void setProdLabel(List<String> prodLabel) {
        this.prodLabel = prodLabel;
    }

    public String getLedgerTypeId() {
        return ledgerTypeId;
    }

    public void setLedgerTypeId(String ledgerTypeId) {
        this.ledgerTypeId = ledgerTypeId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getLedgerTypeText() {
        return ledgerTypeText;
    }

    public void setLedgerTypeText(String ledgerTypeText) {
        this.ledgerTypeText = ledgerTypeText;
    }

    public List<String> getLedgerCols() {
        return ledgerCols;
    }

    public void setLedgerCols(List<String> ledgerCols) {
        this.ledgerCols = ledgerCols;
    }

    public List<String> getColName() {
        return colName;
    }

    public void setColName(List<String> colName) {
        this.colName = colName;
    }

    private List<String> ledgerCols = new ArrayList<>();
    private List<String> colName = new ArrayList<>();

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    private String content = "";
    private List<String> keyword = new ArrayList<>();

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeyword() {
        return keyword;
    }

    public void setKeyword(List<String> keyword) {
        this.keyword = keyword;
    }

}
