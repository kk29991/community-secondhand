<template>
  <div class="page-container">
    <div class="page-title">平台概览</div>

    <div v-loading="loading" class="stat-grid">
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

    <el-row :gutter="16" class="mt16">
      <!-- 订单状态分布 -->
      <el-col :xs="24" :lg="12">
        <div class="card chart-card">
          <div class="card-head">
            <span class="card-title">订单状态分布</span>
            <el-button link type="primary" size="small" @click="load">刷新</el-button>
          </div>
          <el-empty v-if="!loading && orderTotalCount === 0" description="暂无订单数据" />
          <ChartBox v-else :option="pieOption" :loading="loading" height="330px" />
        </div>
      </el-col>

      <!-- 核心指标 -->
      <el-col :xs="24" :lg="12">
        <div class="card chart-card">
          <div class="card-head">
            <span class="card-title">平台核心指标</span>
          </div>
          <div class="metric-list">
            <div v-for="m in metrics" :key="m.label" class="metric-row">
              <span class="metric-label">{{ m.label }}</span>
              <div class="metric-bar-wrap">
                <div
                  class="metric-bar"
                  :style="{ width: m.percent + '%', background: m.color }"
                ></div>
              </div>
              <span class="metric-value">{{ m.display }}</span>
            </div>
          </div>

          <el-divider />

          <div class="quick-links">
            <el-button type="primary" plain @click="$router.push('/admin/merchants')">
              商家管理
            </el-button>
            <el-button type="primary" plain @click="$router.push('/admin/users')">
              用户管理
            </el-button>
            <el-button type="primary" plain @click="$router.push('/admin/products')">
              商品管理
            </el-button>
            <el-button type="primary" plain @click="$router.push('/admin/orders')">
              订单管理
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ChartBox from '@/components/ChartBox.vue'
import { adminApi } from '@/api'
import { money } from '@/utils/format'

const loading = ref(false)

const data = reactive({
  userCount: 0,
  merchantCount: 0,
  productCount: 0,
  onSaleCount: 0,
  categoryCount: 0,
  orderCount: 0,
  unpaidCount: 0,
  undeliveredCount: 0,
  unreceivedCount: 0,
  finishedCount: 0,
  totalSales: 0,
  finishedSalesCount: 0
})

const statCards = computed(() => [
  { label: '平台用户数', value: data.userCount || 0, icon: 'User', color: '#597ef7' },
  { label: '入驻商家数', value: data.merchantCount || 0, icon: 'Shop', color: '#ff6700' },
  { label: '商品总数', value: data.productCount || 0, icon: 'Goods', color: '#36cfc9' },
  { label: '在售商品', value: data.onSaleCount || 0, icon: 'Sell', color: '#52c41a' },
  { label: '分类数量', value: data.categoryCount || 0, icon: 'Menu', color: '#722ed1' },
  { label: '订单总数', value: data.orderCount || 0, icon: 'List', color: '#2f54eb' },
  { label: '待付款', value: data.unpaidCount || 0, icon: 'Wallet', color: '#faad14' },
  { label: '待发货', value: data.undeliveredCount || 0, icon: 'Van', color: '#ffa940' },
  { label: '待收货', value: data.unreceivedCount || 0, icon: 'Box', color: '#13c2c2' },
  { label: '已完成订单', value: data.finishedCount || 0, icon: 'CircleCheck', color: '#52c41a' },
  {
    label: '平台总成交额',
    value: money(data.totalSales),
    prefix: '¥',
    icon: 'Money',
    color: '#f5222d'
  },
  {
    label: '成交订单数',
    value: data.finishedSalesCount || 0,
    icon: 'ShoppingCart',
    color: '#eb2f96'
  }
])

const orderTotalCount = computed(
  () =>
    (data.unpaidCount || 0) +
    (data.undeliveredCount || 0) +
    (data.unreceivedCount || 0) +
    (data.finishedCount || 0)
)

const pieOption = computed(() => {
  const pieData = [
    { name: '待付款', value: data.unpaidCount || 0, itemStyle: { color: '#faad14' } },
    { name: '待发货', value: data.undeliveredCount || 0, itemStyle: { color: '#ff6700' } },
    { name: '待收货', value: data.unreceivedCount || 0, itemStyle: { color: '#13c2c2' } },
    { name: '已完成', value: data.finishedCount || 0, itemStyle: { color: '#52c41a' } }
  ].filter((i) => i.value > 0)

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>数量：{c} 单 ({d}%)'
    },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: {
          show: true,
          formatter: '{b}\n{c} 单'
        },
        labelLine: { length: 10, length2: 10 },
        data: pieData
      }
    ]
  }
})

const metrics = computed(() => {
  const max = Math.max(
    data.userCount || 0,
    data.merchantCount || 0,
    data.productCount || 0,
    data.orderCount || 0,
    1
  )
  const rows = [
    { label: '用户数', value: data.userCount || 0, display: data.userCount || 0, color: '#597ef7' },
    {
      label: '商家数',
      value: data.merchantCount || 0,
      display: data.merchantCount || 0,
      color: '#ff6700'
    },
    {
      label: '商品数',
      value: data.productCount || 0,
      display: data.productCount || 0,
      color: '#36cfc9'
    },
    {
      label: '订单数',
      value: data.orderCount || 0,
      display: data.orderCount || 0,
      color: '#2f54eb'
    }
  ]
  return rows.map((r) => ({ ...r, percent: Math.round((r.value / max) * 100) }))
})

async function load() {
  loading.value = true
  try {
    const res = await adminApi.overview()
    Object.assign(data, res || {})
  } catch (e) {
    // 已统一提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mt16 {
  margin-top: 16px;
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

.chart-card {
  height: 100%;
}

.metric-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 6px 0;
}

.metric-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-label {
  width: 56px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}

.metric-bar-wrap {
  flex: 1;
  height: 12px;
  background: #f0f2f5;
  border-radius: 6px;
  overflow: hidden;
}

.metric-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.5s;
  min-width: 2px;
}

.metric-value {
  width: 60px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
