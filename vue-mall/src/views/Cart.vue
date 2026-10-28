<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearCart,
  getCartList,
  removeCartItems,
  updateCartChecked,
  updateCartCheckedAll,
  updateCartQuantity
} from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const items = ref([])
const updating = ref(false)

/** 按店铺分组 */
const groups = computed(() => {
  const map = new Map()
  for (const item of items.value) {
    const key = item.merchantId ?? 0
    if (!map.has(key)) {
      map.set(key, {
        merchantId: key,
        merchantName: item.merchantName || '未知卖家',
        items: []
      })
    }
    map.get(key).items.push(item)
  }
  return Array.from(map.values())
})

const validItems = computed(() => items.value.filter((i) => !i.invalid))
const checkedItems = computed(() => validItems.value.filter((i) => i.checked === 1))
const allChecked = computed(() => validItems.value.length > 0 && checkedItems.value.length === validItems.value.length)
const someChecked = computed(() => checkedItems.value.length > 0 && !allChecked.value)
const totalCount = computed(() => checkedItems.value.reduce((sum, i) => sum + Number(i.quantity || 0), 0))
const totalPrice = computed(() =>
  checkedItems.value.reduce((sum, i) => sum + Number(i.price || 0) * Number(i.quantity || 0), 0)
)

/** 运费规则: 单笔订单满 99 元免运费, 否则 10 元 (按店铺分别计算) */
const freightInfo = computed(() => {
  const shopIds = new Set(checkedItems.value.map((i) => i.merchantId))
  if (!shopIds.size) return { freight: 0, shops: 0 }
  let freight = 0
  for (const shopId of shopIds) {
    const amount = checkedItems.value
      .filter((i) => i.merchantId === shopId)
      .reduce((sum, i) => sum + Number(i.price || 0) * Number(i.quantity || 0), 0)
    if (amount < 99) freight += 10
  }
  return { freight, shops: shopIds.size }
})

function isGroupAllChecked(group) {
  const valid = group.items.filter((i) => !i.invalid)
  return valid.length > 0 && valid.every((i) => i.checked === 1)
}

function isGroupSomeChecked(group) {
  const valid = group.items.filter((i) => !i.invalid)
  const checked = valid.filter((i) => i.checked === 1)
  return checked.length > 0 && checked.length < valid.length
}

async function loadCart() {
  loading.value = true
  error.value = ''
  try {
    const data = await getCartList()
    items.value = Array.isArray(data) ? data : []
  } catch (e) {
    items.value = []
    error.value = '购物车加载失败, 请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 单项勾选 */
async function toggleItem(item, value) {
  const next = value ? 1 : 0
  const prev = item.checked
  item.checked = next
  try {
    await updateCartChecked(item.id, next)
  } catch (e) {
    item.checked = prev
  }
}

/** 全选 / 全不选 */
async function toggleAll(value) {
  const next = value ? 1 : 0
  const snapshot = items.value.map((i) => i.checked)
  items.value.forEach((i) => {
    if (!i.invalid) i.checked = next
  })
  updating.value = true
  try {
    await updateCartCheckedAll(next)
    await userStore.refreshCartCount()
  } catch (e) {
    items.value.forEach((i, idx) => {
      i.checked = snapshot[idx]
    })
  } finally {
    updating.value = false
  }
}

/** 店铺维度全选 */
async function toggleGroup(group, value) {
  const next = value ? 1 : 0
  const targets = group.items.filter((i) => !i.invalid)
  const snapshot = targets.map((i) => i.checked)
  targets.forEach((i) => {
    i.checked = next
  })
  try {
    await Promise.all(targets.map((i) => updateCartChecked(i.id, next)))
  } catch (e) {
    targets.forEach((i, idx) => {
      i.checked = snapshot[idx]
    })
  }
}

/** 修改数量 */
async function changeQuantity(item, value) {
  const max = Math.max(1, Number(item.stock) || 1)
  const next = Math.min(Math.max(1, Number(value) || 1), max)
  if (next === item.quantity) return
  const prev = item.quantity
  item.quantity = next
  updating.value = true
  try {
    await updateCartQuantity(item.id, next)
    item.totalPrice = Number(item.price) * next
    await userStore.refreshCartCount()
  } catch (e) {
    item.quantity = prev
  } finally {
    updating.value = false
  }
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.productName}」吗?`, '删除商品', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await removeCartItems([item.id])
    items.value = items.value.filter((i) => i.id !== item.id)
    ElMessage.success('已删除')
    await userStore.refreshCartCount()
  } catch (e) {
    // 拦截器已提示
  }
}

async function batchRemove() {
  if (!checkedItems.value.length) {
    ElMessage.warning('请先选择要删除的商品')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${checkedItems.value.length} 件商品吗?`, '批量删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await removeCartItems(checkedItems.value.map((i) => i.id))
    items.value = items.value.filter((i) => i.checked !== 1)
    ElMessage.success('已删除所选商品')
    await userStore.refreshCartCount()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleClear() {
  if (!items.value.length) return
  try {
    await ElMessageBox.confirm('确定要清空购物车吗? 该操作不可撤销。', '清空购物车', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await clearCart()
    items.value = []
    ElMessage.success('购物车已清空')
    await userStore.refreshCartCount()
  } catch (e) {
    // 拦截器已提示
  }
}

function goCheckout() {
  if (!checkedItems.value.length) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  const invalid = checkedItems.value.filter((i) => i.invalid)
  if (invalid.length) {
    ElMessage.warning('选中的商品中包含失效商品, 请先移除')
    return
  }
  router.push({ path: '/confirmOrder', query: { cartIds: checkedItems.value.map((i) => i.id).join(',') } })
}

onMounted(() => {
  loadCart()
  userStore.refreshCartCount()
})
</script>

<template>
  <div class="cart-page">
    <div class="mall-container">
      <div class="mall-page-head">
        <h2>
          <el-icon><ShoppingCart /></el-icon>
          我的购物车
        </h2>
        <span class="head-tip">小区门口自提 / 物业中心代收 · 无运费</span>
      </div>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="loadCart">重新加载</el-button>
        </template>
      </el-alert>

      <div v-loading="loading" element-loading-text="正在加载购物车...">
        <template v-if="items.length">
          <div class="cart-table-head">
            <el-checkbox :model-value="allChecked" :indeterminate="someChecked" @change="toggleAll">全选</el-checkbox>
            <span class="col-goods">商品信息</span>
            <span class="col-price">单价</span>
            <span class="col-qty">数量</span>
            <span class="col-sub">小计</span>
            <span class="col-op">操作</span>
          </div>

          <div v-for="group in groups" :key="group.merchantId" class="shop-group">
            <div class="shop-head">
              <el-checkbox
                :model-value="isGroupAllChecked(group)"
                :indeterminate="isGroupSomeChecked(group)"
                @change="(val) => toggleGroup(group, val)"
              >
                <span class="shop-name">
                  <el-icon><Shop /></el-icon>
                  {{ group.merchantName }}
                </span>
              </el-checkbox>
              <span class="shop-count">共 {{ group.items.length }} 件商品</span>
            </div>

            <div v-for="item in group.items" :key="item.id" class="cart-item" :class="{ invalid: item.invalid }">
              <el-checkbox
                class="item-check"
                :model-value="item.checked === 1"
                :disabled="item.invalid"
                @change="(val) => toggleItem(item, val)"
              />
              <div class="item-goods" @click="router.push({ path: '/goodsDetail', query: { pid: item.productId } })">
                <img :src="item.productImage || DEFAULT_IMAGE" :alt="item.productName" @error="fallbackImage($event, item.productName)" />
                <div class="item-info">
                  <p class="item-name">{{ item.productName }}</p>
                  <p class="item-meta">
                    <span v-if="item.invalid" class="invalid-tag">商品已下架 / 失效</span>
                    <span v-else>库存 {{ item.stock }} 件</span>
                  </p>
                </div>
              </div>
              <div class="item-price">￥{{ money(item.price) }}</div>
              <div class="item-qty">
                <el-input-number
                  :model-value="item.quantity"
                  :min="1"
                  :max="Math.max(1, Number(item.stock) || 1)"
                  size="small"
                  :disabled="item.invalid"
                  @change="(val) => changeQuantity(item, val)"
                />
              </div>
              <div class="item-sub mall-price">
                <span class="symbol">￥</span><span class="num">{{ money(Number(item.price) * Number(item.quantity)) }}</span>
              </div>
              <div class="item-op">
                <el-button text type="danger" size="small" @click="removeItem(item)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 结算栏 -->
          <div class="cart-footer">
            <div class="footer-left">
              <el-checkbox :model-value="allChecked" :indeterminate="someChecked" @change="toggleAll">全选</el-checkbox>
              <el-button text type="danger" @click="batchRemove">删除选中</el-button>
              <el-button text @click="handleClear">清空购物车</el-button>
            </div>
            <div class="footer-right">
              <div class="amount-box">
                <div class="amount-line">
                  已选 <em>{{ totalCount }}</em> 件, 商品合计:
                  <span class="mall-price"><span class="symbol">￥</span><span class="num">{{ money(totalPrice) }}</span></span>
                </div>
                <div class="amount-line sub">
                  交易方式: <em>小区门口自提 / 物业中心代收</em>
                  <span class="freight-tip">（{{ freightInfo.shops }} 位卖家, 下单后与卖家约定时间地点）</span>
                </div>
              </div>
              <el-button type="primary" size="large" class="checkout-btn" @click="goCheckout">
                去结算
              </el-button>
            </div>
          </div>
        </template>

        <div v-else-if="!loading" class="mall-empty">
          <el-empty description="购物车还是空的, 快去挑选心仪的商品吧">
            <el-button type="primary" @click="router.push('/search')">去逛逛</el-button>
          </el-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  padding-top: 18px;
}

.mb16 {
  margin-bottom: 16px;
}

.mall-page-head h2 {
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-tip {
  font-size: 13px;
  color: #ff6700;
}

.cart-table-head {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: var(--mall-radius) var(--mall-radius) 0 0;
  padding: 14px 20px;
  font-size: 14px;
  color: #757575;
  margin-bottom: 12px;
}

.col-goods {
  margin-left: 22px;
  flex: 1;
}

.col-price,
.col-sub {
  width: 120px;
  text-align: center;
}

.col-qty {
  width: 160px;
  text-align: center;
}

.col-op {
  width: 90px;
  text-align: center;
}

.shop-group {
  background: #fff;
  border-radius: var(--mall-radius);
  margin-bottom: 14px;
  overflow: hidden;
}

.shop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f5f5f5;
  background: #fcfcfc;
}

.shop-name {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 15px;
  color: #333;
}

.shop-count {
  font-size: 12px;
  color: #b0b0b0;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid #f7f7f7;
  transition: background 0.2s;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background: #fffaf6;
}

.cart-item.invalid {
  opacity: 0.6;
}

.item-check {
  margin-right: 10px;
}

.item-goods {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-goods img {
  width: 84px;
  height: 84px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
  background: #fafafa;
  border: 1px solid #f2f2f2;
}

.item-info {
  min-width: 0;
}

.item-name {
  margin: 0 0 8px;
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-name:hover {
  color: #ff6700;
}

.item-meta {
  margin: 0;
  font-size: 12px;
  color: #b0b0b0;
}

.invalid-tag {
  color: #ff4d4f;
}

.item-price {
  width: 120px;
  text-align: center;
  font-size: 14px;
  color: #616161;
}

.item-qty {
  width: 160px;
  display: flex;
  justify-content: center;
}

.item-sub {
  width: 120px;
  text-align: center;
}

.item-op {
  width: 90px;
  text-align: center;
}

/* 结算栏 */
.cart-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #fff;
  border-radius: var(--mall-radius);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 14px 20px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.amount-box {
  text-align: right;
  font-size: 14px;
  color: #616161;
}

.amount-line {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 4px;
}

.amount-line em {
  font-style: normal;
  color: #ff6700;
  font-weight: 600;
  padding: 0 2px;
}

.amount-line.sub {
  font-size: 12px;
  color: #9e9e9e;
  margin-top: 4px;
}

.amount-line.sub em {
  color: #616161;
  font-weight: 400;
}

.freight-tip {
  color: #c8c8c8;
}

.checkout-btn {
  min-width: 160px;
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
}

@media (max-width: 1000px) {
  .cart-table-head {
    display: none;
  }
  .cart-item {
    flex-wrap: wrap;
    gap: 10px;
  }
  .item-price,
  .item-qty,
  .item-sub,
  .item-op {
    width: auto;
  }
}
</style>
