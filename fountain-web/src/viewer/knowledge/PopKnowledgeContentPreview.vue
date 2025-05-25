<template>
  <a-modal :visible="modelValue" title="将要入库条目" :width="'95%'" :style="{ top: '0%' }" @cancel="handleCancel"
    cancelText="关闭" okText="提交" @ok="handleOk">
    <div
      style="height: 100%; width: 100%; border: 1px solid #47D45A; font-size: 13px; color: #595959; align-items: center; margin-top: 10px; ">
      <div v-if="hasImg" style="margin-bottom: 15px; margin-left: 8px; margin-top: 10px;">
        <a-checkbox v-model:checked="vlEmbedding" style="font-weight: bold;">使用多模态向量</a-checkbox>
      </div>
      
      <div v-for="(item, index) in currentPageData" :key="index" style="margin-bottom: 20px; padding: 10px; border-bottom: 1px dashed #e8e8e8; display: flex;">
        <div style="min-width: 30px; margin-right: 10px; font-weight: bold;">{{ (currentPage - 1) * pageSize + index + 1 }}</div>
        <div style="flex: 1;">
          <div style="margin-bottom: 10px; white-space: pre-line;">{{ item.content }}</div>
          <div v-if="item.img && item.img.trim() !== ''">
            <img :src="item.img" style="height: 70px; width: auto; object-fit: contain;" />
          </div>
        </div>
      </div>
      
      <a-pagination 
        v-if="totalPages > 1" 
        v-model:current="currentPage" 
        :total="contentJsonData.length" 
        :pageSize="pageSize" 
        style="text-align: center; margin-top: 15px; margin-bottom: 15px;" 
      />
    </div>
  </a-modal>
</template>
<script setup>
import { ref, watch, computed } from 'vue';
import { Modal, Pagination, Checkbox } from 'ant-design-vue';
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
  needSplit: {
    type:Boolean,
    default: false
  },
  contentJson:{
    type:String,
    default: ''
  },
  fileId:{
    type: String,
    default: ''
  },
  fileType:{
    type: Number,
    default: 1
  },
});
const loadingMask = ref(false);
const emit = defineEmits(['update:visible', 'execute-embedding']);
const hasImg = ref(false);
const vlEmbedding = ref(false);
const contentJsonData = ref([]);
const currentPage = ref(1);
const pageSize = 5;

// 计算当前页的数据
const currentPageData = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  return contentJsonData.value.slice(startIndex, endIndex);
});

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(contentJsonData.value.length / pageSize);
});

const handleCancel = () => {
  emit('update:modelValue', false);
  //emit('refresh-detail');
};

const handleOk = async () => {
  loadingMask.value = true;
  try {
    emit('update:modelValue', false);
    emit('execute-embedding',vlEmbedding.value);
  } catch (error) {
    console.error(">>>>>>execute embedding item error",error);
  }
}

// 解析contentJson
const parseContentJson = (jsonStr) => {
  try {
    if (!jsonStr) return [];
    
    const data = JSON.parse(jsonStr);
    if (!Array.isArray(data)) return [];
    
    return data;
  } catch (error) {
    console.error("解析contentJson出错:", error);
    return [];
  }
};

// 检查contentJson中是否有非空的img属性
const checkHasImg = (jsonStr) => {
  try {
    if (!jsonStr) return false;
    
    const data = JSON.parse(jsonStr);
    if (!Array.isArray(data)) return false;
    
    // 遍历数组，检查每个元素的img属性是否有内容
    return data.some(item => item.img && item.img.trim() !== '');
  } catch (error) {
    console.error("解析contentJson出错:", error);
    return false;
  }
};

watch(
  [() => props.modelValue, () => props.contentJson],
  ([newModelValue, newContentJson]) => {
    if (newModelValue) {  // 当模态框显示时
      // 检查contentJson中是否有图片
      hasImg.value = checkHasImg(newContentJson);
      // 解析contentJson数据
      contentJsonData.value = parseContentJson(newContentJson);
      // 重置分页
      currentPage.value = 1;
      // 重置复选框
      vlEmbedding.value = false;
      console.log(">>>>>>是否含有图片:", hasImg.value);
    } else {
      // 模态框关闭时清空数据
      hasImg.value = false;
      contentJsonData.value = [];
    }
  },
  { immediate: true }  // 添加 immediate: true 确保组件挂载时就执行一次
);

</script>

<style scoped></style>