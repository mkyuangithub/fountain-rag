<template>
    <div class="knowledge-detail">

        <div style="display: flex; justify-content: space-between;">
            <div style="width: 100%;">
                <a-button type="text" @click="handleBack"
                    style="padding: 0; height: auto; font-size: 13px; color: #595959;">
                    <template #icon>
                        <arrow-left-outlined style="font-size: 13px; color: #595959;" />
                    </template>
                </a-button>

                <span style="font-size: 13px; color: #595959; cursor: pointer;" @click="handleBack">返回</span>
            </div>
            <div style="display:flex">
                <a-button style="font-size: 13px; color: white;" size="small" type="primary" @click="selectAll">
                    <CheckOutlined />全部选中
                </a-button>
                <a-button style="font-size: 13px; color: white; margin-left: 10px;" size="small" type="primary"
                    @click="removeBatchItems">
                    <DeleteOutlined />删除选中
                </a-button>
            </div>
        </div>

        <div v-if="knowledgeRepo"
            style="margin-top: 10px;  margin-bottom: 15px;  width: 100%;   align-items: center; justify-content: center;">
            <div
                style="font-size: 14px; font-weight: bold; display: flex; align-items: center; justify-content: center; width: 100%; ">
                当前知识库: {{ knowledgeRepo.knowledgeName }}
            </div>
            <div
                style="font-size: 13px; color:#7F7F7F; display: flex; align-items: center; justify-content: center; width: 100%; margin-top: 5px;">
                ({{ knowledgeRepo.knowledgeRepoDescription }})
            </div>
        </div>
        <div style="display: flex; justify-content: space-between;">
            <!--添加记录-->
            <div
                style="font-size: 13px; color:#595959; margin-top: 10px; margin-left: 10px; display: flex; align-items: center;">
                <span>
                    手工添加记录:
                </span>
                <div style="cursor: pointer; display: flex; margin-top: 0px;  align-items: center; margin-left: 10px;"
                    @click="addKnowledgeDetail">
                    <PlusCircleOutlined style="color: blue; font-size: 14px;" />
                </div>
            </div>
            <!--搜索相关-->
            <div style="display: flex; margin-top: 10px; align-items: center; width: 800px; margin-left: 20px;">
                <div style="width:5%">
                    <span style="font-size: 13px; color: dodgerblue;">搜索:</span>
                </div>
                <div style="width: 70%;">
                    <a-input v-model:value="searchedContent" autocomplete="off" placeholder="按照内容搜索" allowClear
                        style="width: 100%; color:#595959; margin-left: 10px;" />
                </div>
                <div style="width: 20%;">
                    <input type="checkbox" v-model="searchWithEmbedding" style="margin-left: 20px;">
                    <span style="font-size: 13px; color: #595959; margin-left: 10px;">相似度搜索</span>
                </div>
                <div>
                </div>
                <div style="width: 5%;">
                    <img src="/assets/images/search-blue.png" style="width: 18px; height: 18px; cursor: pointer;"
                        @click="handleSearch">
                </div>
            </div>
        </div>
        <!--展示具体知识库列表-->
        <template v-if="knowledgeDetails && knowledgeDetails.length > 0">

            <!--具体数据-->
            <div
                style="display: flex; flex-wrap: wrap; gap: 5px; width: 100%; overflow-x: auto; padding-bottom: 3px; margin-top: 6px;">
                <div v-for="detailItem in knowledgeDetails" :key="detailItem.id"
                    style="width: 400px; height: 400px; margin-bottom: 10px;  cursor:pointer;
                           border: 1px solid #47D45A; border-radius: 3%; padding: 16px; transition: all 0.3s ease;  flex-shrink: 0;" :style="{
                            'box-shadow': detailItem.isHovered ? '0 6px 12px rgba(59, 33, 251, 0.15)' : '0 2px 4px rgba(0,0,0,0.05)',
                            'border-color': detailItem.isHovered ? '#3B21FB' : '#47D45A',
                            'transform': detailItem.isHovered ? 'translateY(-2px)' : 'translateY(0)'
                        }">
                    <!--具体条目的内容显示区域-->
                    <div style="height: 130px; overflow-y: auto; width:100%; padding: 0">
                        <!--可以多选条目以便于进行删除-->
                        <div>
                            <a-checkbox :value="detailItem.id" v-model:checked="detailItem.isSelected"
                                style="transform: scale(0.85); margin-top: 2px;"></a-checkbox>
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <div style="display: flex; align-items: center;">
                                <div style="font-size: 12px; color:#595959; font-weight: bold;">
                                    内容:
                                </div>
                                <div>
                                    <a-button type="text" style="font-size: 12px; color:blue;"
                                        @click="popDetail(detailItem)">
                                        <EyeOutlined />
                                        编辑
                                    </a-button>
                                </div>
                            </div>
                            <div>
                                <a-button type="text" style="font-size: 12px; color:red;"
                                    @click="deleteDetailItem(detailItem)">
                                    <MinusCircleOutlined />
                                    删除
                                </a-button>
                            </div>
                        </div>
                        <a-tooltip placement="bottom"
                            :overlayStyle="{ maxWidth: '600px', wordBreak: 'break-word', whiteSpace: 'pre-wrap' }"
                            :mouseEnterDelay="0.3">
                            <template #title>
                                <span>{{ detailItem.originalContent }}</span>
                            </template>
                            <div style="font-size: 12px; color:#595959; margin-top: 5px; width: 100%; 
                                white-space: pre-wrap;
                                word-wrap: break-word;
                                word-break: break-word;" @click="popDetail(detailItem)">
                                {{ detailItem.originalContent.length > 200 ?
                                    detailItem.originalContent.substring(0, 200) + '...' :
                                    detailItem.originalContent }}
                            </div>
                        </a-tooltip>
                    </div>
                    <!--标签显示区域-->
                    <div style="max-height: 200px; overflow-y: auto;">
                        <div style="font-size: 11px; color:#595959; font-weight: bold; margin-top: 8px;">
                            标签:
                        </div>
                        <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 5px;">
                            <div v-for="(label, index) in detailItem.labels" :key="index" style="display: inline-flex; align-items: center; padding: 4px 12px; 
                                        background: white; border: 1px dashed #00B0F0; 
                                        border-radius: 5%; position: relative;">
                                <span style="font-size: 11px; color:#595959">{{ label }}</span>
                            </div>
                        </div>
                    </div>
                    <!--具体条目的页脚区域-->
                    <div style="">
                        <div style="border-top: 1px solid #47D45A; margin: 0; width: 100%;"></div>
                        <div style="padding: 10px 16px;">
                            <div style="display: flex; ">
                                <p style="color: #595959; font-size: 12px; margin-bottom: 8px;">
                                    来自文件: <font style="color:blue; text-decoration: underline;">{{ detailItem.fileName
                                        }}</font>
                                </p>

                            </div>
                            <div style="display: flex; justify-content: space-between;">
                                <p style="color: #595959; font-size: 12px; margin: 0;">
                                    创建日期: {{ formatDate(detailItem.createdDate) }}
                                </p>
                                <p style="color: #595959; font-size: 12px; margin: 0;">
                                    创建人: {{ detailItem.userName }}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
            <!-- 分页器 -->
            <div style="margin-top: 20px; display: flex; justify-content: center;">
                <a-pagination v-model:current="currentPage" :total="total" :pageSize="pageSize"
                    @change="handlePageChange" />
            </div>
        </template>
        <template v-else>
            <div
                style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;">
                <div style="color: #7F7F7F; font-size: 16px; margin-bottom: 16px;">
                    该库内目前无任何数据
                </div>
            </div>
        </template>
        <PopKnowledgeDetail v-model="popDetailShow" @refresh-detail="fetchKnowledgeRepos(1)"
            :item="selectedDetailItem" />
        <PopAddKnowledgeDetail v-model="popAddDetailShow" @refresh-detail="fetchKnowledgeRepos(1)" :repoId="repoId" />
    </div>
</template>

<script setup>
import { defineEmits, defineProps, ref, onMounted, reactive, watch } from 'vue';
import { message } from 'ant-design-vue';
import { Modal } from 'ant-design-vue';
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import authorization from "@/toolkit/authorization.js";
import PopKnowledgeDetail from "@/viewer/knowledge/PopKnowledgeDetail.vue";
import PopAddKnowledgeDetail from "@/viewer/knowledge/PopAddKnowledgeDetail.vue";
import KnowledgeVectorApi from "@/api/KnowledgeVectorApi.js";
import KnowledgeMgtApi from "@/api/KnowledgeMgtApi.js";
import { DeleteOutlined } from '@ant-design/icons-vue';
/*分页相关 开始*/
const searchedContent = ref('');
const knowledgeDetails = ref([]);
const currentPage = ref(1);
const pageSize = ref(50);
const total = ref(0);
const popDetailShow = ref(false);//用于控制弹出具体条目编辑用
const popAddDetailShow = ref(false); //用于控制弹出添加具体的一条条目用
const selectedDetailItem = ref(null);
const searchWithEmbedding = ref(false);
const formatDate = (date) => {
    return new Date(date).toLocaleDateString();
};
/*分页相关 结束*/
const knowledgeRepo = ref();//通过repoId获取knowledgeRepo全信息
const props = defineProps({
    repoData: {
        type: Object,
        default: () => ({})
    },
    lastPage: {  // 接收上一页的页码
        type: Number,
        default: 1
    }
});

const emits = defineEmits(['switchComponent']);

// 创建一个ref来存储repoId，方便全局使用
const repoId = ref(null);

onMounted(() => {

});

const handleBack = () => {
    console.log(">>>>>>before back the lastPage is->"+props.lastPage);
    emits('switchComponent', {
        component: 'KnowledgeMain',
        props: {
            returnedPage: props.lastPage  // 用一个不同的名字避免冲突
        }
    });
}

/*单击某一个知识条目时要弹出明细以便于编辑 */
const popDetail = async (detailItem) => {
    popDetailShow.value = true;
    selectedDetailItem.value = detailItem;
}

/* 单击添加按钮，弹出添加一条具体条目 */
const addKnowledgeDetail = async () => {
    popAddDetailShow.value = true;
}

const removeBatchItems = async () => {
    try {
        // 获取所有选中的条目ID
        const selectedIds = knowledgeDetails.value
            .filter(item => item.isSelected)
            .map(item => item.id);

        // 判断是否至少选中一条记录
        if (selectedIds.length === 0) {
            message.warn("请选择至少一条记录以作删除");
            return;
        }

        // 显示确认对话框
        Modal.confirm({
            title: '确认删除',
            content: '此方法将彻底删除选中的知识条目，请确认',
            okText: '确认',
            cancelText: '取消',
            onOk: () => {
                // 这里实现删除逻辑
                console.log("删除的ID列表:", selectedIds);
                let token = authorization.getToken();
                let encryptToken = encrypt_url(token);
                let userName = authorization.getUserName();
                let payload = {
                    "token": encryptToken,
                    "userName": userName,
                    "knowledgeRepoId": repoId.value,
                    "detailIds": selectedIds,
                }
                KnowledgeMgtApi.deleteDetailKnowledgeItem(payload).then(res => {
                    // 从返回的列表中先拿到总条数
                    if (res === 1) {
                        message.success("删除成功");
                    } else if (res === 101) {
                        message.warn("有上传进行中，请待上传结束后再删除");
                    } else {
                        message.error("删除错误");
                    }
                }).catch(err => {
                    console.log('>>>>>>delete  detail knowledge item from api error:', err);
                }).finally(() => {
                    getKnowledgeInfo();
                    fetchKnowledgeRepos(1);
                });
            }
        });
    } catch (err) {
        console.log(">>>>>>removeBatchItems error", err);
    }
}

const selectAll = () => {
    // 检查是否所有项目都已选中
    const allSelected = knowledgeDetails.value.every(item => item.isSelected);
    // 如果全部选中，则取消全选；否则全选
    knowledgeDetails.value = knowledgeDetails.value.map(item => ({
        ...item,
        isSelected: !allSelected
    }));
};

const deleteDetailItem = async (detailItem) => {
    Modal.confirm({
        title: '确认删除',
        content: '您确认删除当前数据？此操作会影响到聊天会话中的数据准确性且不可撤销!',
        okText: '确定',
        cancelText: '取消',
        onOk: () => {
            try {

                let token = authorization.getToken();
                let encryptToken = encrypt_url(token);
                let userName = authorization.getUserName();
                let payload = {
                    "token": encryptToken,
                    "userName": userName,
                    "knowledgeRepoId": detailItem.knowledgeRepoId,
                    "detailIds": [detailItem.id],
                }
                KnowledgeMgtApi.deleteDetailKnowledgeItem(payload).then(res => {
                    // 从返回的列表中先拿到总条数
                    if (res === 1) {
                        message.success("删除成功");
                    } else if (res === 101) {
                        message.warn("有上传进行中，请待上传结束后再删除");
                    } else {
                        message.error("删除错误");
                    }
                }).catch(err => {
                    console.log('>>>>>>delete  detail knowledge item from api error:', err);
                }).finally(() => {
                    getKnowledgeInfo();
                    fetchKnowledgeRepos(1);
                });
            } catch (error) {
                console.log('>>>>>>delete  detail knowledge item error:', error);
            }
        },
    });


}

/*删除知识库中的某一个标签 */
const removeLabel = async (detailItem, index) => {
    try {

    } catch (err) {
        console.log(">>>>>>删除知识库中某一个标签动作出错->" + err);
    }
};

const getKnowledgeInfo = async () => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "repoId": repoId.value
        }
        KnowledgeVectorApi.getKnowledgeInfo(payload).then(res => {
            // 从返回的列表中先拿到总条数
            knowledgeRepo.value = res;
        }).catch(err => {
            console.log("通过repoId获取知识库失败" + err);
        });
    } catch (error) {
        console.log('通过repoId获取知识库失败:' + error);
    }
}

const handleSearch = async () => {
    try {
        currentPage.value = 1;
        pageSize.value = 12;
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "repoId": repoId.value,
            "pageNumber": 1,
            "pageSize": pageSize.value,
            "searchedContent": searchedContent.value,
            "isEmbedding": searchWithEmbedding.value,
        }
        KnowledgeVectorApi.listDetail(payload).then(res => {
            // 从返回的列表中先拿到总条数
            total.value = res.totalElements;
            // 从返回的Map中解构获取对应的值
            knowledgeDetails.value = res.content.map(item => ({
                ...item
            }));
            if (searchWithEmbedding.value) {
                pageSize.value = total.value;
            }
        }).catch(err => {
            console.log("搜索知识库内条目失败", err);
        });
    } catch (error) {
        console.log('搜索知识库内条目失败:', error);
    }
}

const fetchKnowledgeRepos = async (page) => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "repoId": repoId.value,
            "pageNumber": currentPage.value,
            "pageSize": pageSize.value,
        }
        KnowledgeVectorApi.listDetail(payload).then(res => {
            // 从返回的列表中先拿到总条数
            total.value = res.totalElements;
            // 从返回的Map中解构获取对应的值
            knowledgeDetails.value = res.content.map(item => ({
                ...item,
                isSelected: false  // 添加选中状态属性，默认为未选中
            }));
        }).catch(err => {
            console.log("获取详细知识库内条目失败", err);
        });
    } catch (error) {
        console.log('获取详细知识库内条目失败:', error);
    }
};
const handlePageChange = (page) => {
    currentPage.value = page;
    fetchKnowledgeRepos(page);
};
// 监听props.repoData的变化
watch(() => props.repoData, (newValue) => {
    if (newValue && newValue.id) {
        repoId.value = newValue.id;
        fetchKnowledgeRepos(1);
        getKnowledgeInfo();
    }
}, { immediate: true });  // immediate: true 确保首次加载时也会执行

</script>
<style scoped>
.knowledge-detail {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100%;

}

.small-checkbox :deep(.ant-checkbox-inner) {
    width: 14px;
    height: 14px;
}

.small-checkbox :deep(.ant-checkbox-inner::after) {
    width: 4px;
    height: 8px;
    top: 45%;
    left: 22%;
}
</style>