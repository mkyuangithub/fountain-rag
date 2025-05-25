<template>
    <div style="width: 100%; min-height: 100%; padding: 0; margin: 0;">
        <template v-if="knowledgeRepos && knowledgeRepos.length > 0">
            <input type="file" ref="fileInput" @change="onUploadFileSelected" accept="*" style="display: none" />
            <div style="width:100%; display: flex; justify-content: space-between;">
                <!--添加知识库按钮-->
                <div style="cursor: pointer; display: flex; margin-top: 0px;  margin-bottom: 20px; align-items: center; "
                    @click="handleAddRepo">
                    <div
                        style="width: 25px; height: 25px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                        <PlusOutlined style="color: blue; font-size: 13px;" />
                    </div>
                    <span style="color: #595959;  margin-left: 10px;  font-size:13px;">{{
                        t('knowledgeMain.addKnowledgeButtonText') }}:</span>
                </div>
                <!--刷新按钮-->
                <div>
                    <a-button type="text" size="small" @click="handleRefreshKnowledge()">
                        <SyncOutlined />{{ t('knowledgeMain.refreshButtonText') }}
                    </a-button>
                </div>
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 5px 5px; width: 100%; padding: 0">
                <div v-for="repo in knowledgeRepos" :key="repo.id"
                    style="width: calc(50% - 10px); height: 665px; margin-bottom: 10px; border: 1px solid #4E95D9; border-radius: 3%; padding: 10px; cursor: pointer; transition: all 0.3s ease;"
                    :style="{
                        'box-shadow': repo.isHovered ? '0 6px 12px rgba(59, 33, 251, 0.15)' : '0 2px 4px rgba(0,0,0,0.05)',
                        'border-color': repo.isHovered ? '#3B21FB' : '#4E95D9',
                        'transform': repo.isHovered ? 'translateY(-2px)' : 'translateY(0)'
                    }" @mouseenter="repo.isHovered = true" @mouseleave="repo.isHovered = false">

                    <!-- 头部区域 -->
                    <div style="height: 50px; border-bottom: 1px solid #4E95D9; margin: 0 -10px; padding: 0 10px;">
                        <h3 style="color: #595959; font-size: 13px; margin: 0;">{{ repo.title }}</h3>
                        <div style="display: flex; justify-content: space-between;">
                            <div style="display: flex; align-items: center;">
                                <span style="font-size: 13px; color:#595959">
                                    {{ t('knowledgeMain.knowledgebaseName') }}：{{ repo.knowledgeName }}
                                </span>
                            </div>
                            <!--头部功能按钮区域-->
                            <div style="display: flex;">
                                <div>
                                    <a-tooltip title="进入知识库" placement="bottom">
                                        <a-button type="text" size="small" @click="handleViewRepo(repo)">
                                            <EyeOutlined />
                                        </a-button>
                                    </a-tooltip>
                                </div>
                                <div>
                                    <a-tooltip title="上传文件" placement="bottom">
                                        <a-button type="text" size="small" @click="handleImport(repo)">
                                            <CloudUploadOutlined />
                                        </a-button>
                                    </a-tooltip>
                                </div>
                                <div>
                                    <a-tooltip title="删除" placement="bottom">
                                        <a-button type="text" size="small" @click="handleRemove(repo)">
                                            <DeleteOutlined />
                                        </a-button>
                                    </a-tooltip>
                                </div>
                                <div>
                                    <a-tooltip title="重新索引" placement="bottom">
                                        <a-button type="text" size="small" @click="handleFullIndex(repo)">
                                            <ClusterOutlined />
                                        </a-button>
                                    </a-tooltip>
                                </div>
                                <div>
                                    <template v-if="esAvailable">
                                        <a-tooltip title="同步es" placement="bottom">
                                            <a-button type="text" size="small" @click="syncToEs(repo)">
                                                <InteractionOutlined />
                                            </a-button>
                                        </a-tooltip>
                                    </template>
                                    <template v-else>
                                        <a-tooltip title="es不可用" placement="bottom">
                                            <a-button type="text" size="small" style="color: #c7c7c7" disabled>
                                                <InteractionOutlined />
                                            </a-button>
                                        </a-tooltip>
                                    </template>

                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 内容区域 -->
                    <div style="height: calc(100% - 80px); overflow-y: auto; width:100%; padding: 0">

                        <!-- 显示介绍-->
                        <div style="display: flex; align-items: center;  margin-top: 5px; width: 100%;">
                            <span style="font-size: 12px; color: #595959; width:200px">
                                {{ t('knowledgeMain.knowledgebaseDescription') }}:
                            </span>
                            <span style="font-size: 12px; color: #595959; width:85%">
                                {{ repo.knowledgeRepoDescription }}
                            </span>
                        </div>
                        <!--
                          切分方式的设置
                        -->
                        <div style="margin: 0px; padding: 0 0;">
                            <a-collapse v-model:activeKey="repo.splityMethodSelectKey" :bordered="false"
                                style="padding: 0 0;">
                                <a-collapse-panel key="1">
                                    <template #header>
                                        <span style="font-size: 12px; color:#595959; margin-left: 0px; padding: 0 0">{{
                                            t('knowledgeMain.paragraphSplitMethod.labelText') }}</span>
                                    </template>
                                    <div>
                                        <a-checkbox :checked="repo.splitType === 0"
                                            @change="(checked) => handleSplitTypeChange(repo, 'default', checked)"
                                            style="font-size: 11px;">{{
                                                t('knowledgeMain.paragraphSplitMethod.default') }}</a-checkbox>
                                    </div>
                                    <div style="display: flex; align-items: center; font-size: 12px; color: #595959;">
                                        <a-checkbox :checked="repo.splitType === 1"
                                            @change="(checked) => handleSplitTypeChange(repo, 'paragraph', checked)"
                                            style="font-size: 11px;">{{
                                                t('knowledgeMain.paragraphSplitMethod.splitByParagraph') }}</a-checkbox>
                                        <span style="margin: 0 5px;font-size: 12px; color: #595959;">{{
                                            t('knowledgeMain.paragraphSplitMethod.paragrahMark') }}:</span>
                                        <a-input v-model:value="repo.paragraphMark"
                                            style="width: 50px; height:30px;margin-right: 5px; font-size: 11px; color: #595959;" />
                                        <span style="margin-right: 5px;font-size: 11px; color: #595959;">{{
                                            t('knowledgeMain.paragraphSplitMethod.slideNums') }}:</span>
                                        <a-input v-model:value="repo.slideNums"
                                            style="width: 60px; height:30px;  font-size: 11px; color: #595959;"
                                            type="number" :min="1" />
                                    </div>
                                    <div style="display: flex; align-items: center;">
                                        <div>
                                            <a-checkbox :checked="repo.splitType === 2"
                                                @change="(checked) => handleSplitTypeChange(repo, 'page', checked)"
                                                style="font-size: 12px;">{{
                                                    t('knowledgeMain.paragraphSplitMethod.byPage') }}</a-checkbox>
                                        </div>
                                    </div>
                                    <div>
                                        <a-button type="primary" size="small" style="margin-top: 10px;"
                                            @click="setSplit(repo)">
                                            <SettingOutlined />
                                            <span style="font-size: 12px; color:white">{{
                                                t('knowledgeMain.paragraphSplitMethod.settingButtonText') }}</span>
                                        </a-button>
                                    </div>
                                    <div
                                        style="font-size: 12px; color: #595959; display: flex; align-items: center; margin-top: 10px;">
                                        <span style="font-size: 12px; color:#595959; width:15%;">{{
                                            t('knowledgeMain.paragraphSplitMethod.needAiSplitText') }}：</span>
                                        <a-radio-group v-model:value="repo.needSplit"
                                            style="font-size: 12px; color:#595959">
                                            <a-radio :value="false"><span style="font-size: 12px; color:#595959">{{
                                                t('knowledgeMain.paragraphSplitMethod.needAiSplitNo')
                                                    }}</span></a-radio>
                                            <a-radio :value="true"><span style="font-size: 12px; color:#595959">{{
                                                t('knowledgeMain.paragraphSplitMethod.needAiSplitYes')
                                                    }}</span></a-radio>
                                        </a-radio-group>
                                    </div>
                                    <div style="font-size: 12px; color: #595959; display: flex; align-items: center;">
                                        <span style="font-size: 12px; color:#595959; width:15%;">{{
                                            t('knowledgeMain.paragraphSplitMethod.needParsePicText') }}：</span>
                                        <a-radio-group v-model:value="repo.readImg"
                                            style="font-size: 12px; color:#595959">
                                            <a-radio :value="false"><span style="font-size: 12px; color:#595959">{{
                                                t('knowledgeMain.paragraphSplitMethod.needParsePicNo')
                                                    }}</span></a-radio>
                                            <a-radio :value="true"><span style="font-size: 12px; color:#595959">{{
                                                t('knowledgeMain.paragraphSplitMethod.needParsePicYes')
                                                    }}</span></a-radio>
                                        </a-radio-group>
                                    </div>
                                </a-collapse-panel>
                            </a-collapse>
                        </div>
                        <!--上传进度显示-->
                        <!--
                        <div v-if="processing"
                        -->
                        <div v-if="repo.importStatus !== undefined"
                            style="display: flex; align-items: center; margin-top: 10px;">
                            <div
                                style="width: 120px; height: 10px; background-color: #f0f0f0; border-radius: 5px; overflow: hidden;">
                                <div :style="{
                                    width: `${repo.importStatus || 0}%`,
                                    height: '100%',
                                    backgroundColor: '#ff0000',
                                    transition: 'width 0.3s ease'
                                }"></div>
                            </div>
                            <span style="font-size: 11px; color: blue;">{{
                                t('knowledgeMain.trainProcessLabelText')
                                }}：{{ repo.importStatus || 0 }}%</span>
                            <div v-if="repo.currentStep && repo.currentStep.trim().length > 0"
                                style="margin-left: 10px;">
                                <span style="font-size: 11px; color: #595959;">{{
                                    t('knowledgeMain.trainProcessStepLabelText')
                                    }}: </span><span style="font-size: 11px; color: blue;">{{
                                        repo.currentStep }}</span>
                            </div>
                            <div style="margin-left: 20px;">
                                <a-button type="text"
                                    style="background-color: red; color:white; font-size: 12px; margin-left: 10px;"
                                    @click="stopUploadProcess">
                                    <StopOutlined />{{ t('knowledgeMain.trainProcessStop') }}
                                </a-button>
                            </div>
                        </div>


                        <!-- 显示提示语-->
                        <div style="align-items: center;  margin-top: 5px; width: 100%;">
                            <span style="font-size: 12px; color: #595959; width:200px">
                                {{ t('knowledgeMain.labelPromptText') }}:
                            </span>
                            <a-textarea v-model:value="repo.majorPrompt" :rows="10"
                                :auto-size="{ minRows: 8, maxRows: 8 }"
                                style="resize: none; width: 100%; margin-top: 5px; font-size: 12px;"
                                :placeholder="t('knowledgeMain.labelPromptPlaceholder')" />
                            <div style="display: flex; justify-content: space-between; margin-top: 5px;">
                                <div style="display:flex; font-size: 12px; color: #595959">
                                    <a-button type="primary" size="small" @click="handleLabelRepo(repo)">
                                        <BookOutlined />
                                        <span style="font-size: 12px; color:white">{{ t('knowledgeMain.labelButtonText')
                                            }}</span>
                                    </a-button>
                                    <a-radio-group :value="getLabelAction(repo.id)"
                                        @update:value="(val) => setLabelAction(repo.id, val)" size="small"
                                        style="margin-left: 5px;">
                                        <a-radio :value="1"><span style="font-size: 13px; color:#595959">{{
                                            t('knowledgeMain.labelBehaviorAppendText') }}</span></a-radio>
                                        <a-radio :value="2"><span style="font-size: 13px; color:#595959">{{
                                            t('knowledgeMain.labelBehaviorOverrideText') }}</span></a-radio>
                                    </a-radio-group>
                                </div>
                                <div>
                                    <a-button type="primary" size="small" @click="handleSaveMajorPrompt(repo)">
                                        <CheckOutlined />
                                        <span style="font-size: 12px; color:white">{{
                                            t('knowledgeMain.labelPromptSaveButtonText') }}</span>
                                    </a-button>
                                </div>
                            </div>
                        </div>
                        <!--内容区域显示标签用-->
                        <div style="align-items: center;  margin-top: 10px; width: 100%; ">
                            <div style="display: flex; align-items: center;">
                                <div style="font-size: 12px; color:#595959">
                                    {{ t('knowledgeMain.currentRepoLabel') }}
                                </div>
                                <div style="cursor: pointer; display: flex; margin-top: 0px;  align-items: center; margin-left: 10px;"
                                    @click="handleAddLabel(repo)">
                                    <PlusCircleOutlined style="color: blue; font-size: 12px;" />
                                    <span style="color: #595959;  margin-left:5px;  font-size:12px;">{{
                                        t('knowledgeMain.labelByManualButtonText') }}</span>
                                </div>
                                <a-input v-model:value="newLabel"
                                    :placeholder="t('knowledgeMain.labelByManualPlaceholder')"
                                    :class="custom - lable - textbox"
                                    style="height: 24px; width: 50%; margin-left: 5px;" />
                            </div>
                            <div style="max-height: 145px; overflow-y: auto;">
                                <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 5px;">
                                    <div v-for="(label, index) in repo.labelList" :key="index" style="display: inline-flex; align-items: center; padding: 4px 12px; 
                                        background: white; border: 1px dashed #00B0F0; 
                                        border-radius: 5%; position: relative;">
                                        <span style="font-size: 11px; color:#595959">{{ label }}</span>
                                        <span style="cursor: pointer; margin-left: 4px; font-size: 12px;"
                                            @click="removeLabel(repo, index)">×</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <!--内容区域显示数据用-->
                        <div style="align-items: center;  margin-top: 20px; width: 100%; ">
                            <!--第一行，显示文件相关信息-->
                            <div style="display: flex; justify-content: space-between;">
                                <div style="font-size: 12px; color:#595959; display: flex;">
                                    <div style="font-size: 12px; color:#595959">
                                        {{ t('knowledgeMain.repoContainFileNumText') }}:
                                    </div>
                                    <div style="font-size: 12px; color:#595959; margin-left: 10px;">
                                        <span style="font-size: 12px; color:blue; margin-left: 5px;">{{ repo.fileCount
                                            }}</span>
                                    </div>
                                </div>
                                <div style="font-size: 12px; color:#595959; display: flex;">
                                    <div style="font-size: 12px; color:#595959">
                                        {{ t('knowledgeMain.repoContainRecordNumText') }}:
                                    </div>
                                    <div style="font-size: 12px; color:#595959;">
                                        <span style="font-size: 12px; color:blue; margin-left: 5px;">{{ repo.itemCount
                                            }}</span>
                                    </div>
                                </div>
                            </div>

                            <!--第二行，显示训练相关信息-->
                            <div style="display: flex; justify-content: space-between; margin-top: 10px;">
                                <div style="font-size: 12px; color:#595959; display: flex;">
                                    <div style="font-size: 12px; color:#595959">
                                        {{ t('knowledgeMain.repoContainSuccessNumText') }}:
                                    </div>
                                    <div style="font-size: 12px; color:#595959; margin-left: 10px;">
                                        <span style="font-size: 12px; color:blue; margin-left: 5px;">{{
                                            repo.successCount
                                        }}</span>
                                    </div>
                                </div>
                                <div style="font-size: 12px; color:#595959; display: flex;">
                                    <div style="font-size: 12px; color:#595959">
                                        {{ t('knowledgeMain.repoContainFailNumText') }}:
                                    </div>
                                    <div style="font-size: 12px; color:#595959;">
                                        <span style="font-size: 12px; color:red; margin-left: 5px;">{{ repo.failCount
                                            }}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 底部信息区域 -->
                    <div style="margin: 0 -10px;">
                        <div style="border-top: 1px solid #4E95D9; margin: 0; width: 100%;"></div>
                        <div style="padding: 10px 10px;">
                            <div style="display: flex; justify-content: space-between;">
                                <p style="color: #595959; font-size: 13px; margin: 0;">
                                    {{ t('knowledgeMain.createdDateText') }}: {{ formatDate(repo.createdDate) }}
                                </p>
                                <p style="color: #595959; font-size: 13px; margin: 0;">
                                    {{ t('knowledgeMain.createdByText') }}: {{ repo.userName }}
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
        <!-- 无数据时显示空状态 -->
        <template v-else>
            <div
                style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;">
                <div style="color: #7F7F7F; font-size: 16px; margin-bottom: 16px;">
                    目前暂无知识库内容
                </div>
                <div style="cursor: pointer; display: flex; flex-direction: column; align-items: center;"
                    @click="handleAddRepo">
                    <div
                        style="width: 40px; height: 40px; border: 1px dashed blue; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                        <PlusOutlined style="color: blue; font-size: 20px;" />
                    </div>
                    <span style="color: blue; margin-top: 8px;">去添加</span>
                </div>
            </div>
        </template>
    </div>
    <AddKnowledge v-model="addKnowledgeShow" @refresh-repo="fetchKnowledgeRepos" />
    <PopKnowledgeContentPreview v-model="popKnowledgeContentPreviewShow" :fileId="previewFileId"
        :needSplit="previewneedSplit" :contentJson="previewContentJson" :repoId="previewRepoId"
        :fileType="previewFileType" @execute-embedding="executeEmbedding" />
    <!--专门用来显示全屏遮罩-->
    <div v-if="loadingMask"
        style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(255, 255, 255, 0.7); display: flex; flex-direction: column; justify-content: center; align-items: center; z-index: 9999;">
        <img src="/assets/images/waiting-blue.gif" alt="loading" />
        <div style="margin-top: 10px; font-size: 16px; color: #1890ff; font-weight: 500;">AI生成中</div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive, defineEmits, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { message } from 'ant-design-vue';
import { Modal } from 'ant-design-vue';
import { encrypt, decrypt, encrypt_url } from "@/toolkit/secure.js";
import PopKnowledgeContentPreview from "@/viewer/knowledge/PopKnowledgeContentPreview.vue";
import authorization from "@/toolkit/authorization.js";
import KnowledgeMgtApi from "@/api/KnowledgeMgtApi.js";
import KnowledgeVectorApi from "@/api/KnowledgeVectorApi.js";
import AddKnowledge from "@/viewer/knowledge/AddKnowledge.vue";
import { InteractionOutlined } from '@ant-design/icons-vue';
const { locale, t } = useI18n();
const currentLocale = ref(locale.value);
const splityMethodSelectKey = ref(['0']);
const props = defineProps({
    returnedPage: {  // 使用不同的名字接收返回的页码
        type: Number,
        default: 1
    }
})
const previewFileId = ref('');
const previewNeedSplit = ref(false);
const previewContentJson = ref('');
const previewRepoId = ref('');
const previewFileType = ref(1);

const knowledgeRepos = ref([]);
const currentPage = ref(1);
const pageSize = ref(2);
const total = ref(0);
const newLabel = ref('');
const addKnowledgeShow = ref(false);
const popKnowledgeContentPreviewShow = ref(false);
const labelAction = ref(1);
const fileInput = ref(null);
const processing = ref(false);
// 在setup中添加遮罩控制的ref
const loadingMask = ref(false);

// 修改checkbox的处理方式
const handleSplitTypeChange = (repo, type, checked) => {
    if (checked) {
        // 根据类型设置对应的值
        if (type === 'default') {
            repo.splitType = 0
        } else if (type === 'page') {
            repo.splitType = 2
        } else {
            repo.splitType = 1
        }
    }
}
const stopUploadProcess = async () => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
        }
        const response = await KnowledgeVectorApi.stopUploadProcess(payload);
        message.success("上传进程已终止")
    } catch (err) {
        console.error(">>>>>>stop upload process api error:", err)
    }
}
const setSplit = async (repo) => {
    const currentRepoId = repo.id;
    const isExpanded = repo.splityMethodSelectKey?.length > 0;
    try {
        if (repo.splitType === 1) {
            if (repo.paragraphMark.trim().length < 1) {
                message.warn("当选择按段落切分时，段落标志为必填");
                return;
            }
        }
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "repoId": repo.id,
            "splitType": repo.splitType,
            "paragraphMark": repo.paragraphMark,
            "slideNums": repo.slideNums
        }
        const response = await KnowledgeMgtApi.setSplitType(payload);
        message.success("设定成功")
    } catch (err) {
        console.error(">>>>>>set split type error", err);
    } finally {
        await fetchKnowledgeRepos(currentPage.value);
        // 恢复折叠状态
        if (isExpanded) {
            const updatedRepo = knowledgeRepos.value.find(r => r.id === currentRepoId);
            if (updatedRepo) {
                updatedRepo.splityMethodSelectKey = ['1'];
            }
        }
    }
}

const formatDate = (date) => {
    return new Date(date).toLocaleDateString();
};

// 创建一个 Map 来存储每个 repo 的 labelAction
const repoLabelActions = reactive(new Map())

// 获取特定 repo 的 labelAction，如果不存在则返回默认值 1
const getLabelAction = (repoId) => {
    return repoLabelActions.get(repoId) ?? 1
}

// 设置特定 repo 的 labelAction
const setLabelAction = (repoId, value) => {
    repoLabelActions.set(repoId, value)
}

// 定义 emits
const emits = defineEmits(['switchComponent']);

/*点击了小眼晴-查看知识库详细 */
const handleViewRepo = (repo) => {
    try {
        emits('switchComponent', {
            component: 'KnowledgeDetail',
            props: {
                repoData: repo,
                lastPage: currentPage.value  // 传递当前页码
            }
        });
    } catch (err) {
        console.err(">>>>>>进入知识库查看具体出错->" + err);
    }
}



/*点击了上传文件至知识库按钮*/
const currentRepo = ref(null);
const handleImport = (repo) => {
    try {
        fileInput.value.click();
        currentRepo.value = repo;
    } catch (err) {
        console.log("文件选择失败->" + err);
    }
}

/*文件上传被触发*/
const onUploadFileSelected = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    // 验证文件类型
    if (file.type.includes('image/')) {
        message.error('暂不支持图片解读')
        return
    }
    //processing.value = true;

    // 确保当前选中的知识库存在
    if (!currentRepo.value) return;
    // 初始化上传进度为0
    const repoIndex = knowledgeRepos.value.findIndex(repo => repo.id === currentRepo.value.id);
    if (repoIndex !== -1) {
        knowledgeRepos.value[repoIndex].importStatus = 0;
    }

    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    // 创建 FormData
    const formData = new FormData()

    formData.append('file', file)
    formData.append('repoId', currentRepo.value.id)
    formData.append('needSplit', currentRepo.value.needSplit)
    formData.append('readImg', currentRepo.value.readImg)
    const payload = {
        "token": encryptToken,
        "userName": userName,
    };

    try {
        console.log(">>>>>>进入上传")
        const response = await KnowledgeVectorApi.uploadDocIntoKnowledge(formData, payload);
        //console.log('上传成功:', response);
        // console.log(">>>>>>上传自己的头像后得到图片的地址为->" + response.data);
        // 获取原始URL
        const res = response.data;
        previewFileId.value = res.fileId;
        previewRepoId.value = currentRepo.value.id;
        previewNeedSplit.value = currentRepo.value.needSplit;
        previewFileType.value = res.type;
        if (previewFileType.value === 2) {
            previewContentJson.value = res.fileContent
        } else {
            let contentArray = [];
            const fileContent = res.fileContent;

            // 根据splitType进行不同的分割处理
            if (currentRepo.value.splitType === 0) {
                // 每400字一split
                const chunkSize = 400;
                for (let i = 0; i < fileContent.length; i += chunkSize) {
                    const chunk = fileContent.substring(i, i + chunkSize);
                    contentArray.push({
                        "img": "",
                        "content": chunk
                    });
                }
            } else if (currentRepo.value.splitType === 1) {
                // 连续currentRepo.value.slideNums个\n一split
                const lines = fileContent.split('\n');
                const slideNums = currentRepo.value.slideNums || 1;

                for (let i = 0; i < lines.length; i += slideNums) {
                    const chunk = lines.slice(i, i + slideNums).join('\n');
                    contentArray.push({
                        "img": "",
                        "content": chunk
                    });
                }
            } else if (currentRepo.value.splitType === 2) {
                // 遇到\r一split
                const chunks = fileContent.split('\f');
                contentArray = chunks.map(chunk => ({
                    "img": "",
                    "content": chunk
                }));
            } else {
                // 默认按照换行符分割
                const contentLines = fileContent.split('\n');
                contentArray = contentLines.map(line => ({
                    "img": "",
                    "content": line
                }));
            }

            previewContentJson.value = JSON.stringify(contentArray);
        }
        popKnowledgeContentPreviewShow.value = true;

        console.log("上传后的结果->" + JSON.stringify(res));
        console.log("previewRepoId->" + previewRepoId.value);
        console.log("previewFileId->" + previewFileId.value);
        console.log("previewNeedSplit->" + previewNeedSplit.value);
        console.log("previewFileType->" + previewFileType.value);
        console.log("previewContentJson->" + previewContentJson.value);
        getImportStatus(currentRepo.value.id);
        /*
        if (res !== 1) {
            message.error("上传出错");
            processing.value = false;
            getImportStatus(currentRepo.value.id);
            return;
        } else {
            
            // 开始检查进度

            //getImportStatus(currentRepo.value.id);
        }
        */
    } catch (error) {
        console.error('上传失败:' + error)
        message.error(error.message || '上传失败，请重试')
        processing.value = false;
        getImportStatus(currentRepo.value.id);
    } finally {
        processing.value = false;
    }
}

/*文件上传被触发*/
const executeEmbedding = async (vlEmbedding) => {
    processing.value = true
    // 确保当前选中的知识库存在
    if (!currentRepo.value) return;
    // 初始化上传进度为0
    const repoIndex = knowledgeRepos.value.findIndex(repo => repo.id === currentRepo.value.id);
    if (repoIndex !== -1) {
        knowledgeRepos.value[repoIndex].importStatus = 0;
    }

    let token = authorization.getToken();
    let encryptToken = encrypt_url(token);
    let userName = authorization.getUserName();
    
    const payload = {
        "token": encryptToken,
        "userName": userName,
        "repoId": previewRepoId.value,
        "fileId": previewFileId.value,
        "needSplit": previewNeedSplit.value,
        "readImg": currentRepo.value.readImg,    
        "vlEmbedding": vlEmbedding,    
    };

    try {
        console.log(">>>>>>进入上传")
        const response = await KnowledgeVectorApi.executeEmbedding(payload);
        //console.log('上传成功:', response);
        // console.log(">>>>>>上传自己的头像后得到图片的地址为->" + response.data);
        // 获取原始URL
        
        getImportStatus(currentRepo.value.id);
        /*
        if (res !== 1) {
            message.error("上传出错");
            processing.value = false;
            getImportStatus(currentRepo.value.id);
            return;
        } else {
            
            // 开始检查进度

            //getImportStatus(currentRepo.value.id);
        }
        */
    } catch (error) {
        console.error('入库失败:' + error)
        message.error(error.message || '入库失败，请重试')
        processing.value = false;
        getImportStatus(currentRepo.value.id);
    } finally {
        processing.value = false;
    }
}

// 添加轮询控制变量
const pollTimer = ref(null);

// 添加检查正在进行的导入任务的方法
const checkOngoingImports = async () => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
        }
        const response = await KnowledgeVectorApi.getImportStatus(payload);
        console.log(">>>>>>页面刚进入就检查一下后台是否有任务在运行，得到结果->" + JSON.stringify(response));
        if (response.running) {
            // 如果有正在进行的任务，开始轮询
            console.log("正在运行的任务 repoId:", response.repoId);
            console.log("当前知识库列表:", knowledgeRepos.value.map(repo => repo.id));
            const repoIndex = knowledgeRepos.value.findIndex(repo => repo.id === response.repoId);
            if (repoIndex !== -1) {
                knowledgeRepos.value[repoIndex].importStatus = response.processPercentage;
                knowledgeRepos.value[repoIndex].currentStep = response.currentStep;
                startPolling(response.repoId);
            }
        }
    } catch (error) {
        console.error("检查进行中的导入任务失败:", error);
    }
}

// 修改轮询方法
const startPolling = (repoId) => {
    // 清除可能存在的旧定时器
    if (pollTimer.value) {
        clearTimeout(pollTimer.value);
        pollTimer.value = null;
    }

    getImportStatus(repoId);
}
// 添加一个标记来追踪组件是否已卸载
const isComponentMounted = ref(true);
/*查询上传的进度*/
const getImportStatus = async (repoId) => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
        }
        const response = await KnowledgeVectorApi.getImportStatus(payload);

        // 找到对应的知识库并更新进度
        const repoIndex = knowledgeRepos.value.findIndex(repo => repo.id === repoId);
        if (repoIndex !== -1) {
            if (response.running) {
                // 更新进度
                knowledgeRepos.value[repoIndex].importStatus = response.processPercentage;
                knowledgeRepos.value[repoIndex].currentStep = response.currentStep;
                // 如果还在运行，继续轮询
                // 使用 ref 存储定时器 ID
                if (response.running && isComponentMounted.value) {
                    pollTimer.value = setTimeout(() => getImportStatus(repoId), 2000);
                }
                console.log("importStatus->" + knowledgeRepos.value[repoIndex].importStatus);
            } else {
                // 上传完成，重置进度
                knowledgeRepos.value[repoIndex].importStatus = undefined;
                knowledgeRepos.value[repoIndex].currentStep = '';
                processing.value = false;
                fetchKnowledgeRepos(); //刷新页面
                // 清除定时器
                if (pollTimer.value) {
                    clearTimeout(pollTimer.value);
                    pollTimer.value = null;
                }
            }
        }
    } catch (err) {
        console.error(">>>>>>查询上传的进度出错->" + err);
        processing.value = false;
        // 发生错误时重置对应知识库的进度
        const repoIndex = knowledgeRepos.value.findIndex(repo => repo.id === repoId);
        if (repoIndex !== -1) {
            knowledgeRepos.value[repoIndex].importStatus = undefined;
        }
        // 发生错误时也要清除定时器
        if (pollTimer.value) {
            clearTimeout(pollTimer.value);
            pollTimer.value = null;
        }
    } finally {

    }
}

/*删除知识库中的某一个标签 */
const removeLabel = async (repo, index) => {
    try {
        const result = await Modal.confirm({
            title: '确认删除',
            content: '您确认删除当前标签？此操作不可撤销!',
            okText: '确定',
            cancelText: '取消',
        });

        if (result) {
            // 这里添加删除标签的具体实现逻辑
            // 比如调用API等
        }
    } catch (err) {
        console.log(">>>>>>删除知识库中某一个标签动作出错->" + err);
    }
};
const syncToEs = async (repo) => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "knowledgeRepoId": repo.id
        }
        loadingMask.value = true;
        await KnowledgeMgtApi.syncToES(payload).then(res => {
            console.log(JSON.stringify(res));
        }).catch(err => {
            message.error("同步es错误");
            console.error(">>>>>>同步es后台api出错", err);
        }).finally(() => {
            loadingMask.value = false;
        });
    } catch (err) {
        console.log(">>>>>>syncToEs执行错误", err);
    }
}

/* 用知识库的提示语生成新的标签 */
const handleLabelRepo = async (repo) => {
    try {
        loadingMask.value = true;
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        const action = getLabelAction(repo.id)
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "repoId": repo.id,
            "repoPrompt": repo.majorPrompt,
            "labelAction": action,
        }
        await KnowledgeMgtApi.labelRepo(payload).then(res => {
            console.log(JSON.stringify(res));
        }).catch(err => {
            message.error("为知识库生成标签错误->" + err);
        });
    } catch (err) {
        console.log(">>>>>>为知识库生成标签错误->" + err);
    } finally {
        await fetchKnowledgeRepos();
        loadingMask.value = false;  // 隐藏遮罩
    }
}

/*用标签全部重新清洗一遍 */
const handleFullIndex = async (repo) => {
    try {
        try {
            let token = authorization.getToken();
            let encryptToken = encrypt_url(token);
            let userName = authorization.getUserName();
            let payload = {
                "token": encryptToken,
                "userName": userName,
                "knowledgeRepoId": repo.id,
                "fieldNameList": ["content", "keywords"],
                "min_token_len": 1,
                "max_token_len": 20,
            }
            await KnowledgeMgtApi.makeIndex(payload).then(res => {
                message.success("建立索引成功");
            }).catch(err => {
                message.error("建立索引失败");
            });
        } catch (err) {
            console.log(">>>>>>make index error", err);
        } finally {
            fetchKnowledgeRepos();
        }
    } catch (err) {
        console.error(">>>>>>重新索引错误->", err);
    }
}

/*添加一个知识库*/
const handleAddRepo = async () => {
    try {
        addKnowledgeShow.value = true;
    } catch (err) {
        console.log(">>>>>>添加一个知识库错误->" + err);
    }
}

/*删除一个知识库 */
const handleRemove = async (item) => {
    Modal.confirm({
        title: '确认删除',
        content: '您是否要删除该知识库？此动作为不可撤消。',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
            try {
                let token = authorization.getToken();
                let encryptToken = encrypt_url(token);
                let userName = authorization.getUserName();
                let payload = {
                    "token": encryptToken,
                    "userName": userName,
                    "knowledgeRepoId": item.id,
                }
                await KnowledgeMgtApi.removeRepoById(payload).then(res => {
                    message.success("删除成功");
                }).catch(err => {
                    message.error("删除失败");
                });
            } catch (err) {
                console.log(">>>>>>remove a repo by id error->{}" + err);
            } finally {
                fetchKnowledgeRepos();
            }
        }
    });
}
/*保存知识库猫娘*/
const handleSaveMajorPrompt = async (repo) => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
            "repoId": repo.id,
            "majorPrompt": repo.majorPrompt
        }
        await KnowledgeMgtApi.saveMajorPrompt(payload).then(res => {
            message.success("保存提示语成功");
        }).catch(err => {
            console.log("保存提示语失败" + JSON.stringify(err));
        });
    } catch (err) {
        console.log(">>>>>>保存提示语失败->" + err);
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
            "pageNumber": currentPage.value,
            "pageSize": pageSize.value,
        }
        const res = await KnowledgeMgtApi.listUserRepo(payload);
        // 从返回的Map中解构获取对应的值
        knowledgeRepos.value = res.content.map(repo => ({
            ...repo,
            isHovered: false, // 添加悬停状态
            importStatus: undefined, // 初始化导入状态为undefined
            currentStep: '',
            needSplit: false,
            readImg: false,
            splityMethodSelectKey: [] // 添加折叠面板的独立状态控制
        }));
        total.value = res.totalElements;
        console.log("获取知识库列表成功:", knowledgeRepos.value.map(repo => repo.id));
    } catch (error) {
        console.log('获取知识库列表失败:' + error);
    }
};
const esAvailable = ref(false);

const queryElasticStatus = async () => {
    try {
        let token = authorization.getToken();
        let encryptToken = encrypt_url(token);
        let userName = authorization.getUserName();
        let payload = {
            "token": encryptToken,
            "userName": userName,
        }
        const res = await KnowledgeMgtApi.queryElasticStatus(payload);
        esAvailable.value = res;
        console.log("查询es状态:", res);
    } catch (error) {
        console.log('查询es状态错误:', error);
    }
};

const handleRefreshKnowledge = async (page) => {
    try {
        let myPage = 1;
        if (currentPage) {
            myPage = currentPage.value;
        }
        fetchKnowledgeRepos(myPage);
    } catch (err) {
        console.err(">>>>>>刷新当前知识库页面失败->" + err);
    }
}

const handlePageChange = (page) => {
    currentPage.value = page;
    fetchKnowledgeRepos(page);
};

onMounted(async () => {
    // 先等待获取知识库列表
    await fetchKnowledgeRepos(currentPage.value);
    // 然后再检查进行中的导入任务
    await checkOngoingImports();
    // 然后再检查es状态
    await queryElasticStatus();
});
// 在组件卸载时清理定时器
onUnmounted(() => {
    isComponentMounted.value = false;
    if (pollTimer.value) {
        clearTimeout(pollTimer.value);
        pollTimer.value = null;
    }
});

// 监听每个文档库的切分方式
watch(knowledgeRepos.value, (repos) => {
    repos.forEach(repo => {
        if (typeof repo.splitType !== 'number') {
            repo.splitType = 0  // 默认为0
        }
    })
}, { deep: true })

// 监听返回时传入的页码
watch(
    () => props.returnedPage,
    (newPage) => {
        console.log(">>>>>>Main页面watch触发，newPage值为：", newPage);
        if (newPage !== currentPage.value) {
            currentPage.value = newPage
            fetchKnowledgeRepos(newPage)
        }
    },
    { immediate: true }  // 添加immediate选项
)
</script>

<style scoped>
:deep(:where(.css-dev-only-do-not-override-qaei2i).ant-collapse > .ant-collapse-item > .ant-collapse-header) {
    padding: 0 0;
}

:deep(.ant-radio) {
    transform: scale(0.75);
    /* 缩小radio圆圈的大小 */
}

:deep(.ant-radio-wrapper) {
    margin-right: 0;
    /* 覆盖默认的右边距 */
}

.custom-label-text-box :deep(.ant-input) {
    height: 24px;
    line-height: 24px;
    padding: 0 8px;
    font-size: 11px;
}
</style>