<template>
  <div class="page-container">
    <div class="page-title">经营看板</div>

    <!-- 店铺信息 -->
    <div class="card store-card">
      <el-avatar :size="56" :src="storeLogo" shape="square">
        {{ (dashboard.storeName || 'S').slice(0, 1) }}
      </el-avatar>
      <div class="store-info">
        <div class="store-name">{{ dashboard.storeName || '我的店铺' }}</div>
        <div class="store-sub">
          共 {{ dashboard.productCount || 0 }} 件商品 · {{ dashboard.orderCount || 0 }} 笔订单 ·
          平均评分 {{ dashboard.review?.avgScore ?? '-' }} 分
        </div>
      </div>
      <div class="store-actions">
        <el-button type="primary" @click="$router.push('/products')">商品管理</el-button>
        <el-button @click="$router.push('/orders')">订单管理</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div v-loading="loading" class="stat-grid mt16">
      <div v-for="item in statCards" :key="item.label" class="stat-card">
        <div class="stat-icon" :style="{ background: item.color }">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">
            <span v-if="item.prefix">{{ item.prefix }}</span>{{ item.value
            }}<small v-if="item.suffix">{{ item.suffix }}</small>
          </div>
        </div>
      </div>
    </div>

    <!-- 待处理提醒 -->
    <div class="card mt16">
      <div class="card-head">
        <span class="card-title">待处理事项</span>
      </div>
      <div class="todo-grid">
        <div class="todo-item" @click="$router.push({ path: '/orders', query: { status: 1 } })">
          <div class="todo-num">{{ dashboard.undeliveredCount || 0 }}</div>
          <div class="todo-label">待发货订单</div>
        </div>
        <div class="todo-item" @click="$router.push({ path: '/orders', query: { status: 0 } })">
          <div class="todo-num">{{ dashboard.unpaidCount || 0 }}</div>
          <div class="todo-label">待付款订单</div>
        </div>
        <div class="todo-item" @click="$router.push({ path: '/orders', query: { status: 2 } })">
          <div class="todo-num">{{ dashboard.unreceivedCount || 0 }}</div>
          <div class="todo-label">待收货订单</div>
        </div>
        <div class="todo-item" @click="$router.push({ path: '/reviews', query: { hasReply: 'false' } })">
          <div class="todo-num">{{ dashboard.review?.pendingReply || 0 }}</div>
          <div class="todo-label">待回复评价</div>
        </div>
      </div>
    </div>

    <!-- 销售排行图表 -->
    <div class="card mt16">
      <div class="card-head">
        <span class="card-title">近期商品销量排行 (Top 10)</span>
        <div class="card-head-right">
          <el-radio-group v-model="days" size="small" @change="loadStatistics">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="90">近 90 天</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-row :gutter="16" class="mb12">
        <el-col :xs="24" :sm="12">
          <div class="mini-stat">
            <span class="mini-label">统计区间成交额</span>
            <span class="mini-value text-price">¥{{ money(statistics.totalSales) }}</span>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="mini-stat">
            <span class="mini-label">统计区间订单数</span>
            <span class="mini-value">{{ statistics.totalOrders || 0 }}</span>
          </div>
        </el-col>
      </el-row>

      <el-empty v-if="!loadingStats && !ranking.length" description="所选区间内暂无销售数据" />
      <ChartBox v-else :option="rankOption" :loading="loadingStats" height="360px" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import ChartBox from '@/components/ChartBox.vue'
import { merchantApi } from '@/api'
import { money } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const loadingStats = ref(false)
const days = ref(30)

const dashboard = reactive({
  productCount: 0,
  onSaleCount: 0,
  offSaleCount: 0,
  orderCount: 0,
  unpaidCount: 0,
  undeliveredCount: 0,
  unreceivedCount: 0,
  finishedCount: 0,
  totalSales: 0,
  dealCount: 0,
  review: { avgScore: 0, total: 0, pendingReply: 0 },
  storeName: '',
  storeLogo: ''
})

const statistics = reactive({
  totalSales: 0,
  totalOrders: 0,
  productRanking: [],
  days: 30
})

const storeLogo = computed(() => dashboard.storeLogo || '')

const statCards = computed(() => [
  { label: '商品总数', value: dashboard.productCount || 0, icon: 'Goods', color: '#ff6700' },
  { label: '在售商品', value: dashboard.onSaleCount || 0, icon: 'Sell', color: '#36cfc9' },
  { label: '订单总数', value: dashboard.orderCount || 0, icon: 'List', color: '#597ef7' },
  { label: '待发货', value: dashboard.undeliveredCount || 0, icon: 'Van', color: '#ffa940' },
  { label: '总成交额', value: money(dashboard.totalSales), prefix: '¥', icon: 'Money', color: '#f5222d' },
  { label: '已成交订单', value: dashboard.dealCount || 0, icon: 'CircleCheck', color: '#52c41a' },
  {
    label: '平均评分',
    value: dashboard.review?.avgScore ?? 0,
    suffix: ' 分',
    icon: 'Star',
    color: '#faad14'
  },
  {
    label: '待回复评价',
    value: dashboard.review?.pendingReply || 0,
    icon: 'ChatDotRound',
    color: '#eb2f96'
  }
])

const ranking = computed(() => statistics.productRanking || [])

const rankOption = computed(() => {
  const list = [...ranking.value]
  // 柱状图从下往上, 数值小的在下面, 便于阅读
  const names = list.map((i) => i.productName)
  const values = list.map((i) => i.totalQuantity)
  const amounts = list.map((i) => Number(i.totalAmount))

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        const item = list[p.dataIndex]
        if (!item) return ''
        return `<div style="max-width:260px;white-space:normal"><b>${item.productName}</b><br/>销量：${item.totalQuantity} 件<br/>销售额：¥${money(item.totalAmount)}</div>`
      }
    },
    grid: { left: 12, right: 40, top: 20, bottom: 12, containLabel: true },
    xAxis: { type: 'value', name: '销量', minInterval: 1, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        width: 150,
        overflow: 'truncate',
        fontSize: 12
      }
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: values,
        barMaxWidth: 22,
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#ffb37a' },
              { offset: 1, color: '#ff6700' }
            ]
          }
        },
        label: {
          show: true,
          position: 'right',
          fontSize: 12,
          color: '#606266',
          formatter: (p) => `${p.value} 件 / ¥${money(amounts[p.dataIndex])}`
        }
      }
    ]
  }
})

async function loadDashboard() {
  loading.value = true
  try {
    const data = await merchantApi.dashboard()
    Object.assign(dashboard, data || {})
    if (data && !data.review) dashboard.review = { avgScore: 0, total: 0, pendingReply: 0 }
  } catch (e) {
    // 已统一提示
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  loadingStats.value = true
  try {
    const data = await merchantApi.statistics({ days: days.value, topN: 10 })
    Object.assign(statistics, data || {})
    if (!data || !data.productRanking) statistics.productRanking = []
  } catch (e) {
    statistics.productRanking = []
  } finally {
    loadingStats.value = false
  }
}

onMounted(() => {
  loadDashboard()
  loadStatistics()
})
</script>

<style scoped>
.mt16 {
  margin-top: 16px;
}

.mb12 {
  margin-bottom: 12px;
}

.store-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.store-info {
  flex: 1;
  min-width: 0;
}

.store-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

.store-sub {
  font-size: 13px;
  color: #909399;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  padding-left: 9px;
  border-left: 3px solid var(--el-color-primary);
  line-height: 1.2;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.todo-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.todo-item:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}

.todo-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.1;
}

.todo-label {
  font-size: 13px;
  color: #606266;
  margin-top: 6px;
}

.mini-stat {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mini-label {
  font-size: 13px;
  color: #909399;
}

.mini-value {
  font-size: 19px;
  font-weight: 600;
}
</style>
