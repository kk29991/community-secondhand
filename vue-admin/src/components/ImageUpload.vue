<template>
  <div class="image-upload">
    <div class="image-list">
      <!-- 已上传图片 -->
      <div v-for="(url, index) in list" :key="url + index" class="image-item">
        <el-image
          :src="url"
          fit="cover"
          class="image-thumb"
          :preview-src-list="list"
          :initial-index="index"
          preview-teleported
          hide-on-click-modal
        />
        <div class="image-mask">
          <el-icon @click="remove(index)"><Delete /></el-icon>
        </div>
      </div>

      <!-- 上传按钮 -->
      <el-upload
        v-if="list.length < limit"
        class="image-trigger"
        :show-file-list="false"
        :http-request="doUpload"
        :before-upload="beforeUpload"
        accept="image/*"
        drag
      >
        <div class="trigger-inner">
          <el-icon class="trigger-icon" v-if="!uploading"><Plus /></el-icon>
          <el-icon class="trigger-icon is-loading" v-else><Loading /></el-icon>
          <span class="trigger-text">{{ uploading ? '上传中' : '上传' }}</span>
        </div>
      </el-upload>
    </div>

    <div v-if="tip" class="upload-tip">{{ tip }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadApi } from '@/api'

/**
 * 图片上传 (对接 POST /api/upload/image, form-data 字段名 file)
 * - limit = 1 时 modelValue 为 String
 * - limit > 1 时 modelValue 为 String[]
 */
const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  limit: { type: Number, default: 1 },
  tip: { type: String, default: '' },
  maxSize: { type: Number, default: 5 }
})

const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)

const list = computed(() => {
  const v = props.modelValue
  if (Array.isArray(v)) return v.filter(Boolean)
  return v ? [v] : []
})

function emitValue(arr) {
  if (props.limit === 1) {
    emit('update:modelValue', arr[0] || '')
  } else {
    emit('update:modelValue', arr)
  }
}

function beforeUpload(file) {
  const isImage = file.type && file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const overSize = file.size / 1024 / 1024 > props.maxSize
  if (overSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function doUpload(options) {
  uploading.value = true
  try {
    const data = await uploadApi.image(options.file)
    if (!data || !data.url) throw new Error('上传返回数据异常')
    if (props.limit === 1) {
      emitValue([data.url])
    } else {
      emitValue([...list.value, data.url])
    }
    ElMessage.success('上传成功')
    if (options.onSuccess) options.onSuccess(data)
  } catch (e) {
    if (options.onError) options.onError(e)
  } finally {
    uploading.value = false
  }
}

function remove(index) {
  const arr = [...list.value]
  arr.splice(index, 1)
  emitValue(arr)
}
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--admin-border);
}

.image-thumb {
  width: 100%;
  height: 100%;
  display: block;
}

.image-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
}

.image-item:hover .image-mask {
  opacity: 1;
}

.image-trigger :deep(.el-upload) {
  width: 100px;
  height: 100px;
}

.image-trigger :deep(.el-upload-dragger) {
  width: 100px;
  height: 100px;
  padding: 0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d9d9d9;
  background: #fafafa;
}

.image-trigger :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
}

.trigger-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #909399;
}

.trigger-icon {
  font-size: 20px;
}

.trigger-text {
  font-size: 12px;
}

.is-loading {
  animation: rotating 1.2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.5;
}
</style>
