package org.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

@Configuration
@EnableWebFlux
public class GatewayConfig {

@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
return builder.routes()
.route("user", r -> r
.path("/api/v1/social/users/**")
.uri("http://localhost:8081"))

.route("register", r -> r
.path("/api/v1/social/registration/**")
.uri("http://localhost:8081"))

.route("friend", r -> r
.path("/api/v1/social/friends/**")
.uri("http://localhost:8082"))

.route("post", r -> r
.path("/api/v1/social/posts/**")
.uri("http://localhost:8083"))

.route("comment", r -> r
.path("/api/v1/social/comments/**")
.uri("http://localhost:8084"))

.route("like", r -> r
.path("/api/v1/social/likes/**")
.uri("http://localhost:8085"))

.route("security", r -> r
.path("/api/v1/social/auth/**")
.uri("http://localhost:8888"))
.route("chat", r -> r
.path("/api/v1/social/chats/**", "/ws/**")
.uri("http://localhost:8087"))
.route("notification", r -> r
.path("/api/v1/social/notifications/**", "/ws-notif/**")
.uri("http://localhost:8086"))
.build();
}
}

package org.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

@Bean
public CorsWebFilter corsWebFilter() {
CorsConfiguration config = new CorsConfiguration();
config.setAllowCredentials(true);
config.addAllowedOrigin("http://localhost:5173");
config.addAllowedOrigin("http://localhost:5174");
config.addAllowedHeader("*");
config.addAllowedMethod("*");

UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

// 1. Применяем глобальный CORS ко всем стандартным REST-запросам API
source.registerCorsConfiguration("/api/**", config);

// 2. Применяем глобальный CORS к чату (если там в WebSocketConfig вы НЕ прописывали домены)
source.registerCorsConfiguration("/ws/**", config);
return new CorsWebFilter(source);
}

}
package org.example.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
// Регистрируем ту же точку входа для вебсокетов, что и в чате
registry.addEndpoint("/ws-notif") // 👈 Сделайте путь уникальным!
.setAllowedOriginPatterns("*")
.withSockJS();
}

@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
// Настраиваем префиксы очередей
registry.enableSimpleBroker("/queue", "/topic");
registry.setUserDestinationPrefix("/user");
}

// 💡 ДОБАВИТЬ: Перехватчик для авторизации WebSocket-сессий
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
registration.interceptors(new ChannelInterceptor() {
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
// 💡 Читаем заголовок, который мы только что отправили из App.vue
String userId = accessor.getFirstNativeHeader("X-User-Id");

if (userId != null) {
final String finalUserId = userId;
accessor.setUser(new Principal() {
@Override
public String getName() {
return finalUserId;
}
});
// Лог в консоль микросервиса для проверки, что юзер распознан:
System.out.println("[WebSocket-Сессия] Пользователь " + finalUserId + " успешно авторизован в WebSocket!");
} else {
System.out.println("[WebSocket-Сессия] ПРЕДУПРЕЖДЕНИЕ: Заголовок X-User-Id не найден в CONNECT!");
}
}
return message;
}
});
}

}
<template>
  <!-- ГЛОБАЛЬНАЯ ОБЕРТКА (Задает фон и центрирует все элементы сайта) -->
  <div style="width: 100%; min-height: 100vh; background-color: #f8f9fa; box-sizing: border-box; margin: 0; padding: 0; display: flex; flex-direction: column; align-items: center;">

    <!-- ШАПКА ПРИЛОЖЕНИЯ (Теперь она имеет ту же ширину 1416px, что и контент, и центрируется) -->
    <header style="background: #ffffff; color: #2c3e50; padding: 18px 40px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e1e8ed; box-shadow: 0 2px 4px rgba(0,0,0,0.01); width: 1416px; max-width: 100%; box-sizing: border-box; border-radius: 0 0 16px 16px; margin-bottom: 40px;">
      <h1 style="margin: 0; font-size: 26px; font-weight: 700; color: #54a0ff; letter-spacing: 0.5px; display: flex; align-items: center; gap: 8px; cursor: pointer;" @click="navigateTo('feed')">
        Social <span style="font-size: 20px;">🌐</span>
      </h1>
      <div v-if="token && currentUsername" style="display: flex; align-items: center; gap: 24px;">
        <span style="font-weight: 500; color: #57606f; font-size: 15px;">Привет, <strong style="color: #2c3e50;">{{ currentUsername }}</strong>!</span>
        <button @click="handleLogout" style="padding: 10px 20px; background: #ff7675; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 14px; box-shadow: 0 4px 10px rgba(255, 118, 117, 0.2); transition: 0.2s;">Выйти</button>
      </div>
    </header>

    <!-- КОНТЕНТНАЯ ОБЛАСТЬ (Зафиксирована строго на 1416px в один ряд с шапкой) -->
    <div style="width: 1416px; max-width: 100%; padding: 0 24px; box-sizing: border-box; font-family: 'Segoe UI', system-ui, sans-serif; margin-bottom: 40px;">

      <!-- СЦЕНАРИЙ 1: АВТОРИЗАЦИЯ -->
      <div v-if="!token">
        <AuthModule @login-success="onLoginSuccess" />
      </div>

      <!-- СЦЕНАРИЙ 2: РАБОЧИЙ ИНТЕРФЕЙС -->
      <div v-else :style="{ display: currentSection === 'chat' ? 'block' : 'grid' }" style="grid-template-columns: 280px 1fr; gap: 36px; align-items: start; width: 100%; box-sizing: border-box;">

        <!-- ЛЕВАЯ КОЛОНКА (Скрывается полностью, если открыт чат) -->
        <aside v-if="currentSection !== 'chat'" style="display: flex; flex-direction: column; gap: 20px; position: sticky; top: 20px; width: 280px; box-sizing: border-box; flex-shrink: 0;">          <ProfileModule ref="profileModuleRef" @profile-loaded="onProfileLoaded" />

          <div style="background: white; padding: 20px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7; display: flex; flex-direction: column; gap: 10px; width: 100%; box-sizing: border-box;">
            <h4 style="margin: 0 0 5px 0; color: #a4b0be; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Навигация</h4>

            <button @click="navigateTo('feed')" :style="menuButtonStyle(currentSection === 'feed')">
              🏠 Моя лента
            </button>

            <button @click="() => {
               // Если мы уже находимся в разделе связей, но смотрим ЧУЖИХ друзей — сбрасываем просмотр на свои заявки
               if (currentSection === 'friends' && targetFriendsUserId) {
                 targetFriendsUserId = null;
               } else {
                 navigateTo('friends');
               }
             }" :style="menuButtonStyle(currentSection === 'friends' && !targetFriendsUserId)"
                    style="display: flex; justify-content: space-between; align-items: center; width: 100%;"
            >
              <span>👥 Друзья и заявки</span>
              <span v-if="incomingCount > 0" style="background: #ff7675; color: white; min-width: 18px; height: 18px; padding: 2px 6px; border-radius: 10px; font-size: 11px; font-weight: bold; display: flex; align-items: center; justify-content: center; line-height: 1; flex-shrink: 0; margin-left: 8px;">
   {{ incomingCount }}
 </span>
            </button>

            <button @click="navigateTo('search')" :style="menuButtonStyle(currentSection === 'search')">
              🔍 Поиск людей
            </button>

            <button
                @click="navigateTo('chat')"
                :style="menuButtonStyle(currentSection === 'chat')"
                style="display: flex; justify-content: space-between; align-items: center; width: 100%;"
            >
              <span>💬 Сообщения</span>

              <!-- КРАСНЫЙ КРУЖОК С КОЛИЧЕСТВОМ НЕПРОЧИТАННЫХ ПИСЕМ -->
              <span v-if="unreadMessagesCount > 0" style="background: #ff7675; color: white; min-width: 18px; height: 18px; padding: 2px 6px; border-radius: 10px; font-size: 11px; font-weight: bold; display: flex; align-items: center; justify-content: center; line-height: 1; flex-shrink: 0; margin-left: 8px;">
     {{ unreadMessagesCount }}
   </span>
            </button>
            <button
                @click="navigateTo('notifications')"
                :style="menuButtonStyle(currentSection === 'notifications')"
                style="display: flex; justify-content: space-between; align-items: center; width: 100%;"
            >
              <span>🔔 Уведомления</span>

              <!-- Пастельно-красный кружок с числом -->
              <span v-if="unreadNotificationsCount > 0" style="background: #ff7675; color: white; min-width: 18px; height: 18px; padding: 2px 6px; border-radius: 10px; font-size: 11px; font-weight: bold; display: flex; align-items: center; justify-content: center; line-height: 1; flex-shrink: 0; margin-left: 8px;">
    {{ unreadNotificationsCount }}
  </span>
            </button>
          </div>
        </aside>

        <!-- ПРАВАЯ КОЛОНКА -->
        <main :style="{ width: currentSection === 'chat' ? '100%' : '100%' }" style="display: flex; flex-direction: column; gap: 30px; box-sizing: border-box; flex-shrink: 0; min-width: 0;">


          <!-- СЕКЦИЯ 1: МОЯ ЛЕНТА -->
          <div v-if="currentSection === 'feed'" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; align-items: stretch;">
            <PostModule
                v-if="myUserId"
                :currentUserId="myUserId"
                :currentUserInfo="fullProfileObject"
                :scrollToPostId="targetPostId"
                :openCommentsTarget="autoOpenComments"
                @open-user-profile="viewUserProfile"
                @post-scrolled="() => { targetPostId = null; autoOpenComments = false; }"
            />
          </div>

          <!-- СЕКЦИЯ 2: ДРУЗЬЯ И ЗАЯВКИ -->
          <div v-else-if="currentSection === 'friends'" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; align-items: stretch;">
            <FriendModule
                v-if="myUserId"
                :currentUserId="myUserId"
                :targetUserId="targetFriendsUserId"
                :targetUserName="targetFriendsUserName"
                @open-user-profile="viewUserProfile"
                @update-count="updateIncomingCount"
                @back-to-profile="() => { currentSection = 'user-profile'; targetFriendsUserId = null; }"
                @open-chat="(id) => { activeChatUserId = id; currentSection = 'chat'; }"
            />
          </div>

          <!-- СЕКЦИЯ 3: ПОИСК ЛЮДЕЙ -->
          <div v-else-if="currentSection === 'search'" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; align-items: stretch;">
            <SearchModule
                @open-user-profile="viewUserProfile"
                @open-chat="(id) => { activeChatUserId = id; currentSection = 'chat'; }"
            />
          </div>

          <!-- СЕКЦИЯ 4: МЕССЕНДЖЕР -->
          <div v-else-if="currentSection === 'chat'" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; align-items: stretch;">
            <ChatModule
                v-if="myUserId"
                :currentUserId="myUserId"
                :preselectedUserId="activeChatUserId"
                @message-read="loadUnreadMessagesCount"
                @go-back="() => { currentSection = 'feed'; activeChatUserId = null; }"
                @open-user-profile="viewUserProfile"
            />
          </div>

          <!-- СЕКЦИЯ 5: УВЕДОМЛЕНИЯ -->
          <div v-else-if="currentSection === 'notifications'" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; align-items: stretch;">
            <NotificationModule
                v-if="myUserId"
                :currentUserId="myUserId"
                @open-user-profile="viewUserProfile"
                @update-unread-count="(count) => { unreadNotificationsCount.value = count; }"
                @open-post="(postId, shouldOpenComments) => {
                   targetPostId = postId;
                   autoOpenComments = shouldOpenComments;
                   currentSection = 'feed';
                }"
            />
          </div>

          <!-- СЕКЦИЯ: СТРАНИЦА ЧУЖОГО ПРОФИЛЯ -->
          <div v-else-if="currentSection === 'user-profile'" style="width: 100%; box-sizing: border-box; display: flex; flex-direction: column; align-items: stretch;">
            <UserProfileModule
                v-if="selectedUserId && myUserId"
                :key="selectedUserId"
                :userId="selectedUserId"
                :currentUserId="myUserId"
                @back="goBack"
                @open-user-profile="viewUserProfile"
                @open-user-friends-list="(id, name) => { targetFriendsUserId = id; targetFriendsUserName = name; currentSection = 'friends'; }"
                @open-chat="(id) => { activeChatUserId = id; currentSection = 'chat'; }"
            />
          </div>
        </main>

      </div>
    </div>
    <!-- ХАБ ВСПЛЫВАЮЩИХ ЖИВЫХ УВЕДОМЛЕНИЙ (Отображается поверх всего сайта в правом углу) -->
    <div style="position: fixed; bottom: 30px; right: 30px; display: flex; flex-direction: column; gap: 12px; z-index: 9999; max-width: 360px; width: 100%;">
      <transition-group name="toast-fade">
        <div
            v-for="toast in activeToasts"
            :key="toast.id"
            @click="activeToasts = activeToasts.filter(t => t.id !== toast.id)"
            style="background: #2c3e50; color: white; padding: 16px 20px; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.2); border-left: 4px solid #54a0ff; display: flex; align-items: center; gap: 12px; cursor: pointer; text-align: left; box-sizing: border-box; width: 100%;"
        >
          <!-- Иконка события -->
          <div style="font-size: 20px; flex-shrink: 0;">
            {{ toast.type === 'POST_LIKE' ? '❤️' : toast.type === 'NEW_COMMENT' ? '💬' : '👤' }}
          </div>

          <!-- Текст события -->
          <div style="flex: 1; min-width: 0;">
            <div style="font-size: 13px; font-weight: 700; color: #54a0ff; margin-bottom: 2px;">Новое событие</div>
            <div style="font-size: 13px; color: #ffffff; line-height: 1.3;">
              <strong>{{ toast.senderName }}</strong>
              {{ toast.type === 'POST_LIKE' ? 'оценил вашу публикацию' : toast.type === 'NEW_COMMENT' ? 'прокомментировал вашу запись' : 'хочет добавить вас в друзья' }}
            </div>
          </div>
        </div>
      </transition-group>
    </div>
  </div>
</template>


<script setup>
import { ref, nextTick } from 'vue'
import axios from 'axios'
import AuthModule from './components/AuthModule.vue'
import ProfileModule from './components/ProfileModule.vue'
import PostModule from './components/PostModule.vue'
import FriendModule from './components/FriendModule.vue'
import SearchModule from './components/SearchModule.vue'
import UserProfileModule from './components/UserProfileModule.vue'
import ChatModule from './components/ChatModule.vue'
import NotificationModule from './components/NotificationModule.vue'

/* @ts-ignore */
import SockJS from 'sockjs-client'
/* @ts-ignore */
import { Stomp } from '@stomp/stompjs'

const token = ref(localStorage.getItem('token') || '')
const currentUsername = ref('')
const myUserId = ref(null)
const currentSection = ref(localStorage.getItem('currentSection') || 'feed')

const selectedUserId = ref(Number(localStorage.getItem('selectedUserId')) || null)
const previousSection = ref(localStorage.getItem('previousSection') || 'feed')

// ДОБАВЛЕНО: Безопасная реактивная переменная для счетчика входящих заявок
const incomingCount = ref(Number(localStorage.getItem('incomingCount')) || 0)

const targetFriendsUserName = ref('') // Для хранения имени и фамилии
const targetFriendsUserId = ref(null)
const activeChatUserId = ref(null) // Хранит ID друга, с которым открываем диалог

const unreadMessagesCount = ref(Number(localStorage.getItem('unreadMessagesCount')) || 0)

const fullProfileObject = ref(null)
const profileModuleRef = ref(null)

const unreadNotificationsCount = ref(0) // Хранит количество непрочитанных пушей
const targetPostId = ref(null)      // Хранит ID публикации для скролла
const autoOpenComments = ref(false) // Сигнал для раскрытия блока комментариев

const activeToasts = ref([]) // Хранит активные всплывающие пуши на экране

const setAuthHeader = (jwtToken) => {
  if (jwtToken) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${jwtToken}`
  } else {
    delete axios.defaults.headers.common['Authorization']
  }
}

if (token.value) {
  setAuthHeader(token.value)
}

// Функция обновления счетчика, которую дочерний FriendModule будет вызывать через события
const updateIncomingCount = (count) => {
  incomingCount.value = Number(count)
  localStorage.setItem('incomingCount', count)
}

const navigateTo = (sectionName) => {
  currentSection.value = sectionName
  localStorage.setItem('currentSection', sectionName)
  if (sectionName !== 'chat') {
    activeChatUserId.value = null
    // 💡 ОБНОВЛЯЕМ СЧЁТЧИК: Запрашиваем у бэкенда количество писем, когда уходим из чата
    loadUnreadMessagesCount()
  }
  if (sectionName !== 'user-profile') {
    selectedUserId.value = null
    localStorage.removeItem('selectedUserId')
  }
  if (sectionName !== 'friends') {
    targetFriendsUserId.value = null
  }
  if (profileModuleRef.value) {
    profileModuleRef.value.closeSettings()
  }
  if (sectionName === 'notifications') {
    unreadNotificationsCount.value = 0 // Убираем красный маркер при переходе в хаб
  }
}

const viewUserProfile = async (userId) => {
  if (!userId) return

  if (Number(userId) === Number(myUserId.value)) {
    navigateTo('feed')
    return
  }

  if (currentSection.value === 'user-profile') {
    selectedUserId.value = Number(userId)
    localStorage.setItem('selectedUserId', userId)

    currentSection.value = ''
    await nextTick()
    currentSection.value = 'user-profile'
    return
  }

  previousSection.value = currentSection.value
  localStorage.setItem('previousSection', currentSection.value)

  selectedUserId.value = Number(userId)
  localStorage.setItem('selectedUserId', userId)

  currentSection.value = 'user-profile'
  localStorage.setItem('currentSection', 'user-profile')
}

const goBack = () => {
  navigateTo(previousSection.value)
}

const onLoginSuccess = (jwt) => {
  token.value = jwt
  localStorage.setItem('token', jwt)
  setAuthHeader(jwt)
}

const onProfileLoaded = (profileData) => {
  currentUsername.value = profileData.firstName || profileData.username
  myUserId.value = profileData.id || profileData.userId
  fullProfileObject.value = profileData

  // Запрашиваем системное разрешение на пуши у пользователя сразу при входе!
  if (Notification.permission !== "granted" && Notification.permission !== "denied") {
    Notification.requestPermission();
  }

  // Загружаем счётчик заявок сразу, как только узнали ID пользователя
  loadIncomingFriendsCount()
  loadUnreadMessagesCount()
  checkNewNotifications()
  initNotificationWebSocket()
}

// Функция для загрузки количества заявок (Добавьте в App.vue)
const loadIncomingFriendsCount = async () => {
  if (!myUserId.value) return
  try {
    const tokenValue = localStorage.getItem('token')

    // Вызываем точный эндпоинт входящих заявок, как в модуле друзей
    const res = await axios.get('http://localhost:8080/api/v1/social/friends/requests/incoming', {
      headers: { Authorization: `Bearer ${tokenValue}` }
    })

    // Сервер возвращает массив заявок. Берем его длину.
    if (Array.isArray(res.data)) {
      incomingCount.value = res.data.length
      localStorage.setItem('incomingCount', res.data.length)
      console.log("Счётчик заявок успешно инициализирован:", res.data.length)
    }
  } catch (err) {
    console.error('Не удалось загрузить счетчик заявок при старте:', err)
  }
}

const loadUnreadMessagesCount = async () => {
  if (!myUserId.value) return
  try {
    const tokenValue = localStorage.getItem('token')
    const res = await axios.get('http://localhost:8080/api/v1/social/chats/unread/count', {
      headers: { Authorization: `Bearer ${tokenValue}` }
    })

    unreadMessagesCount.value = Number(res.data)
    localStorage.setItem('unreadMessagesCount', res.data)
  } catch (err) {
    console.error('Не удалось загрузить счетчик сообщений:', err)
  }
}

const handleLogout = () => {
  sessionStorage.clear()
  localStorage.removeItem('cachedSentRequests')

  token.value = ''
  currentUsername.value = ''
  myUserId.value = null
  fullProfileObject.value = null
  currentSection.value = 'feed'
  selectedUserId.value = null
  incomingCount.value = 0

  localStorage.removeItem('token')
  localStorage.removeItem('currentSection')
  localStorage.removeItem('selectedUserId')
  localStorage.removeItem('previousSection')
  localStorage.removeItem('incomingCount')
  setAuthHeader(null)
  // ОЧИСТКА ПАМЯТИ БРАУЗЕРА:
  window.location.reload();
}

//фоновый метод для проверки новых уведомлений
const checkNewNotifications = async () => {
  if (!myUserId.value) return
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('http://localhost:8080/api/v1/social/notifications', {
      params: { page: 0, size: 50 },
      headers: { Authorization: `Bearer ${token}` }
    })

    // 💡 ИСПРАВЛЕНО: Считаем только те уведомления, у которых поле read равно false
    unreadNotificationsCount.value = response.data.filter(n => !n.read).length

  } catch (e) {
    console.error('Ошибка при фоновом расчете уведомлений:', e)
  }
}
// 💡 Функция воспроизведения звука "пилик"
const playNotificationSound = () => {
  console.log("[WebSocket-ЗВУК] Попытка воспроизведения звука уведомления...");

  const audio = new Audio('/notification.mp3')
  audio.volume = 0.5 // Чуть-чуть прибавим громкость для теста

  // Принудительно перезагружаем аудио-поток в памяти браузера
  audio.load()

  audio.play()
      .then(() => {
        console.log("[WebSocket-ЗВУК] Звук 'пилик' успешно воспроизведен!");
      })
      .catch(e => {
        console.warn("[WebSocket-ЗВУК] Браузер заблокировал автоматический звук. Нажмите в любом месте экрана (кликните), чтобы разрешить сайту издавать звуки!", e)
      })
}

// 💡 Инициализация WebSocket для живых уведомлений в App.vue
// 💡 Исправленная инициализация WebSocket с авторизацией и отключением отладки
const initNotificationWebSocket = () => {
  if (!myUserId.value) return

  // 1. Получаем JWT токен из хранилища
  const tokenValue = localStorage.getItem('token')

  // 2. Создаем SockJS соединение
  const socket = new SockJS(`http://localhost:8080/ws-notif?token=${tokenValue}`)

  // 3. Инициализируем Stomp клиент
  const stompClient = Stomp.over(socket)

  // 💡 ИСПРАВЛЕНИЕ ОШИБКИ ТИПОВ: В новой библиотеке логи отключаются пустой функцией, а не null!
  stompClient.debug = () => {}

  // 4. Передаем токен авторизации в Headers при коннекте, чтобы шлюз (Gateway) не выдавал 401 ошибку
  const headers = {
    Authorization: tokenValue ? `Bearer ${tokenValue}` : '',
    'X-User-Id': String(myUserId.value) // 👈 Передаем ID текущего авторизованного юзера
  }

  stompClient.connect(headers, () => {
    console.log("[WebSocket-УВЕДОМЛЕНИЯ] Успешно подключено к серверу через Шлюз!");

    // Подписываемся на персональную очередь уведомлений
    stompClient.subscribe(`/user/${myUserId.value}/queue/notifications`, (message) => {
      console.log("[WebSocket-ТЕСТ] Данные успешно долетели до фронтенда:", message.body);

      // 1. Парсим объект уведомления, пришедший с бэкенда
      const newNotif = JSON.parse(message.body);

      // 2. ВРЕМЕННО: Жестко прописываем имя, чтобы axios не ломал нам логику
      newNotif.senderName = newNotif.senderName || 'Кто-то';

      // Инкрементируем счетчик на панели навигации
      unreadNotificationsCount.value++;

      // 3. Генерируем уникальный ID для тоста и пушим в массив
      const toastId = Date.now();
      activeToasts.value.push({ id: toastId, ...newNotif });

      console.log("[WebSocket-ТЕСТ] Тост добавлен в массив. Текущий массив тостов:", activeToasts.value);

      // Удаляем тост через 5 секунд
      setTimeout(() => {
        activeToasts.value = activeToasts.value.filter(t => t.id !== toastId);
      }, 5000);
    });
  }, (err) => {
    console.error("[WebSocket-УВЕДОМЛЕНИЯ] Ошибка WebSocket, перезапуск через 5 сек...", err)
    setTimeout(initNotificationWebSocket, 5000)
  })
}
// Функция проигрывания звука и показа системного пуша
const showSystemNotification = (title, body) => {
  // Проверяем, разрешил ли пользователь уведомления в браузере
  if (Notification.permission === "granted") {
    new Notification(title, {
      body: body,
      icon: "/logo.png" // путь к иконке вашего сайта
    });
  } else if (Notification.permission !== "denied") {
    // Если еще не спрашивали, запрашиваем разрешение
    Notification.requestPermission();
  }
};
const menuButtonStyle = (isActive) => ({
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
  width: '100%',
  padding: '12px 16px',
  background: isActive ? '#edf5ff' : 'transparent',
  border: 'none',
  borderRadius: '12px',
  fontSize: '15px',
  fontWeight: '600',
  cursor: 'pointer',
  color: isActive ? '#54a0ff' : '#2c3e50',
  textAlign: 'left',
  transition: 'all 0.2s ease',
  boxSizing: 'border-box',
  fontFamily: 'inherit'
})

</script>

<style>
@keyframes slideIn {
  from { opacity: 0; transform: translateX(50px) scale(0.9); }
  to { opacity: 1; transform: translateX(0) scale(1); }
}
.toast-fade-enter-active {
  transition: all 0.3s ease;
  animation: slideIn 0.3s ease forwards;
}
.toast-fade-leave-active {
  transition: all 0.3s ease;
}
</style>