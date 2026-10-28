<template>
  <div class="page-container">
    <div class="page-title">商品管理</div>

    <div class="card">
      <!-- 查询工具条 -->
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

        <div class="spacer"></div>

        <el-button type="primary" @click="openForm()">
          <el-icon><Plus /></el-icon>新增商品
        </el-button>
      </div>

      <!-- 商品表格 -->
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
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
                <div class="cell-product-sub">ID: {{ row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="120" show-overflow-tooltip />

        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">
            <div class="text-price">¥{{ money(row.price) }}</div>
            <div v-if="row.originalPrice" class="text-muted origin-price">
              ¥{{ money(row.originalPrice) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="stock" label="库存" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.stock <= 0" type="danger" size="small">无货</el-tag>
            <el-tag v-else-if="row.stock < 10" type="warning" size="small">{{ row.stock }}</el-tag>
            <span v-else>{{ row.stock }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="sales" label="销量" width="80" align="center" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row.id)">编辑</el-button>
            <el-button link type="warning" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <!-- ------------------------------------------------- 新增/编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="form.id ? '编辑商品' : '新增商品'"
      size="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" v-loading="formLoading">
        <el-form-item label="商品分类" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="categoryOptions"
            :props="cascaderProps"
            placeholder="请选择商品分类"
            clearable
            filterable
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="60" show-word-limit />
        </el-form-item>

        <el-form-item label="商品副标题" prop="subTitle">
          <el-input
            v-model="form.subTitle"
            placeholder="选填，用于商品列表的促销文案"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="商品主图" prop="mainImage">
          <ImageUpload v-model="form.mainImage" :limit="1" tip="建议 400x400，支持 jpg/png，单张不超过 5MB" />
        </el-form-item>

        <el-form-item label="轮播图" prop="images">
          <ImageUpload v-model="form.images" :limit="5" tip="最多 5 张，用于商品详情轮播展示" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="售价" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0.01"
                :max="9999999"
                :precision="2"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :max="9999999"
                :precision="2"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :max="999999"
                :step="1"
                step-strictly
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="件" maxlength="6" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品标签" prop="tagIds">
          <el-select
            v-model="form.tagIds"
            multiple
            placeholder="选择店铺标签（可多选）"
            style="width: 100%"
            :loading="tagLoading"
          >
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id">
              <span :style="{ color: t.color || '#606266' }">● </span>{{ t.name }}
            </el-option>
          </el-select>
          <div v-if="!tags.length" class="form-tip">
            还没有标签，可先到「标签管理」创建
          </div>
        </el-form-item>

        <el-form-item label="商品详情" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="6"
            placeholder="支持 HTML 片段，例如 <p>商品描述</p>"
          />
        </el-form-item>

        <el-form-item label="上架状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
            inline-prompt
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'
import { merchantApi, publicApi } from '@/api'
import { money, parseImageList, parseIdList } from '@/utils/format'

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

/* --------------------------------------------------------------- 分类 */
const categoryTree = ref([])

/** 把后端分类树规整为 cascader 需要的结构 (children 为 null 时移除) */
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

/* --------------------------------------------------------------- 标签 */
const tags = ref([])
const tagLoading = ref(false)

async function loadTags() {
  tagLoading.value = true
  try {
    const data = await merchantApi.tags()
    tags.value = Array.isArray(data) ? data : []
  } catch (e) {
    tags.value = []
  } finally {
    tagLoading.value = false
  }
}

/* --------------------------------------------------------------- 列表 */
async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '' && query.status !== null) params.status = query.status
    if (query.categoryId) params.categoryId = query.categoryId

    const data = await merchantApi.products(params)
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

/* --------------------------------------------------------- 新增 / 编辑 */
const drawerVisible = ref(false)
const submitting = ref(false)
const formLoading = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  id: null,
  categoryId: null,
  name: '',
  subTitle: '',
  mainImage: '',
  images: [],
  detail: '',
  price: 1,
  originalPrice: 0,
  stock: 0,
  unit: '件',
  tagIds: [],
  status: 1
})

const form = reactive(defaultForm())

const rules = {
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 60, message: '商品名称长度为 2 - 60 个字符', trigger: 'blur' }
  ],
  mainImage: [{ required: true, message: '请上传商品主图', trigger: 'change' }],
  price: [
    { required: true, message: '请输入商品售价', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入商品售价'))
        } else if (Number(value) <= 0) {
          callback(new Error('售价必须大于 0'))
        } else if (Number(value) > 9999999) {
          callback(new Error('售价不能超过 9999999'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  originalPrice: [
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === '') return callback()
        if (Number(value) < 0) return callback(new Error('原价不能为负数'))
        // 原价允许等于售价或不填, 仅提示关系
        callback()
      },
      trigger: 'blur'
    }
  ],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入库存数量'))
        } else if (!Number.isInteger(Number(value))) {
          callback(new Error('库存必须为整数'))
        } else if (Number(value) < 0) {
          callback(new Error('库存不能为负数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  unit: [{ required: true, message: '请输入商品单位', trigger: 'blur' }],
  subTitle: [{ max: 100, message: '副标题不能超过 100 个字符', trigger: 'blur' }]
}

function resetForm(data) {
  Object.assign(form, defaultForm(), data || {})
}

async function openForm(id) {
  resetForm()
  if (id) {
    form.id = id
    formLoading.value = true
    drawerVisible.value = true
    try {
      const data = await merchantApi.productDetail(id)
      if (data) {
        resetForm({
          id: data.id,
          categoryId: data.categoryId ?? null,
          name: data.name || '',
          subTitle: data.subTitle || '',
          mainImage: data.mainImage || '',
          images: data.imageList ? [...data.imageList] : parseImageList(data.images),
          detail: data.detail || '',
          price: data.price !== null && data.price !== undefined ? Number(data.price) : 1,
          originalPrice:
            data.originalPrice !== null && data.originalPrice !== undefined
              ? Number(data.originalPrice)
              : 0,
          stock: data.stock ?? 0,
          unit: data.unit || '件',
          tagIds: parseIdList(data.tagIds),
          status: data.status ?? 1
        })
      }
    } catch (e) {
      drawerVisible.value = false
    } finally {
      formLoading.value = false
    }
  } else {
    drawerVisible.value = true
  }
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      categoryId: form.categoryId,
      name: form.name,
      subTitle: form.subTitle,
      mainImage: form.mainImage,
      images: Array.isArray(form.images) ? form.images : [],
      detail: form.detail,
      price: form.price,
      originalPrice: form.originalPrice === null ? 0 : form.originalPrice,
      stock: form.stock,
      unit: form.unit || '件',
      tagIds: Array.isArray(form.tagIds) ? form.tagIds : [],
      status: form.status
    }
    if (form.id) payload.id = form.id

    await merchantApi.saveProduct(payload)
    ElMessage.success(form.id ? '商品已更新' : '商品已创建')
    drawerVisible.value = false
    loadList()
  } catch (e) {
    // 已统一提示
  } finally {
    submitting.value = false
  }
}

/* --------------------------------------------------------- 上下架/删除 */
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
    await merchantApi.updateProductStatus(row.id, next)
    ElMessage.success(next === 1 ? '商品已上架' : '商品已下架')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定要删除商品「${row.name}」吗？此操作不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch (e) {
    return
  }
  try {
    await merchantApi.deleteProduct(row.id)
    ElMessage.success('商品已删除')
    if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(() => {
  loadCategories()
  loadTags()
  loadList()
})
</script>

<style scoped>
.origin-price {
  font-size: 12px;
  text-decoration: line-through;
}

.form-tip {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}
</style>
