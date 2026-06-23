<template>
  <div style="display: flex; height: 650px; background: white; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7; width: 100%; box-sizing: border-box; overflow: hidden;">

    <!-- ЛЕВАЯ КОЛОНКА: СПИСОК СОБЕСЕДНИКОВ -->
    <div style="width: 280px; border-right: 1px solid #edf2f7; display: flex; flex-direction: column; background: #fafafa;">
      <div style="padding: 15px 20px 0 20px;">
        <button @click="emit('go-back')" style="width: 100%; padding: 10px; background: #f1f5f9; color: #64748b; border: none; border-radius: 10px; font-weight: 600; font-size: 13px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 6px;" onmouseover="this.style.background='#e2e8f0'; this.style.color='#2c3e50'" onmouseout="this.style.background='#f1f5f9'; this.style.color='#64748b'">
          ← Назад
        </button>
      </div>
      <h3 style="padding: 20px; margin: 0; font-size: 16px; font-weight: 600; border-bottom: 1px solid #edf2f7; color: #2c3e50;">Диалоги</h3>
      <div style="flex: 1; overflow-y: auto;">
        <!-- Перебираем список друзей -->
        <!-- ОБНОВЛЕННЫЙ СПИСОК ДИАЛОГОВ В ChatModule.vue -->
        <div v-for="user in sortedFriends" :key="user.id"
             @click="selectUser(user)"
             @mouseover="user.showDeleteBtn = true"
             @mouseleave="user.showDeleteBtn = false"
             :style="userItemStyle(selectedUser?.id === user.id, user.isUnread)"
             style="position: relative;"> <!-- Обязательно добавляем position: relative -->

          <!-- Аватарка и индикатор непрочитанных -->
          <div style="width: 40px; height: 40px; background: #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; margin-right: 12px; flex-shrink: 0; position: relative;">
            👤
            <div v-if="user.isUnread && user.unreadCount > 0"
                 style="position: absolute; top: -4px; right: -4px; background: #ff7675; color: white; min-width: 18px; height: 18px; padding: 0 4px; border-radius: 50%; font-size: 10px; font-weight: bold; display: flex; align-items: center; justify-content: center; border: 2px solid white; box-sizing: border-box; line-height: 1;">
              {{ user.unreadCount }}
            </div>
          </div>

          <!-- Центральная часть: Имя и превью текста -->
          <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; padding-right: 40px;">
            <div :style="{ fontWeight: user.isUnread ? '700' : '500', fontSize: '14px', color: '#2c3e50' }">
              {{ user.firstName }} {{ user.lastName }}
            </div>
            <div :style="{ fontSize: '12px', color: user.isUnread ? '#54a0ff' : '#a4b0be', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }">
              {{ user.lastMessage }}
            </div>
          </div>

          <!-- БЛОК СПРАВА ВВЕРХУ: ВРЕМЯ И КНОПКА УДАЛЕНИЯ -->
          <div style="position: absolute; top: 12px; right: 15px; bottom: 12px; display: flex; flex-direction: column; justify-content: space-between; align-items: flex-end; width: 40px; z-index: 10;">

            <!-- Область крестика (занимает верхнюю часть) -->
            <div style="height: 16px; display: flex; align-items: center; justify-content: festivals;">
              <button
                  v-if="user.showDeleteBtn"
                  @click.stop="clearChatFromList(user)"
                  style="background: transparent; border: none; color: #ff7675; font-size: 13px; cursor: pointer; padding: 0; line-height: 1; transition: transform 0.2s;"
                  onmouseover="this.style.transform='scale(1.2)'"
                  onmouseout="this.style.transform='scale(1)'"
                  title="Очистить историю чата"
              >
                ✕
              </button>
            </div>

            <!-- Область времени (всегда снизу под крестиком) -->
            <span
                v-if="user.lastMessageAt && user.lastMessage !== 'Нет сообщений'"
                style="font-size: 11px; color: #a4b0be; font-weight: 500; white-space: nowrap; line-height: 1;"
            >
    {{ formatTime(user.lastMessageAt) }}
  </span>

          </div>

        </div>

        <div v-if="friends.length === 0" style="padding: 20px; text-align: center; color: #a4b0be; font-size: 14px;">
          У вас пока нет друзей для переписки
        </div>
      </div>
    </div>

    <!-- ПРАВАЯ КОЛОНКА: ОКНО ЧАТА -->
    <div style="flex: 1; display: flex; flex-direction: column; background: #f8f9fa;">

      <!-- Режим А: Чат с пользователем выбран -->
      <div v-if="selectedUser" style="flex: 1; display: flex; flex-direction: column; height: 100%;">

        <!-- Шапка чата с кнопкой очистки переписки -->
        <div style="background: white; padding: 18px 20px; border-bottom: 1px solid #edf2f7; font-weight: 600; font-size: 15px; color: #2c3e50; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 2px rgba(0,0,0,0.01);">
          <div
              @click="emit('open-user-profile', selectedUser.id)"
              style="cursor: pointer; display: flex; align-items: center; gap: 6px; transition: color 0.2s;"
              onmouseover="this.style.color='#54a0ff'; this.style.textDecoration='underline';"
              onmouseout="this.style.color='#2c3e50'; this.style.textDecoration='none';"
              title="Открыть профиль пользователя"
          >
            🟢 {{ selectedUser.firstName }} {{ selectedUser.lastName }}
          </div>
          <button @click="clearChatLocal" style="background: transparent; border: none; color: #ff7675; font-size: 13px; font-weight: 600; cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: background 0.2s;" onmouseover="this.style.background='#fff5f5'" onmouseout="this.style.background='transparent'">
            🗑️ Очистить чат
          </button>
        </div>

        <!-- Область сообщений -->
        <div ref="messageContainer" style="flex: 1; padding: 20px; overflow-y: auto; overflow-x: hidden; display: flex; flex-direction: column; gap: 12px; width: 100%; box-sizing: border-box;">
          <div v-for="msg in messages" :key="msg.id" :style="messageRowStyle(msg.senderId === currentUserId)" style="width: 100%; display: flex; margin-bottom: 4px;">

            <!-- Обертка пузыря для вывода кнопок действий при наведении -->
            <div style="position: relative; max-width: 65%; display: flex; flex-direction: column;"
                 @mouseover="msg.showActions = true"
                 @mouseleave="msg.showActions = false">

              <!-- Панель быстрых действий (✏️ и ❌) для моих сообщений -->
              <div v-if="msg.senderId === currentUserId && msg.showActions && editingMessageId !== msg.id"
                   style="position: absolute; top: -20px; right: 0; background: white; border: 1px solid #e2e8f0; border-radius: 8px; display: flex; gap: 4px; padding: 2px 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); z-index: 10;">
                <button @click="startEdit(msg)" style="border: none; background: transparent; cursor: pointer; font-size: 11px;">✏️</button>
                <button @click="deleteMessageLocal(msg.id)" style="border: none; background: transparent; cursor: pointer; font-size: 11px;">❌</button>
              </div>

              <!-- ТЕКСТОВОЙ ПУЗЫРЬ СООБЩЕНИЯ -->
              <div :style="messageBubbleStyle(msg.senderId === currentUserId)">
                <div style="font-size: 14px; line-height: 1.4; word-break: break-word;">{{ msg.content }}</div>
                <div :style="messageTimeStyle(msg.senderId === currentUserId)">
                  {{ formatTime(msg.timestamp) }}
                </div>
              </div>
            </div>

          </div>
        </div>

        <!-- Панель нижнего ввода сообщений (С поддержкой режима редактирования Telegram-style) -->
        <div style="background: white; border-top: 1px solid #edf2f7; display: flex; flex-direction: column; width: 100%;">

          <!-- Верхняя плашка режима редактирования -->
          <div v-if="editingMessageId" style="display: flex; justify-content: space-between; align-items: center; padding: 8px 20px; background: #f1f5f9; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #64748b; box-sizing: border-box;">
            <div style="display: flex; align-items: center; gap: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
              <span>✏️ Редактирование:</span>
              <span style="color: #2c3e50; font-style: italic; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 400px;">{{ editText }}</span>
            </div>
            <button @click="cancelEdit" style="background: transparent; border: none; color: #ff7675; cursor: pointer; font-weight: bold; font-size: 14px;">✕</button>
          </div>

          <!-- Поле ввода и кнопка отправки/сохранения -->
          <div style="padding: 15px 20px; display: flex; gap: 12px; align-items: center; box-sizing: border-box;">
            <input
                v-if="editingMessageId"
                v-model="editText"
                @keyup.enter="saveEdit"
                @keyup.esc="cancelEdit"
                type="text"
                placeholder="Редактировать сообщение..."
                style="flex: 1; padding: 12px 16px; border: 1px solid #54a0ff; border-radius: 24px; outline: none; font-size: 14px; background: #f8f9fa;"
            >
            <input
                v-else
                v-model="textInput"
                @keyup.enter="sendMessage"
                type="text"
                placeholder="Напишите сообщение..."
                style="flex: 1; padding: 12px 16px; border: 1px solid #e2e8f0; border-radius: 24px; outline: none; font-size: 14px; background: #f8f9fa; transition: border 0.2s;"
            >

            <button
                @click="editingMessageId ? saveEdit() : sendMessage()"
                :style="{ background: editingMessageId ? '#20bf6b' : '#54a0ff' }"
                style="padding: 12px 24px; color: white; border: none; border-radius: 24px; font-weight: 600; font-size: 14px; cursor: pointer; transition: background 0.2s;"
            >
              {{ editingMessageId ? 'Сохранить' : 'Отправить' }}
            </button>
          </div>
        </div>

      </div> <!-- Конец блока v-if="selectedUser" -->

      <!-- Режим Б: Заглушка, когда диалог еще не выбран пользователем -->
      <div v-else style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #a4b0be; gap: 10px;">
        <span style="font-size: 40px;">💬</span>
        <div style="font-size: 14px;">Выберите собеседника из списка слева, чтобы начать переписку</div>
      </div>

    </div>
  </div>
</template>

<script setup>
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch  } from 'vue'
import SockJS from 'sockjs-client'

const emit = defineEmits(['message-read', 'go-back', 'open-user-profile'])
const props = defineProps({
  currentUserId: { type: Number, required: true },
  preselectedUserId: { type: Number, required: false, default: null } // <-- Принимаем ID из App.vue
})

const editingMessageId = ref(null) // ID сообщения, которое сейчас редактируем
const editText = ref('') // Временный текст для редактирования
const friends = ref([])
const selectedUser = ref(null)
const messages = ref([])
const textInput = ref('')
const messageContainer = ref(null)
let stompClient = null

// Динамическая сортировка чатов от самых свежих к самым старым
const sortedFriends = computed(() => {
  return [...friends.value].sort((a, b) => {
    return new Date(b.lastMessageAt) - new Date(a.lastMessageAt)
  })
})

// Вспомогательный метод для загрузки имени по ID (как в FriendModule.vue)
const fetchUserName = async (userId) => {
  try {
    const token = localStorage.getItem('token')
    const res = await axios.get(`http://localhost:8080/api/v1/social/users/${userId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data) {
      return `${res.data.firstName} ${res.data.lastName}`
    }
  } catch (err) {
    console.error(`Не удалось подгрузить имя для пользователя ${userId}:`, err)
  }
  return `Пользователь ID: ${userId}`
}

// метод загрузки друзей
// Исправленный метод загрузки друзей в ChatModule.vue
// Переименуем метод для логичности: теперь он загружает именно диалоги, а не просто друзей
const loadFriends = async () => {
  if (!props.currentUserId) return
  try {
    const token = localStorage.getItem('token')

    // 💡 ЗАМЕНА: Вместо эндпоинта друзей вызываем новый эндпоинт всех чатов
    const response = await axios.get('http://localhost:8080/api/v1/social/chats/users', {
      headers: { Authorization: `Bearer ${token}` }
    })

    // В ответе придет чистый массив ID собеседников, например: [1, 3, 4]
    const chatUserIds = response.data
    const mappedFriends = []

    for (let id of chatUserIds) {
      const name = await fetchUserName(id)
      const nameParts = name.split(' ')
      const firstName = nameParts[0] || 'Пользователь'
      const lastName = nameParts[1] || `ID: ${id}`

      let lastMessageText = 'Нет сообщений'
      let isUnreadFromHim = false
      let lastMessageTime = '1970-01-01T00:00:00.000Z'
      let unreadFromThisFriendCount = 0

      try {
        const historyRes = await axios.get(`http://localhost:8080/api/v1/social/chats/history/${id}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        const chatHistory = historyRes.data
        if (chatHistory && chatHistory.length > 0) {
          const lastMsg = chatHistory[chatHistory.length - 1]
          lastMessageText = lastMsg.content
          lastMessageTime = lastMsg.timestamp || new Date().toISOString()

          unreadFromThisFriendCount = chatHistory.filter(msg =>
              msg.senderId === id && msg.recipientId === props.currentUserId && msg.read === false
          ).length

          if (unreadFromThisFriendCount > 0) {
            isUnreadFromHim = true
          }
        }
      } catch (err) {
        console.error('Ошибка получения превью сообщения:', err)
      }

      mappedFriends.push({
        id,
        firstName,
        lastName,
        lastMessage: lastMessageText,
        isUnread: isUnreadFromHim,
        lastMessageAt: lastMessageTime,
        unreadCount: unreadFromThisFriendCount
      })
    }
    friends.value = mappedFriends
  } catch (error) {
    console.error('Ошибка загрузки списка активных чатов:', error)
  }
}

// 2. Подключение к WebSocket
const initWebSocket = () => {
  const token = localStorage.getItem('token')

  stompClient = new Client({
    brokerURL: `ws://localhost:8080/ws/chat?token=${token}`,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  })

  stompClient.onConnect = (frame) => {
    console.log('Успешно подключено к WebSocket чата через чистый WS протокол!')

    stompClient.subscribe(`/user/${props.currentUserId}/queue/messages`, (message) => {
      const activeIncomingMessage = JSON.parse(message.body)

      // Если сообщение относится к текущему открытому диалогу
      if (selectedUser.value && (activeIncomingMessage.senderId === selectedUser.value.id || activeIncomingMessage.recipientId === selectedUser.value.id)) {

        // 1. СИГНАЛ: Полная очистка чата
        if (activeIncomingMessage.content === '[CLEAR_CHAT]') {
          messages.value = []
          const targetFriend = friends.value.find(f => f.id === activeIncomingMessage.senderId)
          if (targetFriend) targetFriend.lastMessage = 'Нет сообщений'
          return
        }

        // 2. СИГНАЛ: Удаление конкретного сообщения
        if (activeIncomingMessage.content === '[DELETED]') {
          messages.value = messages.value.filter(m => m.id !== activeIncomingMessage.id)
          // Обновляем превью в списке друзей, если удалили последнее сообщение
          loadFriends()
          return
        }

        // 3. СИГНАЛ: Редактирование сообщения
        const existingMsg = messages.value.find(m => m.id === activeIncomingMessage.id)
        if (existingMsg) {
          existingMsg.content = activeIncomingMessage.content
          loadFriends()
          return
        }

        // 4. Стандартное новое сообщение
        messages.value.push(activeIncomingMessage)
        scrollToBottom()
      }

      // Обновление превью в левой колонке для фоновых чатов
      const targetFriend = friends.value.find(f => f.id === activeIncomingMessage.senderId)
      if (targetFriend && activeIncomingMessage.content !== '[CLEAR_CHAT]' && activeIncomingMessage.content !== '[DELETED]') {
        targetFriend.lastMessage = activeIncomingMessage.content
        if (!selectedUser.value || selectedUser.value.id !== activeIncomingMessage.senderId) {
          targetFriend.isUnread = true
          targetFriend.unreadCount = (targetFriend.unreadCount || 0) + 1
        }
      }
    })
  }
  stompClient.activate()
}
// Включение режима редактирования
const startEdit = (msg) => {
  editingMessageId.value = msg.id
  editText.value = msg.content
}

// Отмена редактирования
const cancelEdit = () => {
  editingMessageId.value = null
  editText.value = ''
}

// Сохранение измененного сообщения (HTTP PUT)
const saveEdit = async () => {
  if (!editText.value.trim() || !editingMessageId.value) return
  try {
    const token = localStorage.getItem('token')
    const res = await axios.put(`http://localhost:8080/api/v1/social/chats/message/${editingMessageId.value}`, editText.value.trim(), {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'text/plain'
      }
    })

    // Обновляем у себя на экране
    const msg = messages.value.find(m => m.id === editingMessageId.value)
    if (msg) msg.content = res.data.content

    // Сбрасываем режим
    cancelEdit()
    loadFriends() // Обновить левое превью
  } catch (err) {
    console.error('Ошибка редактирования сообщения:', err)
  }
}

// Удаление сообщения (HTTP DELETE)
const deleteMessageLocal = async (messageId) => {
  if (!confirm('Удалить это сообщение для всех?')) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`http://localhost:8080/api/v1/social/chats/message/${messageId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    // Удаляем у себя на экране
    messages.value = messages.value.filter(m => m.id !== messageId)
    loadFriends()
  } catch (err) {
    console.error('Ошибка удаления сообщения:', err)
  }
}

// Полная очистка диалога (HTTP DELETE)
const clearChatLocal = async () => {
  if (!selectedUser.value || !confirm(`Вы уверены, что хотите полностью очистить чат с ${selectedUser.value.firstName}? Это удалит все сообщения для обоих пользователей.`)) return
  try {
    const token = localStorage.getItem('token')
    await axios.delete(`http://localhost:8080/api/v1/social/chats/clear/${selectedUser.value.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    // Очищаем экран у себя
    messages.value = []
    loadFriends()
  } catch (err) {
    console.error('Ошибка очистки чата:', err)
  }
}

// Метод удаления чата прямо из списка диалогов слева
const clearChatFromList = async (user) => {
  if (!confirm(`Вы уверены, что хотите полностью очистить чат с ${user.firstName}?`)) return
  try {
    const token = localStorage.getItem('token')
    // Вызываем уже готовый эндпоинт очистки на бэкенде
    await axios.delete(`http://localhost:8080/api/v1/social/chats/clear/${user.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })

    // Если этот чат был открыт в данный момент — закрываем его окно
    if (selectedUser.value && selectedUser.value.id === user.id) {
      selectedUser.value = null
      messages.value = []
    }

    // Сбрасываем локальные превью текста и времени у этого друга
    user.lastMessage = 'Нет сообщений'
    user.lastMessageAt = '1970-01-01T00:00:00.000Z'
    user.isUnread = false
    user.unreadCount = 0

    // Сообщаем App.vue обновить общий счетчик непрочитанных
    emit('message-read')
  } catch (err) {
    console.error('Ошибка очистки чата из списка:', err)
  }
}


// 3. Выбор собеседника и сброс счетчиков
const selectUser = async (user) => {
  selectedUser.value = user
  messages.value = []

  // Мгновенно обнуляем локальные счетчики, чтобы тройка сразу пропала с аватарки
  user.isUnread = false
  user.unreadCount = 0

  try {
    const token = localStorage.getItem('token')

    // Помечаем сообщения как прочитанные в базе данных
    await axios.put(`http://localhost:8080/api/v1/social/chats/read/${user.id}`, null, {
      headers: { Authorization: `Bearer ${token}` }
    })

    // Обновляем цифру в левом меню App.vue
    emit('message-read')

    // Загружаем историю переписки
    const res = await axios.get(`http://localhost:8080/api/v1/social/chats/history/${user.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    messages.value = res.data
    scrollToBottom()
  } catch (err) {
    console.error('Ошибка при загрузке истории и прочтении сообщений:', err)
  }
}

// 4. Отправка сообщения
const sendMessage = () => {
  if (!textInput.value.trim() || !selectedUser.value) return

  const messagePayload = {
    senderId: props.currentUserId,
    recipientId: selectedUser.value.id,
    content: textInput.value.trim()
  }

  stompClient.publish({
    destination: '/app/chat',
    body: JSON.stringify(messagePayload)
  })

  messages.value.push({
    ...messagePayload,
    timestamp: new Date().toISOString()
  })

  const currentFriend = friends.value.find(f => f.id === selectedUser.value.id)
  if (currentFriend) {
    currentFriend.lastMessage = textInput.value.trim()
    currentFriend.lastMessageAt = new Date().toISOString()
  }

  textInput.value = ''
  scrollToBottom()
}

// 5. Автоматический скролл вниз
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

// 6. Форматирование времени
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 7. Хуки жизненного цикла
onMounted(async () => {
  await loadFriends() // Сначала загружаем список друзей
  initWebSocket() // Подключаем веб-сокеты

  // 💡 Если из профиля или списка друзей пришел ID — автоматически открываем чат с ним
  if (props.preselectedUserId) {
    const targetUserObj = friends.value.find(f => Number(f.id) === Number(props.preselectedUserId))
    if (targetUserObj) {
      selectUser(targetUserObj) // Вызываем метод выбора диалога, который у вас уже написан
    }
  }
})
// 💡 Умный наблюдатель для автооткрытия диалогов с любыми пользователями
watch(() => props.preselectedUserId, async (newUserId) => {
  if (!newUserId) return

  // 1. Сначала проверяем, есть ли уже этот пользователь в списке загруженных диалогов
  const existingUser = friends.value.find(f => Number(f.id) === Number(newUserId))

  if (existingUser) {
    // Если переписка уже была — просто кликаем по ней
    selectUser(existingUser)
  } else {
    // 2. Если переписки ЕЩЁ НЕ БЫЛО — подтягиваем его имя из микросервиса пользователей
    try {
      const name = await fetchUserName(newUserId)
      const nameParts = name.split(' ')

      const newUserCard = {
        id: Number(newUserId),
        firstName: nameParts[0] || 'Пользователь',
        lastName: nameParts[1] || '',
        lastMessage: 'Начните переписку...',
        isUnread: false,
        lastMessageAt: new Date().toISOString(),
        unreadCount: 0
      }

      // Добавляем пустую карточку в самый верх списка диалогов слева
      friends.value.unshift(newUserCard)

      // Автоматически открываем этот пустой чат
      selectUser(newUserCard)
    } catch (err) {
      console.error('Не удалось создать карточку нового чата:', err)
    }
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (stompClient) stompClient.deactivate()
})

// 8. Динамические стили для верстки
const userItemStyle = (isActive, isUnread) => ({
  display: 'flex',
  alignItems: 'center',
  padding: '12px 20px',
  cursor: 'pointer',
  borderBottom: '1px solid #f1f2f6',
  backgroundColor: isActive ? '#edf5ff' : (isUnread ? '#fff5f5' : 'transparent'),
  transition: 'background 0.2s'
})

const messageRowStyle = (isMine) => ({
  display: 'flex',
  justifyContent: isMine ? 'flex-end' : 'flex-start',
  width: '100%'
})

const messageBubbleStyle = (isMine) => ({
  maxWidth: '65%',
  padding: '10px 16px',
  borderRadius: isMine ? '16px 16px 0px 16px' : '16px 16px 16px 0px',
  backgroundColor: isMine ? '#54a0ff' : 'white',
  color: isMine ? 'white' : '#2c3e50',
  boxShadow: '0 2px 8px rgba(164,176,190,0.08)',
  border: isMine ? 'none' : '1px solid #edf2f7',
  wordBreak: 'break-word',
  whiteSpace: 'pre-wrap'
})

const messageTimeStyle = (isMine) => ({
  fontSize: '10px',
  display: 'block',
  textAlign: 'right',
  marginTop: '4px',
  color: isMine ? 'rgba(255,255,255,0.7)' : '#a4b0be'
})

</script>
