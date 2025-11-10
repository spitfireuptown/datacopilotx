import { get, post, put, del, postVoid } from '../utils/request';

// 定义模型类型接口
// 将type改为platform，移除version
interface Model {
  id: string;
  name: string;
  platform: string;
  apiKey: string;
  apiBase?: string;
  params?: string;
  createTime: string;
}

// 定义模型表单数据接口
// 将type改为platform，移除version
interface ModelFormData {
  name: string;
  platform: string;
  apiKey: string;
  apiBase?: string;
  params?: string;
}

// 保留原来的函数声明，只需修改类型引用
/**
 * 获取模型列表
 * @param params 查询参数，可包含type等过滤条件
 * @returns Promise<Model[]>
 */
export const getModelList = (params?: { type?: string }): Promise<Model[]> => {
  return get<Model[]>('/model/list', params);
};

/**
 * 创建新模型
 * @param data 模型数据
 * @returns Promise<Model>
 */
export const createModel = (data: ModelFormData): Promise<Model> => {
  return post<Model>('/model/create', data);
};

/**
 * 更新模型
 * @param id 模型ID
 * @param data 模型数据
 * @returns Promise<Model>
 */
export const updateModel = (data: ModelFormData): Promise<Model> => {
  return put<Model>(`/model/modify`, data);
};

/**
 * 删除模型
 * @param id 模型ID
 * @returns Promise<void>
 */
export const deleteModel = (id: string): Promise<void> => {
  return del<void>(`/model/${id}`);
};

/**
 * 测试模型连接
 * @param id 模型ID
 * @returns Promise<boolean>
 */
export const testModelConnection = (id: string): Promise<void> => {
  return postVoid(`/model/test-connection/${id}`);
};