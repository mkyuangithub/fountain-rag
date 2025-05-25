from flask import Flask, request, jsonify
import os
import time
import logging
from logging.handlers import RotatingFileHandler
from FlagEmbedding import FlagReranker
from FlagEmbedding import FlagModel
import torch
from transformers import AutoModel
import base64
from io import BytesIO
from PIL import Image

app = Flask(__name__)

# 全局 reranker 对象
global_reranker = FlagReranker('BAAI/bge-reranker-large', use_fp16=False)
model = FlagModel('BAAI/bge-large-zh-v1.5',
                  query_instruction_for_retrieval="",
                  use_fp16=False)


# 初始化BGE-VL模型
VL_MODEL_NAME = "BAAI/BGE-VL-large"
VL_MODEL = AutoModel.from_pretrained(VL_MODEL_NAME, trust_remote_code=True)
VL_MODEL.set_processor(VL_MODEL_NAME)
VL_MODEL.eval()
if torch.cuda.is_available():
    VL_MODEL = VL_MODEL.to("cuda")


# 配置日志
if not os.path.exists('logs'):
    os.makedirs('logs')
    
file_handler = RotatingFileHandler('logs/FountainBGE.log', maxBytes=10240000, backupCount=10, encoding='utf-8')
file_handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s: %(message)s [in %(pathname)s:%(lineno)d]'
))
file_handler.setLevel(logging.INFO)
app.logger.addHandler(file_handler)
app.logger.setLevel(logging.INFO)


@app.route('/getBGEVLEmbedding', methods=['POST'])
def get_vl_vector():
    start_time = time.time()
    
    # 验证API Key
    api_key = request.headers.get('apiKey')
    if api_key != os.environ.get('X_API_KEY'):
        error_msg = 'Invalid API Key'
        app.logger.error(error_msg)
        return jsonify({
            'data': {
                'embeddings': [],
                'spentTime': time.time() - start_time
            }
        }), 403
    
    try:
        # 获取请求数据
        data = request.get_json()
        if not data:
            return jsonify({'error': '无效的请求数据'}), 400
            
        img_data = data.get('image', None)
        text = data.get('text', "")
        
        if not img_data:
            return jsonify({'error': '请提供图片的base64数据'}), 400
        
        # 确保Base64数据不包含前缀
        if ',' in img_data:
            img_data = img_data.split(',')[1]
            
        app.logger.info(f"接收到的图片Base64数据长度: {len(img_data)}")
        app.logger.info(f"接收到的文本: {text}")
        
        # 解码Base64图片数据
        try:
            img_bytes = base64.b64decode(img_data)
            app.logger.info(f"解码后的图片字节数: {len(img_bytes)}")
        except Exception as e:
            app.logger.error(f"Base64解码失败: {str(e)}")
            return jsonify({'error': 'Base64解码失败，请检查图片数据格式'}), 400
        
        # 使用临时文件
        import tempfile
        
        # 创建临时文件
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix='.jpg')
        temp_file_path = temp_file.name
        temp_file.close()  # 先关闭以便后续写入
        
        try:
            # 将解码后的图片数据写入临时文件
            with open(temp_file_path, 'wb') as f:
                f.write(img_bytes)
            
            app.logger.info(f"图片已保存到临时文件: {temp_file_path}")
            
            # 验证图片文件是否有效
            try:
                with Image.open(temp_file_path) as img:
                    app.logger.info(f"图片格式: {img.format}, 大小: {img.size}")
            except Exception as e:
                app.logger.error(f"图片数据无效: {str(e)}")
                return jsonify({'error': '图片数据无效或格式不支持'}), 400
            
            # 使用BGE-VL模型生成向量
            with torch.no_grad():
                if text:
                    vector = VL_MODEL.encode(
                        images=temp_file_path,
                        text=text
                    )
                else:
                    vector = VL_MODEL.encode(
                        images=temp_file_path
                    )
                
                vec_list = vector.tolist()
                
                return jsonify({
                    'data': {
                        'embeddings': vec_list,
                        'spentTime': time.time() - start_time
                    }
                }), 200
        except Exception as e:
            app.logger.error(f"模型推理时出错: {str(e)}")
            app.logger.exception(e)
            return jsonify({
                'data': {
                    'embeddings': [],
                    'spentTime': time.time() - start_time
                }
            }), 500
        finally:
            # 无论成功或失败，都删除临时文件
            if os.path.exists(temp_file_path):
                os.unlink(temp_file_path)
                app.logger.info(f"已删除临时文件: {temp_file_path}")
                
    except Exception as e:
        error_msg = f'处理请求时出错: {str(e)}'
        app.logger.error(error_msg)
        app.logger.exception(e)
        return jsonify({
            'data': {
                'embeddings': [],
                'spentTime': time.time() - start_time
            }
        }), 500

@app.route('/getBGEEmbedding', methods=['POST'])
def get_vector():
    start_time = time.time()
    error_messages = []  # 用于收集错误信息
    api_key = request.headers.get('apiKey')
    if api_key != os.environ.get('X_API_KEY'):
            error_msg = 'Invalid API Key'
            app.logger.error(error_msg)
            error_messages.append(error_msg)
            return jsonify({
                'data': {
                    'embeddings': [],
                    'spentTime': time.time() - start_time
                }
            }), 403
        
    #if not api_key or api_key != os.environ.get('LOCAL_MODEL_API_KEY'):
    #    return jsonify({'error': '无效的登录'}), 403
    inputText = request.json.get('input', None)
    if not inputText:
        return jsonify({'error': '请上传有效的文本'}), 400
    else:
        embedding = model.encode([inputText])
        vec_list = embedding.tolist()
        return jsonify({
            'data': {
                'embeddings': vec_list,
                'spentTime': time.time() - start_time
            }
        }), 200

@app.route('/rank', methods=['POST'])
def rank():
    start_time = time.time()
    error_messages = []  # 用于收集错误信息
    
    try:
        # 验证 API Key
        api_key = request.headers.get('apiKey')
        if api_key != os.environ.get('X_API_KEY'):
            error_msg = 'Invalid API Key'
            app.logger.error(error_msg)
            error_messages.append(error_msg)
            return jsonify({
                'data': {
                    'resultList': [],
                    'spentTime': time.time() - start_time,
                    'failReason': ' | '.join(error_messages)
                }
            }), 403
        
        # 获取请求数据
        data = request.get_json()
        user_input = data.get('userInput', '')
        knowledge_list = data.get('knowledgeList', [])
        
        # 如果知识列表为空，返回空结果
        if not knowledge_list:
            return jsonify({
                'data': {
                    'resultList': [],
                    'spentTime': time.time() - start_time,
                    'failReason': ''
                }
            })
        
        # 处理每条知识并计算相似度
        result_list = []
        potential_records = []  # 新增一个列表存储分数小于0的记录
        for idx, item in enumerate(knowledge_list):
            try:
                score = global_reranker.compute_score([user_input, item['fileContent']])[0]
                
                # 构建通用的结果项
                result_item = {
                    'id': item.get('id', ''),
                    'pointId': item.get('pointId', 0),
                    'fileName': item.get('fileName', ''),
                    'fileContent': item.get('fileContent', ''),
                    'score': float(score)
                }
                
                # 根据分数分别添加到不同列表
                if score >=-1.8:
                    result_list.append(result_item)
                else:
                    potential_records.append(result_item)
            except Exception as e:
                error_msg = f'Error processing item {idx}: {str(e)}'
                app.logger.error(error_msg)
                error_messages.append(error_msg)
                continue
        
        # 按 score 降序排序
        result_list.sort(key=lambda x: x['score'], reverse=True)
        
        return jsonify({
            'data': {
                'resultList': result_list,
                'potentialRecords': potential_records,
                'spentTime': time.time() - start_time,
                'failReason': ' | '.join(error_messages) if error_messages else ''
            }
        })
        
    except Exception as e:
        error_msg = f'Error in rank API: {str(e)}'
        app.logger.error(error_msg)
        error_messages.append(error_msg)
        return jsonify({
            'data': {
                'resultList': [],
                'spentTime': time.time() - start_time,
                'failReason': ' | '.join(error_messages)
            }
        })

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)