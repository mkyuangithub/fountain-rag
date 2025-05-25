package com.mkyuan.fountainbase.office.support;

import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;

public abstract class AbstractFileResolverForWord extends AbstractFileResolver{

    protected AbstractFileResolverForWord(int maxReadLength, boolean readImg, String apiKey, OkHttpHelper okHttpHelper,String ocrUrl) {
        super(maxReadLength,readImg,apiKey,okHttpHelper,ocrUrl);

    }
    
}
