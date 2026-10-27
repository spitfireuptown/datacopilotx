import { ref } from 'vue';
import { defineStore } from 'pinia';
import { h } from 'vue';
import { Button, notification } from 'ant-design-vue';
import { reportAnalysisStreamApi } from '@/api/chat.ts';
import type { DataReport } from '@/api/chat.ts';

const REPORT_READY_NOTIFY_KEY = 'report-ready';

/**
 * 数据报告后台任务全局状态
 * <p>
 * 报告生成是异步后台任务，状态提升到全局 store 后，
 * 浮窗（生成进度/查看入口）与报告抽屉可以挂载在 App 级组件中，切换页面不丢失。
 */
export const useReportTaskStore = defineStore('reportTask', () => {
  // 是否正在后台生成
  const generating = ref<boolean>(false);
  // 最新进度文本
  const progress = ref<string>('');
  // 已生成的报告数据
  const report = ref<DataReport | null>(null);
  // 最近一次错误信息
  const error = ref<string>('');
  // 报告抽屉开关（仅用户主动查看时打开）
  const drawerOpen = ref<boolean>(false);

  // AbortController 无需响应式，模块内持有即可
  let controller: AbortController | null = null;

  function openDrawer() {
    if (report.value) {
      drawerOpen.value = true;
    }
  }

  function closeDrawer() {
    drawerOpen.value = false;
  }

  /** 中止请求并清理全部状态（新建对话时调用） */
  function reset() {
    if (controller && !controller.signal.aborted) {
      controller.abort();
    }
    controller = null;
    generating.value = false;
    progress.value = '';
    report.value = null;
    error.value = '';
    drawerOpen.value = false;
    notification.close(REPORT_READY_NOTIFY_KEY);
  }

  /** 取消当前生成任务（浮窗取消按钮），保留已有报告入口 */
  function cancelTask() {
    if (controller && !controller.signal.aborted) {
      controller.abort();
    }
    controller = null;
    generating.value = false;
    progress.value = '';
  }

  /**
   * 启动数据报告后台生成任务
   * <p>
   * 调用 /chat/report SSE 接口，进度实时写入 store；
   * 完成后通过通知提示，用户主动点击才打开报告抽屉。
   */
  function startTask(options: {
    message: string;
    datasetId?: string;
    modelId?: string;
    questionId: string;
    sessionId?: string;
  }) {
    if (generating.value) {
      notification.warning({ message: '数据报告', description: '数据报告生成中，请稍候' });
      return;
    }

    notification.close(REPORT_READY_NOTIFY_KEY);
    report.value = null;
    error.value = '';
    progress.value = '正在准备生成数据报告...';
    generating.value = true;
    controller = new AbortController();

    reportAnalysisStreamApi(
      (p: string) => {
        progress.value = p;
      },
      (r: DataReport) => {
        report.value = r;
        error.value = '';
        // 后台生成完成：通知提示，用户主动点击才打开报告抽屉
        notification.open({
          key: REPORT_READY_NOTIFY_KEY,
          message: '数据报告已生成',
          description: '报告已在后台完成，点击按钮即可查看，不影响当前对话。',
          duration: 0,
          btn: () =>
            h(
              Button,
              {
                type: 'primary',
                size: 'small',
                onClick: () => {
                  notification.close(REPORT_READY_NOTIFY_KEY);
                  openDrawer();
                }
              },
              () => '查看报告'
            )
        });
      },
      {
        signal: controller.signal,
        message: options.message,
        datasetId: options.datasetId,
        modelId: options.modelId,
        questionId: options.questionId,
        sessionId: options.sessionId
      },
      () => {
        generating.value = false;
      }
    ).catch((e: any) => {
      generating.value = false;
      progress.value = '';
      if (e?.name === 'AbortError' || controller?.signal.aborted) {
        error.value = '数据报告生成已取消';
        notification.warning({ message: '数据报告', description: error.value });
      } else {
        error.value = '数据报告生成失败: ' + (e?.message || '未知错误');
        notification.error({ message: '数据报告生成失败', description: error.value });
      }
    });
  }

  return {
    generating,
    progress,
    report,
    error,
    drawerOpen,
    startTask,
    cancelTask,
    reset,
    openDrawer,
    closeDrawer
  };
});
