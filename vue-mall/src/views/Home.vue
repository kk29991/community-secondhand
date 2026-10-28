<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAnnouncements, getBanners, getHotProducts, getProducts } from '@/api/index'
import { useCategoryStore } from '@/stores/category'
import ProductCard from '@/components/ProductCard.vue'
import { placeholder } from '@/utils/placeholder'

const router = useRouter()
const categoryStore = useCategoryStore()

const loading = ref(true)
const error = ref('')
const banners = ref([])
const announcements = ref([])
const products = ref([])
const activeCategoryId = ref(null)

const categories = computed(() => categoryStore.categories)

/** 把后端 banner.linkUrl (/front/goodsDetail?pid=17) 转成前端 hash 路由 */
function normalizeLink(linkUrl) {
  if (!linkUrl) return '/search'
  let url = String(linkUrl).trim()
  url = url.replace(/^https?:\/\/[^/]+/, '')
  url = url.replace(/^\/?front/, '')
  if (!url.startsWith('/')) url = '/' + url
  return url || '/search'
}

function goBanner(banner) {
  const target = normalizeLink(banner.linkUrl)
  const [path, query] = target.split('?')
  const q = {}
  if (query) {
    new URLSearchParams(query).forEach((v, k) => {
      q[k] = v
    })
  }
  router.push({ path, query: q })
}

function goCategory(cat) {
  router.push({ path: '/search', query: { categoryId: cat.id } })
}

function goSearchKeyword(kw) {
  router.push({ path: '/search', query: kw ? { keyword: kw } : {} })
}

async function loadData() {
  loading.value = true
  error.value = ''
  const [bannerRes, annRes, hotRes, listRes] = await Promise.allSettled([
    getBanners(1),
    getAnnouncements(5),
    getHotProducts(10),
    getProducts({ pageNum: 1, pageSize: 20, sort: 'default' })
  ])

  banners.value = bannerRes.status === 'fulfilled' && Array.isArray(bannerRes.value) ? bannerRes.value : []
  announcements.value = annRes.status === 'fulfilled' && Array.isArray(annRes.value) ? annRes.value : []

  let list = []
  if (hotRes.status === 'fulfilled' && Array.isArray(hotRes.value)) list = hotRes.value
  if (!list.length && listRes.status === 'fulfilled' && listRes.value?.records) list = listRes.value.records
  products.value = list

  const allFailed = [bannerRes, annRes, hotRes, listRes].every((r) => r.status === 'rejected')
  if (allFailed) {
    error.value = '商品数据加载失败, 请确认后端服务 (http://localhost:9090) 是否已启动'
  }
  loading.value = false
}

onMounted(() => {
  categoryStore.load()
  loadData()
})
</script>

<template>
  <div class="home">
    <!-- 公告条 -->
    <div class="mall-container">
      <div class="notice-bar">
        <span class="notice-label">
          <el-icon><Bell /></el-icon>
          公告
        </span>
        <div class="notice-body">
          <template v-if="announcements.length">
            <el-carousel height="28px" direction="vertical" :interval="4000" indicator-position="none" arrow="never">
              <el-carousel-item v-for="item in announcements" :key="item.id">
                <span class="notice-text" @click="goSearchKeyword('')">
                  <em v-if="item.isTop === 1" class="top-flag">顶</em>
                  {{ item.title }}
                  <span class="notice-content">{{ item.content }}</span>
                </span>
              </el-carousel-item>
            </el-carousel>
          </template>
          <span v-else class="notice-text muted">欢迎来到社区二手街, 小区当面交易 · 无运费</span>
        </div>
        <div class="notice-links">
          <router-link to="/ai"><el-icon><MagicStick /></el-icon> AI 导购</router-link>
          <router-link to="/user/orders">我的订单</router-link>
          <router-link to="/user/address">收货地址</router-link>
        </div>
      </div>
    </div>

    <!-- 首屏: 分类 + 轮播 -->
    <div class="mall-container">
      <div class="hero">
        <!-- 左侧分类导航 -->
        <aside class="cat-nav">
          <ul>
            <li v-for="cat in categories" :key="cat.id" class="cat-item" @mouseenter="activeCategoryId = cat.id">
              <a href="javascript:;" @click="goCategory(cat)">
                <span class="cat-name">{{ cat.name }}</span>
                <el-icon class="cat-arrow"><ArrowRight /></el-icon>
              </a>
              <!-- 悬浮弹出子分类 -->
              <div v-if="cat.children && cat.children.length" class="cat-popup">
                <div class="cat-popup-inner">
                  <span class="popup-title">{{ cat.name }}</span>
                  <div class="popup-links">
                    <a v-for="sub in cat.children" :key="sub.id" href="javascript:;" @click="goCategory(sub)">
                      {{ sub.name }}
                      <el-icon><ArrowRight /></el-icon>
                    </a>
                  </div>
                  <div class="popup-hot">
                    <span class="popup-hot-title">热门推荐</span>
                    <div class="popup-hot-list">
                      <router-link
                        v-for="p in products.slice(0, 3)"
                        :key="p.id"
                        :to="{ path: '/goodsDetail', query: { pid: p.id } }"
                        class="popup-hot-item"
                      >
                        {{ p.name }}
                      </router-link>
                    </div>
                  </div>
                </div>
              </div>
            </li>
            <li v-if="!categories.length" class="cat-skeleton">
              <el-skeleton :rows="8" animated />
            </li>
          </ul>
        </aside>

        <!-- 轮播图 -->
        <div class="banner-area">
          <el-carousel v-if="banners.length" height="420px" :interval="4500" arrow="hover">
            <el-carousel-item v-for="b in banners" :key="b.id">
              <img class="banner-img" :src="b.image" :alt="b.title" @click="goBanner(b)" />
              <div class="banner-caption">
                <h3>{{ b.title }}</h3>
                <el-button type="primary" round size="small" @click="goBanner(b)">立即查看</el-button>
              </div>
            </el-carousel-item>
          </el-carousel>
          <div v-else class="banner-placeholder">
            <img :src="placeholder('社区二手街 社区闲置置换节', 900, 420)" alt="banner" />
          </div>

          <div class="banner-side">
            <div class="side-card" @click="router.push('/ai')">
              <el-icon class="side-icon"><MagicStick /></el-icon>
              <div>
                <h4>AI 智能导购</h4>
                <p>说出需求, 帮你挑好物</p>
              </div>
            </div>
            <div class="side-card" @click="router.push('/search')">
              <el-icon class="side-icon"><GoodsFilled /></el-icon>
              <div>
                <h4>全部商品</h4>
                <p>20+ 精选好物在售</p>
              </div>
            </div>
            <div class="side-card" @click="router.push('/user/collect')">
              <el-icon class="side-icon"><StarFilled /></el-icon>
              <div>
                <h4>我的收藏</h4>
                <p>收藏好物随时看</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 猜你喜欢 -->
    <div class="mall-container">
      <section class="like-section">
        <div class="mall-section-title">
          <span class="title-text">猜你喜欢</span>
          <router-link class="more-link" to="/search">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <el-alert
          v-if="error"
          class="mb16"
          :title="error"
          type="error"
          show-icon
          :closable="false"
        >
          <template #default>
            <el-button size="small" type="primary" plain @click="loadData">重新加载</el-button>
          </template>
        </el-alert>

        <div v-if="loading" class="product-grid">
          <div v-for="n in 10" :key="n" class="skeleton-card">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" style="width: 100%; height: 220px" />
                <div style="padding: 14px">
                  <el-skeleton-item variant="h3" style="width: 80%" />
                  <el-skeleton-item variant="text" style="margin-top: 12px" />
                  <el-skeleton-item variant="text" style="width: 40%; margin-top: 12px" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <div v-else-if="products.length" class="product-grid">
          <ProductCard v-for="p in products" :key="p.id" :product="p" />
        </div>

        <div v-else class="mall-card">
          <el-empty description="暂无商品数据">
            <el-button type="primary" @click="loadData">重新加载</el-button>
          </el-empty>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding-top: 18px;
}

.mb16 {
  margin-bottom: 16px;
}

/* ---------- 公告条 ---------- */
.notice-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 44px;
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 0 16px;
  margin-bottom: 16px;
  font-size: 13px;
}

.notice-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #ff6700;
  font-weight: 500;
  flex-shrink: 0;
}

.notice-body {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.notice-text {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 28px;
  height: 28px;
  color: #616161;
  cursor: default;
  overflow: hidden;
  white-space: nowrap;
}

.notice-text.muted {
  color: #9e9e9e;
}

.top-flag {
  font-style: normal;
  background: #ff6700;
  color: #fff;
  border-radius: 3px;
  padding: 0 4px;
  font-size: 11px;
  line-height: 16px;
}

.notice-content {
  color: #b0b0b0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-links {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
  color: #757575;
}

.notice-links a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.notice-links a:hover {
  color: #ff6700;
}

/* ---------- 首屏 ---------- */
.hero {
  display: flex;
  gap: 14px;
  margin-bottom: 26px;
  align-items: stretch;
}

.cat-nav {
  width: 234px;
  flex-shrink: 0;
  background: rgba(105, 101, 101, 0.92);
  border-radius: var(--mall-radius);
  padding: 12px 0;
  position: relative;
  z-index: 20;
}

.cat-item {
  position: relative;
}

.cat-item > a {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 42px;
  padding: 0 18px;
  color: #fff;
  font-size: 14px;
  transition: background 0.2s;
}

.cat-item > a:hover {
  background: #ff6700;
}

.cat-arrow {
  font-size: 12px;
  opacity: 0.7;
}

.cat-skeleton {
  padding: 10px 18px;
}

/* 悬浮子分类面板 */
.cat-popup {
  position: absolute;
  left: 100%;
  top: 0;
  margin-left: 2px;
  width: 520px;
  background: #fff;
  border-radius: 0 var(--mall-radius) var(--mall-radius) 0;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
  padding: 22px 24px;
  display: none;
  z-index: 30;
}

.cat-item:hover .cat-popup {
  display: block;
}

.cat-popup-inner {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.popup-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

.popup-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 16px;
}

.popup-links a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #616161;
  padding: 5px 10px;
  border: 1px solid #eee;
  border-radius: 4px;
  transition: all 0.2s;
}

.popup-links a:hover {
  color: #ff6700;
  border-color: #ff6700;
  background: #fff8f4;
}

.popup-hot {
  border-top: 1px dashed #eee;
  padding-top: 12px;
}

.popup-hot-title {
  display: block;
  font-size: 13px;
  color: #ff6700;
  margin-bottom: 8px;
}

.popup-hot-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.popup-hot-item {
  font-size: 13px;
  color: #757575;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popup-hot-item:hover {
  color: #ff6700;
}

/* 轮播 */
.banner-area {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 14px;
}

.banner-area :deep(.el-carousel) {
  flex: 1;
  min-width: 0;
  border-radius: var(--mall-radius);
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.banner-caption {
  position: absolute;
  left: 36px;
  bottom: 34px;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.banner-caption h3 {
  margin: 0 0 12px;
  font-size: 26px;
  font-weight: 500;
}

.banner-placeholder {
  flex: 1;
  border-radius: var(--mall-radius);
  overflow: hidden;
}

.banner-placeholder img {
  width: 100%;
  height: 420px;
  object-fit: cover;
}

.banner-side {
  width: 230px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card {
  flex: 1;
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.25s;
}

.side-card:hover {
  box-shadow: var(--mall-shadow);
  transform: translateY(-2px);
}

.side-icon {
  font-size: 26px;
  color: #ff6700;
  flex-shrink: 0;
}

.side-card h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: #333;
}

.side-card p {
  margin: 0;
  font-size: 12px;
  color: #b0b0b0;
}

/* ---------- 猜你喜欢 ---------- */
.like-section {
  margin-bottom: 20px;
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
  color: #757575;
}

.more-link:hover {
  color: #ff6700;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.skeleton-card {
  background: #fff;
  border-radius: var(--mall-radius);
  overflow: hidden;
}

@media (max-width: 1240px) {
  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .banner-side {
    display: none;
  }
}

@media (max-width: 900px) {
  .cat-nav {
    display: none;
  }
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
