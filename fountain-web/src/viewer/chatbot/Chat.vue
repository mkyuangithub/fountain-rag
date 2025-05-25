<template>
  <div class="center">
    <div class="display-content" ref="chatMessageContainer">
      <div v-if="!isQuestionSubmitted" class="welcome-message">
        <h2 class="title">你好，欢迎使用Fountain引擎的聊天界面</h2>
        <p class="subtitle">选择事先配置好的对话并确保管理员把你加入到“允许对话”列表中然后开始聊天吧</p>
      </div>
      <div style="margin: 5px 5px 5px 5px">
        <chat-bot-message v-for="(message, index) in messages" :key="index" :is-user="message.isUser"
          :message="message.content" :chat-type="m01" />
      </div>
    </div>
    <div style="height:40%">
      <!-- 水平分隔线 -->
      <div style="width: 100%">
        <hr style="border: none; border-top: 1px solid rgb(199, 196, 196)" />
      </div>
      <!-- 下部 chat-input 区域 -->
      <div style="display: flex; align-items: center;">
        <div style="width: 20%; font-size: 13px; color:#595959">
          请选择己配置的会话进行聊天:
        </div>
        <div style="width: 80%;">
          <a-select v-model:value="selectedConfigMainId" style="width: 100%" placeholder="请选择知识库"
            :disabled="isThinking">
            <a-select-option v-for="config in chatConfigList" :key="config.id" :value="config.id">
              {{ config.description }}
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div style="font-size: 13px; color: #595959; margin-top: 10px;">
        请输入您的问题:
      </div>
      <div style="margin: 0; padding: 0; margin-bottom: 0px">
        <div class="textarea-wrapper" style="margin: 0; padding: 0">
          <a-textarea v-model:value="userPrompt" placeholder="请输入您的问题，注意不要输入敏感词内容（政治相关、色情、血腥、暴恐等）。"
            :auto-size="{ minRows: 5, maxRows: 5 }" :disabled="isThinking" class="user-prompt" />
        </div>
        <div style="width: 100%; padding: 0; margin-top:0px;">
          <div
            style="display: flex;  align-items: center; width: 100%; margin-top:3px; justify-content: space-between;">
            <!-- 左侧按钮组 -->
            <div style="display: flex;">
              <img v-show="isThinking" src="/assets/images/waiting-04.gif" alt="思考中"
                style="margin-left: 10px; vertical-align: middle; width: auto; height: 55px;" />
              <div>
                <a-button type="text" @click="newSession" :disabled="isThinking">
                  <FileTextOutlined />
                  新起一个对话
                </a-button>
              </div>
              <div v-if="moreData">
                <a-button type="text" @click="chatNextPage()">
                  <RightOutlined />
                  换一批数据
                </a-button>
              </div>
            </div>
            <div style="margin-top: 10px;  margin-left: 5px;">
              <template v-if="isThinking">
                <a-button type="primary" class="submit-btn" @click="stopChat"
                  style="background-color: red; color: white;  margin-left: auto; margin-right: 5px; margin-top:3px;">
                  <template #icon>
                    <StopOutlined />
                  </template>
                  停止
                </a-button>
              </template>
              <a-button type="primary" :disabled="isThinking" class="submit-btn" @click="handlePromptSubmit"
                style="background-color: #5694F6;  margin-left: auto; margin-right: 5px; margin-top:3px;">
                <template #icon>
                  <CommentOutlined />
                </template>
                提问
              </a-button>
            </div>
          </div>
          <div
            style="display: flex; justify-content: center;align-items: center; font-size: 12px; color: #AEAEAE; padding:0; margin-top:0px;">
            AI也可能会犯错，请核查重要信息。
          </div>
        </div>
      </div>

    </div>
  </div>
</template>
<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { message } from "ant-design-vue";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import UserChatConfigApi from "@/api/UserChatConfigApi.js";
import ChatApi from "@/api/ChatApi.js";
import ChatBotMessage from "@/viewer/chatbot/ChatBotMessage.vue";
import { Modal } from 'ant-design-vue';
const isQuestionSubmitted = ref(false);
const isThinking = ref(false);
const selectedConfigMainId = ref(null);
const messages = ref([]);
const chatConfigList = ref([]);
const userPrompt = ref('');
const oldPrompt = ref('');
const moreData = ref(false);
const nextPage = ref(false);

const chatMessageContainer = ref(null);

const newSession = async () => {
  try {
    moreData.value = false;
    nextPage.value = false;
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "configMainId": selectedConfigMainId.value,
    }
    // 遍历 chatConfigList
    for (let config of chatConfigList.value) {
      let payload = {
        "token": encryptToken,
        "userName": userName,
        "configMainId": config.id, // 使用当前记录的 id
      }
      const res = await ChatApi.newSession(payload);
    }
  } catch (err) {
    console.error(">>>>>>新起一个会话错误", err);
  } finally {
    userPrompt.value = '';
    messages.value = [];
  }
}

const getChatConfigList = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
    }
    const res = await UserChatConfigApi.getChatConfigListByUserName(payload);
    chatConfigList.value = res;
  } catch (err) {
    console.error(">>>>>>getChatConfigList error:", err);
  }
}
const stopChat = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
    }
    const res = await ChatApi.stopChat(payload);
    isThinking.value = false;
    loading.value = false;
    nextPage.value = false;
  } catch (err) {
    console.error(">>>>>>stopChat error:", err);
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatMessageContainer.value) {
      chatMessageContainer.value.scrollTop = chatMessageContainer.value.scrollHeight;
    }
  });
};

const chatNextPage = async () => {
  nextPage.value = true;
  userPrompt.value = String(oldPrompt.value);
  await handlePromptSubmit();
}
const handlePromptSubmit = async () => {
  console.log("selectedConfigMainId->" + selectedConfigMainId.value);
  if (!selectedConfigMainId.value) {
    message.warn("请选择一个己配置的会话才能进入聊天");
    return;
  }
  if (userPrompt.value.trim() !== '') {
    isThinking.value = true;
    isQuestionSubmitted.value = true;
    // 添加用户消息
    // 添加用户消息
    messages.value.push({
      isUser: true,
      content: userPrompt.value.trim()
    });
    try {
      oldPrompt.value = String(userPrompt.value);
      // 添加一个空的 AI 回复消息
      const aiMessageIndex = messages.value.length;
      messages.value.push({
        isUser: false,
        content: ''
      });
      scrollToBottom();
      // SSE 调用
      let token = authorization.getToken();
      let encryptToken = encrypt_url(token);
      let userName = authorization.getUserName();
      let params = {
        inputPrompt: userPrompt.value,
        configMainId: selectedConfigMainId.value,
        nextPage: nextPage.value,
        userName: userName,
        token: encryptToken,
      };
      ChatApi.complete(params, {
        onStart() {
        },
        onError() {
          isThinking.value = false;
          message.error(msg.error);

          //loading.value = false;
        },
        onClose() {
          isThinking.value = false;
          try {
            ChatApi.hasMoreData(params).then(res => {
              //console.log(JSON.stringify(res));
              moreData.value = res;
            }).catch(err => {
              console.error(">>>>>>hasMore data api error", err);
              moreData.value = false;
            });
          } catch (err) { }
        },
        onMessage(data) {
          if (null == data || data == undefined || data == "") {
            return;
          }
          // data 已经是字符串形式，不需要再次 JSON.stringify
          let msg;
          try {
            msg = JSON.parse(data);
          } catch (error) {
            console.warn('JSON解析失败:', error);
            msg = {}; // 设置为空对象
          }
          if (msg.error && msg.error != "") {
            message.error(msg.error);
            return;
          }
          if (msg.done === true) {
            isThinking.value = false;
            return;
          }
          let content = msg.data || "";
          if (msg.dataIds) {
            let dataIds = msg.dataIds;
            console.log("dataIds类型:", typeof dataIds);
            console.log("是否为数组:", Array.isArray(dataIds));
            console.log("数组长度:", dataIds.length);
            console.log(">>>>>>dataIds->" + dataIds);
          }
          messages.value[aiMessageIndex].content += content;
          scrollToBottom();
        }
      }, encryptToken, userName).then(res => {
        // 清空输入并设置加载状态
        const userInput = userPrompt.value.trim();
        userPrompt.value = '';
        //console.log(res)
      }).catch(err => {
        console.log(err);
      }).finally(() => {
        loading.value = false;
        nextPage.value = false;
      })
    } catch (error) {
      console.error('获取AI回复时出错:', error);
      messages.value.push({
        isUser: false,
        content: '抱歉，获取回复时出现错误。请稍后再试。'
      });
      isThinking.value = false;
      scrollToBottom();
    }
  }
};
onMounted(async () => {
  await getChatConfigList();
  await newSession();
});
</script>
<style scoped>
:deep(.ant-btn) {
  height: auto !important;
}

.center {
  width: 100%;
  height: 100%;
  overflow: hidden;
  padding: 0 20px;
  /* 如果需要两边留出一些间距 */
  box-sizing: border-box;
}

.display-content {
  height: 60%;
  overflow-y: auto;
  padding: 0;
  margin: 0 auto;
  width: 100%;
  /* 设置为100%宽度 */
  box-sizing: border-box;
  /* 确保padding不会影响总宽度 */
}


.center .account {
  display: inline-block;
  background-color: #27b5f5;
  padding: 6px 12px;
  border-radius: 4px;
  color: #ffffff;
  margin-top: 18px;
  vertical-align: top;
  margin-right: 5px;
}

.content {
  background: #F2F2F6;
  margin: 0 auto;
  /*width: calc(100vw - 12px);*/
  width: 100%;
  height: calc(100vh - 93px);
}

.chat-container {
  height: 100%;
  margin: 0;
  padding: 0;
  border: 0px solid #e8e8e8;
}

.system-setting {
  height: 100%;
  background-color: #e8e8e8;
}

.system-setting::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 1px;
  border: 0px solid #e8e8e8;
}

.divider-col {
  height: 100%;
  display: flex;
  justify-content: center;
  padding: 0;
  margin: 0;
}

.full-height-divider {
  height: 100%;
  padding: 0;
  margin: 0;
}

.chat-area {
  height: 100%;
  display: flex;
  flex-direction: column;
}


.chat-input {
  height: 40%;
  padding: 0;
  margin: 0;
  margin-top: 0px;
}

.area-content {
  padding: 0;
  margin: 0;
  /*display: flex;*/
  /*justify-content: center;*/
  align-items: center;
  height: 100%;
  width: 100%;
  font-size: 18px;
}

.user-prompt {
  border-radius: 8px;
  margin: 5px;
  width: calc(100% - 10px);
}

.textarea-wrapper {
  margin-top: 0px;
}

.submit-btn {
  align-self: flex-end;
  margin-top: 10px;
  margin-left: 5px;
}

.welcome-message {
  margin-top: 15%;
  text-align: center;
}

.assistant-box {
  display: inline-block;
  border: 0px solid #e8e8e8;
  padding: 8px 3px;
  font-size: 12px;
  text-align: center;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  width: 110px;
  margin-top: 5px;
}

.assistant-box:hover {
  border-color: #40a9ff;
}

.assistant-box.active {
  border: 0px solid #e8e8e8;
  border-color: #40a9ff;
  color: #40a9ff;
}

.fullscreen-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.fullscreen-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
}

.close-button {
  position: absolute;
  top: 20px;
  right: 20px;
  background: transparent;
  border: none;
  color: white;
  font-size: 30px;
  cursor: pointer;
  padding: 10px;
}

.close-button:hover {
  opacity: 0.8;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 自定义折叠面板样式 */
.system-setting {
  transition: all 0.3s ease;
}

:deep(.ant-collapse-ghost) {
  background: transparent;
}

:deep(.ant-collapse-header) {
  padding: 8px 0 !important;
}

:deep(.ant-collapse-content) {
  padding: 0;
}

/* 确保按钮始终可见 */
.ant-btn-link {
  background: transparent !important;
  padding: 4px 8px;
}

.ant-btn-link:hover {
  background: rgba(0, 0, 0, 0.05) !important;
}

/**上传按钮样式 */
.upload-btn-wrapper {
  display: flex;
  align-items: center;
  margin-left: 10px;
}

.upload-btn {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.plus-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #e6f7ff;
  color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  margin-right: 8px;
  position: relative;
  /* 添加相对定位 */
}

/* 添加虚线圆圈 */
.plus-icon::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border: 1px dashed #1890ff;
  border-radius: 50%;
}



.upload-text {
  color: #666;
  font-size: 14px;
  text-decoration: underline;
}
</style>