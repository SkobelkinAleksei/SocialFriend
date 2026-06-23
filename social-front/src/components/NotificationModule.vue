<template>
  <div style="background: white; padding: 32px; border-radius: 16px; box-shadow: 0 10px 30px rgba(164,176,190,0.08); border: 1px solid #edf2f7; max-width: 760px; width: 100%; box-sizing: border-box;">

    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-bottom: 2px solid #f8f9fa; padding-bottom: 15px;">
      <h3 style="margin: 0; color: #2c3e50; font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 8px;">
        Уведомления 🔔
      </h3>
      <button
          v-if="notifications.some(n => !n.read)"
          @click="handleMarkAllAsRead"
          style="background: transparent; border: none; color: #54a0ff; font-size: 13px; font-weight: 600; cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: 0.2s;"
          onmouseover="this.style.background='#edf5ff'"
          onmouseout="this.style.background='transparent'"
      >
        Прочитать все
      </button>
    </div>

    <!-- СПИСОК УВЕДОМЛЕНИЙ -->
    <div v-if="notifications.length > 0" style="display: flex; flex-direction: column; gap: 12px; width: 100%; box-sizing: border-box;">
      <div
          v-for="notif in notifications"
          :key="notif.id"
          @click="handleNotificationClick(notif)"
          @mouseenter="handleNotificationHover(notif)"
          style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-radius: 12px; border: 1px solid #edf2f7; transition: all 0.2s ease; box-sizing: border-box; width: 100%; text-align: left; cursor: pointer;"
          :style="{ background: notif.read ? '#ffffff' : '#f4f9ff', borderColor: notif.read ? '#edf2f7' : '#d0e6ff' }"
          onmouseover="this.style.transform='translateY(-1px)'; this.style.boxShadow='0 4px 12px rgba(164,176,190,0.05)'"
          onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='none'"
          title="Пометить как прочитанное"
      >
        <div style="display: flex; align-items: center; gap: 14px; min-width: 0; flex: 1;">
          <!-- Пастельная иконка в зависимости от типа события -->
          <div style="width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0;"
               :style="getIconStyle(notif.type)">
            {{ getIconText(notif.type) }}
          </div>

          <!-- Текст уведомления (Имя отправителя выделено и кликабельно) -->
          <div style="min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 2px;">
            <p style="margin: 0; font-size: 14px; color: #2c3e50; line-height: 1.4; word-break: break-word;">
              <strong
                  @click.stop="emit('open-user-profile', notif.senderId)"
                  style="color: #2c3e50; cursor: pointer; text-decoration: none;"
                  onmouseover="this.style.color='#54a0ff'; this.style.textDecoration='underline';"
                  onmouseout="this.style.color='#2c3e50'; this.style.textDecoration='none';"
                  title="Перейти в профиль"
              >
                {{ notif.senderName || 'Пользователь' }}
              </strong>
              {{ getTranslatedMessage(notif) }}
            </p>
            <span style="font-size: 11px; color: #a4b0be;">{{ formatDateTime(notif.createdAt) }}</span>
          </div>
        </div>

        <!-- Синяя точка-индикатор для непрочитанных пушей -->
        <div v-if="!notif.read" style="width: 8px; height: 8px; background: #54a0ff; border-radius: 50%; margin-left: 15px; flex-shrink: 0;"></div>
      </div>
    </div>

    <div v-else style="color: #a4b0be; text-align: center; padding: 50px 0; font-size: 14px;">
      У вас пока нет уведомлений. Здесь будут появляться события вашей страницы.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const props = defineProps({
  currentUserId: { type: Number, required: true }
})

const emit = defineEmits(['open-user-profile', 'open-chat', 'update-unread-count'])
const notifications = ref([])

const API_NOTIFICATIONS = 'http://localhost:8080/api/v1/social/notifications'
const API_USERS = 'http://localhost:8080/api/v1/social/users'

// Загрузка уведомлений и догрузка имен отправителей
const loadNotifications = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(API_NOTIFICATIONS, {
      params: { page: 0, size: 50 },
      headers: { Authorization: `Bearer ${token}` }
    })

    const rawList = response.data
    const mappedList = []

    for (let notif of rawList) {
      let senderName = 'Пользователь'
      try {
        const userRes = await axios.get(`${API_USERS}/${notif.senderId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (userRes.data) {
          senderName = `${userRes.data.firstName} ${userRes.data.lastName}`
        }
      } catch (e) { console.error(e) }

      mappedList.push({ ...notif, senderName })
    }

    notifications.value = mappedList
    sendUnreadCountToParent()
  } catch (error) {
    console.error('Ошибка загрузки уведомлений:', error)
  }
}

// Отправка количества непрочитанных в родительский App.vue
const sendUnreadCountToParent = () => {
  const unreadCount = notifications.value.filter(n => !n.read).length
  emit('update-unread-count', unreadCount)
}

// Клик по уведомлению: помечаем прочитанным и при желании совершаем переход
const handleNotificationClick = async (notif) => {
  if (!notif.read) {
    try {
      const token = localStorage.getItem('token')
      await axios.patch(`${API_NOTIFICATIONS}/${notif.id}/read`, null, {
        headers: { Authorization: `Bearer ${token}` }
      })
      notif.read = true
      sendUnreadCountToParent()
    } catch (e) { console.error(e) }
  }

  // Роутинг: перенаправляем пользователя в зависимости от типа события
  if (notif.type.startsWith('FRIEND_REQUEST')) {
    emit('open-user-profile', notif.senderId)
  } else if (notif.type === 'POST_LIKE' || notif.type === 'NEW_COMMENT') {
    const idToSend = notif.targetId;
    if (idToSend) {
      emit('open-post', Number(idToSend), notif.type === 'NEW_COMMENT');
    }
  }
}

const handleMarkAllAsRead = async () => {
  try {
    const token = localStorage.getItem('token')
    await axios.patch(`${API_NOTIFICATIONS}/read-all`, null, {
      headers: { Authorization: `Bearer ${token}` }
    })
    notifications.value.forEach(n => n.read = true)
    sendUnreadCountToParent()
  } catch (e) { console.error(e) }
}

// Хелперы для пастельной стилизации иконок
const getIconStyle = (type) => {
  if (type.includes('FRIEND')) return { background: '#edf5ff', color: '#54a0ff' }
  if (type === 'POST_LIKE') return { background: '#fff5f5', color: '#ff7675' }
  if (type === 'NEW_COMMENT') return { background: '#e3fafc', color: '#0c8599' }
  return { background: '#f1f2f6', color: '#57606f' }
}

const getIconText = (type) => {
  if (type === 'FRIEND_REQUEST_SENT') return '👤'
  if (type === 'FRIEND_REQUEST_ACCEPTED') return '🤝'
  if (type === 'POST_LIKE') return '❤️'
  if (type === 'NEW_COMMENT') return '💬'
  return '🔔'
}

const getTranslatedMessage = (notif) => {
  if (notif.type === 'FRIEND_REQUEST_SENT') return ' хочет добавить вас в друзья.'
  if (notif.type === 'FRIEND_REQUEST_ACCEPTED') return ' принял вашу заявку в друзья! Теперь вы друзья.'
  if (notif.type === 'FRIEND_REQUEST_REJECTED') return ' отклонил ваш запрос в друзья.'
  if (notif.type === 'POST_LIKE') return ' оценил вашу публикацию.'
  if (notif.type === 'NEW_COMMENT') return ' оставил комментарий к вашей записи.'
  return notif.message || ' совершил действие.'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('ru-RU', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
}
// 💡 Функция прочтения уведомления ПРИ НАВЕДЕНИИ курсора
const handleNotificationHover = async (notif) => {
  // Если оно УЖЕ прочитано, ничего не делаем, чтобы не спамить бэкенд запросами
  if (notif.read) return

  try {
    const token = localStorage.getItem('token')
    // Отправляем PATCH запрос на ваш контроллер бэкенда
    await axios.patch(`${API_NOTIFICATIONS}/${notif.id}/read`, null, {
      headers: { Authorization: `Bearer ${token}` }
    })

    // Мгновенно меняем статус на фронтенде, чтобы синяя точка пропала, а фон стал белым
    notif.read = true

    // Обновляем общий счётчик в левом меню App.vue
    sendUnreadCountToParent()
  } catch (error) {
    console.error('Ошибка при автоматическом прочтении уведомления:', error)
  }
}

onMounted(() => { loadNotifications() })
</script>
