<template>
  <div class="function-container" style="display: flex; flex-direction: column;">
    <div style="width:100%; display: flex; justify-content: space-between;">
      <!--添加AI函数按钮-->
      <div style="cursor: pointer; display: flex; margin-top: 0px;  margin-bottom: 20px; align-items: center; "
        @click="handleAddAIFunction()">
        <div
          style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
          <PlusOutlined style="color: blue; font-size: 13px;" />
        </div>
        <span style="color: #595959;  margin-left: 10px;  font-size:13px;">添加一个AI函数</span>
      </div>
      <!--刷新按钮-->
      <div>
        <a-button type="text" size="small" @click="handleRefreshAIFunction()">
          <SyncOutlined />刷新
        </a-button>
      </div>
    </div>
    <!-- 上半部分 -->
    <span style="font-weight: bold; font-size: 16px; color: #595959;">
      <div style="display: flex; justify-content: space-between;">
        <span style="font-weight: bold; font-size: 16px; color: #595959;">
          函数列表
        </span>
        <a-button type="primary" size="small"
          style="margin-right: 8px; background-color: red; border-color: red; color: #ffffff;"
          @click="handleDeleteSelectFunction()">
          <template #icon>
            <DeleteOutlined />
          </template>
          删除
        </a-button>
      </div>
    </span>
    <div style="margin-top: 10px;">
      <a-table :columns="columns" :data-source="functionList" :pagination="pagination" :row-selection="{
        selectedRowKeys: selectedRowKeys,
        onChange: onSelectChange,
        type: 'checkbox'  // 明确指定checkbox类型
      }" :row-class-name="getRowClassName" style="border: 1px solid #47D45A;" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'code'">
            <a @click="handleEdit(record)" style="text-decoration: underline;">{{ record.code }}</a>
          </template>
          <template v-if="column.dataIndex === 'prompt'">
            {{ truncateText(record.prompt, 30) }}
          </template>
          <template v-if="column.dataIndex === 'returnTemplate'">
            {{ truncateText(record.returnTemplate, 30) }}
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
            @click="handleUpdateAIFunction()">
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
              函数编号(不可编辑)
            </div>
            <div style="width: 88%;">
              <span style="color: blue;">
                {{ itemCode }}
              </span>
            </div>
          </template>
          <template v-if="editType === 1">
            <div style=" width: 20%;">
              函数编号
            </div>
            <div style="width: 88%;">
              <a-input v-model:value="itemCode" autocomplete="off" placeholder="必输: 模型编号"
                style="width: 50%; color:#595959" />
            </div>
          </template>
        </div>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: center;">
          <div style=" width: 20%;">
            函数描述
          </div>
          <div style="width: 88%;">
            <a-input v-model:value="itemDescription" autocomplete="off" placeholder="必输: 用于描述该函数是做什么事的摘要"
              :maxlength="40" style="width: 50%; color:#595959" />
          </div>
        </div>
        <div style=" display: flex; margin-top: 10px; margin-left: 10px; align-items: flex-start;">
          <div style=" width: 20%;">
            函数Body
          </div>
          <div style="width: 88%;">
            <a-textarea v-model:value="itemPrompt" :rows="8" :maxRows="10" placeholder="必输，这是该函数具体执行的内容"
              style="width: 100%; font-size: 12px; color:#595959" />
          </div>
        </div>
        <div style="display: flex; margin-top: 10px; margin-left: 10px; align-items: flex-start; margin-bottom: 10px;">
          <div style=" width: 20%;">
            函数的返回值
          </div>
          <div style="width: 88%;">
            <a-textarea v-model:value="itemReturnTemplate" :rows="8" :maxRows="10" placeholder="非必输，你可以直接把返回类型写在函数body里"
              style="width: 100%; font-size: 12px; color:#595959" />
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
import AIFunctionApi from "@/api/AIFunctionApi.js";

const isEditing = ref(false);
const editType = ref(1);
const functionList = ref([]);
const selectedRowKeys = ref([]);

const itemId = ref('');
const itemCode = ref(0);
const itemPrompt = ref('');
const itemDescription = ref('');
const itemReturnTemplate = ref('');

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false
});

const columns = [
  {
    title: '编号',
    dataIndex: 'code',
    key: 'code',
    width: 80,
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '提示语',
    dataIndex: 'prompt',
    key: 'prompt',
  },
  {
    title: '返回内容',
    dataIndex: 'returnTemplate',
    key: 'returnTemplate',
  }
];

const handleDeleteSelectFunction = async () => {
  try {
    if (selectedRowKeys.value.length === 0) {
      message.warning('请至少选择一条记录');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      content: '删除当前选中的AI函数会造成功能上的影响，请充分确认所选函数没有被用在相关功能上！',
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
            "functionIdList": selectedRowKeys.value,
          }
          await AIFunctionApi.deleteFunction(payload);
          message.success("删除成功");
        } catch (err) {
          message.error("删除失败");
          console.error(">>>>>>删除失败->" + err);
        } finally {
          // 无论成功还是失败都会执行
          await listAllAIFunction(1);
        }
      }
    });
  } catch (err) {
    message.error("删除失败");
    console.error(">>>>>>删除失败->" + err);
  }
}
const handleAddAIFunction = () => {
  try {
    itemId.value = '';
    itemCode.value = 0;
    itemDescription.value = '';
    itemPrompt.value = '';
    itemReturnTemplate.value = ''
    isEditing.value = true;
    editType.value = 1;
  } catch (err) {
    console.error(">>>>>>把编辑区域设成新增时发生错误->" + err);
  }
}
const handleEdit = (record) => {
  try {
    itemId.value = record.id;
    itemCode.value = record.code;
    itemDescription.value = record.description;
    itemPrompt.value = record.prompt;
    itemReturnTemplate.value = record.returnTemplate;
    isEditing.value = true;
    editType.value = 2;
  } catch (err) {
    console.error(">>>>>>把单条AI函数内容载入编辑区域时发生错误->" + err);
  }
};
const handleCancelUpdate = async () => {
  editType.value = 0;
  isEditing.value = false;
  itemId.value = "";
  itemCode.value = 0;
  itemDescription.value = "";
  itemPrompt.value = "";
  itemReturnTemplate.value = "";
}
const handleUpdateAIFunction = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "id": itemId.value,
      "code": itemCode.value,
      "description": itemDescription.value,
      "prompt": itemPrompt.value,
      "returnTemplate": itemReturnTemplate.value,
    }
    if (editType.value === 1) { //新增保存
      if (itemCode && itemCode.value <= 0) {
        message.warn("函数编号必须是一个>0开始的整数");
        return;
      }
      await AIFunctionApi.addFunction(payload).then(res => {
        if (res === 1) {
          message.success("保存成功");
        } else if (res === 101) {
          message.warn("相同函数编号已存在，不可重复设置。");
        } else {
          message.error("保存失败");
        }
      }).catch(err => {
        console.error(">>>>>>调用后台保存失败->" + JSON.stringify(err));
        message.error("保存一条AI函数时发生了错误");
      });
    } else if (editType.value === 2) {
      await AIFunctionApi.updateFunction(payload).then(res => {
        if (res === 1) {
          message.success("更新成功");
        } else {
          message.error("更新失败");
        }
      }).catch(err => {
        console.error(">>>>>>调用后台更新失败->" + JSON.stringify(err));
        message.error("更新一条AI函数时发生了错误");
      });
    } else {
      return;
    }
  } catch (err) {
    console.error(">>>>>>保存一条AI函数时发生了错误->" + err);
  } finally {
    listAllAIFunction(1);
  }
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

const truncateText = (text, maxLength) => {
  if (text && text.length > maxLength) {
    return text.substring(0, maxLength) + '...';
  }
  return text;
};

const handleTableChange = (pag) => {
  pagination.value.current = pag.current;
  listAllAIFunction(pag.current);
};
const handleRefreshAIFunction = async () => {
  listAllAIFunction(1);
}
const listAllAIFunction = async (page) => {
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
    AIFunctionApi.listAIFunction(payload).then(async res => {
      functionList.value = res.content.map(item => ({
        ...item,
        key: item.id // 确保每条数据都有唯一的key
      }));
      pagination.value.total = res.totalElements;
    }).catch(err => {
      console.error("获取AIFunction失败" + JSON.stringify(err));
    });
  } catch (err) {
    console.error('获取AIFunction失败:' + JSON.stringify(err));
  }
};

onMounted(() => {
  listAllAIFunction(1);
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