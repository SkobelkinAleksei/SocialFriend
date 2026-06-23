<template>
  <div style="background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.08); border: 1px solid #edf2f7; max-width: 760px; width: 100%; box-sizing: border-box;">
    <!-- ТРИ ПАСТЕЛЬНЫЕ ВКЛАДКИ С ФИКСИРОВАННЫМ ВЫРАВНИВАНИЕМ -->
    <div style="display: flex; gap: 8px; margin-bottom: 30px; width: 100%; box-sizing: border-box; align-items: center; flex-direction: row;">

      <!-- Если смотрим чужого пользователя, выводим как красивый заголовок, если себя — оставляем кнопкой -->
      <div v-if="targetUserId" style="font-size: 18px; font-weight: 700; color: #54a0ff; padding: 6px 0;">
        Друзья {{ targetUserName }} <span style="color: #a4b0be; font-weight: 500; font-size: 16px; margin-left: 4px;">({{ friends.length }})</span>
      </div>

      <!-- Если это наш личный кабинет, сохраняем три вкладки, но убираем серую подложку -->
      <template v-else>
        <button @click="activeTab = 'list'" :style="subTabStyle(activeTab === 'list')">
          <span>Мои друзья ({{ friends.length }})</span>
        </button>
        <button @click="activeTab = 'incoming'" :style="subTabStyle(activeTab === 'incoming')">
          <span>Входящие заявки ({{ incomingRequests.length }})</span>
        </button>
        <button @click="activeTab = 'outgoing'" :style="subTabStyle(activeTab === 'outgoing')">
          <span>Отправленные заявки ({{ outgoingRequests.length }})</span>
        </button>
      </template>

    </div>

  <!-- ВКЛАДКА 1: СПИСОК ДРУЗЕЙ -->
  <div v-if="activeTab === 'list'">
    <div v-if="friends.length > 0" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; width: 100%; box-sizing: border-box;">

      <div
          v-for="friend in friends"
          :key="friend.id"
          @click="emit('open-user-profile', friend.id)"
          style="display: flex; flex-direction: column; align-items: center; padding: 24px 16px; background: #f8f9fa; border-radius: 16px; border: 1px solid #edf2f7; cursor: pointer; transition: all 0.2s ease; box-sizing: border-box; text-align: center;"
          onmouseover="this.style.transform='translateY(-4px)'; this.style.borderColor='#54a0ff'; this.style.boxShadow='0 8px 24px rgba(84,160,255,0.12)'; this.style.background='white';"
          onmouseout="this.style.transform='translateY(0)'; this.style.borderColor='#edf2f7'; this.style.boxShadow='none'; this.style.background='#f8f9fa';"
          title="Открыть профиль"
      >
        <!-- Аватарка по центру кубика -->
        <div style="width: 50px; height: 50px; background: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #54a0ff; margin-bottom: 12px; transition: 0.2s;">
          👤
        </div>

        <!-- Имя и фамилия -->
        <span style="font-size: 15px; font-weight: 600; color: #2c3e50; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; display: block; margin-bottom: 16px;">
       {{ friend.name || ('Пользователь ID: ' + friend.id) }}
     </span>
        <!-- НОВАЯ КНОПКА: НАПИСАТЬ СООБЩЕНИЕ -->
        <button
            @click.stop="emit('open-chat', friend.id)"
            title="Открыть диалог и написать сообщение"
            style="width: 100%; padding: 6px 0; background: #edf5ff; border: 1px solid #d0e6ff; color: #54a0ff; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: 0.2s;"
            onmouseover="this.style.background='#d0e6ff'"
            onmouseout="this.style.background='#edf5ff'"
        >
          💬 Написать сообщение
        </button>
        <!-- Кнопка удаления (показывается только в своем профиле) -->
        <button
            v-if="!targetUserId"
            @click.stop="handleRemoveFriend(friend.id)"
            title="Прекратить дружбу с этим пользователем"
            style="padding: 6px 14px; background: none; border: 1px solid #ff7675; color: #ff7675; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: 0.2s; width: 100%; margin-top: auto;"
            onmouseover="this.style.background='#ff7675'; this.style.color='white'"
            onmouseout="this.style.background='none'; this.style.color='#ff7675'"
        >
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
      <div v-if="incomingRequests.length > 0" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; width: 100%; box-sizing: border-box;">

        <div
            v-for="req in incomingRequests"
            :key="req.id"
            @click="emit('open-user-profile', req.requesterId)"
            style="display: flex; flex-direction: column; align-items: center; padding: 24px 16px; background: #f8f9fa; border-radius: 16px; border: 1px solid #edf2f7; cursor: pointer; transition: all 0.2s ease; box-sizing: border-box; text-align: center;"
            onmouseover="this.style.transform='translateY(-4px)'; this.style.borderColor='#54a0ff'; this.style.boxShadow='0 8px 24px rgba(84,160,255,0.12)'; this.style.background='white';"
            onmouseout="this.style.transform='translateY(0)'; this.style.borderColor='#edf2f7'; this.style.boxShadow='none'; this.style.background='#f8f9fa';"
            title="Открыть профиль"
        >
          <!-- Аватарка по центру -->
          <div style="width: 50px; height: 50px; background: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #54a0ff; margin-bottom: 12px;">
            👤
          </div>

          <!-- Имя и фамилия -->
          <span style="font-size: 15px; font-weight: 600; color: #2c3e50; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; display: block; margin-bottom: 4px;">
       {{ req.name || 'Новый пользователь' }}
     </span>
          <span style="font-size: 12px; color: #a4b0be; margin-bottom: 16px; display: block;">хочет в друзья</span>

          <!-- Кнопки действий (в один ряд внизу кубика) -->
          <div style="display: flex; gap: 8px; width: 100%; margin-top: auto;">
            <button
                @click.stop="handleProcessRequest(req.id, 'ACCEPTED')"
                title="Принять заявку и добавить пользователя в друзья"
                style="flex: 1; padding: 6px 0; background: #e3fafc; color: #0c8599; border: 1px solid #c5f6fa; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: 0.2s;"
                onmouseover="this.style.background='#c5f6fa'"
                onmouseout="this.style.background='#e3fafc'"
            >
              Принять
            </button>
            <button
                @click.stop="handleProcessRequest(req.id, 'REJECTED')"
                title="Отклонить заявку в друзья"
                style="flex: 1; padding: 6px 0; background: #fff5f5; color: #e53e3e; border: 1px solid #fed7d7; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: 0.2s;"
                onmouseover="this.style.background='#ffe3e3'"
                onmouseout="this.style.background='#fff5f5'"
            >
              Отклонить
            </button>
          </div>
        </div>

      </div>
      <div v-else style="color: #a4b0be; text-align: center; padding: 40px 0; font-size: 14px;">
        Нет новых входящих заявок.
      </div>
    </div>

    <!-- ВКЛАДКА 3: ОТПРАВЛЕННЫЕ ЗАЯВКИ -->
    <div v-if="activeTab === 'outgoing'">
      <div v-if="outgoingRequests.length > 0" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; width: 100%; box-sizing: border-box;">

        <div
            v-for="req in outgoingRequests"
            :key="req.id"
            @click="emit('open-user-profile', req.addresseeId)"
            style="display: flex; flex-direction: column; align-items: center; padding: 24px 16px; background: #f8f9fa; border-radius: 16px; border: 1px solid #edf2f7; cursor: pointer; transition: all 0.2s ease; box-sizing: border-box; text-align: center;"
            onmouseover="this.style.transform='translateY(-4px)'; this.style.borderColor='#54a0ff'; this.style.boxShadow='0 8px 24px rgba(84,160,255,0.12)'; this.style.background='white';"
            onmouseout="this.style.transform='translateY(0)'; this.style.borderColor='#edf2f7'; this.style.boxShadow='none'; this.style.background='#f8f9fa';"
            title="Открыть профиль"
        >
          <!-- Аватарка по центру -->
          <div style="width: 50px; height: 50px; background: #f1f2f6; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #a4b0be; margin-bottom: 12px;">
            👤
          </div>

          <!-- Имя и фамилия -->
          <span style="font-size: 15px; font-weight: 600; color: #2c3e50; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; display: block; margin-bottom: 4px;">
       {{ req.name || 'Пользователь' }}
     </span>
          <span style="font-size: 12px; color: #a4b0be; margin-bottom: 16px; display: block;">Заявка ожидает ответа</span>

          <!-- Кнопка Отмены -->
          <button
              @click.stop="handleCancelRequest(req.addresseeId)"
              title="Отозвать отправленную заявку в друзья"
              style="width: 100%; padding: 6px 0; background: none; border: 1px solid #a4b0be; color: #57606f; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: 0.2s; margin-top: auto;"
              onmouseover="this.style.background='#f1f2f6'; this.style.borderColor='#57606f';"
              onmouseout="this.style.background='none'; this.style.borderColor='#a4b0be';"
          >
            Отменить заявку
          </button>
        </div>

      </div>
      <div v-else style="color: #a4b0be; text-align: center; padding: 40px 0; font-size: 14px;">
        Нет отправленных заявок.
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  currentUserId: { type: Number, required: true },
  targetUserId: { type: Number, required: false, default: null },
  targetUserName: { type: String, required: false, default: '' }
})

// ИСПРАВЛЕНО: Добавлен эмит update-count для передачи цифры наверх в App.vue
const emit = defineEmits(['open-user-profile', 'update-count', 'back-to-profile', 'open-chat'])


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
  const activeId = props.targetUserId || props.currentUserId
  if (!activeId) return
  try {
    const response = await axios.get(`${API_PUBLIC}/${activeId}`)
    const flatIds = response.data.map(f => f.userId1 === activeId ? f.userId2 : f.userId1)
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
  if (!props.currentUserId) return
  try {
    const response = await axios.get(`${API_REQUESTS}/incoming`)
    const rawRequests = response.data

    // 💡 Пробегаемся по каждой заявке и запрашиваем имя автора по его requesterId
    const mappedRequests = []
    for (let req of rawRequests) {
      const name = await fetchUserName(req.requesterId)
      mappedRequests.push({
        ...req,
        name: name // Записываем имя прямо в объект заявки
      })
    }

    incomingRequests.value = mappedRequests
  } catch (error) {
    console.error('Ошибка загрузки входящих заявок:', error)
  }
}

const loadOutgoingRequests = async () => {
  try {
    const response = await axios.get(`${API_REQUESTS}/outgoing`, {
      params: { page: 0, size: 20, status: 'PENDING' }
    })
    const rawRequests = response.data

    // 💡 Пробегаемся по каждой отправленной заявке и запрашиваем имя получателя по его addresseeId
    const mappedRequests = []
    for (let req of rawRequests) {
      // Вызываем вашу общую функцию получения имени по ID
      const name = await fetchUserName(req.addresseeId)
      mappedRequests.push({
        ...req,
        name: name // Записываем реальное имя в объект, чтобы шаблон его увидел
      })
    }

    outgoingRequests.value = mappedRequests
  } catch (error) {
    console.error('Ошибка загрузки отправленных заявок:', error)
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
  if (!props.targetUserId) { // Загружаем заявки только для СЕБЯ
    loadIncomingRequests()
    loadOutgoingRequests()
  } else {
    incomingRequests.value = []
    outgoingRequests.value = []
  }
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
