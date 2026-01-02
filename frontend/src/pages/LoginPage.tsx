import React, { useEffect, useState } from 'react';
import { Box, Container, Typography, Alert } from '@mui/material';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';

/**
 * ログインページ
 * 
 * 機能:
 * - Googleログインボタン表示
 * - エラーメッセージ表示（URLクエリパラメータから取得）
 */
const LoginPage: React.FC = () => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    // URLクエリパラメータからエラーメッセージを取得
    const params = new URLSearchParams(window.location.search);
    const error = params.get('error');
    
    if (error) {
      switch (error) {
        case 'access_denied':
          setErrorMessage('Google認証がキャンセルされました');
          break;
        case 'network_error':
          setErrorMessage('ネットワークエラーが発生しました');
          break;
        case 'invalid_client':
          setErrorMessage('認証設定にエラーがあります');
          break;
        case 'session_timeout':
          setErrorMessage('セッションの有効期限が切れました。再度ログインしてください');
          break;
        default:
          setErrorMessage('認証に失敗しました');
      }
    }
  }, []);

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Typography component="h1" variant="h4" gutterBottom>
          Swiss Stage
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          スイス式トーナメント管理システム
        </Typography>

        {errorMessage && (
          <Alert severity="error" sx={{ width: '100%', mt: 2 }}>
            {errorMessage}
          </Alert>
        )}

        <Box sx={{ mt: 4, width: '100%' }}>
          <GoogleLoginButton />
        </Box>

        <Typography variant="caption" color="text.secondary" sx={{ mt: 2 }}>
          Googleアカウントでログインしてください
        </Typography>
      </Box>
    </Container>
  );
};

export default LoginPage;
