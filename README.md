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
本项目是一个基于Spring boot, vue3技术的企业级高级RAG引擎。
在引擎内使用了最前沿的Rewrite、Retrieve、Read设计，使得本引擎可以做到几乎0幻觉。
<img>
https://github.com/mkyuangithub/fountain-rag/blob/main/img/architecture_overview.png
</img>
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
- 启动nacos、mongodb、elastic search、redis、minio、qdrant、dify
- 在intellij里直接导入fountain-rag中的fountain项目。该项目是fountain-base->fountain-gateway这样的结构的。其中fountain-gateway是spring cloud api gateway2.0用于对外暴中接口的，把这两个项目都启动起来。
- 在vs code里导入fountain-web项目：npm install后，记得fountain-web里的.env，.env.dev， .env.pro里的P_SECURE_KEY要和nacos里的secretKey的值必须一致，因为前后端对于一些密码数据传输用的是AESWithPBE来加密的，共用一个secret key的。
- 启动后：vscode->fountain-gateway->fountain-base这样通讯的。这样的结构是为fountain的后端引擎docker化作预留的云原生架构用。
- 如果导入了mongodb的库，那么默认登录用户名和密码为：admin/111111
