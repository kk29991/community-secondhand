<template>
  <div class="page-container">
    <div class="page-title">订单管理</div>

    <div class="card">
      <!-- 状态 Tab -->
      <el-tabs v-model="activeTab" class="status-tabs" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in ORDER_STATUS_TABS" :key="String(tab.value)" :name="String(tab.value)">
          <template #label>
            <span>{{ tab.label }}</span>
            <el-badge
              v-if="tab.value !== '' && counts[countKey(tab.value)]"
              :value="counts[countKey(tab.value)]"
              class="tab-badge"
              type="primary"
            />
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 查询工具条 -->
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

      <!-- 订单表格 -->
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="170" />

        <el-table-column label="商品" min-width="240">
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

        <el-table-column label="收货人" width="150">
          <template #default="{ row }">
            <div>{{ row.receiverName || '-' }}</div>
            <div class="text-muted phone">{{ row.receiverPhone || '' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="text-price">¥{{ money(row.payAmount ?? row.totalAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.statusText ? statusType(row.status) : statusType(row.status)" size="small">
              {{ row.statusText || orderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">
            <span class="text-muted">{{ dateTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="warning"
              size="small"
              @click="openDeliver(row)"
            >
              发货
            </el-button>
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
            :type="alertType(detail.status)"
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
            <el-descriptions-item label="商品总额">
              ¥{{ money(detail.totalAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="运费">¥{{ money(detail.freight) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span class="text-price big">¥{{ money(detail.payAmount) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="detail.status === 1" class="drawer-actions">
            <el-button type="primary" @click="openDeliver(detail)">立即发货</el-button>
          </div>
        </template>

        <el-empty v-else-if="!detailLoading" description="未能加载订单详情" />
      </div>
    </el-drawer>

    <!-- -------------------------------------------------------- 发货弹窗 -->
    <el-dialog v-model="deliverVisible" title="订单发货" width="460px" destroy-on-close>
      <el-form ref="deliverFormRef" :model="deliverForm" :rules="deliverRules" label-width="90px">
        <el-form-item label="订单号">
          <span>{{ deliverForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="快递单号" prop="expressNo">
          <el-input v-model="deliverForm.expressNo" placeholder="例如：SF1234567890" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverVisible = false">取消</el-button>
        <el-button type="primary" :loading="delivering" @click="submitDeliver">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api'
import {
  ORDER_STATUS_TABS,
  dateTime,
  money,
  orderStatusText,
  orderStatusType
} from '@/utils/format'

const route = useRoute()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const counts = reactive({})

const activeTab = ref('')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

/** 后端 counts: {unpaid, undelivered, unreceived, finished, all} */
function countKey(status) {
  return (
    {
      0: 'unpaid',
      1: 'undelivered',
      2: 'unreceived',
      3: 'finished'
    }[status] || ''
  )
}

function statusType(status) {
  return orderStatusType(status)
}

function alertType(status) {
  if (status === 3) return 'success'
  if (status === 4 || status === 5) return 'info'
  if (status === 1) return 'warning'
  return 'info'
}

async function loadCounts() {
  try {
    const data = await merchantApi.orderCounts()
    Object.assign(counts, data || {})
  } catch (e) {
    // 忽略
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '' && query.status !== null) params.status = query.status

    const data = await merchantApi.orders(params)
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

/* ------------------------------------------------------------- 详情 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    // 后端 GET /orders/{id} 存在缺陷会返回 500,
    // api 层已自动回退到列表接口取数
    detail.value = await merchantApi.orderDetail(row.id)
  } catch (e) {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

/* ------------------------------------------------------------- 发货 */
const deliverVisible = ref(false)
const delivering = ref(false)
const deliverFormRef = ref(null)
const deliverForm = reactive({ id: null, orderNo: '', expressNo: '' })

const deliverRules = {
  expressNo: [
    { required: true, message: '请输入快递单号', trigger: 'blur' },
    { min: 4, max: 40, message: '快递单号长度为 4 - 40 个字符', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9-]+$/,
      message: '快递单号只能包含字母、数字和短横线',
      trigger: 'blur'
    }
  ]
}

function openDeliver(row) {
  deliverForm.id = row.id
  deliverForm.orderNo = row.orderNo
  deliverForm.expressNo = row.expressNo || ''
  deliverVisible.value = true
}

async function submitDeliver() {
  if (!deliverFormRef.value) return
  try {
    await deliverFormRef.value.validate()
  } catch (e) {
    return
  }

  delivering.value = true
  try {
    await merchantApi.deliver(deliverForm.id, deliverForm.expressNo)
    ElMessage.success('发货成功')
    deliverVisible.value = false
    detailVisible.value = false
    loadList()
    loadCounts()
  } catch (e) {
    // 已统一提示
  } finally {
    delivering.value = false
  }
}

onMounted(() => {
  // 支持从看板跳转带 status 参数
  const qStatus = route.query.status
  if (qStatus !== undefined && qStatus !== null && qStatus !== '') {
    activeTab.value = String(qStatus)
    query.status = Number(qStatus)
  }
  loadCounts()
  loadList()
})
</script>

<style scoped>
.status-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.tab-badge {
  margin-left: 6px;
  vertical-align: middle;
}

.tab-badge :deep(.el-badge__content) {
  position: static;
  transform: none;
  border: none;
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

.more {
  font-size: 12px;
}

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

.drawer-actions {
  margin-top: 18px;
  text-align: right;
}
</style>
