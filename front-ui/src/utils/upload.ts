import { message } from 'ant-design-vue';
import type { UploadChangeParam } from 'ant-design-vue';

interface UploadOptions<T = any> {
  onSuccess?: (file: FileItem) => void;
  accept?: string;
  maxSize?: number; // 单位：MB
  uploadRequest?: (...args: any) => Promise<T>;
  requestParams?: Record<string, any>;
}

interface FileItem {
  id: string;
  name: string;
  size: number;
  type: string;
}

export function useUpload<T = any>(options: UploadOptions<T>) {
  const {
    onSuccess,
    accept,
    maxSize = 20,
    uploadRequest,
    requestParams = {}
  } = options;

  const uploadLoading = ref(false);
  const uploadFileList = ref([]);

  const customRequest = async (customOptions: any) => {
    const { onSuccess: uploadSuccess, onError, file } = customOptions;

    // 文件大小检查
    if (file.size > maxSize * 1024 * 1024) {
      message.error(`文件大小不能超过 ${maxSize}MB`);
      onError(new Error(`File size exceeded ${maxSize}MB`));
      return;
    }

    try {
      let data: any;
      if (uploadRequest) {
        // 使用传入的上传方法
        data = await uploadRequest(file, requestParams);
      }

      if (data.code === 200) {
        uploadSuccess(data.data);
      } else {
        throw new Error(data.message || '上传失败');
      }
    } catch (error: any) {
      message.error(error.message || '文件上传失败');
      onError(error);
    }
  };

  const handleChange = (info: UploadChangeParam) => {
    const status = info.file.status;

    if (status === 'uploading') {
      uploadLoading.value = true;
      return;
    }

    uploadLoading.value = false;

    if (status === 'done') {
      const fileData = {
        id: info.file.response?.uuid || info.file.uid,
        name: info.file.name,
        size: info.file.size || 0,
        type: info.file.type || 'unknown'
      };

      message.success(`${info.file.name} 上传成功`);
      onSuccess?.(fileData);
    }
  };

  return {
    uploadLoading,
    uploadFileList,
    customRequest,
    handleChange,
    accept
  };
}
