<template>
  <div class="page-container">
    <div class="page-title">管理员列表</div>

    <div class="card">
      <el-alert
        title="管理员账号由系统初始化创建，后台仅支持查看。"
        type="info"
        :closable="false"
        show-icon
        class="mb16"
      />

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column label="管理员" min-width="220">
          <template #default="{ row }">
            <div class="cell-product">
              <el-avatar :size="40" :src="row.avatar">
                {{ (row.nickname || row.username || 'A').slice(0, 1) }}
              </el-avatar>
              <div class="cell-product-info">
                <div class="cell-product-name">{{ row.nickname || '-' }}</div>
                <div class="cell-product-sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="160" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small" effect="plain">
              {{ row.role === 'SUPER_ADMIN' ? '超级管理员' : row.role || '管理员' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="最近更新" width="180">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无管理员数据" />
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
import { adminApi } from '@/api'
import { dateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10 })

async function loadList() {
  loading.value = true
  try {
    const data = await adminApi.admins({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
</style>
