import { useEffect, useRef } from 'react';
import { registerAppBackHandler } from '@/shared/utils/navigation';

/** Пока `active`, свайп вправо / системная «назад» вызывает `onBack`. */
export function useAppBackHandler(active: boolean, onBack: () => void) {
  const onBackRef = useRef(onBack);
  onBackRef.current = onBack;

  useEffect(() => {
    if (!active) return;
    return registerAppBackHandler(() => {
      onBackRef.current();
      return true;
    });
  }, [active]);
}
