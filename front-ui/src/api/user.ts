import { get, post } from '@/utils/request';

// 用户相关接口地址
const userApi = {
  UserList: '/auth/user/list',
  AddUser: '/auth/user/add',
  UpdateUser: '/auth/user/update',
  DeleteUser: '/auth/user/delete',
  ChangeStatus: '/auth/user/status',
  ResetPassword: '/auth/user/reset-password',
  UserInfo: '/auth/user/info'
};

// 用户信息类型定义
export interface UserInfo {
  userId: string;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  role: number;
  roleDesc: string;
  status: number;
  statusDesc: string;
  createdAt: number;
  updatedAt: number;
}

// 用户列表响应类型
export interface UserListResponse {
  list: UserInfo[];
  total: number;
}

// 用户表单类型
export interface UserForm {
  userId?: string;
  username: string;
  password?: string;
  nickname: string;
  email: string;
  phone: string;
  role: number;
  status: number;
}

// 获取用户列表
export function getUserList(params: {
  username?: string;
  page: number;
  size: number;
}): Promise<UserListResponse> {
  return get(userApi.UserList, params);
}

// 添加用户
export function addUser(data: UserForm): Promise<void> {
  return post(userApi.AddUser, data);
}

// 更新用户
export function updateUser(data: UserForm): Promise<void> {
  return post(userApi.UpdateUser, data);
}

// 删除用户
export function deleteUser(userId: string): Promise<void> {
  return post(userApi.DeleteUser, { userId });
}

// 修改用户状态
export function changeUserStatus(userId: string, status: number): Promise<void> {
  return post(userApi.ChangeStatus, { userId, status });
}

// 重置密码
export function resetPassword(userId: string): Promise<void> {
  return post(userApi.ResetPassword, { userId });
}

// 获取当前用户信息
export function getCurrentUserInfo(): Promise<UserInfo> {
  return get(userApi.UserInfo);
}