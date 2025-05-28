<template>
  <div style="display: flex; width: 100%;">
    <!-- 左侧区域 60% -->
    <div style="width: 60%; padding-right: 20px;">
      <div style="font-size: 13px; color: #595959; display: flex; align-items: center;">
        <div>
          全局聊天参数配置
        </div>
        <div>
          <span style="font-size: 13px; color: darkgrey;">({{ getUpdateTypeText(updateType) }})</span>
        </div>
        <div>
          <a-button type="text" size="small" @click="handleRefreshConfig()">
            <SyncOutlined />刷新
          </a-button>
        </div>
      </div>

      <div style="display: flex; margin-top: 20px; justify-content: space-between;">
        <div style="display: flex;">
          <a-button type="primary" style="background-color: #7171FB; font-size: 13px; border: none;"
            @click="handleSaveConfig">
            <SaveOutlined />保存设置
          </a-button>
          <a-button type="primary" style="background-color: #7171FB; font-size: 13px; border: none; margin-left: 10px;"
            @click="handleBacMgt">
            <CloudOutlined />备份
          </a-button>
        </div>
        <div>
          <a-button type="primary" style="background-color: #7171FB; font-size: 13px; border: none;"
            @click="handleBacMgt">
            <CloudSyncOutlined />恢复
          </a-button>
        </div>
      </div>

      <!-- 分隔线 -->
      <div style="width: 100%; height: 1px; background-color: #D9D9D9; margin: 20px 0;"></div>

      <!-- 配置列表 -->
      <div style="font-size: 13px; color: #595959; width: 100%;">
        <div style="cursor: pointer; display: flex; margin-top: 0px; margin-bottom: 20px; align-items: center;"
          @click="onHandleAddConfig()">
          <div
            style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
            <PlusOutlined style="color: blue; font-size: 13px;" />
          </div>
          <span style="color: #595959; margin-left: 10px; font-size:13px;">添加一个配置</span>
        </div>

        <div v-if="chatConfigList && chatConfigList.length > 0" style="width: 100%;">
          <span style="font-size: 13px; color: #595959;">当前己有配置: </span>
          <div v-for="(config, index) in chatConfigList" :key="index">
            <div style="display: flex; width: 100%;">
              <div
                style="margin-bottom: 10px; margin-top: 10px; font-size: 13px; color: blue; text-decoration: underline; cursor: pointer; width: 40%;"
                @click="onHandleUpdateConfig(config)">
                {{ index + 1 }}. {{ config.description }}
              </div>
              <div style="width: 60%">
                <a-button type="text" style="font-size: 14px; color: red;" @click="handleDeleteConfig(config)">
                  <MinusCircleOutlined />
                  删除
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分隔线 -->
      <div style="width: 100%; height: 1px; background-color: #D9D9D9; margin: 10px 0;"></div>

      <!-- 聊天时全局系统角色描述 -->
      <div style="margin-top: 10px;">
        <div style="display: flex; align-items: center;">
          <div style="width: 30%; font-size: 13px; color: #595959;">
            配置描述
          </div>
          <div style="width: 70%;">
            <a-input v-model:value="description" autocomplete="off" placeholder="必填，配置的描述"
              style="width: 100%; color:#595959;" />
          </div>
        </div>
        <div style="display: flex; align-items: center; margin-top: 10px;">
          <div style="width: 30%; font-size: 13px; color: #595959;">
            temperature设置
          </div>
          <div style="width: 60%;">
            <a-slider v-model:value="globalTemperature" :min="0.1" :max="2.0" :step="0.1" style="width: 90%;" />
          </div>
          <div style="width: 10%; font-size: 13px; color: #595959;">
            <a-input :value="globalTemperature.toFixed(1)" autocomplete="off" @change="handleTemperatureChange"
              style="width: 100%; color:#595959;" />
          </div>
        </div>
        <div style="display: flex; margin-top: 10px;">
          <div style="width: 30%; font-size: 13px; color: #595959;">
            使用知识库(暂时只支持单选)
          </div>
          <div style="width: 70%;">
            <a-select v-model:value="selectedKnowledgeRepoId" style="width: 100%" placeholder="请选择知识库">
              <a-select-option value="">--请选择--</a-select-option>
              <a-select-option v-for="repo in knowledgeRepoList" :key="repo.id" :value="repo.id">
                {{ repo.knowledgeName }}
              </a-select-option>
            </a-select>
          </div>
        </div>
        <div style="display: flex; align-items: center; margin-top: 10px;">
          <div style="width: 30%; font-size: 13px; color: #595959;">
            允许参与聊天的用户数
          </div>
          <div style="width: 70%; font-size: 13px; color:blue; margin-left: 20px;">
            <span style="font-size: 13px; color:blue; cursor: pointer; text-decoration: underline;"
              @click="handleUserSelect()">
              {{ allowUserCount }}个
            </span>
          </div>
        </div>
        <div style="font-size: 13px; color: #595959; margin-bottom: 10px; margin-top: 20px;">聊天时全局系统角色描述</div>
        <a-textarea v-model:value="systemMsg" rows="15" style="width: 100%;" />
      </div>
    </div>

    <!-- 右侧区域 40% -->
    <div style="width: 40%; padding-left: 20px;">
      <div style="font-size: 13px; color: #595959; display: flex; align-items: center;">
        <div>
          配置提示词重写
        </div>
      </div>
      <div style="margin-top: 20px;">
        <a-table :columns="difyColumns" :data-source="rewriteDifyConfigList" :pagination="rewriteDifyPagination"
          :row-selection="{
            selectedRowKeys: rewriteSelectedRowKeys,
            onChange: onRewriteSelectChange,
            type: 'radio'  // 明确指定checkbox类型
          }" :row-class-name="rewriteGetRowClassName" style="border: 1px solid #47D45A;"
          @change="handleRewriteTableChange">
        </a-table>
      </div>
      <div style="font-size: 13px; color: #595959; display: flex; align-items: center; margin-top: 20px;">
        <div>
          配置提对话流程
        </div>
      </div>
      <div style="margin-top: 20px;">
        <a-table :columns="difyColumns" :data-source="chatDifyConfigList" :pagination="chatDifyPagination"
          :row-selection="{
            selectedRowKeys: chatSelectedRowKeys,
            onChange: onChatSelectChange,
            type: 'radio'  // 明确指定checkbox类型
          }" :row-class-name="chatGetRowClassName" style="border: 1px solid #47D45A;" @change="handleChatTableChange">
        </a-table>
      </div>
    </div>
    <PopUserSelect v-model="popUserSelectShow" :hasSelectedUserIds="allowUsers"
      @refresh-allowUsersNumber="handleRefreshAllowUserNumbers" />
    <PopBackMgt v-model="popBackMgtShow" @refresh-config="handleRefreshConfig" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { message } from "ant-design-vue";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import UserChatConfigApi from "@/api/UserChatConfigApi.js";
import DifyConfigApi from "@/api/DifyConfigApi.js";
import { Modal } from 'ant-design-vue';
import PopUserSelect from '@/viewer/users/PopUserSelect.vue';
import PopBackMgt from '@/viewer/users/PopBackMgt.vue';



const chatConfigList = ref([]);
const currentMainConfigId = ref(0);
const description = ref('');
const allowUsers = ref([]);
const allowUserCount = ref(0);
const knowledgeRepoList = ref([]);
const systemMsg = ref('');
const globalTemperature = ref(0.1);
const selectedKnowledgeRepoId = ref('');
const popUserSelectShow = ref(false);
const popBackMgtShow = ref(false);
const popStepTitle = ref('');
const stepEnabled = ref(true);
const stepPrompt = ref('');
const stepTemperature = ref(0.1);
const stepType = ref(0);

const rewriteDifyConfigList = ref([]);
const chatDifyConfigList = ref([]);
const rewriteDifySequenceNo = ref('');
const rewriteDifyDescription = ref('');
const rewriteDifyUser = ref('');
const rewriteDifyId = ref('');
const rewriteDifyResponseMode = ref('blocking');
const rewriteDifyApiKey = ref('');
const rewriteSelectedDifySequenceNo = ref('');
const chatSelectedDifySequenceNo = ref('');
const rewriteDifyPagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
});
const chatDifyPagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
});
const rewriteSelectedRowKeys = ref([]);
const chatSelectedRowKeys = ref([]);
const difyColumns = [
  {
    title: '编号',
    dataIndex: 'sequenceNo',
    key: 'sequenceNo',
    width: 60,
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
  },

];

// 修改选择回调函数，添加key字段判断
const onRewriteSelectChange = (selectedRowKey, selectedRows) => {
  rewriteSelectedRowKeys.value = selectedRowKey;
  rewriteSelectedDifySequenceNo.value = selectedRowKey[0];
};
const onChatSelectChange = (selectedRowKey, selectedRows) => {
  chatSelectedRowKeys.value = selectedRowKey;
  chatSelectedDifySequenceNo.value = selectedRowKey[0];
};
// 修改行类名判断逻辑
const rewriteGetRowClassName = (record, index) => {
  //console.log(">>>>>>sequenceNo->"+record.sequenceNo);
  const baseClass = index % 2 === 0 ? 'even-row' : 'odd-row';
  return rewriteSelectedRowKeys.value.includes(record.sequenceNo) ? `${baseClass} selected-row` : baseClass;
};
// 修改行类名判断逻辑
const chatGetRowClassName = (record, index) => {
  //console.log(">>>>>>sequenceNo->"+record.sequenceNo);
  const baseClass = index % 2 === 0 ? 'even-row' : 'odd-row';
  return chatSelectedRowKeys.value.includes(record.sequenceNo) ? `${baseClass} selected-row` : baseClass;
};
const handleRewriteTableChange = (pag) => {
  rewriteDifyPagination.value.current = pag.current;
  listAllRewriteDifyConfigs(pag.current);
};
const handleChatTableChange = (pag) => {
  chatDifyPagination.value.current = pag.current;
  listAllChatDifyConfigs(pag.current);
};
const listAllRewriteDifyConfigs = async (page) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "pageNumber": rewriteDifyPagination.value.current,
      "pageSize": rewriteDifyPagination.value.pageSize,
    }
    DifyConfigApi.listAllDifyConfigs(payload).then(async res => {
      rewriteDifyConfigList.value = res.content.map(item => ({
        ...item,
        key: item.sequenceNo // 确保每条数据都有唯一的key
      }));

      rewriteDifyPagination.value.total = res.totalElements;
      // 如果存在selectedDifySequenceNo，自动选中对应行
      if (rewriteSelectedDifySequenceNo.value && rewriteSelectedDifySequenceNo.value.trim() !== '') {
        rewriteSelectedRowKeys.value = [rewriteSelectedDifySequenceNo.value];
      } else {
        rewriteSelectedRowKeys.value = []; // 清空选中状态
      }
    }).catch(err => {
      console.error("获取DifyConfigList失败", err);
    });
  } catch (err) {
    console.error('获取DifyConfigList失败:', err);
  }
};
const listAllChatDifyConfigs = async (page) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "pageNumber": chatDifyPagination.value.current,
      "pageSize": chatDifyPagination.value.pageSize,
    }
    DifyConfigApi.listAllDifyConfigs(payload).then(async res => {
      chatDifyConfigList.value = res.content.map(item => ({
        ...item,
        key: item.sequenceNo // 确保每条数据都有唯一的key
      }));

      chatDifyPagination.value.total = res.totalElements;
      // 如果存在selectedDifySequenceNo，自动选中对应行
      if (chatSelectedDifySequenceNo.value && chatSelectedDifySequenceNo.value.trim() !== '') {
        chatSelectedRowKeys.value = [chatSelectedDifySequenceNo.value];
      } else {
        chatSelectedRowKeys.value = []; // 清空选中状态
      }
    }).catch(err => {
      console.error("获取DifyConfigList失败", err);
    });
  } catch (err) {
    console.error('获取DifyConfigList失败:', err);
  }
};
const getUpdateTypeText = (currentVal) => {
  if (currentVal === 1) {
    return "新增配置模式";
  } else if (currentVal === 2) {
    return "修改配置模式";
  } else {
    return "浏览模型"
  }
}

const handleTemperatureChange = (e) => {
  let value = parseFloat(e.target.value);

  // 确保是数字且在有效范围内
  if (isNaN(value)) {
    value = 0.1;
  }

  // 限制在0.1到2.0之间
  value = Math.max(0.1, Math.min(2.0, value));

  // 格式化为1位小数
  value = Math.round(value * 10) / 10;

  globalTemperature.value = value;
};

const handleUserSelect = () => {
  try {
    popUserSelectShow.value = true;
    console.log(">>>>>>进入用户选择");
  } catch (err) {
    console.error(">>>>>>popUserSelect error:", err);
  }
}

const handleRefreshAllowUserNumbers = (selectedUsers) => {
  allowUsers.value = selectedUsers;
  allowUserCount.value = allowUsers.value.length;
  console.log(">>>>>>selected allowUsers->" + JSON.stringify(allowUsers.value));
  console.log(">>>>>>allowUserCount->" + allowUserCount.value);
}

const resetChatConfigMain = async () => {
  try {
    currentMainConfigId.value = await getChatConfigMainId();
    await getAllKnowledgeRepo();
    description.value = '';
    allowUsers.value = [];
    systemMsg.value = '';
    globalTemperature.value = 0.1;
    selectedKnowledgeRepoId.value = '';
    allowUserCount.value = 0;
    rewriteSelectedDifySequenceNo.value = '';
    chatSelectedDifySequenceNo.value = '';
  } catch (err) {
    console.error(">>>>>>resetChatConfigMain error->", err);
  }
}

const getAllKnowledgeRepo = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
    }
    const res = await UserChatConfigApi.getAllKnowledgeRepo(payload);
    knowledgeRepoList.value = res;
  } catch (err) {
    console.error(">>>>>>initUserChatConfig error:", err);
  }
}

const handleDeleteConfig = async (config) => {
  Modal.confirm({
    title: '确认删除',
    content: '当前配置如果正应用于对话，删除配置会直接导致该对话失效，请确认是否需要删除?',
    okText: '确定删除',
    cancelText: '取消删除',
    async onOk() {
      try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
          "token": encryptToken,
          "userName": userName,
          "configMainId": config.id,
        }
        await UserChatConfigApi.deleteConfig(payload);
        message.success("删除成功");
      } catch (err) {
        message.error("删除失败");
        console.error(">>>>>>删除失败->", err);
      } finally {
        await getChatConfigList();
        //await initUserChatConfig();
        await getAllKnowledgeRepo();
        await resetChatConfigMain();
        await listAllRewriteDifyConfigs();
        await listAllChatDifyConfigs()
        updateType.value = 0;
      }
    }
  });
}

const updateType = ref(0);
const onHandleAddConfig = async () => {
  if (updateType && updateType.value > 0) {
    let updateText = getUpdateTypeText(updateType.value);
    let confirmContentStr = "当前您正处于[" + updateText + "]，请确认您己保存了刚才的设置"
    Modal.confirm({
      title: '新增配置提示',
      content: confirmContentStr,
      cancelText: '留在当前页面',
      okText: '放弃保存继续新增',
      width: '800px', // 设置宽度
      style: {
        top: '20px' // 调整位置，让内容区域能显示更多
      },
      // 自定义弹窗内容的样式
      bodyStyle: {
        maxHeight: '400px',
        overflow: 'auto',
        padding: '24px'
      },
      async onOk() {
        updateType.value = 1;
        //await initUserChatConfig();
        await resetChatConfigMain();
        await listAllRewriteDifyConfigs();
        await listAllChatDifyConfigs();
      }
    });
  } else {
    updateType.value = 1;
    //await initUserChatConfig();
    await resetChatConfigMain();
    await listAllRewriteDifyConfigs();
    await listAllChatDifyConfigs();
  }
}

const onHandleUpdateConfig = async (config) => {

  try {
    rewriteSelectedDifySequenceNo.value = '';
    chatSelectedDifySequenceNo.value = '';
    if (updateType && updateType.value > 0) {
      let updateText = getUpdateTypeText(updateType.value);
      let confirmContentStr = "当前您正处于[" + updateText + "]，请确认您己保存了刚才的设置"
      Modal.confirm({
        title: '新增配置提示',
        content: confirmContentStr,
        cancelText: '留在当前页面',
        okText: '放弃保存继续新增',
        width: '800px', // 设置宽度
        style: {
          top: '20px' // 调整位置，让内容区域能显示更多
        },
        // 自定义弹窗内容的样式
        bodyStyle: {
          maxHeight: '400px',
          overflow: 'auto',
          padding: '24px'
        },
        async onOk() {
          updateType.value = 2;
          let token = authorization.getToken();
          let encryptToken = encrypt_url(token);
          let userName = authorization.getUserName();
          let payload = {
            "token": encryptToken,
            "userName": userName,
            "configMainId": config.id,
          }
          const res = await UserChatConfigApi.getChatConfigByMainId(payload);
          currentMainConfigId.value = res.mainConfigId;
          description.value = res.description;
          globalTemperature.value = res.temperature;
          selectedKnowledgeRepoId.value = res.knowledgeRepoIdList[0];
          allowUsers.value = res.allowUsers;
          allowUserCount.value = allowUsers.value.length;
          systemMsg.value = res.systemMsg;
          rewriteSelectedDifySequenceNo.value = res.rewriteSelectedDifySequenceNo;
          chatSelectedDifySequenceNo.value = res.chatSelectedDifySequenceNo;
          await listAllRewriteDifyConfigs();
          await listAllChatDifyConfigs();
        }
      });
    } else {
      updateType.value = 2;
      let token = authorization.getToken();
      let encryptToken = encrypt_url(token);
      let userName = authorization.getUserName();
      let payload = {
        "token": encryptToken,
        "userName": userName,
        "configMainId": config.id,
      }
      const res = await UserChatConfigApi.getChatConfigByMainId(payload);
      currentMainConfigId.value = res.mainConfigId;
      description.value = res.description;
      globalTemperature.value = res.temperature;
      selectedKnowledgeRepoId.value = res.knowledgeRepoIdList[0];
      allowUsers.value = res.allowUsers;
      allowUserCount.value = allowUsers.value.length;
      systemMsg.value = res.systemMsg;
      rewriteSelectedDifySequenceNo.value = res.rewriteSelectedDifySequenceNo;
      chatSelectedDifySequenceNo.value = res.chatSelectedDifySequenceNo;
      await listAllRewriteDifyConfigs();
      await listAllChatDifyConfigs();
    }
  } catch (err) {
    console.error(">>>>>>onHandleUpdateConfig error", err);
  }
}

const handleBacMgt = async () => {
  if (updateType && updateType.value > 0) {
    let updateText = getUpdateTypeText(updateType.value);
    let confirmContentStr = "当前您正处于[" + updateText + "]，请保存当前配置后才能使用备份或恢复操作"
    message.warn(confirmContentStr)
    return;
  }
  popBackMgtShow.value = true;
}

const handleSaveConfig = async () => {
  // 实现保存配置的逻辑
  try {
    if (updateType.value === 0) {
      message.warn("当前为浏览模式不可保存，您可以切换新增或者是更新模式");
      return;
    }
    console.log("currentMainConfigId->" + JSON.stringify(currentMainConfigId.value));
    if (!currentMainConfigId.value || currentMainConfigId.value == 0) {
      message.warn("保存失败，配置有误");
      return
    }
    if (!description || description.value.trim().length === 0) {
      message.warn("描述字段必填");
      return
    }
    if (!selectedKnowledgeRepoId.value || selectedKnowledgeRepoId.value.trim().length === 0) {
      message.warn("您还没有选择哪个知识库会被用在此聊天上");
      return;
    }
    if (allowUserCount.value < 1 || allowUsers.value.length < 1) {
      message.warn("请选择参与聊天的用户");
      return
    }
    if (!systemMsg.value || systemMsg.value.trim().length === 0) {
      message.warn("全局系统角色描述必填");
      return
    }
    //组装一个config的完整payload
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "configMainId": currentMainConfigId.value,
      "description": description.value,
      "systemMsg": systemMsg.value,
      "temperature": globalTemperature.value,
      "knowledgeRepoIdList": [selectedKnowledgeRepoId.value], // 将单个值包装成数组
      "allowUsers": Array.from(allowUsers.value),
      "rewriteSelectedDifySequenceNo": rewriteSelectedDifySequenceNo.value,
      "chatSelectedDifySequenceNo": chatSelectedDifySequenceNo.value

    }
    console.log(">>>>>>传给后台前打印输出一下payload值如下:");
    console.log(payload);
    if (updateType.value === 1) { //处理新增
      const res = await UserChatConfigApi.saveConfig(payload);
      console.log(">>>>>>saveConfig res->" + JSON.stringify(res));
      if (res === 1) {
        message.success("新增配置成功");
        await getChatConfigList();
        //await initUserChatConfig();
        await getAllKnowledgeRepo();
        await resetChatConfigMain();
      } else if (res === -1) {
        message.success("新增失败");
      }
      updateType.value = 0;
    } else if (updateType.value === 2) {//处理更新
      const res = await UserChatConfigApi.updateConfig(payload);
      console.log(">>>>>>updateConfig res->" + JSON.stringify(res));
      if (res === 1) {
        message.success("更新配置成功");

      } else if (res === -1) {
        message.success("更新配置失败");
      }
      updateType.value = 0;
    } else {
      message.warn("当前为浏览模式不可保存，您可以切换新增或者是更新模式");
    }
  } catch (err) {
    console.error(">>>>>>更新配置失败->", err);
  } finally {

  }
};


const handleRefreshConfig = async () => {
  try {
    await getChatConfigList();
    //await initUserChatConfig();
    await getAllKnowledgeRepo();
    await resetChatConfigMain();
    updateType.value = 0;
  } catch (err) {
    console.error(">>>>>>handleRefreshConfig error:", err);
  }
};

const getChatConfigMainId = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
    }
    const res = await UserChatConfigApi.getChatConfigMainId(payload);
    if (res) {
      return res;
    }
  } catch (err) {
    console.error(">>>>>>getChatConfigMainId error:", err);
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

onMounted(async () => {
  // 确保DOM元素存在
  await getChatConfigList();
  //await initUserChatConfig();
  await getAllKnowledgeRepo();
  await resetChatConfigMain();
  await listAllRewriteDifyConfigs();
  await listAllChatDifyConfigs();
  
});

</script>

<style scoped>
.even-row {
  background-color: #ffffff;
}

.odd-row {
  background-color: #fafafa;
}

:deep(.ant-table-row:hover > td) {
  background-color: #e6f7ff !important;
}

:deep(.ant-table) {
  font-size: 12px;
  color: #595959;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #ffffff;
  border-bottom: 1px solid #47D45A;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #47D45A;
}

.selected-row {
  background-color: #f5f5f5 !important;
  /* 选中行的浅灰色背景 */
}

:deep(.ant-table-row.selected-row:hover > td) {
  background-color: #f0f0f0 !important;
  /* 选中行hover时的背景色 */
}

/* 覆盖全选时的背景色 */
:deep(.ant-table-tbody > tr.ant-table-row-selected > td) {
  background-color: #f5f5f5 !important;
}

:deep(.ant-table-tbody > tr.ant-table-row-selected:hover > td) {
  background-color: #f0f0f0 !important;
}
</style>