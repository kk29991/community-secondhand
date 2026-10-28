<template>
  <div class="page-container">
    <div class="page-title">评价管理</div>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" placeholder="显示状态" clearable style="width: 150px" @change="search">
          <el-option label="全部状态" :value="''" />
          <el-option label="显示中" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>

        <div class="spacer"></div>

        <el-button @click="loadList">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />

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
              <el-avatar :size="28" :src="row.avatar">
                {{ (row.displayName || row.nickname || 'U').slice(0, 1) }}
              </el-avatar>
              <div class="buyer-info">
                <div>{{ row.anonymous === 1 ? '匿名用户' : row.displayName || row.nickname || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="140">
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

        <el-table-column label="商家回复" min-width="200">
          <template #default="{ row }">
            <div v-if="row.replyContent" class="reply-box">
              <div class="reply-content">{{ row.replyContent }}</div>
              <div class="text-muted reply-time">{{ dateTime(row.replyTime) }}</div>
            </div>
            <span v-else class="text-muted">暂无回复</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示中' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '隐藏' : '显示' }}
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { dateTime, parseImageList } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: ''
})

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await adminApi.reviews(params)
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
  query.status = ''
  query.pageNum = 1
  loadList()
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定要${next === 1 ? '显示' : '隐藏'}这条评价吗？`,
      '操作确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await adminApi.updateReviewStatus(row.id, next)
    ElMessage.success(next === 1 ? '评价已显示' : '评价已隐藏')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(loadList)
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
  background: #fafafa;
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
</style>
