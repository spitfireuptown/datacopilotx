/** AI生成，不保证可用性，请自行替换 */
interface MockUploadResponse {
  data: any;
  code: number;
}

interface MockUploadOptions {
  onProgress?: (percent: number) => void;
  successRate?: number;
  delay?: number;
}

/**
 * 模拟文件上传 API
 * @param file 要上传的文件对象
 * @param options 配置选项
 * @returns Promise 包含上传结果
 */
export function mockUploadApi(
  file: File,
  options: MockUploadOptions = {}
): Promise<MockUploadResponse> {
  const { onProgress, successRate = 0.9, delay = 2000 } = options;

  return new Promise((resolve, reject) => {
    const startTime = Date.now();
    let uploadProgress = 0;

    // 模拟进度更新
    const progressInterval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      uploadProgress = Math.min((elapsed / delay) * 100, 100);

      if (onProgress) {
        onProgress(Math.floor(uploadProgress));
      }

      if (uploadProgress >= 100) {
        clearInterval(progressInterval);
      }
    }, 50);

    // 模拟网络请求
    setTimeout(() => {
      clearInterval(progressInterval);

      console.log('Upload progress:', Math.random(), successRate, file);

      if (successRate) {
        resolve({
          code: 200,
          data: file
        });
      } else {
        reject(new Error('Upload failed due to network error'));
      }
    }, delay);
  });
}
