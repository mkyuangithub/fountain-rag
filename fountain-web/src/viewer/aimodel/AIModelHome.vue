<template>
  <div style="width: 100%; min-height: 100%;">
    <div style="width:100%; display: flex; justify-content: space-between;">
      <!--添加AI模型按钮-->
      <div style="cursor: pointer; display: flex; margin-top: 0px;  margin-bottom: 20px; align-items: center; "
        @click="handleAddAIModel">
        <div
          style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
          <PlusOutlined style="color: blue; font-size: 13px;" />
        </div>
        <span style="color: #595959;  margin-left: 10px;  font-size:13px;">添加一个AI模型</span>
      </div>
      <!--刷新按钮-->
      <div>
        <a-button type="text" size="small" @click="handleRefreshAIModels()">
          <SyncOutlined />刷新
        </a-button>
      </div>
    </div>
    <template v-if="aiModelList && aiModelList.length > 0">
      <div style="display: flex; flex-wrap: wrap; gap: 5px 5px; width: 100%; padding: 0">
        <div v-for="model in aiModelList" :key="model.id"
          style="width: 500px; height: 300px; margin-left: 7px;  margin-bottom: 10px; border: 1px solid #7F7F7F; border-radius: 3%; padding: 16px; transition: all 0.3s ease;"
          :style="{
            'box-shadow': model.isHovered ? '0 6px 12px rgba(59, 33, 251, 0.15)' : '0 2px 4px rgba(0,0,0,0.05)',
            'border-color': model.isHovered ? '#3B21FB' : '#7F7F7F',
            'transform': model.isHovered ? 'translateY(-2px)' : 'translateY(0)'
          }" @mouseenter="model.isHovered = true" @mouseleave="model.isHovered = false">
          <div style="display: flex; align-items: center; gap: 10px; font-weight: bold;">
            <span :style="{
              color: model.routeType === 0 ? '#4EA72E' :
                model.routeType === 1 ? '#0000CC' : '#00B0F0'
            }">
              <img v-if="model.routeType === 1" src="/assets/images/star-yellow-1.png"
                style="width: 20px; height: 20px; margin-right: 4px; vertical-align: middle" />
              <img v-if="model.routeType === 2" src="/assets/images/star-blue-1.png"
                style="width: 20px; height: 20px; margin-right: 4px; vertical-align: middle" />
              {{ model.routeType === 0 ? '未设定' : model.routeType === 1 ? '主线路' : '从线路' }}
            </span>
            <div style="display: flex; gap: 30px">
              <template v-if="model.routeType === 0">
                <a-button type="primary" size="small" @click="setRouteType(model, 1)">
                  <template #icon>
                    <UserOutlined style="color: white" />
                  </template>
                  设成主线路
                </a-button>
                <a-button size="small" @click="setRouteType(model, 2)"
                  style="background: #E97132; border-color: #E97132; color: white;">
                  <template #icon>
                    <TeamOutlined style="color: white" />
                  </template>
                  设成子线路
                </a-button>
              </template>

              <template v-if="model.routeType === 1">
                <a-button size="small" @click="unsetRouteType(model, 1)"
                  style="background: #4EA72E; border-color: #4EA72E; color: white;">
                  <template #icon>
                    <InfoCircleOutlined style="color: white" />
                  </template>
                  取消设定
                </a-button>
              </template>

              <template v-if="model.routeType === 2">
                <a-button size="small" @click="unsetRouteType(model, 2)"
                  style="background: #4EA72E; border-color: #4EA72E; color: black;">
                  <template #icon>
                    <InfoCircleOutlined style="color: white" />
                  </template>
                  取消设定
                </a-button>
              </template>
            </div>
          </div>
          <div
            style="display: flex; font-size: 14px; color: #595959; justify-items: center; align-items: center; height: 40px; margin-top: 10px;">
            <div style="width: 20%;">模型名:</div>
            <div style="margin-left: 5px; width:80%;">
              <template v-if="model.isEditing">
                <a-input v-model:value="model.modelName" />
              </template>
              <template v-else>
                <div style="display: flex; align-items: center;">
                  <div>
                    {{ model.modelName }}
                  </div>
                  <div style="margin-left: 20px;">
                    <a-tooltip title="删除" placement="bottom">
                      <a-button type="text" size="small" @click="handleRemove(model)">
                        <DeleteOutlined />
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
              </template>

            </div>
          </div>
          <div style="margin-top: 15px; display: flex; font-size: 14px; color: #595959; height: 40px; ">
            <div style="width: 20%;">模型连接:</div>
            <div
              style="margin-left: 5px; width: 80%; word-wrap: break-word; word-break: break-all; font-size: 12px; width:80%;">
              <template v-if="model.isEditing">
                <a-input v-model:value="model.url" style="font-size: 12px;" />
              </template>
              <template v-else>
                {{ model.url }}
              </template>
            </div>
          </div>
          <div
            style="margin-top: 15px; display: flex; font-size: 14px; color: #595959; justify-items: center; align-items: center; height: 40px;">
            <div style="width: 20%;">api-key:</div>
            <div style="margin-left: 5px; font-size: 12px; color: #595959; width:80%; ">
              <template v-if="model.isEditing">
                <a-input-password v-model:value="model.apiKey" autocomplete="off" style="font-size: 12px;"
                  class="custom-password-input" />
              </template>
              <template v-else>
                <template v-if="model.apiKey">******</template>
                <template v-else>暂无</template>
              </template>
            </div>
          </div>
          <div style="margin-top: 15px; display: flex; font-size: 14px; color: #595959; justify-items: center; 
            align-items: center; height: 40px;
            ">
            <div style="width: 20%;">模型类型:</div>
            <div style="margin-left: 5px; width:80%;">
              <template v-if="model.isEditing">
                <a-select v-model:value="model.selectedModelType" style="width: 100%">
                  <a-select-option :value="1">deepseek本地</a-select-option>
                  <a-select-option :value="2">deepseek saas</a-select-option>
                  <a-select-option :value="3">GPT 4o-mini</a-select-option>
                  <a-select-option :value="4">GPT 4o</a-select-option>
                  <a-select-option :value="5">Qwen3</a-select-option>
                  <a-select-option :value="6">Microsoft Phi4模型</a-select-option>
                  <a-select-option :value="7">Google Gemma3模型</a-select-option>
                  <a-select-option :value="8">Qwen系列</a-select-option>
                </a-select>
              </template>
              <template v-else>
                <template v-if="model.type === 1">
                  <img src="/assets/images/deep-seek-logo.png" style="width: auto; height: 18px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">deepseek本地</span>
                </template>
                <template v-else-if="model.type === 2">
                  <img src="/assets/images/deep-seek-saas.png" style="width: auto; height: 18px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">deekseek saas</span>
                </template>
                <template v-else-if="model.type === 3">
                  <img src="/assets/images/g4o-mini-logo-green.png"
                    style="width: auto; height: 20px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">GPT-4O-mini</span>
                </template>
                <template v-else-if="model.type === 4">
                  <img src="/assets/images/4o-logo.png" style="width: auto; height: 20px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">GPT-4O</span>
                </template>
                <template v-else-if="model.type === 5">
                  <img src="/assets/images/qwen-logo.png" style="width: auto; height: 20px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">QWen3</span>
                </template>
                <template v-else-if="model.type === 6">
                  <img src="/assets/images/ms-phi4-logo.png" style="width: auto; height: 20px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">Microsoft Phi4模型</span>
                </template>
                <template v-else-if="model.type === 7">
                  <img src="/assets/images/gemma-logo-1.png" style="width: auto; height: 20px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">Google Gemma3模型</span>
                </template>
                 <template v-else-if="model.type === 8">
                  <img src="/assets/images/qwen-logo.png" style="width: auto; height: 20px; margin-right: 5px;" />
                  <span style="font-size: 12px; color:#595959">QWen系列</span>
                </template>
              </template>
            </div>
          </div>
          <div style="border-bottom: 1px solid #7F7F7F; margin: 0 -16px; padding: px 16px;">
          </div>
          <div style="display: flex; justify-items: center; margin-top: 10px; font-size: 13px; color:#595959;">
            <template v-if="model.isEditing">
              <a-button type="primary" size="small" style="margin-right: 8px;" @click="handleUpdateModel(model)">
                <template #icon>
                  <CheckOutlined />
                </template>
                确认
              </a-button>
              <a-button size="small" @click="handleCancelEdit(model)">
                <template #icon>
                  <CloseOutlined />
                </template>
                取消
              </a-button>
            </template>
            <template v-else>
              <a-button type="primary" size="small" @click="handleEditModel(model)">
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </a-button>
            </template>
          </div>
        </div>
      </div>
    </template>
    <AddAIModel v-model="addAIModelShow" @refresh-model="listAllAIModels" />
  </div>
</template>
<script setup>
import { ref, onMounted, reactive, defineEmits } from 'vue';
import { message } from 'ant-design-vue';
import { Modal } from 'ant-design-vue';
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import AIModelApi from "@/api/AIModelApi.js";
import AddAIModel from "@/viewer/aimodel/AddAIModel.vue";
import { InfoCircleOutlined } from '@ant-design/icons-vue';
const aiModelList = ref([]);

const addAIModelShow = ref(false);
const processing = ref(false);
// 在setup中添加遮罩控制的ref
const loadingMask = ref(false);
const formatDate = (date) => {
  return new Date(date).toLocaleDateString();
};

/*设置模型是否为主还是从 开始*/
const setRouteType = async (model, type) => {
  try {
    // 这里添加你的设置路由类型的逻辑
    //await yourApiCall(model.id, type);
    // 更新成功后的处理
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "modelId": model.id,
      "routeType": type,
    }
    await AIModelApi.setAIModelRoute(payload).then(res => {
      if (res === 1) {
        message.success("设定成功");
      } else if (res === 101) {
        message.warn("己有一个主线路，不可重复设置。请把己有的主线路取消，你可以点击->[取消设定按钮]");
      } else if (res === 102) {
        message.warn("己有一个子线路，不可重复设置。请把己有的子线路取消，你可以点击->[取消设定按钮]");
      } else {
        message.error("设置失败");
      }
    }).catch(err => {
      console.error(">>>>>>设置线路失败->" + JSON.stringify(err));
      message.error("设置线路失败");
    });
  } catch (err) {
    console.error("设置线路失败 -> " + err);
  } finally {
    listAllAIModels();
  }
};

const unsetRouteType = async (model, type) => {
  try {
    // 这里添加你的设置路由类型的逻辑
    //await yourApiCall(model.id, type);
    // 更新成功后的处理
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "modelId": model.id,
      "routeType": type,
    }
    await AIModelApi.unsetAIModelRoute(payload).then(res => {
      if (res === 1) {
        message.success("取消设定成功");
      } else {
        message.error("取消设定失败");
      }
    }).catch(err => {
      console.error(">>>>>>取消设定失败->" + JSON.stringify(err));
      message.error("取消设定失败");
    });
  } catch (err) {
    console.error("取消设定失败 -> " + err);
  } finally {
    listAllAIModels();
  }
};
/*设置模型是否为主还是从 结束*/

/**删除一个aimode */
const handleRemove = (model) => {
  Modal.confirm({
    title: '确认删除',
    content: '您确认删除当前AI模型？此操作为不可撤销!',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
          "token": encryptToken,
          "userName": userName,
          "modelId": model.id,
        }
        await AIModelApi.deleteModel(payload);
        listAllAIModels();//刷新页面
        message.success("已删除该AI模型");
      } catch (err) {
        message.error("删除失败");
        console.error(">>>>>>删除一个模型错误->" + err);
      }
    }
  });
}

/*按下编辑按钮开始*/

//提交编辑区内容至后台
const handleUpdateModel = async (model) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "modelId": model.id,
      "modelName": model.modelName,
      "url": model.url,
      "apiKey": model.apiKey,
    }
    await AIModelApi.updateModel(payload).then(res => {
      if(res===1){
        message.success("更新AI模型成功");
      }else if (res===101){
        message.warn("您正在编辑的[模型类型]已经存在，同一类型的模型只允许存在一个。");
      }else{
        message.error("更新AI模型失败");
      }
    }).catch(err => {
      console.error(">>>>>>更新AI模型失败->" + JSON.stringify(err));
      message.error("更新AI模型失败");
    });
  } catch (err) {
    console.error(">>>>>>handleUpdateModel error->" + err);
  } finally {
    listAllAIModels();
  }
}

const handleEditModel = (model) => {
  model.isEditing = true
}
const handleCancelEdit = (model) => {
  model.isEditing = false
}
/*按下编辑按钮结束*/

const handleAddAIModel = async () => {
  try {
    addAIModelShow.value = true;
  } catch (err) {
    console.log(">>>>>>添加一个AI Model 错误->" + err);
  }
}

const getAIModelRouteType = async (model) => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
      "modelId": model.id,
    }
    // 使用同步方式调用
    const res = await AIModelApi.getRouteType(payload);
    console.log(">>>>>>获取到modelId->" + model.id + " 的routeType为->" + res);
    return res;
  } catch (err) {
    console.error(">>>>>>根据每一个model获取其type错误->" + err);
    return 0;
  }
}

const listAllAIModels = async () => {
  try {
    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    let payload = {
      "token": encryptToken,
      "userName": userName,
    }
    AIModelApi.listMyAIModel(payload).then(async res => {
      // 从返回的Map中解构获取对应的值
      const modelsWithRouteTypes = await Promise.all(res.map(async repo => ({
        ...repo,
        isHovered: false,
        isEditing: false,
        selectedModelType: repo.type,
        routeType: await getAIModelRouteType(repo),
      })));
      aiModelList.value = modelsWithRouteTypes;
    }).catch(err => {
      console.error("获取AIModel失败" + JSON.stringify(err));
    });
  } catch (err) {
    console.error('获取AIModel失败:' + JSON.stringify(err));
  }
};

const handleRefreshAIModels = () => {
  listAllAIModels();
}
onMounted(() => {
  listAllAIModels();
});
</script>
<style scoped>
:deep(.custom-password-input) {
  width: 100%;
}

:deep(.custom-password-input .ant-input) {
  width: 100%;
}

/* 如果上面的还不够，可以加这个更具体的选择器 */
:deep(.custom-password-input .ant-input-affix-wrapper) {
  width: 100%;
}
</style>