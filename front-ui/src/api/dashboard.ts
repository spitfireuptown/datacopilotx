import { get, post, put, del } from '@/utils/request';

// ==================== 仪表盘 ====================
export interface DashboardItem {
  id?: number;
  name: string;
  description?: string;
  creator?: string;
  ctime?: string;
  utime?: string;
}

/** 创建仪表盘 */
export const createDashboard = (name: string, description?: string): Promise<DashboardItem> => {
  return post<DashboardItem>('/dashboard/create', { name, description });
};

/** 获取仪表盘列表 */
export const getDashboardList = (): Promise<DashboardItem[]> => {
  return get<DashboardItem[]>('/dashboard/list');
};

/** 重命名仪表盘 */
export const renameDashboard = (id: number, name: string): Promise<void> => {
  return put<void>('/dashboard/rename', { id, name });
};

/** 删除仪表盘 */
export const deleteDashboard = (id: number): Promise<void> => {
  return del(`/dashboard/delete/${id}`);
};

// ==================== 仪表盘图表 ====================
export interface DashboardChartItem {
  id?: number;
  dashboardId?: number;
  chartName?: string;
  chartType: string;
  chartData: string;
  sqlText?: string;
  question?: string;
  questionId?: string;
  sessionId?: string;
  layoutX: number;
  layoutY: number;
  layoutW: number;
  layoutH: number;
  creator?: string;
  ctime?: string;
  utime?: string;
}

/** 保存图表到仪表盘 */
export const saveDashboardChart = (data: DashboardChartItem): Promise<DashboardChartItem> => {
  return post<DashboardChartItem>('/dashboard/chart/save', data);
};

/** 获取某个仪表盘的图表列表 */
export const getDashboardChartList = (dashboardId: number): Promise<DashboardChartItem[]> => {
  return get<DashboardChartItem[]>('/dashboard/chart/list', { dashboardId });
};

/** 获取单个图表 */
export const getDashboardChart = (id: number): Promise<DashboardChartItem> => {
  return get<DashboardChartItem>(`/dashboard/chart/get/${id}`);
};

/** 更新图表 */
export const updateDashboardChart = (data: Partial<DashboardChartItem>): Promise<void> => {
  return put<void>('/dashboard/chart/update', data);
};

/** 更新图表布局 */
export const updateDashboardLayout = (data: { id: number; layoutX: number; layoutY: number; layoutW: number; layoutH: number }): Promise<void> => {
  return put<void>('/dashboard/chart/updateLayout', data);
};

/** 删除图表 */
export const deleteDashboardChart = (id: number): Promise<void> => {
  return del(`/dashboard/chart/delete/${id}`);
};

// ==================== 问数记录 ====================
export interface QuestionWithChartItem {
  id: number;
  questionId: string;
  sessionId: string;
  question: string;
  result: string;
  sql: string;
  ctime: string;
}

/** 获取含有图表数据的问数记录 */
export const getQuestionsWithChart = (): Promise<QuestionWithChartItem[]> => {
  return get<QuestionWithChartItem[]>('/dashboard/questions');
};
