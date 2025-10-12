package com.ai.intelligentcalendarandconflictdetectionassistant.advisor;

import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;

import java.util.Map;


public class LoggingAdvisor implements RequestResponseAdvisor {
    //打印日志拦截器
    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object>context){
        System.out.println("Request: " + request);
        return request;
    }
}
