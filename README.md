# Lik31 Android

Lik31 確定性密碼產生器的 Android 版。演算法與瀏覽器套件
「Lik31 Password Generator」3.0.0 相同，同樣的主密鑰與服務金鑰會得到同樣的密碼。
操作方式延續舊的 MS Password Generator：開啟即出現對話框與鍵盤，點一下密碼就複製並關閉。

## 目前進度：階段 0–2（MVP）

| 階段 | 內容 | 狀態 |
|---|---|---|
| 0 | 專案骨架、CI、golden 測試資料產生器 | 完成 |
| 1 | `:core-lik31` 演算法 | 完成，測試通過 |
| 2 | 最小可用產生器 App | 程式碼完成，**尚未以 Android SDK 實際建置**（見下方） |

MVP 不儲存任何資料：每次開啟都要輸入主密鑰；離開畫面（回桌面、切換 App、關螢幕）即關閉並清除輸入。

## 建置

需要 JDK 17 以上與 Android SDK（API 36）。用 Android Studio 開啟專案資料夾，或：

```
./gradlew :core-lik31:test :app:testDebugUnitTest   # 單元測試
./gradlew :app:assembleDebug                        # 產生 app/build/outputs/apk/debug/app-debug.apk
```

推送到 GitHub 後，`.github/workflows/ci.yml` 會自動跑測試並把 debug APK 存成 artifact。

## 驗證狀態

開發環境連不到 Maven Central 與 Google Maven，因此：

- **已實際編譯並執行**：`:core-lik31` 全部程式與測試、`GeneratorState`（輸入→密碼的狀態邏輯）與其測試，
  共 38 個測試，其中包含 246 筆由瀏覽器套件原始碼產生的比對資料。
  使用 Gradle 內附的 Kotlin 編譯器，指令為 `tools/run-local-tests.sh`。
- **只做過型別檢查**：Activity、Compose 介面、剪貼簿等 Android 專屬程式碼，
  是對著手寫的 API 替身編譯的，沒有用真正的 Android SDK / Compose 建置，也沒有在裝置上執行過。
  版本目錄（`gradle/libs.versions.toml`）中的版本號也未經實際下載驗證。
  第一次建置若有錯誤，預期是小幅修正。

## 結構

```
core-lik31/           純 Kotlin 演算法（不依賴 Android）
app/                  Android App（Compose、Material 3）
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
- 主密鑰為空時照樣產生密碼，與瀏覽器版一致；警告提示留待階段 7。
