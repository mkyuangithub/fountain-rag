package com.mkyuan.fountainbase.office.support;

import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;

public abstract class AbstractFileResolverForExcel extends AbstractFileResolver {

    protected final int maxRow;
    protected final int maxCol;

    protected AbstractFileResolverForExcel(int maxRow, int maxCol, int maxReadLength, boolean readImg, String apiKey, OkHttpHelper okHttpHelper,String ocrUrl) {
        super(maxReadLength,readImg,apiKey,okHttpHelper,ocrUrl);
        this.maxRow = maxRow;
        this.maxCol = maxCol;
    }

}
