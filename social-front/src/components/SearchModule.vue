<template>
  <div style="background: white; padding: 32px; border-radius: 16px; box-shadow: 0 10px 30px rgba(164,176,190,0.08); border: 1px solid #edf2f7; max-width: 760px; width: 100%; box-sizing: border-box; font-family: inherit;">

    <h3 style="margin-top: 0; color: #2c3e50; font-size: 20px; font-weight: 700; margin-bottom: 24px; display: flex; align-items: center; gap: 8px; text-align: left;">
      Поиск людей по параметрам <span style="font-size: 18px;">🔍</span>
    </h3>

    <!-- ОСНОВНЫЕ ПОЛЯ ПОИСКА (ИМЯ И ФАМИЛИЯ) -->
    <div style="display: flex; gap: 20px; margin-bottom: 20px; width: 100%; box-sizing: border-box;">
      <div style="flex: 1; display: flex; flex-direction: column; align-items: flex-start;">
        <label style="display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: #747d8c; text-align: left;">Имя</label>
        <input
            v-model="filter.firstName"
            type="text"
            placeholder="Например, Иван"
            style="width: 100%; padding: 12px 16px; border: 1px solid #ced6e0; border-radius: 10px; font-size: 14px; color: #2c3e50; outline: none; background: #ffffff; box-shadow: inset 0 1px 3px rgba(0,0,0,0.02); box-sizing: border-box;"
        >
      </div>
      <div style="flex: 1; display: flex; flex-direction: column; align-items: flex-start;">
        <label style="display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: #747d8c; text-align: left;">Фамилия</label>
        <input
            v-model="filter.lastName"
            type="text"
            placeholder="Например, Иванов"
            style="width: 100%; padding: 12px 16px; border: 1px solid #ced6e0; border-radius: 10px; font-size: 14px; color: #2c3e50; outline: none; background: #ffffff; box-shadow: inset 0 1px 3px rgba(0,0,0,0.02); box-sizing: border-box;"
        >
      </div>
    </div>

    <!-- ТУМБЛЕР ДЛЯ ДОПОЛНИТЕЛЬНЫХ ФИЛЬТРОВ -->
    <div style="margin-bottom: 24px; text-align: left;">
      <button
          @click="showAdvanced = !showAdvanced"
          style="background: transparent; border: none; color: #54a0ff; font-weight: 600; cursor: pointer; font-size: 13px; padding: 4px 0; display: inline-flex; align-items: center; gap: 6px;"
      >
        <span>{{ showAdvanced ? '▼ Скрыть дополнительные фильтры' : '► Показать дополнительные фильтры' }}</span>
      </button>
    </div>

    <!-- РАСШИРЕННЫЕ ФИЛЬТРЫ -->
    <div
        v-if="showAdvanced"
        style="background: #f1f5f9; padding: 24px; border-radius: 14px; margin-bottom: 24px; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 18px; box-sizing: border-box; width: 100%;"
    >
      <div style="text-align: left; display: flex; flex-direction: column; align-items: flex-start; width: 100%;">
        <label style="display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: #747d8c;">Номер телефона</label>
        <input
            v-model="filter.numberPhone"
            type="text"
            placeholder="+7 (999) 123-45-67"
            style="width: 100%; padding: 12px 16px; border: 1px solid #ced6e0; border-radius: 10px; font-size: 14px; color: #2c3e50; outline: none; background: #ffffff; box-sizing: border-box;"
        >
      </div>

      <div style="display: flex; gap: 20px; text-align: left; width: 100%; box-sizing: border-box;">
        <div style="flex: 1; display: flex; flex-direction: column; align-items: flex-start;">
          <label style="display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: #747d8c;">Дата рождения (От)</label>
          <input
              v-model="filter.birthdayFrom"
              type="date"
              style="width: 100%; padding: 11px 16px; border: 1px solid #ced6e0; border-radius: 10px; font-size: 14px; outline: none; background: #ffffff; color: #2c3e50; box-sizing: border-box;"
          >
        </div>
        <div style="flex: 1; display: flex; flex-direction: column; align-items: flex-start;">
          <label style="display: block; margin-bottom: 8px; font-size: 13px; font-weight: 600; color: #747d8c;">Дата рождения (До)</label>
          <input
              v-model="filter.birthdayTo"
              type="date"
              style="width: 100%; padding: 11px 16px; border: 1px solid #ced6e0; border-radius: 10px; font-size: 14px; outline: none; background: #ffffff; color: #2c3e50; box-sizing: border-box;"
          >
        </div>
      </div>
    </div>

    <!-- КНОПКА ЗАПУСКА ПОИСКА -->
    <button
        @click="handleSearch"
        style="width: 100%; padding: 14px; background: #54a0ff; color: white; border: none; border-radius: 10px; cursor: pointer; font-weight: 700; font-size: 15px; box-shadow: 0 4px 14px rgba(84,160,255,0.25); box-sizing: border-box;"
    >
      Найти пользователей
    </button>
    <!-- СПИСОК РЕЗУЛЬТАТОВ ПОИСКА -->
    <div v-if="searchResults.length > 0" style="margin-top: 32px; display: flex; flex-direction: column; gap: 16px; text-align: left; width: 100%; box-sizing: border-box;">

      <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
        <h4 style="margin: 0; color: #2c3e50; font-size: 16px; font-weight: 700;">Результаты поиска:</h4>
        <button
            @click="clearSearchHistory"
            style="background: transparent; border: none; color: #ff7675; font-size: 12px; font-weight: 600; cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: background 0.2s;"
            onmouseover="this.style.background='#fff5f5'"
            onmouseout="this.style.background='transparent'"
        >
          🗑️ Очистить результаты
        </button>
      </div>

      <div
          v-for="user in searchResults"
          :key="user.userId || user.id"
          style="padding: 20px 24px; background: #ffffff; border-radius: 14px; border: 1px solid #edf2f7; box-shadow: 0 4px 12px rgba(164,176,190,0.03); display: flex; justify-content: space-between; align-items: center; box-sizing: border-box; width: 100%; transition: transform 0.2s;"
          onmouseover="this.style.transform='translateY(-1px)'; this.style.boxShadow='0 6px 16px rgba(164,176,190,0.06)'"
          onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 4px 12px rgba(164,176,190,0.03)'"
      >
        <div style="text-align: left; display: flex; align-items: center;">
          <p
              @click="emit('open-user-profile', user.id || user.userId)"
              style="margin: 0; font-size: 16px; font-weight: 700; color: #2c3e50; cursor: pointer; display: inline-block; transition: all 0.15s ease;"
              onmouseover="this.style.color='#54a0ff'; this.style.textDecoration='underline'"
              onmouseout="this.style.color='#2c3e50'; this.style.textDecoration='none'"
              title="Открыть профиль пользователя"
          >
            {{ user.firstName }} {{ user.lastName }}
          </p>
        </div>

        <!-- УМНЫЙ БЛОК ДИНАМИЧЕСКИХ КНОПОК -->
        <!-- УМНЫЙ БЛОК ДИНАМИЧЕСКИХ КНОПОК (Выстроены ровно в один ряд) -->
        <div style="display: flex; gap: 12px; align-items: center; justify-content: flex-end; flex-direction: row; min-width: 382px; flex-shrink: 0; box-sizing: border-box;">

          <!-- ЛЕВЫЙ КОНТЕЙНЕР ДЛЯ КНОПОК ДРУЖБЫ -->
          <div style="flex-shrink: 0; width: 185px;">
            <!-- УСЛОВИЕ 1: Пользователи уже являются друзьями (Серый) -->
            <button
                v-if="myFriends.includes(Number(user.id || user.userId))"
                @click="handleRemoveFriend(user.id || user.userId)"
                style="font-size: 13px; background: #edf2f7; color: #4a5568; padding: 10px 16px; border-radius: 8px; font-weight: 600; display: inline-block; border: 1px solid #e2e8f0; cursor: pointer; transition: all 0.2s ease; width: 185px; text-align: center; white-space: nowrap; box-sizing: border-box;"
                onmouseover="this.style.background='#fff5f5'; this.style.color='#e53e3e'; this.style.borderColor='#fed7d7'; this.textContent='❌ Удалить из друзей'"
                onmouseout="this.style.background='#edf2f7'; this.style.color='#4a5568'; this.style.borderColor='#e2e8f0'; this.innerHTML='👤 Вы друзья'"
            >
              👤 Вы друзья
            </button>

            <!-- УСЛОВИЕ 2: Этот пользователь прислал запрос НАМ (Нежный лавандовый) -->
            <button
                v-else-if="incomingRequests.includes(Number(user.id || user.userId))"
                @click="handleAcceptFriendRequest(user)"
                style="font-size: 13px; background: #e0f2fe; color: #0369a1; padding: 10px 16px; border-radius: 8px; font-weight: 600; display: inline-block; border: 1px solid #bae6fd; cursor: pointer; transition: all 0.2s ease; width: 185px; text-align: center; white-space: nowrap; box-sizing: border-box;"
                onmouseover="this.style.background='#bae6fd'; this.style.borderColor='#7dd3fc'; this.textContent='✅ Подтвердить'"
                onmouseout="this.style.background='#e0f2fe'; this.style.borderColor='#bae6fd'; this.innerHTML='👤 Принять заявку'"
            >
              👤 Принять заявку
            </button>

            <!-- УСЛОВИЕ 3: Мы первыми отправили исходящий запрос (Желтый) -->
            <button
                v-else-if="sentRequests.includes(Number(user.id || user.userId))"
                @click="handleCancelRequest(user.id || user.userId)"
                style="font-size: 13px; background: #fff9db; color: #f59f00; padding: 10px 16px; border-radius: 8px; font-weight: 600; display: inline-block; border: 1px solid #ffe3e3; cursor: pointer; transition: all 0.2s ease; width: 185px; text-align: center; white-space: nowrap; box-sizing: border-box;"
                onmouseover="this.style.background='#fff5f5'; this.style.color='#ff7675'; this.style.borderColor='#fecaca'; this.textContent='❌ Отменить запрос'"
                onmouseout="this.style.background='#fff9db'; this.style.color='#f59f00'; this.style.borderColor='#ffe3e3'; this.innerHTML='⏳ Запрос отправлен'"
            >
              ⏳ Запрос отправлен
            </button>

            <!-- УСЛОВИЕ 4: Никаких связей нет — чистая кнопка добавления (Зеленый) -->
            <button
                v-else
                @click="sendFriendRequest(user.id || user.userId)"
                style="padding: 10px 16px; background: #2ecc71; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 13px; box-shadow: 0 4px 12px rgba(46,204,113,0.2); transition: all 0.2s; width: 185px; text-align: center; white-space: nowrap; box-sizing: border-box;"
                onmouseover="this.style.background='#27ae60'; this.style.boxShadow='0 6px 16px rgba(46,204,113,0.3)'"
                onmouseout="this.style.background='#2ecc71'; this.style.boxShadow='0 4px 12px rgba(46,204,113,0.2)'"
            >
              Добавить в друзья
            </button>
          </div>

          <!-- 💡 ПРАВАЯ КНОПКА: ВСЕГДА КЛИКАБЕЛЬНАЯ ПАСТЕЛЬНАЯ КНОПКА ОТПРАВКИ СООБЩЕНИЯ -->
          <button
              @click.stop="() => {
              const targetId = user.id ? user.id : user.userId;
              if (targetId) {
                emit('open-chat', Number(targetId));
              } else {
                alert('Ошибка: Не удалось определить ID пользователя');
              }
            }"
              title="Написать сообщение этому пользователю"
              style="font-size: 13px; background: #edf5ff; color: #54a0ff; padding: 10px 16px; border-radius: 8px; font-weight: 600; border: 1px solid #d0e6ff; cursor: pointer; transition: all 0.2s ease; width: 185px; text-align: center; white-space: nowrap; box-sizing: border-box; flex-shrink: 0;"
              onmouseover="this.style.background='#d0e6ff'"
              onmouseout="this.style.background='#edf5ff'"
          >
            💬 Написать
          </button>

        </div>
      </div>
    </div>

    <div v-else-if="searchExecuted" style="text-align: center; color: #a4b0be; padding: 40px 0; font-size: 14px; margin-top: 24px; background: #f8f9fa; border-radius: 12px; border: 1px dashed #ced6e0; box-sizing: border-box; width: 100%;">
      Пользователи с такими параметрами не найдены.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'

const emit = defineEmits(['open-user-profile', 'open-chat'])

const showAdvanced = ref(JSON.parse(sessionStorage.getItem('search_showAdvanced')) || false)
const searchExecuted = ref(JSON.parse(sessionStorage.getItem('search_searchExecuted')) || false)
const searchResults = ref(JSON.parse(sessionStorage.getItem('search_searchResults')) || [])

const sentRequests = ref([])
const incomingRequests = ref([])
const myFriends = ref([])
const currentUserId = ref(null)

const filter = ref(JSON.parse(sessionStorage.getItem('search_filter')) || {
  firstName: '',
  lastName: '',
  numberPhone: null,
  birthdayFrom: null,
  birthdayTo: null,
  timeStamp: null
})

const API_BASE = 'http://localhost:8080/api/v1/social'

watch(filter, (newFilter) => {
  sessionStorage.setItem('search_filter', JSON.stringify(newFilter))
}, { deep: true })

watch(showAdvanced, (newVal) => {
  sessionStorage.setItem('search_showAdvanced', JSON.stringify(newVal))
})

// Наблюдатель сброса истории
watch(() => [filter.value.firstName, filter.value.lastName, filter.value.numberPhone, filter.value.birthdayFrom, filter.value.birthdayTo], (newValues) => {
  const hasAnyText = newValues.some(val => val && val.toString().trim().length > 0)
  if (!hasAnyText) {
    searchResults.value = []
    searchExecuted.value = false
    sessionStorage.removeItem('search_searchResults')
    sessionStorage.removeItem('search_searchExecuted')
  }
}, { deep: true, immediate: true })

const loadCurrentFriends = async () => {
  try {
    const userMeResponse = await axios.get(`${API_BASE}/users/me`)
    currentUserId.value = userMeResponse.data.id || userMeResponse.data.userId

    if (!currentUserId.value) return

    // 1. Загружаем исходящие запросы из БД
    try {
      const outgoingResponse = await axios.get(`${API_BASE}/friends/requests/outgoing`, { params: { page: 0, size: 100 } })
      if (Array.isArray(outgoingResponse.data)) {
        sentRequests.value = outgoingResponse.data.map(req => Number(req.addresseeId || req.userId2))
      }
    } catch (e) { console.error(e) }

    // 2. Загружаем входящие запросы из БД
    try {
      const incomingResponse = await axios.get(`${API_BASE}/friends/requests/incoming`, { params: { page: 0, size: 100 } })
      if (Array.isArray(incomingResponse.data)) {
        incomingRequests.value = incomingResponse.data.map(req => Number(req.requesterId || req.userId1 || req.id))
      }
    } catch (e) { console.error(e) }

    // 3. Загружаем текущих друзей
    const response = await axios.get(`${API_BASE}/friends/public/${currentUserId.value}`)
    if (Array.isArray(response.data)) {
      myFriends.value = response.data.map(f => {
        const id1 = Number(f.userId1)
        const id2 = Number(f.userId2)
        const myId = Number(currentUserId.value)
        if (id1 === myId) return id2
        if (id2 === myId) return id1
        return f.friendId || f.id
      }).filter(id => id !== null && !isNaN(id) && id !== Number(currentUserId.value))
    }
  } catch (error) {
    console.error('Ошибка загрузки связей профиля:', error)
  }
}

const handleSearch = async () => {
  if (!filter.value.firstName.trim() && !filter.value.lastName.trim() && !filter.value.numberPhone && !filter.value.birthdayFrom && !filter.value.birthdayTo) {
    alert('Введите хотя бы один параметр для поиска!')
    return
  }
  try {
    const cleanedFilter = {}
    if (filter.value.firstName.trim()) cleanedFilter.firstName = filter.value.firstName.trim()
    if (filter.value.lastName.trim()) cleanedFilter.lastName = filter.value.lastName.trim()
    if (filter.value.numberPhone) cleanedFilter.numberPhone = filter.value.numberPhone.trim()
    if (filter.value.birthdayFrom) cleanedFilter.birthdayFrom = filter.value.birthdayFrom
    if (filter.value.birthdayTo) cleanedFilter.birthdayTo = filter.value.birthdayTo

    const response = await axios.get(`${API_BASE}/users/search`, { params: { ...cleanedFilter, page: 0, size: 10 } })
    searchResults.value = response.data
    searchExecuted.value = true

    sessionStorage.setItem('search_searchResults', JSON.stringify(response.data))
    sessionStorage.setItem('search_searchExecuted', JSON.stringify(true))
  } catch (error) {
    console.error(error)
  }
}

const sendFriendRequest = async (addresseeId) => {
  try {
    await axios.post(`${API_BASE}/friends/requests/${addresseeId}`)
    sentRequests.value.push(Number(addresseeId))
    alert('Заявка в друзья успешно отправлена!')
  } catch (error) { console.error(error) }
}

const handleCancelRequest = async (addresseeId) => {
  if (!confirm('Вы уверены, что хотите отменить эту заявку в друзья?')) return
  try {
    await axios.delete(`${API_BASE}/friends/requests`, { params: { addresseeId } })
    sentRequests.value = sentRequests.value.filter(id => Number(id) !== Number(addresseeId))
    alert('Заявка успешно отменена!')
  } catch (error) { console.error(error) }
}

const handleAcceptFriendRequest = async (user) => {
  try {
    const incomingResponse = await axios.get(`${API_BASE}/friends/requests/incoming`, { params: { page: 0, size: 100 } })
    const targetUserId = Number(user.id || user.userId)
    const activeRequest = incomingResponse.data.find(req => Number(req.requesterId || req.userId1 || req.id) === targetUserId)

    if (activeRequest) {
      const requestId = activeRequest.id || activeRequest.requestId
      await axios.put(`${API_BASE}/friends/requests/${requestId}`, null, { params: { status: 'ACCEPTED' } })
      alert('Заявка в друзья успешно принята!')
      await loadCurrentFriends()
    }
  } catch (error) { console.error(error) }
}

const handleRemoveFriend = async (friendId) => {
  if (!confirm('Удалить пользователя из друзей?')) return
  try {
    await axios.delete(`${API_BASE}/friends/private`, { params: { userId1: currentUserId.value, userId2: friendId } })
    myFriends.value = myFriends.value.filter(id => Number(id) !== Number(friendId))
    sessionStorage.setItem('search_searchResults', JSON.stringify(searchResults.value))
    alert('Пользователь удален из списка друзей.')
  } catch (error) { console.error(error) }
}

const clearSearchHistory = () => {
  searchResults.value = []
  searchExecuted.value = false
  filter.value.firstName = ''
  filter.value.lastName = ''
  filter.value.numberPhone = null
  filter.value.birthdayFrom = null
  filter.value.birthdayTo = null
  sessionStorage.removeItem('search_searchResults')
  sessionStorage.removeItem('search_searchExecuted')
  sessionStorage.removeItem('search_filter')
}

onMounted(() => { loadCurrentFriends() })
</script>
