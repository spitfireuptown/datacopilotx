import { post, get } from '@/utils/request';

export interface LoginParams {
  username: string;
  password: string;
}

export interface RegisterParams {
  username: string;
  password: string;
  nickname: string;
  email: string;
  phone: string;
}

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
}

export interface LoginResult {
  token: string;
}

export function login(params: LoginParams) {
  return post<LoginResult>('/auth/login', params, { noAuth: true, noError: true });
}

export function register(params: RegisterParams) {
  return post<void>('/auth/register', params, { noAuth: true });
}

export function getUserInfo() {
  return get<UserInfo>('/auth/user/info');
}

export function changePassword(oldPassword: string, newPassword: string) {
  return post<void>('/auth/user/change-password', null, {
    params: { oldPassword, newPassword },
  });
}