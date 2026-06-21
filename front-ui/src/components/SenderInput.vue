<template>
  <Sender
    class="sender-input-comp"
    :loading="senderLoading"
    :value="question"
    placeholder="Hello~ this is DataCopilotX!"
    @change="changeValue"
    @submit="sendMessage"
    @cancel="cancelSend"
  >
    <template #prefix>
      <a-space>
        <!-- 添加数据集选择下拉菜单，添加警告样式 -->
        <a-select 
          v-model:value="selectedDatasetId" 
          placeholder="请选择数据集!" 
          style="width: 150px;"
          allowClear
          :class="{ 'transparent-border-select': !showDatasetWarning, 'dataset-warning': showDatasetWarning }"
        >
          <a-select-option v-for="dataset in datasets" :key="dataset.id" :value="dataset.id">
            {{ dataset.name }}
          </a-select-option>
        </a-select>
        
        <a-button type="text" :class="{ 'model-button-warning': showModelWarning }" @click="handleOpenHeader">
          <template #icon>
            <SettingOutlined />
          </template>
        </a-button>
        <a-tag
          v-if="selectedModel"
          class="flex items-center gap-1"
          color="processing"
          :bordered="false"
          closable
          @close="handleRemoveModel"
        >
          <template #closeIcon>
            <a-tooltip>
              <template #title>取消在对话中选择此模型</template>
              <DeleteOutlined class="text-red-600" />
            </a-tooltip>
          </template>
          <span class="truncate max-w-[150px]" :title="selectedModel.model">
             {{ selectedModel.model }}
          </span>
          <EyeOutlined
            class="cursor-pointer text-blue-600"
            @click="handlePreviewModel" 
          />
        </a-tag>
      </a-space>
    </template>
    <template #header>
      <Sender.Header title="请选择模型配置" :open="openHeader" class="bg-white">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-select
              v-model:value="selectedModelId"
              placeholder="请选择模型"
              style="width: 100%;"
              allowClear
              :loading="modelsLoading"
              @change="handleModelSelect"
            >
              <a-select-option v-for="model in models" :key="model.id" :value="model.id">
                {{ model.platform }} ({{ model.model }})
              </a-select-option>
            </a-select>
          </a-col>
        </a-row>
      </Sender.Header>
    </template>
  </Sender>
</template>

<script setup lang="ts">
// 导入路由和图标
import { Sender, useXAgent, useXChat } from 'ant-design-x-vue';
import { message as Msg } from 'ant-design-vue';
import { mockChatStreamApi } from '@/api/chat.ts';
import { useDialogueStore } from '@/stores/modules/dialogues';
import { ref, watch, onMounted } from 'vue';

// 导入模型相关API
import { getModelList } from '@/api/model.ts';
import type { Model } from '@/api/model.ts';

// 导入数据集相关API
import { getDatasetList } from '@/api/dataset';

// 定义数据集类型
interface Dataset {
  id: string;
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  table: string;
  username: string;
  createTime: string;
}

// 添加props接收外部传入的sessionId
const props = defineProps({
  externalSessionId: {
    type: String,
    default: undefined
  }
});

const emit = defineEmits(['messagesChange', 'loadingChange']);

const dialogueStores = useDialogueStore();

/** 重置所有数据，开启了新对话 */
// 修改newChat函数，确保仅在明确需要新建对话时才清空历史记录
const newChat = () => {
  selectedModel.value = null;
  selectedModelId.value = undefined;
  conversation_uuid.value = undefined;
  messages.value = [];
  // 只有在明确新建对话时才重置历史，从历史对话进入时不应调用此函数
  dialogueStores.resetHistory();
  dialogueStores.setCurrentConversitionUuid(undefined);
  emit('messagesChange', []);
  showDatasetWarning.value = false;
  showModelWarning.value = false;
  // 新建对话时重新生成 questionId 和 sessionId
  generateNewIds();
};

// 添加watch监听externalSessionId变化
watch(
  () => props.externalSessionId,
  (newSessionId) => {
    if (newSessionId) {
      // 当外部传入sessionId时，使用传入的sessionId
      sessionId.value = newSessionId;
      // 确保不会清空历史记录
      // 不需要调用dialogueStores.resetHistory()
    }
  },
  { immediate: true }
);
defineExpose({
  newChat
});

/** 发送消息 */
const question = ref('');
const senderLoading = ref<boolean>(false);
const waitResponse = ref<boolean>(false);
const conversation_uuid = ref<string | undefined>(undefined);
const isCode = ref<boolean>(false); // 本次对话内容是否是代码
let controller: AbortController;

// 添加警告状态变量
const showDatasetWarning = ref<boolean>(false);
const showModelWarning = ref<boolean>(false);

// 添加 questionId 和 sessionId 状态变量
const questionId = ref<string>('');
const sessionId = ref<string>('');

// 生成带前缀的UUID函数
const generateUuidWithPrefix = (prefix: string): string => {
  // 生成简单的UUID，实际项目中可以使用更标准的UUID生成方法
  const uuid = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
  return `${prefix}_${uuid}`;
};

// 生成新的questionId和sessionId
const generateNewIds = () => {
  questionId.value = generateUuidWithPrefix('ques');
  sessionId.value = generateUuidWithPrefix('sess');
};

// 仅生成新的questionId
const generateNewQuestionId = () => {
  questionId.value = generateUuidWithPrefix('ques');
};

const [agent] = useXAgent({
  request: async ({ message }, { onSuccess, onUpdate }) => {
    controller = new AbortController();

    senderLoading.value = true;
    waitResponse.value = true;
    console.log('message', message);    // 读取流数据
    
    // 每次请求重新生成questionId
    generateNewQuestionId();
    
    // 添加fullContent变量用于累积所有接收到的数据，实现打字机效果
    let fullContent = '';
    let sqlResultData = null;
    // 删除未使用的hasSqlResult变量
    // let hasSqlResult = false;
    try {
      // 模拟对话接口，添加数据集ID和模型ID参数
      await mockChatStreamApi(
        (chunk: string) => {
          // 移除这行代码，避免loading状态过早结束
          // waitResponse.value = false;
          try {
            // 解析JSON数据
            // 移除所有可能的'data:'前缀
            chunk = chunk.replace(/^data:\s*/, '');
            if (!chunk || chunk.trim() === '') {
              console.log('接收到空的chunk数据');
              return;
            }

            let jsonData = {};
            jsonData = JSON.parse(chunk);
            
            // 检查是否是sql_result_node类型的数据
            if (jsonData.id === 'sql_result_node' && jsonData.data) {
              // 保存SQL结果数据
              sqlResultData = jsonData.data;
              // 使用ECharts渲染数据
              fullContent += sqlResultData;
              onUpdate(fullContent);
            } else if (jsonData.code === 200 && jsonData.data) {
              // code为200时，累积data数据，实现打字机效果
              // 检查是否是SQL结果数据（包含columns和data字段）
              try {
                const parsedData = typeof jsonData.data === 'string' ? JSON.parse(jsonData.data) : jsonData.data;
                if (parsedData.columns && parsedData.data) {
                  // 这是SQL结果数据，添加问数结果标记
                  if (!fullContent.includes('问数结果:')) {
                    fullContent += '#### 问数结果: ';
                  }
                  // 将JSON数据附加到消息中
                  fullContent += JSON.stringify(parsedData);
                  onUpdate(fullContent);
                } else {
                  // 普通文本数据
                  fullContent += jsonData.data;
                  onUpdate(fullContent);
                }
              } catch {
                // 解析失败，当作普通文本处理
                fullContent += jsonData.data;
                onUpdate(fullContent);
              }
            } else if (jsonData.code === 500 && jsonData.message) {
              // code为500时，显示错误消息并实现打字机效果
              fullContent += jsonData.message;
              onUpdate(fullContent); // 使用fullContent来更新，实现打字机效果
              onSuccess(fullContent);
            } else {
              // 处理其他情况
              onUpdate('未知响应格式');
              console.error('未知响应格式:', jsonData);
            }
          } catch (error) {
            console.error('解析响应失败:', error);
          }
        },
        {
          signal: controller.signal,
          message: question.value,
          datasetId: selectedDatasetId.value,
          modelId: selectedModelId.value,
          questionId: questionId.value,
          sessionId: sessionId.value
        }
      );
    } catch (streamError: any) {
      Msg.error(streamError.message);
    } finally {
      senderLoading.value = false;
      waitResponse.value = false;
      isCode.value = false;
    }
  }
});

const { onRequest, messages } = useXChat({
  agent: agent.value
});

watch(messages, (newMessages) => {
  emit('messagesChange', newMessages);
});

watch(
  () => waitResponse.value,
  (newVal) => {
    if (typeof newVal === 'boolean') {
      emit('loadingChange', newVal);
    }
  },
  {
    flush: 'post'
  }
);

watch(
  () => senderLoading.value,
  (newVal) => {
    dialogueStores.setIsChatting(newVal);
  }
);

const changeValue = (val: string) => {
  question.value = val;
};

// 数据集相关状态
const datasets = ref<Dataset[]>([]);
const selectedDatasetId = ref<string>(undefined);

/** 获取数据集列表 */
const fetchDatasets = async () => {
  try {
    const data = await getDatasetList();
    datasets.value = data;
  } catch (error) {
    console.error('获取数据集列表失败:', error);
    Msg.error('获取数据集列表失败');
  }
};

// 发送消息时带上选中的数据集信息
const sendMessage = (value: any) => {
  // 重置警告状态
  showDatasetWarning.value = false;
  showModelWarning.value = false;
  
  // 校验是否选择了数据集
  if (!selectedDatasetId.value) {
    Msg.warning('请选择数据集');
    showDatasetWarning.value = true;
    return;
  }
  
  // 校验是否选择了模型
  if (!selectedModel.value) {
    Msg.warning('请选择模型');
    showModelWarning.value = true;
    return;
  }
  
  onRequest(value);
  question.value = '';
  openHeader.value = false;
  // 在发送消息时可以使用 selectedDatasetId.value 来获取选中的数据集ID
  console.log('选中的数据集ID:', selectedDatasetId.value);
  // 在发送消息时可以使用 selectedModel 来获取选中的模型信息
  console.log('选中的模型:', selectedModel.value);
};

const cancelSend = () => {
  if (controller) {
    controller.abort();
  }
  waitResponse.value = false;
  question.value = '';
  senderLoading.value = false;
};

/** 模型选择相关 */
const openHeader = ref<boolean>(false);
const handleOpenHeader = () => {
  openHeader.value = !openHeader.value;
};

// 模型相关状态
const models = ref<Model[]>([]);
const modelsLoading = ref<boolean>(false);
const selectedModel = ref<Model | null>(null);
const selectedModelId = ref<string>(undefined);

// 模型选择处理
const handleModelSelect = (modelId: string) => {
  if (modelId) {
    selectedModel.value = models.value.find(model => model.id === modelId) || null;
    showModelWarning.value = false;
  } else {
    selectedModel.value = null;
  }
};

// 数据集选择监听
watch(selectedDatasetId, (newValue) => {
  if (newValue) {
    showDatasetWarning.value = false;
  }
});

// 移除选中的模型
const handleRemoveModel = () => {
  selectedModel.value = null;
  selectedModelId.value = undefined;
  showModelWarning.value = false;
};

// 预览模型信息
const handlePreviewModel = () => {
  if (selectedModel.value) {
    Msg.info(`模型详情: ${selectedModel.value.platform} (${selectedModel.value.model})`);
  }
};

/** 获取模型列表 */
const fetchModels = async () => {
  try {
    modelsLoading.value = true;
    const data = await getModelList({
      'type': 'chat'
    });
    models.value = data;
    // 如果有缓存的模型ID，尝试匹配
    if (selectedModelId.value) {
      selectedModel.value = models.value.find(model => model.id === selectedModelId.value) || null;
    }
  } catch (error) {
    console.error('获取模型列表失败:', error);
    Msg.error('获取模型列表失败');
  } finally {
    modelsLoading.value = false;
  }
};

// 组件挂载时获取模型列表和数据集列表，并生成初始的questionId和sessionId
onMounted(() => {
  fetchModels();
  fetchDatasets();
  generateNewIds();
});

// 监听selectedModel 和conversation_uuid，有变化就给dialogueStores 赋值
watch(
  [() => selectedModel.value, () => conversation_uuid.value],
  (newUuid) => {  
    // 这里可以根据实际需求调整，可能需要转换格式
    dialogueStores.setCurrentConversitionUuid(newUuid);
  }
);
// 监听外部传入的sessionId变化
watch(
  () => props.externalSessionId,
  (newSessionId) => {
    if (newSessionId) {
      sessionId.value = newSessionId;
      console.log('使用外部传入的sessionId:', newSessionId);
    }
  },
  { immediate: true }
);
</script>

<style scoped>
/* 设置选择框边框为透明 */
:deep(.transparent-border-select .ant-select-selector) {
  border-color: transparent !important;
  box-shadow: none !important;
}

/* 聚焦时也保持透明边框 */
:deep(.transparent-border-select .ant-select-selector:hover) {
  border-color: transparent !important;
}

:deep(.transparent-border-select .ant-select-selector:focus-within) {
  border-color: transparent !important;
  box-shadow: none !important;
}

/* 数据集警告样式 */
:deep(.dataset-warning .ant-select-selector) {
  border-color: #ff4d4f !important;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2) !important;
}

/* 模型按钮警告样式 */
.model-button-warning {
  color: #ff4d4f !important;
  border-color: #ff4d4f !important;
}

.model-button-warning:hover {
  color: #ff7875 !important;
  border-color: #ff7875 !important;
}

/* 调整用户发送消息框的样式，减少空白感 */
.sender-input-comp {
  min-height: 40px !important;
  max-height: 165px !important;
}

/* 穿透样式，调整输入区域的内边距和行高 */
:deep(.antd-x-sender) {
  min-height: 40px !important;
}

:deep(.antd-x-sender-content) {
  padding: 6px 12px !important; /* 减小内边距 */
  min-height: 40px !important;
}

:deep(.antd-x-sender-input) {
  line-height: 1.5 !important; /* 调整行高 */
  min-height: 40px !important;
  max-height: 120px !important;
  font-size: 14px !important;
}

/* 调整发送按钮区域样式 */
:deep(.antd-x-sender-actions) {
  padding: 6px 12px !important;
}
</style>
