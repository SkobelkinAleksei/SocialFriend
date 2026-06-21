<template>
  <div style="background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.08); border: 1px solid #edf2f7; max-width: 760px; width: 100%; box-sizing: border-box;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 2px solid #f8f9fa; padding-bottom: 15px;">
      <h2 style="margin: 0; color: #2c3e50; font-size: 20px;">Управление связями 👥</h2>
      <button @click="refreshAll" style="background: none; border: none; color: #54a0ff; font-weight: 600; cursor: pointer; font-size: 14px;">Обновить все</button>
    </div>

    <!-- ТРИ ПАСТЕЛЬНЫЕ ВКЛАДКИ С ФИКСИРОВАННЫМ ВЫРАВНИВАНИЕМ -->
    <div style="display: flex; gap: 8px; margin-bottom: 30px; background: #f8f9fa; padding: 6px; border-radius: 10px; width: 100%; box-sizing: border-box; justify-content: space-between; align-items: center; flex-direction: row;">
      <button @click="activeTab = 'list'" :style="subTabStyle(activeTab === 'list')">
        <span>Мои друзья ({{ friends.length }})</span>
      </button>

      <!-- ИСПРАВЛЕНО: Текст и кружок выровнены в одну строчку через flex/center -->
      <button @click="activeTab = 'incoming'" :style="subTabStyle(activeTab === 'incoming')">
        <span style="display: inline-flex; align-items: center; gap: 6px; justify-content: center; width: 100%;">
          Входящие заявки
          <span v-if="incomingRequests.length > 0" style="background: #ff7675; color: white; min-width: 18px; height: 18px; padding: 0 6px; border-radius: 10px; font-size: 11px; font-weight: bold; display: inline-flex; align-items: center; justify-content: center; line-height: 1; flex-shrink: 0;">
            {{ incomingRequests.length }}
          </span>
        </span>
      </button>

      <button @click="activeTab = 'outgoing'" :style="subTabStyle(activeTab === 'outgoing')">
        <span>Отправленные заявки ({{ outgoingRequests.length }})</span>
      </button>
    </div>

    <!-- ВКЛАДКА 1: СПИСОК ДРУЗЕЙ -->
    <div v-if="activeTab === 'list'">
      <div v-if="friends.length > 0" style="display: flex; flex-direction: column; gap: 14px;">
        <div v-for="friend in friends" :key="friend.id" style="display: flex; align-items: center; justify-content: space-between; padding: 16px; background: #f8f9fa; border-radius: 12px; border: 1px solid #edf2f7;">
          <div @click="emit('open-user-profile', friend.id)" style="display: flex; align-items: center; gap: 12px; cursor: pointer;" title="Открыть профиль">
            <div style="width: 40px; height: 40px; background: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; color: #54a0ff; transition: 0.2s;" onmouseover="this.style.backgroundColor='#d0e4ff'" onmouseout="this.style.backgroundColor='#edf5ff'">👤</div>
            <span style="font-size: 15px; font-weight: 600; color: #2c3e50; transition: 0.2s;" onmouseover="this.style.color='#54a0ff'" onmouseout="this.style.color='#2c3e50'">
              {{ friend.name || ('Пользователь ID: ' + friend.id) }}
            </span>
          </div>
          <button @click="handleRemoveFriend(friend.id)" style="padding: 8px 16px; background: none; border: 1px solid #ff7675; color: #ff7675; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.2s;" onmouseover="this.style.background='#ff7675'; this.style.color='white'" onmouseout="this.style.background='none'; this.style.color='#ff7675'">
            Удалить из друзей
          </button>
        </div>
      </div>
      <div v-else style="color: #a4b0be; text-align: center; padding: 40px 0; font-size: 14px;">
        Список друзей пока пуст. Самое время кого-нибудь найти!
      </div>
    </div>

    <!-- ВКЛАДКА 2: ВХОДЯЩИЕ ЗАЯВКИ -->
    <div v-if="activeTab === 'incoming'">
      <div v-if="incomingRequests.length > 0" style="display: flex; flex-direction: column; gap: 14px;">
        <div v-for="req in incomingRequests" :key="req.id" style="padding: 16px; background: #f4fbf7; border-radius: 12px; border: 1px solid #bbf7d0; display: flex; justify-content: space-between; align-items: center;">
          <div @click="emit('open-user-profile', req.requesterId)" style="font-size: 15px; color: #2c3e50; cursor: pointer;" title="Открыть профиль">
            Пользователь <strong style="color: #2ecc71; transition: 0.2s;" onmouseover="this.style.color='#54a0ff'" onmouseout="this.style.color='#2ecc71'">{{ req.requesterName || ('ID: ' + req.requesterId) }}</strong> хочет добавить вас в друзья
          </div>
          <!-- БЛОК ДЕЙСТВИЙ (ИСПРАВЛЕНО: ГАЛОЧКА И КРЕСТИК) -->
          <div style="display: flex; gap: 10px; align-items: center; flex-shrink: 0;">
            <!-- Зеленая круглая кнопка-галочка вместо слова "Принять" -->
            <button
                @click="handleProcessRequest(req.id, 'ACCEPTED')"
                style="width: 32px; height: 32px; background: #2ecc71; color: white; border: none; border-radius: 50%; font-size: 16px; font-weight: bold; cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0; box-shadow: 0 4px 10px rgba(46,204,113,0.15); line-height: 1; outline: none;"
                title="Принять заявку"
            >
              ✓
            </button>

            <!-- Красная круглая кнопка-крестик -->
            <button
                @click="handleProcessRequest(req.id, 'REJECTED')"
                style="width: 32px; height: 32px; background: #ff7675; color: white; border: none; border-radius: 50%; font-size: 18px; font-weight: 400; cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0; box-shadow: 0 4px 10px rgba(255, 118, 117, 0.15); line-height: 1; outline: none;"
                title="Отклонить заявку"
            >
              ×
            </button>
          </div>
        </div>
      </div>
      <div v-else style="color: #a4b0be; text-align: center; padding: 40px 0; font-size: 14px;">
        Новых входящих заявок нет.
      </div>
    </div>

    <!-- ВКЛАДКА 3: ИСХОДЯЩИЕ ЗАЯВКИ -->
    <div v-if="activeTab === 'outgoing'">
      <div v-if="outgoingRequests.length > 0" style="display: flex; flex-direction: column; gap: 14px;">
        <div v-for="req in outgoingRequests" :key="req.addresseeId" style="padding: 16px; background: #fdfaf4; border-radius: 12px; border: 1px solid #fde8e8; display: flex; justify-content: space-between; align-items: center;">
          <div @click="emit('open-user-profile', req.addresseeId)" style="font-size: 15px; color: #2c3e50; cursor: pointer;" title="Открыть профиль">
            Вы отправили заявку пользователю <strong style="color: #d97706; transition: 0.2s;" onmouseover="this.style.color='#54a0ff'" onmouseout="this.style.color='#d97706'">{{ req.addresseeName || ('ID: ' + req.addresseeId) }}</strong>
          </div>
          <div style="display: flex; align-items: center; gap: 16px;">
            <span style="font-size: 12px; background: #fef3c7; color: #d97706; padding: 4px 10px; border-radius: 6px; font-weight: 600;">
              Ожидание
            </span>
            <button @click="handleCancelRequest(req.addresseeId)" style="width: 28px; height: 28px; background: none; border: 1px solid #ff7675; color: #ff7675; border-radius: 50%; font-size: 16px; font-weight: 400; cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0; transition: 0.2s; outline: none;" onmouseover="this.style.background='#ff7675'; this.style.color='white'" onmouseout="this.style.background='none'; this.style.color='#ff7675'" title="Отменить заявку">
              ×
            </button>
          </div>
        </div>
      </div>
      <div v-else style="color: #a4b0be; text-align: center; padding: 40px 0; font-size: 14px;">
        Вы еще никому не отправляли заявки.
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  currentUserId: { type: Number, required: true }
})

// ИСПРАВЛЕНО: Добавлен эмит update-count для передачи цифры наверх в App.vue
const emit = defineEmits(['open-user-profile', 'update-count'])

const activeTab = ref('list')
const friends = ref([])
const incomingRequests = ref([])
const outgoingRequests = ref([])

const API_REQUESTS = 'http://localhost:8080/api/v1/social/friends/requests'
const API_PRIVATE = 'http://localhost:8080/api/v1/social/friends/private'
const API_PUBLIC = 'http://localhost:8080/api/v1/social/friends/public'
const API_USERS = 'http://localhost:8080/api/v1/social/users'

const fetchUserName = async (userId) => {
  try {
    const res = await axios.get(`${API_USERS}/${userId}`)
    if (res.data) {
      return `${res.data.firstName} ${res.data.lastName}`
    }
  } catch (err) {
    console.error(`Не удалось подгрузить имя для пользователя ${userId}:`, err)
  }
  return `Пользователь ID: ${userId}`
}

const loadFriendsList = async () => {
  if (!props.currentUserId) return
  try {
    const response = await axios.get(`${API_PUBLIC}/${props.currentUserId}`)
    const flatIds = response.data.map(f => f.userId1 === props.currentUserId ? f.userId2 : f.userId1)

    const mappedFriends = []
    for (let id of flatIds) {
      const name = await fetchUserName(id)
      mappedFriends.push({ id, name })
    }
    friends.value = mappedFriends
  } catch (error) {
    console.error('Ошибка загрузки списка друзей:', error)
  }
}

const loadIncomingRequests = async () => {
  try {
    const response = await axios.get(`${API_REQUESTS}/incoming`)
    const list = response.data

    for (let req of list) {
      req.requesterName = await fetchUserName(req.requesterId)
    }
    incomingRequests.value = list

    // ИСПРАВЛЕНО: Как только подгрузили заявки, сразу сообщаем App.vue их точное количество
    emit('update-count', list.length)
  } catch (error) {
    console.error('Ошибка входящих заявок:', error)
  }
}

const loadOutgoingRequests = async () => {
  try {
    const response = await axios.get(`${API_REQUESTS}/outgoing`, {
      params: { page: 0, size: 20 }
    })
    const list = response.data
    for (let req of list) {
      req.addresseeName = await fetchUserName(req.addresseeId)
    }
    outgoingRequests.value = list
  } catch (error) {
    console.error('Ошибка исходящих заявок:', error)
  }
}

const handleProcessRequest = async (requestId, responseStatus) => {
  try {
    await axios.put(`http://localhost:8080/api/v1/social/friends/requests/${requestId}`, null, {
      params: { status: responseStatus }
    })
    alert(responseStatus === 'ACCEPTED' ? 'Заявка принята!' : 'Заявка отклонена.')
    refreshAll()
  } catch (error) {
    console.error(error)
    alert('Не удалось обработать запрос.')
  }
}

const handleCancelRequest = async (addresseeId) => {
  if (!confirm('Вы уверены, что хотите отменить эту заявку в друзья?')) return
  try {
    await axios.delete(API_REQUESTS, {
      params: { addresseeId: addresseeId }
    })
    alert('Заявка успешно отменена!')
    loadOutgoingRequests()
  } catch (error) {
    console.error('Ошибка отмены заявки:', error)
    alert('Не удалось отменить заявку.')
  }
}

const handleRemoveFriend = async (friendId) => {
  if (!confirm('Удалить пользователя из друзей?')) return
  try {
    await axios.delete(API_PRIVATE, { params: { userId2: friendId } })
    alert('Удалено!')
    loadFriendsList()
  } catch (error) {
    console.error(error)
  }
}

const refreshAll = () => {
  loadFriendsList()
  loadIncomingRequests()
  loadOutgoingRequests()
}

watch(() => props.currentUserId, () => {
  refreshAll()
}, { immediate: true })

const subTabStyle = (isActive) => ({
  // ИСПРАВЛЕНО: Полностью убираем flex-балансировку и жестко фиксируем размер в пикселях
  width: '230px',             // Каждая вкладка ВСЕГДА будет ровно 230px
  height: '42px',             // ВСЕГДА фиксированная высота для всех трех кнопок
  flexShrink: 0,              // Запрещаем браузеру сжимать кнопки
  flexGrow: 0,                // Запрещаем браузеру растягивать кнопки

  padding: '0 10px',          // Внутренние отступы только по бокам
  background: isActive ? '#ffffff' : 'transparent',
  border: 'none',
  borderRadius: '8px',
  fontSize: '14px',
  fontWeight: '600',
  cursor: 'pointer',
  color: isActive ? '#54a0ff' : '#a4b0be',
  boxShadow: isActive ? '0 2px 10px rgba(164,176,190,0.12)' : 'none',

  // Анимируем только цвета, размеры кнопок заблокированы
  transition: 'background-color 0.15s ease, color 0.15s ease',

  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: '8px',
  whiteSpace: 'nowrap',       // Текст строго в одну строчку
  boxSizing: 'border-box'
})



onMounted(() => {
  refreshAll()
})
</script>
