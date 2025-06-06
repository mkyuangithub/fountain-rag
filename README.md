# fountain-rag
A Java Spring Enterprise RAG Engine
## Language

- [English](#english)
- [中文](#中文)

---

### English
will update later

---

### 中文
#### 介绍
本项目是一个基于Spring boot, vue3技术的企业级高级RAG引擎。它使用了最前沿的RAG技术：Rewrite、Retrieve、Read设计，使得本引擎可以做到几乎0幻觉并支持多模态的企业级RAG应用，可以作为企业知识中台的基本底座。
它即支持云原生集群式布署也可支持单机布署。  
- 云原生
- spring boot微服务架构
- 支持txt, doc, docx, xlsx, xls, pdf, csv格式的文本上传与chunk
- 上传和chunk时，引擎支持AI提练成Q&A, AI 提练成完整一段, AI自动给数据打标答, AI自动关联段落与段落, AI自动关联上页与下页内容
- 支持解读word和pdf里的图片
- 回答时支持“以图搜图”模型（用的是bge-vl-embedding)
- 集成dify使用了并行流以应对诸如：数据富文本（图文并茂）在LLM回答后以并行chat flow的形式在本地关联主数据渲染
- 己支持国际化i18n
- Rewrite处以及LLM最后促裁节点集成的是dify（dify dsl文件已置于工程fountain/install/dify内）
##### Fountain引擎设计思路
![image](https://github.com/mkyuangithub/fountain-rag/blob/main/img/architecture-overview-1.jpg)
##### Fountain引擎和一般的RAG引擎的区别
![image](https://github.com/mkyuangithub/fountain-rag/blob/main/img/architecture-overview-2.jpg)
##### Fountain引擎拥有着极高的召回率
![image](https://github.com/mkyuangithub/fountain-rag/blob/main/img/architecture-overview-3.jpg)
##### Fountain Engine Architecture Design
![image](https://github.com/mkyuangithub/fountain-rag/blob/main/img/architecture-overview-4.jpg)
#### 使用技术
##### 后端：
- 使用的是spring 2.4.2 + spring cloud2.0 使用的rag引擎
- 引擎中的rewrite和聊天用的是和dify集成，项目内提供了dify的dsl
- rerank，embedding算法使用的是bge large, 使用的是python flask(位于 fountain/install/python目录内
- 配置中心使用的是：nacos 2.0.2
- elastic search: 7.9(目前后端spring boot不支持elastic search8.0）
- 向量库：qdrant 最新版
- 引擎存储：mongodb-4.2.1
- 缓存：redis-6.2.17
- 分布式文件存储：minio-2025-02-07 go1.23.6 windows/amd64
##### 前端技术
- vue3
- ant design
- javascript setup语法
#### 安装指南
##### nacos
- 请安装naocs 2.0.2
- 安装后导入fountain/install/nacos里的DEFAULT_GROUP中的配置，内含spring boot工程用于连接mongodb、redis、elastic search、minio的基本配置
- nacos里相关api key，secret等都用的是系统环境变量写法，可以自定义
##### mongodb
- 请安装4.2.1版本，安装后导入：fountain/install/mongo-script里的建库语句以及相关数据
##### es
- 请安装7.9，本版本只支持es7.9，不支持高于7.9，这是spring boot 2.4.2版本特性使然。安装完后无需做任何数据导入工作
##### redis
- 支持7.0版本的redis，开发项目时用的是:6.2.17，安装后无数据导入工作，只要和nacos里的配置对照着设置即可。
##### minio
- 请安装minio-2025-02-07 go1.23.6 windows/amd64，安装后可以对照着naocs里的设置即可
- 注意了：minio的管理员用户名和密码定义在系统环境变量
##### qdrant安装
- 安装
```
docker pull qdrant/qdrant
```
- 配置（必须要配置api key）
一个典型的标准标准 qdrant_config.yaml文件，内容如下
```
storage:
  # Specify where you want to store snapshots.
  snapshots_path: ./snapshots
service:
  #qdrant api-key
  api_key: ltdo0a6m9h-gh-af-eg-cf-ccand!093
```
- 启动qdrant时可以如下启动
```
docker run -p 6333:6333 \
-v /datadrive01/qdrant/data:/qdrant/storage \
-v /datadrive01/qdrant/snapshots:/qdrant/snapshots \
-v /datadrive01/qdrant/config/qdrant_config.yaml:/qdrant/config/production.yaml \
qdrant/qdrant &
```
- windows下你也可以使用docker desktop
- 你也可以使用qdrant的编译安装
- 安装后无需导入任何数据，在引擎的web界面内在创建知识库时会自动创建的
##### dify集成
- 自行安装dify 1.3.0或者以下（目前最新为1.4.0）
- 引擎内的聊天功能使用的rewrite步骤和最终AI聊天显示用的是dify中的流程。
- rewrite: dify工作流，提供了2个rewrite和2个chatflow，位于fountain/install/dify中，均可导入
#### 项目启动
![image](https://github.com/mkyuangithub/fountain-rag/blob/main/img/data-flow-1.jpg)  
- 启动nacos、mongodb、elastic search、redis、minio、qdrant、dify
- 在intellij里直接导入fountain-rag中的fountain项目。该项目是fountain-base->fountain-gateway这样的结构的。其中fountain-gateway是spring cloud api gateway2.0用于对外暴中接口的，把这两个项目都启动起来。
- 在vs code里导入fountain-web项目：npm install后，记得fountain-web里的.env，.env.dev， .env.pro里的P_SECURE_KEY要和nacos里的secretKey的值必须一致，因为前后端对于一些密码数据传输用的是AESWithPBE来加密的，共用一个secret key的。
- 启动后：vscode->fountain-gateway->fountain-base这样通讯的。这样的结构是为fountain的后端引擎docker化作预留的云原生架构用。
- 如果导入了mongodb的库，那么默认登录用户名和密码为：admin/111111
##### python服务为flask服务
实现了：paddle-ocr, bge-reranker-large, bge-vl-large, bge-rerank-large，启动文件在：/fountain/install/python。
- 安装必要依赖
```
pip install -U FlagEmbedding
pip install  transformers
python3 -m pip install paddlepaddle==2.6.0 -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install paddlepaddle
pip install paddlenlp
pip install paddleocr==3.0.0
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```
- python flask的启动与运行  
可以在cursor里直接启动：  
![image](https://github.com/mkyuangithub/fountain-rag/blob/main/img/python-service-1.jpg)  
也可以使用生产级别的gunicorn启动如：  
```
-- gunicorn -w 4 -b 0.0.0.0:5000 FountainBGE:app
```
整体项目运行的流程为：vue3->fountain-gateway->fountain-base->FountainBGE.py(包括FountainOcr.py)。  
你可以把这两个文件自己合并成一个flask，不过一般在生产环境最佳实践为：  
把FountainBGE.py运行在：5000端口  
把FountainOcr.py运行在: 5001端口  
spring boot->flask service之间需要配置系统（用户）环境变量：X_API_KEY，因为spring boot和python flask间通讯会使用http header来匹配这个 X_API_KEY。具体见nacos里的配置  

#### 如何使用
请看https://github.com/mkyuangithub/fountain-rag/wiki
