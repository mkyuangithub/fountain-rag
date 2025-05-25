import os
import logging
from logging.handlers import RotatingFileHandler
import numpy as np
import cv2
from flask import Flask, request, jsonify
from paddleocr import PaddleOCR
from werkzeug.utils import secure_filename

# 创建 Flask 应用
app = Flask(__name__)

# 配置 API Key
API_KEY = os.environ.get('X_API_KEY')  # 设置默认值或从环境变量获取

# 初始化 PaddleOCR
ocr = PaddleOCR(use_angle_cls=True, lang="ch")

# 配置日志
if not os.path.exists('logs'):
    os.makedirs('logs')

file_handler = RotatingFileHandler('logs/paddleocr.log', maxBytes=10240000, backupCount=10)
file_handler.setFormatter(logging.Formatter(
    '%(asctime)s %(levelname)s: %(message)s [in %(pathname)s:%(lineno)d]'
))
file_handler.setLevel(logging.INFO)
app.logger.addHandler(file_handler)
app.logger.setLevel(logging.INFO)

# 验证 API Key 的装饰器
def require_api_key(f):
    def decorated(*args, **kwargs):
        api_key = request.headers.get('apiKey')
        if api_key != API_KEY:
            app.logger.error('无效的 API Key')
            return jsonify({'error': '无效的 API Key'}), 401
        return f(*args, **kwargs)
    decorated.__name__ = f.__name__
    return decorated

# 允许的图片格式
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/ocr', methods=['POST'])
@require_api_key
def ocr_api():
    try:
        # 检查是否有文件上传
        if 'image' not in request.files:
            app.logger.error('没有上传图片文件')
            return jsonify({'error': '没有上传图片文件'}), 400
        
        file = request.files['image']
        
        # 检查文件名是否为空
        if file.filename == '':
            app.logger.error('未选择图片文件')
            return jsonify({'error': '未选择图片文件'}), 400
        
        # 检查文件类型
        if not allowed_file(file.filename):
            app.logger.error(f'不支持的文件类型: {file.filename}')
            return jsonify({'error': '不支持的文件类型'}), 400
        
        # 读取图片为 numpy 数组
        file_bytes = file.read()
        nparr = np.frombuffer(file_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        # 获取请求参数
        cls = request.form.get('cls', 'true').lower() == 'true'
        
        # 执行 OCR
        app.logger.info(f'开始处理图片: {secure_filename(file.filename)}')
        result = ocr.ocr(img, cls=cls)
        
        # 处理结果
        ocr_results = []
        for idx in range(len(result)):
            res = result[idx]
            for line in res:
                ocr_results.append({
                    'text': line[1][0],
                    'confidence': float(line[1][1]),
                    'position': line[0]
                })
        
        app.logger.info(f'成功处理图片: {secure_filename(file.filename)}')
        return jsonify({'results': ocr_results})
    
    except Exception as e:
        app.logger.error(f'处理图片时出错: {str(e)}')
        return jsonify({'error': f'处理图片时出错: {str(e)}'}), 500

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5001, debug=False)