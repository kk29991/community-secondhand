<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelOrder,
  getOrderCounts,
  getOrderList,
  payOrder,
  receiveOrder,
  refundOrder
} from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, formatTime, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const orders = ref([])
const total = ref(0)
const counts = ref({ unpaid: 0, undelivered: 0, unreceived: 0, finished: 0, all: 0 })

const query = ref({ status: null, pageNum: 1, pageSize: 5 })

const statusTabs = [
  { label: '全部订单', value: null, countKey: 'all' },
  { label: '待付款', value: 0, countKey: 'unpaid' },
  { label: '待发货', value: 1, countKey: 'undelivered' },
  { label: '待收货', value: 2, countKey: 'unreceived' },
  { label: '已完成', value: 3, countKey: 'finished' }
]

const statusMap = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: 'primary' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'info' }
}

const payTypeMap = { 1: '余额支付', 2: '支付宝', 3: '微信支付' }

const activeTab = computed({
  get: () => (query.value.status === null ? 'all' : String(query.value.status)),
  set: (val) => {
    query.value.status = val === 'all' ? null : Number(val)
    query.value.pageNum = 1
    loadOrders()
  }
})

function tabCount(countKey) {
  return counts.value?.[countKey] ?? 0
}

async function loadCounts() {
  try {
    const data = await getOrderCounts()
    counts.value = { unpaid: 0, undelivered: 0, unreceived: 0, finished: 0, all: 0, ...(data || {}) }
  } catch (e) {
    // 拦截器已提示
  }
}

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const params = { pageNum: query.value.pageNum, pageSize: query.value.pageSize }
    if (query.value.status !== null) params.status = query.value.status
    const data = await getOrderList(params)
    orders.value = data?.records || []
    total.value = Number(data?.total) || 0
  } catch (e) {
    orders.value = []
    total.value = 0
    error.value = '订单列表加载失败, 请稍后重试'
  } finally {
    loading.value = false
  }
}

async function refresh() {
  await Promise.all([loadCounts(), loadOrders()])
  userStore.refreshCartCount()
}

function changePage(page) {
  query.value.pageNum = page
  loadOrders()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function handlePay(order) {
  try {
    await payOrder(order.id, order.payType || 2)
    ElMessage.success('支付成功')
    await refresh()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleCancel(order) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('请填写取消原因 (选填)', `取消订单 ${order.orderNo}`, {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      inputPlaceholder: '例如: 不想买了 / 买错了',
      inputValue: '用户主动取消'
    })
    reason = res.value || '用户主动取消'
  } catch (e) {
    return
  }
  try {
    await cancelOrder(order.id, reason)
    ElMessage.success('订单已取消')
    await refresh()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleReceive(order) {
  try {
    await ElMessageBox.confirm('请确认已收到商品, 确认后订单将完成。', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await receiveOrder(order.id)
    ElMessage.success('已确认收货')
    await refresh()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleRefund(order) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('请填写退款原因', `申请退款 ${order.orderNo}`, {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '例如: 商品与描述不符',
      inputValue: '7 天无理由退款'
    })
    reason = res.value || '用户申请退款'
  } catch (e) {
    return
  }
  try {
    await refundOrder(order.id, reason)
    ElMessage.success('退款申请已受理')
    await refresh()
  } catch (e) {
    // 拦截器已提示
  }
}

function goDetail(order) {
  router.push({ path: '/payResult', query: { orderId: order.id, paid: order.status === 0 ? '0' : '1' } })
}

function goReview() {
  router.push('/user/reviews')
}

function goGoods(item) {
  router.push({ path: '/goodsDetail', query: { pid: item.productId } })
}

onMounted(() => {
  refresh()
})
</script>

<template>
  <div class="orders-page">
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">我的订单</span>
        <el-button text type="primary" @click="refresh">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>

      <el-tabs v-model="activeTab" class="order-tabs">
        <el-tab-pane v-for="tab in statusTabs" :key="tab.label" :name="tab.value === null ? 'all' : String(tab.value)">
          <template #label>
            <span>
              {{ tab.label }}
              <em class="tab-count">{{ tabCount(tab.countKey) }}</em>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="loadOrders">重新加载</el-button>
        </template>
      </el-alert>

      <div v-loading="loading" element-loading-text="正在加载订单..." class="order-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-head">
            <div class="head-left">
              <span class="order-no">订单号: {{ order.orderNo }}</span>
              <span class="order-store">
                <el-icon><Shop /></el-icon>
                {{ order.storeName || `店铺 #${order.merchantId}` }}
              </span>
            </div>
            <div class="head-right">
              <span class="order-time">{{ formatTime(order.createTime) }}</span>
              <el-tag :type="statusMap[order.status]?.type || 'info'" size="small" effect="light">
                {{ order.statusText || statusMap[order.status]?.text || '未知状态' }}
              </el-tag>
            </div>
          </div>

          <div class="order-main">
            <div class="order-goods">
              <div v-for="item in order.items || []" :key="item.id" class="goods-row" @click="goGoods(item)">
                <img :src="item.productImage || DEFAULT_IMAGE" :alt="item.productName" @error="fallbackImage($event, item.productName)" />
                <div class="goods-info">
                  <p class="goods-name">{{ item.productName }}</p>
                  <p class="goods-meta">
                    ￥{{ money(item.price) }} × {{ item.quantity }}
                    <el-tag v-if="item.commented === 0 && order.status === 3" size="small" type="warning" effect="plain">
                      待评价
                    </el-tag>
                  </p>
                </div>
                <span class="goods-sub">￥{{ money(item.totalPrice) }}</span>
              </div>
              <el-empty v-if="!(order.items || []).length" :image-size="60" description="无商品明细" />
            </div>

            <div class="order-aside">
              <div class="aside-line">
                <span>商品金额</span>
                <em>￥{{ money(order.totalAmount) }}</em>
              </div>
              <div class="aside-line">
                <span>平台服务费</span>
                <em>{{ Number(order.freight) > 0 ? `￥${money(order.freight)}` : '免收' }}</em>
              </div>
              <div class="aside-line total">
                <span>实付金额</span>
                <span class="mall-price"><span class="symbol">￥</span><span class="num">{{ money(order.payAmount) }}</span></span>
              </div>
              <div class="aside-line">
                <span>支付方式</span>
                <em>{{ order.payType ? payTypeMap[order.payType] : '-' }}</em>
              </div>
              <div v-if="order.expressNo" class="aside-line">
                <span>交易凭证号</span>
                <em>{{ order.expressNo }}</em>
              </div>
            </div>
          </div>

          <div class="order-foot">
            <span class="foot-address">
              <el-icon><LocationInformation /></el-icon>
              {{ order.receiverName }} {{ order.receiverPhone }} · {{ order.receiverAddress }}
            </span>
            <div class="foot-actions">
              <el-button size="small" @click="goDetail(order)">订单详情</el-button>
              <el-button v-if="order.status === 0" size="small" type="primary" @click="handlePay(order)">立即支付</el-button>
              <el-button v-if="order.status === 0" size="small" @click="handleCancel(order)">取消订单</el-button>
              <el-button v-if="order.status === 1" size="small" type="warning" plain disabled>等待商家发货</el-button>
              <el-button v-if="order.status === 2" size="small" type="primary" @click="handleReceive(order)">确认收货</el-button>
              <el-button v-if="order.status === 3" size="small" type="warning" plain @click="goReview">发表评价</el-button>
              <el-button v-if="order.status === 3" size="small" type="danger" plain @click="handleRefund(order)">申请退款</el-button>
            </div>
          </div>
        </div>

        <div v-if="!orders.length && !loading" class="empty-wrap">
          <el-empty description="暂无相关订单">
            <el-button type="primary" @click="router.push('/search')">去挑选商品</el-button>
          </el-empty>
        </div>
      </div>

      <div v-if="total > query.pageSize" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.pageSize"
          :current-page="query.pageNum"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 20px 24px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
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

.mb16 {
  margin-bottom: 16px;
}

.tab-count {
  font-style: normal;
  color: #ff6700;
  font-size: 12px;
}

.order-list {
  min-height: 200px;
}

.order-card {
  border: 1px solid #f0f0f0;
  border-radius: var(--mall-radius);
  margin-bottom: 16px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fcfcfc;
  border-bottom: 1px solid #f5f5f5;
  padding: 12px 18px;
  font-size: 13px;
  color: #616161;
  flex-wrap: wrap;
}

.head-left,
.head-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.order-no {
  font-weight: 500;
  color: #333;
}

.order-store {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #757575;
}

.order-time {
  color: #b0b0b0;
}

.order-main {
  display: flex;
  gap: 20px;
  padding: 16px 18px;
  flex-wrap: wrap;
}

.order-goods {
  flex: 1;
  min-width: 280px;
}

.goods-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #f5f5f5;
  cursor: pointer;
}

.goods-row:last-child {
  border-bottom: none;
}

.goods-row img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  background: #fafafa;
  border: 1px solid #f2f2f2;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-name {
  margin: 0 0 6px;
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-name:hover {
  color: #ff6700;
}

.goods-meta {
  margin: 0;
  font-size: 12px;
  color: #9e9e9e;
  display: flex;
  align-items: center;
  gap: 8px;
}

.goods-sub {
  font-size: 14px;
  color: #616161;
  flex-shrink: 0;
}

.order-aside {
  width: 240px;
  flex-shrink: 0;
  border-left: 1px solid #f0f0f0;
  padding-left: 18px;
}

.aside-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #9e9e9e;
  padding: 4px 0;
}

.aside-line em {
  font-style: normal;
  color: #616161;
}

.aside-line.total {
  border-top: 1px dashed #f0f0f0;
  margin-top: 6px;
  padding-top: 10px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  background: #fcfcfc;
  border-top: 1px solid #f5f5f5;
  padding: 12px 18px;
  flex-wrap: wrap;
}

.foot-address {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #9e9e9e;
  max-width: 55%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.foot-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
  flex-wrap: wrap;
}

.empty-wrap {
  padding: 20px 0;
}

.pager {
  display: flex;
  justify-content: center;
  padding-top: 10px;
}

@media (max-width: 900px) {
  .order-aside {
    width: 100%;
    border-left: none;
    padding-left: 0;
    border-top: 1px dashed #f0f0f0;
    padding-top: 12px;
  }
  .foot-address {
    max-width: 100%;
  }
}
</style>
