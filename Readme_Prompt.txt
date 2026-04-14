# DayTradeChat v1.10-17 Complete Overlay

このZIPは、この会話で安定化した「通信復帰 + 監視/履歴/LOG/設定タブ」構成の
**上書き用完全版ソースセット**です。

## 先に削除するファイル
以下が残っていると競合や古い参照で壊れる可能性があります。

- app/src/main/java/com/daytradchat/papa/Main.kt
- app/src/main/java/com/daytradchat/papa/ChatMessage.kt
- app/src/main/java/com/daytradchat/papa/ui/DisplayApp.kt
- app/src/main/java/com/daytradchat/papa/ui/MonitorScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/HistoryScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/LogScreen.kt
- app/src/main/java/com/daytradchat/papa/ui/SettingsScreen.kt

## 上書き対象
- app/src/main/java/com/daytradchat/papa/MainActivity.kt
- app/src/main/java/com/daytradchat/papa/MainViewModel.kt
- app/src/main/java/com/daytradchat/papa/SocketClientManager.kt
- app/src/main/java/com/daytradchat/papa/ui/MainScreen.kt

## 現在の状態
- サーバ接続: OK
- signal受信: OK
- 監視タブ: 9枠表示
- 履歴タブ: 受信一覧
- LOGタブ: rawログ表示
- 設定タブ: 表示のみ（保存未実装）

## 注意
これは「今の壊れた状態から立て直すための、会話内で確定している完全上書きセット」です。
既存GradleやManifestは流用前提です。





{"type":"server_hello"}
{"type":"register_ack"}
{"type":"pong"}
	
app/
 └─ src/main/java/com/daytradchat/papa/
     ├─ MainActivity.kt
     ├─ ui/
     │   ├─ MainScreen.kt
     │   ├─ MonitorScreen.kt
     │   ├─ HistoryScreen.kt
     │   ├─ LogScreen.kt
     │   └─ SettingsScreen.kt
     ├─ viewmodel/
     │   └─ MainViewModel.kt
     └─ network/
         └─ SocketClientManager.kt
		 
		 
		 

# DayTradeChat Androidアプリ 実装仕様 v1.10-11

あなたは Android / Kotlin 開発者です。
既存の DayTradeChat プロジェクトを修正してください。
開発環境は AndroidIDE、Kotlin、Compose、Gradle Kotlin DSL。
差分ではなく、毎回ファイル全文で提示し、可能ならZIPでまとめること。
各ソース先頭には必ずフルパスコメントとバージョンコメントを付けること。

## 最重要ルール
- 既存ソースを壊さず、ビルドが通る最小構成を優先
- 不要ファイルが残る場合は、削除対象を明示すること
- ソースは全文提示
- Kotlinソース先頭に必ず以下形式を付ける
  - //app/src/main/java/...
  - //ver 1.10-11
- XMLは1行目を <?xml ...?> にし、2行目以降にフルパスコメント
- 途中で質問しない
- 仮決めで進める
- 毎回最後にgitコマンドを出す

---

## 現在の状態
- サーバ通信は成功している
- メイン画面の見栄えは概ねOK
- タブはある
- 監視 / 履歴 / LOG / 設定 が存在
- ただしUI改善と追加機能が必要

---

## 修正要件

### 1. メイン画面の文字調整
- SELL が改行されないようにフォントサイズ、余白、行間を調整
- BUY / SELL を1行に収める
- コード + BUY/SELL を最優先で見やすくする
- カード内テキストは均等に見えるよう調整
- 可能なら等幅寄りフォント、または数字の位置が揃うよう調整

### 2. 連続回数表示
各銘柄カードに以下を追加
- 買（上昇）連続回数
- 売（下落）連続回数

仮仕様:
- signalType == BUY を買連続
- signalType == SELL を売連続
- 同一銘柄で直近受信履歴を見て連続数を算出
- 連続が途切れたら1に戻す
- 表示例:
  - 買連:3
  - 売連:5

### 3. 銘柄タップで履歴ポップアップ
- メイン画面の銘柄カードをタップすると、その銘柄の履歴ポップアップを表示
- ダイアログまたは BottomSheet で可
- 数行分の履歴を表示
- 表示内容:
  - 時刻
  - BUY/SELL
  - 値
  - 点
  - 差
  - 買↑/売↓
  - 上強/下弱 など
- 新しい履歴を上、古い履歴を下
- 上昇下降傾向がわかる形式にする

### 4. 銘柄履歴の簡易グラフ
- 可能なら履歴ポップアップ内に棒グラフまたは簡易折れ線を表示
- 直近5〜10件
- まずは簡易棒グラフで良い
- 値動きまたは score の推移を表示
- 外部の重いライブラリは避ける
- Compose Canvas で描けるならそれで良い

### 5. 8固定 + 9番目入替枠
監視画面の銘柄配置ルール:
- 1〜8番目は固定銘柄枠
- 9番目は新規銘柄枠
- 新規銘柄はまず9番目に表示
- 1〜8のどれかが一定時間または一定回数受信なしなら、9番目を昇格
- 昇格ルールは仮決めで実装してよい

仮決め仕様:
- 直近受信時刻を保持
- 1〜8の固定銘柄が 5分以上更新なし なら脱落候補
- 9番目の銘柄が一定回数（例: 3回）以上連続受信で昇格候補
- 脱落候補と入替候補が揃ったら昇格
- そのロジックは MainViewModel 側で管理

### 6. 設定画面のホスト保存
- 設定画面のホスト名はローカル保存
- アプリ再起動時に読み出す
- ソースコードに固定埋め込みしない
- DataStore または SharedPreferences を使用
- 起動時に保存値があればそれを優先
- 未保存時だけデフォルト値を使う

### 7. 設定画面にリセットボタン
- リセットボタン追加
- メイン画面の監視銘柄表示をクリア
- 履歴もクリア
- LOGもクリア
- 保存している固定銘柄管理情報もクリア

### 8. 銘柄コード送信機能
設定ではなく、メイン画面または専用エリアに以下追加
- 銘柄コード送信テキストボックス
- 送信ボタン
- その左に、現在受信中銘柄のドロップダウンリスト
- ドロップダウンで選んだ銘柄を「入替前」
- テキストボックスへ入力した銘柄を「入替後」
- 送信時に、サーバへ JSON を1行送信する

サーバの受信処理は別チャットで実装するため、
このチャットでは Android側の送信機能だけ実装すること

### 9. 送信用JSON仕様
以下フォーマットで送信すること

単純銘柄送信:
{
  "type": "client_code_select",
  "before_code": "6323",
  "after_code": "7203",
  "sent_at": "2026-04-10 15:30:00"
}

日経平均指定:
{
  "type": "client_code_select",
  "before_code": "6323",
  "after_code": "【日経】",
  "sent_at": "2026-04-10 15:30:00"
}

業界平均指定も将来ありうるため、after_code は文字列で扱うこと
銘柄コード前提の数値型にしないこと

### 10. 送信機能の実装方法
- 既存 SocketClientManager の writer を使って1行JSON送信
- 改行区切りで送る
- UTF-8
- writer.write(json)
- writer.write("\\n")
- writer.flush()
- 接続中のみ送信可
- 未接続時は LOG に「未接続で送信失敗」と表示
- 送信成功時は LOG に「送信: before→after」と表示

### 11. 日経平均・業界平均対応
将来の受信で以下のような特殊コードが来ても落ちないようにする
- 【日経】
- 【業界平均】
- その他のラベル文字列

仕様:
- code は String で保持
- 数字4桁固定前提で実装しない
- 表示でもそのまま文字列を表示する

### 12. 履歴画面
- 新着を上に追加
- 過去を下へ流す
- signalのみ表示
- 行間を詰める
- 可能なら簡易表形式
- 表示列:
  - 時刻
  - コード
  - BUY/SELL
  - 値
  - 点
  - 差
  - 買↑/売↓
  - 上強/下弱

### 13. LOG画面
- 新着を上
- 過去を下
- ping/pong は省略または集約
- signalログは行ごとに色分け
  - BUY系は赤
  - SELL系は緑
- 接続ログは別色
- 可能なら表形式またはセル風に揃える
- 見栄えを重視
- 行間を詰める

### 14. UI見栄え
- メイン画面は今の見栄えをベース
- BUYカードは赤系
- SELLカードは緑系
- 空枠はダークグレー
- フォントは一段小さく調整して改行崩れ防止
- SELL の強制改行防止
- 数字、コード、時刻が揃って見えるように工夫

### 15. 実装方針
- MainViewModel に監視銘柄管理ロジックを持たせる
- SocketClientManager は送受信専任
- DataStore/SharedPreferences で host 保存
- ポップアップは Dialog または ModalBottomSheet
- グラフは Compose Canvas の簡易実装で良い
- まずビルドが通る最小構成
- その後見栄え微調整

---

## 欲しい成果物
- ZIP完全版
- 削除対象ファイル一覧
- MainActivity.kt 全文
- MainViewModel.kt 全文
- MainScreen.kt 全文
- SocketClientManager.kt 全文
- 設定保存関連ファイル全文
- 送信JSONの仕様説明
- gitコマンド


未実装
SELL改行防止のフォント調整
履歴/LOGの新着を上へ
Host保存
リセットボタン
送信テキストボックス＋送信ボタン
銘柄タップ履歴ポップアップ
8固定+9入替
連続回数
簡易グラフ

送信
{
  "type": "client_code_select",
  "before_code": "6323",
  "after_code": "7203",
  "sent_at": "2026-04-10 15:30:00"
}


