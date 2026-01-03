import apiClient from '../utils/apiClient';
import { User } from '../types/User';

/**
 * 認証サービス
 * 
 * API呼び出し:
 * - getCurrentUser: 現在のユーザー情報を取得
 * - logout: ログアウト処理
 */

/**
 * 現在のユーザー情報を取得
 * 
 * @returns ユーザー情報
 */
export const getCurrentUser = async (): Promise<User> => {
  const response = await apiClient.get<User>('/api/auth/me');
  console.info('Current user retrieved. userId:', response.data.userId);
  return response.data;
};

/**
 * ログアウト処理
 */
export const logout = async (): Promise<void> => {
  await apiClient.post('/api/auth/logout');
  console.info('User logged out');
};
