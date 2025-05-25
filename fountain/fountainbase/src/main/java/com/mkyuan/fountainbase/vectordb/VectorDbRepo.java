package com.mkyuan.fountainbase.vectordb;

import com.mkyuan.fountainbase.knowledge.bean.VectorIndexRequest;
import com.mkyuan.fountainbase.vectordb.bean.addpoint.PointsWrapper;
import com.mkyuan.fountainbase.vectordb.bean.query.Filter;
import com.mkyuan.fountainbase.vectordb.bean.query.QDRantQueryResult;

import java.util.List;

public interface VectorDbRepo {
    public void createRepo(String repoName)throws Exception;
    public void deleteRepo(String repoName)throws Exception;

    public List<Float> getEmbedding(String inputText) throws Exception;

    public List<Float> getVlEmbedding(String inputText,String imgData) throws Exception;
    public void addBatchEmbeddingItemIntoVectorDB(PointsWrapper points, String url) throws Exception;
    public List<QDRantQueryResult> queryWithVector(String url, List<Float> vector, Filter queryFilter, int top) ;
    public void makeIndex(VectorIndexRequest vectorIndexRequest,String indexAddr);
    public void deletePoints(String collectionName,List<Long>pointIds)throws Exception;
    public void deleteRepoVl(String repoName)throws Exception;
    public void deleteVlPoints(String collectionName,List<Long>pointIds)throws Exception;
}
