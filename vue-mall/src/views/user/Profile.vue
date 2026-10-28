<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updatePassword, updateProfile, uploadImage } from '@/api/index'
import { useUserStore } from '@/stores/user'
import { DEFAULT_IMAGE, fallbackImage, formatTime } from '@/utils/placeholder'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const saving = ref(false)
const uploading = ref(false)
const pwdDialogVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref(null)

const source = computed(() => userStore.profile?.nickname ? userStore.profile : userStore.user || {})

const form = reactive({
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  gender: 0
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  nickname: [{ max: 20, message: '昵称最多 20 个字符', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const validateNewPwd = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入新密码'))
  if (value.length < 6 || value.length > 32) return callback(new Error('新密码长度为 6-32 位'))
  if (value === pwdForm.oldPassword) return callback(new Error('新密码不能与原密码相同'))
  return callback()
}

const validateConfirmPwd = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== pwdForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  return callback()
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validateNewPwd, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPwd, trigger: 'blur' }]
}

const genderText = computed(() => ({ 0: '保密', 1: '男', 2: '女' }[Number(source.value.gender) || 0]))

function syncForm() {
  form.nickname = source.value.nickname || ''
  form.avatar = source.value.avatar || ''
  form.phone = source.value.phone || ''
  form.email = source.value.email || ''
  form.gender = Number(source.value.gender) || 0
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await updateProfile({ ...form })
    userStore.patchProfile({ ...form })
    ElMessage.success('资料已更新')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!/^image\//.test(raw.type)) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }
  uploading.value = true
  try {
    const url = await uploadImage(raw)
    form.avatar = typeof url === 'string' ? url : url?.url || ''
    if (form.avatar) ElMessage.success('头像上传成功, 请点击「保存修改」提交')
  } catch (e) {
    ElMessage.warning('头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleUpdatePassword() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }
  pwdSaving.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功, 请重新登录')
    pwdDialogVisible.value = false
    await userStore.logout()
    router.replace('/login')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    pwdSaving.value = false
  }
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

onMounted(async () => {
  await userStore.loadProfile(true)
  syncForm()
})
</script>

<template>
  <div class="profile-page">
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">个人资料</span>
        <el-button text type="primary" @click="syncForm">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </div>

      <el-alert
        v-if="userStore.profileError"
        class="mb16"
        type="warning"
        show-icon
        :closable="false"
        title="资料同步失败"
        description="未能从服务端获取最新资料, 下方展示的是最近一次登录缓存的账号信息。请检查网络或重新登录后再试。"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="profile-form">
        <el-form-item label="头像">
          <div class="avatar-row">
            <img class="avatar-preview" :src="form.avatar || DEFAULT_IMAGE" alt="头像" @error="fallbackImage($event, form.nickname || '用户')" />
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept="image/*"
              :on-change="handleAvatarChange"
            >
              <el-button :loading="uploading" size="small">
                <el-icon><Upload /></el-icon> 更换头像
              </el-button>
            </el-upload>
            <span class="tip">支持 jpg/png, 不超过 2MB</span>
          </div>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input :model-value="source.username" disabled />
          <span class="tip">用户名注册后不可修改</span>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
          <el-button @click="openPwdDialog">
            <el-icon><Key /></el-icon> 修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">账号信息</span>
      </div>
      <div class="info-grid">
        <div class="info-item"><span>用户 ID</span><em>{{ source.id ?? '-' }}</em></div>
        <div class="info-item"><span>账号角色</span><em>{{ source.role === 'USER' ? '普通用户' : source.role || '普通用户' }}</em></div>
        <div class="info-item"><span>性别</span><em>{{ genderText }}</em></div>
        <div class="info-item"><span>购物车商品</span><em>{{ userStore.cartCount }} 件</em></div>
        <div class="info-item"><span>登录状态</span><em>{{ userStore.isLogin ? '已登录' : '未登录' }}</em></div>
        <div class="info-item"><span>当前时间</span><em>{{ formatTime(new Date().toISOString()) }}</em></div>
      </div>
    </div>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="460px" append-to-body>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="88px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="6-32 位新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="handleUpdatePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 20px 24px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-title {
  font-size: 16px;
  font-weight: 500;
  position: relative;
  padding-left: 12px;
}

.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: #ff6700;
}

.mb16 {
  margin-bottom: 16px;
}

.profile-form {
  max-width: 560px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.avatar-preview {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  background: #fafafa;
  border: 2px solid #fff2e6;
}

.tip {
  font-size: 12px;
  color: #b0b0b0;
  margin-left: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  background: #f0f0f0;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.info-item {
  background: #fff;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-item span {
  font-size: 12px;
  color: #b0b0b0;
}

.info-item em {
  font-style: normal;
  font-size: 14px;
  color: #333;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
