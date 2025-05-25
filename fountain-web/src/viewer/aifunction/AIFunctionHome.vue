<template>
  <div class="ai-function-container">
    <div class="tab-container">
      <div 
        class="tab-item" 
        :class="{ active: currentTab === 'function' }"
        @click="switchTab('function')"
        style="margin-right: 5px; font-size: 13px; color: #575757; border: 1px dashed #7f7f7f; border-top-left-radius: 8px; border-top-right-radius: 8px; padding: 8px 16px; cursor: pointer;"
      >
        AI函数设定
      </div>
      <div 
        class="tab-item" 
        :class="{ active: currentTab === 'dify' }"
        @click="switchTab('dify')"
        style="font-size: 13px; color: #575757; border: 1px dashed #7f7f7f; border-top-left-radius: 8px; border-top-right-radius: 8px; padding: 8px 16px; cursor: pointer;"
      >
        Dify设定
      </div>
    </div>
    
    <div class="content-container">
      <FunctionMgt v-if="currentTab === 'function'" />
      <DifyMgt v-if="currentTab === 'dify'" />
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import FunctionMgt from '@/viewer/aifunction/FunctionMgt.vue'
import DifyMgt from '@/viewer/aifunction//DifyMgt.vue'



export default {
  name: 'AIFunctionHome',
  components: {
    FunctionMgt,
    DifyMgt
  },
  setup() {
    const currentTab = ref('function')

    const switchTab = (tab) => {
      currentTab.value = tab
    }

    return {
      currentTab,
      switchTab
    }
  }
}
</script>

<style scoped>
.ai-function-container {
  padding: 20px;
}

.tab-container {
  display: flex;
  align-items: center;
}

.tab-item {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.tab-item.active {
  border: 1px solid blue !important;
}

.content-container {
  margin-top: 20px;
}
</style>