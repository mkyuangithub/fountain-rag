package com.mkyuan.fountainbase.vectordb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VectorDbRepoFactory {
    private final Map<Integer, VectorDbRepo> vectorDbRepoMap = new ConcurrentHashMap<>();
    @Autowired
    public VectorDbRepoFactory(MxbaiEmbeddedRepo mxbaiEmbeddedRepo, BgeEmbeddingRepo bgeEmbeddingRepo,BgeVLEmbeddingRepo bgeVLEmbeddingRepo){
        vectorDbRepoMap.put(1,mxbaiEmbeddedRepo);
        vectorDbRepoMap.put(2,bgeEmbeddingRepo);
        vectorDbRepoMap.put(3,bgeVLEmbeddingRepo);
    }
    public VectorDbRepo getVectorDbRepo(int vectorDbType) {
        VectorDbRepo vectorDbRepo = vectorDbRepoMap.get(vectorDbType);
        if (vectorDbRepo == null) {
            throw new IllegalArgumentException("Unsupported vectordb type: " + vectorDbType);
        }
        return vectorDbRepo;
    }

}
