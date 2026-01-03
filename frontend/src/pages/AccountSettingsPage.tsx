import React, { useState } from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  Paper,
  AppBar,
  Toolbar,
  CircularProgress,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import DeleteAccountDialog from '../components/account/DeleteAccountDialog';
import * as userService from '../services/userService';

/**
 * アカウント設定ページ
 * 
 * 機能:
 * - アカウント削除ボタン
 * - DeleteAccountDialog表示
 * - 削除成功後、ログイン画面にリダイレクト
 */
const AccountSettingsPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, isLoading } = useAuth();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const handleDeleteAccount = async (email: string, confirmation: string) => {
    if (!user) return;

    await userService.deleteAccount(user.userId, email, confirmation);
    
    // 削除成功後、ログイン画面にリダイレクト
    window.location.href = '/login?message=account_deleted';
  };

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
          <Button
            color="inherit"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/dashboard')}
            sx={{ mr: 2 }}
          >
            戻る
          </Button>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            アカウント設定
          </Typography>
        </Toolbar>
      </AppBar>

      <Container maxWidth="md" sx={{ mt: 4 }}>
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h5" gutterBottom>
            アカウント情報
          </Typography>
          <Box sx={{ mt: 2 }}>
            <Typography variant="body1">
              <strong>ユーザーID:</strong> {user.userId}
            </Typography>
            <Typography variant="body1" sx={{ mt: 1 }}>
              <strong>表示名:</strong> {user.displayName}
            </Typography>
            <Typography variant="body1" sx={{ mt: 1 }}>
              <strong>アカウント作成日:</strong>{' '}
              {new Date(user.createdAt).toLocaleDateString('ja-JP')}
            </Typography>
          </Box>
        </Paper>

        <Paper sx={{ p: 3, bgcolor: '#ffebee' }}>
          <Typography variant="h6" color="error" gutterBottom>
            危険な操作
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            アカウントを削除すると、全ての関連データが完全に削除され、復元できません。
          </Typography>
          <Button
            variant="contained"
            color="error"
            startIcon={<DeleteIcon />}
            onClick={() => setDeleteDialogOpen(true)}
            sx={{ mt: 2 }}
          >
            アカウントを削除
          </Button>
        </Paper>
      </Container>

      <DeleteAccountDialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={handleDeleteAccount}
        userEmail={user.displayName}
      />
    </Box>
  );
};

export default AccountSettingsPage;
