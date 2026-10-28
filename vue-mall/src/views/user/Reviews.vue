<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyReviews, getPendingReviews, submitReview, uploadImage } from '@/api/index'
import { money, formatTime, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const router = useRouter()

const pendingLoading = ref(false)
const myLoading = ref(false)
const error = ref('')
const pendingList = ref([])
const myList = ref([])
const myTotal = ref(0)
const myPage = ref(1)
const myPageSize = ref(5)
const activeTab = ref('pending')

const dialogVisible = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const currentItem = ref(null)

const form = reactive({
  score: 5,
  content: '',
  images: [],
  anonymous: 0
})

const canSubmit = computed(() => form.score >= 1 && form.score <= 5)

async function loadPending() {
  pendingLoading.value = true
  error.value = ''
  try {
    const data = await getPendingReviews()
    pendingList.value = Array.isArray(data) ? data : []
  } catch (e) {
    pendingList.value = []
    error.value = '待评价列表加载失败'
  } finally {
    pendingLoading.value = false
  }
}

async function loadMyReviews() {
  myLoading.value = true
  try {
    const data = await getMyReviews({ pageNum: myPage.value, pageSize: myPageSize.value })
    myList.value = data?.records || []
    myTotal.value = Number(data?.total) || 0
  } catch (e) {
    myList.value = []
    myTotal.value = 0
  } finally {
    myLoading.value = false
  }
}

function refresh() {
  loadPending()
  loadMyReviews()
}

function openDialog(item) {
  currentItem.value = item
  form.score = 5
  form.content = ''
  form.images = []
  form.anonymous = 0
  dialogVisible.value = true
}

async function handleUpload(uploadFile) {
  const raw = uploadFile?.raw
  if (!raw) return
  if (!/^image\//.test(raw.type)) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (form.images.length >= 6) {
    ElMessage.warning('最多上传 6 张图片')
    return
  }
  uploading.value = true
  try {
    const url = await uploadImage(raw)
    const finalUrl = typeof url === 'string' ? url : url?.url
    if (finalUrl) form.images.push(finalUrl)
  } catch (e) {
    ElMessage.warning('图片上传失败')
  } finally {
    uploading.value = false
  }
}

function removeImage(idx) {
  form.images.splice(idx, 1)
}

async function handleSubmit() {
  if (!currentItem.value) return
  if (!canSubmit.value) {
    ElMessage.warning('请先选择评分')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  submitting.value = true
  try {
    await submitReview({
      orderItemId: currentItem.value.id,
      score: form.score,
      content: form.content.trim(),
      images: form.images,
      anonymous: form.anonymous
    })
    ElMessage.success('评价提交成功')
    dialogVisible.value = false
    activeTab.value = 'mine'
    refresh()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function changeMyPage(page) {
  myPage.value = page
  loadMyReviews()
}

function goGoods(productId) {
  router.push({ path: '/goodsDetail', query: { pid: productId } })
}

onMounted(refresh)
</script>

<template>
  <div class="reviews-page">
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title">我的评价</span>
        <el-button text type="primary" @click="refresh">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>

      <el-alert v-if="error" class="mb16" :title="error" type="error" show-icon :closable="false" />

      <el-tabs v-model="activeTab">
        <el-tab-pane name="pending">
          <template #label>
            <span>待评价 <em class="tab-count">{{ pendingList.length }}</em></span>
          </template>

          <div v-loading="pendingLoading" element-loading-text="正在加载..." class="pending-body">
            <div v-if="pendingList.length" class="pending-list">
              <div v-for="item in pendingList" :key="item.id" class="pending-item">
                <img
                  class="goods-img"
                  :src="item.productImage || DEFAULT_IMAGE"
                  :alt="item.productName"
                  @error="fallbackImage($event, item.productName)"
                />
                <div class="goods-info">
                  <h4 class="goods-name" @click="goGoods(item.productId)">{{ item.productName }}</h4>
                  <p class="goods-meta">
                    ￥{{ money(item.price) }} × {{ item.quantity }} = ￥{{ money(item.totalPrice) }}
                  </p>
                  <p class="goods-meta light">
                    订单号: {{ item.orderNo || item.orderId }}
                    <span v-if="item.orderCreateTime"> · 下单时间: {{ formatTime(item.orderCreateTime) }}</span>
                  </p>
                </div>
                <el-button type="primary" @click="openDialog(item)">
                  <el-icon><EditPen /></el-icon> 去评价
                </el-button>
              </div>
            </div>

            <el-empty v-else-if="!pendingLoading" description="没有待评价的商品">
              <el-button type="primary" @click="router.push('/user/orders')">查看我的订单</el-button>
            </el-empty>
          </div>
        </el-tab-pane>

        <el-tab-pane name="mine">
          <template #label>
            <span>已发表 <em class="tab-count">{{ myTotal }}</em></span>
          </template>

          <div v-loading="myLoading" element-loading-text="正在加载..." class="my-body">
            <div v-if="myList.length" class="my-list">
              <div v-for="rv in myList" :key="rv.id" class="my-item">
                <div class="my-head">
                  <img
                    class="rv-goods"
                    :src="rv.productImage || DEFAULT_IMAGE"
                    :alt="rv.productName"
                    @error="fallbackImage($event, rv.productName)"
                    @click="goGoods(rv.productId)"
                  />
                  <div class="rv-info">
                    <h4 class="rv-goods-name" @click="goGoods(rv.productId)">{{ rv.productName || '商品' }}</h4>
                    <div class="rv-rate">
                      <el-rate :model-value="Number(rv.score) || 0" disabled size="small" />
                      <el-tag v-if="rv.anonymous === 1" size="small" type="info" effect="plain">匿名评价</el-tag>
                      <span class="rv-time">{{ formatTime(rv.createTime) }}</span>
                    </div>
                  </div>
                </div>

                <p class="rv-content">{{ rv.content || '该评价没有文字内容' }}</p>

                <div v-if="rv.imageList && rv.imageList.length" class="rv-images">
                  <el-image
                    v-for="(img, i) in rv.imageList"
                    :key="i"
                    class="rv-image"
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
                <p v-else class="reply-empty">商家暂未回复</p>
              </div>
            </div>

            <el-empty v-else-if="!myLoading" description="还没有发表过评价" />

            <div v-if="myTotal > myPageSize" class="pager">
              <el-pagination
                background
                layout="prev, pager, next, total"
                :total="myTotal"
                :page-size="myPageSize"
                :current-page="myPage"
                @current-change="changeMyPage"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 评价弹窗 -->
    <el-dialog v-model="dialogVisible" title="发表商品评价" width="560px" append-to-body>
      <div v-if="currentItem" class="dialog-goods">
        <img :src="currentItem.productImage || DEFAULT_IMAGE" :alt="currentItem.productName" />
        <div>
          <p class="dialog-goods-name">{{ currentItem.productName }}</p>
          <p class="dialog-goods-meta">￥{{ money(currentItem.price) }} × {{ currentItem.quantity }}</p>
        </div>
      </div>

      <el-form label-width="80px" class="review-form">
        <el-form-item label="商品评分" required>
          <el-rate v-model="form.score" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text />
        </el-form-item>

        <el-form-item label="评价内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说这件商品怎么样吧, 你的评价可以帮助其他小伙伴~"
          />
        </el-form-item>

        <el-form-item label="晒图">
          <div class="upload-wrap">
            <div v-for="(img, idx) in form.images" :key="idx" class="upload-item">
              <img :src="img" alt="评价图" />
              <el-icon class="upload-remove" @click="removeImage(idx)"><CircleCloseFilled /></el-icon>
            </div>
            <el-upload
              v-if="form.images.length < 6"
              class="upload-btn"
              :show-file-list="false"
              :auto-upload="false"
              accept="image/*"
              :on-change="handleUpload"
            >
              <div class="upload-inner" :class="{ loading: uploading }">
                <el-icon><Plus /></el-icon>
                <span>上传</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="匿名评价">
          <el-switch v-model="form.anonymous" :active-value="1" :inactive-value="0" />
          <span class="form-tip">开启后其他用户将看不到你的昵称与头像</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评价</el-button>
      </template>
    </el-dialog>
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
  margin-bottom: 10px;
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

.mb16 {
  margin-bottom: 16px;
}

.tab-count {
  font-style: normal;
  color: #ff6700;
  font-size: 12px;
}

.pending-body,
.my-body {
  min-height: 160px;
}

/* 待评价 */
.pending-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}

.pending-item:last-child {
  border-bottom: none;
}

.goods-img {
  width: 84px;
  height: 84px;
  border-radius: 6px;
  object-fit: cover;
  background: #fafafa;
  border: 1px solid #f2f2f2;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-name {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
}

.goods-name:hover {
  color: #ff6700;
}

.goods-meta {
  margin: 0 0 4px;
  font-size: 13px;
  color: #757575;
}

.goods-meta.light {
  font-size: 12px;
  color: #b0b0b0;
}

/* 我的评价 */
.my-item {
  padding: 18px 0;
  border-bottom: 1px solid #f5f5f5;
}

.my-item:last-child {
  border-bottom: none;
}

.my-head {
  display: flex;
  gap: 14px;
  margin-bottom: 10px;
}

.rv-goods {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  background: #fafafa;
  border: 1px solid #f2f2f2;
  cursor: pointer;
  flex-shrink: 0;
}

.rv-info {
  flex: 1;
  min-width: 0;
}

.rv-goods-name {
  margin: 0 0 8px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rv-goods-name:hover {
  color: #ff6700;
}

.rv-rate {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.rv-time {
  font-size: 12px;
  color: #c8c8c8;
  margin-left: auto;
}

.rv-content {
  margin: 0 0 10px;
  font-size: 14px;
  color: #424242;
  line-height: 1.7;
}

.rv-images {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.rv-image {
  width: 78px;
  height: 78px;
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

.reply-empty {
  margin: 0;
  font-size: 12px;
  color: #c8c8c8;
}

.pager {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

/* 弹窗 */
.dialog-goods {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #fafafa;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 18px;
}

.dialog-goods img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
}

.dialog-goods-name {
  margin: 0 0 4px;
  font-size: 14px;
  color: #333;
}

.dialog-goods-meta {
  margin: 0;
  font-size: 12px;
  color: #9e9e9e;
}

.upload-wrap {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-item {
  position: relative;
  width: 78px;
  height: 78px;
  border-radius: 6px;
  overflow: hidden;
}

.upload-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-remove {
  position: absolute;
  right: 2px;
  top: 2px;
  color: #ff4d4f;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
}

.upload-inner {
  width: 78px;
  height: 78px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  color: #9e9e9e;
  transition: border-color 0.2s;
}

.upload-inner:hover {
  border-color: #ff6700;
  color: #ff6700;
}

.upload-inner.loading {
  opacity: 0.6;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #b0b0b0;
}
</style>
