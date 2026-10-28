<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProducts } from '@/api/index'
import { useCategoryStore } from '@/stores/category'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()

const loading = ref(false)
const error = ref('')
const list = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  categoryId: null,
  sort: 'default',
  pageNum: 1,
  pageSize: 12
})

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '销量', value: 'sales' },
  { label: '价格升序', value: 'priceAsc' },
  { label: '价格降序', value: 'priceDesc' }
]

const categories = computed(() => categoryStore.categories)
const currentCategory = computed(() => {
  if (!query.categoryId) return null
  const id = Number(query.categoryId)
  for (const top of categories.value) {
    if (top.id === id) return { id: top.id, name: top.name, isTop: true, parentName: '', siblings: top.children || [] }
    const sub = (top.children || []).find((c) => c.id === id)
    if (sub) return { id: sub.id, name: sub.name, isTop: false, parentName: top.name, siblings: top.children || [] }
  }
  return null
})

function syncFromRoute() {
  const q = route.query
  query.keyword = q.keyword ? String(q.keyword) : ''
  query.categoryId = q.categoryId ? Number(q.categoryId) : null
  query.sort = ['default', 'sales', 'priceAsc', 'priceDesc'].includes(String(q.sort)) ? String(q.sort) : 'default'
  query.pageNum = q.pageNum ? Math.max(1, Number(q.pageNum) || 1) : 1
}

function pushRoute(patch = {}) {
  const next = {
    keyword: query.keyword || undefined,
    categoryId: query.categoryId || undefined,
    sort: query.sort !== 'default' ? query.sort : undefined,
    pageNum: query.pageNum > 1 ? query.pageNum : undefined,
    ...patch
  }
  Object.keys(next).forEach((k) => next[k] === undefined && delete next[k])
  router.push({ path: '/search', query: next })
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      sort: query.sort
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.categoryId) {
      params.categoryId = query.categoryId
      params.includeChildren = true
    }
    const data = await getProducts(params)
    list.value = data?.records || []
    total.value = Number(data?.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
    error.value = '商品列表加载失败, 请稍后重试'
  } finally {
    loading.value = false
  }
}

function changeSort(value) {
  query.sort = value
  query.pageNum = 1
  pushRoute({ sort: value !== 'default' ? value : undefined, pageNum: undefined })
}

function changeCategory(id) {
  query.categoryId = query.categoryId === id ? null : id
  query.pageNum = 1
  pushRoute({ categoryId: query.categoryId || undefined, pageNum: undefined })
}

function changePage(page) {
  query.pageNum = page
  pushRoute({ pageNum: page > 1 ? page : undefined })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resetAll() {
  router.push({ path: '/search' })
}

watch(
  () => route.query,
  () => {
    if (route.path !== '/search') return
    syncFromRoute()
    loadList()
  }
)

onMounted(async () => {
  syncFromRoute()
  categoryStore.load()
  loadList()
})
</script>

<template>
  <div class="search-page">
    <div class="mall-container">
      <div class="mall-page-head">
        <div class="head-left">
          <h2>
            <template v-if="query.keyword">“{{ query.keyword }}” 的搜索结果</template>
            <template v-else-if="currentCategory">{{ currentCategory.name }}</template>
            <template v-else>全部商品</template>
          </h2>
          <span class="head-count">共 {{ total }} 件商品</span>
        </div>
        <el-button v-if="query.keyword || query.categoryId" text type="primary" @click="resetAll">
          <el-icon><RefreshLeft /></el-icon>
          清空筛选
        </el-button>
      </div>

      <!-- 筛选 + 排序 -->
      <div class="filter-panel">
        <div class="filter-row">
          <span class="filter-label">分类:</span>
          <div class="filter-options">
            <a
              href="javascript:;"
              class="filter-option"
              :class="{ active: !query.categoryId }"
              @click="query.categoryId && changeCategory(query.categoryId)"
            >
              全部
            </a>
            <a
              v-for="cat in categories"
              :key="cat.id"
              href="javascript:;"
              class="filter-option"
              :class="{ active: Number(query.categoryId) === cat.id }"
              @click="changeCategory(cat.id)"
            >
              {{ cat.name }}
            </a>
          </div>
        </div>

        <div v-if="currentCategory && currentCategory.siblings.length" class="filter-row sub-row">
          <span class="filter-label">细分:</span>
          <div class="filter-options">
            <a
              v-for="sub in currentCategory.siblings"
              :key="sub.id"
              href="javascript:;"
              class="filter-option"
              :class="{ active: Number(query.categoryId) === sub.id }"
              @click="changeCategory(sub.id)"
            >
              {{ sub.name }}
            </a>
          </div>
        </div>

        <div class="filter-row sort-row">
          <span class="filter-label">排序:</span>
          <div class="filter-options">
            <a
              v-for="opt in sortOptions"
              :key="opt.value"
              href="javascript:;"
              class="filter-option"
              :class="{ active: query.sort === opt.value }"
              @click="changeSort(opt.value)"
            >
              {{ opt.label }}
              <el-icon v-if="opt.value === 'priceAsc'"><Top /></el-icon>
              <el-icon v-else-if="opt.value === 'priceDesc'"><Bottom /></el-icon>
            </a>
          </div>
          <span class="filter-tip">小区门口自提 / 物业中心代收 · 无运费</span>
        </div>
      </div>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false">
        <template #default>
          <el-button size="small" type="primary" plain @click="loadList">重新加载</el-button>
        </template>
      </el-alert>

      <!-- 列表 -->
      <div v-loading="loading" element-loading-text="正在加载商品...">
        <div v-if="list.length" class="product-grid">
          <ProductCard v-for="p in list" :key="p.id" :product="p" />
        </div>
        <div v-else-if="!loading" class="mall-empty">
          <el-empty description="没有找到符合条件的商品">
            <el-button type="primary" @click="resetAll">查看全部商品</el-button>
          </el-empty>
        </div>
        <div v-else class="product-grid">
          <div v-for="n in 8" :key="n" class="skeleton-card">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" style="width: 100%; height: 220px" />
                <div style="padding: 14px">
                  <el-skeleton-item variant="h3" style="width: 80%" />
                  <el-skeleton-item variant="text" style="margin-top: 12px" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>
      </div>

      <div v-if="total > query.pageSize" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, jumper, total"
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
.search-page {
  padding-top: 18px;
}

.mb16 {
  margin-bottom: 16px;
}

.head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.head-count {
  font-size: 13px;
  color: #b0b0b0;
}

.filter-panel {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 14px 20px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.filter-row:last-child {
  border-bottom: none;
}

.filter-label {
  flex-shrink: 0;
  font-size: 13px;
  color: #b0b0b0;
  width: 42px;
}

.filter-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.filter-option {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: #616161;
  padding: 4px 12px;
  border-radius: 14px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.filter-option:hover {
  color: #ff6700;
}

.filter-option.active {
  color: #fff;
  background: #ff6700;
}

.sort-row .filter-tip {
  font-size: 12px;
  color: #c8c8c8;
  flex-shrink: 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  min-height: 120px;
}

.skeleton-card {
  background: #fff;
  border-radius: var(--mall-radius);
  overflow: hidden;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 26px 0 10px;
}

@media (max-width: 1240px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
