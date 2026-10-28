<template>
  <div class="page-container">
    <div class="page-title">轮播图管理</div>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.position" placeholder="展示位置" clearable style="width: 160px" @change="search">
          <el-option label="全部位置" :value="''" />
          <el-option
            v-for="p in BANNER_POSITIONS"
            :key="p.value"
            :label="p.label"
            :value="p.value"
          />
        </el-select>

        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="search">
          <el-option label="全部状态" :value="''" />
          <el-option label="已启用" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>

        <div class="spacer"></div>

        <el-button type="primary" @click="openForm()">
          <el-icon><Plus /></el-icon>新增轮播图
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="预览图" width="200">
          <template #default="{ row }">
            <el-image
              :src="row.image"
              fit="cover"
              class="banner-thumb"
              :preview-src-list="[row.image]"
              preview-teleported
              hide-on-click-modal
            />
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || '-' }}</template>
        </el-table-column>

        <el-table-column label="跳转链接" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-muted">{{ row.linkUrl || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="位置" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.position === 1 ? 'primary' : 'warning'" size="small" effect="plain">
              {{ bannerPositionText(row.position) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序" width="80" align="center" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170">
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
          <el-empty description="暂无轮播图数据" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- ------------------------------------------------- 新增/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑轮播图' : '新增轮播图'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入轮播图标题" maxlength="60" show-word-limit />
        </el-form-item>

        <el-form-item label="图片" prop="image">
          <ImageUpload
            v-model="form.image"
            :limit="1"
            tip="建议尺寸 900x360，支持 jpg/png，单张不超过 5MB"
          />
        </el-form-item>

        <el-form-item label="跳转链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="例如：/front/goodsDetail?pid=17" maxlength="200" />
        </el-form-item>

        <el-form-item label="展示位置" prop="position">
          <el-radio-group v-model="form.position">
            <el-radio-button v-for="p in BANNER_POSITIONS" :key="p.value" :value="p.value">
              {{ p.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
            :step="1"
            step-strictly
            controls-position="right"
          />
          <span class="text-muted ml10">数字越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
          />
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
import ImageUpload from '@/components/ImageUpload.vue'
import { adminApi } from '@/api'
import { BANNER_POSITIONS, bannerPositionText, dateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  position: '',
  status: ''
})

const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  id: null,
  title: '',
  image: '',
  linkUrl: '',
  position: 1,
  sort: 0,
  status: 1
})

const form = reactive(defaultForm())

const rules = {
  title: [
    { required: true, message: '请输入轮播图标题', trigger: 'blur' },
    { min: 2, max: 60, message: '标题长度为 2 - 60 个字符', trigger: 'blur' }
  ],
  image: [{ required: true, message: '请上传轮播图片', trigger: 'change' }],
  linkUrl: [
    { required: true, message: '请输入跳转链接', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback()
        if (value.length > 200) return callback(new Error('链接长度不能超过 200 个字符'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  position: [{ required: true, message: '请选择展示位置', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }]
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.position !== '' && query.position !== null) params.position = query.position
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await adminApi.banners(params)
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadList()
}

function reset() {
  query.position = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function openForm(row) {
  Object.assign(form, defaultForm())
  if (row) {
    form.id = row.id
    form.title = row.title || ''
    form.image = row.image || ''
    form.linkUrl = row.linkUrl || ''
    form.position = row.position ?? 1
    form.sort = row.sort ?? 0
    form.status = row.status ?? 1
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
      title: form.title,
      image: form.image,
      linkUrl: form.linkUrl,
      position: form.position,
      sort: form.sort,
      status: form.status
    }
    if (form.id) payload.id = form.id

    await adminApi.saveBanner(payload)
    ElMessage.success(form.id ? '轮播图已更新' : '轮播图已创建')
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
    await ElMessageBox.confirm(`确定要删除轮播图「${row.title || row.id}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch (e) {
    return
  }
  try {
    await adminApi.deleteBanner(row.id)
    ElMessage.success('轮播图已删除')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(loadList)
</script>

<style scoped>
.banner-thumb {
  width: 160px;
  height: 64px;
  border-radius: 6px;
  border: 1px solid var(--admin-border);
  display: block;
}

.ml10 {
  margin-left: 10px;
  font-size: 12px;
}
</style>
