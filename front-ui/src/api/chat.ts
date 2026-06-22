// 添加到文件顶部的导入部分
import { get, del } from '@/utils/request';

/** AI生成，不保证可用性，请自行替换 */
interface ChatStreamOptions {
  /** 中止控制器信号 */
  signal?: AbortSignal;
  /** 发送的消息内容 */
  message?: string;
  /** 数据集ID */
  datasetId?: string;
  /** 模型ID */
  modelId?: string;
  /** 问题ID */
  questionId?: string;
  /** 会话ID */
  sessionId?: string;
}

// interface StreamResponse {
//   message: string;
//   conversation_uuid: string;
// }

/**
 * 对话流式 API
 * @param onChunk 块回调函数
 * @param options 配置选项
 */
export async function mockChatStreamApi(
  onChunk: (chunk: string) => void,
  options: ChatStreamOptions = {}
): Promise<void> {
  const { signal, message = '', datasetId, modelId, questionId: providedQuestionId, sessionId } = options;
  
  // 生成questionId，如果未提供则创建一个新的
  const questionId = providedQuestionId || `qu_${generateUUID()}`;

  // 移除重试机制，直接执行请求
  // 构建完整的请求URL
  const requestUrl = `/api/chat/completions`;
  
  // 发送请求到后端接口，添加credentials选项解决跨域
  const response = await fetch(requestUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // 包含cookies等凭证信息，解决跨域问题
    body: JSON.stringify({
      'question': message,
      'datasetId': datasetId,
      'modelId': modelId,
      'questionId': questionId,
      'sessionId': sessionId
    }),
    signal,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('No response body');
  }

  const decoder = new TextDecoder();
  let done = false;
  // 存储上次未处理完的数据
  let remainingData = '';
  let lastId = '';

  while (!done) {
    const { value, done: readerDone } = await reader.read();
    done = readerDone;
    
    if (value) {
      // 解码新数据
      const chunk = decoder.decode(value, { stream: readerDone ? false : true });
      // 合并剩余数据和新数据
      const fullData = remainingData + chunk;
      // 按行拆分数据
      const lines = fullData.split('\n');
      
      // 处理每行数据，提取SSE格式中的有效信息
      for (let i = 0; i < lines.length - 1; i++) {
        const line = lines[i].trim();
        if (!line) {continue;} // 忽略空行
        
        // 解析SSE格式，提取data字段
        if (line.startsWith('id:')) {
          // 提取id
          lastId = line.substring(3).trim();
        } else if (line.startsWith('data:')) {
          // 移除data:前缀并再次trim，获取实际数据内容
          const dataContent = line.substring(5).trim();
          if (dataContent) {
            const dataContentObj = JSON.parse(dataContent)
            dataContentObj.id = lastId
            onChunk(JSON.stringify(dataContentObj));
          }
        }
      }
      
      // 保存最后一行作为未处理完的数据
      remainingData = lines[lines.length - 1];
    }
  }
  
  // 处理最后剩余的数据（如果有）
  if (remainingData.trim()) {
    if (remainingData.startsWith('data:')) {
      const dataContent = remainingData.substring(5).trim();
      if (dataContent) {
        onChunk(dataContent);
      }
    }
  }
}

/**
 * 生成简单的UUID
 */
function generateUUID(): string {
  // 简单的UUID生成实现
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// 定义历史记录项的接口
export interface ChatHistoryItem {
  id: string;
  question: string;
  ctime: string;
}

// 定义分页响应接口，与后端返回结构保持一致
export interface PagedResponse<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  totalPage: number;
  data: T[];
}

/**
 * 获取聊天历史记录
 * @param pageNo 当前页码（从1开始）
 * @param pageSize 每页数量
 * @returns Promise<PagedResponse<ChatHistoryItem>>
 */
export async function getChatHistory(
  pageNo: number = 1, // 确保默认值为1
  pageSize: number = 10,
  searchKey: string
): Promise<PagedResponse<ChatHistoryItem>> {
  try {
    // 确保pageNo至少为1
    const validPageNo = Math.max(1, pageNo);
    const response = await get<PagedResponse<ChatHistoryItem>>('/chat/history', {
      searchkey: searchKey,
      pageNo: validPageNo,
      pageSize
    });
    return response;
  } catch (error) {
    console.error('获取聊天历史记录失败:', error);
    // 返回空数据作为默认值
    return {
      pageNo: 1,
      pageSize,
      total: 0,
      totalPage: 0,
      data: []
    };
  }
}

// 定义聊天历史详情项接口（根据用户提供的后端返回格式）
export interface ChatHistoryDetailItem {
  id: number;
  questionId: string;
  sessionId: string;
  question: string;
  answer: string;
  result: string;
  ctime: string;
  datasetId: number;
  dsName: string;
  modelId: number;
  modelName: string;
}

// 定义获取聊天历史详情的函数
export async function getChatHistoryDetail(
  sessionId: string
): Promise<ChatHistoryDetailItem[]> {
  try {
    const response = await get<ChatHistoryDetailItem[]>('/chat/history/detail', {
      sessionId
    });
    return response;
  } catch (error) {
    console.error('获取聊天历史详情失败:', error);
    // 返回空数组作为默认值
    return [];
  }
}

/**
 * 删除数据集
 * @param id 数据集ID
 * @returns Promise<void>
 */
export const deleteChatHistory = (id: string): Promise<void> => {
  return del(`/chat/delete/${id}`);
};