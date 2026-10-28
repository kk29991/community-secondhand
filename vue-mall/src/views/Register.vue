<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入密码'))
  if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
  return callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_\u4e00-\u9fa5]+$/, message: '用户名仅支持中英文、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }],
  nickname: [{ max: 20, message: '昵称最多 20 个字符', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function handleRegister() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || form.username,
      phone: form.phone || null,
      email: form.email || null,
      role: 'USER'
    })
    await ElMessageBox.alert(
      `账号「${form.username}」注册成功, 请使用该账号登录。`,
      '注册成功',
      { confirmButtonText: '去登录', type: 'success' }
    ).catch(() => {})
    router.replace({ path: '/login', query: { username: form.username } })
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-box">
      <div class="auth-brand" @click="router.push('/')">
        <span class="logo-mark">二</span>
        <div>
          <h1>注册新账号</h1>
          <p>社区二手街 · 用户注册</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位, 登录时使用" :prefix-icon="'User'" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-32 位" :prefix-icon="'Lock'" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="'Lock'"
            show-password
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="选填, 不填则默认与用户名相同" :prefix-icon="'Postcard'" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填, 11 位手机号" :prefix-icon="'Iphone'" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填, 用于接收通知" :prefix-icon="'Message'" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号?
        <router-link to="/login">直接登录</router-link>
        <el-divider direction="vertical" />
        <router-link to="/">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  background: linear-gradient(135deg, #fff8f4 0%, #f5f5f5 45%, #ffe9d9 100%);
}

.auth-box {
  width: 460px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 18px 48px rgba(255, 103, 0, 0.16);
  padding: 34px 36px 26px;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  margin-bottom: 20px;
}

.logo-mark {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff8f40, #ff6700);
  color: #fff;
  font-weight: 700;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-brand h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.auth-brand p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #b0b0b0;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  border-radius: 23px;
}

.auth-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 13px;
  color: #9e9e9e;
}

.auth-footer a {
  color: #ff6700;
}

:deep(.el-form-item__label) {
  padding-bottom: 4px !important;
  color: #757575;
  font-size: 13px;
}
</style>
