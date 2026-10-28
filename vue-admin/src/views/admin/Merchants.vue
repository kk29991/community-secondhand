<template>
  <div class="page-container">
    <div class="page-title">商家管理</div>

    <div class="card">
      <el-alert
        title="商家入驻审核：新入驻商家默认处于「待审核 / 已禁用」状态，可在此处审核通过（启用）或驳回（禁用）。"
        type="info"
        :closable="false"
        show-icon
        class="mb16"
      />

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="店铺名 / 账号 / 联系人"
          clearable
          style="width: 240px"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="query.status" placeholder="审核状态" clearable style="width: 150px" @change="search">
          <el-option label="全部状态" :value="''" />
          <el-option label="已通过（正常）" :value="1" />
          <el-option label="待审核 / 已禁用" :value="0" />
        </el-select>

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="店铺" min-width="230">
          <template #default="{ row }">
            <div class="cell-product">
              <el-image
                :src="row.storeLogo"
                fit="cover"
                class="cell-thumb"
                :preview-src-list="[row.storeLogo]"
                preview-teleported
                hide-on-click-modal
              />
              <div class="cell-product-info">
                <div class="cell-product-name">{{ row.storeName || '-' }}</div>
                <div class="cell-product-sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="storeDesc" label="店铺简介" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.storeDesc || '-' }}</template>
        </el-table-column>

        <el-table-column label="联系人" width="170">
          <template #default="{ row }">
            <div>{{ row.contactName || '-' }}</div>
            <div class="text-muted small">{{ row.contactPhone || '' }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '-' }}</template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已通过' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="入驻时间" width="170">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '审核通过' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无商家数据" />
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
import { dateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await adminApi.merchants(params)
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

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '审核通过（启用）' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}商家「${row.storeName}」吗？`, '操作确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await adminApi.updateMerchantStatus(row.id, next)
    ElMessage.success(next === 1 ? '商家已通过审核' : '商家已禁用')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除商家「${row.storeName}」吗？该商家下的商品将无法访问，此操作不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch (e) {
    return
  }
  try {
    await adminApi.deleteMerchant(row.id)
    ElMessage.success('商家已删除')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(loadList)
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}

.small {
  font-size: 12px;
}
</style>
