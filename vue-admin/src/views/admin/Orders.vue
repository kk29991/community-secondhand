<template>
  <div class="page-container">
    <div class="page-title">平台订单管理</div>

    <div class="card">
      <el-tabs v-model="activeTab" class="status-tabs" @tab-change="onTabChange">
        <el-tab-pane
          v-for="tab in ORDER_STATUS_TABS"
          :key="String(tab.value)"
          :name="String(tab.value)"
          :label="tab.label"
        />
      </el-tabs>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 收货人 / 手机号"
          clearable
          style="width: 240px"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="商品" min-width="230">
          <template #default="{ row }">
            <div v-if="row.items && row.items.length" class="order-items">
              <div v-for="it in row.items.slice(0, 2)" :key="it.id" class="order-item">
                <el-image :src="it.productImage" fit="cover" class="cell-thumb sm" />
                <span class="order-item-name">{{ it.productName }}</span>
                <span class="text-muted">x{{ it.quantity }}</span>
              </div>
              <div v-if="row.items.length > 2" class="text-muted more">
                等共 {{ row.items.length }} 件商品
              </div>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="买家/收货人" width="160">
          <template #default="{ row }">
            <div>{{ row.receiverName || '-' }}</div>
            <div class="text-muted phone">{{ row.receiverPhone || '' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="店铺" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.storeName || '-' }}</template>
        </el-table-column>

        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">
            <span class="text-price">¥{{ money(row.payAmount ?? row.totalAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">
              {{ row.statusText || orderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无订单数据" />
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

    <!-- ------------------------------------------------------ 订单详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="640px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <el-alert
            :title="`当前状态：${detail.statusText || orderStatusText(detail.status)}`"
            :type="detail.status === 3 ? 'success' : 'info'"
            :closable="false"
            show-icon
            class="mb16"
          />

          <el-descriptions title="基本信息" :column="2" border size="small" class="mb16">
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">
              {{ dateTime(detail.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="付款时间">
              {{ dateTime(detail.payTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="发货时间">
              {{ dateTime(detail.deliveryTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="完成时间">
              {{ dateTime(detail.finishTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="快递单号">
              {{ detail.expressNo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="取消原因" :span="2">
              {{ detail.cancelReason || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="买家备注" :span="2">
              {{ detail.remark || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="收货信息" :column="2" border size="small" class="mb16">
            <el-descriptions-item label="收货人">{{ detail.receiverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ detail.receiverPhone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ detail.receiverAddress || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="detail-title">商品明细</div>
          <el-table :data="detail.items || []" border size="small" class="mb16">
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <div class="cell-product">
                  <el-image :src="row.productImage" fit="cover" class="cell-thumb" />
                  <div class="cell-product-name">{{ row.productName }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }">¥{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="70" align="center" />
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }">
                <span class="text-price">¥{{ money(row.totalPrice) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="商品总额">¥{{ money(detail.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="运费">¥{{ money(detail.freight) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span class="text-price big">¥{{ money(detail.payAmount) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </template>

        <el-empty v-else-if="!detailLoading" description="未能加载订单详情" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { adminApi } from '@/api'
import { ORDER_STATUS_TABS, dateTime, money, orderStatusText, orderStatusType } from '@/utils/format'

const route = useRoute()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const activeTab = ref('')

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

    const data = await adminApi.orders(params)
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.status = activeTab.value === '' ? '' : Number(activeTab.value)
  query.pageNum = 1
  loadList()
}

function search() {
  query.pageNum = 1
  loadList()
}

function reset() {
  query.keyword = ''
  query.pageNum = 1
  loadList()
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    // 后端 GET /orders/{id} 存在缺陷会返回 500, api 层已自动回退到列表接口
    detail.value = await adminApi.orderDetail(row.id)
  } catch (e) {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  const qStatus = route.query.status
  if (qStatus !== undefined && qStatus !== null && qStatus !== '') {
    activeTab.value = String(qStatus)
    query.status = Number(qStatus)
  }
  loadList()
})
</script>

<style scoped>
.status-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cell-thumb.sm {
  width: 34px;
  height: 34px;
}

.order-item-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.more,
.phone {
  font-size: 12px;
}

.detail-wrap {
  min-height: 200px;
}

.mb16 {
  margin-bottom: 16px;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.text-price.big {
  font-size: 16px;
}
</style>
