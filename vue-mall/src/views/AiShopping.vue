<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addToCart, aiChat, aiChatStream, clearAiSession, getAiHistory, getAiStatus, getProductDetail } from '@/api/index'
import { useUserStore } from '@/stores/user'
import { money, formatTime, DEFAULT_IMAGE, fallbackImage } from '@/utils/placeholder'

const router = useRouter()
const userStore = useUserStore()

const SESSION_KEY = 'mall_ai_session_id'

const sessionId = ref('')
const messages = ref([])
const input = ref('')
const sending = ref(false)
const loading = ref(false)
const chatBodyRef = ref(null)

/** 大模型是否已启用(未启用则本地规则引擎兜底) 与模型名 */
const llmEnabled = ref(false)
const modelName = ref('')

const quickQuestions = [
  '推荐一款 2000 元以内的手机',
  '有没有适合秋冬的毛衣?',
  '帮我找智能手表',
  '想买护肤品, 有什么推荐?',
  '有什么销量高的商品?'
]

async function scrollToBottom() {
  await nextTick()
  const el = chatBodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function pushMessage(role, content, extra = {}) {
  messages.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    time: new Date().toISOString(),
    ...extra
  })
}

async function loadHistory() {
  if (!sessionId.value) return
  loading.value = true
  try {
    const data = await getAiHistory(sessionId.value)
    const history = Array.isArray(data) ? data : []
    if (history.length) {
      messages.value = []
      for (const item of history) {
        // 后端返回 AiChat 实体: { role: 'user'|'assistant', content, intent, refProductIds }
        if (item.role === 'user') {
          pushMessage('user', item.content, { time: item.createTime })
        } else if (item.role === 'assistant') {
          pushMessage('ai', item.content, {
            time: item.createTime,
            intent: item.intent,
            refProductIds: item.refProductIds,
            products: []
          })
        }
      }
      // 历史消息中的推荐商品 ID 批量补齐商品信息
      await fillHistoryProducts()
    }
  } catch (e) {
    // 历史加载失败不阻塞聊天
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/** 历史记录只存了推荐商品 ID, 这里批量拉取商品详情补齐卡片 */
async function fillHistoryProducts() {
  const ids = new Set()
  for (const msg of messages.value) {
    if (msg.role !== 'ai' || !msg.refProductIds) continue
    String(msg.refProductIds)
      .split(',')
      .map((s) => Number(s.trim()))
      .filter((n) => Number.isFinite(n) && n > 0)
      .forEach((n) => ids.add(n))
  }
  if (!ids.size) return
  const results = await Promise.allSettled([...ids].map((id) => getProductDetail(id)))
  const map = new Map()
  results.forEach((r) => {
    if (r.status === 'fulfilled' && r.value?.id) map.set(r.value.id, r.value)
  })
  for (const msg of messages.value) {
    if (msg.role !== 'ai' || !msg.refProductIds) continue
    msg.products = String(msg.refProductIds)
      .split(',')
      .map((s) => map.get(Number(s.trim())))
      .filter(Boolean)
  }
}

async function send(text) {
  const content = (text ?? input.value).trim()
  if (!content) {
    ElMessage.warning('请输入你想咨询的内容')
    return
  }
  if (sending.value) return

  pushMessage('user', content)
  input.value = ''
  sending.value = true
  scrollToBottom()

  // 先占位插入 AI 气泡, 流式往里追加文本
  const aiMsg = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role: 'ai',
    content: '',
    time: new Date().toISOString(),
    products: [],
    streaming: true,
    engine: ''
  }
  messages.value.push(aiMsg)

  let gotAny = false
  try {
    await aiChatStream(content, sessionId.value || null, {
      onDelta: (delta) => {
        gotAny = true
        aiMsg.content += delta
        aiMsg.streaming = true
        scrollToBottom()
      },
      onMeta: (meta) => {
        if (meta?.sessionId && meta.sessionId !== sessionId.value) {
          sessionId.value = meta.sessionId
          localStorage.setItem(SESSION_KEY, meta.sessionId)
        }
        aiMsg.intent = meta?.intent
        aiMsg.engine = meta?.engine
        aiMsg.products = Array.isArray(meta?.products) ? meta.products : []
      }
    })
    if (!aiMsg.content) {
      aiMsg.content = '抱歉, 我暂时没有找到合适的商品。'
    }
  } catch (e) {
    // 流式失败再尝试一次性接口, 尽量不让用户看到失败
    try {
      const data = await aiChat(content, sessionId.value || null)
      if (data?.sessionId && data.sessionId !== sessionId.value) {
        sessionId.value = data.sessionId
        localStorage.setItem(SESSION_KEY, data.sessionId)
      }
      aiMsg.content = data?.reply || '抱歉, 我暂时没有找到合适的商品。'
      aiMsg.intent = data?.intent
      aiMsg.products = Array.isArray(data?.products) ? data.products : []
      aiMsg.engine = 'rule'
    } catch (e2) {
      if (!gotAny) {
        aiMsg.content = '抱歉, AI 助手暂时无法响应, 请稍后再试。'
        aiMsg.failed = true
      }
    }
  } finally {
    aiMsg.streaming = false
    sending.value = false
    scrollToBottom()
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空当前会话吗? 清空后聊天记录不可恢复。', '清空会话', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    if (sessionId.value) await clearAiSession(sessionId.value)
    sessionId.value = ''
    localStorage.removeItem(SESSION_KEY)
    messages.value = []
    ElMessage.success('会话已清空')
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleAddCart(product) {
  try {
    await addToCart(product.id, 1)
    ElMessage.success(`已将「${product.name}」加入购物车`)
    await userStore.refreshCartCount()
  } catch (e) {
    // 拦截器已提示
  }
}

function goDetail(product) {
  router.push({ path: '/goodsDetail', query: { pid: product.id } })
}

onMounted(() => {
  sessionId.value = localStorage.getItem(SESSION_KEY) || ''
  userStore.refreshCartCount()
  // 查询大模型是否已启用, 用于顶部提示与"AI 正在思考"文案
  getAiStatus()
    .then((data) => {
      llmEnabled.value = !!data?.llmEnabled
      modelName.value = data?.model || ''
    })
    .catch(() => {
      llmEnabled.value = false
    })
  if (sessionId.value) {
    loadHistory().then(() => {
      if (!messages.value.length) {
        pushMessage('ai', '你好, 我是社区二手交易平台的 AI 导购助手。告诉我你的预算和需求, 我来帮你从在售闲置里挑选合适的物品~')
      }
    })
  } else {
    pushMessage('ai', '你好, 我是社区二手交易平台的 AI 导购助手 👋\n可以试着这样问我: “推荐一款 1000 元以内的二手自行车”。')
  }
})
</script>

<template>
  <div class="ai-page">
    <div class="mall-container">
      <div class="mall-page-head">
        <h2><el-icon><MagicStick /></el-icon> AI 智能导购</h2>
        <div class="head-actions">
          <el-tag :type="llmEnabled ? 'success' : 'info'" size="small" effect="plain">
            <el-icon><Connection /></el-icon>
            {{ llmEnabled ? `已接入 ${modelName || '大模型'}` : '本地规则引擎' }}
          </el-tag>
          <span v-if="sessionId" class="session-tip">会话 ID: {{ sessionId.slice(0, 8) }}...</span>
          <el-button text type="danger" @click="handleClear">
            <el-icon><Delete /></el-icon> 清空会话
          </el-button>
        </div>
      </div>

      <div class="chat-wrap">
        <div ref="chatBodyRef" v-loading="loading" element-loading-text="正在加载历史会话..." class="chat-body">
          <div v-for="msg in messages" :key="msg.id" class="chat-row" :class="msg.role">
            <div class="avatar" :class="msg.role">
              <el-icon v-if="msg.role === 'ai'"><MagicStick /></el-icon>
              <img
                v-else
                :src="userStore.avatar || DEFAULT_IMAGE"
                alt="我"
                @error="fallbackImage($event, userStore.nickname)"
              />
            </div>

            <div class="bubble-wrap">
              <div class="bubble" :class="{ failed: msg.failed }">
                <p class="bubble-text">
                  <template v-if="msg.content">{{ msg.content }}</template>
                  <span v-else-if="msg.streaming" class="pending-text">
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                    <span class="typing-text">AI 正在思考中...</span>
                  </span>
                  <span v-if="msg.streaming && msg.content" class="cursor"></span>
                </p>

                <div v-if="msg.intent" class="intent-tag">
                  <el-tag size="small" effect="plain" type="warning">识别意图: {{ msg.intent }}</el-tag>
                  <el-tag
                    v-if="msg.engine"
                    size="small"
                    effect="plain"
                    :type="msg.engine === 'llm' ? 'success' : 'info'"
                    class="engine-tag"
                  >
                    {{ msg.engine === 'llm' ? (modelName || '大模型') + ' 生成' : '本地规则引擎' }}
                  </el-tag>
                </div>

                <div v-if="msg.products && msg.products.length" class="product-list">
                  <div v-for="p in msg.products" :key="p.id" class="product-item">
                    <img :src="p.mainImage || DEFAULT_IMAGE" :alt="p.name" @error="fallbackImage($event, p.name)" @click="goDetail(p)" />
                    <div class="product-info">
                      <h4 class="product-name" @click="goDetail(p)">{{ p.name }}</h4>
                      <p class="product-sub">{{ p.subTitle }}</p>
                      <p class="product-merchant">
                        <el-icon><Shop /></el-icon>
                        {{ p.merchantName || '个人卖家' }}
                      </p>
                      <div class="product-price-row">
                        <span class="mall-price">
                          <span class="symbol">￥</span><span class="num">{{ money(p.price) }}</span>
                        </span>
                        <span class="product-stock">库存 {{ p.stock }}</span>
                        <div class="product-actions">
                          <el-button size="small" @click="goDetail(p)">查看详情</el-button>
                          <el-button size="small" type="primary" :disabled="p.stock === 0" @click="handleAddCart(p)">
                            <el-icon><ShoppingCart /></el-icon> 加购
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <span class="time">{{ formatTime(msg.time).slice(11, 16) }}</span>
            </div>
          </div>

          <div v-if="sending && messages[messages.length - 1]?.role !== 'ai'" class="chat-row ai">
            <div class="avatar ai"><el-icon><MagicStick /></el-icon></div>
            <div class="bubble-wrap">
              <div class="bubble typing">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                <span class="typing-text">AI 正在思考中...</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <div class="quick-row">
            <span class="quick-label">试试这样问:</span>
            <el-tag
              v-for="q in quickQuestions"
              :key="q"
              class="quick-tag"
              effect="plain"
              @click="send(q)"
            >
              {{ q }}
            </el-tag>
          </div>

          <div class="input-row">
            <el-input
              v-model="input"
              type="textarea"
              :rows="2"
              resize="none"
              maxlength="200"
              show-word-limit
              placeholder="请输入你的需求, 例如: 推荐一款 2000 元以内的手机 (Enter 发送, Shift+Enter 换行)"
              @keydown.enter.exact.prevent="send()"
            />
            <el-button type="primary" class="send-btn" :loading="sending" @click="send()">
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  padding-top: 18px;
}

.mall-page-head h2 {
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.session-tip {
  font-size: 12px;
  color: #c8c8c8;
}

.chat-wrap {
  background: #fff;
  border-radius: var(--mall-radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 280px);
  min-height: 480px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  background: linear-gradient(180deg, #fffaf6 0%, #ffffff 240px);
}

.chat-row {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
}

.chat-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fff2e6;
  color: #ff6700;
  font-size: 20px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bubble-wrap {
  max-width: 76%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-row.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px 18px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

.chat-row.user .bubble {
  background: linear-gradient(135deg, #ff8f40, #ff6700);
  color: #fff;
  border-color: transparent;
}

.bubble.failed {
  border-color: #ffccc7;
  background: #fff1f0;
}

.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.intent-tag {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 流式输出时的光标 */
.cursor {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 2px;
  background: #ff6700;
  vertical-align: text-bottom;
  animation: blink-cursor 1s steps(2, start) infinite;
}

@keyframes blink-cursor {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.pending-text {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.time {
  font-size: 11px;
  color: #c8c8c8;
}

/* 推荐商品卡片 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #f0f0f0;
}

.product-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.2s;
}

.product-item:hover {
  background: #fff8f4;
}

.product-item img {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  cursor: pointer;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-name:hover {
  color: #ff6700;
}

.product-sub {
  margin: 0 0 4px;
  font-size: 12px;
  color: #9e9e9e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-merchant {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0 0 6px;
  font-size: 12px;
  color: #b0b0b0;
}

.product-price-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.product-price-row .mall-price .num {
  font-size: 18px;
}

.product-stock {
  font-size: 12px;
  color: #b0b0b0;
}

.product-actions {
  display: flex;
  gap: 6px;
  margin-left: auto;
}

/* 输入区 */
.chat-footer {
  border-top: 1px solid #f0f0f0;
  padding: 14px 20px 18px;
  background: #fff;
}

.quick-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.quick-label {
  font-size: 12px;
  color: #b0b0b0;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag:hover {
  color: #ff6700;
  border-color: #ff6700;
  background: #fff8f4;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.send-btn {
  height: 56px;
  min-width: 110px;
  border-radius: 10px;
  font-size: 15px;
}

/* 打字动画 */
.bubble.typing {
  display: flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ff6700;
  animation: blink 1.2s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

.typing-text {
  margin-left: 8px;
  font-size: 12px;
  color: #b0b0b0;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

@media (max-width: 900px) {
  .bubble-wrap {
    max-width: 88%;
  }
  .product-actions {
    margin-left: 0;
  }
}
</style>
