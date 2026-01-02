import apiClient from '../utils/apiClient';

/**
 * ユーザーサービス
 * 
 * API呼び出し:
 * - deleteAccount: アカウントを削除
 */

/**
 * アカウントを削除
 * 
 * @param userId ユーザーID
 * @param email 確認用メールアドレス
 * @param confirmation 削除確認文字列
 */
export const deleteAccount = async (userId: string, email: string, confirmation: string): Promise<void> => {
  await apiClient.delete(`/api/users/${userId}`, {
    data: {
      email,
      confirmation,
    },
  });
  console.info('Account deleted. userId:', userId);
};
