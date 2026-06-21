<template>
  <div :style="cardStyle">

    <!-- Переключатель вкладок -->
    <div style="display: flex; margin-bottom: 35px; border-bottom: 2px solid #f1f2f6;">
      <button @click="authMode = 'login'" :style="tabStyle(authMode === 'login')">Вход</button>
      <button @click="authMode = 'register'" :style="tabStyle(authMode === 'register')">Регистрация</button>
    </div>

    <!-- 1. ФОРМА ВХОДА -->
    <div v-if="authMode === 'login'">
      <div style="margin-bottom: 20px;">
        <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Email пользователя</label>
        <input v-model="loginData.username" type="text" placeholder="example@mail.com" style="width: 100%; padding: 14px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 15px; outline: none; background: #f8f9fa;">
      </div>
      <div style="margin-bottom: 35px;">
        <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Пароль</label>
        <input v-model="loginData.password" type="password" placeholder="••••••••" style="width: 100%; padding: 14px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 15px; outline: none; background: #f8f9fa;">
      </div>
      <button @click="handleLogin" style="width: 100%; padding: 14px; background: #2ecc71; color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: bold; cursor: pointer; box-shadow: 0 4px 12px rgba(46, 204, 113, 0.2);">
        Войти
      </button>
    </div>

    <!-- 2. СИНХРОНИЗИРОВАННАЯ ФОРМА РЕГИСТРАЦИИ (RegistrationUserDto) -->
    <div v-if="authMode === 'register'">

      <!-- Блок Имени и Фамилии в одну строку -->
      <div style="display: flex; gap: 16px; margin-bottom: 18px;">
        <div style="flex: 1;">
          <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Имя</label>
          <input v-model="registerData.firstName" type="text" placeholder="Иван" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa;">
        </div>
        <div style="flex: 1;">
          <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Фамилия</label>
          <input v-model="registerData.lastName" type="text" placeholder="Иванов" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa;">
        </div>
      </div>

      <!-- Email -->
      <div style="margin-bottom: 18px;">
        <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Email</label>
        <input v-model="registerData.email" type="email" placeholder="example@mail.com" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa;">
      </div>

      <!-- Номер телефона -->
      <div style="margin-bottom: 18px;">
        <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Номер телефона</label>
        <input v-model="registerData.numberPhone" type="text" placeholder="+79991234567" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa;">
        <small style="color: #a4b0be; font-size: 11px; display: block; margin-top: 4px;">Формат: +7 и 10 цифр без пробелов</small>
      </div>

      <!-- Дата рождения -->
      <div style="margin-bottom: 18px;">
        <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Дата рождения</label>
        <input v-model="registerData.birthday" type="date" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa; color: #2c3e50;">
      </div>

      <!-- Пароль -->
      <div style="margin-bottom: 35px;">
        <label style="display: block; margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #57606f;">Пароль</label>
        <input v-model="registerData.password" type="password" placeholder="••••••••" style="width: 100%; padding: 12px; border: 1px solid #ced6e0; border-radius: 8px; box-sizing: border-box; font-size: 14px; outline: none; background: #f8f9fa;">
        <small style="color: #a4b0be; font-size: 11px; display: block; margin-top: 4px; line-height: 1.3;">
          От 8 до 100 символов, английские буквы, минимум одна заглавная буква и одна цифра.
        </small>
      </div>

      <button @click="handleRegister" style="width: 100%; padding: 14px; background: #54a0ff; color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: bold; cursor: pointer; box-shadow: 0 4px 12px rgba(84, 160, 255, 0.2);">
        Зарегистрироваться
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'

const emit = defineEmits(['login-success'])
const API_BASE = 'http://localhost:8080/api/v1/social'

const authMode = ref('login')
const loginData = ref({ username: '', password: '' })

// Структура полей полностью совпадает с вашей Java-моделью RegistrationUserDto
const registerData = ref({
  firstName: '',
  lastName: '',
  email: '',
  numberPhone: '',
  password: '',
  birthday: '' // Поле типа HTML date возвращает строку формата 'YYYY-MM-DD', что идеально для LocalDate в Jackson
})

// Динамически меняем ширину карточки (форма регистрации больше, сделаем её чуть просторнее)
const cardStyle = computed(() => ({
  maxWidth: authMode.value === 'register' ? '520px' : '440px',
  margin: '50px auto',
  background: 'white',
  padding: '40px',
  borderRadius: '16px',
  boxShadow: '0 10px 30px rgba(164, 176, 190, 0.15)',
  border: '1px solid #edf2f7',
  transition: 'max-width 0.3s ease'
}))

const handleLogin = async () => {
  try {
    const response = await axios.post(`${API_BASE}/auth/login`, loginData.value)
    const jwt = response.data.token
    emit('login-success', jwt)
  } catch (error) {
    console.error(error)
    alert('Не удалось войти. Проверьте правильность заполнения полей.')
  }
}

const handleRegister = async () => {
  // Базовая проверка заполнения на фронтенде перед отправкой
  if (!registerData.value.firstName || !registerData.value.lastName || !registerData.value.birthday) {
    alert('Пожалуйста, заполните имя, фамилию и дату рождения!')
    return
  }

  try {
    const response = await axios.post(`${API_BASE}/registration/signUp`, registerData.value)
    alert('Пользователь успешно создан!')

    // Переключаемся на вкладку логина и автоматически подставляем email в поле ввода
    authMode.value = 'login'
    loginData.value.username = response.data.email || registerData.value.email

    // Сбрасываем форму регистрации
    registerData.value = { firstName: '', lastName: '', email: '', numberPhone: '', password: '', birthday: '' }
  } catch (error) {
    console.error(error)

    // Новая умная обработка ошибок валидации Spring Boot (@Valid)
    if (error.response && error.response.data) {
      const data = error.response.data

      // Если бэкенд возвращает стандартную структуру ошибок Spring Validation
      if (data.errors && Array.isArray(data.errors) && data.errors.length > 0) {
        alert('Ошибка валидации: ' + data.errors[0].defaultMessage)
      } else if (data.message) {
        alert('Ошибка: ' + data.message)
      } else {
        alert('Сервер отклонил запрос. Проверьте правильность заполнения полей.')
      }
    } else {
      alert('Нет связи с сервером.')
    }
  }
}

const tabStyle = (isActive) => ({
  flex: 1,
  padding: '14px',
  background: 'none',
  border: 'none',
  fontSize: '16px',
  fontWeight: '600',
  cursor: 'pointer',
  color: isActive ? '#54a0ff' : '#a4b0be',
  borderBottom: isActive ? '3px solid #54a0ff' : 'none',
  transition: '0.2s'
})
</script>
