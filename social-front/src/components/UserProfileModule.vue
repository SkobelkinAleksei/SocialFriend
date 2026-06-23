<template>
    <!-- КАРТОЧКА ЧУЖОГО ПРОФИЛЯ -->
    <div style="background: white; padding: 25px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7; display: flex; align-items: center; gap: 20px; position: relative;">

      <!-- Пастельный аватар -->
      <div style="width: 64px; height: 64px; background-color: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #54a0ff;">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
      </div>

      <!-- Имя и информация -->
      <div style="flex: 1; text-align: left;">
        <h2 style="margin: 0 0 4px 0; color: #2c3e50; font-size: 20px; font-weight: 700;">
          {{ targetUser?.firstName || 'Пользователь' }} {{ targetUser?.lastName || '' }}
        </h2>
        <p style="margin: 0; font-size: 13px; color: #a4b0be; font-weight: 500;">ID пользователя: {{ userId }}</p>
        <p v-if="targetUser?.email" style="margin: 2px 0 0 0; font-size: 12px; color: #54a0ff;">{{ targetUser.email }}</p>
      </div>

      <!-- Кнопки действия в шапке профиля -->
      <div v-if="Number(userId) !== Number(currentUserId)">

        <!-- СЦЕНАРИЙ 1: Пользователь уже ваш друг -->
        <div v-if="myFriendsIds.includes(Number(userId))" style="display: flex; gap: 10px; align-items: center;">

          <!-- Ваша кнопка удаления / hover статуса "Вы друзья" -->
          <button
              @click="handleRemoveFriend(userId)"
              style="font-size: 13px; background: #edf2f7; color: #4a5568; padding: 10px 16px; border-radius: 8px; font-weight: 600; border: 1px solid #e2e8f0; cursor: pointer; transition: all 0.2s ease;"
              onmouseover="this.style.background='#fff5f5'; this.style.color='#e53e3e'; this.style.borderColor='#fed7d7'; this.textContent='❌ Удалить из друзей'"
              onmouseout="this.style.background='#edf2f7'; this.style.color='#4a5568'; this.style.borderColor='#e2e8f0'; this.innerHTML='👤 Вы друзья'"
          >
            👤 Вы друзья
          </button>
        </div>


        <!-- СЦЕНАРИЙ 2: Этот пользователь прислал запрос НАМ (Принять или Отказать) -->
        <div v-else-if="incomingRequestsIds.includes(Number(userId))" style="display: flex; gap: 10px; align-items: center;">
          <button
              @click="handleProcessIncomingRequest('ACCEPTED')"
              style="padding: 10px 16px; background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; transition: all 0.2s ease; white-space: nowrap; box-sizing: border-box;"
              onmouseover="this.style.background='#bae6fd'; this.style.borderColor='#7dd3fc'; this.textContent='✅ Подтвердить';"
              onmouseout="this.style.background='#e0f2fe'; this.style.borderColor='#bae6fd'; this.innerHTML='👤 Принять заявку';"
          >
            👤 Принять заявку
          </button>
          <button
              @click="handleProcessIncomingRequest('REJECTED')"
              style="padding: 10px 16px; background: #fff5f5; color: #e53e3e; border: 1px solid #fed7d7; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; transition: all 0.2s; white-space: nowrap;"
              onmouseover="this.style.background='#ffe3e3'; this.style.borderColor='#febaca';"
              onmouseout="this.style.background='#fff5f5'; this.style.borderColor='#fed7d7';"
          >
            ❌ Отказать
          </button>
        </div>

        <!-- СЦЕНАРИЙ 3: Мы первыми отправили исходящую заявку -->
        <span
            v-else-if="isRequestSent"
            style="padding: 10px 16px; background: #fff9db; color: #f59f00; border-radius: 8px; font-weight: 600; font-size: 13px; display: inline-block; border: 1px solid #ffe3e3;"
        >
          ⏳ Запрос отправлен
        </span>

        <!-- СЦЕНАРИЙ 4: Никаких связей нет — обычная кнопка добавления -->
        <button
            v-else
            @click="sendFriendRequest(userId)"
            style="padding: 10px 16px; background: #2ecc71; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 13px; box-shadow: 0 4px 10px rgba(46,204,113,0.15); transition: 0.2s;"
            onmouseover="this.style.background='#27ae60'"
            onmouseout="this.style.background='#2ecc71'"
        >
          Добавить в друзья
        </button>
        <!-- ПРАВАЯ КНОПКА: ТЕПЕРЬ ОНА НАХОДИТСЯ ВНЕ ВСЕХ УСЛОВИЙ И СВЕТИТСЯ ВСЕГДА ДЛЯ ЛЮБОГО ЧУЖОГО АККАУНТА -->
        <button
            @click="emit('open-chat', Number(userId))"
            style="font-size: 13px; background: #edf5ff; color: #54a0ff; padding: 10px 16px; border-radius: 8px; font-weight: 600; border: 1px solid #d0e6ff; cursor: pointer; transition: all 0.2s ease;"
            onmouseover="this.style.background='#d0e6ff'"
            onmouseout="this.style.background='#edf5ff'"
        >
          💬 Написать сообщение
        </button>
      </div>

    </div>
    <!-- БЛОК ДРУЗЕЙ ЭТОГО ПОЛЬЗОВАТЕЛЯ -->
    <div v-if="isRequestSent === false && myFriendsIds.includes(Number(userId))" style="background: white; padding: 25px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7; text-align: left; width: 100%; box-sizing: border-box;">

      <!-- КЛИКАБЕЛЬНЫЙ ЗАГОЛОВОК "Друзья" -->
      <h3
          @click="() => {
           // Универсально собираем имя из объекта текущего профиля
           const fName = targetUser?.firstName || 'Пользователь';
           const lName = targetUser?.lastName || '';
           const fullName = (fName + ' ' + lName).trim();

           emit('open-user-friends-list', Number(userId), fullName);
         }"
          style="margin-top: 0; margin-bottom: 16px; color: #2c3e50; font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 8px; cursor: pointer; width: max-content; transition: color 0.2s;"
            onmouseover="this.style.color='#54a0ff'"
            onmouseout="this.style.color='#2c3e50'"
            title="Посмотреть полный список друзей"
          >
          Друзья <span style="background: #edf5ff; color: #54a0ff; padding: 2px 8px; border-radius: 20px; font-size: 13px;">{{ targetUserFriends.length }}</span>
      </h3>


      <!-- КАРУСЕЛЬ С КНОПКАМИ ВЛЕВО / ВПРАВО -->
      <div v-if="targetUserFriends.length > 0" style="display: flex; align-items: center; gap: 10px; width: 100%; position: relative;">

        <!-- Кнопка Влево (показывается только если есть куда листать) -->
        <button
            v-if="currentStartIndex > 0"
            @click="currentStartIndex--"
            style="width: 32px; height: 32px; border-radius: 50%; border: 1px solid #edf2f7; background: white; color: #57606f; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: 0.2s; flex-shrink: 0;"
            onmouseover="this.style.background='#f1f2f6'"
            onmouseout="this.style.background='white'"
        >
          ←
        </button>
        <div v-else style="width: 32px; flex-shrink: 0;"></div> <!-- Отступ-заглушка для выравнивания -->

        <!-- Строка с карточками друзей (строго 4 человека) -->
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; width: 100%; box-sizing: border-box;">
          <div
              v-for="friend in targetUserFriends.slice(currentStartIndex, currentStartIndex + 4)"
              :key="friend.id"
              @click="handleNavigateToFriend(friend.id)"
              style="display: flex; flex-direction: column; align-items: center; padding: 12px; border-radius: 12px; border: 1px solid #f1f2f6; cursor: pointer; transition: all 0.2s; min-width: 0; box-sizing: border-box;"
              onmouseover="this.style.transform='translateY(-2px)'; this.style.borderColor='#54a0ff'; this.style.boxShadow='0 4px 12px rgba(84,160,255,0.08)'"
              onmouseout="this.style.transform='translateY(0)'; this.style.borderColor='#f1f2f6'; this.style.boxShadow='none'"
          >
            <div style="width: 44px; height: 44px; background-color: #f1f2f6; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #a4b0be; margin-bottom: 8px; flex-shrink: 0;">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <span style="font-size: 13px; font-weight: 600; color: #2c3e50; text-align: center; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
         {{ friend.firstName }}
       </span>
            <span style="font-size: 11px; color: #a4b0be; margin-top: 2px; text-align: center; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
         {{ friend.lastName }}
       </span>
          </div>

          <!-- Пустые карточки-заглушки, если у пользователя меньше 4 друзей, чтобы сетка не ломалась -->
          <div v-for="i in Math.max(0, 4 - targetUserFriends.slice(currentStartIndex, currentStartIndex + 4).length)" :key="'empty-'+i" style="visibility: hidden;"></div>
        </div>

        <!-- Кнопка Вправо (показывается только если впереди есть еще друзья) -->
        <button
            v-if="currentStartIndex + 4 < targetUserFriends.length"
            @click="currentStartIndex++"
            style="width: 32px; height: 32px; border-radius: 50%; border: 1px solid #edf2f7; background: white; color: #57606f; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: 0.2s; flex-shrink: 0;"
            onmouseover="this.style.background='#f1f2f6'"
            onmouseout="this.style.background='white'"
        >
          →
        </button>
        <div v-else style="width: 32px; flex-shrink: 0;"></div> <!-- Отступ-заглушка для выравнивания -->

      </div>
      <div v-else style="color: #a4b0be; font-size: 13px; text-align: center; padding: 10px 0;">
        У этого пользователя пока нет друзей.
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
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
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
                <svg width="22" height="22" viewBox="0 0 24 24" :fill="post.isLiked ? '#ff7675' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                </svg>
                <span style="font-size: 14px; font-weight: 600;">{{ post.likesCount || 0 }}</span>
              </button>
              <div v-if="post.showTooltip && post.likers && post.likers.length > 0" style="position: absolute; bottom: 30px; left: 0; background: #2c3e50; color: white; padding: 10px 14px; border-radius: 8px; font-size: 12px; white-space: nowrap; z-index: 100; box-shadow: 0 4px 15px rgba(0,0,0,0.15); display: flex; flex-direction: column; gap: 6px;">
                <div style="font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px; color: #54a0ff; text-align: left;">Оценили:</div>
                <div v-for="liker in post.likers" :key="liker.userId" style="opacity: 0.9;">Пользователь ID: {{ liker.userId }}</div>
              </div>
            </div>

            <!-- Кнопка комментариев -->
            <button v-if="post.commentsAllowed" @click="toggleCommentsBlock(post)" style="background: none; border: none; color: #a4b0be; cursor: pointer; display: flex; align-items: center; padding: 0; outline: none;" :style="{ color: post.showComments ? '#54a0ff' : '#a4b0be' }">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </button>

            <!-- Кнопка Поделиться -->
            <button style="background: none; border: none; color: #a4b0be; cursor: pointer; display: flex; align-items: center; padding: 0;">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="18" cy="5" r="3"></circle>
                <circle cx="6" cy="12" r="3"></circle>
                <circle cx="18" cy="19" r="3"></circle>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
              </svg>
            </button>
          </div>
        </div>

        <!-- БЛОК КОММЕНТАРИЕВ (ФИКСИРОВАННЫЙ ПО ШИРИНЕ) -->
        <div v-if="post.showComments" style="margin-top: 15px; border-top: 1px dashed #e1e8ed; padding-top: 15px; width: 100%; box-sizing: border-box; overflow: hidden;">

          <!-- Форма ввода комментария -->
          <div style="display: flex; gap: 10px; margin-bottom: 15px; width: 100%;">
            <input
                v-model="post.newCommentText"
                type="text"
                @focus="cancelAllCommentsEditing(post)"
                placeholder="Написать комментарий (2-100 симв.)..."
                style="flex: 1; padding: 10px 14px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 13px; outline: none; background: #f8f9fa; min-width: 0;"
            >
            <button @click="handleCreateComment(post)" style="padding: 10px 18px; background: #54a0ff; color: white; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; flex-shrink: 0;">Отправить</button>
          </div>

          <!-- Список комментариев -->
          <div v-if="post.commentsList && post.commentsList.length > 0" style="display: flex; flex-direction: column; gap: 10px; width: 100%;">
            <div v-for="comment in post.commentsList" :key="comment.id" style="background: #f8f9fa; padding: 14px 16px; border-radius: 12px; border: 1px solid #edf2f7; display: flex; flex-direction: column; gap: 6px; position: relative; width: 100%; box-sizing: border-box; text-align: left; overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; min-width: 0;">

              <!-- Крестик удаления -->
              <button v-if="comment.authorId === currentUserId && !comment.isEditing" @click="handleDeleteComment(post, comment.id)" style="position: absolute; right: 16px; top: 14px; background: none; border: none; color: #ff7675; font-size: 18px; cursor: pointer; opacity: 0.6; padding: 0; line-height: 1; outline: none;">×</button>

              <!-- Имя автора -->
              <div
                  @click="emit('open-user-profile', comment.authorId)"
                  style="font-size: 13px; font-weight: 700; color: #2c3e50; padding-right: 25px; cursor: pointer; display: inline-block; width: max-content; transition: 0.2s;"
                  onmouseover="this.style.color='#54a0ff'"
                  onmouseout="this.style.color='#2c3e50'"
                  title="Открыть профиль"
              >
                {{ comment.authorName }}
              </div>

              <!-- Текст -->
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

      </div>
      <div v-else style="text-align: center; color: #a4b0be; padding: 40px 0; font-size: 14px; background: white; border-radius: 12px; border: 1px solid #edf2f7;">
        У этого пользователя пока нет публичных записей.
      </div>
    </div>
</template>

    <script setup>import { ref, onMounted, watch } from 'vue'
    import axios from 'axios'

    // 💡 Пропсы компонента: получаем ID просматриваемого профиля и ID текущего залогиненного пользователя
    const props = defineProps({
      userId: { type: Number, required: true },
      currentUserId: { type: Number, required: true }
    })

    // 💡 Эмиты для связи с родителем: возврат назад и открытие новой страницы профиля
    const emit = defineEmits(['back', 'open-user-profile', 'open-user-friends-list', 'open-chat'])


    // 💡 Реактивные стейты для управления кнопками, профилем и публикациями автора
    const incomingRequestsIds = ref([]) // Хранит ID пользователей, которые прислали запрос текущему аккаунту
    const targetUser = ref(null)        // Данные профиля просматриваемого пользователя
    const userPosts = ref([])           // Массив публикаций на стене автора
    const isRequestSent = ref(false)    // Флаг, определяющий, отправили ли мы уже исходящий запрос

    const currentStartIndex = ref(0)

    // 💡 Массивы для управления доступом к просмотру списка чужих друзей
    const targetUserFriends = ref([])   // Список друзей открытого пользователя
    const myFriendsIds = ref([])        // Список ID наших личных друзей для сверки прав доступа

    // 💡 Базовые URL-адреса ваших микросервисов бэкенда на Spring Boot
    const API_USERS = 'http://localhost:8080/api/v1/social/users'
    const API_POSTS = 'http://localhost:8080/api/v1/social/posts'
    const API_LIKES = 'http://localhost:8080/api/v1/social/likes'
    const API_COMMENTS_PUBLIC = 'http://localhost:8080/api/v1/social/comments/public'
    const API_COMMENTS_PRIVATE = 'http://localhost:8080/api/v1/social/comments/private'

    // 💡 Главная функция загрузки: собирает данные профиля, проверяет связи из БД и подгружает посты с лайками
    const loadGuestData = async () => {
      if (!props.userId) return
      try {
        // 1. Загружаем основные данные профиля пользователя
        const userResponse = await axios.get(`${API_USERS}/${props.userId}`)
        targetUser.value = userResponse.data

        // 2. Проверяем кэш, чтобы понять, отправляли ли мы исходящую заявку
        const cached = JSON.parse(localStorage.getItem('cachedSentRequests')) || []
        isRequestSent.value = cached.map(Number).includes(Number(props.userId))

        // 3. Загружаем из базы данных входящие заявки, чтобы вовремя показать кнопки одобрения/отказа
        try {
          const incomingResponse = await axios.get('http://localhost:8080/api/v1/social/friends/requests/incoming', {
            params: { page: 0, size: 100 }
          })
          if (Array.isArray(incomingResponse.data)) {
            incomingRequestsIds.value = incomingResponse.data.map(req => Number(req.requesterId || req.userId1 || req.id))
          }
        } catch (e) {
          console.error(e)
        }

        // 4. Подтягиваем список НАШИХ друзей, чтобы проверить, имеем ли мы право заглядывать в чужой список друзей
        const myFriendsResponse = await axios.get(`http://localhost:8080/api/v1/social/friends/public/${props.currentUserId}`)
        if (Array.isArray(myFriendsResponse.data)) {
          myFriendsIds.value = myFriendsResponse.data.map(f => {
            return Number(f.userId1) === Number(props.currentUserId) ? Number(f.userId2) : Number(f.userId1)
          })
        }

        // 5. Если пользователь является нашим другом — запрашиваем список его друзей и подтягиваем их имена
        if (myFriendsIds.value.includes(Number(props.userId))) {
          const friendsResponse = await axios.get(`http://localhost:8080/api/v1/social/friends/public/${props.userId}`)
          if (Array.isArray(friendsResponse.data)) {
            const rawFriendsRelations = friendsResponse.data
            const friendsList = []
            for (let relation of rawFriendsRelations) {
              const friendId = Number(relation.userId1) === Number(props.userId) ? Number(relation.userId2) : Number(relation.userId1)
              if (friendId === Number(props.currentUserId)) continue // Пропускаем себя в чужом списке друзей
              try {
                const uRes = await axios.get(`${API_USERS}/${friendId}`)
                if (uRes.data) {
                  friendsList.push({ id: friendId, firstName: uRes.data.firstName, lastName: uRes.data.lastName })
                }
              } catch (e) { console.error(e) }
            }
            targetUserFriends.value = friendsList
          }
        } else {
          targetUserFriends.value = []
        }

        // 6. Загружаем посты автора стены и инициализируем реактивные поля для лайков/комментариев к каждому посту
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
    // 💡 Функция перехода: генерирует событие для открытия профиля другого пользователя при клике на его карточку
    const handleNavigateToFriend = (friendId) => {
      emit('open-user-profile', friendId)
    }

    // 💡 Отправка заявки: шлет POST-запрос на создание нового приглашения в друзья по ID адресата
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
        loadGuestData()
      } catch (error) {
        console.error(error)
      }
    }

    // 💡 Обработка входящего запроса: принимает или отклоняет заявку в друзья, отправляя статус ACCEPTED/REJECTED в PUT-метод
    const handleProcessIncomingRequest = async (actionStatus) => {
      try {
        const incomingResponse = await axios.get('http://localhost:8080/api/v1/social/friends/requests/incoming', {
          params: { page: 0, size: 100 }
        })
        const activeRequest = incomingResponse.data.find(req => Number(req.requesterId || req.userId1 || req.id) === Number(props.userId))
        if (activeRequest) {
          const requestId = activeRequest.id || activeRequest.requestId
          await axios.put(`http://localhost:8080/api/v1/social/friends/requests/${requestId}`, null, {
            params: { status: actionStatus }
          })
          alert(actionStatus === 'ACCEPTED' ? 'Заявка в друзья успешно принята!' : 'Заявка отклонена.')
          await loadGuestData()
        }
      } catch (error) {
        console.error(error)
      }
    }

    // 💡 Тумблер комментариев: открывает или скрывает блок обсуждения поста, запуская подгрузку данных
    const toggleCommentsBlock = async (post) => {
      post.showComments = !post.showComments
      if (post.showComments) {
        loadComments(post)
      }
    }

    // 💡 Загрузка комментариев: получает список ответов к публикации и параллельно запрашивает имена их авторов
    const loadComments = async (post) => {
      try {
        const response = await axios.get(`${API_COMMENTS_PUBLIC}/post/${post.id}`)
        const rawComments = response.data
        for (let comment of rawComments) {
          comment.isEditing = false
          comment.editContent = ''
          comment.isExpanded = false
          comment.authorName = `Пользователь ID: ${comment.authorId}`
          try {
            const userRes = await axios.get(`${API_USERS}/${comment.authorId}`)
            if (userRes.data) {
              comment.authorName = `${userRes.data.firstName} ${userRes.data.lastName}`
            }
          } catch (userError) {
            console.error(userError)
          }
        }
        post.commentsList = rawComments
      } catch (error) {
        console.error(error)
      }
    }

    // 💡 Создание комментария: проверяет длину текста (2-100 знаков) и шлет POST-запрос на добавление ответа
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

    // 💡 Старт редактирования: открывает инлайн-форму изменения комментария и заполняет её текущим текстом
    const startEditComment = (comment) => {
      comment.editContent = comment.content
      comment.isEditing = true
    }

    // 💡 Сброс редактирования: автоматически закрывает форму изменения у всех комментариев текущей публикации
    const cancelAllCommentsEditing = (post) => {
      if (post.commentsList && post.commentsList.length > 0) {
        post.commentsList.forEach(comment => {
          if (comment.isEditing) {
            comment.isEditing = false
            comment.editContent = ''
          }
        })
      }
    }

    // 💡 Обновление комментария: проверяет отредактированный текст и отправляет PUT-запрос в приватную ручку API
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

    // 💡 Удаление комментария: запрашивает подтверждение и отправляет DELETE-запрос на бэкенд
    const handleDeleteComment = async (post, commentId) => {
      if (!confirm('Вы уверены, что хотите удалить свой комментарий?')) return
      try {
        await axios.delete(`${API_COMMENTS_PRIVATE}/${commentId}`)
        loadComments(post)
      } catch (error) {
        console.error(error)
      }
    }

    // 💡 Превью лайкнувших: показывает всплывающую подсказку и подтягивает список из 3 случайных человек, оценивших пост
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

    // 💡 Переключение лайка: отправляет POST-запрос и динамически увеличивает или уменьшает счетчик на экране
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

    // 💡 Форматирование времени: преобразует системную ISO-дату публикации в красивый вид ЧЧ:ММ ДД.ММ
    const formatDateTime = (dateTimeString) => {
      if (!dateTimeString) return ''
      return new Date(dateTimeString).toLocaleString('ru-RU', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
    }

    // 💡 Наблюдатель за ID: автоматически перезапускает сбор данных, если мы переходим со страницы одного гостя к другому
    watch(() => props.userId, () => {
      currentStartIndex.value = 0 // Сбрасываем слайдер к первой странице
      loadGuestData()
    }, { immediate: true })

    //метод удаления
    const handleRemoveFriend = async (friendId) => {
      if (!confirm('Удалить пользователя из друзей?')) return
      try {
        await axios.delete('http://localhost:8080/api/v1/social/friends/private', {
          params: {
            userId1: props.currentUserId,
            userId2: friendId
          }
        })
        // Локально убираем ID из списка друзей, чтобы кнопка сразу переключилась на "Добавить в друзья"
        myFriendsIds.value = myFriendsIds.value.filter(id => Number(id) !== Number(friendId))
        alert('Пользователь удален из списка друзей.')
        await loadGuestData() // Перезапускаем сбор данных, чтобы обновить списки на странице
      } catch (error) {
        console.error('Ошибка при удалении друга:', error)
        alert('Не удалось удалить пользователя из друзей.')
      }
    }

    // 💡 Жизненный цикл: собирает все связи и посты в момент первоначального открытия экрана
    onMounted(() => {
      loadGuestData()
    })
    </script>