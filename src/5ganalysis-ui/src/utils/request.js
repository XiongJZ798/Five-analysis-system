import axios from 'axios'
import { ElMessage } from 'element-plus'

// 请求配置 封装axios请求工具，处理与后端通信
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    console.log('Sending request:', config.method.toUpperCase(), config.url)
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    console.log('Received response:', response.status, response.config.url)
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('Response error:', error)
    const message = error.response?.data?.message || '网络错误，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request 