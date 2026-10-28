<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '',
  password: '',
  role: 'USER'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ]
}

const redirect = ref('/')

async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password, role: 'USER' })
    ElMessage.success(`欢迎回来, ${userStore.nickname}`)
    router.replace(redirect.value || '/')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function fillDemo() {
  form.username = 'zhangsan'
  form.password = '123456'
}

onMounted(() => {
  if (route.query.redirect) redirect.value = String(route.query.redirect)
  if (route.query.tab) activeTab.value = String(route.query.tab)
  if (userStore.isLogin) {
    redirect.value = '/'
  }
})
</script>

<template>
  <div class="auth-page">
    <div class="auth-box">
      <div class="auth-brand" @click="router.push('/')">
        <span class="logo-mark">二</span>
        <div>
          <h1>社区二手街</h1>
          <p>社区二手交易平台 · 用户前台</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="auth-tabs" stretch>
        <el-tab-pane label="账号登录" name="login">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="handleLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="'User'" clearable @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="'Lock'"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="auth-extra">
            <span>还没有账号?</span>
            <router-link to="/register">立即注册</router-link>
            <el-divider direction="vertical" />
            <a href="javascript:;" @click="fillDemo">填入演示账号 (zhangsan / 123456)</a>
          </div>
        </el-tab-pane>

        <el-tab-pane label="没有账号? 去注册" name="register">
          <div class="register-guide">
            <el-icon class="guide-icon"><UserFilled /></el-icon>
            <h3>创建你的平台账号</h3>
            <p>注册后即可加入购物车、下单、收藏商品、发表评价, 并使用 AI 导购助手。</p>
            <el-button type="primary" size="large" class="submit-btn" @click="router.push('/register')">
              前往注册页面
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="auth-footer">
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
  width: 420px;
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
  margin-bottom: 22px;
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

.auth-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  border-radius: 23px;
}

.auth-extra {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #9e9e9e;
  flex-wrap: wrap;
  margin-top: 4px;
}

.auth-extra a {
  color: #ff6700;
}

.register-guide {
  text-align: center;
  padding: 12px 0 6px;
}

.guide-icon {
  font-size: 44px;
  color: #ff6700;
  margin-bottom: 12px;
}

.register-guide h3 {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 500;
  color: #333;
}

.register-guide p {
  margin: 0 0 20px;
  font-size: 13px;
  color: #9e9e9e;
  line-height: 1.8;
}

.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
}

.auth-footer a {
  color: #9e9e9e;
}

.auth-footer a:hover {
  color: #ff6700;
}
</style>
