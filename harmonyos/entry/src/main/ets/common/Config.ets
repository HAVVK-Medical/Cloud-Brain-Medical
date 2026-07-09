export const API_BASE_URL = 'http://172.18.120.191:8088/api';
export const SESSION_STORAGE_KEY = 'havvk_medical_patient_session';

export function getStatusLabel(status: string | null | undefined): string {
  switch (status) {
    case 'WAITING':
      return '待就诊';
    case 'IN_PROGRESS':
      return '就诊中';
    case 'COMPLETED':
      return '已完成';
    case 'CANCELLED':
      return '已取消';
    case null:
    case undefined:
    case '':
      return '未知';
    default:
      return status;
  }
}

export function formatDate(input: string | null | undefined): string {
  if (!input) {
    return '未安排';
  }
  return input.replace('T', ' ').slice(0, 16);
}
