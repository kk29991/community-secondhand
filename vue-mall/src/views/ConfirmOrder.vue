<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOrder, getAddressList, getCartList, getProductDetail, previewOrder, saveAddress } from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const error = ref('')

const addresses = ref([])
const selectedAddressId = ref(null)
const preview = ref(null)
const remark = ref('')
const payType = ref(2)
const payImmediately = ref(true)

const addressDialogVisible = ref(false)
const addressSaving = ref(false)
const addressFormRef = ref(null)

const source = reactive({
  cartIds: [],
  productId: null,
  quantity: 1,
  fromCart: true
})

const addressForm = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const addressRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const FREE_FREIGHT = 99
const FREIGHT = 10

const selectedAddress = computed(() => addresses.value.find((a) => a.id === selectedAddressId.value) || null)

/** merchantId -> 店铺名: 后端结算预览未返回 storeName, 用购物车/商品详情补齐 */
const merchantNames = ref({})

async function fillMerchantNames() {
  const map = {}
  if (source.fromCart) {
    try {
      const cart = await getCartList()
      for (const item of cart || []) {
        if (item.merchantId && item.merchantName) map[item.merchantId] = item.merchantName
      }
    } catch (e) {
      // 忽略: 仅影响店铺名展示
    }
  } else if (source.productId) {
    try {
      const detail = await getProductDetail(source.productId)
      if (detail?.merchantId && detail?.merchantName) map[detail.merchantId] = detail.merchantName
    } catch (e) {
      // 忽略: 仅影响店铺名展示
    }
  }
  merchantNames.value = map
}

/** 按店铺分组 (后端的 groups 中 storeName 为空, 这里用地址/店铺兜底) */
const groups = computed(() => {
  return (preview.value?.groups || []).map((g, idx) => {
    const amount = Number(g.amount) || 0
    const freight = amount >= FREE_FREIGHT ? 0 : FREIGHT
    return {
      ...g,
      index: idx,
      storeName: g.storeName || merchantNames.value[g.merchantId] || `店铺 #${g.merchantId}`,
      amount,
      freight,
      payAmount: amount + freight
    }
  })
})

const goodsAmount = computed(() => groups.value.reduce((sum, g) => sum + g.amount, 0))
const freightAmount = computed(() => groups.value.reduce((sum, g) => sum + g.freight, 0))
const payAmount = computed(() => goodsAmount.value + freightAmount.value)
const totalQuantity = computed(() =>
  groups.value.reduce((sum, g) => sum + (g.items || []).reduce((s, i) => s + Number(i.quantity || 0), 0), 0)
)

function parseSource() {
  const q = route.query
  if (q.cartIds) {
    source.cartIds = String(q.cartIds)
      .split(',')
      .map((s) => Number(s.trim()))
      .filter((n) => Number.isFinite(n) && n > 0)
    source.fromCart = true
  } else if (q.productId) {
    source.productId = Number(q.productId)
    source.quantity = Math.max(1, Number(q.quantity) || 1)
    source.fromCart = false
  } else {
    source.fromCart = true
  }
}

async function loadAddresses() {
  try {
    const data = await getAddressList()
    addresses.value = Array.isArray(data) ? data : []
    if (!selectedAddressId.value || !addresses.value.some((a) => a.id === selectedAddressId.value)) {
      const def = addresses.value.find((a) => a.isDefault === 1)
      selectedAddressId.value = (def || addresses.value[0])?.id || null
    }
  } catch (e) {
    addresses.value = []
    selectedAddressId.value = null
  }
}

async function loadPreview() {
  if (!source.cartIds.length && !source.productId) {
    loading.value = false
    error.value = '没有可结算的商品, 请先从购物车选择或直接购买'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const params = source.fromCart
      ? { cartIds: source.cartIds.join(',') }
      : { productId: source.productId, quantity: source.quantity }
    const data = await previewOrder(params)
    preview.value = data
  } catch (e) {
    preview.value = null
    error.value = '结算信息加载失败, 请返回购物车重试'
  } finally {
    loading.value = false
  }
}

function openAddressDialog() {
  Object.assign(addressForm, {
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: addresses.value.length ? 0 : 1
  })
  addressDialogVisible.value = true
}

async function submitAddress() {
  if (!addressFormRef.value) return
  try {
    await addressFormRef.value.validate()
  } catch (e) {
    return
  }
  addressSaving.value = true
  try {
    await saveAddress({ ...addressForm })
    ElMessage.success('地址已保存')
    addressDialogVisible.value = false
    await loadAddresses()
  } catch (e) {
    // 拦截器已提示
  } finally {
    addressSaving.value = false
  }
}

async function submitOrder() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  if (!groups.value.length) {
    ElMessage.warning('没有可提交的商品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认提交订单? 应付金额 ￥${money(payAmount.value)}${payImmediately.value ? ' (提交后立即支付)' : ''}`,
      '提交订单',
      { confirmButtonText: '确认提交', cancelButtonText: '再想想', type: 'info' }
    )
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      addressId: selectedAddressId.value,
      payType: payType.value,
      remark: remark.value,
      payImmediately: payImmediately.value
    }
    if (source.fromCart) {
      payload.cartIds = source.cartIds
    } else {
      payload.productId = source.productId
      payload.quantity = source.quantity
    }
    const orders = await createOrder(payload)
    const ids = (Array.isArray(orders) ? orders : []).map((o) => o.id).filter(Boolean)
    await userStore.refreshCartCount()
    ElMessage.success('下单成功')
    router.replace({
      path: '/payResult',
      query: ids.length ? { orderIds: ids.join(','), paid: payImmediately.value ? '1' : '0' } : {}
    })
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  parseSource()
  await Promise.all([loadAddresses(), loadPreview(), fillMerchantNames()])
})
</script>

<template>
  <div class="confirm-page">
    <div class="mall-container">
      <div class="mall-page-head">
        <h2><el-icon><Document /></el-icon> 确认订单</h2>
        <el-steps :active="1" simple class="steps">
          <el-step title="我的购物车" />
          <el-step title="确认订单信息" />
          <el-step title="付款" />
          <el-step title="完成" />
        </el-steps>
      </div>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="router.push('/cart')">返回购物车</el-button>
        </template>
      </el-alert>

      <div v-loading="loading" element-loading-text="正在加载结算信息...">
        <template v-if="preview">
          <!-- 收货地址 -->
          <section class="panel">
            <div class="panel-head">
              <span class="panel-title">收货地址</span>
              <el-button text type="primary" @click="openAddressDialog">
                <el-icon><Plus /></el-icon> 新增地址
              </el-button>
            </div>

            <div v-if="addresses.length" class="address-list">
              <div
                v-for="addr in addresses"
                :key="addr.id"
                class="address-card"
                :class="{ active: selectedAddressId === addr.id }"
                @click="selectedAddressId = addr.id"
              >
                <div class="address-top">
                  <span class="receiver">{{ addr.receiverName }}</span>
                  <span class="phone">{{ addr.receiverPhone }}</span>
                  <el-tag v-if="addr.isDefault === 1" size="small" type="danger" effect="plain">默认</el-tag>
                </div>
                <p class="address-detail">
                  {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
                </p>
                <el-icon v-if="selectedAddressId === addr.id" class="check-icon"><SuccessFilled /></el-icon>
              </div>
            </div>
            <el-empty v-else :image-size="80" description="还没有收货地址, 请先新增">
              <el-button type="primary" @click="openAddressDialog">新增收货地址</el-button>
            </el-empty>
          </section>

          <!-- 商品清单 -->
          <section class="panel">
            <div class="panel-head">
              <span class="panel-title">商品清单</span>
              <span class="panel-tip">共 {{ totalQuantity }} 件商品</span>
            </div>

            <div v-for="group in groups" :key="group.merchantId" class="preview-group">
              <div class="group-head">
                <el-icon><Shop /></el-icon>
                <span>{{ group.storeName }}</span>
                <span class="group-freight">
                  服务费:
                  <em v-if="group.freight === 0">免收 (满 99 元)</em>
                  <em v-else>￥{{ money(group.freight) }}</em>
                </span>
              </div>

              <div class="preview-table-head">
                <span class="c-goods">商品信息</span>
                <span class="c-price">单价</span>
                <span class="c-qty">数量</span>
                <span class="c-sub">小计</span>
              </div>

              <div v-for="item in group.items" :key="item.productId + '-' + item.cartId" class="preview-item">
                <div class="c-goods">
                  <img :src="item.productImage || DEFAULT_IMAGE" :alt="item.productName" @error="fallbackImage($event, item.productName)" />
                  <span class="goods-name">{{ item.productName }}</span>
                </div>
                <span class="c-price">￥{{ money(item.price) }}</span>
                <span class="c-qty">x{{ item.quantity }}</span>
                <span class="c-sub mall-price">￥{{ money(item.totalPrice) }}</span>
              </div>
            </div>
          </section>

          <!-- 备注 + 支付方式 -->
          <section class="panel">
            <div class="panel-head">
              <span class="panel-title">备注与支付</span>
            </div>

            <div class="form-row">
              <span class="form-label">订单备注</span>
              <el-input
                v-model="remark"
                type="textarea"
                :rows="2"
                maxlength="200"
                show-word-limit
                placeholder="选填, 可填写对本次交易的特殊要求 (如发票信息、期望送达时间等)"
                class="remark-input"
              />
            </div>

            <div class="form-row">
              <span class="form-label">支付方式</span>
              <el-radio-group v-model="payType">
                <el-radio :value="1">余额支付</el-radio>
                <el-radio :value="2">支付宝</el-radio>
                <el-radio :value="3">微信支付</el-radio>
              </el-radio-group>
            </div>

            <div class="form-row">
              <span class="form-label">支付时机</span>
              <el-radio-group v-model="payImmediately">
                <el-radio :value="true">提交订单后立即支付 (模拟)</el-radio>
                <el-radio :value="false">先提交订单, 稍后支付</el-radio>
              </el-radio-group>
            </div>
          </section>

          <!-- 交易规则 + 金额 -->
          <section class="panel summary">
            <div class="rule-box">
              <h4>交易规则说明</h4>
              <ul>
                <li>本平台为社区闲置转让, 交易方式为<em>小区门口自提 / 物业中心代收</em>, 不产生物流费用;</li>
                <li>平台对单笔订单商品金额满 <em>99 元</em> 免收服务费, 未满 99 元收取 <em>10 元</em> 平台服务费 (按卖家分别计算);</li>
                <li>请<em>当面验货确认无误后</em>再签收; 二手物品不支持无理由退货, 详情见平台交易协议。</li>
              </ul>
            </div>

            <div class="amount-box">
              <div class="amount-line">
                <span>商品金额</span>
                <em>￥{{ money(goodsAmount) }}</em>
              </div>
              <div class="amount-line">
                <span>平台服务费</span>
                <em>￥{{ money(freightAmount) }}</em>
              </div>
              <div class="amount-line total">
                <span>应付金额</span>
                <span class="mall-price"><span class="symbol">￥</span><span class="num">{{ money(payAmount) }}</span></span>
              </div>

              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="submitting"
                @click="submitOrder"
              >
                提交订单
              </el-button>
              <p class="submit-tip">
                提交订单即表示同意
                <a href="javascript:;">《平台用户交易协议》</a>
              </p>
            </div>
          </section>
        </template>

        <div v-else-if="!loading && !error" class="mall-empty">
          <el-empty description="没有可结算的商品">
            <el-button type="primary" @click="router.push('/search')">去挑商品</el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <!-- 新增地址弹窗 -->
    <el-dialog v-model="addressDialogVisible" title="新增收货地址" width="520px" append-to-body>
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区">
          <div class="region-row">
            <el-input v-model="addressForm.province" placeholder="省" />
            <el-input v-model="addressForm.city" placeholder="市" />
            <el-input v-model="addressForm.district" placeholder="区/县" />
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="addressForm.detailAddress" type="textarea" :rows="2" placeholder="街道、门牌号等" maxlength="100" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addressSaving" @click="submitAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.confirm-page {
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

.steps {
  width: 560px;
  max-width: 100%;
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
}

.panel-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
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

.panel-tip {
  font-size: 13px;
  color: #b0b0b0;
}

/* 地址 */
.address-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.address-card {
  position: relative;
  border: 1px solid #e8e8e8;
  border-radius: var(--mall-radius);
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.address-card:hover {
  border-color: #ffb380;
}

.address-card.active {
  border-color: #ff6700;
  background: #fff8f4;
}

.address-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.receiver {
  font-size: 15px;
  color: #333;
}

.phone {
  font-size: 13px;
  color: #757575;
}

.address-detail {
  margin: 0;
  font-size: 13px;
  color: #757575;
  line-height: 1.6;
}

.check-icon {
  position: absolute;
  right: 10px;
  bottom: 10px;
  color: #ff6700;
  font-size: 18px;
}

/* 商品清单 */
.preview-group {
  border: 1px solid #f0f0f0;
  border-radius: var(--mall-radius);
  margin-bottom: 14px;
  overflow: hidden;
}

.group-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  background: #fcfcfc;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
  color: #333;
}

.group-freight {
  margin-left: auto;
  font-size: 12px;
  color: #9e9e9e;
}

.group-freight em {
  font-style: normal;
  color: #ff6700;
}

.preview-table-head,
.preview-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
}

.preview-table-head {
  font-size: 12px;
  color: #b0b0b0;
  border-bottom: 1px dashed #f0f0f0;
}

.preview-item {
  border-bottom: 1px solid #f7f7f7;
  font-size: 14px;
  color: #616161;
}

.preview-item:last-child {
  border-bottom: none;
}

.c-goods {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.c-goods img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  background: #fafafa;
  border: 1px solid #f2f2f2;
  flex-shrink: 0;
}

.goods-name {
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.c-price {
  width: 110px;
  text-align: center;
}

.c-qty {
  width: 90px;
  text-align: center;
}

.c-sub {
  width: 120px;
  text-align: right;
}

/* 表单 */
.form-row {
  display: flex;
  gap: 16px;
  padding: 10px 0;
  align-items: flex-start;
}

.form-label {
  width: 84px;
  flex-shrink: 0;
  font-size: 14px;
  color: #757575;
  line-height: 32px;
}

.remark-input {
  max-width: 720px;
}

/* 汇总 */
.summary {
  display: flex;
  gap: 30px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.rule-box {
  flex: 1;
  min-width: 300px;
  background: #fff8f4;
  border-radius: var(--mall-radius);
  padding: 16px 20px;
}

.rule-box h4 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 500;
  color: #ff6700;
}

.rule-box ul {
  padding-left: 4px;
}

.rule-box li {
  font-size: 13px;
  color: #757575;
  line-height: 2;
  padding-left: 14px;
  position: relative;
}

.rule-box li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 13px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #ff6700;
  opacity: 0.6;
}

.rule-box em {
  font-style: normal;
  color: #ff6700;
  font-weight: 600;
}

.amount-box {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid #f0f0f0;
  padding-left: 26px;
}

.amount-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: #616161;
  padding: 8px 0;
}

.amount-line em {
  font-style: normal;
}

.amount-line.total {
  border-top: 1px dashed #f0f0f0;
  margin-top: 6px;
  padding-top: 14px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 24px;
  margin-top: 16px;
}

.submit-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #b0b0b0;
  text-align: center;
}

.submit-tip a {
  color: #ff6700;
}

.region-row {
  display: flex;
  gap: 10px;
}

@media (max-width: 1000px) {
  .address-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .amount-box {
    width: 100%;
    border-left: none;
    padding-left: 0;
  }
}
</style>
