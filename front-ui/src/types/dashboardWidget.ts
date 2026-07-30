/** 仪表盘可拖拽功能组件类型 */
export type WidgetType = 'carousel' | 'text' | 'timer' | 'marquee' | 'numberFlip' | 'progressRing' | 'pulse';

/** 仪表盘功能组件 */
export interface DashboardWidget {
  id: string;
  type: WidgetType;
  title: string;
  layoutX: number;
  layoutY: number;
  layoutW: number;
  layoutH: number;
  bgColor?: string;
  config: Record<string, any>;
}

/** 轮播组件配置 */
export interface CarouselConfig {
  images: string[];       // 图片 URL 列表
  interval: number;       // 切换间隔(ms)
  transition: 'fade' | 'slide'; // 切换动画
}

/** 文本标注组件配置 */
export interface TextConfig {
  content: string;
  fontSize: number;
  fontColor: string;
  textAlign: 'left' | 'center' | 'right';
}

/** 计时器组件配置 */
export interface TimerConfig {
  format: '12h' | '24h';
  fontSize: number;
  fontColor: string;
  showDate: boolean;
}

/** 滚动字幕组件配置 */
export interface MarqueeConfig {
  text: string;
  speed: number;        // 滚动速度(px/s)
  direction: 'left' | 'right'; // 滚动方向
  fontSize: number;
  fontColor: string;
}

/** 数字翻牌器组件配置 */
export interface NumberFlipConfig {
  value: number;        // 目标值
  prefix: string;       // 前缀（如 ¥）
  suffix: string;       // 后缀（如 %）
  fontSize: number;
  fontColor: string;
  duration: number;     // 动画时长(ms)
  decimals: number;     // 小数位数
}

/** 进度环组件配置 */
export interface ProgressRingConfig {
  value: number;        // 0-100
  size: number;         // 环大小
  strokeWidth: number;  // 线宽
  color: string;        // 进度条颜色
  trackColor: string;   // 轨道颜色
  showLabel: boolean;   // 显示百分比
  fontSize: number;
}

/** 脉冲指示器组件配置 */
export interface PulseConfig {
  text: string;
  color: string;        // 脉冲颜色
  fontSize: number;
  pulseSize: 'small' | 'medium' | 'large';
}

// ==================== localStorage 工具 ====================
const WIDGETS_PREFIX = 'dashboard_widgets_';
const BG_PREFIX = 'dashboard_bg_';
const CARD_COLOR_PREFIX = 'card_color_';

export function loadWidgets(dashboardId: number): DashboardWidget[] {
  try {
    const raw = localStorage.getItem(`${WIDGETS_PREFIX}${dashboardId}`);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveWidgets(dashboardId: number, widgets: DashboardWidget[]) {
  localStorage.setItem(`${WIDGETS_PREFIX}${dashboardId}`, JSON.stringify(widgets));
}

export function loadDashboardBg(dashboardId: number): string {
  return localStorage.getItem(`${BG_PREFIX}${dashboardId}`) || '';
}

export function saveDashboardBg(dashboardId: number, color: string) {
  if (color) {
    localStorage.setItem(`${BG_PREFIX}${dashboardId}`, color);
  } else {
    localStorage.removeItem(`${BG_PREFIX}${dashboardId}`);
  }
}

export function loadCardColor(cardId: number): string {
  return localStorage.getItem(`${CARD_COLOR_PREFIX}${cardId}`) || '';
}

export function saveCardColor(cardId: number, color: string) {
  if (color) {
    localStorage.setItem(`${CARD_COLOR_PREFIX}${cardId}`, color);
  } else {
    localStorage.removeItem(`${CARD_COLOR_PREFIX}${cardId}`);
  }
}

/** 生成简单 UUID */
export function generateWidgetId(): string {
  return `w_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

/** 判断颜色是否深色，用于自动切换文字颜色 */
export function isDarkColor(hex: string): boolean {
  if (!hex || hex.length < 4) {return false;}
  const c = hex.replace('#', '');
  const r = parseInt(c.substring(0, 2), 16);
  const g = parseInt(c.substring(2, 4), 16);
  const b = parseInt(c.substring(4, 6), 16);
  // 相对亮度公式
  return (r * 0.299 + g * 0.587 + b * 0.114) < 150;
}

/** 调整颜色明暗 */
export function adjustColor(hex: string, percent: number): string {
  const c = hex.replace('#', '');
  const r = Math.min(255, Math.max(0, parseInt(c.substring(0, 2), 16) + Math.round(255 * percent / 100)));
  const g = Math.min(255, Math.max(0, parseInt(c.substring(2, 4), 16) + Math.round(255 * percent / 100)));
  const b = Math.min(255, Math.max(0, parseInt(c.substring(4, 6), 16) + Math.round(255 * percent / 100)));
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}
