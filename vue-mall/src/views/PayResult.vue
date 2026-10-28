<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail, payOrder } from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, formatTime } from '@/utils/placeholder'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const paying = ref(false)
const orders = ref([])

const paid = computed(() => route.query.paid !== '0')

const orderIds = computed(() => {
  const raw = route.query.orderIds ?? route.query.orderId ?? route.query.ids
  if (!raw) return []
  return String(raw)
    .split(',')
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n > 0)
})

const totalPaid = computed(() =>
  orders.value.reduce((sum, o) => sum + Number(o.payAmount ?? o.totalAmount ?? 0), 0)
)

const allPaid = computed(() =>
  orders.value.length > 0 && orders.value.every((o) => o.status !== 0)
)

const headline = computed(() => {
  if (!orders.value.length) return { title: '订单结果', desc: '未找到订单信息' }
  if (allPaid.value) {
    return {
      title: '支付成功',
      desc: `已成功支付 ${orders.value.length} 笔订单, 共 ￥${money(totalPaid.value)}, 商家将尽快为你发货`
    }
  }
  return {
    title: '订单已提交',
    desc: `还有 ${orders.value.filter((o) => o.status === 0).length} 笔订单待支付, 请尽快完成支付`
  }
})

async function loadOrders() {
  if (!orderIds.value.length) {
    loading.value = false
    return
  }
  loading.value = true
  const results = await Promise.allSettled(orderIds.value.map((id) => getOrderDetail(id)))
  orders.value = results
    .filter((r) => r.status === 'fulfilled' && r.value)
    .map((r) => r.value)
  loading.value = false
}

async function handlePay(order) {
  paying.value = true
  try {
    await payOrder(order.id, order.payType || 2)
    ElMessage.success('支付成功')
    await loadOrders()
  } catch (e) {
    // 拦截器已提示
  } finally {
    paying.value = false
  }
}

async function payAll() {
  const unpaid = orders.value.filter((o) => o.status === 0)
  if (!unpaid.length) return
  paying.value = true
  try {
    for (const order of unpaid) {
      await payOrder(order.id, order.payType || 2)
    }
    ElMessage.success('支付成功')
    await loadOrders()
  } catch (e) {
    // 拦截器已提示
  } finally {
    paying.value = false
  }
}

const statusMap = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: 'primary' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'info' }
}

onMounted(async () => {
  await loadOrders()
  userStore.refreshCartCount()
})
</script>

<template>
  <div class="result-page">
    <div class="mall-container">
      <div class="mall-page-head">
        <h2><el-icon><Wallet /></el-icon> 支付结果</h2>
        <el-button text type="primary" @click="router.push('/')">返回首页</el-button>
      </div>

      <div v-loading="loading" element-loading-text="正在查询订单...">
        <div class="result-card" :class="allPaid && orders.length ? 'success' : 'pending'">
          <el-icon class="result-icon">
            <component :is="allPaid && orders.length ? 'SuccessFilled' : 'Clock'" />
          </el-icon>
          <div class="result-text">
            <h1>{{ headline.title }}</h1>
            <p>{{ headline.desc }}</p>
          </div>
        </div>

        <div v-if="orders.length" class="panel">
          <div class="panel-head">
            <span class="panel-title">订单信息</span>
            <el-button v-if="orders.some((o) => o.status === 0)" type="primary" :loading="paying" @click="payAll">
              一键支付全部待付款订单
            </el-button>
          </div>

          <div v-for="order in orders" :key="order.id" class="order-block">
            <div class="order-head">
              <span class="order-no">订单号: {{ order.orderNo }}</span>
              <el-tag :type="statusMap[order.status]?.type || 'info'" size="small" effect="light">
                {{ order.statusText || statusMap[order.status]?.text || '未知' }}
              </el-tag>
              <span class="order-time">下单时间: {{ formatTime(order.createTime) }}</span>
            </div>

            <div class="order-body">
              <div class="order-items">
                <div v-for="item in order.items || []" :key="item.id" class="order-item">
                  <img :src="item.productImage" :alt="item.productName" />
                  <div class="item-info">
                    <p class="item-name">{{ item.productName }}</p>
                    <p class="item-meta">￥{{ money(item.price) }} × {{ item.quantity }}</p>
                  </div>
                  <span class="item-sub mall-price">￥{{ money(item.totalPrice) }}</span>
                </div>
                <el-empty v-if="!(order.items || []).length" :image-size="60" description="暂无商品明细" />
              </div>

              <div class="order-aside">
                <div class="aside-line">
                  <span>商品金额</span>
                  <em>￥{{ money(order.totalAmount) }}</em>
                </div>
                <div class="aside-line">
                  <span>平台服务费</span>
                  <em>￥{{ money(order.freight) }}</em>
                </div>
                <div class="aside-line total">
                  <span>应付金额</span>
                  <span class="mall-price"><span class="symbol">￥</span><span class="num">{{ money(order.payAmount) }}</span></span>
                </div>
                <div class="aside-line">
                  <span>收货信息</span>
                  <em class="small">{{ order.receiverName }} {{ order.receiverPhone }}</em>
                </div>
                <div class="aside-line">
                  <span>收货地址</span>
                  <em class="small">{{ order.receiverAddress }}</em>
                </div>
                <el-button
                  v-if="order.status === 0"
                  type="primary"
                  class="pay-btn"
                  :loading="paying"
                  @click="handlePay(order)"
                >
                  立即支付
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="mall-empty">
          <el-empty description="没有找到对应的订单">
            <el-button type="primary" @click="router.push('/search')">继续购物</el-button>
          </el-empty>
        </div>

        <div class="actions">
          <el-button type="primary" size="large" @click="router.push('/user/orders')">
            <el-icon><List /></el-icon> 查看我的订单
          </el-button>
          <el-button size="large" @click="router.push('/search')">
            <el-icon><GoodsFilled /></el-icon> 继续购物
          </el-button>
          <el-button size="large" text @click="router.push('/ai')">
            <el-icon><MagicStick /></el-icon> 让 AI 推荐更多好物
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.result-page {
  padding-top: 18px;
}

.mall-page-head h2 {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-card {
  display: flex;
  align-items: center;
  gap: 22px;
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 34px 36px;
  margin-bottom: 16px;
  border-left: 6px solid #52c41a;
}

.result-card.pending {
  border-left-color: #ff6700;
}

.result-icon {
  font-size: 56px;
  color: #52c41a;
}

.result-card.pending .result-icon {
  color: #ff6700;
}

.result-text h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 500;
  color: #333;
}

.result-text p {
  margin: 0;
  font-size: 14px;
  color: #757575;
}

.panel {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 20px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 16px;
  font-weight: 500;
  position: relative;
  padding-left: 12px;
}

.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: #ff6700;
}

.order-block {
  border: 1px solid #f0f0f0;
  border-radius: var(--mall-radius);
  margin-bottom: 14px;
  overflow: hidden;
}

.order-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fcfcfc;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
  color: #616161;
  flex-wrap: wrap;
}

.order-time {
  margin-left: auto;
  color: #b0b0b0;
}

.order-body {
  display: flex;
  gap: 20px;
  padding: 16px;
  flex-wrap: wrap;
}

.order-items {
  flex: 1;
  min-width: 280px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f5f5f5;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: #fafafa;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  margin: 0 0 4px;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  margin: 0;
  font-size: 12px;
  color: #b0b0b0;
}

.order-aside {
  width: 300px;
  flex-shrink: 0;
  border-left: 1px solid #f0f0f0;
  padding-left: 20px;
}

.aside-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  color: #757575;
  padding: 5px 0;
}

.aside-line em {
  font-style: normal;
  color: #333;
  text-align: right;
}

.aside-line em.small {
  font-size: 12px;
  color: #9e9e9e;
  max-width: 180px;
}

.aside-line.total {
  border-top: 1px dashed #f0f0f0;
  margin-top: 6px;
  padding-top: 10px;
}

.pay-btn {
  width: 100%;
  margin-top: 12px;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 10px 0 20px;
  flex-wrap: wrap;
}

.actions .el-button {
  min-width: 150px;
}
</style>
