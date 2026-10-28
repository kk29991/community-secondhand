<template>
  <div class="page-container">
    <div class="page-title">标签管理</div>

    <div class="card">
      <div class="toolbar">
        <span class="text-muted">
          店铺标签用于商品详情展示，例如「服务体验优秀」「细节处理得当」。共
          {{ list.length }} 个标签。
        </span>
        <div class="spacer"></div>
        <el-button type="primary" @click="openForm()">
          <el-icon><Plus /></el-icon>新增标签
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column label="标签名称" min-width="180">
          <template #default="{ row }">
            <el-tag :color="row.color || '#ff6700'" effect="dark" style="border: none">
              {{ row.name }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="标签颜色" width="140">
          <template #default="{ row }">
            <div class="color-cell">
              <span class="color-dot" :style="{ background: row.color || '#ff6700' }"></span>
              <span class="text-muted">{{ row.color || '#ff6700' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序" width="100" align="center" />

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="还没有标签，点击右上角新增" />
        </template>
      </el-table>
    </div>

    <!-- ------------------------------------------------- 新增/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑标签' : '新增标签'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：服务体验优秀" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="标签颜色" prop="color">
          <el-color-picker v-model="form.color" :predefine="predefineColors" />
          <span class="text-muted color-value">{{ form.color }}</span>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" :step="1" step-strictly controls-position="right" />
          <span class="text-muted sort-tip">数字越小越靠前</span>
        </el-form-item>

        <el-form-item label="预览">
          <el-tag :color="form.color || '#ff6700'" effect="dark" style="border: none">
            {{ form.name || '标签预览' }}
          </el-tag>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi } from '@/api'
import { dateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])

const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const predefineColors = [
  '#ff6700',
  '#52c41a',
  '#13c2c2',
  '#1890ff',
  '#722ed1',
  '#eb2f96',
  '#faad14',
  '#f5222d',
  '#909399'
]

const defaultForm = () => ({ id: null, name: '', color: '#ff6700', sort: 0 })

const form = reactive(defaultForm())

const rules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 2, max: 20, message: '标签名称长度为 2 - 20 个字符', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择标签颜色', trigger: 'change' },
    {
      pattern: /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/,
      message: '颜色格式应为 #RRGGBB',
      trigger: 'change'
    }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入排序值'))
        } else if (Number(value) < 0) {
          callback(new Error('排序值不能为负数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function loadList() {
  loading.value = true
  try {
    const data = await merchantApi.tags()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  Object.assign(form, defaultForm())
  if (row) {
    form.id = row.id
    form.name = row.name
    form.color = row.color || '#ff6700'
    form.sort = row.sort ?? 0
  }
  formVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      color: form.color,
      sort: form.sort
    }
    if (form.id) payload.id = form.id

    await merchantApi.saveTag(payload)
    ElMessage.success(form.id ? '标签已更新' : '标签已创建')
    formVisible.value = false
    loadList()
  } catch (e) {
    // 已统一提示
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定要删除标签「${row.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch (e) {
    return
  }
  try {
    await merchantApi.deleteTag(row.id)
    ElMessage.success('标签已删除')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(loadList)
</script>

<style scoped>
.color-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1px solid var(--admin-border);
  display: inline-block;
}

.color-value {
  margin-left: 10px;
  font-size: 13px;
}

.sort-tip {
  margin-left: 10px;
  font-size: 12px;
}
</style>
