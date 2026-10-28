<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="notfound">
    <div class="mall-container">
      <div class="nf-box">
        <div class="nf-code">404</div>
        <h1>抱歉, 页面走丢了</h1>
        <p>你访问的页面不存在或已被移除, 请检查地址是否正确。</p>
        <div class="nf-actions">
          <el-button type="primary" size="large" @click="router.push('/')">
            <el-icon><HomeFilled /></el-icon> 返回首页
          </el-button>
          <el-button size="large" @click="router.push('/search')">
            <el-icon><GoodsFilled /></el-icon> 去逛商品
          </el-button>
          <el-button size="large" text @click="router.push('/ai')">
            <el-icon><MagicStick /></el-icon> AI 导购
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notfound {
  padding: 60px 0;
}

.nf-box {
  background: #fff;
  border-radius: var(--mall-radius);
  padding: 70px 24px;
  text-align: center;
}

.nf-code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, #ff8f40, #ff6700);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 16px;
}

.nf-box h1 {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 500;
  color: #333;
}

.nf-box p {
  margin: 0 0 30px;
  font-size: 14px;
  color: #9e9e9e;
}

.nf-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
