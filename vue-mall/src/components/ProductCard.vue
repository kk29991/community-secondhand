<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addToCart } from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const props = defineProps({
  product: { type: Object, required: true },
  /** 展示模式: grid 网格卡片 / list 横向列表 */
  mode: { type: String, default: 'grid' },
  /** 是否显示加入购物车按钮 */
  showCart: { type: Boolean, default: true }
})

const router = useRouter()
const userStore = useUserStore()

const p = computed(() => props.product || {})
const isGrid = computed(() => props.mode === 'grid')

const off = computed(() => {
  const price = Number(p.value.price)
  const origin = Number(p.value.originalPrice)
  if (!Number.isFinite(price) || !Number.isFinite(origin) || origin <= price) return 0
  return Math.round(((origin - price) / origin) * 100)
})

function toDetail() {
  router.push({ path: '/goodsDetail', query: { pid: p.value.id } })
}

async function handleAddCart() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  try {
    await addToCart(p.value.id, 1)
    ElMessage.success('已加入购物车')
    await userStore.refreshCartCount()
  } catch (e) {
    // 错误提示已由请求拦截器统一处理
  }
}
</script>

<template>
  <div class="product-card" :class="isGrid ? 'is-grid' : 'is-list'" @click="toDetail">
    <div class="thumb">
      <img :src="p.mainImage || DEFAULT_IMAGE" :alt="p.name" loading="lazy" @error="fallbackImage($event, p.name)" />
      <span v-if="off > 0" class="off-tag">省 {{ off }}%</span>
      <span v-if="p.stock === 0" class="soldout-mask">已售罄</span>
    </div>

    <div class="info">
      <h3 class="name" :title="p.name">{{ p.name }}</h3>
      <p v-if="p.subTitle" class="subtitle" :title="p.subTitle">{{ p.subTitle }}</p>

      <div v-if="p.tags && p.tags.length" class="tags">
        <span
          v-for="tag in p.tags.slice(0, 3)"
          :key="tag.id"
          class="tag"
          :style="{ color: tag.color || '#ff6700', borderColor: tag.color || '#ff6700' }"
        >
          {{ tag.name }}
        </span>
      </div>

      <div class="meta">
        <span v-if="p.merchantName" class="merchant">{{ p.merchantName }}</span>
        <span v-if="p.sales != null" class="sales">已售 {{ p.sales }} {{ p.unit || '件' }}</span>
      </div>

      <div class="price-row">
        <span class="mall-price">
          <span class="symbol">￥</span><span class="num">{{ money(p.price) }}</span>
        </span>
        <span v-if="Number(p.originalPrice) > Number(p.price)" class="mall-price-old">￥{{ money(p.originalPrice) }}</span>

        <el-button
          v-if="showCart"
          class="cart-btn"
          type="primary"
          size="small"
          :disabled="p.stock === 0"
          @click.stop="handleAddCart"
        >
          <el-icon><ShoppingCart /></el-icon>
          <span>加购</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  background: #fff;
  border-radius: var(--mall-radius);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  border: 1px solid transparent;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--mall-shadow-hover);
  border-color: #ffe0cc;
}

/* 网格模式 */
.is-grid .thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #fafafa;
  overflow: hidden;
}

.is-grid .thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s;
}

.is-grid:hover .thumb img {
  transform: scale(1.05);
}

.is-grid .info {
  padding: 14px 16px 18px;
}

/* 列表模式 */
.is-list {
  display: flex;
  gap: 18px;
  padding: 16px;
}

.is-list .thumb {
  position: relative;
  width: 180px;
  height: 180px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #fafafa;
}

.is-list .thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.is-list .info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.off-tag {
  position: absolute;
  left: 0;
  top: 12px;
  background: #ff6700;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 0 10px 10px 0;
}

.soldout-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  letter-spacing: 2px;
}

.name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.is-list .name {
  font-size: 18px;
  -webkit-line-clamp: 1;
}

.subtitle {
  margin: 0 0 8px;
  font-size: 12px;
  color: #b0b0b0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-list .subtitle {
  font-size: 13px;
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag {
  font-size: 11px;
  border: 1px solid;
  border-radius: 3px;
  padding: 0 5px;
  line-height: 17px;
  opacity: 0.9;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #b0b0b0;
  margin-bottom: 8px;
  overflow: hidden;
}

.merchant {
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: auto;
}

.cart-btn {
  margin-left: auto;
}

@media (max-width: 700px) {
  .is-list .thumb {
    width: 110px;
    height: 110px;
  }
}
</style>
