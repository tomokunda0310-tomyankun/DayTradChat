# DayTradeChat

Android 14 / Kotlin / Compose / Room ベースの受信専用デイトレ通知チャットクライアント最小構成です。

## 主要仕様
- TCP Socket クライアント
- ホスト可変、ポート5001固定
- 改行区切り JSON 受信
- 5秒自動再接続
- 30秒 ping
- 初回 register 送信
- Room に履歴保存
- Compose UI
- ログ画面あり
- 銘柄コード行タップでコピー

## ディレクトリ
- `MainActivity.kt` : 起動
- `Main.kt` : ViewModel
- `Sync.kt` : 通信入口プレースホルダ
- `Display.kt` : UI入口プレースホルダ
- `Config.kt` : 設定入口プレースホルダ
- `network/SocketClientManager.kt` : TCP接続
- `data/*` : Room / Repository
- `ui/DisplayApp.kt` : 画面
- `config/ConfigStore.kt` : AES + JSON 保存

## 初期化コマンド例
```bash
git init
git add .
git commit -m "init: DayTradeChat minimal client"
```
