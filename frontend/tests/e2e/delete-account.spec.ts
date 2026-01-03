import { test, expect } from '@playwright/test';

/**
 * アカウント削除E2Eテスト
 * 
 * フロー:
 * 1. ログイン（Google OAuth2認証をモック）
 * 2. ダッシュボード表示
 * 3. アカウント設定ページに移動
 * 4. アカウント削除ダイアログを開く
 * 5. メールアドレス入力
 * 6. "DELETE"入力
 * 7. 削除確認
 * 8. ログイン画面にリダイレクト
 */

test.describe('アカウント削除フロー', () => {
  test.beforeEach(async ({ page, context }) => {
    // JWT Cookieをモック設定（実際の環境ではGoogle OAuth2を通す）
    await context.addCookies([
      {
        name: 'jwt_token',
        value: 'mock-jwt-token-for-testing',
        domain: 'localhost',
        path: '/',
        httpOnly: true,
        secure: false,
        sameSite: 'Lax',
      },
    ]);
  });

  test('正常系: アカウント削除が成功する', async ({ page }) => {
    // API呼び出しをモック
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          userId: '12345678-1234-1234-1234-123456789abc',
          displayName: 'テストユーザー',
          createdAt: '2026-01-01T00:00:00Z',
          lastLoginAt: '2026-01-02T00:00:00Z',
        }),
      });
    });

    await page.route('**/api/users/12345678-1234-1234-1234-123456789abc', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            userId: '12345678-1234-1234-1234-123456789abc',
            displayName: 'テストユーザー',
            email: 'test@example.com',
            createdAt: '2026-01-01T00:00:00Z',
            lastLoginAt: '2026-01-02T00:00:00Z',
          }),
        });
      } else if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 204,
        });
      }
    });

    // 1. ダッシュボードにアクセス
    await page.goto('/dashboard');
    await expect(page.locator('text=テストユーザー')).toBeVisible();

    // 2. アカウント設定アイコンをクリック
    await page.getByRole('button', { name: /settings/i }).click();

    // 3. アカウント設定ページに移動
    await expect(page).toHaveURL('/account-settings');
    await expect(page.locator('h4:has-text("アカウント設定")')).toBeVisible();

    // 4. アカウント削除ボタンをクリック
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    // 5. ダイアログが表示されることを確認
    await expect(page.locator('text=アカウント削除の確認')).toBeVisible();

    // 6. メールアドレスを入力
    await page.fill('input[type="email"]', 'test@example.com');

    // 7. 削除確認文字列を入力
    await page.fill('input[placeholder="DELETE"]', 'DELETE');

    // 8. 削除実行ボタンをクリック
    await page.getByRole('button', { name: '削除する' }).click();

    // 9. ログイン画面にリダイレクトされることを確認
    await expect(page).toHaveURL(/\/login\?message=account_deleted/);
    await expect(page.locator('text=アカウントが削除されました')).toBeVisible();
  });

  test('異常系: メールアドレスが空欄の場合は削除できない', async ({ page }) => {
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          userId: '12345678-1234-1234-1234-123456789abc',
          displayName: 'テストユーザー',
          createdAt: '2026-01-01T00:00:00Z',
          lastLoginAt: '2026-01-02T00:00:00Z',
        }),
      });
    });

    await page.goto('/account-settings');
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    // メールアドレスを入力せず、削除確認文字列だけ入力
    await page.fill('input[placeholder="DELETE"]', 'DELETE');

    // 削除実行ボタンが無効化されていることを確認
    const deleteButton = page.getByRole('button', { name: '削除する' });
    await expect(deleteButton).toBeDisabled();
  });

  test('異常系: 削除確認文字列が"DELETE"でない場合は削除できない', async ({ page }) => {
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          userId: '12345678-1234-1234-1234-123456789abc',
          displayName: 'テストユーザー',
          createdAt: '2026-01-01T00:00:00Z',
          lastLoginAt: '2026-01-02T00:00:00Z',
        }),
      });
    });

    await page.goto('/account-settings');
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[placeholder="DELETE"]', 'delete'); // 小文字

    // 削除実行ボタンが無効化されていることを確認
    const deleteButton = page.getByRole('button', { name: '削除する' });
    await expect(deleteButton).toBeDisabled();
  });
});
