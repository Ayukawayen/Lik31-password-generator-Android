# Lik31 Android

Lik31 確定性密碼產生器的 Android 版。演算法與瀏覽器套件
「Lik31 Password Generator」3.0.0 相同，同樣的主密鑰與服務金鑰會得到同樣的密碼。
操作方式延續舊的 MS Password Generator：開啟即出現對話框與鍵盤，點一下密碼就複製並關閉。

## 目前進度：階段 0–5

| 階段 | 內容 | 狀態 |
|---|---|---|
| 0 | 專案骨架、CI、golden 測試資料產生器 | 完成 |
| 1 | `:core-lik31` 演算法 | 完成，測試通過 |
| 2 | 最小可用產生器 App | 完成，CI 建置成功，APK 已實機測試 |
| 3 | 快速設定方塊、正式圖示 | 程式碼完成，**待 CI 建置與實機確認** |
| 4 | 記住每個服務的長度與符號 | 邏輯測試通過；儲存部分**待 CI 建置與實機確認** |
| 5 | 儲存主密鑰（Android Keystore 加密） | 邏輯測試通過；Keystore 部分**待 CI 建置與實機確認** |

## 使用方式

- 從 App 圖示或快速設定方塊開啟（方塊需先在快速設定的編輯畫面手動加入）。
- 輸入主密鑰與服務金鑰，密碼即時顯示；點密碼或按鍵盤的完成鍵即複製並關閉。
- 複製時會記住這個服務的長度與符號，下次輸入同一個服務金鑰時自動套用。
- 打開「儲存主密鑰於此裝置」後，之後開啟會直接從服務金鑰開始輸入。
  - 儲存的是主密鑰的雜湊，以 Android Keystore 中的 AES-256-GCM 金鑰加密，不會進入備份或裝置轉移。
  - 主密鑰欄留空＝使用已儲存的主密鑰；輸入內容＝本次改用輸入的主密鑰，複製密碼時取代已儲存的。
  - 關閉開關會先確認，確認後刪除已儲存的資料與金鑰。
- 離開畫面（回桌面、切換 App、關螢幕）即關閉並清除輸入中的內容。

## 建置

需要 JDK 17 以上與 Android SDK（API 36）。用 Android Studio 開啟專案資料夾，或：

```
./gradlew :core-lik31:test :app:testDebugUnitTest   # 單元測試
./gradlew :app:assembleDebug                        # 產生 app/build/outputs/apk/debug/app-debug.apk
```

推送到 GitHub 後，`.github/workflows/ci.yml` 會自動跑測試並把 debug APK 存成 artifact。

## 驗證狀態

- 階段 0–2：GitHub Actions CI（2026-11-12）全部成功，debug APK 已實機測試。
- 階段 3–5：
  - 純 Kotlin 邏輯（狀態轉換、設定編碼、儲存鍵值雜湊）已實際編譯並通過測試，目前共 61 個測試。
  - Android 專屬程式碼（方塊服務、Keystore、SharedPreferences、介面更新）只對著手寫的 API 替身做過型別檢查，
    尚未以 Android SDK 建置，也未在裝置上執行。
- 無法連線 Maven 時，可用 `tools/run-local-tests.sh` 以 Gradle 內附的 Kotlin 編譯器跑純 Kotlin 部分的測試。

## 結構

```
core-lik31/           純 Kotlin 演算法（不依賴 Android）
app/                  Android App（Compose、Material 3）
design/               圖示原稿（依 U+26BF ⚿ 的造型繪製）
tools/gen-vectors/    用 Node 執行原版 popup.js，產生 golden-vectors.json
tools/run-local-tests.sh  不經 Gradle 相依解析的本機測試
```

## 更新比對資料

瀏覽器套件改版時，以新版覆蓋 `tools/gen-vectors/popup.js`，然後：

```
node tools/gen-vectors/gen-vectors.mjs
```

CI 會檢查已提交的 `golden-vectors.json` 與重新產生的結果一致。

## 與瀏覽器版的行為差異（都不影響產生的密碼）

- 服務金鑰為空時不顯示密碼（瀏覽器版會產生一組）。
- 符號欄只接受一個 BMP 字元；emoji 與控制字元會被拒絕（瀏覽器版會插入半個代理對，產生亂碼）。
- 主密鑰為空且沒有已儲存的主密鑰時，照樣產生密碼，與瀏覽器版一致；警告提示留待階段 7。
- 每個服務除了長度，也記住符號（瀏覽器版只記長度）。記錄在複製密碼時寫入。
- 已儲存主密鑰時清空主密鑰欄，代表使用已儲存的主密鑰（瀏覽器版會把空字串的雜湊存起來覆蓋掉）。
- 新輸入的主密鑰在複製密碼時才取代已儲存的（瀏覽器版每打一個字就覆蓋一次）。空的主密鑰不會被儲存。
