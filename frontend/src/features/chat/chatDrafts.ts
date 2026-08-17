export type ChatDraftScope = 'personal' | 'group';

const DRAFT_EVENT = 'myraion-chat-drafts-changed';
const keyOf = (scope: ChatDraftScope, id: string | number) => `myraion.chat.draft.${scope}.${id}`;

function notifyDraftsChanged() {
    try {
        window.dispatchEvent(new Event(DRAFT_EVENT));
    } catch {
        // ignore
    }
}

export function subscribeChatDrafts(onChange: () => void) {
    window.addEventListener(DRAFT_EVENT, onChange);
    window.addEventListener('storage', onChange);
    return () => {
        window.removeEventListener(DRAFT_EVENT, onChange);
        window.removeEventListener('storage', onChange);
    };
}

export function readChatDraft(scope: ChatDraftScope, id: string | number): string {
    try {
        return localStorage.getItem(keyOf(scope, id)) || '';
    } catch {
        return '';
    }
}

export function writeChatDraft(scope: ChatDraftScope, id: string | number, text: string) {
    try {
        const key = keyOf(scope, id);
        if (!text.trim()) localStorage.removeItem(key);
        else localStorage.setItem(key, text);
        notifyDraftsChanged();
    } catch {
        // quota / private mode
    }
}

export function clearChatDraft(scope: ChatDraftScope, id: string | number) {
    try {
        localStorage.removeItem(keyOf(scope, id));
        notifyDraftsChanged();
    } catch {
        // ignore
    }
}
