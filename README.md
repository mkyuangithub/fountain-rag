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
#### 使用技术
##### 后端：
- 使用的是spring 2.4.2 + spring cloud2.0 使用的rag引擎
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

