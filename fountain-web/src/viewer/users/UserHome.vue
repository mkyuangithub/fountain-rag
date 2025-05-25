<template>
  <div>
    <!--添加增加和刷新按钮-->
    <div style="width:100%; display: flex; justify-content: space-between;">
      <div style="cursor: pointer; display: flex; margin-top: 0px;  margin-bottom: 20px; align-items: center; "
        @click="handleAddUser()">
        <div
          style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
          <PlusOutlined style="color: blue; font-size: 13px;" />
        </div>
        <span style="color: #595959;  margin-left: 10px;  font-size:13px;">添加一个用户</span>
      </div>
      <!--刷新按钮-->
      <div>
        <a-button type="text" size="small" @click="handleRefresh()">
          <SyncOutlined />刷新
        </a-button>
      </div>
    </div>
    <!-- 上半部分 -->
    <span style="font-weight: bold; font-size: 16px; color: #595959;">
      <div style="display: flex; justify-content: space-between;">
        <span style="font-weight: bold; font-size: 16px; color: #595959;">
          当前系统用户列表
        </span>
      </div>
    </span>
    <div style="display: flex; margin-top: 10px; align-items: center;">
      <div style="width:5%">
        <span style="font-size: 14px; color: dodgerblue;">搜索:</span>
      </div>
      <div style="width: 90%;">
        <a-input v-model:value="searchUserName" autocomplete="off" placeholder="按照用户名搜" allowClear
          style="width: 100%; color:#595959; margin-left: 10px;" />
      </div>
      <div style="width: 5%; margin-left: 10px;">
        <img src="/assets/images/search-blue.png" style="width: 24px; height: 24px; margin-left: 10px; cursor: pointer;"
          @click="handleSearchUser()">
      </div>
    </div>
    <div style="margin-top: 20px;">
      <a-button type="primary" size="small"
        style="margin-right: 8px; background-color: red; border-color: red; color: #ffffff;"
        @click="handleDeleteSelectUser()">
        <template #icon>
          <DeleteOutlined />
        </template>
        删除
      </a-button>
    </div>
    <div style="margin-top: 10px;">
      <a-table :columns="columns" :data-source="userList" :pagination="pagination" :row-selection="{
        selectedRowKeys: selectedRowKeys,
        onChange: onSelectChange,
        type: 'checkbox'  // 明确指定checkbox类型
      }" :row-class-name="getRowClassName" style="border: 1px solid #47D45A;" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userName'">
            <a @click="handleEdit(record)" style="text-decoration: underline;">{{ record.userName }}</a>
          </template>
          <template v-if="column.dataIndex === 'type'">
            {{ getUserTypeText(record.type) }}
          </template>
        </template>
      </a-table>
    </div>
    <div v-if="isEditing">
      <div style="margin-top: 20px;">
        <template v-if="editType === 1">
          <span style="font-weight: bold; font-size: 16px; color: #595959;">
            用户新增
          </span>
        </template>
        <template v-if="editType === 2">
          <span style="font-weight: bold; font-size: 16px; color: #595959;">
            用户编辑
          </span>
        </template>
      </div>
      <div style="margin-top: 20px; width: 100%; display: flex; justify-content: flex-end;">
        <div style="display: flex;">
          <a-button type="primary" size="small"
            style="margin-right: 8px; background-color: #47D45A; border-color: #47D45A; color: #ffffff;"
            @click="handleUpdateUser()">
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
              用户名(不可编辑)
            </div>
            <div style="width: 88%;">
              <span style="color: blue;">
                {{ inputUserName }}
              </span>
            </div>
          </template>
          <template v-if="editType === 1">
            <div style=" width: 20%;">
              用户名
            </div>
            <div style="width: 88%;">
              <a-input v-model:value="inputUserName" autocomplete="off" placeholder="必输: 用户唯一标识，可以是用户的email地址"
                style="width: 50%; color:#595959" />
            </div>
          </template>
        </div>
        <template v-if="editType === 1">
          <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
            <div style=" width: 20%;">
              用户密码
            </div>
            <div style="width: 88%;">
              <a-input-password v-model:value="inputUserPassword" placeholder="非必输，用户密码" autocomplete="new-password"
                :maxlength="40" style="width: 50%; color:#595959" />
            </div>
          </div>
        </template>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: flex-start; margin-bottom: 10px;">
          <div style=" width: 20%;">
            选择用户类型
          </div>
          <div style="width: 88%;">
            <a-select v-model:value="inputUserType" style="width: 100%">
              <a-select-option :value="0">--请选择一个用户的类型--</a-select-option>
              <a-select-option :value="2">管理用户</a-select-option>
              <a-select-option :value="3">会话聊天用户(不具备任何管理功能)</a-select-option>
            </a-select>
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
import SystemUserMgtApi from "@/api/SystemUserMgtApi.js";
import { Modal } from 'ant-design-vue';
const isEditing = ref(false);
const editType = ref(1);
const userList = ref([]);
const selectedRowKeys = ref([]);
const itemId = ref('');
const searchUserName = ref('');
const inputUserName = ref('');
const inputUserType = ref(0);
const inputUserPassword = ref('');
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
});

const columns = [
  {
    title: '用户名',
    dataIndex: 'userName',
    key: 'userName',
    width: 200,
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type',
    width: 200,
    align: 'center',  // 添加居中对齐
  }
];
// 修改行类名判断逻辑
const getRowClassName = (user, index) => {
  const baseClass = index % 2 === 0 ? 'even-row' : 'odd-row';
  return selectedRowKeys.value.includes(user.id) ? `${baseClass} selected-row` : baseClass;
};

// 选择回调函数，添加key字段判断
const onSelectChange = (selectedKeys, selectedRows) => {
  selectedRowKeys.value = selectedKeys;
};

const handleDeleteSelectUser = async () => {
  try {
    if (selectedRowKeys.value.length === 0) {
      message.warning('请至少选择一条记录');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      content: '删除当前选中的用户会造成功能上的影响，请充分确认所选用户没有被用在建立相关知识库上！',
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
            "userIdList": selectedRowKeys.value,
          }
          await SystemUserMgtApi.deleteUser(payload);
          message.success("删除成功");
        } catch (err) {
          message.error("删除失败");
          console.error(">>>>>>删除失败->" + err);
        } finally {
          // 无论成功还是失败都会执行
          await listAllUsers(1);
        }
      }
    });
  } catch (err) {
    console.error(">>>>>>deleteUser error: " + err);
  }
}
const handleEdit = async (user) => {
  try {
    editType.value = 2;
    isEditing.value = true;
    inputUserName.value = user.userName;
    inputUserType.value = user.type;
    itemId.value = user.id;
  } catch (err) {
    console.error(">>>>>>edit user error" + err);
  }
}

const handleAddUser = async () => {
  try {
    editType.value = 1;
    isEditing.value = true;
    inputUserName.value = "";
    inputUserPassword.value = "";
    inputUserType.value = 0;
    itemId.value = "";
  } catch (err) {
    console.error(">>>>>>addUser error->" + err);
  }
}

const handleUpdateUser = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let encryptPassword = encrypt_url(inputUserPassword.value);
    let userName = authorization.getUserName();
    if (inputUserType.value === 0) {
      message.warn("必须选择一个用户的类型");
      return;
    }
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "inputUserName": inputUserName.value,
      "inputType": inputUserType.value,
      "userId": itemId.value,
      "inputPassword": encryptPassword,
    }
    if (editType.value === 1) {
      if (!inputUserPassword.value || inputUserPassword.value.length < 1) {
        message.warn("添加用户时用户密码必填");
        return;
      }
      await SystemUserMgtApi.addUser(payload).then(res => {
        if (res === 1) {
          message.success("添加用户成功");
        } else if (res === 101) {
          message.warn("您正在添加的[用户名]已经存在。");
        } else {
          message.error("添加用户失败");
        }
      }).catch(err => {
        console.error(">>>>>>添加用户失败->" + JSON.stringify(err));
        message.error("添加用户失败");
      });
    } else if (editType.value === 2) {
      await SystemUserMgtApi.updateUser(payload).then(res => {
        message.success("更新用户成功");
      }).catch(err => {
        console.error(">>>>>>更新用户失败->" + JSON.stringify(err));
        message.error("更新用户失败");
      });
    }
  } catch (err) {
    console.error(">>>>>>handleUpdateUser error->" + err);
  } finally {
    listAllUsers(1);
    editType.value = 1;
    isEditing.value = true;
    inputUserName.value = "";
    inputUserPassword.value = "";
    inputUserType.value = 0;
    itemId.value = "";
  }
}
const handleCancelUpdate = async () => {
  editType.value = 0;
  isEditing.value = false;
  inputUserName.value = "";
  inputUserPassword.value = "";
  inputUserType.value = 0;
  itemId.value = "";
}

const handleTableChange = (pag) => {
  pagination.value.current = pag.current;
  listAllUsers(pag.current);
};
const handleRefresh = async () => {
  try {
    listAllUsers();
  } catch (err) {
    console.error(">>>>>>refresh user error->" + err);
  }
}
const handleSearchUser = async (page) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "pageNumber": 1,
      "pageSize": pagination.value.pageSize,
      "searchUserName": searchUserName.value,
    }
    SystemUserMgtApi.searchUser(payload).then(async res => {
      userList.value = res.content.map(item => ({
        ...item,
        key: item.id // 确保每条数据都有唯一的key
      }));
      pagination.value.total = res.totalElements;
    }).catch(err => {
      console.error("搜索用户失败" + err);
    });
  } catch (err) {
    console.error('搜索用户失败:' + err);
  }
}

const listAllUsers = async (page) => {
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
    SystemUserMgtApi.listUsers(payload).then(async res => {
      userList.value = res.content.map(item => ({
        ...item,
        key: item.id // 确保每条数据都有唯一的key
      }));
      pagination.value.total = res.totalElements;
    }).catch(err => {
      console.error("获取系统管理用户列表失败" + err);
    });
  } catch (err) {
    console.error('获取系统管理用户列表失败:' + err);
  }
};

const getUserTypeText =  (userType) => {
  if(userType){
    if(userType===1){
      return "超级管理员";
    }else if(userType===2){
      return "拥有管理权限的用户";
    }else if(userType===3){
      return "可参与聊天用户";
    }
  }
}

onMounted(() => {
  listAllUsers(1);
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