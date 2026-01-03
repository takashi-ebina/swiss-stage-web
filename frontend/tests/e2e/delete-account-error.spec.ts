import { test, expect } from '@playwright/test';

/**
 * アカウント削除エラーケースE2Eテスト
 * 
 * フロー:
 * 1. メールアドレス不一致エラー
 * 2. サーバーエラー（進行中トーナメント存在など）
 */

test.describe('アカウント削除エラーケース', () => {
  test.beforeEach(async ({ page, context }) => {
    // JWT Cookieをモック設定
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

    // 共通モック: ユーザー情報取得
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
            email: 'actual@example.com',
            createdAt: '2026-01-01T00:00:00Z',
            lastLoginAt: '2026-01-02T00:00:00Z',
          }),
        });
      }
    });
  });

  test('異常系: メールアドレスが一致しない場合エラーメッセージを表示', async ({ page }) => {
    // DELETE APIをモック（メール不一致エラー）
    await page.route('**/api/users/12345678-1234-1234-1234-123456789abc', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Email address does not match',
          }),
        });
      }
    });

    await page.goto('/account-settings');
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    // 間違ったメールアドレスを入力
    await page.fill('input[type="email"]', 'wrong@example.com');
    await page.fill('input[placeholder="DELETE"]', 'DELETE');

    // 削除実行
    await page.getByRole('button', { name: '削除する' }).click();

    // エラーメッセージが表示されることを確認
    await expect(page.locator('text=メールアドレスが一致しません')).toBeVisible();
  });

  test('異常系: 進行中のトーナメントが存在する場合エラーメッセージを表示', async ({ page }) => {
    // DELETE APIをモック（進行中トーナメント存在エラー）
    await page.route('**/api/users/12345678-1234-1234-1234-123456789abc', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Cannot delete account with pending tournaments',
          }),
        });
      }
    });

    await page.goto('/account-settings');
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    await page.fill('input[type="email"]', 'actual@example.com');
    await page.fill('input[placeholder="DELETE"]', 'DELETE');

    // 削除実行
    await page.getByRole('button', { name: '削除する' }).click();

    // エラーメッセージが表示されることを確認
    await expect(page.locator('text=進行中のトーナメントが存在するため削除できません')).toBeVisible();
  });

  test('異常系: サーバーエラーの場合汎用エラーメッセージを表示', async ({ page }) => {
    // DELETE APIをモック（サーバーエラー）
    await page.route('**/api/users/12345678-1234-1234-1234-123456789abc', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'Internal server error',
          }),
        });
      }
    });

    await page.goto('/account-settings');
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    await page.fill('input[type="email"]', 'actual@example.com');
    await page.fill('input[placeholder="DELETE"]', 'DELETE');

    // 削除実行
    await page.getByRole('button', { name: '削除する' }).click();

    // 汎用エラーメッセージが表示されることを確認
    await expect(page.locator('text=削除に失敗しました')).toBeVisible();
  });

  test('異常系: キャンセルボタンで削除をキャンセルできる', async ({ page }) => {
    await page.goto('/account-settings');
    await page.getByRole('button', { name: 'アカウントを削除' }).click();

    // ダイアログが表示されることを確認
    await expect(page.locator('text=アカウント削除の確認')).toBeVisible();

    // キャンセルボタンをクリック
    await page.getByRole('button', { name: 'キャンセル' }).click();

    // ダイアログが閉じられることを確認
    await expect(page.locator('text=アカウント削除の確認')).not.toBeVisible();

    // アカウント設定ページに留まることを確認
    await expect(page).toHaveURL('/account-settings');
  });
});
