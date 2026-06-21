<template>
  <div style="background: white; padding: 20px; border-radius: 16px; box-shadow: 0 4px 20px rgba(164,176,190,0.06); border: 1px solid #edf2f7;">

    <!-- РЕЖИМ ПРОСМОТРА ПРОФИЛЯ -->
    <div v-if="!isEditing">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <div style="display: flex; align-items: center; gap: 14px;">
          <div style="width: 48px; height: 48px; background-color: #edf5ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #54a0ff;">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
          </div>
          <div v-if="profile">
            <div style="font-size: 16px; font-weight: 700; color: #2c3e50;">{{ profile.firstName }}</div>
            <div style="font-size: 16px; font-weight: 700; color: #2c3e50;">{{ profile.lastName }}</div>
          </div>
          <div v-else style="color: #a4b0be; font-size: 14px;">Загрузка...</div>
        </div>
        <button @click="isEditing = true" style="background: #f1f2f6; border: none; width: 36px; height: 36px; border-radius: 8px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #57606f; transition: 0.2s;" title="Настройки профиля">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
        </button>
      </div>
    </div>

    <!-- РЕЖИМ НАСТРОЕК -->
    <div v-else>
      <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #f8f9fa; padding-bottom: 12px; margin-bottom: 20px;">
        <h3 style="margin: 0; color: #2c3e50; font-size: 16px;">Настройки ⚙️</h3>
        <button @click="cancelEditing" style="padding: 6px 12px; background: #fff0f0; color: #ff7675; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 12px;">
          Назад
        </button>
      </div>

      <div style="display: flex; gap: 8px; margin-bottom: 20px; background: #f8f9fa; padding: 4px; border-radius: 8px;">
        <button @click="settingsMode = 'info'" :style="subTabStyle(settingsMode === 'info')">Данные</button>
        <button @click="settingsMode = 'password'" :style="subTabStyle(settingsMode === 'password')">Пароль</button>
      </div>

      <!-- ДАННЫЕ С БЛОКИРОВКОЙ ПО УМОЛЧАНИЮ -->
      <div v-if="settingsMode === 'info'">
        <div style="display: flex; gap: 12px; margin-bottom: 12px;">
          <div style="flex: 1;">
            <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Имя</label>
            <input v-model="updateData.firstName" type="text" :disabled="!isFieldsUnlocked" :style="inputStyle(isFieldsUnlocked)">
          </div>
          <div style="flex: 1;">
            <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Фамилия</label>
            <input v-model="updateData.lastName" type="text" :disabled="!isFieldsUnlocked" :style="inputStyle(isFieldsUnlocked)">
          </div>
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Email</label>
          <input v-model="updateData.email" type="email" :disabled="!isFieldsUnlocked" :style="inputStyle(isFieldsUnlocked)">
        </div>
        <div style="margin-bottom: 12px;">
          <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Телефон</label>
          <input v-model="updateData.numberPhone" type="text" placeholder="+7..." :disabled="!isFieldsUnlocked" :style="inputStyle(isFieldsUnlocked)">
        </div>
        <div style="margin-bottom: 20px;">
          <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Дата рождения</label>
          <input v-model="updateData.birthday" type="date" :disabled="!isFieldsUnlocked" :style="inputStyle(isFieldsUnlocked)" style="color: #2c3e50;">
        </div>

        <!-- УПРАВЛЯЮЩИЕ КНОПКИ -->
        <div style="display: flex; flex-direction: column; gap: 10px;">
          <button v-if="!isFieldsUnlocked" @click="isFieldsUnlocked = true" style="width: 100%; padding: 10px; background: #f1f2f6; color: #54a0ff; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; font-size: 14px;">
            🔓 Изменить личные данные
          </button>
          <button v-else @click="handleUpdateProfile" style="width: 100%; padding: 10px; background: #54a0ff; color: white; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; font-size: 14px; box-shadow: 0 4px 12px rgba(84,160,255,0.2);">
            💾 Сохранить изменения
          </button>
        </div>
      </div>

      <!-- ИЗМЕНЕНИЕ ПАРОЛЯ -->
      <div v-if="settingsMode === 'password'">
        <div style="margin-bottom: 12px;">
          <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Текущий пароль</label>
          <input v-model="passwordData.oldPassword" type="password" placeholder="••••••••" style="width: 100%; padding: 8px 12px; border: 1px solid #ced6e0; border-radius: 6px; box-sizing: border-box; background: #f8f9fa; outline: none; font-size: 14px;">
        </div>
        <div style="margin-bottom: 20px;">
          <label style="display: block; margin-bottom: 4px; font-size: 12px; font-weight: 600; color: #57606f;">Новый пароль</label>
          <input v-model="passwordData.newPassword" type="password" placeholder="••••••••" style="width: 100%; padding: 8px 12px; border: 1px solid #ced6e0; border-radius: 6px; box-sizing: border-box; background: #f8f9fa; outline: none; font-size: 14px;">
        </div>
        <button @click="handleUpdatePassword" style="width: 100%; padding: 10px; background: #2ecc71; color: white; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; font-size: 14px; box-shadow: 0 4px 12px rgba(46,204,113,0.15);">
          Обновить пароль
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const emit = defineEmits(['profile-loaded'])
const API_BASE = 'http://localhost:8080/api/v1/social/users'

const isEditing = ref(false)
const settingsMode = ref('info')
const isFieldsUnlocked = ref(false) // Новое состояние разблокировки полей

const profile = ref(null)
const updateData = ref({ firstName: '', lastName: '', email: '', numberPhone: '', birthday: '' })
const passwordData = ref({ oldPassword: '', newPassword: '' })

const loadProfile = async () => {
  try {
    const response = await axios.get(`${API_BASE}/me`)
    profile.value = response.data

    updateData.value = {
      firstName: response.data.firstName,
      lastName: response.data.lastName,
      email: response.data.email,
      numberPhone: response.data.numberPhone,
      birthday: response.data.birthday
    }
    emit('profile-loaded', response.data)
  } catch (error) {
    console.error('Ошибка загрузки профиля:', error)
  }
}

const handleUpdateProfile = async () => {
  try {
    const response = await axios.put(`${API_BASE}/me`, updateData.value)
    alert('Профиль успешно обновлен!')
    profile.value = response.data
    isFieldsUnlocked.value = false // Снова блокируем поля
    isEditing.value = false
    loadProfile()
  } catch (error) {
    console.error(error)
    alert('Ошибка при обновлении данных.')
  }
}

const handleUpdatePassword = async () => {
  if (!passwordData.value.oldPassword || !passwordData.value.newPassword) {
    alert('Заполните оба поля!')
    return
  }
  try {
    await axios.put(`${API_BASE}/me/pass`, passwordData.value)
    alert('Пароль успешно изменен!')
    passwordData.value = { oldPassword: '', newPassword: '' }
    isEditing.value = false
  } catch (error) {
    console.error(error)
    alert('Не удалось изменить пароль.')
  }
}

const cancelEditing = () => {
  isEditing.value = false
  isFieldsUnlocked.value = false
  passwordData.value = { oldPassword: '', newPassword: '' }
}

// Динамический стиль для инпутов (меняет прозрачность и цвет текста при блокировке)
const inputStyle = (unlocked) => ({
  width: '100%',
  padding: '8px 12px',
  border: '1px solid #ced6e0',
  borderRadius: '6px',
  boxSizing: 'border-box',
  background: unlocked ? '#ffffff' : '#f1f2f6',
  color: unlocked ? '#2c3e50' : '#a4b0be',
  outline: 'none',
  fontSize: '14px',
  transition: '0.2s',
  cursor: unlocked ? 'text' : 'not-allowed'
})

const subTabStyle = (isActive) => ({
  flex: 1,
  padding: '8px',
  background: isActive ? 'white' : 'none',
  border: 'none',
  borderRadius: '6px',
  fontSize: '14px',
  fontWeight: '600',
  cursor: 'pointer',
  color: isActive ? '#54a0ff' : '#a4b0be',
  boxShadow: isActive ? '0 2px 8px rgba(164,176,190,0.1)' : 'none',
  transition: '0.2s'
})

onMounted(() => {
  loadProfile()
})

// Функция принудительного закрытия настроек извне
const closeSettings = () => {
  isEditing.value = false
  isFieldsUnlocked.value = false
  passwordData.value = { oldPassword: '', newPassword: '' }
}

// Экспортируем метод наружу, чтобы App.vue мог его вызвать
defineExpose({
  closeSettings
})
</script>
