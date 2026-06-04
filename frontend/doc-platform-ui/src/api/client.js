import axios from 'axios'
import { ElMessage } from 'element-plus'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 120000
})

function extractErrorMessage(err) {
  const data = err.response?.data
  if (!data) {
    return err.message || '请求失败'
  }
  if (typeof data === 'string') {
    return data
  }
  if (data.detail) {
    return data.detail
  }
  if (data.message) {
    return data.message
  }
  if (data.error) {
    return data.error
  }
  return err.message || '请求失败'
}

client.interceptors.response.use(
  (res) => res,
  (err) => {
    ElMessage.error(extractErrorMessage(err))
    return Promise.reject(err)
  }
)

export default client
