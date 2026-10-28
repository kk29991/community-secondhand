<template>
  <div class="page-container">
    <div class="page-title">分类管理</div>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索分类名称"
          clearable
          style="width: 220px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-button @click="loadList">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>

        <div class="spacer"></div>

        <el-button type="primary" @click="openForm(null, 0)">
          <el-icon><Plus /></el-icon>新增一级分类
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column prop="name" label="分类名称" min-width="240">
          <template #default="{ row }">
            <span class="cat-name">
              <el-icon v-if="row.parentId === 0" class="cat-icon"><FolderOpened /></el-icon>
              <el-icon v-else class="cat-icon sub"><Document /></el-icon>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column label="层级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.parentId === 0 ? 'primary' : 'info'" size="small" effect="plain">
              {{ row.parentId === 0 ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序" width="90" align="center" />

        <el-table-column label="图标" width="120" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ row.icon || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.parentId === 0"
              link
              type="success"
              size="small"
              @click="openForm(null, row.id)"
            >
              添加子分类
            </el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无分类数据" />
        </template>
      </el-table>
    </div>

    <!-- ------------------------------------------------- 新增/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑分类' : '新增分类'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择上级分类" style="width: 100%">
            <el-option label="顶级分类（一级）" :value="0" />
            <el-option
              v-for="c in topCategories"
              :key="c.id"
              :label="c.name"
              :value="c.id"
              :disabled="c.id === form.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：智能穿戴" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="选填，图标名称或图片地址" maxlength="120" />
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
            :step="1"
            step-strictly
            controls-position="right"
          />
          <span class="text-muted ml10">数字越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const flatList = ref([])
const keyword = ref('')

const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = () => ({ id: null, parentId: 0, name: '', icon: '', sort: 0, status: 1 })
const form = reactive(defaultForm())

const rules = {
  parentId: [{ required: true, message: '请选择上级分类', trigger: 'change' }],
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 20, message: '分类名称长度为 2 - 20 个字符', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入排序值'))
        } else if (Number(value) < 0) {
          callback(new Error('排序值不能为负数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 顶级分类, 供上级分类下拉使用 */
const topCategories = computed(() => flatList.value.filter((c) => c.parentId === 0))

/** 由平铺列表构建树, 并按 keyword 过滤 (命中子节点时保留父节点) */
const tableData = computed(() => {
  const list = flatList.value
  const kw = keyword.value.trim().toLowerCase()

  const build = (parentId) =>
    list
      .filter((c) => c.parentId === parentId)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      .map((c) => {
        const children = build(c.id)
        const node = { ...c }
        if (children.length) node.children = children
        return node
      })

  const tree = build(0)

  if (!kw) return tree

  const filterTree = (nodes) =>
    nodes
      .map((n) => {
        const children = n.children ? filterTree(n.children) : []
        const hit = String(n.name || '').toLowerCase().includes(kw)
        if (hit || children.length) {
          const copy = { ...n }
          if (children.length) copy.children = children
          else delete copy.children
          return copy
        }
        return null
      })
      .filter(Boolean)

  return filterTree(tree)
})

async function loadList() {
  loading.value = true
  try {
    // 优先使用树接口, 失败则回退到平铺接口
    let data
    try {
      data = await adminApi.categoryTree()
    } catch (e) {
      data = null
    }
    if (!Array.isArray(data)) {
      data = await adminApi.categories()
    }

    // 平铺化, 便于构建树 / 下拉 / 统计
    const flat = []
    const walk = (nodes) => {
      if (!Array.isArray(nodes)) return
      nodes.forEach((n) => {
        flat.push({
          id: n.id,
          parentId: n.parentId ?? 0,
          name: n.name,
          icon: n.icon,
          sort: n.sort,
          status: n.status
        })
        walk(n.children)
      })
    }
    walk(data)
    flatList.value = flat
  } catch (e) {
    flatList.value = []
  } finally {
    loading.value = false
  }
}

function openForm(row, parentId) {
  Object.assign(form, defaultForm())
  if (row) {
    form.id = row.id
    form.parentId = row.parentId ?? 0
    form.name = row.name
    form.icon = row.icon || ''
    form.sort = row.sort ?? 0
    form.status = row.status ?? 1
  } else if (parentId !== undefined && parentId !== null) {
    form.parentId = parentId
  }
  formVisible.value = true
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
      parentId: form.parentId,
      name: form.name,
      icon: form.icon,
      sort: form.sort,
      status: form.status
    }
    if (form.id) payload.id = form.id

    await adminApi.saveCategory(payload)
    ElMessage.success(form.id ? '分类已更新' : '分类已创建')
    formVisible.value = false
    loadList()
  } catch (e) {
    // 已统一提示
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  const hasChildren = flatList.value.some((c) => c.parentId === row.id)
  try {
    await ElMessageBox.confirm(
      hasChildren
        ? `分类「${row.name}」下还有子分类，删除后可能影响商品分类关联，确定继续吗？`
        : `确定要删除分类「${row.name}」吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch (e) {
    return
  }
  try {
    await adminApi.deleteCategory(row.id)
    ElMessage.success('分类已删除')
    loadList()
  } catch (e) {
    // 已统一提示
  }
}

onMounted(loadList)
</script>

<style scoped>
.cat-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cat-icon {
  color: var(--el-color-primary);
}

.cat-icon.sub {
  color: #909399;
}

.ml10 {
  margin-left: 10px;
  font-size: 12px;
}
</style>
