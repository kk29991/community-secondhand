<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  addToCart,
  getMerchantTags,
  getProductDetail,
  getProductReviews,
  getProducts,
  toggleCollect
} from '@/api/index'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'
import { resolveImages, money, formatDate, formatTime, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')
const product = ref(null)
const tags = ref([])
const activeImage = ref('')
const quantity = ref(1)
const activeTab = ref('detail')
const collecting = ref(false)

// 评价
const reviewLoading = ref(false)
const reviews = ref([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewPageSize = ref(5)

// 相关推荐
const recommend = ref([])

/** 兼容 ?pid=xx 与 ?id=xx 两种参数 */
const productId = computed(() => {
  const raw = route.query.pid ?? route.query.id
  const num = Number(raw)
  return Number.isFinite(num) && num > 0 ? num : null
})

const images = computed(() => resolveImages(product.value?.images, product.value?.mainImage))
const off = computed(() => {
  const price = Number(product.value?.price)
  const origin = Number(product.value?.originalPrice)
  if (!Number.isFinite(price) || !Number.isFinite(origin) || origin <= price) return 0
  return (origin - price).toFixed(2)
})
const isCollected = computed(() => product.value?.collected === true)
const soldOut = computed(() => Number(product.value?.stock) <= 0)

function decodeHtml(html) {
  // 后端详情为富文本 HTML, 直接渲染 (毕业设计内部数据)
  return html || '<p style="color:#b0b0b0">暂无商品详情</p>'
}

async function loadProduct() {
  if (!productId.value) {
    loading.value = false
    error.value = '缺少商品参数, 请从商品列表进入'
    return
  }
  loading.value = true
  error.value = ''
  quantity.value = 1
  activeTab.value = 'detail'
  try {
    const data = await getProductDetail(productId.value)
    product.value = data
    activeImage.value = resolveImages(data?.images, data?.mainImage)[0] || DEFAULT_IMAGE
    // 商家标签: 详情已带 tags, 仍按需求调用 getMerchantTags 获取完整标签
    tags.value = Array.isArray(data?.tags) ? data.tags : []
    if (data?.merchantId) {
      getMerchantTags(data.merchantId)
        .then((res) => {
          if (Array.isArray(res) && res.length) tags.value = res
        })
        .catch(() => {})
    }
    // 同类推荐(同分类商品)
    if (data?.categoryId) {
      getProducts({ pageNum: 1, pageSize: 5, categoryId: data.categoryId, includeChildren: true })
        .then((res) => {
          recommend.value = (res?.records || []).filter((p) => p.id !== data.id).slice(0, 4)
        })
        .catch(() => {})
    }
  } catch (e) {
    product.value = null
    error.value = '商品详情加载失败, 该商品可能已下架或不存在'
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  if (!productId.value) return
  reviewLoading.value = true
  try {
    const data = await getProductReviews(productId.value, {
      pageNum: reviewPage.value,
      pageSize: reviewPageSize.value
    })
    reviews.value = data?.records || []
    reviewTotal.value = Number(data?.total) || 0
  } catch (e) {
    reviews.value = []
    reviewTotal.value = 0
  } finally {
    reviewLoading.value = false
  }
}

function changeReviewPage(page) {
  reviewPage.value = page
  loadReviews()
}

function requireLogin() {
  if (userStore.isLogin) return true
  ElMessage.warning('请先登录')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function handleAddCart() {
  if (!requireLogin()) return
  try {
    await addToCart(productId.value, quantity.value)
    ElMessage.success(`已加入购物车 ${quantity.value} 件`)
    await userStore.refreshCartCount()
  } catch (e) {
    // 拦截器已提示
  }
}

function handleBuyNow() {
  if (!requireLogin()) return
  router.push({ path: '/confirmOrder', query: { productId: productId.value, quantity: quantity.value } })
}

async function handleCollect() {
  if (!requireLogin()) return
  collecting.value = true
  try {
    const collected = await toggleCollect(productId.value)
    if (product.value) product.value.collected = collected === true
    ElMessage.success(collected ? '收藏成功' : '已取消收藏')
  } catch (e) {
    // 拦截器已提示
  } finally {
    collecting.value = false
  }
}

watch(
  () => route.query,
  () => {
    if (route.path !== '/goodsDetail') return
    reviewPage.value = 1
    loadProduct()
    loadReviews()
  }
)

onMounted(() => {
  loadProduct()
  loadReviews()
})
</script>

<template>
  <div class="detail-page">
    <div class="mall-container">
      <!-- 面包屑 -->
      <el-breadcrumb class="crumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="product?.categoryName" :to="{ path: '/search', query: { categoryId: product.categoryId } }">
          {{ product.categoryName }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ product?.name || '商品详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="router.push('/search')">返回商品列表</el-button>
        </template>
      </el-alert>

      <!-- 主体 -->
      <div v-loading="loading" element-loading-text="正在加载商品详情...">
        <div v-if="product" class="detail-main">
          <!-- 左侧图库 -->
          <div class="gallery">
            <div class="main-image">
              <img :src="activeImage || DEFAULT_IMAGE" :alt="product.name" @error="fallbackImage($event, product.name)" />
            </div>
            <div v-if="images.length > 1" class="thumbs">
              <div
                v-for="(img, idx) in images"
                :key="idx"
                class="thumb"
                :class="{ active: activeImage === img }"
                @mouseenter="activeImage = img"
              >
                <img :src="img" :alt="`${product.name}-${idx + 1}`" @error="fallbackImage($event, product.name)" />
              </div>
            </div>
          </div>

          <!-- 右侧信息 -->
          <div class="info">
            <h1 class="goods-name">{{ product.name }}</h1>
            <p v-if="product.subTitle" class="goods-subtitle">{{ product.subTitle }}</p>

            <div v-if="tags.length" class="tag-row">
              <span
                v-for="tag in tags"
                :key="tag.id"
                class="tag"
                :style="{ color: tag.color || '#ff6700', borderColor: tag.color || '#ff6700' }"
              >
                {{ tag.name }}
              </span>
            </div>

            <div class="price-box">
              <div class="price-left">
                <span class="mall-price">
                  <span class="symbol">￥</span><span class="num">{{ money(product.price) }}</span>
                </span>
                <span v-if="Number(product.originalPrice) > Number(product.price)" class="mall-price-old">
                  ￥{{ money(product.originalPrice) }}
                </span>
                <el-tag v-if="off > 0" class="off-tag" type="danger" effect="dark" size="small">立省 ￥{{ off }}</el-tag>
              </div>
              <div class="price-right">
                <el-rate :model-value="Number(product.avgScore) || 0" disabled show-score :score-template="`${product.avgScore || 0} 分`" />
                <span class="review-count">{{ product.reviewCount || 0 }} 条评价</span>
              </div>
            </div>

            <div class="stat-row">
              <div class="stat">
                <span class="stat-label">已售</span>
                <span class="stat-value">{{ product.sales ?? 0 }} {{ product.unit || '件' }}</span>
              </div>
              <div class="stat">
                <span class="stat-label">库存</span>
                <span class="stat-value" :class="{ danger: soldOut }">{{ product.stock ?? 0 }} {{ product.unit || '件' }}</span>
              </div>
              <div class="stat">
                <span class="stat-label">上架日期</span>
                <span class="stat-value">{{ formatDate(product.createTime) }}</span>
              </div>
            </div>

            <!-- 店铺信息 -->
            <div class="store-box">
              <img class="store-logo" :src="product.storeLogo || DEFAULT_IMAGE" alt="店铺Logo" @error="fallbackImage($event, product.merchantName || '店铺')" />
              <div class="store-info">
                <div class="store-name">
                  <el-icon><Shop /></el-icon>
                  {{ product.merchantName || '个人卖家' }}
                </div>
                <div class="store-tags">
                  <span
                    v-for="tag in tags.slice(0, 4)"
                    :key="tag.id"
                    class="store-tag"
                    :style="{ color: tag.color || '#ff6700', background: `${tag.color || '#ff6700'}1a` }"
                  >
                    {{ tag.name }}
                  </span>
                  <span v-if="!tags.length" class="store-tag muted">暂无商家标签</span>
                </div>
              </div>
              <div class="store-meta">
                <el-icon><Van /></el-icon>
                小区当面交易
              </div>
            </div>

            <!-- 数量 + 操作 -->
            <div class="buy-row">
              <span class="buy-label">数量</span>
              <el-input-number v-model="quantity" :min="1" :max="Math.max(1, Number(product.stock) || 1)" :disabled="soldOut" />
              <span class="stock-tip">（库存 {{ product.stock ?? 0 }} {{ product.unit || '件' }}）</span>
            </div>

            <div class="action-row">
              <el-button type="primary" size="large" :disabled="soldOut" @click="handleAddCart">
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button type="warning" size="large" :disabled="soldOut" @click="handleBuyNow">
                <el-icon><Wallet /></el-icon>
                立即购买
              </el-button>
              <el-button size="large" :loading="collecting" @click="handleCollect">
                <el-icon :class="{ collected: isCollected }">
                  <component :is="isCollected ? 'StarFilled' : 'Star'" />
                </el-icon>
                {{ isCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>

            <div class="service-row">
              <span><el-icon><CircleCheck /></el-icon> 正品保障</span>
              <span><el-icon><CircleCheck /></el-icon> 7 天无理由退货</span>
              <span><el-icon><CircleCheck /></el-icon> 15 天免费换货</span>
            </div>
          </div>
        </div>

        <div v-else-if="!loading && !error" class="mall-card">
          <el-empty description="商品不存在或已下架" />
        </div>
      </div>

      <!-- Tabs -->
      <div v-if="product" class="detail-tabs mall-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div class="rich-detail" v-html="decodeHtml(product.detail)"></div>
            <div class="spec-list">
              <div class="spec-item"><span>商品名称</span><em>{{ product.name }}</em></div>
              <div class="spec-item"><span>商品分类</span><em>{{ product.categoryName || '-' }}</em></div>
              <div class="spec-item"><span>所属店铺</span><em>{{ product.merchantName || '-' }}</em></div>
              <div class="spec-item"><span>计量单位</span><em>{{ product.unit || '件' }}</em></div>
              <div class="spec-item"><span>当前库存</span><em>{{ product.stock ?? 0 }}</em></div>
              <div class="spec-item"><span>累计销量</span><em>{{ product.sales ?? 0 }}</em></div>
              <div class="spec-item"><span>上架时间</span><em>{{ formatTime(product.createTime) }}</em></div>
            </div>
          </el-tab-pane>

          <el-tab-pane name="reviews">
            <template #label>
              <span>历史评价 <em class="tab-count">({{ reviewTotal }})</em></span>
            </template>

            <div v-loading="reviewLoading" class="review-wrap">
              <div v-if="reviews.length" class="review-list">
                <div v-for="rv in reviews" :key="rv.id" class="review-item">
                  <img class="review-avatar" :src="rv.avatar || DEFAULT_IMAGE" alt="头像" @error="fallbackImage($event, '用户')" />
                  <div class="review-body">
                    <div class="review-head">
                      <span class="review-user">{{ rv.displayName || rv.nickname || '匿名用户' }}</span>
                      <el-rate :model-value="Number(rv.score) || 0" disabled size="small" />
                      <span class="review-time">{{ formatTime(rv.createTime) }}</span>
                    </div>
                    <p class="review-content">{{ rv.content || '该用户没有填写评价内容' }}</p>
                    <div v-if="rv.imageList && rv.imageList.length" class="review-images">
                      <el-image
                        v-for="(img, i) in rv.imageList"
                        :key="i"
                        class="review-image"
                        :src="img"
                        :preview-src-list="rv.imageList"
                        :initial-index="i"
                        fit="cover"
                        preview-teleported
                      />
                    </div>
                    <div v-if="rv.replyContent" class="merchant-reply">
                      <span class="reply-label">商家回复:</span>
                      <span class="reply-content">{{ rv.replyContent }}</span>
                      <span class="reply-time">{{ formatTime(rv.replyTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-else-if="!reviewLoading" description="还没有评价, 快来抢沙发" />

              <div v-if="reviewTotal > reviewPageSize" class="review-pager">
                <el-pagination
                  background
                  layout="prev, pager, next"
                  :total="reviewTotal"
                  :page-size="reviewPageSize"
                  :current-page="reviewPage"
                  @current-change="changeReviewPage"
                />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 相关推荐 -->
      <section v-if="recommend.length" class="recommend">
        <div class="mall-section-title">
          <span class="title-text">同类好物</span>
        </div>
        <div class="recommend-grid">
          <ProductCard v-for="p in recommend" :key="p.id" :product="p" />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  padding-top: 16px;
}

.mb16 {
  margin-bottom: 16px;
}

.crumb {
  margin-bottom: 14px;
  font-size: 13px;
}

.detail-main {
  display: flex;
  gap: 30px;
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 26px;
  margin-bottom: 16px;
  min-height: 460px;
}

/* ---------- 图库 ---------- */
.gallery {
  width: 420px;
  flex-shrink: 0;
}

.main-image {
  width: 100%;
  height: 420px;
  border-radius: var(--mall-radius);
  overflow: hidden;
  background: #fafafa;
  border: 1px solid #f2f2f2;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.thumb {
  width: 68px;
  height: 68px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fafafa;
}

.thumb.active,
.thumb:hover {
  border-color: #ff6700;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ---------- 信息 ---------- */
.info {
  flex: 1;
  min-width: 0;
}

.goods-name {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 500;
  line-height: 1.35;
  color: #212121;
}

.goods-subtitle {
  margin: 0 0 12px;
  font-size: 14px;
  color: #b0b0b0;
  line-height: 1.5;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.tag {
  font-size: 12px;
  border: 1px solid;
  border-radius: 4px;
  padding: 1px 7px;
  line-height: 18px;
}

.price-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #fff8f4;
  border-radius: var(--mall-radius);
  padding: 16px 18px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.price-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-left .mall-price .num {
  font-size: 32px;
}

.off-tag {
  margin-left: 6px;
}

.price-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-count {
  font-size: 12px;
  color: #b0b0b0;
}

.stat-row {
  display: flex;
  gap: 42px;
  padding: 0 2px 16px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #b0b0b0;
}

.stat-value {
  font-size: 15px;
  color: #333;
}

.stat-value.danger {
  color: #ff4d4f;
}

/* 店铺 */
.store-box {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fafafa;
  border-radius: var(--mall-radius);
  padding: 12px 16px;
  margin-bottom: 18px;
}

.store-logo {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: #fff;
}

.store-info {
  flex: 1;
  min-width: 0;
}

.store-name {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 15px;
  color: #333;
  margin-bottom: 6px;
}

.store-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.store-tag {
  font-size: 11px;
  line-height: 18px;
  padding: 0 7px;
  border-radius: 3px;
}

.store-tag.muted {
  color: #b0b0b0;
  background: #f0f0f0;
}

.store-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #ff6700;
  flex-shrink: 0;
}

/* 购买 */
.buy-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.buy-label {
  font-size: 14px;
  color: #757575;
}

.stock-tip {
  font-size: 12px;
  color: #b0b0b0;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.action-row .el-button {
  min-width: 130px;
}

.collected {
  color: #ff6700;
}

.service-row {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #9e9e9e;
  flex-wrap: wrap;
}

.service-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.service-row .el-icon {
  color: #52c41a;
}

/* ---------- Tabs ---------- */
.detail-tabs {
  padding: 8px 26px 26px;
  margin-bottom: 16px;
}

.rich-detail {
  line-height: 1.9;
  color: #424242;
  font-size: 14px;
  padding: 10px 0 20px;
}

.rich-detail :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 10px 0;
}

.spec-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  background: #f0f0f0;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.spec-item {
  display: flex;
  background: #fff;
  padding: 12px 16px;
  font-size: 13px;
}

.spec-item span {
  width: 90px;
  color: #b0b0b0;
  flex-shrink: 0;
}

.spec-item em {
  font-style: normal;
  color: #333;
}

.tab-count {
  font-style: normal;
  color: #b0b0b0;
  font-size: 12px;
}

/* 评价 */
.review-wrap {
  min-height: 160px;
  padding-top: 8px;
}

.review-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid #f5f5f5;
}

.review-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #f5f5f5;
}

.review-body {
  flex: 1;
  min-width: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.review-user {
  font-size: 14px;
  color: #333;
}

.review-time {
  font-size: 12px;
  color: #c8c8c8;
  margin-left: auto;
}

.review-content {
  margin: 0 0 10px;
  font-size: 14px;
  color: #424242;
  line-height: 1.7;
}

.review-images {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.review-image {
  width: 84px;
  height: 84px;
  border-radius: 6px;
  overflow: hidden;
}

.merchant-reply {
  background: #fff8f4;
  border-left: 3px solid #ff6700;
  border-radius: 0 6px 6px 0;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.7;
}

.reply-label {
  color: #ff6700;
  margin-right: 6px;
}

.reply-content {
  color: #616161;
}

.reply-time {
  display: block;
  color: #c8c8c8;
  font-size: 12px;
  margin-top: 4px;
}

.review-pager {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}

/* 推荐 */
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1000px) {
  .detail-main {
    flex-direction: column;
  }
  .gallery {
    width: 100%;
  }
  .recommend-grid,
  .spec-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
