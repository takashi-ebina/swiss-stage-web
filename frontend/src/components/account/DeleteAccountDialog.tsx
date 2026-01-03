import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Alert,
  Box,
} from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';

/**
 * アカウント削除ダイアログコンポーネント
 * 
 * 機能:
 * - 2段階確認
 * - メールアドレス再入力
 * - 削除確認文字列（"DELETE"）入力
 * - 削除実行
 */
interface DeleteAccountDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (email: string, confirmation: string) => Promise<void>;
  userEmail?: string;
}

const DeleteAccountDialog: React.FC<DeleteAccountDialogProps> = ({
  open,
  onClose,
  onConfirm,
  userEmail = '',
}) => {
  const [email, setEmail] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleConfirm = async () => {
    // メールアドレス確認
    if (email !== userEmail) {
      setError('メールアドレスが一致しません');
      return;
    }

    // 削除確認文字列チェック
    if (confirmation !== 'DELETE') {
      setError('確認文字列が正しくありません。"DELETE"と入力してください');
      return;
    }

    try {
      setIsDeleting(true);
      setError(null);
      await onConfirm(email, confirmation);
      onClose();
    } catch (err) {
      console.error('Failed to delete account:', err);
      setError('アカウント削除に失敗しました');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleClose = () => {
    setEmail('');
    setConfirmation('');
    setError(null);
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center">
          <WarningIcon color="error" sx={{ mr: 1 }} />
          アカウント削除の確認
        </Box>
      </DialogTitle>
      <DialogContent>
        <Alert severity="error" sx={{ mb: 2 }}>
          この操作は取り消せません。アカウントと関連する全てのデータが完全に削除されます。
        </Alert>

        <Typography variant="body2" color="text.secondary" gutterBottom>
          削除を確認するため、メールアドレスを入力してください:
        </Typography>
        <TextField
          fullWidth
          label="メールアドレス"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          margin="normal"
          type="email"
        />

        <Typography variant="body2" color="text.secondary" gutterBottom sx={{ mt: 2 }}>
          削除を確認するため、"DELETE"と入力してください:
        </Typography>
        <TextField
          fullWidth
          label="確認文字列"
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
          margin="normal"
          placeholder="DELETE"
        />

        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={isDeleting}>
          キャンセル
        </Button>
        <Button
          onClick={handleConfirm}
          color="error"
          variant="contained"
          disabled={isDeleting || !email || !confirmation}
        >
          {isDeleting ? '削除中...' : 'アカウントを削除'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default DeleteAccountDialog;
