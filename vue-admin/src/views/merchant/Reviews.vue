<template>
  <div class="page-container">
    <div class="page-title">评价管理</div>

    <div class="card">
      <!-- 查询工具条 -->
      <div class="toolbar">
        <el-radio-group v-model="replyFilter" @change="search">
          <el-radio-button value="">全部评价</el-radio-button>
          <el-radio-button value="false">待回复</el-radio-button>
          <el-radio-button value="true">已回复</el-radio-button>
        </el-radio-group>

        <el-select v-model="query.status" placeholder="显示状态" clearable style="width: 140px" @change="search">
          <el-option label="全部" :value="''" />
          <el-option label="显示中" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>

        <div class="spacer"></div>

        <el-button @click="loadList">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>

      <!-- 评价列表 -->
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="cell-product">
              <el-image
                :src="row.productImage"
                fit="cover"
                class="cell-thumb"
                :preview-src-list="[row.productImage]"
                preview-teleported
                hide-on-click-modal
              />
              <div class="cell-product-info">
                <div class="cell-product-name">{{ row.productName || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="买家" width="150">
          <template #default="{ row }">
            <div class="buyer">
              <el-avatar :size="28" :src="row.avatar">{{ (row.displayName || row.nickname || 'U').slice(0, 1) }}</el-avatar>
              <div class="buyer-info">
                <div>{{ row.displayName || row.nickname || '匿名用户' }}</div>
                <div v-if="row.anonymous === 1" class="text-muted anon">匿名评价</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.score) || 0" disabled size="small" />
            <div class="text-muted score-text">{{ row.score }} 分</div>
          </template>
        </el-table-column>

        <el-table-column label="评价内容" min-width="280">
          <template #default="{ row }">
            <div class="review-content">{{ row.content || '此用户没有填写评价内容' }}</div>
            <div v-if="parseImageList(row.images).length" class="review-images mt8">
              <el-image
                v-for="(img, i) in parseImageList(row.images)"
                :key="i"
                :src="img"
                fit="cover"
                :preview-src-list="parseImageList(row.images)"
                :initial-index="i"
                preview-teleported
                hide-on-click-modal
              />
            </div>
            <div class="text-muted mt8 time">{{ dateTime(row.createTime) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示中' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="我的回复" min-width="240">
          <template #default="{ row }">
            <div v-if="row.replyContent" class="reply-box">
              <div class="reply-content">{{ row.replyContent }}</div>
              <div class="text-muted reply-time">回复于 {{ dateTime(row.replyTime) }}</div>
            </div>
            <span v-else class="text-muted">暂未回复</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openReply(row)">
              {{ row.replyContent ? '修改回复' : '回复' }}
            </el-button>
            <el-button
              v-if="row.replyContent"
              link
              type="danger"
              size="small"
              @click="removeReply(row)"
            >
              删除回复
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无评价数据" />
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

    <!-- ------------------------------------------------------ 回复弹窗 -->
    <el-dialog
      v-model="replyVisible"
      :title="currentReview && currentReview.replyContent ? '修改回复' : '回复评价'"
      width="560px"
      destroy-on-close
    >
      <div v-if="currentReview" class="reply-quote">
        <div class="quote-head">
          <el-rate :model-value="Number(currentReview.score) || 0" disabled size="small" />
          <span class="text-muted">{{ currentReview.displayName || currentReview.nickname }}</span>
        </div>
        <div class="quote-content">{{ currentReview.content || '（无评价内容）' }}</div>
      </div>

      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="回复内容" prop="content">
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入回复内容，例如：感谢您的支持，我们会继续努力！"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi } from '@/api'
import { dateTime, parseImageList } from '@/utils/format'

const route = useRoute()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const replyFilter = ref('')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: ''
})

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (replyFilter.value !== '') params.hasReply = replyFilter.value
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await merchantApi.reviews(params)
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

/* ------------------------------------------------------------- 回复 */
const replyVisible = ref(false)
const replying = ref(false)
const replyFormRef = ref(null)
const currentReview = ref(null)
const replyForm = reactive({ id: null, content: '' })

const replyRules = {
  content: [
    { required: true, message: '请输入回复内容', trigger: 'blur' },
    { min: 2, max: 200, message: '回复内容长度为 2 - 200 个字符', trigger: 'blur' }
  ]
}

function openReply(row) {
  currentReview.value = row
  replyForm.id = row.id
  replyForm.content = row.replyContent || ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyFormRef.value) return
  try {
    await replyFormRef.value.validate()
  } catch (e) {
    return
  }

  replying.value = true
  try {
    await merchantApi.replyReview(replyForm.id, replyForm.content)
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadList()
  } catch (e) {
    // 已统一提示
  } finally {
    replying.value = false
  }
}

async function removeReply(row) {
  try {
    await ElMessageBox.confirm('确定要删除这条评价的回复吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch (e) {
    return
  }
  try {
    await merchantApi.deleteReply(row.id)
    ElMessage.success('回复已删除')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(() => {
  const hasReply = route.query.hasReply
  if (hasReply !== undefined && hasReply !== null && hasReply !== '') {
    replyFilter.value = String(hasReply)
  }
  loadList()
})
</script>

<style scoped>
.buyer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.buyer-info {
  min-width: 0;
  font-size: 13px;
}

.anon {
  font-size: 12px;
}

.score-text {
  font-size: 12px;
  margin-top: 2px;
}

.review-content {
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.review-images :deep(.el-image) {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  border: 1px solid var(--admin-border);
}

.mt8 {
  margin-top: 8px;
}

.time {
  font-size: 12px;
}

.reply-box {
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
  padding: 8px 10px;
}

.reply-content {
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.reply-time {
  font-size: 12px;
  margin-top: 4px;
}

.reply-quote {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.quote-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.quote-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  word-break: break-word;
}
</style>
