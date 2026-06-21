<template>
  <div style="background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.08); border: 1px solid #edf2f7; max-width: 760px; width: 100%; box-sizing: border-box;">    <h3 style="margin-top: 0; color: #2c3e50; font-size: 18px; margin-bottom: 20px;">Поиск людей по параметрам 🔍</h3>

    <!-- ОСНОВНЫЕ ПОЛЯ ПОИСКА (ИМЯ И ФАМИЛИЯ) -->
    <div style="display: flex; gap: 14px; margin-bottom: 16px;">
      <div style="flex: 1;">
        <label style="display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: #57606f;">Имя</label>
        <input v-model="filter.firstName" type="text" placeholder="Иван" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 14px; outline: none; background: #f8f9fa;">
      </div>
      <div style="flex: 1;">
        <label style="display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: #57606f;">Фамилия</label>
        <input v-model="filter.lastName" type="text" placeholder="Иванов" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 14px; outline: none; background: #f8f9fa;">
      </div>
    </div>

    <!-- ТУМБЛЕР ДЛЯ ДОПОЛНИТЕЛЬНЫХ ФИЛЬТРОВ -->
    <div style="margin-bottom: 20px;">
      <button @click="showAdvanced = !showAdvanced" style="background: none; border: none; color: #54a0ff; font-weight: 600; cursor: pointer; font-size: 13px; padding: 0;">
        {{ showAdvanced ? '▼ Скрыть дополнительные фильтры' : '► Показать дополнительные фильтры' }}
      </button>
    </div>

    <!-- РАСШИРЕННЫЕ ФИЛЬТРЫ -->
    <div v-if="showAdvanced" style="background: #f8f9fa; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 1px solid #edf2f7; display: flex; flex-direction: column; gap: 16px;">
      <div>
        <label style="display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: #57606f;">Номер телефона</label>
        <input v-model="filter.numberPhone" type="text" placeholder="+79991234567" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 14px; outline: none; background: white;">
      </div>

      <div style="display: flex; gap: 14px;">
        <div style="flex: 1;">
          <label style="display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: #57606f;">Дата рождения (От)</label>
          <input v-model="filter.birthdayFrom" type="date" style="width: 100%; padding: 11px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 14px; outline: none; background: white; color: #2c3e50;">
        </div>
        <div style="flex: 1;">
          <label style="display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: #57606f;">Дата рождения (До)</label>
          <input v-model="filter.birthdayTo" type="date" style="width: 100%; padding: 11px; border: 1px solid #ced6e0; border-radius: 8px; font-size: 14px; outline: none; background: white; color: #2c3e50;">
        </div>
      </div>
    </div>

    <!-- КНОПКА ЗАПУСКА ПОИСКА -->
    <button @click="handleSearch" style="width: 100%; padding: 14px; background: #54a0ff; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; font-size: 15px; box-shadow: 0 4px 12px rgba(84,160,255,0.2);">
      Найти пользователей
    </button>

    <!-- СПИСОК РЕЗУЛЬТАТОВ ПОИСКА -->
    <div v-if="searchResults.length > 0" style="margin-top: 30px; display: flex; flex-direction: column; gap: 14px;">
      <h4 style="margin: 0 0 5px 0; color: #2c3e50; font-size: 15px;">Результаты поиска:</h4>

      <div v-for="user in searchResults" :key="user.userId" style="padding: 16px 20px; background: #f4fbf7; border-radius: 12px; border: 1px solid #bbf7d0; display: flex; justify-content: space-between; align-items: center;">
        <div>
          <!-- ИСПРАВЛЕНО: Теперь имя и фамилия кликабельны, имеют указатель-курсор и красивый ховер-эффект смены цвета на синий -->
          <p
              @click="emit('open-user-profile', user.userId)"
              style="margin: 0 0 6px 0; font-size: 16px; font-weight: 700; color: #2ecc71; cursor: pointer; transition: color 0.2s; display: inline-block;"
              onmouseover="this.style.color='#54a0ff'"
              onmouseout="this.style.color='#2ecc71'"
              title="Открыть профиль пользователя"
          >
            {{ user.firstName }} {{ user.lastName }}
          </p>
          <p style="margin: 0; font-size: 13px; color: #718096;">
            ID: {{ user.userId }} <span v-if="user.numberPhone">| Тел: {{ user.numberPhone }}</span> <span v-if="user.email">| Email: {{ user.email }}</span>
          </p>
        </div>

        <!-- УМНАЯ СМЕНА КНОПКИ НА ПОМЕТКУ -->
        <div>
          <!-- Если ID пользователя есть в списке отправленных — выводим пастельную надпись -->
          <span v-if="sentRequests.includes(user.userId)" style="font-size: 13px; background: #fef3c7; color: #d97706; padding: 10px 18px; border-radius: 8px; font-weight: 600; display: inline-block;">
            ⏳ Запрос отправлен
          </span>
          <!-- Если запроса еще не было — показываем зеленую кнопку добавления -->
          <button v-else @click="sendFriendRequest(user.userId)" style="padding: 10px 18px; background: #2ecc71; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 13px; box-shadow: 0 4px 10px rgba(46,204,113,0.15);">
            Добавить в друзья
          </button>
        </div>

      </div>
    </div>

    <div v-else-if="searchExecuted" style="text-align: center; color: #a4b0be; padding: 30px 0; font-size: 14px; margin-top: 20px;">
      Пользователи с такими параметрами не найдены.
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import axios from 'axios'

// ДОБАВЛЕНО: Объявляем событие open-user-profile, чтобы связываться с App.vue
const emit = defineEmits(['open-user-profile'])

const showAdvanced = ref(false)
const searchExecuted = ref(false)
const searchResults = ref([])

const sentRequests = ref(JSON.parse(localStorage.getItem('cachedSentRequests')) || [])

const filter = ref({
  firstName: '',
  lastName: '',
  numberPhone: null,
  birthdayFrom: null,
  birthdayTo: null,
  timeStamp: null
})

const API_BASE = 'http://localhost:8080/api/v1/social'

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

    const response = await axios.get(`${API_BASE}/users/search`, {
      params: {
        ...cleanedFilter,
        page: 0,
        size: 10
      }
    })

    searchResults.value = response.data
    searchExecuted.value = true
  } catch (error) {
    console.error('Ошибка при поиске пользователей:', error)
    alert('Не удалось выполнить поиск. Проверьте формат данных.')
  }
}

const sendFriendRequest = async (addresseeId) => {
  try {
    await axios.post(`${API_BASE}/friends/requests/${addresseeId}`)

    sentRequests.value.push(addresseeId)
    localStorage.setItem('cachedSentRequests', JSON.stringify(sentRequests.value))

    alert('Заявка в друзья успешно отправлена!')
  } catch (error) {
    console.error(error)
    alert('Не удалось отправить заявку.')
  }
}
</script>
