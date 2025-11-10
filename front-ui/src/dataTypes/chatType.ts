export interface MessageItem {
  id: string;
  message: string;
  status: 'local' | 'ai';
  loading?: boolean;
  jsonData?: any; // 添加jsonData字段
}
