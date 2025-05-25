<template>
  <div style="display: flex; flex-direction: column;">
    <div style="width:100%; display: flex; justify-content: space-between;">
      <!--添加AI函数按钮-->
      <div style="cursor: pointer; display: flex; margin-top: 0px;  margin-bottom: 20px; align-items: center; "
        @click="handleAddDifyConfig()">
        <div
          style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
          <PlusOutlined style="color: blue; font-size: 13px;" />
        </div>
        <span style="color: #595959;  margin-left: 10px;  font-size:13px;">添加一个Dify工作流</span>
      </div>
      <!--刷新按钮-->
      <div>
        <a-button type="text" size="small" @click="handleRefreshDifyConfigList()">
          <SyncOutlined />刷新
        </a-button>
      </div>
    </div>
    <span style="font-weight: bold; font-size: 16px; color: #595959;">
      <div style="display: flex; justify-content: space-between;">
        <span style="font-weight: bold; font-size: 16px; color: #595959;">
          Dify工作流列表
        </span>
        <a-button type="primary" size="small"
          style="margin-right: 8px; background-color: red; border-color: red; color: #ffffff;"
          @click="handleDeleteSelectDifyConfig()">
          <template #icon>
            <DeleteOutlined />
          </template>
          删除
        </a-button>
      </div>
    </span>
    <div style="margin-top: 10px;">
      <a-table :columns="columns" :data-source="difyConfigList" :pagination="pagination" :row-selection="{
        selectedRowKeys: selectedRowKeys,
        onChange: onSelectChange,
        type: 'checkbox'  // 明确指定checkbox类型
      }" :row-class-name="getRowClassName" style="border: 1px solid #47D45A;" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'sequenceNo'">
            <a @click="handleEdit(record)" style="text-decoration: underline;">{{ record.sequenceNo }}</a>
          </template>
          <template v-if="column.dataIndex === 'apiKey'">
            ******
          </template>
        </template>
      </a-table>
    </div>
    <div v-if="isEditing">
      <div style="margin-top: 20px;">
        <span style="font-weight: bold; font-size: 16px; color: #595959;">
          函数编辑
        </span>
      </div>
      <div style="margin-top: 20px; width: 100%; display: flex; justify-content: flex-end;">
        <div style="display: flex;">
          <a-button type="primary" size="small"
            style="margin-right: 8px; background-color: #47D45A; border-color: #47D45A; color: #ffffff;"
            @click="handleUpdateDifyConfig()">
            <template #icon>
              <CheckOutlined />
            </template>
            保存
          </a-button>

          <a-button type="primary" size="small"
            style="margin-right: 8px; background-color: #F6C6AD; border-color: #F6C6AD; color: black; margin-left: 16px;"
            @click="handleCancelUpdate()">
            <template #icon>
              <StopOutlined />
            </template>
            取消
          </a-button>
        </div>
      </div>
      <!-- 下半部分 -->
      <div
        style="flex: 1; margin-top: 10px; border: 1px solid #47D45A; font-size: 13px; color: #595959; width:100%; align-items: center;">

        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
          <template v-if="editType === 2">
            <div style=" width: 20%;">
              编号(不可编辑)
            </div>
            <div style="width: 88%;">
              <span style="color: blue;">
                {{ itemCode }}
              </span>
            </div>
          </template>
          <template v-if="editType === 1">
            <div style=" width: 20%;">
              编号
            </div>
            <div style="width: 88%;">
              <a-input v-model:value="sequenceNo" autocomplete="off" placeholder="必输: dify工作流的编号"
                style="width: 50%; color:#595959" />
            </div>
          </template>
        </div>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
          <div style=" width: 20%;">
            描述
          </div>
          <div style="width: 88%;">
            <a-input v-model:value="description" autocomplete="off" placeholder="必输: 用于描述该dify工作流是做什么事的摘要"
              :maxlength="40" style="width: 50%; color:#595959" />
          </div>
        </div>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
          <div style=" width: 20%;">
            dify用户
          </div>
          <div style="width: 88%;">
            <a-input v-model:value="difyUser"   autocomplete="new-password" placeholder="必输: 用于描述该dify工作流用户" :maxlength="40"
              style="width: 50%; color:#595959" />
          </div>
        </div>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
          <div style=" width: 20%;">
            api_key
          </div>
          <div style="width: 88%;">
            <a-input-password v-model:value="apiKey"   autocomplete="new-password" placeholder="必输: 运行此工作流的apiKey" :maxlength="40"
              style="width: 50%; color:#595959" />
          </div>
        </div>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
          <div style=" width: 20%;">
            response_mode
          </div>
          <div style="width: 88%;">
            <a-radio-group v-model:value="responseMode">
              <a-radio value="blocking">blocking</a-radio>
              <a-radio value="streaming">streaming</a-radio>
            </a-radio-group>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import { message } from "ant-design-vue";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import { Modal } from 'ant-design-vue';
import DifyConfigApi from "@/api/DifyConfigApi.js";

const difyConfigList = ref([]);
const selectedRowKeys = ref([]);
const isEditing = ref(false);
const editType = ref(1);
const sequenceNo = ref(0);
const description = ref('');
const difyUser = ref('');
const difyId = ref('');
const responseMode = ref('blocking');
const apiKey = ref('');

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
});

const columns = [
  {
    title: '编号',
    dataIndex: 'sequenceNo',
    key: 'sequenceNo',
    width: 80,
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '用户',
    dataIndex: 'user',
    key: 'user',
  },
  {
    title: 'responseMode',
    dataIndex: 'responseMode',
    key: 'responseMode',
  },
  {
    title: 'apiKey',
    dataIndex: 'apiKey',
    key: 'apiKey',
  },
  {
    title: '创建日期',
    dataIndex: 'createdDate',
    key: 'createdDate',
  },
  {
    title: '修改日期',
    dataIndex: 'updatedDate',
    key: 'updatedDate',
  }
];
const handleAddDifyConfig = async (page) => {

  isEditing.value = true;
  editType.value = 1;
  difyId.value = '';
  sequenceNo.value = '';
  description.value = '';
  responseMode.value = 'blocking'
  difyUser.value = '';
  apiKey.value = '';
}

const listAllDifyConfigs = async (page) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "pageNumber": pagination.value.current,
      "pageSize": pagination.value.pageSize,
    }
    DifyConfigApi.listAllDifyConfigs(payload).then(async res => {
      difyConfigList.value = res.content.map(item => ({
        ...item,
        key: item.id // 确保每条数据都有唯一的key
      }));
      pagination.value.total = res.totalElements;
    }).catch(err => {
      console.error("获取DifyConfigList失败" + JSON.stringify(err));
    });
  } catch (err) {
    console.error('获取DifyConfigList失败:' + JSON.stringify(err));
  }
};
const handleRefreshDifyConfigList = async () => {
  listAllDifyConfigs(1);
}
const handleEdit = (record) => {
  try {
    difyId.value = record.id;
    sequenceNo.value = record.sequenceNo;
    description.value = record.description;
    responseMode.value = record.responseMode;
    difyUser.value = record.user;
    apiKey.value = record.apiKey;
    isEditing.value = true;
    editType.value = 2;
  } catch (err) {
    console.error(">>>>>>把单条DIFY Config内容载入编辑区域时发生错误->", err);
  }
};
const handleUpdateDifyConfig = async () => {

  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let encryptApiKey = encrypt_url(apiKey.value);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "difyId": difyId.value,
      "sequenceNo": sequenceNo.value,
      "description": description.value,
      "user": difyUser.value,
      "responseMode": responseMode.value,
      "apiKey": encryptApiKey,
    }
    if (editType.value === 1) { //新增保存
      if (sequenceNo && sequenceNo.value <= 0) {
        message.warn("编号必须是一个>0开始的整数");
        return;
      }
      await DifyConfigApi.addDifyConfig(payload).then(res => {
        if (res === 1) {
          message.success("保存成功");
        } else if (res === 2) {
          message.warn("相同编号的工作流已存在，不可重复设置。");
        } else {
          message.error("保存失败");
        }
      }).catch(err => {
        console.error(">>>>>>调用后台保存失败", err);
      });
    } else if (editType.value === 2) {
      await DifyConfigApi.updateDifyConfigs(payload).then(res => {
        message.success("保存成功");
      }).catch(err => {
        console.error(">>>>>>调用后台保存失败", err);
      });
    } else {
      return;
    }
  } catch (err) {
    console.error(">>>>>>save dify config error", err);
  } finally {
    listAllDifyConfigs(1);
  }
}

const handleCancelUpdate = async () => {
  editType.value = 0;
  isEditing.value = false;
  sequenceNo.value = 0;
  description.value = "";
  difyUser.value = "";
  responseMode.value = "blocking";
}
// 修改行类名判断逻辑
const getRowClassName = (record, index) => {
  const baseClass = index % 2 === 0 ? 'even-row' : 'odd-row';
  return selectedRowKeys.value.includes(record.id) ? `${baseClass} selected-row` : baseClass;
};

// 修改选择回调函数，添加key字段判断
const onSelectChange = (selectedKeys, selectedRows) => {
  selectedRowKeys.value = selectedKeys;
};

const handleDeleteSelectDifyConfig = async () => {
  try {
    if (selectedRowKeys.value.length === 0) {
      message.warning('请至少选择一条记录');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      content: '删除当前选中的Dify工作流会造成功能上的影响，请充分确认所选Dify工作流没有被用在相关功能上！',
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
            "idList": selectedRowKeys.value,
          }
          await DifyConfigApi.deleteDifyConfigs(payload);
          message.success("删除成功");
        } catch (err) {
          console.error(">>>>>>删除失败", err);
        } finally {
          // 无论成功还是失败都会执行
          await listAllDifyConfigs(1);
        }
      }
    });
  } catch (err) {
    message.error("删除失败");
    console.error(">>>>>>删除失败->" + err);
  }
}

onMounted(() => {
  listAllDifyConfigs(1);
});
</script>