<template>
  <!-- ИСПРАВЛЕНО: Восстановлен главный корневой контейнер страницы -->
  <div style="display: flex; flex-direction: column; gap: 25px; max-width: 760px; width: 100%; margin: 0; box-sizing: border-box;">
    <!-- КНОПКА НАЗАД -->
    <div style="text-align: left;">
      <button
          @click="$emit('back')"
          style="padding: 8px 14px; background: #f1f2f6; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; color: #57606f; font-size: 13px; transition: 0.2s;"
          onmouseover="this.style.background='#e4e7eb'"
          onmouseout="this.style.background='#f1f2f6'"
      >
        ← Назад
      </button>
    </div>

    <!-- КАРТОЧКА ЧУЖОГО ПРОФИЛЯ -->
    <div style="background: white; padding: 25px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7; display: flex; align-items: center; gap: 20px; position: relative;">

      <!-- Пастельный аватар -->
      <div style="width: 64px; height: 64px; background-color: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #54a0ff;">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
      </div>

      <!-- Имя и информация -->
      <div style="flex: 1; text-align: left;">
        <h2 style="margin: 0 0 4px 0; color: #2c3e50; font-size: 20px; font-weight: 700;">
          {{ targetUser?.firstName || 'Пользователь' }} {{ targetUser?.lastName || '' }}
        </h2>
        <p style="margin: 0; font-size: 13px; color: #a4b0be; font-weight: 500;">ID пользователя: {{ userId }}</p>
        <p v-if="targetUser?.email" style="margin: 2px 0 0 0; font-size: 12px; color: #54a0ff;">{{ targetUser.email }}</p>
      </div>

      <!-- Кнопка действия -->
      <div v-if="userId !== currentUserId">
        <span
            v-if="isRequestSent"
            style="padding: 10px 16px; background: #fef3c7; color: #d97706; border-radius: 8px; font-weight: 600; font-size: 13px; display: inline-block; border: 1px solid #fde68a;"
        >
          ⏳ Запрос отправлен
        </span>
        <button
            v-else
            @click="sendFriendRequest(userId)"
            style="padding: 10px 16px; background: #2ecc71; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 13px; box-shadow: 0 4px 10px rgba(46,204,113,0.15); transition: 0.2s;"
            onmouseover="this.style.background='#27ae60'"
            onmouseout="this.style.background='#2ecc71'"
        >
          Добавить в друзья
        </button>
      </div>
    </div>

    <!-- СТЕНА ПОСТОВ ЭТОГО ПОЛЬЗОВАТЕЛЯ -->
    <div style="display: flex; flex-direction: column; gap: 16px; width: 100%;">
      <h3 style="margin: 0; text-align: left; color: #2c3e50; font-size: 16px; font-weight: 700;">Публикации автора:</h3>

      <div v-if="userPosts.length > 0" v-for="post in userPosts" :key="post.id" style="background: white; border-radius: 12px; padding: 20px; box-shadow: 0 4px 15px rgba(164,176,190,0.04); border: 1px solid #edf2f7; position: relative; width: 100%; box-sizing: border-box; overflow: hidden;">

        <!-- Шапка поста -->
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px;">
          <div style="display: flex; align-items: center; gap: 12px;">
            <div style="width: 40px; height: 40px; background-color: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #54a0ff;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </div>
            <div style="text-align: left;">
              <span style="font-size: 15px; font-weight: 600; color: #2c3e50;">
                {{ targetUser ? (targetUser.firstName + ' ' + targetUser.lastName) : ('Пользователь ID: ' + post.authorId) }}
              </span>
              <div style="font-size: 11px; color: #a4b0be; margin-top: 2px;">{{ formatDateTime(post.createdAt) }}</div>
            </div>
          </div>
        </div>

        <!-- Текст публикации -->
        <div style="margin-bottom: 20px; text-align: left;">
          <div style="font-size: 15px; color: #2c3e50; line-height: 1.5; word-break: break-word; white-space: pre-wrap;">{{ post.content }}</div>
        </div>
        <!-- Подвал поста -->
        <div style="display: flex; align-items: center; justify-content: space-between; border-top: 1px solid #f1f2f6; padding-top: 12px;">
          <div style="display: flex; align-items: center; gap: 18px; color: #a4b0be;">

            <!-- Кнопка Лайка -->
            <div style="position: relative; display: inline-block;" @mouseenter="preloadLikers(post)" @mouseleave="post.showTooltip = false">
              <button @click="handleToggleLike(post)" style="background: none; border: none; cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; transition: 0.2s; outline: none;" :style="{ color: post.isLiked ? '#ff7675' : '#a4b0be' }">
                <svg width="22" height="22" viewBox="0 0 24 24" :fill="post.isLiked ? '#ff7675' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
                <span style="font-size: 14px; font-weight: 600;">{{ post.likesCount || 0 }}</span>
              </button>
              <div v-if="post.showTooltip && post.likers && post.likers.length > 0" style="position: absolute; bottom: 30px; left: 0; background: #2c3e50; color: white; padding: 10px 14px; border-radius: 8px; font-size: 12px; white-space: nowrap; z-index: 100; box-shadow: 0 4px 15px rgba(0,0,0,0.15); display: flex; flex-direction: column; gap: 6px;">
                <div style="font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px; color: #54a0ff; text-align: left;">Оценили:</div>
                <div v-for="liker in post.likers" :key="liker.userId" style="opacity: 0.9;">Пользователь ID: {{ liker.userId }}</div>
              </div>
            </div>

            <!-- Кнопка комментариев -->
            <button v-if="post.commentsAllowed" @click="toggleCommentsBlock(post)" style="background: none; border: none; color: #a4b0be; cursor: pointer; display: flex; align-items: center; padding: 0; outline: none;" :style="{ color: post.showComments ? '#54a0ff' : '#a4b0be' }">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
            </button>

            <!-- Кнопка Поделиться -->
            <button style="background: none; border: none; color: #a4b0be; cursor: pointer; display: flex; align-items: center; padding: 0;">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
            </button>
          </div>
        </div>

        <!-- БЛОК КОММЕНТАРИЕВ (ФИКСИРОВАННЫЙ ПО ШИРИНЕ) -->
        <div v-if="post.showComments" style="margin-top: 15px; border-top: 1px dashed #e1e8ed; padding-top: 15px; width: 100%; box-sizing: border-box; overflow: hidden;">

          <!-- Форма ввода комментария -->
          <div style="display: flex; gap: 10px; margin-bottom: 15px; width: 100%;">
            <input v-model="post.newCommentText" type="text" placeholder="Написать комментарий (2-100 симв.)..." style="flex: 1; padding: 10px 14px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 13px; outline: none; background: #f8f9fa; min-width: 0;">
            <button @click="handleCreateComment(post)" style="padding: 10px 18px; background: #54a0ff; color: white; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; flex-shrink: 0;">Отправить</button>
          </div>

          <!-- Список комментариев -->
          <div v-if="post.commentsList && post.commentsList.length > 0" style="display: flex; flex-direction: column; gap: 10px; width: 100%;">
            <div v-for="comment in post.commentsList" :key="comment.id" style="background: #f8f9fa; padding: 14px 16px; border-radius: 12px; border: 1px solid #edf2f7; display: flex; flex-direction: column; gap: 6px; position: relative; width: 100%; box-sizing: border-box; text-align: left; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; min-width: 0;">

              <!-- Крестик удаления -->
              <button v-if="comment.authorId === currentUserId && !comment.isEditing" @click="handleDeleteComment(post, comment.id)" style="position: absolute; right: 16px; top: 14px; background: none; border: none; color: #ff7675; font-size: 18px; cursor: pointer; opacity: 0.6; padding: 0; line-height: 1; outline: none;">×</button>

              <!-- Имя автора (ИСПРАВЛЕНО: выводит реальное имя вместо ID) -->
              <div
                  @click="emit('open-user-profile', comment.authorId)"
                  style="font-size: 13px; font-weight: 700; color: #2c3e50; padding-right: 25px; cursor: pointer; display: inline-block; width: max-content; transition: 0.2s;"
                  onmouseover="this.style.color='#54a0ff'"
                  onmouseout="this.style.color='#2c3e50'"
                  title="Открыть профиль"
              >
                {{ comment.authorName }}
              </div>

              <!-- Текст с ограничением -->
              <div style="width: 100%;">
                <div v-if="!comment.isEditing">
                  <div style="font-size: 14px; color: #2c3e50; line-height: 1.4; white-space: pre-wrap;">
                    {{ comment.isExpanded || comment.content.length <= 120 ? comment.content : comment.content.slice(0, 120) + '...' }}
                  </div>
                  <button v-if="comment.content.length > 120" @click="comment.isExpanded = !comment.isExpanded" style="background: none; border: none; color: #54a0ff; font-weight: 600; font-size: 12px; cursor: pointer; padding: 4px 0 0 0; margin: 0; display: block;">
                    {{ comment.isExpanded ? 'Свернуть текст ▲' : 'Читать далее ▼' }}
                  </button>
                </div>

                <!-- Встроенное редактирование -->
                <div v-else style="display: flex; gap: 8px; width: 100%; margin-top: 4px;">
                  <input v-model="comment.editContent" type="text" style="flex: 1; padding: 8px 12px; border: 1px solid #ced6e0; border-radius: 6px; font-size: 13px; outline: none; background: white; min-width: 0;">
                  <button @click="comment.isEditing = false" style="padding: 6px 12px; background: #f1f2f6; color: #57606f; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; flex-shrink: 0;">Отмена</button>
                  <button @click="handleUpdateComment(post, comment)" style="padding: 6px 12px; background: #54a0ff; color: white; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; flex-shrink: 0;">ОК</button>
                </div>
              </div>

              <!-- Подвал комментария -->
              <div style="display: flex; align-items: center; gap: 12px; font-size: 11px; color: #a4b0be; font-weight: 500; margin-top: 2px;">
                <span>{{ formatDateTime(comment.createdAt) }}</span>
                <div v-if="comment.authorId === currentUserId && !comment.isEditing" style="display: flex; gap: 10px; align-items: center;">
                  <span style="color: #edf2f7;">|</span>
                  <button @click="startEditComment(comment)" style="background: none; border: none; color: #54a0ff; cursor: pointer; padding: 0; font-size: 11px; font-weight: 600; outline: none;">Изменить</button>
                </div>
              </div>

            </div>
          </div>
          <div v-else style="text-align: center; color: #a4b0be; font-size: 12px; padding: 10px 0;">Комментариев пока нет. Будьте первым!</div>
        </div>
      </div> <!-- Конец одной карточки чужого поста -->
      <div v-else style="text-align: center; color: #a4b0be; padding: 40px 0; font-size: 14px; background: white; border-radius: 12px; border: 1px solid #edf2f7;">
        У этого пользователя пока нет публичных записей.
      </div>
    </div> <!-- Конец общего v-if контейнера постов -->


  </div> <!-- Конец главного контейнера страницы -->
</template>




        <script setup>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  userId: { type: Number, required: true },       // ID пользователя, к которому пришли в гости
  currentUserId: { type: Number, required: true }  // Ваш собственный ID
})

const emit = defineEmits(['back', 'open-user-profile'])


const targetUser = ref(null)
const userPosts = ref([])
const isRequestSent = ref(false)

const API_USERS = 'http://localhost:8080/api/v1/social/users'
const API_POSTS = 'http://localhost:8080/api/v1/social/posts'
const API_LIKES = 'http://localhost:8080/api/v1/social/likes'
const API_COMMENTS_PUBLIC = 'http://localhost:8080/api/v1/social/comments/public'
const API_COMMENTS_PRIVATE = 'http://localhost:8080/api/v1/social/comments/private'

const loadGuestData = async () => {
  if (!props.userId) return

  try {
    // 1. Сначала загружаем данные профиля
    const userResponse = await axios.get(`${API_USERS}/${props.userId}`)
    targetUser.value = userResponse.data

    // 2. Только после загрузки профиля проверяем кэш отправленных заявок
    const cached = JSON.parse(localStorage.getItem('cachedSentRequests')) || []

    // Приводим оба ID к типу Number для точного сравнения
    isRequestSent.value = cached.map(Number).includes(Number(props.userId))

    // 3. Загружаем посты автора
    const postsResponse = await axios.get(`${API_POSTS}/user/${props.userId}`)
    const fetchedPosts = postsResponse.data

    for (let post of fetchedPosts) {
      const actualId = post.id
      post.showTooltip = false
      post.showMenu = false
      post.isEditing = false
      post.editContent = ''
      post.likers = []
      post.showComments = false
      post.newCommentText = ''
      post.commentsList = []

      try {
        const countRes = await axios.get(`${API_LIKES}/${actualId}/likes-count`)
        post.likesCount = countRes.data
        const likedRes = await axios.get(`${API_LIKES}/${actualId}/is-liked`)
        post.isLiked = likedRes.data
      } catch (likeError) {
        post.likesCount = 0
        post.isLiked = false
      }
    }
    userPosts.value = fetchedPosts
  } catch (error) {
    console.error('Ошибка загрузки гостевого профиля:', error)
  }
}


const sendFriendRequest = async (addresseeId) => {
  try {
    await axios.post(`http://localhost:8080/api/v1/social/friends/requests/${addresseeId}`)
    isRequestSent.value = true
    const cached = JSON.parse(localStorage.getItem('cachedSentRequests')) || []
    if (!cached.includes(addresseeId)) {
      cached.push(addresseeId)
      localStorage.setItem('cachedSentRequests', JSON.stringify(cached))
    }
    alert('Заявка в друзья успешно отправлена!')
  } catch (error) {
    console.error(error)
    alert('Не удалось отправить заявку.')
  }
}

const toggleCommentsBlock = async (post) => {
  post.showComments = !post.showComments
  if (post.showComments) {
    loadComments(post)
  }
}

const loadComments = async (post) => {
  try {
    const response = await axios.get(`${API_COMMENTS_PUBLIC}/post/${post.id}`)
    const rawComments = response.data

    // Для каждого комментария запрашиваем имя автора с бэкенда по его ID
    for (let comment of rawComments) {
      comment.isEditing = false
      comment.editContent = ''
      comment.isExpanded = false
      comment.authorName = `Пользователь ID: ${comment.authorId}` // Заглушка по умолчанию

      try {
        // Делаем запрос к вашему новому эндпоинту getUserById
        const userRes = await axios.get(`${API_USERS}/${comment.authorId}`)
        if (userRes.data) {
          comment.authorName = `${userRes.data.firstName} ${userRes.data.lastName}`
        }
      } catch (userError) {
        console.error(`Не удалось загрузить имя для автора ID ${comment.authorId}:`, userError)
      }
    }

    post.commentsList = rawComments
  } catch (error) {
    console.error('Ошибка загрузки комментариев:', error)
  }
}


const handleCreateComment = async (post) => {
  const textLen = post.newCommentText.trim().length
  if (textLen < 2 || textLen > 100) {
    alert('Ошибка: Длина комментария должна быть от 2 до 100 символов!')
    return
  }
  try {
    await axios.post(`${API_COMMENTS_PUBLIC}/post/${post.id}`, { content: post.newCommentText })
    post.newCommentText = ''
    loadComments(post)
  } catch (error) {
    console.error(error)
  }
}

const startEditComment = (comment) => {
  comment.editContent = comment.content
  comment.isEditing = true
}

const handleUpdateComment = async (post, comment) => {
  const textLen = comment.editContent.trim().length
  if (textLen < 2 || textLen > 100) {
    alert('Ошибка: Текст должен быть от 2 до 100 символов!')
    return
  }
  try {
    await axios.put(`${API_COMMENTS_PRIVATE}/${comment.id}`, { content: comment.editContent })
    comment.isEditing = false
    loadComments(post)
  } catch (error) {
    console.error(error)
  }
}

const handleDeleteComment = async (post, commentId) => {
  if (!confirm('Вы уверены, что хотите удалить свой комментарий?')) return
  try {
    await axios.delete(`${API_COMMENTS_PRIVATE}/${commentId}`)
    loadComments(post)
  } catch (error) {
    console.error(error)
  }
}

const preloadLikers = async (post) => {
  const actualId = post.id
  post.showTooltip = true
  if (post.likesCount > 0) {
    try {
      const response = await axios.get(`${API_LIKES}/${actualId}/list-like`)
      const allLikers = response.data
      if (allLikers && allLikers.length > 0) {
        const shuffled = [...allLikers].sort(() => 0.5 - Math.random())
        post.likers = shuffled.slice(0, 3)
      } else {
        post.likers = []
      }
    } catch (error) {
      console.error(error)
    }
  }
}

const handleToggleLike = async (post) => {
  const actualId = post.id
  try {
    await axios.post(`${API_LIKES}/${actualId}`)
    post.isLiked = !post.isLiked
    if (post.isLiked) {
      post.likesCount++
      preloadLikers(post)
    } else {
      post.likesCount--
      post.likers = []
      post.showTooltip = false
    }
  } catch (error) {
    console.error(error)
  }
}

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return ''
  return new Date(dateTimeString).toLocaleString('ru-RU', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
}

watch(() => props.userId, () => {
  loadGuestData()
}, { immediate: true })

onMounted(() => {
  loadGuestData()
})
</script>



