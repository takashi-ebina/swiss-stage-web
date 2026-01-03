import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import { ToastProvider } from './contexts/ToastContext';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import AccountSettingsPage from './pages/AccountSettingsPage';

/**
 * Appコンポーネント
 * 
 * ルーティング:
 * - /login: ログインページ
 * - /dashboard: ダッシュボードページ（認証必須）
 * - /account-settings: アカウント設定ページ（認証必須）
 * - /: ダッシュボードにリダイレクト
 */
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

const App: React.FC = () => {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ToastProvider>
        <Router>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/account-settings" element={<AccountSettingsPage />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Router>
      </ToastProvider>
    </ThemeProvider>
  );
};

export default App;
