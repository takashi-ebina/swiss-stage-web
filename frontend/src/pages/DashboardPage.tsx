import React from 'react';
import { AppBar, Toolbar, Typography, Box, Container, CircularProgress, IconButton } from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import { useNavigate } from 'react-router-dom';
import LogoutButton from '../components/auth/LogoutButton';
import { useAuth } from '../hooks/useAuth';

/**
 * ダッシュボードページ
 * 
 * 機能:
 * - ヘッダーにユーザー名表示
 * - ログアウトボタン配置
 * - アカウント設定へのリンク
 * - 認証確認
 */
const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, isLoading, logout } = useAuth();

  if (isLoading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (!user) {
    window.location.href = '/login';
    return null;
  }

  return (
    <Box>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Swiss Stage
          </Typography>
          <Typography variant="body1" sx={{ mr: 2 }}>
            {user.displayName}
          </Typography>
          <IconButton
            color="inherit"
            onClick={() => navigate('/account-settings')}
            sx={{ mr: 1 }}
          >
            <SettingsIcon />
          </IconButton>
          <LogoutButton onLogout={logout} />
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Typography variant="h4" gutterBottom>
          ダッシュボード
        </Typography>
        <Typography variant="body1" color="text.secondary">
          ようこそ、{user.displayName}さん
        </Typography>
      </Container>
    </Box>
  );
};

export default DashboardPage;
