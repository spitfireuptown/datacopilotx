import { get, post, del, postVoid } from '../utils/request';

// 定义知识库条目类型接口
export interface KnowledgeItem {
  docId: string;
  question: string;
  answer: string;
  createTime?: string; // 添加创建时间字段，问号表示可选
}

// 定义知识库检索结果项类型接口
export interface KnowledgeRetrievalItem {
  score: number;
  question: string;
  answer: string;
}

// 定义分页列表响应类型接口
export interface KnowledgePageResponse {
  data: KnowledgeItem[];
  pageNo: number;
  pageSize: number;
  total: number;
  totalPage: number;
}

// 定义知识库保存请求参数接口
export interface KnowledgeSaveRequest {
  knowledgeLibId: string;
  question: string;
  answer: string;
}

// 定义知识库保存请求参数接口
export interface KnowledgeCreateRequest {
  modelId: string;
  datasetId: string;
  name: string;
  description: string;
  score?: number;
}

// 定义知识库更新请求参数接口
export interface KnowledgeUpdateRequest {
  id: number;
  modelId: string;
  datasetId: string;
  name: string;
  description: string;
  score?: number;
}

// 定义知识库列表项类型接口（对应新的接口返回格式）
export interface KnowledgeListItem {
  id: number;
  name: string;
  datasetId: number;
  modelId: number;
  description: string;
  score?: number;
}

// 定义知识库列表响应类型接口
export interface KnowledgeListResponse {
  code: number;
  message: string | null;
  traceId: string | null;
  data: KnowledgeListItem[];
}

/**
 * 获取知识库分页列表
 * @param pageNo 页码
 * @param pageSize 每页条数
 * @returns Promise<KnowledgeListItem[]>
 */
export const getKnowledgeList = (pageNo: number = 0, pageSize: number = 10): Promise<KnowledgeListItem[]> => {
  // 直接返回get请求的结果，request函数已经处理了response.data
  return get('/knowledgelib/list', { pageNo, pageSize });
};

/**
 * 获取知识库条目详情
 * @param docId 文档ID
 * @returns Promise<KnowledgeItem>
 */
export const getKnowledgeDetail = (docId: string): Promise<KnowledgeItem> => {
  return get(`/knowledgelib/detail/${docId}`);
};

/**
 * 保存知识库条目
 * @param data 知识库条目数据
 * @returns Promise<KnowledgeItem>
 */
export const saveKnowledgeItem = (data: KnowledgeSaveRequest): Promise<KnowledgeItem> => {
  return postVoid('/knowledgelib/data/store', data);
};

/**s
 * 保存知识库
 * @param data 知识库条目数据
 * @returns Promise<KnowledgeItem>
 */
export const createKnowledge = (data: KnowledgeCreateRequest): Promise<KnowledgeItem> => {
  return post('/knowledgelib/create', data);
};

/**
 * 保存知识库
 * @param data 知识库条目数据
 * @returns Promise<KnowledgeItem>
 */
export const updateKnowledge = (data: KnowledgeUpdateRequest): Promise<KnowledgeListItem> => {
  return post('/knowledgelib/update', data);
};

/**
 * 删除知识库
 * @param id 知识库ID
 * @returns Promise<void>
 */
export const deleteKnowledge = (id: string): Promise<void> => {
  return del<void>(`/knowledgelib/delete/${id}`);
};

/**
 * 获取知识库条目列表
 * @param pageNo 页码
 * @param pageSize 每页条数
 * @param knowledgeLibId 知识库ID
 * @param name 名称搜索
 * @param enable 启用状态过滤（可选，boolean类型）
 * @returns Promise<KnowledgePageResponse>
 */
export const getKnowledgeItems = (
  pageNo: number = 1,
  pageSize: number = 20,
  knowledgeLibId: string = '',
  name: string = '',
  enable?: boolean // 修改为boolean类型，可选参数
): Promise<KnowledgePageResponse> => {
  // 构建查询参数
  const params: any = {
    pageNo,
    pageSize,
    knowledgeLibId,
    name,
    enable
  };
  
  return get('/knowledgelib/data/list', params);
};

/**
 * 删除知识库条目
 * @param knowledgeLibId 知识库ID
 * @param docId 文档ID
 * @returns Promise<void>
 */
export const deleteKnowledgeItem = ({ knowledgeLibId, docId }: { knowledgeLibId: string; docId: string }): Promise<void> => {
  return postVoid('/knowledgelib/data/delete', { knowledgeLibId, docId });
};

/**
 * 修改知识库条目
 * @param data 修改的知识库条目数据
 * @returns Promise<KnowledgeItem>
 */
export const modifyKnowledgeItem = (data: KnowledgeSaveRequest & { docId: string, enable: boolean }): Promise<void> => {
  return postVoid('/knowledgelib/data/modify', data);
};

/**
 * 知识库检索
 * @param knowledgeLibId 知识库ID
 * @param question 检索问题
 * @param score 分数阈值
 * @param topK 返回结果数量
 * @returns Promise<KnowledgeRetrievalItem[]>
 */
export const getKnowledgeRetrieval = (
  knowledgeLibId: string,
  question: string,
  score: number,
  topK: number
): Promise<KnowledgeRetrievalItem[]> => {
  return post('/knowledgelib/retrieval', {
    knowledgeLibId,
    question,
    score,
    topK
  });
};
