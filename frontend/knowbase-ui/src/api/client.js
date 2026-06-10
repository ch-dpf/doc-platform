import axios from 'axios'
import { ElMessage } from 'element-plus'
import { formatApiErrorPayload } from '../utils/ingestErrors'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 120000
})

function extractErrorMessage(err) {
  const friendly = formatApiErrorPayload(err.response?.data)
  if (friendly) {
    return friendly
  }
  if (!err.response?.data) {
    return err.message || '请求失败'
  }
  return err.message || '请求失败'
}

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (!err.config?.skipGlobalErrorToast) {
      ElMessage.error(extractErrorMessage(err))
    }
    return Promise.reject(err)
  }
)

export default client
