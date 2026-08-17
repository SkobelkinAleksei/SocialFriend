import React, { useState, useEffect } from 'react';
import api from '@/shared/lib/api';
import { theme } from '@/shared/ui/theme';
import { useAuth } from '@/shared/context/AuthContext';
import Card from '@/shared/ui/Card';
import Button from '@/shared/ui/Button';
import StatusMenuButton from '@/shared/ui/StatusMenuButton';
import { openNeighborProfile, getAvatarUrl } from '@/shared/utils/navigation';
import { fetchPresence, PRESENCE_INTERVAL_MS } from '@/shared/utils/presence';
import { showAppConfirm, showAppInfoToast } from '@/shared/utils/appToast';
import {
  Search,
  MapPin,
  UserPlus,
  MessageSquare,
  SlidersHorizontal,
  Settings,
  ChevronDown,
  ChevronUp,
  Users
} from 'lucide-react';

interface Neighbor {
  userId: number;
  id?: number;
  firstName: string;
  lastName: string;
  city: string;
  districtName: string;
  streetAddress?: string;
  isFriend: boolean;
  hasPendingRequest: boolean;
  isIncomingRequest: boolean;
  status?: string;
  mutualCount?: number;
  isOnline?: boolean;
  canMessage?: boolean;
  avatarUrl?: string | null;
}

interface FriendsProps {
  mode?: 'owner' | 'guest';
  targetUserId?: number | string | null;
}

const FRIEND_TABS = ['friends', 'requests', 'subscribers', 'search'] as const;
type FriendTab = typeof FRIEND_TABS[number];

function isFriendTab(value: string | null): value is FriendTab {
  return !!value && (FRIEND_TABS as readonly string[]).includes(value);
}

function readFriendsSearch(): URLSearchParams {
  return new URLSearchParams(window.location.search);
}

function writeFriendsQuery(params: Record<string, string>) {
  const url = new URL(window.location.href);
  const path = url.pathname.replace(/\/+$/, '') || '/';
  if (path !== '/friends') url.pathname = '/friends';
  url.search = '';
  Object.entries(params).forEach(([key, value]) => {
    if (value) url.searchParams.set(key, value);
  });
  window.history.replaceState({ ...(window.history.state || {}), page: 'friends' }, '', `${url.pathname}${url.search}`);
}

const getFriendsLabel = (count: number): string => {
  const remainder10 = count % 10;
  const remainder100 = count % 100;
  if (remainder10 === 1 && remainder100 !== 11) return `${count} общий друг`;
  if (remainder10 >= 2 && remainder10 <= 4 && (remainder100 < 10 || remainder100 >= 20)) return `${count} общих друга`;
  return `${count} общих друзей`;
};

export default function Friends({ mode = 'owner', targetUserId }: FriendsProps) {
  const { user } = useAuth();
  const isOwner = mode === 'owner';
  const neighborId = targetUserId ? Number(targetUserId) : 0;
  const initialQuery = readFriendsSearch();

  const [activeTab, setActiveTabInternal] = useState<FriendTab>(() => {
    if (mode !== 'owner') return 'friends';
    const fromUrl = initialQuery.get('tab');
    return isFriendTab(fromUrl) ? fromUrl : 'friends';
  });

// Кастомный сеттер, который при смене вкладки сразу пишет её в URL браузера
  const setActiveTab = (newTab: FriendTab) => {
    setActiveTabInternal(newTab);
  };

  const [requestsSubTab, setRequestsSubTab] = useState<'incoming' | 'outgoing'>(() => {
    if (mode !== 'owner') return 'incoming';
    const savedSubTab = localStorage.getItem('friendSubTab');
    return (savedSubTab as any) || 'incoming';
  });

  // ОДИН ОФИЦИАЛЬНЫЙ ЭФФЕКТ ДЛЯ МАРШРУТИЗАЦИИ УВЕДОМЛЕНИЙ
  useEffect(() => {
    if (!isOwner) return;

    const handleApplyNotificationRouting = () => {
      // Небольшой таймаут гарантирует, что React успеет обновить DOM дерева SPA
      setTimeout(() => {
        const savedTab = localStorage.getItem('friendActiveTab');
        const savedSubTab = localStorage.getItem('friendSubTab');

        if (savedTab) {
          setActiveTab(savedTab as any);
        }
        if (savedSubTab) {
          setRequestsSubTab(savedSubTab as any);
        }

        // Чистим хранилище строго после того, как стейты применились в интерфейсе
        if (savedTab || savedSubTab) {
          localStorage.removeItem('friendActiveTab');
          localStorage.removeItem('friendSubTab');
        }
      }, 20);
    };

    // Проверяем наличие маркеров при монтировании компонента
    handleApplyNotificationRouting();

    // Слушаем глобальную шину событий для динамического переключения табов
    window.addEventListener('changePage', handleApplyNotificationRouting);
    return () => {
      window.removeEventListener('changePage', handleApplyNotificationRouting);
    };
  }, [isOwner]);

  const [subscribersSubTab, setSubscribersSubTab] = useState<'incoming' | 'outgoing'>('incoming');

  const [showSearchFilters, setShowSearchFilters] = useState(() => {
    return !!(
        initialQuery.get('name') ||
        initialQuery.get('lastName') ||
        initialQuery.get('city') ||
        initialQuery.get('district') ||
        initialQuery.get('birthFrom') ||
        initialQuery.get('birthTo')
    );
  });
  const [filterName, setFilterName] = useState(() => initialQuery.get('name') || '');
  const [filterLastName, setFilterLastName] = useState(() => initialQuery.get('lastName') || '');
  const [filterCity, setFilterCity] = useState(() => initialQuery.get('city') || '');
  const [filterDistrict, setFilterDistrict] = useState(() => initialQuery.get('district') || '');
  const [filterBirthFrom, setFilterBirthFrom] = useState(() => initialQuery.get('birthFrom') || '');
  const [filterBirthTo, setFilterBirthTo] = useState(() => initialQuery.get('birthTo') || '');
  const [searchQuery, setSearchQuery] = useState(() => initialQuery.get('q') || '');
  const [globalSearchQuery, setGlobalSearchQuery] = useState(() => initialQuery.get('gq') || '');
  const [neighbors, setNeighbors] = useState<Neighbor[]>([]);
  const [loading, setLoading] = useState(false);
  const [isActionLocked, setIsActionLocked] = useState(false);

  // Бесконечная пагинация
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const PAGE_SIZE = 20;
  const hasPeopleSearch = Boolean(
    globalSearchQuery.trim() ||
    filterName.trim() ||
    filterLastName.trim() ||
    filterCity.trim() ||
    filterDistrict.trim() ||
    filterBirthFrom ||
    filterBirthTo
  );
  const [counts, setCounts] = useState({
    friends: 0,
    friendsOnline: 0,
    incomingRequests: 0,
    outgoingRequests: 0,
    incomingSubscribers: 0,
    outgoingSubscribers: 0
  });

  // Автоматически синхронизируем введенные фильтры с URL строкой браузера
  useEffect(() => {
    if (!isOwner) return;
    const newParams: Record<string, string> = { tab: activeTab };

    if (filterName) newParams.name = filterName;
    if (filterLastName) newParams.lastName = filterLastName;
    if (filterCity) newParams.city = filterCity;
    if (filterDistrict) newParams.district = filterDistrict;
    if (filterBirthFrom) newParams.birthFrom = filterBirthFrom;
    if (filterBirthTo) newParams.birthTo = filterBirthTo;
    if (searchQuery) newParams.q = searchQuery;
    if (globalSearchQuery) newParams.gq = globalSearchQuery;

    writeFriendsQuery(newParams);
  }, [isOwner, activeTab, filterName, filterLastName, filterCity, filterDistrict, filterBirthFrom, filterBirthTo, searchQuery, globalSearchQuery]);

  let outgoingRequests: any[] = [];
  let incomingRequests: any[] = [];

  const handleWriteMessage = async (friendId: number) => {
    if (!friendId) return;
    try {
      await api.post(`/api/v1/social/chats/personal/init/${friendId}`);
      localStorage.setItem('openDirectChatWith', friendId.toString());
      window.dispatchEvent(new CustomEvent('changePage', { detail: 'chats' }));
    } catch (error) {
      console.error("Не удалось открыть чат с другом:", error);
    }
  };

  useEffect(() => {
    setPage(0);
    setNeighbors([]);
  }, [
    activeTab,
    requestsSubTab,
    subscribersSubTab,
    neighborId,
    globalSearchQuery,
    filterName,
    filterLastName,
    filterCity,
    filterDistrict,
    filterBirthFrom,
    filterBirthTo
  ]);

  useEffect(() => {
    loadNeighbors(page);
  }, [
    activeTab,
    requestsSubTab,
    subscribersSubTab,
    page,
    user,
    neighborId,
    // ПРОДАКШН-ФИКС: Запускаем повторный запрос к API, когда пользователь пишет или стирает текст
    globalSearchQuery,
    filterName,
    filterLastName,
    filterCity,
    filterDistrict,
    filterBirthFrom,
    filterBirthTo
  ]);

  const neighborIdsKey = neighbors.map((n) => n.userId).join(',');

  useEffect(() => {
    if (!neighborIdsKey) return;
    let cancelled = false;
    const ids = neighborIdsKey.split(',').filter(Boolean);
    const tick = async () => {
      try {
        const map = await fetchPresence(ids);
        if (cancelled) return;
        setNeighbors((prev) => prev.map((n) => ({
          ...n,
          isOnline: Number(n.userId) === Number(user?.id) ? true : (map[String(n.userId)]?.online ?? false),
        })));
        if (!isOwner || activeTab === 'friends') {
          const onlineCount = ids.filter((id) =>
            Number(id) === Number(user?.id) || map[String(id)]?.online
          ).length;
          setCounts((c) => ({ ...c, friendsOnline: onlineCount }));
        }
      } catch {
        // статус подтянется на следующем тике
      }
    };
    tick();
    const intervalId = window.setInterval(tick, PRESENCE_INTERVAL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [neighborIdsKey, user?.id, activeTab, isOwner]);

  const loadNeighbors = async (currentPage: number) => {
    if (!user) return;
    setLoading(true);

    const myId = user.id;
    let outgoingRequests: any[] = [];
    let incomingRequests: any[] = [];
    let outgoingSubscribers: any[] = [];
    let myFriendsIds: number[] = [];
    let currentFriends: any[] = [];

    const asCount = (res: PromiseSettledResult<{ data?: unknown }>) =>
      res.status === 'fulfilled' ? Number(res.value.data || 0) : 0;
    const asIds = (res: PromiseSettledResult<{ data?: unknown }>) =>
      res.status === 'fulfilled' && Array.isArray(res.value.data)
        ? res.value.data.map((id: unknown) => Number(id)).filter((id: number) => id > 0)
        : [];

    if (currentPage === 0) {
      try {
        if (isOwner) {
          const [friendsCountRes, idsRes, incReqCountRes, outReqCountRes, incSubCountRes, outSubCountRes] = await Promise.allSettled([
            api.get(`/api/v1/social/friends/public/${myId}/count`),
            api.get(`/api/v1/social/friends/public/${myId}/ids`),
            api.get('/api/v1/social/friends/requests/count/incoming', { params: { status: 'PENDING' } }),
            api.get('/api/v1/social/friends/requests/count/outgoing', { params: { status: 'PENDING' } }),
            api.get('/api/v1/social/friends/requests/count/incoming', { params: { status: 'REJECTED' } }),
            api.get('/api/v1/social/friends/requests/count/outgoing', { params: { status: 'REJECTED' } })
          ]);

          myFriendsIds = asIds(idsRes);
          currentFriends = myFriendsIds.map((id) => ({ userId1: myId, userId2: id }));

          setCounts({
            friends: asCount(friendsCountRes),
            friendsOnline: 0,
            incomingRequests: asCount(incReqCountRes),
            outgoingRequests: asCount(outReqCountRes),
            incomingSubscribers: asCount(incSubCountRes),
            outgoingSubscribers: asCount(outSubCountRes)
          });

          if (activeTab === 'search' && hasPeopleSearch) {
            const [incReqRes, outReqRes, outSubRes] = await Promise.allSettled([
              api.get('/api/v1/social/friends/requests/incoming', { params: { status: 'PENDING', page: 0, size: PAGE_SIZE } }),
              api.get('/api/v1/social/friends/requests/outgoing', { params: { status: 'PENDING', page: 0, size: PAGE_SIZE } }),
              api.get('/api/v1/social/friends/requests/outgoing', { params: { status: 'REJECTED', page: 0, size: PAGE_SIZE } })
            ]);
            incomingRequests = incReqRes.status === 'fulfilled' ? (incReqRes.value.data || []) : [];
            outgoingRequests = outReqRes.status === 'fulfilled' ? (outReqRes.value.data || []) : [];
            outgoingSubscribers = outSubRes.status === 'fulfilled' ? (outSubRes.value.data || []) : [];
          }
        } else {
          const [friendsCountRes, idsRes] = await Promise.allSettled([
            api.get(`/api/v1/social/friends/public/${neighborId}/count`),
            api.get(`/api/v1/social/friends/public/${myId}/ids`)
          ]);
          myFriendsIds = asIds(idsRes);
          currentFriends = myFriendsIds.map((id) => ({ userId1: myId, userId2: id }));
          setCounts(prev => ({
            ...prev,
            friends: asCount(friendsCountRes)
          }));
          const [incReqRes, outReqRes] = await Promise.allSettled([
            api.get('/api/v1/social/friends/requests/incoming', { params: { status: 'PENDING', page: 0, size: PAGE_SIZE } }),
            api.get('/api/v1/social/friends/requests/outgoing', { params: { status: 'PENDING', page: 0, size: PAGE_SIZE } })
          ]);
          incomingRequests = incReqRes.status === 'fulfilled' ? (incReqRes.value.data || []) : [];
          outgoingRequests = outReqRes.status === 'fulfilled' ? (outReqRes.value.data || []) : [];
        }
      } catch (e) {
        console.error("Ошибка обновления счетчиков:", e);
      }
    }

    if (myFriendsIds.length === 0 && currentPage > 0) {
      try {
        const idsRes = await api.get(`/api/v1/social/friends/public/${myId}/ids`);
        myFriendsIds = Array.isArray(idsRes.data) ? idsRes.data.map((id: unknown) => Number(id)).filter((id: number) => id > 0) : [];
        currentFriends = myFriendsIds.map((id) => ({ userId1: myId, userId2: id }));
      } catch (e) {
        console.error("Ошибка получения друзей во втором блоке:", e);
      }
    }

    if (isOwner && activeTab === 'search' && !hasPeopleSearch) {
      setNeighbors([]);
      setHasMore(false);
      setLoading(false);
      return;
    }

    // 2. ОПРЕДЕЛЕНИЕ ЦЕЛЕВОГО ЭНДПОИНТА НА ОСНОВЕ АКТИВНОГО ТАБА
    let endpoint = '';
    let params: any = { page: currentPage, size: PAGE_SIZE };

    if (!isOwner) {
      endpoint = `/api/v1/social/friends/public/${neighborId}`;
    } else {
      switch (activeTab) {
        case 'friends':
          endpoint = `/api/v1/social/friends/public/${myId}`;
          break;
        case 'search':
          endpoint = '/api/v1/social/users/search';

          // 1. Если заполнена большая верхняя поисковая строка
          if (globalSearchQuery) {
            params.firstName = globalSearchQuery;
          }

          // 2. СИНХРОНИЗАЦИЯ С БЭКЕНДОМ: Передаем детальные фильтры из панели
          if (filterName) params.firstName = filterName; // Если ввели имя и там, и там — детальный фильтр в приоритете
          if (filterLastName) params.lastName = filterLastName;
          if (filterCity) params.city = filterCity;
          if (filterDistrict) params.districtName = filterDistrict;
          if (filterBirthFrom) params.birthdayFrom = filterBirthFrom;
          if (filterBirthTo) params.birthdayTo = filterBirthTo;
          break;
        case 'requests':
          endpoint = `/api/v1/social/friends/requests/${requestsSubTab === 'outgoing' ? 'outgoing' : 'incoming'}`;
          params.status = 'PENDING';
          break;
        case 'subscribers':
          endpoint = `/api/v1/social/friends/requests/${subscribersSubTab === 'outgoing' ? 'outgoing' : 'incoming'}`;
          params.status = 'REJECTED';
          break;
      }
    }

    // 3. ВЫПОЛНЕНИЕ ОСНОВНОГО ЗАПРОСА ДАННЫХ
    try {
      const response = await api.get(endpoint, { params });
      const rawData = response.data || [];
      setHasMore(rawData.length === PAGE_SIZE);

      let profilesMap: Record<number, any> = {};
      let neighborFriendsMap: Record<number, number[]> = {};
      if (!isOwner || activeTab === 'requests' || activeTab === 'friends' || activeTab === 'subscribers') {
        const targetIds: number[] = rawData.map((item: any) => {
          if (!isOwner || activeTab === 'friends') return item.userId1 === (isOwner ? myId : neighborId) ? item.userId2 : item.userId1;
          if (activeTab === 'requests') return requestsSubTab === 'incoming' ? item.requesterId : item.addresseeId;
          return subscribersSubTab === 'incoming' ? item.requesterId : item.addresseeId;
        });
        const cleanTargetIds = Array.from(new Set(targetIds))
            .map(id => Number(id))
            .filter(id => id > 0 && id !== Number(myId));
        if (cleanTargetIds.length > 0) {
          const [profileResponses, friendsResponses] = await Promise.all([
            Promise.all(cleanTargetIds.map(id => api.get(`/api/v1/social/users/${id}/profile`).catch(() => null))),
            Promise.all(cleanTargetIds.map(id => api.get(`/api/v1/social/friends/public/${id}/ids`).catch(() => null)))
          ]);
          profileResponses.forEach((res, index) => {
            if (res && res.data) profilesMap[cleanTargetIds[index]] = res.data;
          });
          friendsResponses.forEach((res, index) => {
            if (res && res.data) {
              const targetId = cleanTargetIds[index];
              neighborFriendsMap[targetId] = (res.data || []).map((id: unknown) => Number(id)).filter((id: number) => id > 0);
            }
          });
        }
      }

      if (isOwner && activeTab === 'search') {
        try {
          if (rawData.length > 0) {
            const searchFriendsResponses = await Promise.all(
                rawData.map((item: any) => api.get(`/api/v1/social/friends/public/${item.userId}/ids`).catch(() => null))
            );
            searchFriendsResponses.forEach((res, index) => {
              if (res && res.data) {
                const targetId = rawData[index].userId;
                neighborFriendsMap[targetId] = (res.data || []).map((id: unknown) => Number(id)).filter((id: number) => id > 0);
              }
            });
          }
        } catch (e) {
          console.error("Ошибка мета-данных в поиске:", e);
        }
      }

      const mappedData: Neighbor[] = rawData.map((item: any) => {
        if (isOwner && (activeTab === 'requests' || activeTab === 'subscribers')) {
          const isIncoming = activeTab === 'requests' ? requestsSubTab === 'incoming' : subscribersSubTab === 'incoming';
          const targetId = isIncoming ? item.requesterId : item.addresseeId;
          const profile = profilesMap[targetId];
          const neighborFriendsIds = neighborFriendsMap[targetId] || [];
          const mutualFriendsCount = neighborFriendsIds.filter(id => myFriendsIds.includes(id)).length;
          return {
            id: item.id,
            userId: targetId,
            firstName: profile ? profile.firstName : 'Загрузка...',
            lastName: profile ? profile.lastName : '',
            city: profile ? profile.city : user.city,
            districtName: profile ? profile.districtName : user.districtName,
            isFriend: false,
            hasPendingRequest: activeTab === 'requests' && requestsSubTab === 'outgoing',
            isIncomingRequest: activeTab === 'requests' && requestsSubTab === 'incoming',
            status: item.status,
            mutualCount: mutualFriendsCount,
            canMessage: profile ? profile.canMessage : true,
            avatarUrl: profile?.avatarUrl || null,
            isOnline: profile?.online || false,
          };
        } else if (!isOwner || activeTab === 'friends') {
          const friendId = item.userId1 === (isOwner ? user?.id : neighborId) ? item.userId2 : item.userId1;
          const profile = profilesMap[friendId];
          const neighborFriendsIds = neighborFriendsMap[friendId] || [];
          const mutualFriendsCount = neighborFriendsIds.filter(id => myFriendsIds.includes(id)).length;
          const isFriendOfCurrentUser = currentFriends.some((f: any) => f.userId1 === friendId || f.userId2 === friendId);
          const hasPending = outgoingRequests.some((r: any) => r.addresseeId === friendId);
          const isIncoming = incomingRequests.some((r: any) => r.requesterId === friendId);

          const isCurrentLoggedUser = user?.id && Number(friendId) === Number(user.id);
          return {
            userId: friendId,
            // Если это я — берем имя из useAuth context, иначе — из карты профилей бэкенда
            firstName: isCurrentLoggedUser ? (user.firstName || 'Я') : (profile?.firstName || `Сосед #${friendId}`),
            lastName: isCurrentLoggedUser ? (user.lastName || '') : (profile?.lastName || ''),
            city: isCurrentLoggedUser ? (user.city || 'На районе') : (profile?.city || user?.city || 'На районе'),
            districtName: isCurrentLoggedUser ? (user.districtName || '') : (profile?.districtName || user?.districtName || ''),
            isFriend: isFriendOfCurrentUser,
            hasPendingRequest: hasPending,
            isIncomingRequest: isIncoming,
            isOnline: isCurrentLoggedUser ? true : (profile?.online || false),
            mutualCount: mutualFriendsCount,
            canMessage: profile ? profile.canMessage : true,
            avatarUrl: isCurrentLoggedUser ? (user.avatarUrl || null) : (profile?.avatarUrl || null),
          };
        }
        else {
          const targetUserId = item.userId;
          const isFriend = currentFriends.some((f: any) => f.userId1 === targetUserId || f.userId2 === targetUserId);
          const foundOutgoing = outgoingRequests.find((r: any) => r.addresseeId === targetUserId);
          const foundIncoming = incomingRequests.find((r: any) => r.requesterId === targetUserId);

          // Добавляем поиск среди отклонённых (исходящих подписок):
          const foundRejectedOutgoing = outgoingSubscribers.find((r: any) => r.addresseeId === targetUserId);

          const neighborFriendsIds = neighborFriendsMap[targetUserId] || [];
          const mutualFriendsCount = neighborFriendsIds.filter(id => myFriendsIds.includes(id)).length;

          let backendCanMessage = true;
          if (item.canMessage === false) { backendCanMessage = false; }

          return {
            userId: targetUserId,
            // Если статус REJECTED, берем id из отклоненной заявки, чтобы кнопка "Отписаться" работала корректно
            id: foundIncoming ? foundIncoming.id : (foundOutgoing ? foundOutgoing.id : (foundRejectedOutgoing ? foundRejectedOutgoing.id : undefined)),
            firstName: item.firstName,
            lastName: item.lastName,
            city: item.city,
            districtName: item.districtName,
            isFriend: isFriend,
            hasPendingRequest: !!foundOutgoing,
            isIncomingRequest: !!foundIncoming && foundIncoming.status === 'PENDING',
            // КРИТИЧЕСКИЙ ВПРЫСК: Если есть отклоненная заявка, жестко проставляем статус REJECTED
            status: foundRejectedOutgoing ? 'REJECTED' : (foundOutgoing ? foundOutgoing.status : (foundIncoming ? foundIncoming.status : undefined)),
            mutualCount: mutualFriendsCount,
            canMessage: isFriend ? true : (item.canMessage ?? true),
            avatarUrl: item.avatarUrl || null,
            isOnline: item.online || false,
          };
        }
      });

      const finalData = isOwner && activeTab === 'subscribers'
          ? mappedData.filter(n => n.userId !== myId && !myFriendsIds.includes(n.userId))
          : (isOwner && activeTab === 'search' ? mappedData.filter(n => n.userId !== myId) : mappedData);

      if (currentPage === 0 && (!isOwner || activeTab === 'friends')) {
        setCounts(prev => ({
          ...prev,
          friendsOnline: finalData.filter(n => n.isOnline).length
        }));
      }

      if (currentPage === 0) {
        setNeighbors(finalData);
      } else {
        setNeighbors(prev => [...prev, ...finalData]);
      }
    } catch (error) {
      console.error("Ошибка загрузки:", error);
      if (currentPage === 0) setNeighbors([]);
    } finally { setLoading(false); }
  };

  const handleAddFriend = async (targetId: number) => {
    if (!targetId || isActionLocked) return; // Если замок активен — игнорируем клик

    setIsActionLocked(true); // Закрываем замок
    // Сразу ставим статус "Заявка отправлена" в интерфейсе
    setNeighbors(neighbors.map(n => n.userId === targetId ? { ...n, hasPendingRequest: true, status: undefined } : n));

    try {
      await api.post(`/api/v1/social/friends/requests/${targetId}`);
      setCounts(prev => ({ ...prev, outgoingRequests: prev.outgoingRequests + 1 }));
    } catch (error) {
      console.error("Ошибка отправки заявки:", error);
      setNeighbors(neighbors.map(n => n.userId === targetId ? { ...n, hasPendingRequest: false } : n));
    } finally {
      // Открываем замок только через 1.5 секунды
      setTimeout(() => setIsActionLocked(false), 1500);
    }
  };

  const handleProcessRequest = async (requestId: number, targetUserId: number, decision: 'ACCEPT' | 'REJECT') => {
    try {
      await api.put(`/api/v1/social/friends/requests/${requestId}`, null, {
        params: { status: decision === 'ACCEPT' ? 'ACCEPTED' : 'REJECTED' }
      });
      setNeighbors(neighbors.filter(n => n.userId !== targetUserId));
      setCounts(prev => ({
        ...prev,
        incomingRequests: activeTab === 'requests' && requestsSubTab === 'incoming' ? Math.max(0, prev.incomingRequests - 1) : prev.incomingRequests,
        incomingSubscribers: activeTab === 'requests' && decision === 'REJECT' ? prev.incomingSubscribers + 1 : prev.incomingSubscribers,
        friends: decision === 'ACCEPT' ? prev.friends + 1 : prev.friends
      }));
      window.dispatchEvent(new Event('friendRequestProcessed'));
    } catch (error) { console.error("Ошибка обработки заявки:", error); }
  };

  const handleRemoveFriend = async (targetUserId: number) => {
    if (isActionLocked) return; // Наш замок от спама

    // 1. ВОЗВРАЩАЕМ ПРОПАВШУЮ ПЕРЕМЕННУЮ (вычисляем до отправки запроса)
    const isGuestCancelingRequest = !isOwner && neighbors.find(n => n.userId === targetUserId)?.hasPendingRequest;

    setIsActionLocked(true); // Закрываем замок

    try {
      // 2. Ниже идет ваше стандартное условие, где она используется
      if (activeTab === 'requests' || activeTab === 'subscribers' || activeTab === 'search' || isGuestCancelingRequest) {
        await api.delete('/api/v1/social/friends/requests', { params: { addresseeId: targetUserId } });

        if (activeTab === 'search') {
          setNeighbors(neighbors.map(n => n.userId === targetUserId ? { ...n, hasPendingRequest: false, status: undefined } : n));
        } else {
          setNeighbors(neighbors.filter(n => n.userId !== targetUserId));
        }

        // КРИТИЧЕСКИЙ ВПРЫСК: Синхронизируем сайдбар, заставляя его мгновенно запросить свежие цифры с сервера
        window.dispatchEvent(new Event('friendRequestProcessed'));
        // Корректируем счетчики во вкладках
        if (activeTab === 'requests' && requestsSubTab === 'outgoing') {
          setCounts(prev => ({ ...prev, outgoingRequests: Math.max(0, prev.outgoingRequests - 1) }));
        } else if (activeTab === 'subscribers' && subscribersSubTab === 'outgoing') {
          setCounts(prev => ({ ...prev, outgoingSubscribers: Math.max(0, prev.outgoingSubscribers - 1) }));
        }
      } else {
        await api.delete('/api/v1/social/friends/private', { params: { userId2: targetUserId } });
        setNeighbors(neighbors.filter(n => n.userId !== targetUserId));
        setCounts(prev => ({ ...prev, friends: Math.max(0, prev.friends - 1) }));
      }
    } catch (error) {
      console.error("Ошибка при удалении:", error);
    } finally {
      // Снимаем блокировку через 1.5 секунды
      setTimeout(() => setIsActionLocked(false), 1500);
    }
  };

  const handleBlock = async (targetUserId: number) => {
    const confirmed = await showAppConfirm({
      title: 'Заблокировать соседа',
      message: 'Он не сможет писать вам, не будет виден в поиске, заявка в друзья не пройдёт. Это не бан на весь район — только между вами.',
      confirmText: 'Заблокировать',
      cancelText: 'Отмена',
      danger: true,
    });
    if (!confirmed) return;
    try {
      await api.post(`/api/v1/social/friends/blocks/${targetUserId}`);
      setNeighbors((prev) => prev.filter((n) => n.userId !== targetUserId));
    } catch (error) {
      console.error('Не удалось заблокировать соседа:', error);
      showAppInfoToast('Не удалось заблокировать', 'Сервис друзей не ответил. Обновите страницу и попробуйте снова.');
    }
  };

  const handleResetFilters = () => {
    setFilterName(''); setFilterLastName(''); setFilterCity(''); setFilterDistrict('');
    setFilterBirthFrom(''); setFilterBirthTo(''); setSearchQuery(''); setGlobalSearchQuery('');
  };

  const filteredNeighbors = neighbors.filter(n => {
    const currentQuery = activeTab === 'search' ? globalSearchQuery : searchQuery;
    const matchesSearch = `${n.firstName} ${n.lastName}`.toLowerCase().includes(currentQuery.toLowerCase());
    if (activeTab !== 'search') {
      return matchesSearch;
    }

    const matchesName = filterName ? n.firstName.toLowerCase().includes(filterName.toLowerCase()) : true;
    const matchesLastName = filterLastName ? n.lastName.toLowerCase().includes(filterLastName.toLowerCase()) : true;
    const matchesCity = filterCity ? n.city.toLowerCase().includes(filterCity.toLowerCase()) : true;
    const matchesDistrict = filterDistrict ? (n.districtName || '').toLowerCase().includes(filterDistrict.toLowerCase()) : true;

    return matchesSearch && matchesName && matchesLastName && matchesCity && matchesDistrict;
  });

  return (
      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8 min-h-screen">
        <div className="mb-8">
          <p className="text-[11px] uppercase tracking-[0.22em] text-[#8A8494] mb-2">Соседи · связи</p>
          <h1 className="myraion-display text-[32px] sm:text-[44px] md:text-[52px] leading-[0.92] text-[#1C1824]">
            {isOwner ? 'Друзья' : `Друзья ${neighbors.find(n => n.userId !== user?.id)?.firstName === 'Иван' ? 'Алексея' : 'соседа'}`}
          </h1>
        </div>
        {isOwner ? (
            <div className="flex flex-nowrap md:flex-wrap gap-1.5 md:gap-2 mb-7">
              <button onClick={() => setActiveTab('friends')} className={`flex-1 md:flex-none min-w-0 inline-flex items-center justify-center gap-1 md:gap-2 px-2 md:px-4 py-2 md:py-2.5 rounded-full text-[11px] md:text-sm transition ${activeTab === 'friends' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                <span className="truncate">Друзья</span> <span className={`text-[13px] md:text-[15px] font-bold tabular-nums ${activeTab === 'friends' ? 'text-[#EDE6F5]' : 'text-[#5C4B7A]'}`}>{counts.friends}</span>
              </button>
              <button onClick={() => setActiveTab('requests')} className={`flex-1 md:flex-none min-w-0 inline-flex items-center justify-center gap-1 md:gap-2 px-2 md:px-4 py-2 md:py-2.5 rounded-full text-[11px] md:text-sm transition ${activeTab === 'requests' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                <span className="truncate">Заявки</span> <span className={`text-[13px] md:text-[15px] font-bold tabular-nums ${activeTab === 'requests' ? 'text-[#EDE6F5]' : 'text-[#B85C5C]'}`}>{counts.incomingRequests}</span>
              </button>
              <button onClick={() => setActiveTab('subscribers')} className={`flex-1 md:flex-none min-w-0 px-2 md:px-4 py-2 md:py-2.5 rounded-full text-[11px] md:text-sm text-center truncate transition ${activeTab === 'subscribers' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                Подписки
              </button>
              <button onClick={() => setActiveTab('search')} className={`flex-1 md:flex-none min-w-0 px-2 md:px-4 py-2 md:py-2.5 rounded-full text-[11px] md:text-sm text-center truncate transition ${activeTab === 'search' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                <span className="md:hidden">Поиск</span><span className="hidden md:inline">Поиск людей</span>
              </button>
            </div>
        ) : (
            <p className="text-sm text-[#8A8494] -mt-4 mb-7">{counts.friends} в списке</p>
        )}

        {/* Подтабы ЗАЯВКИ */}
        {isOwner && activeTab === 'requests' && (
            <div className="flex flex-wrap gap-2 mb-7">
              <button onClick={() => setRequestsSubTab('incoming')} className={`px-4 py-2 rounded-full text-xs transition ${requestsSubTab === 'incoming' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                Входящие <span className={`ml-1.5 font-bold tabular-nums ${requestsSubTab === 'incoming' ? 'text-[#EDE6F5]' : 'text-[#5C4B7A]'}`}>{counts.incomingRequests}</span>
              </button>
              <button onClick={() => setRequestsSubTab('outgoing')} className={`px-4 py-2 rounded-full text-xs transition ${requestsSubTab === 'outgoing' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                Исходящие <span className={`ml-1.5 font-bold tabular-nums ${requestsSubTab === 'outgoing' ? 'text-[#EDE6F5]' : 'text-[#8A8494]'}`}>{counts.outgoingRequests}</span>
              </button>
            </div>
        )}

        {/* Подтабы ПОДПИСКИ */}
        {isOwner && activeTab === 'subscribers' && (
            <div className="flex flex-wrap gap-2 mb-7">
              <button onClick={() => setSubscribersSubTab('incoming')} className={`px-4 py-2 rounded-full text-xs transition ${subscribersSubTab === 'incoming' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                Входящие <span className={`ml-1.5 font-bold tabular-nums ${subscribersSubTab === 'incoming' ? 'text-[#EDE6F5]' : 'text-[#5C4B7A]'}`}>{counts.incomingSubscribers}</span>
              </button>
              <button onClick={() => setSubscribersSubTab('outgoing')} className={`px-4 py-2 rounded-full text-xs transition ${subscribersSubTab === 'outgoing' ? 'bg-[#1C1824] text-[#FFFCFA]' : 'bg-[#FFFCFA] text-[#1C1824] hover:bg-white'}`}>
                Исходящие <span className={`ml-1.5 font-bold tabular-nums ${subscribersSubTab === 'outgoing' ? 'text-[#EDE6F5]' : 'text-[#8A8494]'}`}>{counts.outgoingSubscribers}</span>
              </button>
            </div>
        )}
        {activeTab === 'friends' && (
            <div className="bg-[#FFFCFA] rounded-[32px] p-4 mb-6">
              <div className="relative">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input type="text" placeholder="Поиск среди друзей..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/20 focus:border-[#5C4B7A]/40 transition" />
              </div>
            </div>
        )}

        {/* Панель глобального поиска людей */}
        {isOwner && activeTab === 'search' && (
            <div className="bg-[#FFFCFA] rounded-[32px] p-4 mb-6">
              <div className="flex gap-2 md:gap-3 items-center">
                <div className="relative flex-1 min-w-0">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input type="text" placeholder="Поиск людей в районе..." value={globalSearchQuery} onChange={(e) => setGlobalSearchQuery(e.target.value)} className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[#5C4B7A]/20 focus:border-[#5C4B7A]/40 transition" />
                </div>
                <button
                  type="button"
                  onClick={() => setShowSearchFilters(!showSearchFilters)}
                  className={`shrink-0 inline-flex items-center justify-center gap-2 w-11 h-11 md:w-auto md:h-auto md:px-4 md:py-2.5 border rounded-xl text-sm font-medium transition ${
                    showSearchFilters ? 'bg-[#EDE6F5] border-[#5C4B7A]/30 text-[#5C4B7A]' : 'border-slate-200 text-slate-700 hover:bg-slate-50'
                  }`}
                  aria-label="Фильтры"
                  title="Фильтры"
                >
                  <Settings className="w-5 h-5 md:hidden" />
                  <SlidersHorizontal className="w-4 h-4 hidden md:block" />
                  <span className="hidden md:inline">Фильтры</span>
                  {showSearchFilters ? <ChevronUp className="w-4 h-4 hidden md:block" /> : <ChevronDown className="w-4 h-4 hidden md:block" />}
                </button>
              </div>
              {showSearchFilters && (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-4 pt-4 border-t border-slate-100 animate-fadeIn">
                    <div><label className="block text-xs font-semibold text-slate-500 mb-1">Имя</label><input type="text" placeholder="Алексей" value={filterName} onChange={e => setFilterName(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-[#5C4B7A]/40" /></div>
                    <div><label className="block text-xs font-semibold text-slate-500 mb-1">Фамилия</label><input type="text" placeholder="Иванов" value={filterLastName} onChange={e => setFilterLastName(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-[#5C4B7A]/40" /></div>
                    <div><label className="block text-xs font-semibold text-slate-500 mb-1">Город</label><input type="text" placeholder="Санкт-Петербург" value={filterCity} onChange={e => setFilterCity(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-[#5C4B7A]/40" /></div>
                    <div><label className="block text-xs font-semibold text-slate-500 mb-1">Район</label><input type="text" placeholder="Центральный" value={filterDistrict} onChange={e => setFilterDistrict(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-[#5C4B7A]/40" /></div>
                    <div><label className="block text-xs font-semibold text-slate-500 mb-1">Дата рождения от</label><input type="date" value={filterBirthFrom} onChange={e => setFilterBirthFrom(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-[#5C4B7A]/40" /></div>
                    <div><label className="block text-xs font-semibold text-slate-500 mb-1">Дата рождения до</label><input type="date" value={filterBirthTo} onChange={e => setFilterBirthTo(e.target.value)} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-[#5C4B7A]/40" /></div>
                    <div className="flex items-end lg:col-span-2 justify-between pt-2"><button onClick={handleResetFilters} className="text-xs font-medium text-slate-400 hover:text-slate-600 transition">Сбросить всё</button></div>
                  </div>
              )}
            </div>
        )}

        {loading && filteredNeighbors.length === 0 ? (
            <div className="flex justify-center py-16"><div className="animate-spin rounded-full h-7 w-7 border-b-2 border-[#5C4B7A]"></div></div>
        ) : (
            <div>
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {filteredNeighbors.map((neighbor) => (
                    <Card key={neighbor.userId} padded={false} className="p-3.5 md:p-4 hover:shadow-md transition duration-200 flex flex-col justify-between h-full relative">
                      <div>
                        <div className="flex gap-2.5 items-start mb-2">
                          <button type="button" onClick={() => openNeighborProfile(neighbor.userId)} className="relative shrink-0 cursor-pointer focus:outline-none text-left block" title="Открыть профиль соседа">
                            <img src={getAvatarUrl(neighbor.userId, neighbor.avatarUrl)} alt="" className="w-11 h-11 md:w-12 md:h-12 rounded-full object-cover border border-slate-100 shadow-sm" />
                            <span className={`absolute bottom-0 right-0 w-3.5 h-3.5 border-2 border-white rounded-full ${neighbor.isOnline ? 'bg-[#4A8B6F]' : 'bg-slate-300'}`}></span>
                          </button>
                          <div className="min-w-0">
                            <button type="button" onClick={() => openNeighborProfile(neighbor.userId)} className="font-bold text-sm md:text-[15px] text-slate-900 truncate leading-snug text-left block w-full focus:outline-none cursor-pointer">
                              {neighbor.firstName} {neighbor.lastName}
                            </button>
                            <div className="flex items-center gap-1 text-xs text-slate-400 mt-0.5 font-medium">
                              <MapPin className="w-3.5 h-3.5 shrink-0 text-slate-400" />
                              <span className="truncate">{neighbor.city}, {neighbor.districtName}</span>
                            </div>
                            {neighbor.mutualCount !== undefined && neighbor.mutualCount > 0 ? (
                                <div className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-50 border border-slate-100 rounded-md text-[11px] font-semibold text-slate-500 mt-2">
                                  <Users className="w-3 h-3 text-slate-400" /><span>{getFriendsLabel(neighbor.mutualCount)}</span>
                                </div>
                            ) : <div className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-50 border border-slate-100 rounded-md text-[11px] font-semibold text-slate-400 mt-2"><Users className="w-3 h-3 text-slate-400" /><span>Нет общих друзей</span></div>}
                          </div>
                        </div>
                      </div>
                      {/* Кнопки действий на основе состояний */}
                      {isOwner ? (
                          <div className="mt-auto pt-3 border-t border-slate-50 min-h-[44px] flex items-center overflow-visible">
                            {activeTab === 'requests' && requestsSubTab === 'incoming' && (
                                <StatusMenuButton
                                    className="w-full"
                                    variant="accent"
                                    label="Заявка в друзья"
                                    items={[
                                      { label: 'Принять', onClick: () => handleProcessRequest(neighbor.id!, neighbor.userId, 'ACCEPT'), tone: 'success' },
                                      { label: 'Отклонить', onClick: () => handleProcessRequest(neighbor.id!, neighbor.userId, 'REJECT'), tone: 'danger' },
                                      { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                    ]}
                                />
                            )}
                            {activeTab === 'requests' && requestsSubTab === 'outgoing' && (
                                <StatusMenuButton
                                    className="w-full"
                                    variant="amber"
                                    label="Заявка отправлена"
                                    items={[
                                      { label: 'Отменить заявку', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' },
                                      { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                    ]}
                                />
                            )}
                            {activeTab === 'subscribers' && (
                                subscribersSubTab === 'incoming' ? (
                                    <StatusMenuButton
                                        className="w-full"
                                        variant="muted"
                                        label="Подписан на вас"
                                        items={[
                                          { label: 'Добавить в друзья', onClick: () => handleProcessRequest(neighbor.id!, neighbor.userId, 'ACCEPT'), tone: 'success' },
                                          { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                        ]}
                                    />
                                ) : (
                                    <StatusMenuButton
                                        className="w-full"
                                        variant="muted"
                                        label="Вы подписаны"
                                        items={[
                                          { label: 'Отписаться', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' },
                                          { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                        ]}
                                    />
                                )
                            )}
                            {activeTab !== 'requests' && activeTab !== 'subscribers' && (
                                <>
                                      {neighbor.isFriend ? (
                                      <div className="flex gap-2 w-full">
                                        {neighbor.canMessage !== false ? (
                                            <>
                                              <Button size="sm" onClick={() => handleWriteMessage(neighbor.userId)} className="flex-1 bg-[#5C4B7A] hover:bg-[#4A3C66] text-white font-bold h-9 text-xs rounded-xl shadow-sm flex items-center justify-center gap-1"><MessageSquare className="w-4 h-4" /> Написать</Button>
                                              <StatusMenuButton
                                                  className="flex-1 min-w-0"
                                                  variant="muted"
                                                  label="В друзьях"
                                                  items={[
                                                    { label: 'Удалить из друзей', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' },
                                                    { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                                  ]}
                                              />
                                            </>
                                        ) : (
                                            <StatusMenuButton
                                                className="w-full"
                                                variant="muted"
                                                label="В друзьях"
                                                items={[
                                                  { label: 'Удалить из друзей', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' },
                                                  { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                                ]}
                                            />
                                        )}
                                      </div>
                                  ) : neighbor.hasPendingRequest ? (
                                      <StatusMenuButton
                                          className="w-full"
                                          variant="amber"
                                          label="Заявка отправлена"
                                          items={[
                                            { label: 'Отменить заявку', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' },
                                            { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                          ]}
                                      />
                                  ) : neighbor.isIncomingRequest ? (
                                      <StatusMenuButton
                                          className="w-full"
                                          variant="accent"
                                          label="Заявка в друзья"
                                          items={[
                                            { label: 'Принять', onClick: () => handleProcessRequest(neighbor.id!, neighbor.userId, 'ACCEPT'), tone: 'success' },
                                            { label: 'Отклонить', onClick: () => handleProcessRequest(neighbor.id!, neighbor.userId, 'REJECT'), tone: 'danger' },
                                            { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                          ]}
                                      />
                                  ) : neighbor.status === 'REJECTED' ? (
                                      <StatusMenuButton
                                          className="w-full"
                                          variant="muted"
                                          label="Вы подписаны"
                                          items={[
                                            { label: 'Отписаться', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' },
                                            { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                          ]}
                                      />
                                  ) : (
                                      <div className="flex gap-2 w-full">
                                        {neighbor.canMessage !== false ? (
                                            <>
                                              <StatusMenuButton
                                                  className="flex-1 min-w-0"
                                                  variant="accent"
                                                  label="Добавить в друзья"
                                                  items={[
                                                    { label: 'Добавить в друзья', onClick: () => handleAddFriend(neighbor.userId) },
                                                    { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                                  ]}
                                              />
                                              <Button size="sm" variant="secondary" onClick={() => handleWriteMessage(neighbor.userId)} className="w-9 h-9 p-0 text-[#5C4B7A] border border-slate-200 hover:bg-[#EDE6F5] rounded-xl flex items-center justify-center shrink-0 transition" title="Написать сообщение соискателю"><MessageSquare className="w-4 h-4" /></Button>
                                            </>
                                        ) : (
                                            <StatusMenuButton
                                                className="w-full"
                                                variant="accent"
                                                label="Добавить в друзья"
                                                items={[
                                                  { label: 'Добавить в друзья', onClick: () => handleAddFriend(neighbor.userId) },
                                                  { label: 'Заблокировать', onClick: () => handleBlock(neighbor.userId), tone: 'danger' },
                                                ]}
                                            />
                                        )}
                                      </div>
                                  )}
                                </>
                            )}
                          </div>
                      ) : (
                          /* ВЕТКА ДЛЯ ГОСТЯ */
                          <div className="mt-auto pt-3 border-t border-slate-50 min-h-[44px] flex items-center w-full overflow-visible">
                            {neighbor.userId === user?.id ? (
                                <span className="text-[11px] text-slate-400 font-bold bg-slate-100 px-2.5 py-1 rounded-lg select-none italic w-full text-center">Это вы</span>
                            ) : neighbor.isFriend ? (
                                <Button size="sm" onClick={() => handleWriteMessage(neighbor.userId)} className="w-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white font-bold h-9 text-xs rounded-xl shadow-sm flex items-center justify-center gap-1"><MessageSquare className="w-4 h-4" /> Написать</Button>
                            ) : neighbor.hasPendingRequest ? (
                                <StatusMenuButton
                                    className="w-full"
                                    variant="amber"
                                    label="Заявка отправлена"
                                    items={[{ label: 'Отменить заявку', onClick: () => handleRemoveFriend(neighbor.userId), tone: 'danger' }]}
                                />
                            ) : (
                                <Button size="sm" onClick={() => handleAddFriend(neighbor.userId)} className="w-full bg-[#5C4B7A] hover:bg-[#4A3C66] text-white font-bold h-9 text-xs rounded-xl shadow-sm flex items-center justify-center gap-1"><UserPlus className="w-4 h-4" /> Добавить в друзья</Button>
                            )}
                          </div>
                      )}
                    </Card>
                ))}
              </div>
              {filteredNeighbors.length === 0 && (
                <div className="text-center py-16 bg-[#FFFCFA] rounded-[32px] text-[#8A8494] text-sm">
                  {isOwner && activeTab === 'search' && !hasPeopleSearch
                    ? 'Введите имя или откройте фильтры, чтобы найти соседей'
                    : 'Ничего не найдено в этом разделе'}
                </div>
              )}
              {hasMore && (
                  <div className="flex justify-center mt-8 pb-4">
                    <Button variant="secondary" onClick={() => setPage(prev => prev + 1)} disabled={loading} className="px-6 py-2.5 text-xs font-semibold text-slate-600 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition shadow-sm flex items-center gap-2">
                      {loading ? <><div className="animate-spin rounded-full h-3.5 w-3.5 border-b-2 border-slate-400"></div>Загружаем...</> : "Показать еще соседей"}
                    </Button>
                  </div>
              )}
            </div>
        )}
      </div>
  );
}
