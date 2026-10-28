<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌区 -->
      <div class="brand">
        <div class="brand-logo">🛒</div>
        <h1 class="brand-title">社区二手交易平台</h1>
        <p class="brand-sub">后台管理端</p>
        <ul class="brand-features">
          <li>商家：商品 · 订单 · 评价 · 标签</li>
          <li>管理员：用户 · 商家 · 分类 · 内容</li>
          <li>Vue 3 + Vite + Element Plus + ECharts</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-area">
        <h2 class="form-title">账号登录</h2>
        <p class="form-sub">请选择登录身份后输入账号密码</p>

        <el-radio-group v-model="form.role" class="role-switch" size="large">
          <el-radio-button value="MERCHANT">商家登录</el-radio-button>
          <el-radio-button value="ADMIN">管理员登录</el-radio-button>
        </el-radio-group>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable :prefix-icon="User" />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert :closable="false" type="info" class="demo-tip">
          <template #title>
            <div class="demo-line">
              <b>测试账号</b>
            </div>
            <div class="demo-line">
              商家：<code v-for="(m, i) in merchantDemos" :key="m.username">
                <template v-if="i > 0"> 、</template>{{ m.username }} / merchant123
              </code>
              <span v-if="!merchantDemos.length" class="demo-loading">加载中…</span>
            </div>
            <div class="demo-line">管理员：<code>admin / admin123</code></div>
          </template>
        </el-alert>

        <div class="quick-fill">
          <el-button link type="primary" @click="fill('MERCHANT')">
            填入商家账号<template v-if="merchantDemos.length > 1">
              （{{ merchantDemos[merchantIndex].storeName }}）
            </template>
          </el-button>
          <el-divider direction="vertical" />
          <el-button link type="primary" @click="fill('ADMIN')">填入管理员账号</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { publicApi } from '@/api/index'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  role: 'MERCHANT'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 30, message: '用户名长度为 2 - 30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为 6 - 30 个字符', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择登录身份', trigger: 'change' }]
}

const DEMO = {
  MERCHANT: { username: 'xunxi', password: 'merchant123' },
  ADMIN: { username: 'admin', password: 'admin123' }
}

/**
 * 商家测试账号从后端读取, 避免把用户名硬编码在前端
 * （商家改名或新增后, 登录页提示会自动同步）。
 */
const merchantDemos = ref([])
const merchantIndex = ref(0)

async function loadMerchantDemos() {
  try {
    const list = await publicApi.demoMerchants()
    if (Array.isArray(list) && list.length) {
      merchantDemos.value = list
      DEMO.MERCHANT.username = list[0].username
    }
  } catch (e) {
    // 接口不可用时保留默认账号, 不阻塞登录
  }
}

onMounted(loadMerchantDemos)

function fill(role) {
  form.role = role
  if (role === 'MERCHANT' && merchantDemos.value.length) {
    // 多次点击可轮流填入不同商家, 方便逐个查看
    const item = merchantDemos.value[merchantIndex.value % merchantDemos.value.length]
    merchantIndex.value = (merchantIndex.value + 1) % merchantDemos.value.length
    form.username = item.username
    form.password = 'merchant123'
    return
  }
  form.username = DEMO[role].username
  form.password = DEMO[role].password
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    const data = await userStore.login({ ...form })
    ElMessage.success(`欢迎回来，${data.nickname || data.username}`)

    // 按角色跳转: 商家 /dashboard, 管理员 /admin/overview
    const redirect = route.query.redirect
    const target = userStore.homePath
    if (redirect && typeof redirect === 'string' && redirect.startsWith('/')) {
      // 仅当目标路由属于当前角色时才跟随 redirect
      const isAdminPath = redirect.startsWith('/admin')
      if (isAdminPath === userStore.isAdmin && redirect !== '/login') {
        router.replace(redirect)
        return
      }
    }
    router.replace(target)
  } catch (e) {
    // 错误提示已由 request.js 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6700 0%, #ff9a44 45%, #ffb37a 100%);
  padding: 20px;
}

.login-card {
  width: 900px;
  max-width: 100%;
  min-height: 520px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.22);
}

/* 左侧品牌 */
.brand {
  width: 380px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #2b2f3a 0%, #3a4050 100%);
  color: #fff;
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  font-size: 46px;
  line-height: 1;
  margin-bottom: 18px;
}

.brand-title {
  font-size: 27px;
  margin: 0 0 8px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 15px;
  color: #ff6700;
  margin: 0 0 30px;
  letter-spacing: 3px;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 13px;
  color: #b8bcc6;
  line-height: 2.1;
}

.brand-features li::before {
  content: '·';
  color: #ff6700;
  font-weight: 700;
  margin-right: 8px;
}

/* 右侧表单 */
.form-area {
  flex: 1;
  padding: 48px 46px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.form-title {
  font-size: 23px;
  margin: 0 0 6px;
  color: #303133;
}

.form-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 22px;
}

.role-switch {
  width: 100%;
  margin-bottom: 22px;
  display: flex;
}

.role-switch :deep(.el-radio-button) {
  flex: 1;
}

.role-switch :deep(.el-radio-button__inner) {
  width: 100%;
}

.submit-btn {
  width: 100%;
  letter-spacing: 4px;
  font-weight: 600;
}

.demo-tip {
  margin-top: 4px;
}

.demo-line {
  line-height: 1.9;
  font-size: 12px;
}

.demo-line code {
  background: rgba(255, 103, 0, 0.1);
  color: #cc5200;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: Consolas, Monaco, monospace;
}

.quick-fill {
  margin-top: 12px;
  text-align: center;
}

@media (max-width: 820px) {
  .brand {
    display: none;
  }
  .login-card {
    width: 100%;
    min-height: auto;
  }
  .form-area {
    padding: 34px 26px;
  }
}
</style>
