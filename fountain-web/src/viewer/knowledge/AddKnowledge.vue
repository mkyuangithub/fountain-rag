<template>
  <a-modal :visible="modelValue" title="添加知识库" :width="'50%'" :style="{ top: '0%' }"
    @cancel="handleCancel" okText="提交" @ok="handleOk">
    <div style="height: 100%; width: 100%;">
      <div style="display: flex; width: 80%; align-items: center;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          知识库名称:
        </div>
        <div style="width: 80%;">
          <a-input v-model:value="knowledgeName" placeholder="必输: 请输入知识库名称" style="width: 100%; margin-left: 20px;" />
        </div>
      </div>
      <div style="display: flex; width: 80%; align-items: center; margin-top: 40px;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          知识库描述:
        </div>
        <div style="width: 80%;">
          <a-input v-model:value="knowledgeDescr" placeholder="非必输: 输入对此知识库的一个描述便于记忆"
            style="width: 100%; margin-left: 20px;" />
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
import KnowledgeMgtApi from "@/api/KnowledgeMgtApi.js";
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
});

const emit = defineEmits(['update:visible', 'refresh-repo']);
const knowledgeName = ref('');
const knowledgeDescr = ref('');




const handleCancel = () => {
  emit('update:modelValue', false);
};

const handleOk = () => {
  try {
    // 调用 API

    doAdd();
    emit('update:modelValue', false);
  } catch (error) {
    message.error('添加知识库失败：' + error.message);
  }
  //emit('update:modelValue', false);
};

const doAdd = async () => {
  try {
    try {
      let token = authorization.getToken();
      let encryptToken = encrypt_url(token);
      let userName = authorization.getUserName();
      let payload = {
        "token": encryptToken,
        "userName": userName,
        "knowledgeName": knowledgeName.value,
        "knowledgeDescr": knowledgeDescr.value,
      }
      await KnowledgeMgtApi.addRepo(payload).then(res => {
        let returnCode=res;
        if(returnCode===100){
        message.success("添加知识库成功");
        }else if(returnCode===101){
          message.warn("要添加的: "+knowledgeName.value+" 己经存在，请不要重复添加!");
        }else{
          message.error("添加失败");
        }
      }).catch(err => {
        message.error("添加失败");
      });
    } catch (err) {
      console.log(">>>>>>add a repo by id error->{}" + err);
    }

  } catch (err) {
    console.log(">>>>>>添加知识库出错->" + err);
  } finally {
    emit("refresh-repo");
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