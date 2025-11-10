import { MessageItem } from '@/dataTypes/chatType.ts';

export const useDialogueStore = defineStore('dialogue', () => {
  const currentFile = ref<any>();
  const currentConversitionUuid = ref<string | undefined>('');
  const dialogueRecord = ref<Array<MessageItem>>([]);
  const isChatting = ref<boolean>(false);
  
  // 添加对sessionId的支持
  const sessionId = ref<string | undefined>('');

  const getCurrentFileUuid = computed(() => currentFile.value.uuid);
  const getCurrentFilename = computed(() => currentFile.value.filename);
  const getCurrentConversitionUuid = computed(
    () => currentConversitionUuid.value
  );
  const getDialogueRecord = computed(() => dialogueRecord.value);
  const getIsChatting = computed(() => isChatting.value);
  const getSessionId = computed(() => sessionId.value);

  function setCurrentFileInfo(file: any) {
    currentFile.value = file;
  }

  function setCurrentConversitionUuid(uuid: string | undefined) {
    currentConversitionUuid.value = uuid;
  }

  function setDialogueRecord(record: MessageItem[]) {
    dialogueRecord.value = record;
  }

  function setIsChatting(status: boolean) {
    isChatting.value = status;
  }
  
  // 添加设置sessionId的方法
  function setSessionId(id: string | undefined) {
    sessionId.value = id;
  }

  function resetHistory() {
    currentConversitionUuid.value = undefined;
    dialogueRecord.value = [];
    sessionId.value = undefined; // 重置sessionId
  }

  return {
    getCurrentFileUuid,
    getCurrentFilename,
    getCurrentConversitionUuid,
    getDialogueRecord,
    getIsChatting,
    getSessionId,
    setCurrentFileInfo,
    setCurrentConversitionUuid,
    setDialogueRecord,
    setIsChatting,
    setSessionId,
    resetHistory
  };
});
