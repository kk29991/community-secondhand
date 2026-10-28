<template>
  <div class="page-container">
    <div class="page-title">公告管理</div>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="公告标题关键词"
          clearable
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="query.status" placeholder="发布状态" clearable style="width: 140px" @change="search">
          <el-option label="全部状态" :value="''" />
          <el-option label="已发布" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>

        <div class="spacer"></div>

        <el-button type="primary" @click="openForm()">
          <el-icon><Plus /></el-icon>发布公告
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag v-if="row.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
              <span class="title-text">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="announcementTypeTag(row.type)" size="small">
              {{ announcementTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="内容摘要" min-width="280">
          <template #default="{ row }">
            <span class="text-muted summary">{{ row.content }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.publishTime || row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无公告数据" />
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
      :title="form.id ? '编辑公告' : '发布公告'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="60" show-word-limit />
        </el-form-item>

        <el-form-item label="公告类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button
              v-for="t in ANNOUNCEMENT_TYPES"
              :key="t.value"
              :value="t.value"
            >
              {{ t.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="1000"
            show-word-limit
            placeholder="请输入公告正文内容"
          />
        </el-form-item>

        <el-form-item label="是否置顶" prop="isTop">
          <el-switch
            v-model="form.isTop"
            :active-value="1"
            :inactive-value="0"
            active-text="置顶"
            inactive-text="普通"
            inline-prompt
          />
        </el-form-item>

        <el-form-item label="发布状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="发布"
            inactive-text="下架"
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
import { adminApi } from '@/api'
import { ANNOUNCEMENT_TYPES, announcementTypeTag, announcementTypeText, dateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = () => ({ id: null, title: '', content: '', type: 1, isTop: 0, status: 1 })
const form = reactive(defaultForm())

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 2, max: 60, message: '标题长度为 2 - 60 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { min: 5, max: 1000, message: '内容长度为 5 - 1000 个字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await adminApi.announcements(params)
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
  query.keyword = ''
  query.status = ''
  query.pageNum = 1
  loadList()
}

function openForm(row) {
  Object.assign(form, defaultForm())
  if (row) {
    form.id = row.id
    form.title = row.title
    form.content = row.content
    form.type = row.type ?? 1
    form.isTop = row.isTop ?? 0
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
      content: form.content,
      type: form.type,
      isTop: form.isTop,
      status: form.status
    }
    if (form.id) payload.id = form.id

    await adminApi.saveAnnouncement(payload)
    ElMessage.success(form.id ? '公告已更新' : '公告已发布')
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
    await ElMessageBox.confirm(`确定要删除公告「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch (e) {
    return
  }
  try {
    await adminApi.deleteAnnouncement(row.id)
    ElMessage.success('公告已删除')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(loadList)
</script>

<style scoped>
.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-weight: 500;
}

.summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
}
</style>
