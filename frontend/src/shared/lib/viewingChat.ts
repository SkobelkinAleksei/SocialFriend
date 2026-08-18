import { isGroupChatNotificationLabel } from '@/shared/utils/navigation';

export type ViewingChat = { kind: 'personal' | 'group'; id: string };

let viewing: ViewingChat | null = null;

export function setViewingChat(kind: 'personal' | 'group', id: string | number): void {
  viewing = { kind, id: String(id) };
}

export function clearViewingChat(): void {
  viewing = null;
}

export function getViewingChat(): ViewingChat | null {
  return viewing;
}

/** Открыт ли сейчас тот же чат, из которого пришло уведомление. */
export function isViewingThisChat(body: {
  type?: string;
  contextLabel?: string | null;
  senderId?: unknown;
  targetId?: unknown;
}): boolean {
  if (!body || body.type !== 'NEW_CHAT_MESSAGE' || !viewing) return false;
  if (isGroupChatNotificationLabel(body.contextLabel)) {
    return viewing.kind === 'group' && viewing.id === String(body.targetId ?? '');
  }
  return viewing.kind === 'personal' && viewing.id === String(body.senderId ?? '');
}
