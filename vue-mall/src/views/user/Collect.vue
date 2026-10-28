<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addToCart, getCollectList, removeCollect, toggleCollect } from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, formatTime, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const list = ref([])

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const data = await getCollectList()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
    error.value = '收藏列表加载失败, 请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(`确定要取消收藏「${item.product?.name || '该商品'}」吗?`, '取消收藏', {
      confirmButtonText: '取消收藏',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await removeCollect(item.productId)
    list.value = list.value.filter((c) => c.id !== item.id)
    ElMessage.success('已取消收藏')
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleAddCart(item) {
  if (!item.product) return
  try {
    await addToCart(item.productId, 1)
    ElMessage.success('已加入购物车')
    await userStore.refreshCartCount()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleToggle(item) {
  try {
    const collected = await toggleCollect(item.productId)
    if (collected === false) {
      list.value = list.value.filter((c) => c.id !== item.id)
      ElMessage.success('已取消收藏')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

function goDetail(item) {
  router.push({ path: '/goodsDetail', query: { pid: item.productId } })
}

onMounted(loadList)
</script>

<template>
  <div class="collect-page">
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">我的收藏</span>
        <span class="panel-tip">共 {{ list.length }} 件商品</span>
      </div>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="loadList">重新加载</el-button>
        </template>
      </el-alert>

      <div v-loading="loading" element-loading-text="正在加载收藏..." class="collect-body">
        <div v-if="list.length" class="collect-grid">
          <div v-for="item in list" :key="item.id" class="collect-card">
            <div class="thumb" @click="goDetail(item)">
              <img
                :src="item.product?.mainImage || DEFAULT_IMAGE"
                :alt="item.product?.name || '商品'"
                @error="fallbackImage($event, item.product?.name || '商品')"
              />
              <span v-if="item.product?.status !== 1" class="offline-mask">已下架</span>
            </div>
            <div class="info">
              <h4 class="name" :title="item.product?.name" @click="goDetail(item)">
                {{ item.product?.name || '商品已删除' }}
              </h4>
              <p class="time">收藏于 {{ formatTime(item.createTime) }}</p>
              <div class="price-row">
                <span class="mall-price">
                  <span class="symbol">￥</span><span class="num">{{ money(item.product?.price) }}</span>
                </span>
                <span v-if="item.product?.merchantName" class="merchant">{{ item.product.merchantName }}</span>
              </div>
              <div class="actions">
                <el-button size="small" type="primary" :disabled="item.product?.status !== 1" @click="handleAddCart(item)">
                  <el-icon><ShoppingCart /></el-icon> 加入购物车
                </el-button>
                <el-button size="small" @click="handleRemove(item)">取消收藏</el-button>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else-if="!loading" description="还没有收藏任何商品">
          <el-button type="primary" @click="router.push('/search')">去逛逛</el-button>
        </el-empty>
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
  margin-bottom: 18px;
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

.panel-tip {
  font-size: 13px;
  color: #b0b0b0;
}

.mb16 {
  margin-bottom: 16px;
}

.collect-body {
  min-height: 200px;
}

.collect-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.collect-card {
  border: 1px solid #f0f0f0;
  border-radius: var(--mall-radius);
  overflow: hidden;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
}

.collect-card:hover {
  box-shadow: var(--mall-shadow);
  border-color: #ffe0cc;
}

.thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #fafafa;
  cursor: pointer;
  overflow: hidden;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.collect-card:hover .thumb img {
  transform: scale(1.04);
}

.offline-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 2px;
}

.info {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.5;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.name:hover {
  color: #ff6700;
}

.time {
  margin: 0;
  font-size: 12px;
  color: #c8c8c8;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.merchant {
  font-size: 12px;
  color: #b0b0b0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 8px;
}

.actions .el-button {
  flex: 1;
}

@media (max-width: 1100px) {
  .collect-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
