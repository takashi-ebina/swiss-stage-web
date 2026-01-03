import { useState, useEffect, useCallback } from 'react';
import { User } from '../types/User';
import * as authService from '../services/authService';

/**
 * 認証フック
 * 
 * 機能:
 * - 認証状態の管理
 * - ユーザー情報の取得
 * - ログイン/ログアウト処理
 */
export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /**
   * ユーザー情報を取得
   */
  const fetchUser = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const userData = await authService.getCurrentUser();
      setUser(userData);
    } catch (err: any) {
      console.error('Failed to fetch user:', err);
      
      // 401エラー（未認証/セッションタイムアウト）の場合、ログイン画面にリダイレクト
      if (err.response?.status === 401) {
        setUser(null);
        if (window.location.pathname !== '/login') {
          window.location.href = '/login?error=session_timeout';
        }
      } else {
        setError('ユーザー情報の取得に失敗しました');
      }
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * ログアウト処理
   */
  const handleLogout = useCallback(async () => {
    try {
      await authService.logout();
      setUser(null);
      window.location.href = '/login';
    } catch (err) {
      console.error('Logout failed:', err);
      setError('ログアウトに失敗しました');
    }
  }, []);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  return {
    user,
    isLoading,
    error,
    fetchUser,
    logout: handleLogout,
  };
};
