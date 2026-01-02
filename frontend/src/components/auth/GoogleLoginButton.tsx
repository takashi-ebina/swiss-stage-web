import React from 'react';
import { Button } from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';

/**
 * Googleログインボタンコンポーネント
 * 
 * OAuth2認証フロー:
 * 1. ボタンクリック
 * 2. /oauth2/authorization/googleにリダイレクト
 * 3. Google認証画面
 * 4. 認証成功後、バックエンドのコールバックURL
 * 5. JWTトークン生成、Cookie設定
 * 6. ダッシュボードにリダイレクト
 */
const GoogleLoginButton: React.FC = () => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const googleLoginUrl = `${apiBaseUrl}/oauth2/authorization/google`;

  return (
    <Button
      variant="contained"
      color="primary"
      size="large"
      startIcon={<GoogleIcon />}
      href={googleLoginUrl}
      fullWidth
      sx={{
        textTransform: 'none',
        fontSize: '1rem',
        padding: '12px',
      }}
    >
      Googleでログイン
    </Button>
  );
};

export default GoogleLoginButton;
