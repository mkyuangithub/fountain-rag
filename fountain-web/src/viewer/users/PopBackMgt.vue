<template>
  <a-modal :visible="modelValue" title="用户聊天配置备份管理" :width="'90%'" :style="{ top: '0%' }" @cancel="handleCancel"
    cancelText="关闭">
    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>
    <div style="cursor: pointer; display: flex; margin-top: 30px; margin-bottom: 20px; align-items: center;">
      <div style="display: flex;  width: 70%; align-items: center;">
        <div style="width: 10%;">
          <span style="font-size: 13px; color: #595959;">
            文件名:
          </span>
        </div>
        <div style="width: 90%;">
          <a-input v-model:value="backupDescr" autocomplete="off" placeholder="非必填，不填会使用年月日时分秒做备份文件名" allowClear
            style="width: 100%; color:#595959; margin: 10px;" />
        </div>
      </div>
      <div style="display: flex; align-items: center;" @click="handleBackup()">
        <div
          style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-left: 20px;">
          <PlusOutlined style="color: blue; font-size: 13px;" />
        </div>
        <span style="color: #595959; margin-left: 10px; font-size:13px;">添加一个备份</span>
      </div>
    </div>
    <div style="display: flex; width: 100%; align-items: center;">
      <div style="display: flex; width: 50%; align-items: center;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          按文件名模型匹配
        </div>
        <div style="width: 75%;">
          <a-input v-model:value="queryBackupDescr" autocomplete="off" placeholder="非必填，不填代表搜索所有文件" allowClear
            style="width: 100%; color:#595959; margin: 10px;" />
        </div>
      </div>
      <div style="display: flex; width: 50%; align-items: center;">
        <div style="width: 20%; font-size: 13px; color: #595959;">
          按创建日期搜
        </div>
        <div style="width: 75%;">
          <a-date-picker @change="onDateChange" style="font-size: 13px; width: 100%;" placeholder="选择日期"
            :show-time="false" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" />
        </div>
      </div>
      <div style="width: 5%;">
        <img src="/assets/images/search-blue.png" style="width: 18px; height: 18px; cursor: pointer;"
          @click="getBackupList">
      </div>
    </div>
    <div v-if="backupList && pagination.total > 0" style="margin-top: 3px;">
      <a-table :columns="columns" :data-source="backupList" :pagination="pagination" style="border: 1px solid #47D45A;">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'delete'">
            <delete-outlined style="font-size: 14px; color: red; cursor: pointer;" @click="handleDelete(record)" />
          </template>
          <template v-else-if="column.dataIndex === 'backupDescr'">
            {{ record.backupDescr }}
          </template>
          <template v-else-if="column.dataIndex === 'createdDate'">
            {{ record.createdDate }}
          </template>
          <template v-else-if="column.dataIndex === 'download'">
            <CloudDownloadOutlined style="font-size: 14px; color: green; cursor: pointer;"
              @click="handleDownload(record)" />
          </template>
          <template v-else-if="column.dataIndex === 'restore'">
            <RedoOutlined style="font-size: 14px; color: blue; cursor: pointer;"
              @click="handleRestore(record)" />
          </template>
        </template>
      </a-table>
    </div>
    <div v-else
      style="margin-top: 10px; font-size: 16px; color: #c7c7c7; display: flex; justify-items: center; justify-content: center;">
      <<暂无备份记录>>
    </div>
    <!--专门用来显示全屏遮罩-->
    <div v-if="loadingMask"
      style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(255, 255, 255, 0.7); display: flex; flex-direction: column; justify-content: center; align-items: center; z-index: 9999;">
      <img src="/assets/images/waiting-blue.gif" alt="loading" />
      <div style="margin-top: 10px; font-size: 16px; color: #1890ff; font-weight: 500;">处理中</div>
    </div>
  </a-modal>

</template>
<script setup>
import { ref, watch } from 'vue';
import { message } from "ant-design-vue";
import dayjs from 'dayjs' // Ant Design Vue 内置了 dayjs
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import UserChatConfigApi from "@/api/UserChatConfigApi.js";
import { Modal } from 'ant-design-vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
});
const loadingMask = ref(false);
const backupDescr = ref('');
const backupList = ref([]);
const queryDate = ref('');
const queryBackupDescr = ref('');
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
});
const columns = [
  {
    title: '序号',
    key: 'index',
    width: 50,
    align: 'center',
    customRender: ({ index }) => index + 1
  },
  {
    title: '文件描述名',
    dataIndex: 'backupDescr',
    key: 'backupDescr',
    width: 200,
  },
  {
    title: '创建时间',
    dataIndex: 'createdDate',
    key: 'createdDate',
    width: 200,
    align: 'center',  // 添加居中对齐
  },
  {
    title: '恢复',
    dataIndex: 'restore',
    key: 'restore',
    width: 50,
    align: 'center',
  },
  {
    title: '下载',
    dataIndex: 'download',
    key: 'download',
    width: 50,
    align: 'center',
  },
  {
    title: '删除',
    key: 'delete',
    width: 50,
    align: 'center',
  },

];
// 生成指定格式的文件名
const generateFileName = (record) => {
  const fileName=record.backupDescr+'.json';
  return fileName;
};
const emit = defineEmits(['update:visible']);

const onDateChange = (date, dateString) => {
  if (date) {
    // 由于没有选择时间，我们设置默认时间为当天的 00:00:00
    queryDate.value = dayjs(date).format('YYYY-MM-DD HH:mm:ss')
    console.log('格式化后的日期:', queryDate.value) // 2024-03-21 00:00:00
    // 这里可以将 formattedDate 发送给后端
  } else {
    queryDate.value = ''
    console.log('日期已清空:', queryDate.value)
  }
}

const handleDownload = async (record) => {
  try {
    //loadingMask.value = true;
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "fileCode": record.fileCode,
    }
    // 使用 await 等待 API 调用完成
    const response = await UserChatConfigApi.downloadBackupFile(payload);

    // 创建 blob URL
    const blob = new Blob([response], { type: response.type });
    const url = window.URL.createObjectURL(blob);

    // 创建一个临时的 a 标签来触发下载
    const link = document.createElement('a');
    link.href = url;
    link.download = generateFileName(record); // 设置下载的文件名
    document.body.appendChild(link);
    link.click();

    // 清理
    window.URL.revokeObjectURL(url);
    document.body.removeChild(link);

  } catch (err) {
    console.error(">>>>>>download and preview error", err);
  }
}

const handleRestore= (record) => {
  Modal.confirm({
    title: '恢复操作',
    content: '确认使用当前备份覆盖所有当前会话设置？',
    okText: '确定覆盖',
    cancelText: '取消',
    async onOk() {
      try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
          "token": encryptToken,
          "userName": userName,
          "backupId": record.id,
        }
        await UserChatConfigApi.restoreByBackupFile(payload);
        message.success("恢复成功");
      } catch (err) {
        message.error("恢复失败");
        console.error(">>>>>>恢复失败->", err);
      } finally {
        await getBackupList();
      }
    }
  });
}

const handleDelete = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: '确认删除当前备份？',
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
          "backupId": record.id,
        }
        await UserChatConfigApi.removeBackupFile(payload);
        message.success("删除成功");
      } catch (err) {
        message.error("删除失败");
        console.error(">>>>>>删除失败->", err);
      } finally {
        await getBackupList();
      }
    }
  });
}

const handleCancel = () => {
  emit('update:modelValue', false);
  emit('refresh-config');
};
const handleBackup = async () => {
  try {
    loadingMask.value = true;
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "backupDescr": backupDescr.value,
    }
    // 使用 await 等待 API 调用完成
    const res = await UserChatConfigApi.backupAllConfig(payload);
    if (res === 1) {
      message.success("备份成功");
    } else if (res === 101) {
      message.warn("文件名重复");
      return;
    } else if (res === -1) {
      message.warn("备份错误");
    }
  } catch (err) {
    console.error(">>>>>>handleBackup error", err);
  } finally {
    loadingMask.value = false;
    getBackupList();
  }
}

const getBackupList = async (page) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "pageNumber": pagination.value.current,
      "pageSize": pagination.value.pageSize,
      "backupDescr": queryBackupDescr.value,
      "queryDate": queryDate.value,
    }
    UserChatConfigApi.getBackupList(payload).then(async res => {
      backupList.value = res.content.map(item => ({
        ...item,
        key: item.id // 确保每条数据都有唯一的key
      }));
      pagination.value.total = res.totalElements;


    }).catch(err => {
      console.error("获取聊天配置列表失败" + err);
    });
  } catch (err) {
    console.error('获取聊天配置列表失败:' + err);
  }
};
watch(
  [() => props.modelValue],
  ([newModelValue]) => {
    if (newModelValue) {  // 当模态框显示时
      getBackupList(1);  // 获取第一页数据     
    } else {
      // 模态框关闭时清空数据
      backupList.value = [];
      queryBackupDescr.value = '';
      queryDate.value = '';
      pagination.value.current = 1;
    }
  },
  { immediate: true }  // 添加 immediate: true 确保组件挂载时就执行一次
);
</script>