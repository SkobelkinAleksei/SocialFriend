import { useEffect } from 'react';
import { isCompactViewport } from '@/shared/utils/navigation';

export const CHAT_THREAD_OPEN_CLASS = 'myraion-chat-thread-open';

/** Держит открытый чат в видимой области, чтобы клавиатура не уезжала шапку с именем. */
export function useMobileChatViewport(active: boolean) {
    useEffect(() => {
        if (!active) return;
        const root = document.documentElement;
        const body = document.body;

        const apply = () => {
            if (!isCompactViewport()) {
                body.classList.remove(CHAT_THREAD_OPEN_CLASS);
                root.style.removeProperty('--myraion-chat-vv-height');
                root.style.removeProperty('--myraion-chat-vv-top');
                root.style.removeProperty('--myraion-chat-composer-pad');
                return;
            }
            body.classList.add(CHAT_THREAD_OPEN_CLASS);
            const vv = window.visualViewport;
            const height = Math.round(vv?.height ?? window.innerHeight);
            const offsetTop = Math.round(vv?.offsetTop ?? 0);
            const keyboardOpen = window.innerHeight - height - offsetTop > 80;
            root.style.setProperty('--myraion-chat-vv-height', `${height}px`);
            root.style.setProperty('--myraion-chat-vv-top', `${offsetTop}px`);
            root.style.setProperty(
                '--myraion-chat-composer-pad',
                keyboardOpen ? '0.75rem' : 'max(0.75rem, env(safe-area-inset-bottom))'
            );
        };

        apply();
        const vv = window.visualViewport;
        vv?.addEventListener('resize', apply);
        vv?.addEventListener('scroll', apply);
        window.addEventListener('resize', apply);
        window.addEventListener('orientationchange', apply);
        return () => {
            body.classList.remove(CHAT_THREAD_OPEN_CLASS);
            root.style.removeProperty('--myraion-chat-vv-height');
            root.style.removeProperty('--myraion-chat-vv-top');
            root.style.removeProperty('--myraion-chat-composer-pad');
            vv?.removeEventListener('resize', apply);
            vv?.removeEventListener('scroll', apply);
            window.removeEventListener('resize', apply);
            window.removeEventListener('orientationchange', apply);
        };
    }, [active]);
}
