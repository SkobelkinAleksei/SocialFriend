import { showAppInfoToast } from '@/shared/utils/appToast';

export type ChatShareTarget = { recipientId?: number; chatId?: number };

type ChatStomp = {
  connected?: boolean;
  publish: (args: { destination: string; body: string }) => void;
} | null | undefined;

type ChatUser = {
  id?: number;
  firstName?: string;
  lastName?: string;
} | null | undefined;

export function publishChatMessages(
  stompClient: ChatStomp,
  user: ChatUser,
  targets: ChatShareTarget[],
  payload: { content?: string; photos?: string[] }
): boolean {
  if (!stompClient?.connected) {
    showAppInfoToast('Чат', 'Нет соединения с чатом. Откройте раздел сообщений и попробуйте снова.');
    return false;
  }
  if (!user?.id) {
    showAppInfoToast('Чат', 'Нужно войти в аккаунт');
    return false;
  }
  if (!targets.length) return false;

  targets.forEach((target) => {
    stompClient.publish({
      destination: '/app/chat',
      body: JSON.stringify({
        chatId: target.chatId ? Number(target.chatId) : null,
        recipientId: target.recipientId ? Number(target.recipientId) : null,
        content: payload.content || '',
        photos: payload.photos || [],
        senderId: user.id,
        senderFirstName: user.firstName,
        senderLastName: user.lastName,
        parentIds: [],
      }),
    });
  });
  return true;
}

export function buildSharePostContent(
  postId: number,
  authorName: string,
  content: string,
  comment?: string,
  authorId?: number
): string {
  const snippet = (content || '').replace(/\s+/g, ' ').trim().slice(0, 80);
  const who = (authorName || 'Сосед').trim();
  const label = snippet
    ? `Пост ${who}: «${snippet}${snippet.length >= 80 ? '…' : ''}»`
    : `Пост ${who}`;
  const marker = authorId
    ? `[SHARE_POST:${postId}:${authorId}]`
    : `[SHARE_POST:${postId}]`;
  let text = `${marker} ${label}`;
  if (comment?.trim()) {
    text += `\n${comment.trim()}`;
  }
  return text;
}

export function isSharePostText(text?: string | null): boolean {
  return String(text || '').includes('[SHARE_POST:');
}

export function isShareEventText(text?: string | null): boolean {
  return String(text || '').includes('[SHARE_EVENT:');
}

export function isShareCardText(text?: string | null): boolean {
  return isSharePostText(text) || isShareEventText(text);
}

export function republishShareCardContent(originalText: string, comment?: string): string {
  const firstLine = String(originalText || '').split('\n')[0].trim();
  if (!firstLine) return String(originalText || '');
  if (comment?.trim()) return `${firstLine}\n${comment.trim()}`;
  return firstLine;
}

export type ForwardableMessage = {
  id?: string | number;
  senderId?: number;
  senderFirstName?: string;
  senderLastName?: string;
  text?: string;
  content?: string;
  photos?: string[];
  files?: Array<{ url?: string; name?: string; size?: number; mimeType?: string }>;
  voiceUrl?: string;
  voiceDuration?: number;
};

function messageText(message: ForwardableMessage): string {
  return message.text || message.content || '';
}

export function publishForwardToChats(
  stompClient: ChatStomp,
  user: ChatUser,
  targets: ChatShareTarget[],
  messages: ForwardableMessage[],
  comment?: string
): boolean {
  if (!stompClient?.connected) {
    showAppInfoToast('Чат', 'Нет соединения с чатом. Откройте раздел сообщений и попробуйте снова.');
    return false;
  }
  if (!user?.id) {
    showAppInfoToast('Чат', 'Нужно войти в аккаунт');
    return false;
  }
  if (!targets.length || !messages.length) return false;

  const shares = messages.filter((item) => isShareCardText(messageText(item)));
  const others = messages.filter((item) => !isShareCardText(messageText(item)));
  const shareComment = others.length ? undefined : comment;

  targets.forEach((target) => {
    const base = {
      chatId: target.chatId ? Number(target.chatId) : null,
      recipientId: target.recipientId ? Number(target.recipientId) : null,
      senderId: user.id,
      senderFirstName: user.firstName,
      senderLastName: user.lastName,
    };

    shares.forEach((share) => {
      stompClient.publish({
        destination: '/app/chat',
        body: JSON.stringify({
          ...base,
          content: republishShareCardContent(messageText(share), shareComment),
          photos: share.photos || [],
          parentIds: [],
        }),
      });
    });

    if (!others.length) return;

    stompClient.publish({
      destination: '/app/chat',
      body: JSON.stringify({
        ...base,
        content: comment || '',
        parentIds: others
          .map((item) => Number(item.id))
          .filter((id) => Number.isFinite(id) && id > 0),
        forwardedFrom: null,
        bundledForwards: others.map((msg) => ({
          id: msg.id ? Number(msg.id) : null,
          senderId: msg.senderId ? Number(msg.senderId) : null,
          senderFirstName: msg.senderFirstName || '',
          senderLastName: msg.senderLastName || '',
          content: messageText(msg),
          photos: msg.photos || [],
          files: msg.files || [],
          voiceUrl: msg.voiceUrl,
          voiceDuration: msg.voiceDuration,
        })),
      }),
    });
  });

  return true;
}
