<template>
  <div style="display: flex; flex-direction: column; gap: 20px; width: 100%; box-sizing: border-box; align-items: stretch;"
    <!-- ФОРМА СОЗДАНИЯ ПОСТА -->
    <div style="background: white; padding: 25px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7; width: 100%; box-sizing: border-box;">
      <h3 style="margin-top: 0; color: #2c3e50; font-size: 16px; margin-bottom: 15px; text-align: left;">Создать новую публикацию</h3>
      <textarea v-model="newPost.content" placeholder="Что у вас нового? (Минимум 5 символов)..." style="width: 100%; height: 90px; padding: 14px; border: 1px solid #ced6e0; border-radius: 10px; resize: none; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa; font-family: inherit;"></textarea>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 15px; width: 100%;">
        <label style="display: flex; align-items: center; gap: 8px; font-size: 14px; color: #57606f; cursor: pointer;">
          <input type="checkbox" v-model="newPost.commentsAllowed" style="width: 16px; height: 16px; cursor: pointer; accent-color: #54a0ff;">
          Разрешить комментарии
        </label>
        <button @click="handleCreatePost" style="padding: 10px 24px; background: #54a0ff; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 14px; box-shadow: 0 4px 12px rgba(84,160,255,0.2); flex-shrink: 0;">Опубликовать</button>
      </div>
    </div>

    <!-- ЛЕНТА ПОСТОВ -->
    <div style="display: flex; flex-direction: column; gap: 16px; width: 100%; box-sizing: border-box;">
      <div v-if="posts.length > 0" v-for="post in posts" :key="post.id" style="background: white; border-radius: 12px; padding: 20px; box-shadow: 0 4px 15px rgba(164,176,190,0.04); border: 1px solid #edf2f7; position: relative; width: 100%; box-sizing: border-box; overflow: hidden;">

        <!-- Шапка поста -->
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; width: 100%;">
          <div @click="emit('open-user-profile', post.authorId)" style="display: flex; align-items: center; gap: 12px; cursor: pointer;" title="Перейти в профиль">
            <div style="width: 40px; height: 40px; background-color: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #54a0ff; transition: 0.2s;" onmouseover="this.style.color='#2e86de'" onmouseout="this.style.color='#54a0ff'">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </div>
            <div style="text-align: left;">
              <span style="font-size: 15px; font-weight: 600; color: #2c3e50; transition: 0.2s;" onmouseover="this.style.color='#54a0ff'" onmouseout="this.style.color='#2c3e50'">
                {{ post.authorId === currentUserId && currentUserInfo ? (currentUserInfo.firstName + ' ' + currentUserInfo.lastName) : ('Пользователь ID: ' + post.authorId) }}
              </span>
            </div>
          </div>

          <div v-if="post.authorId === currentUserId" style="position: relative;">
            <button @click="toggleMenu(post)" style="background: none; border: none; color: #a4b0be; cursor: pointer; padding: 5px; outline: none;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>
            </button>
            <div v-if="post.showMenu" style="position: absolute; right: 0; top: 25px; background: white; border: 1px solid #edf2f7; border-radius: 8px; box-shadow: 0 4px 12px rgba(164,176,190,0.15); z-index: 50; min-width: 120px; display: flex; flex-direction: column; padding: 4px;">
              <button @click="startEdit(post)" style="width: 100%; text-align: left; padding: 8px 12px; background: none; border: none; font-size: 13px; color: #2c3e50; cursor: pointer; border-radius: 6px;">✏️ Изменить</button>
              <button @click="handleDeletePost(post.id)" style="width: 100%; text-align: left; padding: 8px 12px; background: none; border: none; font-size: 13px; color: #ff7675; cursor: pointer; border-radius: 6px;">🗑️ Удалить</button>
            </div>
          </div>
        </div>

        <!-- Текст публикации -->
        <div style="margin-bottom: 20px; text-align: left; width: 100%;">
          <div v-if="!post.isEditing" style="font-size: 15px; color: #2c3e50; line-height: 1.5; word-break: break-word; white-space: pre-wrap;">{{ post.content }}</div>
          <div v-else style="display: flex; flex-direction: column; gap: 10px; width: 100%;">
            <textarea v-model="post.editContent" style="width: 100%; height: 70px; padding: 10px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 14px; outline: none; resize: none; font-family: inherit; background: #f8f9fa;"></textarea>
            <div style="display: flex; gap: 8px; justify-content: flex-end; width: 100%;">
              <button @click="post.isEditing = false" style="padding: 6px 12px; background: #f1f2f6; color: #57606f; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer;">Отмена</button>
              <button @click="handleUpdatePost(post)" style="padding: 6px 12px; background: #54a0ff; color: white; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer;">Сохранить</button>
            </div>
          </div>
        </div>

        <!-- Подвал поста -->
        <div style="display: flex; align-items: center; justify-content: space-between; border-top: 1px solid #f1f2f6; padding-top: 12px; width: 100%;">
          <div style="display: flex; align-items: center; gap: 18px; color: #a4b0be;">
            <div style="position: relative; display: inline-block;" @mouseenter="preloadLikers(post)" @mouseleave="post.showTooltip = false">
              <button @click="handleToggleLike(post)" style="background: none; border: none; cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; transition: 0.2s; outline: none;" :style="{ color: post.isLiked ? '#ff7675' : '#a4b0be' }">
                <svg width="22" height="22" viewBox="0 0 24 24" :fill="post.isLiked ? '#ff7675' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
                <span style="font-size: 14px; font-weight: 600;">{{ post.likesCount || 0 }}</span>
              </button>
              <div v-if="post.showTooltip && post.likers && post.likers.length > 0" style="position: absolute; bottom: 30px; left: 0; background: #2c3e50; color: white; padding: 10px 14px; border-radius: 8px; font-size: 12px; white-space: nowrap; z-index: 100; box-shadow: 0 4px 15px rgba(0,0,0,0.15); display: flex; flex-direction: column; gap: 6px;">
                <div style="font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px; color: #54a0ff;">Оценили:</div>
                <div v-for="liker in post.likers" :key="liker.userId" style="opacity: 0.9;">Пользователь ID: {{ liker.userId }}</div>
              </div>
            </div>
            <button v-if="post.commentsAllowed" @click="toggleCommentsBlock(post)" style="background: none; border: none; color: #a4b0be; cursor: pointer; display: flex; align-items: center; padding: 0; outline: none;" :style="{ color: post.showComments ? '#54a0ff' : '#a4b0be' }">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
            </button>
            <button style="background: none; border: none; color: #a4b0be; cursor: pointer; display: flex; align-items: center; padding: 0;">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
            </button>
          </div>
          <div style="font-size: 12px; color: #a4b0be; text-transform: lowercase; font-weight: 500;">{{ getRelativeTime(post.createdAt) }}</div>
        </div>

        <!-- БЛОК КОММЕНТАРИЕВ -->
        <div v-if="post.showComments" style="margin-top: 15px; border-top: 1px dashed #e1e8ed; padding-top: 15px; width: 100%; box-sizing: border-box; overflow: hidden;">
          <div style="display: flex; gap: 10px; margin-bottom: 15px; width: 100%;">
            <input v-model="post.newCommentText" type="text" placeholder="Написать комментарий (2-100 симв.)..." style="flex: 1; padding: 10px 14px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 13px; outline: none; background: #f8f9fa; min-width: 0;">
            <button @click="handleCreateComment(post)" style="padding: 10px 18px; background: #54a0ff; color: white; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; flex-shrink: 0;">Отправить</button>
          </div>

          <div v-if="post.commentsList && post.commentsList.length > 0" style="display: flex; flex-direction: column; gap: 10px; width: 100%;">
            <div v-for="comment in post.commentsList" :key="comment.id" style="background: #f8f9fa; padding: 14px 16px; border-radius: 12px; border: 1px solid #edf2f7; display: flex; flex-direction: column; gap: 6px; position: relative; width: 100%; box-sizing: border-box; text-align: left; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; min-width: 0;">
              <button v-if="comment.authorId === currentUserId && !comment.isEditing" @click="handleDeleteComment(post, comment.id)" style="position: absolute; right: 16px; top: 14px; background: none; border: none; color: #ff7675; font-size: 18px; cursor: pointer; opacity: 0.6; padding: 0; line-height: 1; outline: none;">×</button>

              <div @click="emit('open-user-profile', comment.authorId)" style="font-size: 13px; font-weight: 700; color: #2c3e50; padding-right: 25px; cursor: pointer; display: inline-block; width: max-content; transition: 0.2s;" onmouseover="this.style.color='#54a0ff'" onmouseout="this.style.color='#2c3e50'">
                {{ comment.firstName || comment.authorName ? ((comment.firstName || '') + ' ' + (comment.lastName || '')) : (comment.authorId === currentUserId && currentUserInfo ? (currentUserInfo.firstName + ' ' + currentUserInfo.lastName) : 'Пользователь ID: ' + comment.authorId) }}
              </div>

              <div style="width: 100%;">
                <div v-if="!comment.isEditing">
                  <div style="font-size: 14px; color: #2c3e50; line-height: 1.4; white-space: pre-wrap;">
                    {{ comment.isExpanded || comment.content.length <= 120 ? comment.content : comment.content.slice(0, 120) + '...' }}
                  </div>
                  <button v-if="comment.content.length > 120" @click="comment.isExpanded = !comment.isExpanded" style="background: none; border: none; color: #54a0ff; font-weight: 600; font-size: 12px; cursor: pointer; padding: 4px 0 0 0; margin: 0; display: block;">
                    {{ comment.isExpanded ? 'Свернуть текст ▲' : 'Читать далее ▼' }}
                  </button>
                </div>
                <div v-else style="display: flex; gap: 8px; width: 100%; margin-top: 4px;">
                  <input v-model="comment.editContent" type="text" style="flex: 1; padding: 8px 12px; border: 1px solid #ced6e0; border-radius: 6px; font-size: 13px; outline: none; background: white; min-width: 0;">
                  <button @click="comment.isEditing = false" style="padding: 6px 12px; background: #f1f2f6; color: #57606f; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; flex-shrink: 0;">Отмена</button>
                  <button @click="handleUpdateComment(post, comment)" style="padding: 6px 12px; background: #54a0ff; color: white; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; flex-shrink: 0;">ОК</button>
                </div>
              </div>

              <div style="display: flex; align-items: center; gap: 12px; font-size: 11px; color: #a4b0be; font-weight: 500; margin-top: 2px; width: 100%;">
                <span>{{ getRelativeTime(comment.createdAt) }}</span>
                <div v-if="comment.authorId === currentUserId && !comment.isEditing" style="display: flex; gap: 10px; align-items: center;">
                  <span style="color: #edf2f7;">|</span>
                  <button @click="startEditComment(comment)" style="background: none; border: none; color: #54a0ff; cursor: pointer; padding: 0; font-size: 11px; font-weight: 600; outline: none;">Изменить</button>
                </div>
              </div>
            </div>
          </div>
          <div v-else style="text-align: center; color: #a4b0be; font-size: 12px; padding: 10px 0;">Комментариев пока нет. Будьте первым!</div>
        </div>

      </div> <!-- Конец одной карточки поста -->
    </div> <!-- Конец цикла v-if ленты постов -->

    <!-- СИЛОВАЯ ЗАГЛУШКА ДЛЯ ПУСТОЙ ЛЕНТЫ (v-else) -->
    <div v-if="posts.length === 0" style="text-align: center !important; color: #a4b0be !important; font-size: 14px !important; padding: 40px 0 !important; background: white !important; border-radius: 12px !important; border: 1px solid #edf2f7 !important; width: 100% !important; box-sizing: border-box !important;">
      Тут пока пусто. Будьте первым, кто опубликует запись!
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

// Регистрируем эмит, чтобы App.vue знал о клике на автора
const emit = defineEmits(['open-user-profile'])

defineProps({
  currentUserId: { type: Number, required: true },
  currentUserInfo: { type: Object, required: true }
})

const API_POSTS = 'http://localhost:8080/api/v1/social/posts'
const API_LIKES = 'http://localhost:8080/api/v1/social/likes'
const API_COMMENTS_PUBLIC = 'http://localhost:8080/api/v1/social/comments/public'
const API_COMMENTS_PRIVATE = 'http://localhost:8080/api/v1/social/comments/private'

const posts = ref([])
const newPost = ref({ content: '', commentsAllowed: true })

const loadMyPosts = async () => {
  try {
    const response = await axios.get(`${API_POSTS}/my-posts`, {
      params: { page: 0, size: 20, status: 'PUBLISHED' }
    })
    const fetchedPosts = response.data

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
        console.error(likeError)
        post.likesCount = 0
        post.isLiked = false
      }
    }
    posts.value = fetchedPosts
  } catch (error) {
    console.error('Ошибка при загрузке постов:', error)
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
    post.commentsList = response.data.map(comment => ({
      ...comment,
      isEditing: false,
      editContent: '',
      isExpanded: false
    }))
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
    alert('Комментарий изменен!')
    comment.isEditing = false
    loadComments(post)
  } catch (error) {
    console.error(error)
    alert('Не удалось изменить комментарий.')
  }
}

const handleDeleteComment = async (post, commentId) => {
  if (!confirm('Вы уверены, что хотите удалить свой комментарий?')) return
  try {
    await axios.delete(`${API_COMMENTS_PRIVATE}/${commentId}`)
    alert('Комментарий удален!')
    loadComments(post)
  } catch (error) {
    console.error(error)
  }
}

const preloadLikers = async (post) => {
  const actualId = post.id || post.postId
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
  const actualId = post.id || post.postId
  try {
    await axios.post(`http://localhost:8080/api/v1/social/likes/${actualId}`)
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

const handleCreatePost = async () => {
  const contentLen = newPost.value.content.trim().length
  if (contentLen < 5 || contentLen > 100) {
    alert('Ошибка: Длина текста должна быть от 5 до 100 символов!')
    return
  }
  try {
    await axios.post(API_POSTS, newPost.value)
    newPost.value.content = ''
    loadMyPosts()
  } catch (error) {
    console.error(error)
  }
}

const handleDeletePost = async (postId) => {
  if (!confirm('Вы точно хотите навсегда удалить эту публикацию?')) return
  try {
    await axios.delete(`${API_POSTS}/${postId}`)
    alert('Публикация успешно удалена!')
    loadMyPosts()
  } catch (error) {
    console.error(error)
  }
}

const startEdit = (post) => {
  post.editContent = post.content
  post.isEditing = true
  post.showMenu = false
}

const handleUpdatePost = async (post) => {
  const contentLen = post.editContent.trim().length
  if (contentLen < 5 || contentLen > 100) {
    alert('Ошибка: Текст должен быть от 5 до 100 символов!')
    return
  }
  try {
    await axios.put(`${API_POSTS}/${post.id}`, { content: post.editContent })
    alert('Публикация изменена!')
    post.isEditing = false
    loadMyPosts()
  } catch (error) {
    console.error(error)
  }
}

const toggleMenu = (post) => {
  posts.value.forEach(p => { if (p.id !== post.id) p.showMenu = false })
  post.showMenu = !post.showMenu
}

const getRelativeTime = (dateTimeString) => {
  if (!dateTimeString) return ''
  const postDate = new Date(dateTimeString)
  const now = new Date()
  const diffMs = now - postDate
  if (diffMs < 60000) return 'только что'
  return postDate.toLocaleString('ru-RU', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
}

onMounted(() => {
  loadMyPosts()
})
</script>
