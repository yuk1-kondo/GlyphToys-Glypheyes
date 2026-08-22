## Glypheyes

A playful "eye expressions" toy for the Nothing Phone's Glyph button. The pupil follows device tilt, and touch/long‑press changes expressions. When the battery is low, a sleepy upper eyelid gently descends.

[Download APK (latest)](https://github.com/yuk1-kondo/GlyphToys-Glypheyes/releases/latest/download/Glypheyes.apk)

### Features
- Tilt‑driven pupil movement (accelerometer)
  - Smoothly follows right/left/up/down and stays clamped inside the oval sclera
- Touch interactions
  - Touch down: Blink
  - Touch up: Smiling eyes (slightly squinted)
  - Long press: Surprise
  - Double press: Wink (left eye closes)
- Battery‑aware sleepiness
  - Below 30%: upper eyelid drops slightly
  - Below 15%: drops further (sleepier)
  - Charging starts: wakes up surprised, then smiles

### Controls
- Glyph button short press: switch toys (system behavior)
- Glyph button long press: Surprise
- Touch down: Blink
- Touch up: Smiling eyes
- Touch double press: Wink

---

## Glypheyes（日本語）

Nothing Phone の Glyph ボタンで遊べる“目の表情”トイです。端末の傾きに連動して瞳が動き、タッチやロングプレスで表情が変化します。バッテリー残量が少ないと、上まぶたがゆっくり降りて「眠そう」な表情になります。

[APK をダウンロード（最新）](https://github.com/yuk1-kondo/GlyphToys-Glypheyes/releases/latest/download/Glypheyes.apk)

### 主な機能
- 傾き連動の瞳移動（加速度センサー）
  - 右/左/上/下へ滑らかに追従。白目の楕円からはみ出さないようにクランプ
- タッチ操作
  - Touch-down: まばたき
  - Touch-up: 目が笑う（すこし細くなる）
  - Long press: 驚きの表情
  - ダブルプレス: ウインク（左目だけ閉じる）
- バッテリー連動の眠気
  - 30%未満: 上まぶたが少し降りる
  - 15%未満: さらに重く降りる
  - 充電開始: 驚いて目覚め、続けて笑顔になる

### 操作方法
- Glyph ボタン短押し: トイ切り替え（システム側機能）
- Glyph ボタン長押し: 驚き
- 押下（タッチダウン）: まばたき
- 離す（タッチアップ）: 目が笑う
- 2連続プレス（ダブルプレス）: ウインク

---

## Supported Devices

| Device | Glyph Matrix | Toy features |
|---|---|---|
| Phone (3) | 25×25 | All (touch interactions, AOD) |
| Phone (4a) Pro | 13×13 | AOD only (no Glyph Touch — touch/wink interactions unavailable) |

## Build

Requirements: Android Studio (JDK 21), Nothing Phone (3) / Phone (4a) Pro for on-device testing.

1. `app/libs/GlyphMatrixSDK.aar` must be present (bundled in this repo; Glyph Matrix SDK 2.0).
2. Open the project in Android Studio, or build from CLI:

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

3. Install the APK on your device and enable the toy from Settings → Glyph Essentials → Glyph Toys.

Notes: The `scripts/` and `data/` directories used during early prototyping (CSV/JSON frame generation for the Glyph Matrix Composer) have been removed; history remains in this repository's git history.

## 対応デバイス

| デバイス | Glyph Matrix | トイ機能 |
|---|---|---|
| Phone (3) | 25×25 | 全機能（タッチ操作・AOD） |
| Phone (4a) Pro | 13×13 | AOD専用（Glyph Touch非対応のためタッチ系操作は不可） |
