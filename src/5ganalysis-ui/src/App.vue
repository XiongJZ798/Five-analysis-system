<template>
  <router-view></router-view>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* 添加基础样式 */
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  background-color: #f5f7fa;
}

#app {
  width: 100%;
  min-height: 100vh;
}

/* 添加全局样式 */
.el-card {
  margin-bottom: 20px;
}

.el-card__header {
  padding: 15px 20px;
}

.el-card__body {
  padding: 20px;
}
</style>
