<template>
  <div class="main-container">
    <!-- Banner部分 -->
    <div style="height: 55px; background-color: black; width: 100%; border-bottom: 1px solid #e8e8e8; jus">
      <div style="display: flex; align-items: center; height: 100%; justify-content: space-between; ">
        <span style="font-size: 13px; color: white;">Fountain引擎 1.0</span>
        <div>
          <span style="font-size: 14px; color: white; text-decoration: underline; cursor: pointer;" @click="onLogout()">{{ t('homeMenu.logoutText') }}</span>>
          <span style="font-size: 14px; color: white; margin-left: 10px; margin-right: 10px;">
            {{ t('login.languageSelect') }}:
          </span>
          <a-select v-model:value="currentLocale" style="width: 120px; margin-right: 5px;" @change="handleLocaleChange">
            <a-select-option value="zh">中文</a-select-option>
            <a-select-option value="en">English</a-select-option>
          </a-select>
        </div>
      </div>
    </div>

    <!-- 主体内容区域 -->
    <div style="display: flex; height: calc(100vh - 85px);">
      <!-- 左侧导航区域 -->
      <div style="width: 16%; background-color: #fff; border-right: 1px solid #e8e8e8;">
        <a-collapse v-model:activeKey="activeKeys" :bordered="false">
          <a-collapse-panel v-for="menu in menus" :key="menu.key" :header="menu.title">
            <div style="display: flex; flex-direction: column; width: 100%;">
              <div v-for="item in menu.items" :key="item.key" @click="handleMenuClick(item)"
                style="cursor: pointer; padding: 8px 16px; display: flex; align-items: center;">
                <div style="display: flex; align-items: center;">
                  <component :is="item.icon" style="font-size: 18px; margin-right: 12px;" />
                  <span style="font-size: 14px;">{{ item.title }}</span>
                </div>
              </div>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </div>

      <!-- 右侧内容区域 -->
      <div style="width: 84%; height: 100%; overflow-y: auto; overflow-x: hidden; padding: 5px;">
        <component 
            :is="currentComponent" 
            v-bind="componentProps"
            @switchComponent="handleComponentSwitch" 
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import UserLoginApi from "@/api/UserLoginApi.js";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import Welcome from "@/viewer/Welcome.vue";
import KnowledgeMain from "@/viewer/knowledge/KnowledgeMain.vue";
import KnowledgeDetail from './knowledge/KnowledgeDetail.vue';
import AIModelHome from './aimodel/AIModelHome.vue';
import AIFunctionHome from './aifunction/AIFunctionHome.vue';
import UserHome from './users/UserHome.vue';
import UserChatConfig from './users/UserChatConfig.vue';
import Chat from './chatbot/Chat.vue';
import SmartPurchase from './agent/smartpurchase/Chat.vue';
import PicTools from './agent/pictools/PicTools.vue';
import { ref } from 'vue';
import { message, Modal } from "ant-design-vue"; // 添加 Modal 导入

import { useRoute, useRouter } from "vue-router";
import { useI18n } from 'vue-i18n';
const route = useRoute();
const router = useRouter();

// 激活的折叠面板key
const activeKeys = ref(['1']);

// 当前显示的组件
const currentComponent = ref(Welcome);
const WelcomComponent = ref(Welcome);
const KnowledgeMainComponent = ref(KnowledgeMain);
const KnowledgeDetailComponent = ref(KnowledgeDetail);
const AIModelHomeComponent = ref(AIModelHome);
const AIFunctionComponent = ref(AIFunctionHome);
const UserComponent=ref(UserHome);
const UserChatConfigComponent=ref(UserChatConfig);
const ChatComponent=ref(Chat);
const SmartPurchaseComponent=ref(SmartPurchase);
const PicToolsComponent=ref(PicTools);
const componentProps = ref({});
const { locale, t } = useI18n();
const currentLocale = ref(locale.value);


const handleLocaleChange = (value) => {
  locale.value = value;
  currentLocale.value = value;
  console.log("local->" + value);
  localStorage.setItem('locale', value);
  
  // 获取当前路由信息
  const currentRoute = router.currentRoute.value;
  
  // 刷新当前页面
  router.replace({
    name: currentRoute.name,
    params: currentRoute.params,
    query: currentRoute.query
  }).then(() => {
    window.location.reload();
  });
};
// 菜单数据
const menus = ref([
  {
    key: '1',
    title: t('homeMenu.welcomeMsg'),
    items: [
      { key: '1-1', title: t('homeMenu.homePageText'), icon: 'HomeOutlined', component: WelcomComponent },
    ]
  },
  {
    key: '2',
    title: t('homeMenu.knowledgeRepoText'),
    items: [
      { key: '2-1', title: t('homeMenu.knowledgeRepoManageText'), icon: 'ReadOutlined', component: KnowledgeMainComponent },
    ]
  },
  {
    key: '3',
    title: t('homeMenu.aiSettingText'),
    items: [
      { key: '3-1', title: t('homeMenu.aiModelSettingText'), icon: 'AndroidOutlined', component: AIModelHomeComponent },
      { key: '3-2', title: t('homeMenu.aiFunctionSettingText'), icon: 'CalculatorOutlined', component: AIFunctionComponent },
    ]
  },
  {
    key: '4',
    title: t('homeMenu.userSettingText'),
    items: [
      { key: '4-1', title: t('homeMenu.userManagementText'), icon: 'TeamOutlined', component: UserComponent },
      { key: '4-2', title: t('homeMenu.userChatSettingText'), icon: 'SettingOutlined', component: UserChatConfigComponent },
    ]
  },
  {
    key: '5',
    title: t('homeMenu.chatAppText'),
    items: [
      { key: '5-1', title: t('homeMenu.ChatWithKnowledge'), icon: 'MessageOutlined', component: ChatComponent },      
    ]
  },
  {
    key: '6',
    title: 'AI Agent应用',
    items: [
      { key: '6-1', title: '智能导购', icon: 'ShoppingCartOutlined', component: SmartPurchaseComponent },      
      //{ key: '6-2', title: '图片AI', icon: 'FileImageOutlined', component: PicToolsComponent },   

    ]
  }
]);


// 处理组件切换
const handleComponentSwitch = ({ component, props = {} }) => {
    console.log("emit handleComponentSwitch");
    switch(component) {
        case 'KnowledgeDetail':
            currentComponent.value = KnowledgeDetailComponent.value;
            break;
        case 'KnowledgeMain':
            currentComponent.value = KnowledgeMainComponent.value;
            break;
        default:
            currentComponent.value = WelcomComponent.value;
    }
    componentProps.value = props;
};
// 处理菜单点击
const handleMenuClick = (item) => {
  currentComponent.value = item.component;
};

//logout
const onLogout = () => {
  Modal.confirm({
    title: '确认退出',
    content: '您确定要退出系统吗？',
    okText: '确认',
    cancelText: '取消',
    onOk() {
      handleLogout();
    }
  });
};

// 执行logout
const handleLogout = () => {
  try {
    let token = authorization.getToken();
    let encryptedToken = encrypt_url(token);
    let userName = authorization.getUserName();
    console.log(">>>>>>userName->"+userName)
    let payload = {
      "userName": userName,
      "token": encryptedToken,
    }
    UserLoginApi.logout(payload).then(res => {
      let channel = route.params.channel || 'default';
      router.replace({
        name: 'Login',
        params: {
          channel
        }
      });
    }).catch(err => {
      console.log("login api error->" + err);
    });
  } catch (err) {
    console.log("logout error->" + err);
  }
};


</script>

<style scoped>
.ant-collapse-content-box {
  padding: 0 !important;
}

/* 添加鼠标悬停效果 */
.ant-collapse-content-box>div>div:hover {
  background-color: #f5f5f5;
}

/* 覆盖 Ant Design 的默认样式 */
:deep(.ant-collapse) {
  background: transparent;
}

:deep(.ant-collapse-item) {
  background: transparent;
}

:deep(.ant-collapse-content) {
  background: transparent;
}
</style>