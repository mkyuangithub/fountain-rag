<template>
  <div class="chat-message" :class="{ 'user-message': isUser }">
    <div class="message-content">
      <div class="avatar-container">
        <div class="avatar-wrapper">
          <img :src="avatarSrc" :alt="isUser ? 'User Avatar' : 'AI Avatar'" class="avatar-image">
        </div>
      </div>
      <!--
      <div class="message-text" :class="{ 'ai-message': !isUser }" v-html="formattedMessage"></div>
  -->
      <div class="markdown-container">
        <md-preview :model-value="formattedMessage" class="message-text ai-message" :preview-theme="'default'"
          :code-theme="'atom'" :show-code-row-number="false" :table-cell-max-width="500"   :editorId="'preview-only'" 
          :html="true" />
      </div>

    </div>
  </div>
</template>

<script setup>

import { MdPreview } from "md-editor-v3";
import { computed } from 'vue';
import { isBlank } from "@/toolkit/utils.js";

const props = defineProps({
  isUser: {
    type: Boolean,
    default: false
  },
  message: {
    type: String,
    required: true
  },
  dataIds: {
    type: Array,
    default: () => [],
    required: false
  },
  chatType: {  // 添加这个prop
    type: String,
    required: true
  }
});
const AVATAR_CONFIG = {
  user: '/assets/images/user-chat-2.png',
  m01: {
    src: '/assets/images/ai-4.png',
    label: 'AI'
  },
  default: {
    src: '/assets/images/ai-4.png',
    label: 'AI'
  }
};

const avatarSrc = computed(() => {
  if (props.isUser && props.chatType === "m05") {
    const headPicture = sessionStorage.getItem("userAvatar");
    return isBlank(headPicture) ? AVATAR_CONFIG.user : headPicture;
  }

  if (props.isUser) {
    return AVATAR_CONFIG.user;
  }
  return (AVATAR_CONFIG[props.chatType] || AVATAR_CONFIG.default).src;
});

const avatarLabel = computed(() => {
  return (AVATAR_CONFIG[props.chatType] || AVATAR_CONFIG.default).label;
});
const formattedMessage = computed(() => {
  if (!props.message) return '';

  // 将<think>标签内容转换为Markdown引用块
  let content = props.message;
  
  // 使用正则表达式匹配<think>标签内容并替换为Markdown引用块
  content = content.replace(/<think>([\s\S]*?)<\/think>/g, (match, thinkContent) => {
    // 将think内容转换为Markdown引用块格式
    // 1. 添加引用块标记 >
    // 2. 确保每行都有引用标记
    // 3. 添加标题和适当的空行
    return '\n\n> **思考过程：**\n> ' + thinkContent.trim().split('\n').join('\n> ') + '\n\n';
  });
  
  return content;
});
</script>
<style scoped>
.chat-message {
  width: calc(100% - 20px);
  margin: 10px;
  margin-top: 15px;
  padding: 0px;
  padding-left: 10px;
  /* 设置左边距为10px */
  margin-left: 0;
  /* 清除可能存在的左边距 */
}

.message-content {
  display: flex;
  align-items: flex-start;
  margin: 0px;
}

.avatar-container {
  margin-left: 5px;
  padding: 0px;
  min-width: 80px;
  /* 设置固定最小宽度 */
  width: 80px;
  /* 设置固定宽度 */
  flex-shrink: 0;
  /* 防止容器被压缩 */
}

.avatar-wrapper {
  margin: 0px;
  padding: 0px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  /* 调整label和图片之间的间距 */
}

.avatar-label {
  font-size: 12px;
  white-space: nowrap;
  /* 防止文本换行 */
  max-width: none;
  /* 允许文本超出容器宽度 */
}

.avatar-image {
  width: 32px;
  height: 32px;
}


.avatar {
  width: 28px;
  height: auto;
  object-fit: contain;
}

.message-text {
  flex: 1;
  padding: 8px;
  border-radius: 8px;
  white-space: normal;
  /* 改为normal */
  word-wrap: break-word;
  margin-left: 10px;
}

.user-message .message-text {
  background-color: transparent;
}

.ai-message {
  background-color: white;
}

.markdown-container {
  font-size: 13px;
  line-height: 1.5;
}

.markdown-container ::v-deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  background-color: #f8f8f8;
  /* 更浅的灰色背景 */
  border-left: 4px solid #d4d4d4;
  /* 更浅的边框颜色 */
  border-radius: 4px;
}

.markdown-container ::v-deep(blockquote p) {
  margin: 0.4em 0;
  color: #666;
  /* 思考内容的文字颜色稍微淡一些 */
}


.markdown-container ::v-deep(p) {
  margin: 0.2em 0;
}

.markdown-container ::v-deep(ol),
::v-deep(ul),
::v-deep(dl) {
  margin: 0.2em 0;
}

.markdown-container ::v-deep(h1),
::v-deep(h2),
::v-deep(h3),
::v-deep(h4),
::v-deep(h5),
::v-deep(h6) {
  margin: 0.1em 0;
}

/* 思考内容的样式 */
.markdown-container ::v-deep(.think-content) {
  background-color: #f8f8f8;
  border-left: 4px solid #d4d4d4;
  padding: 0.5em 1em;
  margin: 1em 0;
  border-radius: 4px;
  color: #666;
  font-family: inherit;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 确保md-preview不会对think-content应用Markdown样式 */
.markdown-container ::v-deep(.think-content *) {
  all: inherit;
  display: inline;
}
</style>