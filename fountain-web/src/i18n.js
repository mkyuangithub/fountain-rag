import { createI18n } from 'vue-i18n'
import en from './locales/en.js'
import zh from './locales/zh.js'

const i18n = createI18n({
  legacy: false, // 使用组合式API
  locale: localStorage.getItem('locale') || 'zh', // 默认语言
  fallbackLocale: 'zh', // 备用语言
  messages: {
    en,
    zh
  }
})

export default i18n