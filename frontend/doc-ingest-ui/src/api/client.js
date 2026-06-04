import axios from 'axios'
import { ElMessage } from 'element-plus'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 120000
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg =
      err.response?.data?.message ||
      err.response?.data?.error ||
      err.message ||
      '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default client
