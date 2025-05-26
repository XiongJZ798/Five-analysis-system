import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',  // 设置baseURL为/api
  timeout: 60000,   // 增加超时时间到60秒
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    console.log('Sending request:', config.method.toUpperCase(), config.url, config.data)
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('Response received:', response.data)
    return response.data
  },
  error => {
    console.error('Response error:', error)
    // 处理错误响应
    if (error.response) {
      // 服务器返回了错误状态码
      const message = error.response.data?.message || '请求失败'
      ElMessage.error(message)
    } else if (error.request) {
      // 请求发出但没有收到响应
      ElMessage.error('服务器无响应，请检查网络连接')
    } else {
      // 请求配置出错
      ElMessage.error('请求配置错误：' + error.message)
    }
    return Promise.reject(error)
  }
)

export default request
