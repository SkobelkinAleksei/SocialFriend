/** Красивое информационное окно по центру вместо window.alert */
export function showAppInfoToast(title: string, message: string): void {
    if (typeof window === 'undefined') return;
    window.dispatchEvent(new CustomEvent('appInfoToast', {
        detail: { title, message }
    }));
}

export type AppConfirmOptions = {
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    danger?: boolean;
};

/** Красивое окно подтверждения по центру вместо window.confirm */
export function showAppConfirm(options: AppConfirmOptions): Promise<boolean> {
    if (typeof window === 'undefined') return Promise.resolve(false);
    const requestId = `confirm-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    return new Promise((resolve) => {
        const onResult = (e: Event) => {
            const detail = (e as CustomEvent).detail || {};
            if (detail.requestId !== requestId) return;
            window.removeEventListener('appConfirmResult', onResult);
            resolve(Boolean(detail.confirmed));
        };
        window.addEventListener('appConfirmResult', onResult);
        window.dispatchEvent(new CustomEvent('appConfirm', {
            detail: { requestId, ...options }
        }));
    });
}
