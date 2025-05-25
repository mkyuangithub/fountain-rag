<template>
  <a-modal :visible="modelValue" title="用户选择" :width="'50%'" :style="{ top: '0%' }" @cancel="handleCancel" okText="提交"
    @ok="handleOk">
    <div style="height: 100%; width: 100%;">
      <div style="margin-top: 10px;">
        <a-table :columns="columns" :data-source="userList" :pagination="pagination" :row-selection="{
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
          type: 'checkbox'  // 明确指定checkbox类型
        }" :row-class-name="getRowClassName" style="border: 1px solid #47D45A;" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'userName'">
              {{ record.userName }}
            </template>
            <template v-if="column.dataIndex === 'type'">
              {{ getUserTypeText(record.type) }}
            </template>
          </template>
        </a-table>
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
import SystemUserMgtApi from "@/api/SystemUserMgtApi.js";
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  hasSelectedUserIds: {
    type: Array,
    default: () => []  // 对于数组类型，default 需要是一个函数返回空数组
  },
});
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

const emit = defineEmits(['update:visible', 'refresh-allowUsersNumber']);
const selectedAllowUsers = ref([]);
const userList = ref([]);
const selectedRowKeys = ref([]);

// 选择回调函数，添加key字段判断
const onSelectChange = (selectedKeys, selectedRows) => {
  selectedRowKeys.value = selectedKeys;
};

const handleTableChange = (pag) => {
  pagination.value.current = pag.current;
  listAllUsers(pag.current);
};
const handleCancel = () => {
  emit('update:modelValue', false);
};

const handleOk = () => {
  try {
    // 调用 API
    doSelect();
    emit('update:modelValue', false);
  } catch (error) {
    message.error('选择用户失败：' + error);
  }
};

const doSelect = async () => {
  try {
    selectedAllowUsers.value = selectedRowKeys.value;
  } catch (err) {
    console.error(">>>>>>选择用户失败->" + JSON.stringify(err));
  } finally {
    emit("refresh-allowUsersNumber", selectedAllowUsers.value);
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
      // 设置已选择的行
      if (props.hasSelectedUserIds && props.hasSelectedUserIds.length > 0) {
        selectedRowKeys.value = props.hasSelectedUserIds;
        selectedAllowUsers.value = props.hasSelectedUserIds;
      }
    }).catch(err => {
      console.error("获取系统管理用户列表失败" + err);
    });
  } catch (err) {
    console.error('获取系统管理用户列表失败:' + err);
  }
};

const getUserTypeText = (userType) => {
  if (userType) {
    if (userType === 1) {
      return "超级管理员";
    } else if (userType === 2) {
      return "拥有管理权限的用户";
    } else if (userType === 3) {
      return "可参与聊天用户";
    }
  }
}

watch(
  [() => props.modelValue, () => props.hasSelectedUserIds],
  ([newModelValue, newSelectedIds]) => {
    if (newModelValue) {  // 当模态框显示时
      listAllUsers(1);  // 获取第一页数据
      // 设置已选择的行
      if (newSelectedIds && newSelectedIds.length > 0) {
        selectedRowKeys.value = newSelectedIds;
        selectedAllowUsers.value = newSelectedIds;
      }
    } else {
      // 模态框关闭时清空数据
      userList.value = [];
      selectedRowKeys.value = [];
      pagination.value.current = 1;
    }
  },
  { immediate: true }  // 添加 immediate: true 确保组件挂载时就执行一次
);

</script>


<style scoped>

</style>