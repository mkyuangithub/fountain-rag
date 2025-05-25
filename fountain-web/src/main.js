import {createApp} from "vue"

import Router from "@/router";

import {createPinia} from "pinia";

import AntDesign from "ant-design-vue";
import * as Icons from '@ant-design/icons-vue';
import "ant-design-vue/dist/reset.css";

import {PerfectScrollbarPlugin} from "vue3-perfect-scrollbar";
import "vue3-perfect-scrollbar/style.css";

import {isLogin} from "@/toolkit/utils.js";
import zhCN from 'ant-design-vue/es/locale/zh_CN' // 导入中文语言包


import "@/main.css";
import "@/global.css"
import Main from "@/Main.vue";
import i18n from "@/i18n.js";  // 使用 @/ 路径别名
const application = createApp(Main);
// 添加这段代码来全局注册所有图标
Object.keys(Icons).forEach(key => {
    application.component(key, Icons[key]);
});

application.use(Router);
application.use(AntDesign);
application.use(createPinia());
application.use(PerfectScrollbarPlugin);
application.use(i18n);

application.mount("#root");
