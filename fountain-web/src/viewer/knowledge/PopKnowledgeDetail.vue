<template>
  <a-modal :visible="modelValue" title="编辑知识库条目" :width="'95%'" :style="{ top: '0%' }" @cancel="handleCancel"
    cancelText="关闭" okText="提交" @ok="handleOk">
    <div
      style="height: 100%; width: 100%; border: 1px solid #47D45A; font-size: 13px; color: #595959; align-items: center; margin-top: 10px;">
      <div style="display: flex; flex-direction: column;">
        <span style="margin-left: 10px; margin-top: 10px;">知识库条目内容</span>
        <a-textarea v-model:value="editingItemOriginalContent" :rows="10" :auto-size="{ minRows: 12, maxRows: 12 }"
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
      <div v-if="editingItemLabels.length > 0">
        <div v-for="(label, index) in editingItemLabels" :key="index" style="display: inline-flex; align-items: center; padding: 4px 12px; 
                    background: white; border: 1px dashed #00B0F0; 
                    border-radius: 5%; position: relative; margin: 4px;">
          <span style="font-size: 11px; color:#595959">{{ label }}</span>
          <span style="cursor: pointer; margin-left: 4px; font-size: 12px;" @click="removeLabel(index)">×</span>
        </div>
      </div>
      <div v-else>
        <span
          style="margin-top: 10px; margin-top: 10px; margin-left: 10px; font-size: 13px; color: #c7c7c7;">当前条目暂无标签</span>
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
import KnowledgeMgtApi from "@/api/KnowledgeMgtApi.js";
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  item: {
    type: Object,
    default: () => ({})  // 对象类型的默认值必须使用工厂函数返回
  },
});
const editingItemId = ref('');
const editingItemRepoId = ref('');
const editingItemFileId = ref(0);
const editingItemUserName = ref('');
const editingItemFileName = ref('');
const editingItemPointId = ref(0);
const editingItemOriginalContent = ref('');
const editingItemLabels = ref([]);
const editingItemContentMd5 = ref('');
const newLabel = ref('');

const emit = defineEmits(['update:visible', 'refresh-detail']);


const handleCancel = () => {
  emit('update:modelValue', false);
  emit('refresh-detail');
};

const handleOk = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "detailId": editingItemId.value,
      "knowledgeRepoId": editingItemRepoId.value,
      "originalContent": editingItemOriginalContent.value,
      "labels": editingItemLabels.value,
    }
    // 使用 await 等待 API 调用完成
    const res = await KnowledgeMgtApi.updateDetailKnowledgeItem(payload);
    if (res === 1) {
      message.success("更新成功");
    } else if (res === 101) {
      message.warn("有上传进行中，请待上传结束后再更新");
    } else if (res === -1) {
      message.warn("更新失败");
    }
    //emit('update:modelValue', false);
  } catch (error) {
    message.error('更新知识库条目过程中发生了错误', error.message);
  } finally {
    // 此时确保更新操作已完成后再加载最新数据
    await loadlKnowledgeDetail();
  }
};

const handleAddLabel = async () => {
  try {
    if (newLabel.value && newLabel.value.trim()) {
      const inputValue = newLabel.value.trim();
      // 直接按英文逗号分割，过滤空字符串，去除每个标签的首尾空格
      const labels = inputValue.split(',')
        .map(label => label.trim())
        .filter(label => label.length > 0);

      // 将所有有效的标签添加到数组中
      labels.forEach(label => {
        editingItemLabels.value.push(label);
      });

      newLabel.value = ''; // 清空输入
    }
  } catch (err) {
    console.error("add a label error", err);
  }
}

const loadlKnowledgeDetail = async () => {
  if (props.item) {
    try {
      let token = authorization.getToken();
      let encryptToken = encrypt_url(token);
      let userName = authorization.getUserName();
      let payload = {
        "token": encryptToken,
        "userName": userName,
        "detailId": editingItemId.value,
      }
      await KnowledgeMgtApi.getDetailKnowledgeItem(payload).then(res => {
        editingItemRepoId.value = res.knowledgeRepoId;
        editingItemFileId.value = res.fileId;
        editingItemFileName.value = res.fileName;
        editingItemPointId.value = res.itemId;
        editingItemOriginalContent.value = res.originalContent;
        editingItemLabels.value = res.labels;
        editingItemContentMd5.value = res.contentMd5;
      }).catch(err => {
        message.error("获取知识库条目失败");
      });
    } catch (err) {
      console.log(">>>>>>loadKnowledgeTail error", err);
    }
  }
}

const removeLabel = (index) => {
  Modal.confirm({
    title: '确认删除',
    content: '您确认删除当前标签？此操作不可撤销!',
    okText: '确定',
    cancelText: '取消',
    onOk: () => {
      editingItemLabels.value.splice(index, 1);
    },
  });
}


watch(
  [() => props.modelValue],
  ([newModelValue]) => {
    if (newModelValue) {  // 当模态框显示时
      console.log(">>>>>>props.detailItem.id->" + props.item.id);
      editingItemId.value = props.item.id;
      loadlKnowledgeDetail();
    } else {
      // 模态框关闭时清空数据
      editingItemId.value = '';
      editingItemRepoId.value = '';
      editingItemFileId.value = 0;
      editingItemUserName.value = '';
      editingItemFileName.value = '';
      editingItemPointId.value = 0;
      editingItemOriginalContent.value = '';
      editingItemLabels.value = [];
      editingItemContentMd5.value = '';
    }
  },
  { immediate: true }  // 添加 immediate: true 确保组件挂载时就执行一次
);

</script>

<style scoped></style>