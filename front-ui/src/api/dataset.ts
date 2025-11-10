import { get, post, del } from '../utils/request';

// 定义数据集类型接口
interface Dataset {
  id: string;
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  table: string;
  username: string;
  password?: string;
  createTime: string;
  fields?: Array<{
    fieldName: string;
    fieldType: string;
    description: string;
  }>;
}

// 定义数据集表单数据接口
interface DatasetFormData {
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  table: string;
  username: string;
  password: string;
  tableInput?: string;
  fields?: Array<{
    fieldName: string;
    fieldType: string;
    description: string;
  }>;
}

interface DatasetField {
    fieldName: string;
    fieldType: string;
    description: string;
}

/**
 * 获取数据集列表
 * @returns Promise<Dataset[]>
 */
export const getDatasetList = (): Promise<Dataset[]> => {
  return get('/dataset/list');
};

/**
 * 创建新数据集
 * @param data 数据集数据
 * @returns Promise<Dataset>
 */
export const createDataset = (data: DatasetFormData): Promise<Dataset> => {
  return post('/dataset/create', data);
};

/**
 * 更新数据集
 * @param id 数据集ID
 * @param data 数据集数据
 * @returns Promise<Dataset>
 */
export const updateDataset = (data: DatasetFormData): Promise<Dataset> => {
  return post(`/dataset/update`, data);
};

/**
 * 删除数据集
 * @param id 数据集ID
 * @returns Promise<void>
 */
export const deleteDataset = (id: string): Promise<void> => {
  return del(`/dataset/delete/${id}`);
};

/**
 * 获取数据集详情
 * @param id 数据集ID
 * @returns Promise<Dataset>
 */
export const getDatasetDetail = (id: string): Promise<Dataset> => {
  return get(`/dataset/detail/${id}`);
};

/**
 * 测试数据库连接
 * @param data 连接信息
 * @returns Promise<{
 *   success: boolean;
 *   tables?: string[];
 *   message?: string;
 * }>
 */
export const testDatabaseConnection = (data: {
  type: string;
  host: string;
  port: number;
  username: string;
  password?: string;
  database: string;
  table?: string;
  name?: string;
}): Promise<DatasetField[]> => {
  return post<DatasetField[]>('/dataset/table/info', data);
};

/**
 * 获取数据库表结构
 * @param data 连接信息和表名
 * @returns Promise<{
 *   fields: Array<{
 *     fieldName: string;
 *     fieldType: string;
 *     description?: string;
 *   }>;
 * }>
 */
export const getTableStructure = (data: {
  type: string;
  host: string;
  port: number;
  username: string;
  password?: string;
  database: string;
  table: string;
  name?: string;
}): Promise<{
  fields: Array<{
    fieldName: string;
    fieldType: string;
    description?: string;
  }>;
}> => {
  return post('/dataset/get-table-structure', data);
};