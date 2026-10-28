<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAddress, getAddressList, saveAddress, setDefaultAddress } from '@/api/index'

const loading = ref(false)
const error = ref('')
const list = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const rules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
    { max: 20, message: '姓名最多 20 个字符', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { max: 100, message: '详细地址最多 100 个字符', trigger: 'blur' }
  ]
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const data = await getAddressList()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
    error.value = '地址列表加载失败, 请稍后重试'
  } finally {
    loading.value = false
  }
}

function openDialog(item) {
  Object.assign(form, {
    id: item?.id ?? null,
    receiverName: item?.receiverName ?? '',
    receiverPhone: item?.receiverPhone ?? '',
    province: item?.province ?? '',
    city: item?.city ?? '',
    district: item?.district ?? '',
    detailAddress: item?.detailAddress ?? '',
    isDefault: item ? Number(item.isDefault) || 0 : list.value.length ? 0 : 1
  })
  dialogVisible.value = true
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
    await saveAddress({ ...form })
    ElMessage.success(form.id ? '地址已更新' : '地址已新增')
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function handleSetDefault(item) {
  if (item.isDefault === 1) return
  try {
    await setDefaultAddress(item.id)
    ElMessage.success('已设为默认地址')
    await loadList()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.receiverName}」的收货地址吗?`, '删除地址', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deleteAddress(item.id)
    ElMessage.success('地址已删除')
    await loadList()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(loadList)
</script>

<template>
  <div class="address-page">
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">收货地址</span>
        <el-button type="primary" @click="openDialog(null)">
          <el-icon><Plus /></el-icon> 新增收货地址
        </el-button>
      </div>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="loadList">重新加载</el-button>
        </template>
      </el-alert>

      <div v-loading="loading" element-loading-text="正在加载地址..." class="address-body">
        <div v-if="list.length" class="address-grid">
          <div v-for="item in list" :key="item.id" class="address-card" :class="{ default: item.isDefault === 1 }">
            <div class="card-head">
              <span class="receiver">{{ item.receiverName }}</span>
              <span class="phone">{{ item.receiverPhone }}</span>
              <el-tag v-if="item.isDefault === 1" size="small" type="danger" effect="dark">默认</el-tag>
            </div>
            <p class="detail">
              <el-icon><LocationInformation /></el-icon>
              {{ item.province }}{{ item.city }}{{ item.district }}{{ item.detailAddress }}
            </p>
            <div class="card-actions">
              <el-button size="small" @click="openDialog(item)">
                <el-icon><EditPen /></el-icon> 编辑
              </el-button>
              <el-button size="small" :disabled="item.isDefault === 1" @click="handleSetDefault(item)">
                <el-icon><Star /></el-icon> 设为默认
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(item)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-else-if="!loading" description="还没有收货地址, 添加一个吧">
          <el-button type="primary" @click="openDialog(null)">新增收货地址</el-button>
        </el-empty>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑收货地址' : '新增收货地址'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区">
          <div class="region-row">
            <el-input v-model="form.province" placeholder="省" />
            <el-input v-model="form.city" placeholder="市" />
            <el-input v-model="form.district" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="街道、门牌号、楼层等"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
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

.address-body {
  min-height: 180px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.address-card {
  border: 1px solid #eee;
  border-radius: var(--mall-radius);
  padding: 16px 18px;
  transition: all 0.2s;
}

.address-card:hover {
  border-color: #ffb380;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}

.address-card.default {
  border-color: #ff6700;
  background: #fff8f4;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.receiver {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.phone {
  font-size: 13px;
  color: #757575;
}

.detail {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin: 0 0 14px;
  font-size: 13px;
  color: #757575;
  line-height: 1.7;
  min-height: 44px;
}

.detail .el-icon {
  margin-top: 3px;
  color: #ff6700;
  flex-shrink: 0;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.region-row {
  display: flex;
  gap: 10px;
}

@media (max-width: 900px) {
  .address-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
