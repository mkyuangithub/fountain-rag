<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 左侧图片 -->
      <div class="login-left">
        <img src="/assets/images/login-bg-1.jpg" alt="login background" />
      </div>
      <div class="login-right">
        <div class="language-selector">
          <span style="font-size: 14px; color: #595959; margin-right: 10px;">
            {{ t('login.languageSelect') }}:
          </span>
          <a-select v-model:value="currentLocale" style="width: 120px" @change="handleLocaleChange">
            <a-select-option value="zh">中文</a-select-option>
            <a-select-option value="en">English</a-select-option>
          </a-select>
        </div>
        <h2 style="text-align: center; margin-bottom: 30px;">
          {{ t('login.welcomeMsg') }}
        </h2>

        <div style="width: 100%; padding: 0 10px;">
          <div style="margin-bottom: 20px;display: flex; align-items: center;">
            <div style="margin-bottom: 8px; width:20%;">{{ t('login.userName') }}:</div>
            <div style="margin-left: 50px; width: 70%;">
              <a-input v-model:value="userName" :placeholder="t('login.userNamePlaceholder')" />
            </div>
          </div>

          <div style="margin-bottom: 20px; display: flex; align-items: center;">
            <div style="margin-bottom: 8px;width:20%;">{{ t('login.password') }}:</div>
            <div style="margin-left: 50px; width: 70%">
              <a-input-password v-model:value="password" :placeholder="t('login.passwordPlaceholder')" />
            </div>
          </div>
          <a-button type="primary"
            style="width: 100%; margin-bottom: 20px; background-color: #006FE6; color: #fff; border-color: #006FE6;"
            @click="onLogin">{{ t('login.loginBtn') }}</a-button>

          <div style="display: flex; width: 100%;">
            <div>
              <span style="font-size: 14px;">{{ t('login.loginTypeText') }}：</span>
            </div>
            <div style="align-items: center; width: 70%; display: flex; justify-content: center; ">
              <span style="font-size: 13px; text-decoration: underline;">{{ t('login.loginTypeUserNamePassword')
                }}</span>
              <span style="font-size: 13px; text-decoration: underline; margin-left: 20px;">{{ t('login.loginTypeSms')
                }}</span>
              <span style="font-size: 13px; text-decoration: underline; margin-left: 20px;">{{
                t('login.loginTypeWechat') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import UserLoginApi from "@/api/UserLoginApi.js";
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import { message } from 'ant-design-vue'; // 添加这行导入
import { useRoute, useRouter } from "vue-router";
import { useI18n } from 'vue-i18n';
const route = useRoute();
const router = useRouter();

const userName = ref('')
const password = ref('')
const loginType = ref(1)



const { locale, t } = useI18n();
const currentLocale = ref(locale.value);

const handleLocaleChange = (value) => {
  locale.value = value;
  currentLocale.value = value;
  console.log("local->" + value)
  localStorage.setItem('locale', value);
};

const onLogin = () => {
  try {
    let encryptPassword = encrypt_url(password.value);

    let payload = {
      "userName": userName.value,
      "password": encryptPassword,
      "loginType": loginType.value,
    }
    UserLoginApi.doLogin(payload).then(res => {
      // 从返回的Map中解构获取对应的值
      //lastSessionWords.value = res.lastSessionWords;
      if (res.result === 1) {
        authorization.set(res);
        let encryptedToken = encrypt_url(res.token);
        console.log(">>>>>>encryptedToken->" + encryptedToken);
        intoMaster();
        setLocale();
      }
      console.log(">>>>>>login response->" + JSON.stringify(res));
      console.log(">>>>>>userName->" + authorization.getUserName());
      console.log(">>>>>>token->" + authorization.getToken());
    }).catch(err => {
      console.log("login api error->" + err);
      message.error("登录出错");
    });
  }
  catch (err) {
    console.log(">>>>>>login error->" + err);
  }
}
const setLocale = () => {
  try {
    let encryptPassword = encrypt_url(password.value);
    let localeValue=localStorage.getItem('locale');
    let payload = {
      "userName": userName.value,
      "password": encryptPassword,
      "localeValue": localeValue,
    }
    UserLoginApi.setLocale(payload).then(res => {
    }).catch(err => {
      console.log("setLocale api error->" + err);
    });
  } catch (err) {
    console.error(">>>>>>setLocal error", err);
  }
}

const intoMaster = () => {
  router.replace({
    name: "Center",
    query: route.query || {},
    params: {
      channel: route.params?.channel || "default"
    }
  });
}



</script>
<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.login-box {
  display: flex;
  width: 50%;
  height: 500px;
  border: 1px solid #0E2841;
  background-color: #fff;
}

.login-left {
  width: 500px;
  height: 100%;
}

.login-left img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
  margin-left: 30px;
  position: relative;
  /* 添加这行 */
}

/* 添加新的样式 */
.language-selector {
  position: absolute;
  top: 20px;
  right: 20px;
  width: auto;
}
</style>