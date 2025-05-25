<template>
  <a-modal :visible="modelValue" :title="props.stepTitle" :width="'50%'" :style="{ top: '0%' }" @cancel="handleCancel"
    okText="提交" @ok="handleOk">
    <div style="height: 100%; width: 100%;">
      <div style="margin-top: 10px; display: flex;">
        <div style="width: 20%;">
          是否启用
        </div>
        <div style="width: 80%;">
          <a-radio-group v-model:value="stepEnabled">
            <a-radio :value="true">启用</a-radio>
            <a-radio :value="false">不启用</a-radio>
          </a-radio-group>
        </div>
      </div>
      <div style="margin-top: 10px; display: flex;">
        <div style="width: 20%;">
          temperature设定
        </div>
        <div style="width: 70%;">
          <a-slider v-model:value="stepTemperature" :min="0.1" :max="2.0" :step="0.1" style="width: 90%;" />
        </div>
        <div style="width: 10%;">
          <a-input v-model:value="stepTemperature" autocomplete="off" style="width: 100%; color:#595959; " />
        </div>
      </div>
      <div style="margin-top: 10px;">
        <div>
          提示语
        </div>
        <div>
          <a-textarea v-model:value="stepPrompt" rows="15" style="width: 100%;" />
        </div>
        
      </div>
    </div>
  </a-modal>
</template>
<script setup>
import { ref, watch } from 'vue';
import { message } from "ant-design-vue";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import UserChatConfigApi from "@/api/UserChatConfigApi.js";
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  stepTitle: {
    type: String,
    default: ''
  },
  enabled: {
    type: Boolean,
    default: true
  },
  prompt: {
    type: String,
    default: ''
  },
  temperature: {
    type: Number,
    default: 0.1
  },
  type:{
    type: Number,
    default:0
  }
});

const stepEnabled = ref(true);
const stepPrompt = ref('');
const stepTemperature = ref(0.1);

const emit = defineEmits(['update:visible', 'refresh-chatStep']);

const handleCancel = () => {
  emit('update:modelValue', false);
};

const handleOk = () => {
  try {
    // 调用 API
    doUpdate();
    emit('update:modelValue', false);
  } catch (error) {
    message.error('配置步骤相关参数失败' + error);
  }
};

const doUpdate = async () => {
  try {
      if(props.type>0){
        let callBackData={
          type:props.type,
          enabled: stepEnabled.value,
          prompt: stepPrompt.value,
          temperature: stepTemperature.value,
        }
        emit("refresh-chatStep",callBackData);
      }
  } catch (err) {
    console.error(">>>>>>配置步骤相关参数失败->" + JSON.stringify(err));
  }
}
watch(
  [() => props.modelValue],
  ([newModelValue]) => {
    if (newModelValue) {  // 当模态框显示时
      stepEnabled.value = props.enabled;
      stepPrompt.value = props.prompt;
      stepTemperature.value = props.temperature;
    } else {
      // 模态框关闭时清空数据
      stepEnabled.value = false;
      stepPrompt.value = '';
      stepTemperature.value = 0.1;
    }
  },
  { immediate: true }  // 添加 immediate: true 确保组件挂载时就执行一次
);

</script>


<style scoped></style>