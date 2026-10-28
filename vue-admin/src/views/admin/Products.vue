<template>
  <div class="page-container">
    <div class="page-title">平台商品管理</div>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="商品名称 / 关键词"
          clearable
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="query.status" placeholder="商品状态" clearable style="width: 130px" @change="search">
          <el-option label="全部状态" :value="''" />
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>

        <el-cascader
          v-model="query.categoryId"
          :options="categoryOptions"
          :props="cascaderProps"
          placeholder="商品分类"
          clearable
          filterable
          style="width: 200px"
          @change="search"
        />

        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">
          <el-icon><RefreshLeft /></el-icon>重置
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="cell-product">
              <el-image
                :src="row.mainImage"
                fit="cover"
                class="cell-thumb"
                :preview-src-list="[row.mainImage, ...parseImageList(row.images)]"
                preview-teleported
                hide-on-click-modal
              />
              <div class="cell-product-info">
                <div class="cell-product-name">{{ row.name }}</div>
                <div class="cell-product-sub">{{ row.subTitle || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="店铺" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.merchantName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>

        <el-table-column label="价格" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">¥{{ money(row.price) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stock" label="库存" width="80" align="center" />
        <el-table-column prop="sales" label="销量" width="80" align="center" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无商品数据" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, publicApi } from '@/api'
import { money, parseImageList } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  categoryId: null
})

const categoryTree = ref([])

function normalizeTree(nodes) {
  if (!Array.isArray(nodes)) return []
  return nodes.map((n) => {
    const node = { id: n.id, name: n.name }
    const children = normalizeTree(n.children)
    if (children.length) node.children = children
    return node
  })
}

const categoryOptions = computed(() => categoryTree.value)

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: true
}

async function loadCategories() {
  try {
    const data = await publicApi.categories()
    categoryTree.value = normalizeTree(data)
  } catch (e) {
    categoryTree.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '' && query.status !== null) params.status = query.status
    if (query.categoryId) params.categoryId = query.categoryId

    const data = await adminApi.products(params)
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadList()
}

function reset() {
  query.keyword = ''
  query.status = ''
  query.categoryId = null
  query.pageNum = 1
  loadList()
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确定要${next === 1 ? '上架' : '下架'}商品「${row.name}」吗？`,
      '操作确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await adminApi.updateProductStatus(row.id, next)
    ElMessage.success(next === 1 ? '商品已上架' : '商品已下架')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(() => {
  loadCategories()
  loadList()
})
</script>
