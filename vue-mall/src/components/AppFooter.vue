<script setup>
const year = new Date().getFullYear()

const columns = [
  { title: '帮助中心', links: ['交易指南', '当面验货', '发布闲置', '常见问题'] },
  { title: '交易保障', links: ['实名认证', '举报与申诉', '纠纷处理', '联系客服'] },
  { title: '关于我们', links: ['平台简介', '加入我们', '隐私政策', '交易协议'] },
  { title: '社区服务', links: ['物业二手置换角', '社区置换节', '爱心捐赠', '失物招领'] }
]
</script>

<template>
  <footer class="app-footer">
    <div class="mall-container">
      <div class="footer-service">
        <div v-for="item in ['小区当面交易', '实名认证卖家', '闲置资源循环', '支持当面验货', '邻里互助']" :key="item" class="service-item">
          <el-icon class="service-icon"><CircleCheck /></el-icon>
          <span>{{ item }}</span>
        </div>
      </div>

      <div class="footer-main">
        <div class="footer-intro">
          <div class="footer-logo">
            <span class="logo-mark">二</span>
            <span class="logo-text">社区二手街</span>
          </div>
          <p class="footer-desc">
            社区二手交易平台 · 用户端前台<br />
            基于 Vue 3 + Vite + Element Plus 构建<br />
            后端 Spring Boot 提供真实数据接口
          </p>
        </div>

        <div class="footer-links">
          <div v-for="col in columns" :key="col.title" class="link-col">
            <h4>{{ col.title }}</h4>
            <ul>
              <li v-for="link in col.links" :key="link">
                <a href="javascript:;">{{ link }}</a>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div class="footer-copy">
        © {{ year }} 社区二手街 · 社区二手交易平台 毕业设计项目 · 仅供学习交流使用
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: #fff;
  border-top: 1px solid #e8e8e8;
  margin-top: 30px;
  color: #616161;
}

.footer-service {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 26px 0;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.service-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #616161;
}

.service-icon {
  color: #ff6700;
  font-size: 18px;
}

.footer-main {
  display: flex;
  gap: 60px;
  padding: 30px 0 20px;
  border-bottom: 1px solid #f0f0f0;
}

.footer-intro {
  width: 300px;
  flex-shrink: 0;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff8f40, #ff6700);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.footer-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: #9e9e9e;
}

.footer-links {
  flex: 1;
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.link-col h4 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.link-col li {
  margin-bottom: 10px;
  font-size: 13px;
  color: #9e9e9e;
}

.link-col a:hover {
  color: #ff6700;
}

.footer-copy {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #b0b0b0;
}

@media (max-width: 900px) {
  .footer-main {
    flex-direction: column;
    gap: 24px;
  }
  .footer-links {
    flex-wrap: wrap;
  }
}
</style>
