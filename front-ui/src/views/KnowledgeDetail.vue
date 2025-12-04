<template>
  <div class="main-container">
    <!-- 左侧边栏 -->
    <LeftSidebar />
    
    <!-- 中间内容区域 -->
    <div class="knowledge-detail-container">
      <!-- 全局loading组件 -->
      <div v-if="loading" class="global-loading">
        <ASpin tip="加载中..." size="large" />
      </div>
      
      <div v-else class="detail-content">
        <div class="detail-header">
          <div class="header-actions">
            <a-button type="primary" ghost style="margin-right: 12px;" @click="$router.back()">
              <LeftOutlined /> 返回列表
            </a-button>
          </div>
          <h2>{{ knowledgeBaseName }}</h2>
          <div></div> <!-- 占位元素，保持标题居中 -->
        </div>

        <div class="items-header">
          <div class="items-count">共 {{ totalItems }} 条记录</div>
          <div class="items-filter-search">
            <ASelect
              v-model:value="filterStatus"
              style="width: 120px; margin-right: 12px;"
              @change="handleFilterChange"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="false">已禁用</a-select-option>
              <a-select-option value="true">已启用</a-select-option>
            </ASelect>
            <AInputSearch
              v-model:value="searchKeyword"
              placeholder="搜索问题或答案"
              allow-clear
              enter-button
              size="middle"
              style="width: 240px;"
              @search="handleSearch"
            />
        </div>
      </div>
        
        <!-- 问答列表 -->
        <div class="knowledge-items">
          <!-- 新建条目卡片 -->
          <div 
            class="knowledge-item-card create-card"
            @click="handleCreate"
          >
            <div class="card-header">
              <div class="card-title">
                <span>新建条目</span>
              </div>
            </div>
            
            <div class="card-content create-content">
              <div class="create-icon">
                <PlusOutlined />
              </div>
              <p class="create-text">点击创建新的知识库条目</p>
            </div>
          </div>
          
          <!-- 原有问答列表 -->
          <div 
            v-for="item in knowledgeItems" 
            :key="item.docId"
            class="knowledge-item-card"
          >
            <div class="card-header">
              <div class="card-title">
                <span>{{ item.createTime || `问答 ${item.docId}` }}</span>
              </div>
              <div class="card-actions-header">
                <ASwitch 
                  v-model:checked="item.enabled"
                  @change="handleToggleEnabled(item.docId, $event)"
                />
                <a-dropdown @click.stop>
                  <a-button type="text" size="small">
                    <EllipsisOutlined />
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleEdit(item)">编辑</a-menu-item>
                      <a-menu-item danger @click="handleDelete(item.docId)">删除</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
            </div>
            
            <div class="card-content">
              <div class="card-section">
                <h3>问题</h3>
                <p class="knowledge-question">{{ item.question }}</p>
              </div>
              
              <div class="card-section">
                <h3>回答</h3>
                <p class="knowledge-answer">{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 最右侧：命中测试面板 -->
    <div class="document-info-panel">
      <div class="info-card">
        <h3>命中测试</h3>
        
        <!-- 上半部分：输入框 -->
        <div class="test-section">
          <h4>测试查询</h4>
          <a-textarea
            v-model:value="testQuery"
            placeholder="请输入测试查询文本"
            rows={4}
            style="width: 100%"
          />
          <a-button
            type="primary"
            block
            style="margin-top: 12px;"
            :loading="testLoading"
            @click="performHitTest"
          >
            执行测试
          </a-button>
        </div>
        
        <!-- 中间部分：召回配置 -->
        <div class="test-section" style="margin-top: 20px;">
          <h4>召回配置</h4>
          <div class="config-item">
            <label>Score阈值</label>
            <a-slider
              v-model:value="scoreThreshold"
              :min="0"
              :max="1"
              :step="0.01"
              :tooltip="{ formatter: value => `${value.toFixed(2)}` }"
              style="width: 100%;"
            />
            <div style="text-align: center; margin-top: 8px; color: #666;">
              当前值: {{ scoreThreshold.toFixed(2) }}
            </div>
          </div>
          <div class="config-item" style="margin-top: 12px;">
            <label>Top K</label>
            <a-slider
              v-model:value="topK"
              :min="1"
              :max="20"
              :step="1"
              :tooltip="{ formatter: value => `Top ${value}` }"
              style="width: 100%;"
            />
            <div style="text-align: center; margin-top: 8px; color: #666;">
              当前值: Top {{ topK }}
            </div>
          </div>
        </div>
        
        <!-- 下半部分：输出框 -->
        <div class="test-section" style="margin-top: 20px;">
          <h4>测试结果</h4>
          <div 
            class="result-output"
          >
            <div v-if="testResults.length === 0" class="no-results">暂无结果</div>
            <div 
              v-for="(result, index) in testResults" 
              :key="result.docId"
              class="result-item"
              :style="{
                marginBottom: '16px',
                paddingBottom: '16px',
                borderBottom: index < testResults.length - 1 ? '1px solid #f0f0f0' : 'none'
              }"
            >
              <div class="result-rank">第{{ index + 1 }}名 (Score: {{ result.score }})</div>
              <div class="result-question"><strong>问题:</strong> {{ result.question }}</div>
              <div class="result-answer"><strong>回答:</strong> {{ result.answer }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 编辑弹窗 -->
    <a-modal
      v-model:visible="editModalVisible"
      title="编辑知识库条目"
      :confirm-loading="editModalLoading"
      okText="保存"
      cancelText="取消"
      @ok="handleEditSubmit"
      @cancel="handleEditCancel"
    >
      <a-form
        ref="editFormRef"
        :model="editFormData"
        layout="vertical"
      >
        <!-- 问题输入框 -->
        <a-form-item
          label="问题"
          name="question"
          :rules="[{ required: true, message: '请输入问题' }]"
        >
          <a-input
            v-model:value="editFormData.question"
            placeholder="请输入问题"
            style="width: 100%"
          />
        </a-form-item>
        
        <!-- 回答输入框 -->
        <a-form-item
          label="回答"
          name="answer"
          :rules="[{ required: true, message: '请输入回答' }]"
        >
          <a-input
            v-model:value="editFormData.answer"
            placeholder="请输入回答"
            style="width: 100%"
            type="textarea"
            rows={4}
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
// 导入必要的依赖
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { Spin as ASpin, FormInstance, Switch as ASwitch } from 'ant-design-vue';
import { LeftOutlined, EllipsisOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { useRoute } from 'vue-router';

// 导入侧边栏组件
import LeftSidebar from '../components/LeftSidebar.vue';

// 导入API
import { type KnowledgeItem, saveKnowledgeItem, deleteKnowledgeItem, modifyKnowledgeItem, getKnowledgeRetrieval, type KnowledgeRetrievalItem } from '../api/knowledge';
import { getKnowledgeList } from '../api/knowledge';

// 扩展KnowledgeItem接口，添加启用状态和标题
interface KnowledgeItemWithState extends KnowledgeItem {
  enabled: boolean;
  title?: string;
}

// 命中测试结果接口
interface HitTestResult {
  score: number;
  question: string;
  answer: string;
  docId: string;
}

// 获取路由信息
const route = useRoute();

// 获取文档ID
const docId = route.params.id as string;

// 定义知识库条目数据
const knowledgeItems = ref<KnowledgeItemWithState[]>([]);
const knowledgeBaseName = ref('');
const totalItems = ref(0); // 总记录数变量

// 添加loading状态
const loading = ref(false);

// 添加过滤和搜索相关变量
const filterStatus = ref(''); // 过滤状态：全部、已启用、已禁用
const searchKeyword = ref(''); // 搜索关键词

// 编辑弹窗相关状态
const editModalVisible = ref(false);
const editModalLoading = ref(false);
const editFormRef = ref<FormInstance>();
const currentEditItem = ref<KnowledgeItemWithState | null>(null);

// 编辑表单数据
const editFormData = ref({
  modelId: '',
  datasetId: '',
  question: '',
  answer: '',
  docId: ''
});

// 命中测试相关状态
const testQuery = ref('');
const scoreThreshold = ref(0.6);
const topK = ref(3);
const testLoading = ref(false);
const testResults = ref<HitTestResult[]>([]);

// 导入获取知识库条目列表的函数
import { getKnowledgeItems } from '../api/knowledge';

// 在script setup部分添加handleFilterChange方法的实现
// 处理过滤状态变化
const handleFilterChange = () => {
  loadKnowledgeDetail(); // 调用加载方法刷新数据
};

// 处理搜索
const handleSearch = () => {
  loadKnowledgeDetail(); // 调用加载方法刷新数据
};

// 加载知识库详情
const loadKnowledgeDetail = async () => {
  // 开始加载时设置loading为true
  loading.value = true;
  try {
    // 获取知识库的基本信息
    const knowledgeBaseInfo = await getKnowledgeList().then(list => 
      list.find(item => String(item.id) === docId)
    );
    
    if (knowledgeBaseInfo) {
      knowledgeBaseName.value = knowledgeBaseInfo.name;
    }
    
    const response = await getKnowledgeItems(0, 20, docId, searchKeyword.value, filterStatus.value);
    
    // 将获取的数据转换为KnowledgeItemWithState类型
    knowledgeItems.value = response.data.map((item: any) => ({
      docId: item.docId,
      question: item.question,
      answer: item.answer,
      createTime: item.createTime,
      enabled: item.enable !== undefined ? item.enable : true, // 使用接口返回的enable字段
      title: item.title || item.question.substring(0, 20) + '...'
    }));
    
    // 设置总记录数
    totalItems.value = knowledgeItems.value.length;
  } catch (error) {
    console.error('获取知识库详情失败:', error);
    message.error('获取知识库详情失败');
  } finally {
    // 无论成功失败，结束加载时设置loading为false
    loading.value = false;
  }
};

// 处理启用/禁用切换
const handleToggleEnabled = async (id: string, checked: boolean) => {
  const item2 = knowledgeItems.value.find(item => item.docId === id);
  if (item2) {
    try {
      // 调用API保存启用状态
      await modifyKnowledgeItem({
        knowledgeLibId: docId, // 使用docId作为knowledgeLibId的值
        docId: id,
        question: item2.question,
        answer: item2.answer,
        enable: checked
      });
      item2.enabled = checked;
      message.success(`${checked ? '启用' : '禁用'}成功`);
    } catch (error) {
      console.error('保存启用状态失败:', error);
      message.error(`${checked ? '启用' : '禁用'}失败`);
      // 恢复原来的状态
      item2.enabled = !checked;
    }
  }
};

// 处理编辑操作
const handleEdit = (item: KnowledgeItemWithState) => {
  currentEditItem.value = item;
  // 设置编辑表单数据
  editFormData.value = {
    modelId: '', // 实际项目中可能需要根据记录获取对应的modelId
    datasetId: '', // 实际项目中可能需要根据记录获取对应的datasetId
    question: item.question,
    answer: item.answer,
    docId: item.docId
  };
  
  // 显示编辑弹窗
  editModalVisible.value = true;
};

// 处理编辑提交
const handleEditSubmit = async () => {
  if (!editFormRef.value) {return;}
  
  try {
    await editFormRef.value.validateFields();
    editModalLoading.value = true;
    
    // 判断是创建还是编辑操作
    const { question, answer } = editFormData.value;
    if (currentEditItem.value) {
      // 编辑操作：调用修改接口
      await modifyKnowledgeItem({
        knowledgeLibId: docId, // 使用docId作为knowledgeLibId的值
        docId: currentEditItem.value.docId,
        question,
        answer,
        enable: currentEditItem.value.enabled
      });
      message.success('更新成功');
    } else {
      // 新建操作：调用保存接口
      await saveKnowledgeItem({
        knowledgeLibId: docId, // 使用docId作为knowledgeLibId的值
        question,
        answer
      });
      message.success('保存成功');
    }
    editModalVisible.value = false;
    // 刷新详情
    setTimeout(() => {
      loadKnowledgeDetail();
    }, 1000);
  } catch (error) {
    console.error('提交失败:', error);
    message.error('提交失败，请检查表单数据');
  } finally {
    editModalLoading.value = false;
  }
};

// 处理编辑取消
const handleEditCancel = () => {
  editModalVisible.value = false;
};

// 处理删除操作
const handleDelete = async (id: string) => {
  try {
    await deleteKnowledgeItem({ 
      knowledgeLibId: docId, 
      docId: id 
    });
    message.success(`删除知识库条目成功`);
    // 重新加载列表数据
    loadKnowledgeDetail();
  } catch (error) {
    message.error(`删除知识库条目失败: ${error}`);
  }
};

// 处理新建操作
const handleCreate = () => {
  currentEditItem.value = null;
  // 清空表单数据
  editFormData.value = {
    modelId: '',
    datasetId: '',
    question: '',
    answer: '',
    docId: ''
  };
  
  // 显示编辑弹窗
  editModalVisible.value = true;
};

// 执行命中测试
const performHitTest = async () => {
  if (!testQuery.value.trim()) {
    message.warning('请输入测试查询文本');
    return;
  }
  
  testLoading.value = true;
  try {
    console.log('开始调用知识库检索API...');
    // 调用实际的知识库检索API
    const results: KnowledgeRetrievalItem[] = await getKnowledgeRetrieval(
      docId, // 使用当前知识库的ID作为knowledgeLibId
      testQuery.value,
      scoreThreshold.value,
      parseInt(topK.value)
    );
    
    console.log('检索API返回结果:', results);
    
    // 将返回的结果转换为HitTestResult类型
    // 注意：这里可能需要根据API实际返回的字段进行调整
    testResults.value = results.map(result => ({
      score: result.score,
      question: result.question,
      answer: result.answer,
      docId: '' // 如果API不返回docId，可以留空或使用其他唯一标识
    }));
    
    if (results.length === 0) {
      message.info('未找到符合条件的匹配结果');
    } else {
      message.success(`找到${results.length}条匹配结果`);
    }
  } catch (error) {
    console.error('命中测试失败:', error);
    message.error('命中测试失败');
    
    // 在API调用失败时，可以提供模拟结果作为备选方案
    // 根据当前的查询和配置生成模拟结果
    // const enabledItems = knowledgeItems.value.filter(item => item.enabled);
    
    // // 简单的模拟匹配逻辑
    // const mockResults: HitTestResult[] = enabledItems.map(item => {
    //   // 生成随机但有一定相关性的分数
    //   const baseScore = Math.random() * 0.4 + 0.5; // 0.5-0.9之间
    //   // 如果查询文本与问题有共同词汇，分数会更高
    //   const hasCommonWords = testQuery.value.split(' ').some(word =>
    //     item.question.toLowerCase().includes(word.toLowerCase())
    //   );
    //   const score = hasCommonWords ? baseScore + 0.1 : baseScore;
    //
    //   return {
    //     score: Math.round(score * 100) / 100, // 保留两位小数
    //     question: item.question,
    //     answer: item.answer,
    //     docId: item.docId
    //   };
    // })
    // .filter(result => result.score >= scoreThreshold.value) // 应用分数阈值
    // .sort((a, b) => b.score - a.score) // 按分数降序排序
    // .slice(0, parseInt(topK.value)); // 取TopK
    //
    // testResults.value = mockResults;
    // message.info('已显示模拟结果');
  } finally {
    testLoading.value = false;
  }
};

// 在组件挂载时加载数据
onMounted(() => {
  loadKnowledgeDetail();
});
</script>

<style lang="scss" scoped>
/* 修改样式以适应新的布局 */
.main-container {

.items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 0;
}

.items-count {
  font-size: 14px;
  color: #666;
}

.items-filter-search {
  display: flex;
  align-items: center;
}
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.knowledge-detail-container {
  flex: 1;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  margin-left: 0; /* 移除之前可能存在的margin */
  position: relative;
  max-width: calc(100% - 450px); /* 限制最大宽度，为左侧边栏和右侧信息面板留出空间 */
}

/* 全局loading样式 */
.global-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.7);
  z-index: 1000;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.detail-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  flex: 1;
}

.detail-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.knowledge-items {
  /* 将网格布局改为弹性盒子流式布局 */
  display: flex;
  flex-wrap: wrap;
  gap: 16px; /* 调整间距以适应四个卡片 */
  /* 确保从左到右排列 */
  justify-content: flex-start;
  /* 顶部对齐 */
  align-content: flex-start;
}

.knowledge-item-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  /* 设置适合一行四个的宽度 */
  width: calc(25% - 12px); /* 减去间距的四分之一宽度 */
  /* 保留最小宽度确保内容显示正常 */
  min-width: 220px;
}

/* 新建卡片样式 */
.create-card {
  cursor: pointer;
  background-color: #f8f9fa;
  border: 2px dashed #d9d9d9;
  transition: all 0.3s;
}

.create-card:hover {
  border-color: #1890ff;
  background-color: #e6f7ff;
}

.create-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  padding: 24px 16px;
}

.create-icon {
  font-size: 32px;
  color: #999;
  margin-bottom: 12px;
}

.create-text {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.create-card:hover .create-icon,
.create-card:hover .create-text {
  color: #1890ff;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* 减小头部内边距 */
  padding: 12px 16px;
  background-color: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.card-title span {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.card-actions-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-content {
  /* 减小内容区域内边距 */
  padding: 16px;
}

.card-section {
  margin-bottom: 16px;
}

.card-section:last-child {
  margin-bottom: 0;
}

.card-section h3 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #666;
}

.knowledge-question {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  font-weight: 500;
  padding: 10px 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.knowledge-answer {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 10px 12px;
  background-color: #fafafa;
  border-radius: 4px;
}

.document-info-panel {
  width: 400px;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px;
  flex: 1;
}

.info-card h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 12px;
}

.info-card h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  margin-bottom: 12px;
}

.test-section {
  flex: 1;
  min-height: 0;
}

.config-item {
  margin-bottom: 8px;
}

.config-item label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.result-output {
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.5;
}

.no-results {
  color: #999;
  text-align: center;
  padding: 20px 0;
}

.result-item {
  word-break: break-word;
}

.result-rank {
  color: #1890ff;
  font-size: 12px;
}

.result-question,
.result-answer {
  font-size: 12px;
  line-height: 1.5;
}

.result-question strong,
.result-answer strong {
  color: #666;
  margin-right: 6px;
}
</style>