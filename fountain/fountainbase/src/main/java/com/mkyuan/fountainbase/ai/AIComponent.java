package com.mkyuan.fountainbase.ai;

import com.mkyuan.fountainbase.ai.bean.AIModel;
import com.mkyuan.fountainbase.ai.bean.RequestPayload;

public interface AIComponent {

    public String jsonCall(AIModel aiModel, String promptStr)throws Exception;
    public String jsonCall(AIModel aiModel, String promptStr,double temperature)throws Exception;
    public String jsonCall(AIModel aiModel, RequestPayload requestPayload)throws Exception;
    public  CompleteResponse doRequestWithStream(String userName, AIModel aiModel,RequestPayload request, CompleteCallback callback);
}
