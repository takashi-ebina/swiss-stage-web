import React from 'react';
import { Button } from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';

/**
 * ログアウトボタンコンポーネント
 * 
 * 機能:
 * - ログアウトAPIを呼び出し
 * - Cookie削除
 * - ログイン画面にリダイレクト
 */
interface LogoutButtonProps {
  onLogout: () => void;
}

const LogoutButton: React.FC<LogoutButtonProps> = ({ onLogout }) => {
  return (
    <Button
      variant="outlined"
      color="inherit"
      startIcon={<LogoutIcon />}
      onClick={onLogout}
      sx={{
        textTransform: 'none',
      }}
    >
      ログアウト
    </Button>
  );
};

export default LogoutButton;
