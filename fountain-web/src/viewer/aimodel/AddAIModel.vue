<template>
  <a-modal :visible="modelValue" title="添加AI模型" :width="'50%'" :style="{ top: '0%' }" @cancel="handleCancel" okText="提交"
    @ok="handleOk">
    <div style="height: 100%; width: 100%;">
      <div style="display: flex; width: 80%; align-items: center;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          AI模型名:
        </div>
        <div style="width: 80%;">
          <a-input v-model:value="modelName" autocomplete="off" placeholder="必输: 请输入模型名"
            style="width: 100%; margin-left: 20px;" />
        </div>
      </div>
      <div style="display: flex; width: 80%; align-items: center; margin-top: 15px;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          模型连接地址:
        </div>
        <div style="width: 80%;">
          <a-input v-model:value="modelUrl" autocomplete="off" placeholder="必输: 请输入模型连接地址"
            style="width: 100%; margin-left: 20px;" />
        </div>
      </div>
      <div style="display: flex; width: 80%; align-items: center; margin-top: 15px;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          api-key:
        </div>
        <div style="width: 80%;">
          <a-input-password v-model:value="modelApiKey" autocomplete="new-password" placeholder="非必输: 请输入api-key"
            style="width: 100%; margin-left: 20px;" />
        </div>
      </div>
      <div style="display: flex; width: 80%; align-items: center; margin-top: 15px;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          选择模型类型:
        </div>
        <div style="width: 80%; margin-left: 20px;">
          <a-select v-model:value="selectedModelType" style="width: 100%">
            <a-select-option :value="1">deeseek本地</a-select-option>
            <a-select-option :value="2">deeseek saas</a-select-option>
            <a-select-option :value="3">GPT 4o-mini</a-select-option>
            <a-select-option :value="4">GPT 4o</a-select-option>
            <a-select-option :value="5">Qwen3</a-select-option>
            <a-select-option :value="6">Microsoft Phi4模型</a-select-option>
            <a-select-option :value="7">Google Gemma3模型</a-select-option>
            <a-select-option :value="8">Qwen系列</a-select-option>
          </a-select>
        </div>
      </div>
    </div>
  </a-modal>
</template>
<script setup>
import { ref } from 'vue';
import { message } from "ant-design-vue";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import AIModelApi from "@/api/AIModelApi.js";
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
});

const emit = defineEmits(['update:visible', 'refresh-model']);
const modelName = ref('');
const modelUrl = ref('');
const modelApiKey = ref('');
const selectedModelType = ref(1);


const handleCancel = () => {
  emit('update:modelValue', false);
};

const handleOk = () => {
  try {
    // 调用 API
    doAdd();
    emit('update:modelValue', false);
  } catch (error) {
    message.error('添加AI模型失败：' , error);
  }
  //emit('update:modelValue', false);
};

const doAdd = async () => {
  try {

    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "modelName": modelName.value,
      "url": modelUrl.value,
      "modelApiKey": modelApiKey.value,
      "type": selectedModelType.value,
    }
    await AIModelApi.addModel(payload).then(res => {
      if(res===1){
        message.success("添加AI模型成功");
      }else if (res===101){
        message.warn("您正在添加的[模型类型]已经存在，同一类型的模型只允许存在一个。");
      }else{
        message.error("添加AI模型失败");
      }
    }).catch(err => {
      console.error(">>>>>>添加AI模型失败->" ,err);
      message.error("添加AI模型失败");
    });
  } catch (err) {
    console.error(">>>>>>添加AI模型失败->" , err);
  } finally {
    emit("refresh-model");
  }
}







</script>

<style>
/* 使用特定的类名来限制范围 */
.midjourney-modal .ant-modal-content {
  background-color: white !important;
}

.midjourney-modal .ant-modal-header {
  background-color: white !important;
  border-bottom: none !important;
}

.midjourney-modal .ant-modal-title {
  color: black !important;
}

.midjourney-modal .ant-modal-body {
  background-color: white !important;
}

.midjourney-modal .ant-modal-footer {
  background-color: white !important;
  border-top: none !important;
}

.midjourney-modal .ant-modal-close-x {
  color: black !important;
}


.midjourney-modal .ant-modal-footer .ant-btn:hover {
  border-color: blue !important;
}

.midjourney-modal .ant-modal-footer .ant-btn+.ant-btn {
  margin-inline-start: 8px;
}
</style>