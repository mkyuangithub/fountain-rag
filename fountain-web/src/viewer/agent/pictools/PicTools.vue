<template>
  <div class="main">


    <div class="workspace">
      <div style="width: 70%;height: 100%; border-right: 1px solid #BFBFBF; overflow-y: auto;" @paste="handlePasteImg">
        <div v-if="hasImage" style="display: flex; justify-content: flex-end; padding: 0; margin-top: 5px; width:100%;">
          <a-button type="primary" style="margin-right: 10px; background-color:#A642F8;" @click="handleRestorePic"
            :disabled="processing">
            <template #icon>
              <UndoOutlined />
            </template>
            还原
          </a-button>
          <a-button type="primary" style="margin-right: 10px; background-color:#A642F8;" @click="handleClearPic"
            :disabled="processing">
            <template #icon>
              <DeleteOutlined />
            </template>
            清除
          </a-button>
        </div>
        <!--创建一个隐式的文件上传框-->
        <input type="file" ref="fileInput" @change="onFileSelected" accept="image/*" style="display: none" />
        <div
          style="margin: 0; width: 70%; height: 70%; margin-top: 20px; margin-left: 20px; margin-right: 10px; display: flex; flex-direction: column; justify-content: center; align-items: center;">
          <div v-if="!hasImage"
            style="margin:0; width: 100%; height: 100%; border: 2px dashed white; border-radius: 20px; display: flex; flex-direction: column; justify-content: center; align-items: center;">
            <!-- 第一行：图片 -->
            <img src="/assets/images/image-icon-white.png" alt="Placeholder"
              style="max-width: 100px; margin-bottom: 10px;" />
            <!-- 第二行：文字 -->
            <div style="margin-bottom: 10px; color:greenyellow;">您也可以使用Ctrl C Ctrl V 直接上传图片</div>
            <!-- 第三行：文字 -->
            <div style="margin-bottom: 10px; color:white">支持5MB图片，PNG、JPG、JPEG格式</div>
            <!-- 第四行：按钮 -->
            <a-button type="primary" :disabled="isThinking" style="width: 300px; background-color: #A642F8;"
              @click="handlePicUpload">
              <template #icon>
                <PlusCircleOutlined />
              </template>
              上传图片
            </a-button>
          </div>
          <div v-else
            style="margin:0; width:100%; height: 100%; margin-left: 10px; margin-right: 10px; display: flex; flex-direction: column; justify-content: center; align-items: center; ">
            <!-- 显示图片的区域 -->
            <div ref="photoExp"
              style="padding:0; width:100%; height: 100%; position: relative; display: flex; justify-content: center; align-items: center;">
              <img ref="displayImage" :key="imageKey" :src="model.picUrl" alt="Uploaded Image"
                style="max-width: 100%; max-height: 100%;  z-index:1;" />
              <canvas v-show="type === 'removewatermark'" ref="canvasRef" :width="imageWidth" :height="imageHeight"
                :class="{ 'crosshair-cursor': shouldShowCrosshair }"
                style="position: absolute;  
                 top: 50%; 
                 left: 50%;
                 transform: translate(-50%, -50%); max-width: 100%; max-height:100%;   margin: 0; z-index:2; padding:0; " @mouseover="handleMouseOver"
                @mousemove="handleMouseMove" @mouseleave="handleMouseLeave" @mousedown="startDrawing"
                @mouseup="stopDrawing" @mouseout="stopDrawing" />
              <!-- 文本框拖动用-->
              <canvas v-show="type === 'puttextonpicture'" ref="canvasTextRef" :width="imageWidth" :height="imageHeight"
                style="position: absolute;  
                 top: 50%; 
                 left: 50%; 
                 transform: translate(-50%, -50%); max-width: 100%; max-height:100%;   margin: 0; border-radius:20px;  z-index:2; padding:0;" />
              <!--画笔光标-->
              <div v-show="showBrushCursor" class="brush-cursor" :style="{
                left: `${brushPosition.x}px`,
                top: `${brushPosition.y}px`,
                width: `${brushSize * 10}px`,
                height: `${brushSize * 10}px`,
                transform: 'translate(-50%, -50%)', // 添加这行来使光标居中
                borderRadius: '50%',
                border: '1px solid #000',
                backgroundColor: 'rgba(216,110,204,0.3)',
                position: 'absolute',
                pointerEvents: 'none',
                cursor: 'none',
                zIndex: '3',
              }">
              </div>
            </div>
            <!--大图下部图片修改历史显示区域-->
            <div style="display: flex; margin-top: 10px; padding:0;width: 100%; justify-content: center;">
              <div v-for="(image, index) in historyImageList" :key="index" @click="handleImageClick(index)" :style="{
                height: '90px',
                width: '90px',
                border: selectedHistoryIndex === index ? '2px solid white' : '1px solid #01CFFF',
                borderRadius: '8px',
                padding: '0',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                cursor: 'pointer',
                marginLeft: '10px',
                boxShadow: selectedIndex === index ? '0 0 5px #1890ff' : 'none'
              }">
                <div
                  style="width: 90%; height: 80%; display: flex; justify-content: center; margin-bottom: 3px; padding:0;">
                  <img :src="image.url" alt="Image" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                </div>
                <span style="font-size: 13px; margin-bottom: 5px; color: white">{{ image.label }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="hasImage"
          style="display: flex; justify-content: flex-end; padding: 0; margin-top: 20px; width:100%;">
          <a-button type="primary" style="margin-right: 10px; background-color:#A642F8;" @click="handlePicUpload"
            :disabled="processing">
            <template #icon>
              <CloudUploadOutlined />
            </template>
            上传
          </a-button>
          <a-button type="primary" style="margin-right: 10px; background-color:#A642F8;" @click="handleDownloadPic"
            :disabled="processing">
            <template #icon>
              <CloudDownloadOutlined />
            </template>
            下载
          </a-button>
        </div>
      </div>
      <div class="workspace-toolzone">
        <div style="margin-top: 10px; margin-left: 10px;">
          <span style="color:white">选择编辑方式</span>
        </div>
        <div style="display: flex;">
          <div @click="picToolSelected('removebg')" :style="[
            {
              marginTop: '20px',
              marginLeft: '10px',
              marginRight: '15px',
              width: '80px',
              height: '80px',
              backgroundColor: type === 'removebg' ? '#DBB2FC' : '#EAEAEA',
              border: '1px solid #555',
              borderRadius: '10px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
              cursor: 'pointer'
            }
          ]">
            <img src="/assets/images/remove-bg-1.png" alt="icon"
              style="width: 28px; height: 28px; margin-bottom: 5px;" />
            <span style="color: black; font-size: 13px;">移去背景</span>
          </div>
          <div @click="picToolSelected('removewatermark')" :style="[
            {
              marginTop: '20px',
              marginLeft: '10px',
              marginRight: '15px',
              width: '80px',
              height: '80px',
              backgroundColor: type === 'removewatermark' ? '#DBB2FC' : '#EAEAEA',
              border: '1px solid #555',
              borderRadius: '10px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
              cursor: 'pointer'
            }
          ]">
            <img src="/assets/images/erase-1.png" alt="icon" style="width: 24px; height: 24px; margin-bottom: 5px;" />
            <span style="color: black; font-size: 13px;">涂抹消除</span>
          </div>
        </div>
        <div v-if="type === 'removewatermark'"
          style="width: 100%; display: flex; justify-content: center; margin-top: 50px;">
          <div
            style="padding:0; width: 100%; margin-left: 10px; display: flex; justify-content: center; align-items: center;">
            <div style="width: 30%; color:white;">
              画笔大小：
            </div>
            <div style="width: 70%;">
              <a-slider class="white-slider" v-model:value="brushSize" :min="1" :max="10" :step="1"
                style="width: 100%;" />
            </div>
          </div>
        </div>

        <div v-if="type === 'removewatermark'"
          style="width: 100%; display: flex; justify-content: center; margin-top: 60px; font-size: 18px; color: white; align-items: center;">
          （可手动涂抹图片中的部位去水印）
        </div>
        <!-- 功能区提交按钮-->
        <div style="width: 100%; display: flex; justify-content: center; margin-top: 50px; margin-left: 10px;">
          <a-button type="primary" style="width: 300px; background-color:#A642F8;" @click="handlePicTool"
            :disabled="processing">
            <template #icon>
              <PictureOutlined />
            </template>
            立即生成
          </a-button>
        </div>
        <div v-if="processing" style="width: 100%; display: flex; justify-content: center; margin-top: 10px;">
          <img src="/assets/images/waiting-04.gif" style="width: 80px; height: auto;" />
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import authorization from "@/toolkit/authorization.js";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import { computed, onMounted, onUnmounted, ref, watch, nextTick, reactive } from "vue";
import { message, Modal } from "ant-design-vue";

import html2canvas from 'html2canvas';  // 确保已经安装并导入 html2canvas

import { isBlank, isLogin } from "@/toolkit/utils.js";
import PictureApi from "@/api/PictureApi.js";
import settings from "@/toolkit/settings.js"
const displayImage = ref(null);
const loading = ref(false);

const model = ref({
  picUrl: "",
});

// 添加选中状态变量，默认选中第一个
const selectedHistoryIndex = ref(0)

const type = ref('removebg') // 添加默认值



/*粘贴图片用*/
const picContent = ref('');
const imageWidth = ref(0);
const imageHeight = ref(0);

const hasImage = ref(false);
const imageSrc = ref(''); // 这里可以设置为上传后的图片路径

//画笔用开始
const brushSize = ref(4)
const showBrushCursor = ref(false)
const brushPosition = ref({ x: 0, y: 0 })
const isDrawing = ref(false)

const marks = reactive({
  6: '4',
  7: '5',
  8: '6',
  9: '7',
  10: '8',
  11: '9',
  12: '10'
})


//画笔用结束
// canvas引用开始
const ctxDrawing = ref(null)
// canvas引用结束


const onBackClick = () => {
  let params = route.params;
  router.push({
    name: "Center",
    params: {
      channel: params.channel
    }
  });
}

const handleImageClick = (index) => {
  //console.log(`Image ${index} clicked`);
  model.value.picUrl = historyImageList.value[index].url;
  selectedHistoryIndex.value = index;
}

const clickOriginalPic = () => {
  model.value.picUrl = originalUrl.value;
}
const clickChangedPic = () => {
  model.value.picUrl = changedUrl.value;
}

const handleRestorePic = () => {
  // 检查 originalUrl.value 是否为空或空字符串
  if (!originalUrl.value || originalUrl.value.trim() === '') {
    return;
  }
  model.value.picUrl = String(originalUrl.value);
  hasImage.value = true;
  //originalUrl.value='';
  changedUrl.value = '';
  const canvasDrawing = canvasRef.value;
  const ctxDrawing = canvasDrawing.getContext('2d');
  ctxDrawing.clearRect(0, 0, canvasDrawing.width, canvasDrawing.height);
  historyImageList.value = [];
  addImage(1, model.value.picUrl);
}

const handleClearPic = () => {
  model.value.picUrl = '';
  hasImage.value = false;
  originalUrl.value = '';
  changedUrl.value = '';
  historyImageList.value = [];
}
/**去除背景 */
const imageKey = ref(0);
const originalUrl = ref('');
const changedUrl = ref('');



const handlePasteImg = async (event) => {
  const items = event.clipboardData?.items;
  if (!items) return;
  changedUrl.value = '';
  try {
    for (let i = 0; i < items.length; i++) {
      if (items[i].type.indexOf('image') !== -1) {
        const blob = items[i].getAsFile();
        if (!blob) continue;

        const reader = new FileReader();
        reader.onload = async (e) => {
          picContent.value = e.target?.result;
          const img = new Image();
          img.onload = () => {
            imageWidth.value = img.width;
            imageHeight.value = img.height;
          };
          img.src = e.target?.result;

          // 创建File对象并上传
          const file = new File([blob], "uploaded-image.png", { type: blob.type });
          const formData = new FormData();
          formData.append('file', file);
          formData.append('isPic', true);
          formData.append('uploadType', 2);
          let token = authorization.getToken();
          let encryptToken = encrypt_url(token);
          let userName = authorization.getUserName();
          const headers = {
            "token": encryptToken,
            "userName": userName,
          };
          processing.value = true;
          try {
            const response = await PictureApi.uploadPic(formData, headers);
            console.log('上传成功:', response);
            console.log(">>>>>>上传自己的头像后得到图片的地址为->" + response.data);
            // 获取原始URL
            const code = response.data.code;
            let imgUrl = '';
            let imgUrlPrefix = settings.request.previewImgUrl;
            imgUrl = imgUrlPrefix + code;
            model.value.picUrl = imgUrl;
            hasImage.value = true;
            originalUrl.value = String(imgUrl);
            historyImageList.value = [];
            addImage(1, originalUrl.value);
            message.success('图片上传成功');
            processing.value = false;
          } catch (error) {
            processing.value = false;
            console.error("后台上传自己的头像图片失败:", error);
            message.error('图片上传失败');
          } finally {
          }
        };
        reader.readAsDataURL(blob);
      }
    }
  } catch (error) {
  } finally {
  }
};

const picToolSelected = (selectedType) => {
  if (!selectedType || selectedType.trim() === '') {
  }
  type.value = selectedType;
  if (selectedType === 'removewatermark') {

  }

  initCanvas();
}


const handlePicTool = () => {
  if (!type.value || type.value.trim() === '') {
    return;
  }
  if (type.value === 'removebg') {
    removeBG();
  } else if (type.value === 'removewatermark') {
    removewatermark();
  }
}

//用来历史痕迹的-开始
const historyImageList = ref([]);

const addImage = (picType, newUrl) => {
  if ((picType !== 1 && picType !== 2) || !newUrl || newUrl === '') {
    return;
  }
  let newLabel = '';
  let newImage = {};
  if (picType === 1) {
    newLabel = '原图';
    newImage = {
      url: newUrl,
      label: newLabel,
    }
  } else if (picType === 2) {
    newLabel = `修改${historyImageList.value.length}`;
    newImage = {
      url: newUrl,
      label: newLabel,
    }
  }

  if (historyImageList.value.length < 3) {
    historyImageList.value.push(newImage);
  } else {
    historyImageList.value[historyImageList.value.length - 1] = newImage;
  }
  selectedHistoryIndex.value = historyImageList.value.length - 1;
};
//用来历史痕迹的-结束

const removewatermark = async () => {
  processing.value = true;
  try {
    // 保存原图URL
    //originalUrl.value = model.value.picUrl;
    const img = new Image();
    img.crossOrigin = 'Anonymous';

    const imageLoadPromise = new Promise((resolve, reject) => {
      img.onload = resolve;
      img.onerror = reject;
    });

    img.src = model.value.picUrl; //拿当前的图片传到后台
    //img.src = originalUrl.value;
    await imageLoadPromise;

    const canvas = document.createElement('canvas');
    canvas.width = img.width;
    canvas.height = img.height;

    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0);

    const base64Data = canvas.toDataURL('image/jpeg');
    let originalPicContent = base64Data;
    let maskedPicContent = null;
    if (canvasRef.value) {
      const canvasDrawing = canvasRef.value; // 通过.value获取canvas DOM元素
      maskedPicContent = canvasDrawing.toDataURL('image/jpeg');
    }
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "image": originalPicContent,
      "mask": maskedPicContent,
      "zits_wireframe": true,
      "cv2_flag": "INPAINT_NS",
      "cv2_radius": 5,
      "hd_strategy": "Crop",
      "hd_strategy_crop_triger_size": 640,
      "hd_strategy_crop_margin": 128,
      "hd_trategy_resize_imit": 2048,
      "enable_brushnet": false,
      "brushnet_method": "random_mask",
      "brushnet_conditioning_scale": 1,
      "enable_powerpaint_v2": false,
      "powerpaint_task": "text-guided"
    }

    PictureApi.removeWatermark(payload).then(async (res) => {
      if (res) {
        // 清空 canvas
        const canvasDrawing = canvasRef.value;
        const ctxDrawing = canvasDrawing.getContext('2d');
        ctxDrawing.clearRect(0, 0, canvas.width, canvas.height);
        model.value.picUrl = res;
        changedUrl.value = res;
        imageKey.value += 1;
        addImage(2, res);
        processing.value = false;
      }
    }).catch(err => {
      console.log(`>>>>>>发送后台图片处理失败`, err);
    }).finally(() => {
      //console.log(">>>>>>返回了");
    });
  } catch (err) {
    console.error(err);
    processing.value = false;
  } finally {

  }
}



const removeBG = async () => {
  if (!model.value.picUrl) {
    message.error('请先选择图片');
    return;
  }

  try {
    processing.value = true;
    // 保存原图URL
    //originalUrl.value = model.value.picUrl;
    const img = new Image();
    img.crossOrigin = 'Anonymous';

    const imageLoadPromise = new Promise((resolve, reject) => {
      img.onload = resolve;
      img.onerror = reject;
    });

    //img.src = originalUrl.value;
    img.src = model.value.picUrl; //始终拿当前图
    await imageLoadPromise;

    const canvas = document.createElement('canvas');
    canvas.width = img.width;
    canvas.height = img.height;

    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0);

    const base64Data = canvas.toDataURL('image/jpeg').split(',')[1];
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    const payload = {
      "token": token,
      "userName": userName,
      name: "RemoveBG",
      image: base64Data
    };

    const response = await PictureApi.removeBG(payload);
    //console.log("response->", response);
    //originalUrl.value = response;
    model.value.picUrl = response;
    changedUrl.value = response;
    imageKey.value += 1;
    addImage(2, response);
    message.success('背景移除成功');
  } catch (error) {
    console.error('处理图片失败:', error);
  } finally {
    processing.value = false;
  }
};


// 组件卸载时清理 URL
onUnmounted(() => {
});

const photoExp = ref(null);
/*下载开始 */
const handleDownloadPic = async () => {
  if (!model.value.picUrl || type.value.trim() === '') {
    return;
  }
  try {
    handleDownloadPicWithTxt();
  } catch (error) {
    console.error('下载失败:', error)
    message.error('图片下载失败，请重试')
  }
}
/*下载结束 */

/* 下载带有文字的图片开始 */
const handleDownloadPicWithTxt = async () => {
  //if (!model.value.picUrl || type.value !== 'puttextonpicture') {
  //  return;
  //}

  try {
    const canvas = photoExp.value;
    if (!canvas) {
      throw new Error('确保图片不为空');
    }

    canvas.style.border = 'none';
    // 找到可编辑的div
    const editableDiv = canvas.querySelector('[contenteditable="true"]');
    if (editableDiv) {
      editableDiv.style.border = 'none';
    }

    await nextTick();

    const result = await html2canvas(canvas, {
      scale: window.devicePixelRatio || 2,
      useCORS: true,
      backgroundColor: null,
      width: canvas.clientWidth,
      height: canvas.clientHeight
    });

    result.toBlob((blob) => {
      if (!blob) {
        throw new Error('Failed to create blob');
      }

      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.style.display = 'none';
      link.href = blobUrl;
      link.download = `image_ai_${Date.now()}.png`;

      document.body.appendChild(link);
      link.click();

      document.body.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);

      if (editableDiv) {
        editableDiv.style.border = '1px dashed white';
      }

      message.success('图片已保存');
    }, 'image/png');

  } catch (error) {
    console.error('保存失败:', error);
    message.error('图片保存失败，请重试');
  }
};
/* 下载带有文字的图片结束 */


/*上传开始*/
const fileInput = ref(null);
const processing = ref(false);

const handlePicUpload = () => {
  fileInput.value.click()
}

const onFileSelected = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  changedUrl.value = '';
  // 验证文件类型
  if (!file.type.includes('image/')) {
    message.error('请选择图片文件')
    return
  }

  // 验证文件大小（4MB）
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    message.error('图片大小不能超过5MB')
    return
  }

  // 创建 FormData
  const formData = new FormData()
  formData.append('file', file);
  formData.append('isPic', true);
  formData.append('uploadType', 2);
  let token = authorization.getToken();
  let encryptToken = encrypt_url(token);
  let userName = authorization.getUserName();
  const headers = {
    "token": encryptToken,
    "userName": userName,
  };

  try {
    processing.value = true;
    const response = await PictureApi.uploadPic(formData, headers);
    console.log('上传成功:', response);
    console.log(">>>>>>上传自己的头像后得到图片的地址为->" + response.data);
    // 获取原始URL
    const code = response.data.code;


    let imgUrl = '';
    let imgUrlPrefix = settings.request.previewImgUrl;
    imgUrl = imgUrlPrefix + code;
    model.value.picUrl = imgUrl;
    originalUrl.value = String(imgUrl);
    hasImage.value = true;
    // 添加获取图片尺寸的代码
    const img = new Image();
    img.src = imgUrl;
    img.onload = () => {
      imageWidth.value = img.width;
      imageHeight.value = img.height;
    };
    historyImageList.value = [];
    addImage(1, imgUrl);
    message.success('上传成功');
  } catch (error) {
    model.value.picUrl = '';
    hasImage.value = false;
    console.error('上传失败:', error)
    message.error(error.message || '上传失败，请重试')
  } finally {
    // 清空文件输入框，使得能够重复上传相同文件
    event.target.value = ''
    processing.value = false;
  }
}
/*上传结束*/
/**文件上传结束 */
// 添加上一个点的位置记录
const lastPoint = ref({ x: 0, y: 0 })

//画布上作画
const handleMouseOver = (event) => {
  if (type.value === 'removebg') {
    //console.log(">>>>>>当前type.value->" + type.value);
    return;
  }
  document.body.style.cursor = 'crosshair';
  const rect = canvasRef.value.getBoundingClientRect();
  const mouseX = event.clientX - rect.left;
  const mouseY = event.clientY - rect.top;

  // 更新画笔位置
  brushPosition.value = {
    x: mouseX,
    y: mouseY
  };
  //console.log(">>>>>>鼠标进入画布")
  //showBrushCursor.value = true // 显示画笔光标
  //document.body.style.cursor = 'none'; // 隐藏默认鼠标光标
  // 获取canvas元素和上下文

}

// 处理鼠标移动
const handleMouseMove = (event) => {
  if (type.value === "removebg") {
    return;
  }
  document.body.style.cursor = 'default';
  const rect = canvasRef.value.getBoundingClientRect();
  // 使用与绘制相同的位置计算方法
  brushPosition.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  };
  draw(event);
}


const canvasRef = ref(null);

// 开始绘画
// 修改 startDrawing 函数
const startDrawing = (event) => {
  if (type.value === "removebg" || event.button !== 0) {
    return;
  }
  isDrawing.value = true;
  const rect = canvasRef.value.getBoundingClientRect();
  const scaleX = canvasRef.value.width / rect.width;
  const scaleY = canvasRef.value.height / rect.height;

  const mouseX = event.clientX - rect.left;
  const mouseY = event.clientY - rect.top;

  // 更新画笔位置
  brushPosition.value = {
    x: mouseX,
    y: mouseY
  };

  lastPoint.value = {
    x: mouseX * scaleX,
    y: mouseY * scaleY
  };
}


// 修改 draw 函数
const draw = (event) => {
  if (!isDrawing.value || type.value === "removebg") {
    return;
  }
  const ctx = ctxDrawing.value;
  if (ctx) {
    const rect = canvasRef.value.getBoundingClientRect();
    const scaleX = canvasRef.value.width / rect.width;
    const scaleY = canvasRef.value.height / rect.height;

    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    // 同步更新画笔光标位置
    brushPosition.value = {
      x: mouseX,
      y: mouseY
    };

    const x = mouseX * scaleX;
    const y = mouseY * scaleY;

    ctx.strokeStyle = '#D86ECC';
    ctx.lineWidth = brushSize.value * 10;
    ctx.lineCap = 'round';
    //ctx.lineJoin = 'round';

    ctx.beginPath();
    ctx.moveTo(lastPoint.value.x, lastPoint.value.y);
    ctx.lineTo(x, y);
    ctx.stroke();

    lastPoint.value = { x, y };
  }
}

// 停止绘画
const stopDrawing = () => {
  if (type.value === "removebg") {
    return;
  }
  isDrawing.value = false
}

// 处理鼠标离开
const handleMouseLeave = () => {
  if (type.value === "removebg") {
    return;
  }
  //console.log("鼠标离开画布")
  showBrushCursor.value = false
  //document.body.style.cursor = 'default'; // 恢复默认鼠标光标
  stopDrawing()
}


onUnmounted(() => {
})

onMounted(async () => {

  // 初始化 canvas 上下文
  if (canvasRef.value) {
    ctxDrawing.value = canvasRef.value.getContext('2d');
    if (ctxDrawing.value) {
      //ctxDrawing.value.lineJoin = 'round';
      ctxDrawing.value.lineCap = 'round';
      ctxDrawing.value.strokeStyle = 'rgba(255, 255, 0, 0.5)';
    }
  }
});




// 新增的watch用于监听图片变化
watch(
  () => model.value.picUrl,
  (newVal) => {
    if (newVal && type.value === 'removewatermark') {
      nextTick(() => {
        initCanvas()
      })
    }
  },
  { immediate: true }
)


// canvas初始化函数
const initCanvas = () => {
  if (canvasRef.value) {
    //console.log(">>>>>>初始化context")
    ctxDrawing.value = canvasRef.value.getContext('2d')
    if (ctxDrawing.value) {
      ctxDrawing.value.lineJoin = 'round'
      ctxDrawing.value.lineCap = 'round'
      ctxDrawing.value.strokeStyle = 'rgba(255, 255, 0, 0.5)'
    }
  }
}
// 添加一个计算属性来控制是否显示十字准线
const shouldShowCrosshair = computed(() => {
  if (type.value === 'removewatermark') {
    return true;
  }
});
</script>
<style scoped>
:deep(.ant-btn) {
  height: auto !important;
}

/* 根容器样式 */
.main {
  background: #262626;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
}

/* 确保 spin 容器占满高度 */
:deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 头部区域固定高度 */
.header-section {
  flex-shrink: 0;
  /* 防止头部被压缩 */
}

/* workspace 占据剩余空间 */
.workspace {
  display: flex;
  flex: 1;
  min-height: 0;
  /* 重要：防止溢出 */
}

.workspace-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}



.workspace-toolzone {
  height: 100%;
  min-width: 0;
}

.crosshair-cursor {
  cursor: crosshair !important;
  /* 使用!important确保样式优先级 */
}

/* 使用:deep()进行样式穿透 */
.white-slider :deep(.ant-slider-track) {
  background-color: rgba(255, 255, 255, 0.8) !important;
}

.white-slider :deep(.ant-slider-handle) {
  background-color: #ffffff !important;
  border-color: #ffffff !important;
}

.white-slider :deep(.ant-slider-rail) {
  background-color: rgba(255, 255, 255, 0.2) !important;
}

.white-slider:hover :deep(.ant-slider-track) {
  background-color: #ffffff !important;
}

.white-slider:hover :deep(.ant-slider-handle) {
  border-color: #ffffff !important;
}
</style>