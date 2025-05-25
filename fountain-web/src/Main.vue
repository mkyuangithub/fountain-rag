<template>
	<a-config-provider :theme="{ token: { borderRadius: 2, colorPrimary: '#0d2f5b' } }" :locale="locale">
		<router-view />
	</a-config-provider>
</template>
<script setup>
import { onMounted, ref, watch } from "vue";

import settings from "@/toolkit/settings.js";

import { useI18n } from 'vue-i18n';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import enUS from 'ant-design-vue/es/locale/en_US';
const { locale } = useI18n();
const antdLocale = ref(zhCN);

onMounted(() => {
	// 在组件挂载时读取保存的语言设置
	const savedLocale = localStorage.getItem('locale');
	if (savedLocale) {
		locale.value = savedLocale;
		antdLocale.value = savedLocale === 'zh' ? zhCN : enUS;
	}

});


// 监听语言变化
watch(locale, (newLocale) => {
	antdLocale.value = newLocale === 'zh' ? zhCN : enUS;
	localStorage.setItem('locale', newLocale);
});

</script>
<style scoped></style>