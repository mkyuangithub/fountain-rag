<template>
  <a-modal :visible="modelValue" title="新增知识库条目" :width="'95%'" :style="{ top: '0%' }" @cancel="handleCancel"
    cancelText="关闭" okText="提交" @ok="handleOk">
    <div
      style="height: 100%; width: 100%; border: 1px solid #47D45A; font-size: 13px; color: #595959; align-items: center; margin-top: 10px;">
      <div style="display: flex; flex-direction: column;">
        <span style="margin-left: 10px; margin-top: 10px;">知识库条目内容</span>
        <a-textarea v-model:value="content" :rows="10" :auto-size="{ minRows: 12, maxRows: 12 }"
          style="resize: none; width: 100%; margin-top: 10px;" placeholder="输入知识库条目内容" />
      </div>
      <div style="font-size: 13px; color:#595959; margin-top: 10px; margin-left: 10px;">
        当前标签:
      </div>
      <div
        style="font-size: 13px; color:#595959; margin-top: 10px; margin-left: 10px; display: flex; align-items: center;">
        <span>
          手工添加:
        </span>
        <a-input v-model:value="newLabel" placeholder="必输: 手工添加标签，用逗号分隔多个标签" :class="custom - lable - textbox"
          style="height: 24px; width: 60%; margin-left: 5px;" />
        <div style="cursor: pointer; display: flex; margin-top: 0px;  align-items: center; margin-left: 10px;"
          @click="handleAddLabel()">
          <PlusCircleOutlined style="color: blue; font-size: 12px;" />
          <span style="color: #595959;  margin-left:5px;  font-size:12px;">手工添加</span>
        </div>
      </div>
      <div v-if="labels.length > 0">
        <div v-for="(label, index) in labels" :key="index" style="display: inline-flex; align-items: center; padding: 4px 12px; 
                    background: white; border: 1px dashed #00B0F0; 
                    border-radius: 5%; position: relative; margin: 4px;">
          <span style="font-size: 11px; color:#595959">{{ label }}</span>
          <span style="cursor: pointer; margin-left: 4px; font-size: 12px;" @click="removeLabel(index)">×</span>
        </div>
      </div>
      <!--专门用来显示全屏遮罩-->
      <div v-if="loadingMask"
        style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(255, 255, 255, 0.7); display: flex; flex-direction: column; justify-content: center; align-items: center; z-index: 9999;">
        <img src="/assets/images/waiting-blue.gif" alt="loading" />
        <div style="margin-top: 10px; font-size: 16px; color: #1890ff; font-weight: 500;">AI生成中</div>
      </div>
    </div>
  </a-modal>
</template>
<script setup>
import { ref, watch } from 'vue';
import { Modal } from 'ant-design-vue';
import { message } from "ant-design-vue";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import KnowledgeVectorApi from "@/api/KnowledgeVectorApi.js";
import { string } from 'vue-types';
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  repoId: {
    type: String,
    default: ''
  },

});
const content = ref('');
const newLabel = ref('');
const labels = ref([]);
const loadingMask = ref(false);
const emit = defineEmits(['update:visible', 'refresh-detail']);


const handleCancel = () => {
  emit('update:modelValue', false);
  emit('refresh-detail');
};

const handleOk = async () => {
  loadingMask.value = true;
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "knowledgeRepoId": props.repoId,
      "content": content.value,
      "labels": labels.value,
    }
    console.log(">>>>>>start post to addKnowledgeDetail")
    KnowledgeVectorApi.addKnowledgeDetail(payload).then(res => {
      // 从返回的列表中先拿到总条数     
      console.log(">>>>>>post to addKnowledgeDetail res->"+res)
    }).catch(err => {
      console.log('>>>>>>add  detail knowledge item api error:', err);
    }).finally(() => {
      loadingMask.value = false;
      emit('update:modelValue', false);
      emit('refresh-detail');
    });
  } catch (error) {
    console.error(">>>>>>add detail knowledge item error",error);
    message.error('add detail knowledge item error', error.message);
  }
}

const handleAddLabel = async () => {
  try {
    if (newLabel.value && newLabel.value.trim()) {
      const inputValue = newLabel.value.trim();
      // 直接按英文逗号分割，过滤空字符串，去除每个标签的首尾空格
      const addedLabels = inputValue.split(',')
        .map(label => label.trim())
        .filter(label => label.length > 0);

      // 将所有有效的标签添加到数组中
      addedLabels.forEach(label => {
        labels.value.push(label);
      });

      newLabel.value = ''; // 清空输入
    }
  } catch (err) {
    console.error("add a label error", err);
  }
}

const removeLabel = (index) => {
  Modal.confirm({
    title: '确认删除',
    content: '您确认删除当前标签？此操作不可撤销!',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      labels.value.splice(index, 1);
    },
  });
}
watch(
  [() => props.modelValue],
  ([newModelValue]) => {
    if (!newModelValue) {  // 当模态框显示时
      // 模态框关闭时清空数据
      content.value = '';
      labels.value = [];
    }
  },
  { immediate: true }  // 添加 immediate: true 确保组件挂载时就执行一次
);

</script>

<style scoped></style>