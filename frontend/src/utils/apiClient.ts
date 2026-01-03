import axios from 'axios';

/**
 * Axiosインスタンス
 * 
 * 設定:
 * - baseURL: バックエンドAPIのベースURL
 * - withCredentials: Cookie送信を許可（JWTトークン用）
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// レスポンスインターセプター: エラーハンドリング
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 認証エラーの場合、ログインページにリダイレクト
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
