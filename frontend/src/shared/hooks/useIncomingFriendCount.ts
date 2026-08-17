import { useCallback, useEffect, useState } from 'react';
import api from '@/shared/lib/api';

export function useIncomingFriendCount() {
  const [incomingCount, setIncomingCount] = useState(0);

  const refresh = useCallback(() => {
    api.get('/api/v1/social/friends/requests/incoming', { params: { status: 'PENDING' } })
      .then((res) => setIncomingCount((res.data || []).length))
      .catch(() => setIncomingCount(0));
  }, []);

  useEffect(() => {
    refresh();
    window.addEventListener('friendRequestProcessed', refresh);
    return () => window.removeEventListener('friendRequestProcessed', refresh);
  }, [refresh]);

  return incomingCount;
}
