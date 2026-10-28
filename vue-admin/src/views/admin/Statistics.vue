<template>
  <div class="page-container">
    <div class="page-title">销售统计</div>

    <div class="card">
      <div class="toolbar">
        <span class="label">统计区间</span>
        <el-radio-group v-model="days" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>

        <span class="label ml">排行数量</span>
        <el-select v-model="topN" style="width: 110px" @change="load">
          <el-option label="Top 5" :value="5" />
          <el-option label="Top 10" :value="10" />
          <el-option label="Top 20" :value="20" />
        </el-select>

        <div class="spacer"></div>
        <el-button @click="load">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <div class="summary">
            <div class="summary-icon" style="background: #f5222d">
              <el-icon><Money /></el-icon>
            </div>
            <div>
              <div class="summary-label">区间总成交额</div>
              <div class="summary-value">¥{{ money(data.totalSales) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="summary">
            <div class="summary-icon" style="background: #597ef7">
              <el-icon><List /></el-icon>
            </div>
            <div>
              <div class="summary-label">区间成交订单数</div>
              <div class="summary-value">{{ data.totalOrders || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 销售额趋势 -->
    <div class="card">
      <div class="card-head">
        <span class="card-title">近 {{ days }} 天销售额趋势</span>
      </div>
      <el-empty v-if="!loading && !trend.length" description="所选区间内暂无成交数据" />
      <ChartBox v-else :option="lineOption" :loading="loading" height="360px" />
    </div>

    <!-- 商品销量排行 -->
    <div class="card">
      <div class="card-head">
        <span class="card-title">商品销量排行 (Top {{ topN }})</span>
      </div>
      <el-empty v-if="!loading && !ranking.length" description="所选区间内暂无商品销量数据" />
      <ChartBox v-else :option="barOption" :loading="loading" height="420px" />
    </div>

    <!-- 排行明细表 -->
    <div class="card">
      <div class="card-head">
        <span class="card-title">排行明细</span>
      </div>
      <el-table :data="ranking" border stripe style="width: 100%" v-loading="loading">
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <el-tag :type="$index < 3 ? 'danger' : 'info'" size="small" effect="plain">
              {{ $index + 1 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productId" label="商品ID" width="100" align="center" />
        <el-table-column prop="productName" label="商品名称" min-width="260" show-overflow-tooltip />
        <el-table-column prop="totalQuantity" label="销量" width="110" align="center" sortable />
        <el-table-column label="销售额" width="150" align="right" sortable :sort-by="'totalAmount'">
          <template #default="{ row }">
            <span class="text-price">¥{{ money(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无排行数据" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ChartBox from '@/components/ChartBox.vue'
import { adminApi } from '@/api'
import { money } from '@/utils/format'

const loading = ref(false)
const days = ref(30)
const topN = ref(10)

const data = reactive({
  trend: [],
  productRanking: [],
  totalSales: 0,
  totalOrders: 0
})

const trend = computed(() => data.trend || [])
const ranking = computed(() => data.productRanking || [])

const lineOption = computed(() => {
  const list = trend.value
  const xData = list.map((i) => i.day)
  const amounts = list.map((i) => Number(i.amount) || 0)
  const counts = list.map((i) => Number(i.cnt) || 0)

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const idx = params[0].dataIndex
        const item = list[idx] || {}
        return `${item.day}<br/>销售额：¥${money(item.amount)}<br/>订单数：${item.cnt || 0} 单`
      }
    },
    legend: { data: ['销售额', '订单数'], top: 0 },
    grid: { left: 10, right: 20, top: 44, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
      axisLabel: { rotate: xData.length > 12 ? 40 : 0, fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额(元)',
        axisLabel: { formatter: (v) => (v >= 10000 ? `${v / 10000}万` : v) },
        splitLine: { lineStyle: { type: 'dashed' } }
      },
      {
        type: 'value',
        name: '订单数',
        minInterval: 1,
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: amounts,
        itemStyle: { color: '#ff6700' },
        lineStyle: { width: 3, color: '#ff6700' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255,103,0,0.32)' },
              { offset: 1, color: 'rgba(255,103,0,0.02)' }
            ]
          }
        }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbolSize: 6,
        data: counts,
        itemStyle: { color: '#597ef7' },
        lineStyle: { width: 2, type: 'dashed', color: '#597ef7' }
      }
    ]
  }
})

const barOption = computed(() => {
  const list = ranking.value
  const names = list.map((i) => i.productName)
  const values = list.map((i) => Number(i.totalQuantity) || 0)
  const amounts = list.map((i) => Number(i.totalAmount) || 0)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const idx = params[0].dataIndex
        const item = list[idx] || {}
        return `<div style="max-width:280px;white-space:normal"><b>${item.productName}</b><br/>销量：${item.totalQuantity} 件<br/>销售额：¥${money(item.totalAmount)}</div>`
      }
    },
    grid: { left: 10, right: 30, top: 30, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        rotate: 40,
        fontSize: 11,
        width: 110,
        overflow: 'truncate'
      }
    },
    yAxis: {
      type: 'value',
      name: '销量',
      minInterval: 1,
      splitLine: { lineStyle: { type: 'dashed' } }
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: values,
        barMaxWidth: 34,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#ff6700' },
              { offset: 1, color: '#ffb37a' }
            ]
          }
        },
        label: {
          show: true,
          position: 'top',
          fontSize: 11,
          color: '#606266',
          formatter: (p) => `${p.value} 件`
        }
      }
    ]
  }
})

async function load() {
  loading.value = true
  try {
    const res = await adminApi.statistics({ days: days.value, topN: topN.value })
    data.trend = (res && res.trend) || []
    data.productRanking = (res && res.productRanking) || []
    data.totalSales = (res && res.totalSales) || 0
    data.totalOrders = (res && res.totalOrders) || 0
  } catch (e) {
    data.trend = []
    data.productRanking = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.label {
  font-size: 13px;
  color: #606266;
}

.label.ml {
  margin-left: 8px;
}

.summary {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 22px;
  font-weight: 600;
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
</style>
