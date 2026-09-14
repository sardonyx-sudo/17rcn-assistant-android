# 扶輪公益網 Android 專屬 App - GitHub 推送與 CI/CD 流程指南

本文件記錄「扶輪公益網專屬 Android App (`05_Android專屬App`)」推送至 GitHub 的完整流程、儲存庫資訊、CI/CD 自動化編譯機制與排錯要點，供未來對話與日常維護快速查閱。

---

## 📌 一、儲存庫核心資訊 (Repository Metadata)

| 項目 | 詳細資訊 |
| :--- | :--- |
| **GitHub 儲存庫網址** | `https://github.com/sardonyx-sudo/17rcn-assistant-android.git` |
| **網頁專案主頁** | `https://github.com/sardonyx-sudo/17rcn-assistant-android` |
| **本機對應目錄** | `05_Android專屬App/`（Git 根目錄即在此資料夾內） |
| **預設追蹤分支** | `main` |
| **遠端名稱 (Remote)** | `origin` |
| **雲端自動編譯 (Actions)** | [GitHub Actions 狀態頁](https://github.com/sardonyx-sudo/17rcn-assistant-android/actions) |
| **最新 APK 一鍵下載 (Release)** | [GitHub Releases 最新發布頁](https://github.com/sardonyx-sudo/17rcn-assistant-android/releases/tag/latest) |

---

## 🚀 二、日常修改與代碼推送流程 (Standard Push Workflow)

當在本地端修改完 Android 專案代碼後，依照以下步驟推送：

### 步驟 1：開啟 PowerShell 並切換至專案目錄
```powershell
cd "d:\Users\Thomas\Desktop\AI\新增資料夾\扶輪公益網插件\05_Android專屬App"
```

### 步驟 2：檢查變更狀態
```powershell
git status
```
> 確認修改的檔案是否符合預期，確認沒有未被 `.gitignore` 忽略的臨時暫存檔（如 `build/`、`.gradle/`）。

### 步驟 3：暫存、提交並推送
```powershell
# 1. 將所有變更加入暫存區
git add .

# 2. 建立提交紀錄（填入本次修改的摘要）
git commit -m "feat: 更新物資卡片重新辨識與刪除功能"

# 3. 推送至 GitHub main 分支
git push origin main
```

---

## ⚙️ 三、雲端 CI/CD 自動編譯與 APK 產出機制

專案已配置 GitHub Actions 工作流程腳本：
- **檔案路徑**：`.github/workflows/build-apk.yml`
- **觸發時機**：每次推送（`push`）到 `main` 分支，或在 GitHub Actions 頁面上點擊「Run workflow」手動觸發。

### 🔄 雲端編譯流程包含：
1. **環境初始化**：Ubuntu 最新虛擬機，配置 Temurin JDK 17 與 Gradle 快取。
2. **自動檢查 Wrapper**：若缺少 `gradle-wrapper.jar` 會自動補齊。
3. **Gradle 構建**：執行 `./gradlew assembleDebug --stacktrace` 生成 Debug APK。
4. **雙重產出發布**：
   - **產出物 1 (Artifacts)**：上傳保留 30 天的 ZIP 檔案（供開發測試除錯）。
   - **產出物 2 (GitHub Releases)**：自動發布至 `latest` 標籤，掛載 **`app-debug.apk`**。

### 📲 手機端下載安裝方式：
志工或專責人員**無需登入 GitHub、無需解壓縮**，直接以手機瀏覽器打開：
👉 **`https://github.com/sardonyx-sudo/17rcn-assistant-android/releases/tag/latest`**  
點擊 **`app-debug.apk`** 即可在 Android 手機上一鍵下載安裝！

---

## 🛡️ 四、隱私與安全注意事項 (Privacy & Security)

1. **預設設定值 (PreferencesManager)**：
   - 專案中的 [`PreferencesManager.kt`](file:///d:/Users/Thomas/Desktop/AI/新增資料夾/扶輪公益網插件/05_Android專屬App/app/src/main/java/org/rotary/goodsassistant/data/PreferencesManager.kt) 內含自用測試階段之 **預設 GAS 網址** 與 **預設地址**。
2. **儲存庫公開性 (Visibility)**：
   - 強烈建議保持 GitHub 儲存庫為 **Private（私密）**，保護測試試算表佇列與真實地址不外洩。
   - 若未來需公開（Public），請務必將 `PreferencesManager.kt` 中的 `DEFAULT_GAS_URL` 與 `DEFAULT_ADDRESS` 清空，改為由使用者安裝後自行輸入。

---

## 🔧 五、常見問題與排錯指南 (Troubleshooting)

### Q1：`git push` 提示被拒絕（non-fast-forward / conflict）
* **原因**：遠端 GitHub 儲存庫有本地尚未同步的提交。
* **解法**：先拉取並重整，再重新推送：
  ```powershell
  git pull --rebase origin main
  git push origin main
  ```

### Q2：Windows 提示需要 GitHub 帳號密碼 / 驗證失敗
* **原因**：GitHub 已全面停用帳號密碼驗證，改用 Personal Access Token (PAT) 或 Git Credential Manager。
* **解法**：
  1. 系統通常會彈出瀏覽器授權視窗，點擊「Sign in with your browser」即可。
  2. 若無彈窗，請至 GitHub ➔ Settings ➔ Developer settings ➔ Personal access tokens 產生具備 `repo` 權限的 Token，在終端機密碼欄輸入該 Token。

### Q3：GitHub Actions 編譯失敗提示「Permission denied to github-actions[bot]」
* **原因**：儲存庫未開啟 Actions 寫入 Releases 權限。
* **解法**：進入 GitHub 儲存庫 ➔ `Settings` ➔ `Actions` ➔ `General` ➔ 滑到 `Workflow permissions` ➔ 勾選 **Read and write permissions** ➔ 點擊 Save。
