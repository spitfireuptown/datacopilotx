import { get, post, put, del } from '../utils/request';

interface ExpressionItem {
  type: string;
  fieldName?: string;
  filterType?: string;
  term?: string;
  value?: any;
  subTree?: ExpressionTree;
}

interface ExpressionTree {
  logic: string;
  items: ExpressionItem[];
}

interface ColumnPermission {
  fieldId: number;
  fieldName: string;
  fieldComment: string;
  enable: boolean;
}

export interface Permission {
  id?: number;
  enable: number;
  type: string;
  dsId: number;
  tableId?: number;
  tableName?: string;
  name: string;
  expressionTree?: ExpressionTree;
  permissions?: ColumnPermission[];
  whiteListUser?: string[];
}

export interface Rule {
  id?: number;
  enable: number;
  name: string;
  permissionList: number[];
  userList: string[];
}

export const createPermission = (data: Permission): Promise<Permission> => {
  return post('/api/permission/create', data);
};

export const updatePermission = (data: Permission): Promise<Permission> => {
  return put('/api/permission/update', data);
};

export const deletePermission = (id: number): Promise<void> => {
  return del(`/api/permission/delete/${id}`);
};

export const getPermissionById = (id: number): Promise<Permission> => {
  return get(`/api/permission/get/${id}`);
};

export const getPermissionsByDsId = (dsId: number): Promise<Permission[]> => {
  return get(`/api/permission/list/${dsId}`);
};

export const getRowPermissionsByDsId = (dsId: number): Promise<Permission[]> => {
  return get(`/api/permission/row/${dsId}`);
};

export const getColumnPermissionsByDsId = (dsId: number): Promise<Permission[]> => {
  return get(`/api/permission/column/${dsId}`);
};

export const createRule = (data: Rule): Promise<Rule> => {
  return post('/api/permission/rule/create', data);
};

export const updateRule = (data: Rule): Promise<Rule> => {
  return put('/api/permission/rule/update', data);
};

export const deleteRule = (id: number): Promise<void> => {
  return del(`/api/permission/rule/delete/${id}`);
};

export const getRuleById = (id: number): Promise<Rule> => {
  return get(`/api/permission/rule/get/${id}`);
};

export const getAllRules = (): Promise<Rule[]> => {
  return get('/api/permission/rule/list');
};

export const batchCreatePermissions = (data: Permission[]): Promise<Permission[]> => {
  return post('/api/permission/batch', data);
};