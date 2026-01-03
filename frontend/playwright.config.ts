import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright設定ファイル
 * E2Eテスト実行設定
 */
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false, // テストを順次実行（アカウント削除は影響大）
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
