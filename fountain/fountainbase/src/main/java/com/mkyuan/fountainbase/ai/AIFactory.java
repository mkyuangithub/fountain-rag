package com.mkyuan.fountainbase.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AIFactory {
    private final Map<Integer, AIComponent> aiComponentMap = new ConcurrentHashMap<>();

    @Autowired
    public AIFactory(LocalDeepSeekComponent localDeepSeekComponent, SaasDeepSeekComponent saasDeepSeekComponent,
                     Gpt4ominiComponent gpt4ominiComponent, Gpt4oComponent gpt4oComponent,
                     QWenComponent qWenComponent,LocalPhi4Component localPhi4Component,LocalGemma3Component localGemma3Component, QWenComponent qwenSlaveComponent) {
        aiComponentMap.put(1, localDeepSeekComponent);
        aiComponentMap.put(2, saasDeepSeekComponent);
        aiComponentMap.put(3, gpt4ominiComponent);
        aiComponentMap.put(4, gpt4oComponent);
        aiComponentMap.put(5, qWenComponent);
        aiComponentMap.put(6,localPhi4Component);
        aiComponentMap.put(7,localGemma3Component);
        aiComponentMap.put(8,qwenSlaveComponent);
    }

    public AIComponent getAIComponent(int aiType) {
        AIComponent aiComponent = aiComponentMap.get(aiType);
        if (aiComponent == null) {
            throw new IllegalArgumentException("fountain AI Model-invoke->Unsupported ai type: " + aiType);
        }        return aiComponent;
    }
}
