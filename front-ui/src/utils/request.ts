import { message } from 'ant-design-vue';

// 定义请求配置接口
export interface FetchRequestConfig {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers?: Record<string, string>;
  body?: any;
  params?: Record<string, any>;
  timeout?: number;
  skipResponseParse?: boolean; // 新增选项，表示是否跳过响应体解析
  noAuth?: boolean; // 标记为不需要认证的请求
  noError?: boolean; // 标记为不显示错误消息，由调用方自行处理
}

// 定义响应数据接口
interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

// 基础URL，与vite.config.ts中的代理配置对应
const BASE_URL = '/api';

// 创建请求函数
const request = async <T = any>(
  url: string,
  config: FetchRequestConfig = {}
): Promise<T> => {
  // 构建完整URL
  let fullUrl = BASE_URL + url;
  
  // 处理查询参数
  if (config.params) {
    const params = new URLSearchParams();
    Object.entries(config.params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params.append(key, String(value));
      }
    });
    const paramsString = params.toString();
    if (paramsString) {
      fullUrl += (fullUrl.includes('?') ? '&' : '?') + paramsString;
    }
  }
  
  // 设置默认请求头
  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  
  // 添加token等认证信息
  const token = localStorage.getItem('access_token');
  if (token && !config.noAuth) {
    defaultHeaders.Authorization = `Bearer ${token}`;
  }
  
  // 合并请求头
  const headers = { ...defaultHeaders, ...config.headers };
  
  // 构建fetch选项
  const fetchOptions: RequestInit = {
    method: config.method || 'GET',
    headers,
  };
  
  // 处理请求体
  if (config.body) {
    // 如果body是FormData，删除Content-Type让浏览器自动设置multipart/form-data和边界
    if (config.body instanceof FormData) {
      delete headers['Content-Type'];
      fetchOptions.body = config.body;
    } else if (headers['Content-Type'] === 'application/json') {
      fetchOptions.body = JSON.stringify(config.body);
    } else {
      fetchOptions.body = config.body;
    }
  }
  
  // 更新fetch选项的headers（如果被修改）
  fetchOptions.headers = headers;
  
  // 设置超时
  const timeout = config.timeout || 60000; // 默认60秒
  
  try {
    // 使用Promise.race实现超时控制
    const controller = new AbortController();
    const { signal } = controller;
    fetchOptions.signal = signal;
    
    const timeoutId = setTimeout(() => controller.abort(), timeout);
    
    const response = await fetch(fullUrl, fetchOptions);
    clearTimeout(timeoutId);
    
    if (!response.ok) {
      // 处理HTTP错误
      const errorData = await response.json().catch(() => ({}));
      if (response.status === 401) {
        localStorage.removeItem('access_token');
        window.location.href = '/login';
      }
      throw {
        status: response.status,
        message: errorData.message || `Request failed with status ${response.status}`,
        data: errorData
      };
    }
    
    // 检查是否需要跳过响应体解析，或响应体是否为空
    if (config.skipResponseParse || config.method === 'DELETE') {
      const contentType = response.headers.get('content-type');
      if (!contentType || !contentType.includes('application/json')) {
        // 如果不需要解析响应体或响应体不是JSON格式或为空，直接返回void
        return undefined as unknown as T;
      }
    }
    
    // 解析响应数据
    const data: ApiResponse<T> = await response.json();
    
    // 统一处理响应数据
    if (data.code !== 200) {
      throw new Error(data.message || '请求失败');
    }
    
    return data.data;
  } catch (error: any) {
    // 统一处理错误
    if (error.name === 'AbortError') {
      if (!config.noError) {message.error('请求超时，请稍后重试');}
    } else if (error.status) {
      switch (error.status) {
        case 401:
          localStorage.removeItem('access_token');
          window.location.href = '/login';
          break;
        case 403:
          if (!config.noError) {message.error('拒绝访问');}
          break;
        case 404:
          if (!config.noError) {message.error('请求资源不存在');}
          break;
        case 500:
          if (!config.noError) {message.error('服务器错误');}
          break;
        default:
          if (!config.noError) {message.error(error.message || '请求失败');}
      }
    } else {
      if (!config.noError) {message.error(error.message);}
    }
    
    throw error;
  }
};

/**
 * 通用请求方法
 * @param config 请求配置
 * @returns Promise<any>
 */
export const requestApi = <T = any>(config: {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  data?: any;
  params?: Record<string, any>;
  headers?: Record<string, string>;
  timeout?: number;
  noAuth?: boolean;
}): Promise<T> => {
  return request<T>(config.url, {
    method: config.method,
    body: config.data,
    params: config.params,
    headers: config.headers,
    timeout: config.timeout,
    noAuth: config.noAuth,
  });
};

/**
 * GET请求
 * @param url 请求地址
 * @param params 查询参数
 * @param config 其他配置
 * @returns Promise<any>
 */
export const get = <T = any>(
  url: string,
  params?: Record<string, any>,
  config?: Omit<FetchRequestConfig, 'method' | 'body' | 'params'>
): Promise<T> => {
  return request<T>(url, {
    method: 'GET',
    params,
    ...config
  });
};

/**
 * POST请求
 * @param url 请求地址
 * @param data 请求数据
 * @param config 其他配置
 * @returns Promise<any>
 */
export const post = <T = any>(
  url: string,
  data?: any,
  config?: Omit<FetchRequestConfig, 'method' | 'body'>
): Promise<T> => {
  return request<T>(url, {
    method: 'POST',
    body: data,
    ...config
  });
};

/**
 * POST void请求
 * @param url 请求地址
 * @param config 其他配置
 * @returns Promise<void>
 */
export const postVoid = (
  url: string,
  data?: any,
  config?: Omit<FetchRequestConfig, 'method' | 'body'>
): Promise<void> => {
  return request<void>(url, {
    method: 'POST',
    body: data,
    skipResponseParse: true, // 设置为true，表示跳过响应体解析
    ...config
  });
};

/**
 * PUT请求
 * @param url 请求地址
 * @param data 请求数据
 * @param config 其他配置
 * @returns Promise<any>
 */
export const put = <T = any>(
  url: string,
  data?: any,
  config?: Omit<FetchRequestConfig, 'method' | 'body'>
): Promise<T> => {
  return request<T>(url, {
    method: 'PUT',
    body: data,
    ...config
  });
};

/**
 * DELETE请求
 * @param url 请求地址
 * @param config 其他配置
 * @returns Promise<void>
 */
export const del = (
  url: string,
  config?: Omit<FetchRequestConfig, 'method' | 'body'>
): Promise<void> => {
  return request<void>(url, {
    method: 'DELETE',
    ...config
  });
};

/**
 * 文件上传请求
 * @param url 请求地址
 * @param formData 包含文件的FormData对象
 * @param config 其他配置
 * @returns Promise<T>
 */
export const uploadFile = <T = any>(
  url: string,
  formData: FormData,
  config?: Omit<FetchRequestConfig, 'method' | 'body' | 'headers'>
): Promise<T> => {
  return request<T>(url, {
    method: 'POST',
    body: formData,
    ...config
  });
};

export default request;