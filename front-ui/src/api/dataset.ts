import { get, post, del, uploadFile as uploadFileRequest } from '../utils/request';

interface DatasetTableConfig {
  table: string;
  prompt: string;
  fields: Array<{
    fieldName: string;
    fieldType: string;
    description: string;
  }>;
}

interface Dataset {
  id: number;
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  table: string;
  username: string;
  password?: string;
  createTime: string;
  description?: string;
  fields?: Array<{
    fieldName: string;
    fieldType: string;
  }>;
  tables?: Array<{
    id: number;
    table: string;
    prompt: string;
    fields: Array<{
      fieldName: string;
      fieldType: string;
      description: string;
    }>;
  }>;
}

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
  prompt?: string;
  fields?: Array<{
    fieldName: string;
    fieldType: string;
    description: string;
  }>;
  relations?: Array<{
    fromTable: string;
    fromField: string;
    toTable: string;
    toField: string;
    relationType: string;
  }>;
}

interface DatasetCreateWithTablesData {
  id?: number;
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  username: string;
  password: string;
  description?: string;
  tables: DatasetTableConfig[];
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
 * 创建新数据集（包含多表配置）
 * @param data 数据集数据（包含表配置列表）
 * @returns Promise<number> 返回数据集ID
 */
export const createDatasetWithTables = (data: DatasetCreateWithTablesData): Promise<number> => {
  return post('/dataset/create', data);
};

/**
 * 更新数据集（包含多表配置）
 * @param data 数据集数据（包含表配置列表）
 * @returns Promise<number> 返回数据集ID
 */
export const updateDatasetWithTables = (data: DatasetCreateWithTablesData): Promise<number> => {
  return post('/dataset/update', data);
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
 * 添加数据集关联关系
 * @param data 关联关系数据
 * @returns Promise<{ data: number }>
 */
export const addDatasetRelation = (data: {
  datasetId: number;
  leftTable: string;
  leftField: string;
  rightTable: string;
  rightField: string;
  relationType: string;
  description?: string;
}): Promise<{ data: number }> => {
  return post(`/dataset/relation/create`, data);
};

/**
 * 删除数据集关联关系
 * @param id 关联关系ID
 * @returns Promise<any>
 */
export const deleteDatasetRelation = (id: string): Promise<any> => {
  return del(`/dataset/relation/delete/${id}`);
};

/**
 * 根据数据集ID查询关联关系
 * @param datasetId 数据集ID
 * @returns 关联关系列表
 */
export const getDatasetRelations = (datasetId: string): Promise<Array<{
  id: number;
  datasetId: number;
  datasetName: string;
  leftTable: string;
  leftField: string;
  rightTable: string;
  rightField: string;
  relationType: string;
  description?: string;
}>> => {
  return get(`/dataset/relation/list/${datasetId}`);
};

/**
 * 获取表字段信息
 * @param data 连接信息和表名
 * @returns Promise<DatasetField[]> 字段列表
 */
export const getTableFields = (data: {
  type: string;
  host: string;
  port: number;
  username: string;
  password?: string;
  database: string;
  table: string;
}): Promise<DatasetField[]> => {
  return post<DatasetField[]>('/dataset/table/info', data);
};

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
 * 获取数据库中的表列表
 * @param data 连接信息
 * @returns Promise<string[]> 表名列表
 */
export const getTables = (data: {
  type: string;
  host: string;
  port: number;
  username: string;
  password?: string;
  database: string;
}): Promise<string[]> => {
  return post<string[]>('/dataset/tables', data);
};

/**
 * 上传文件
 * @param file 要上传的文件
 * @param name 文件名称（可选）
 * @param description 文件描述（可选）
 * @returns Promise<DatasetField[]> 返回字段列表
 */
export const uploadFile = (
  file: File,
  name?: string,
  description?: string
): Promise<DatasetField[]> => {
  const formData = new FormData();
  formData.append('file', file);
  if (name) {
    formData.append('name', name);
  }
  if (description) {
    formData.append('description', description);
  }
  return uploadFileRequest<DatasetField[]>('/dataset/file/upload', formData);
};