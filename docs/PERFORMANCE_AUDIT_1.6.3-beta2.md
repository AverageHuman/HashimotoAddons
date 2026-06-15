# HashimotoAddons 1.6.3-beta2 GUI Performance Audit

監査日: 2026-06-14  
対象: Minecraft Java Edition 1.16.5 / Fabric / Full・Safe共通

## 結論

主因はMinecraft 1.16.5固有ではなく、HashimotoAddons固有の追加処理です。

Minecraft標準の `HandledScreen` は画面描画時にスロットを列挙しますが、tooltipはホバー中の `focusedSlot` に対してだけ生成します。HashimotoAddonsは従来、通常チェストを含む `GenericContainerScreen` の初回tickに全スロットの `ItemStack#getTooltip` を同期実行し、同じクライアントスレッドで複数JSONを書き込んでいました。この差分が「開いた瞬間」の停止を説明します。

Minecraft側にも通常の画面構築、アイテム描画、レシピブック更新、サーバーからのスロット同期コストはあります。そのため完全なゼロ停止は保証できませんが、標準処理にはHashimotoAddonsの全スロットtooltip学習や設定/索引JSON書き込みはありません。

## 一次資料

- [Fabric Yarn 1.16.5 HandledScreen](https://maven.fabricmc.net/docs/yarn-1.16.5+build.10/net/minecraft/client/gui/screen/ingame/HandledScreen.html)
- [Fabric Yarn 1.16.5 InventoryScreen](https://maven.fabricmc.net/docs/yarn-1.16.5+build.10/net/minecraft/client/gui/screen/ingame/InventoryScreen.html)
- [Fabric Yarn 1.16.5 GenericContainerScreen](https://maven.fabricmc.net/docs/yarn-1.16.5+build.10/net/minecraft/client/gui/screen/ingame/GenericContainerScreen.html)
- ローカルのYarnマップ済み1.16.5 JARを `javap -c -p` で確認し、`HandledScreen` がtooltipを `focusedSlot` にだけ渡すことを検証。

## 実施した軽量化

- 全画面学習は削除せず、1tickあたり最低18スロット、通常の90スロット画面で最大5tick以内に分割。
- ホバーされたアイテムは従来どおり、そのフレームで即時学習。
- 同一tooltipをホバーし続けた際の毎フレーム再学習を内容シグネチャで省略。
- config、Evolution Forge、Drop Tracker、Chest SearchのJSON書き込みを単一バックグラウンドワーカーへ移動。
- 同一ファイルへの連続保存は最新スナップショットへ集約。
- trackerの経過時間・累計値の設定保存を共有10秒周期へ集約。画面表示値は毎tick更新を維持。
- 切断時とJVM終了時に保存キューをflush。書き込み失敗はstderrへ報告し、黙殺しない。

## ユーザー影響

- Evolution Forge推定に必要な「GUI全体からの学習」と「ホバーからの学習」は両方維持。
- チェスト索引のメモリ更新、tracker表示、設定画面の反映は即時のまま。
- 通常終了・切断では最新状態を保存。異常終了時のみ、trackerの直近最大10秒がOS/JVM強制停止条件により失われる可能性がある。
- SafeへFull専用UI、設定、キー、動作は追加していない。

## ビルド履歴

すべて `-Pmod_version=1.6.3-beta2` を指定。

1. 変更前ベースライン: Full成功 / Safe成功
2. 共通非同期設定保存: Full成功 / Safe成功
3. Evolution Forge分割学習・非同期保存: Full成功 / Safe成功
4. Chest Search非同期保存: Full成功 / Safe成功
5. Drop Tracker非同期保存: Full成功 / Safe成功
6. tracker共有10秒保存: Full成功 / Safe成功

## ファイル別判定

判定は「今回のインベントリ/チェストを開く瞬間の停止を解消するため、コードリファクタリングが必要か」です。継続的な描画・Entity走査は、今回の停止原因でない場合は変更せず、計測が示したときだけ別作業にします。

| ファイル | 必要性 | 判定・対応 |
|---|---|---|
| `.codex/CURRENT_STATE.md` | いいえ | 揮発的な作業状態記録。ランタイム処理なし。 |
| `.codex/CURRENT_STATE.template.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `.github/scripts/validate_skills.py` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `.github/workflows/ci.yml` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `.gitignore` | いいえ | プロジェクト補助ファイル。今回のランタイム停止経路なし。 |
| `AGENTS.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `GPT55_context_template.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `build-full.bat` | いいえ | ビルド設定。ゲーム内GUI実行経路なし。 |
| `build-safe.bat` | いいえ | ビルド設定。ゲーム内GUI実行経路なし。 |
| `build.gradle` | いいえ | ビルド設定。ゲーム内GUI実行経路なし。 |
| `codex/skills/hashimotoaddons-development/SKILL.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `codex/skills/hashimotoaddons-development/agents/openai.yaml` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `codex/skills/hashimotoaddons-development/references/completion-checklist.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `codex/skills/hashimotoaddons-development/references/feature-workflow.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `codex/skills/hashimotoaddons-development/references/release-workflow.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `codex/skills/hashimotoaddons-development/references/session-transition.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/ARCHITECTURE.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/DEVELOPMENT.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/KNOWLEDGE.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/PERFORMANCE_AUDIT_1.6.3-beta2.md` | いいえ | 本監査記録。ランタイム処理なし。 |
| `docs/PROJECT.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/SAFE_FULL_POLICY.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/TECHNICAL_DEBT.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/decisions/0001-safe-full-variants.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `docs/decisions/0002-structured-session-knowledge.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `gradle.properties` | いいえ | ビルド設定。ゲーム内GUI実行経路なし。 |
| `review_stat_names.py` | いいえ | オフライン開発補助。ゲーム実行時には読み込まれない。 |
| `session_notes_10.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `session_notes_3.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `session_notes_4.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `session_notes_5.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `session_notes_6.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `session_notes_8.md` | いいえ（削除済み） | 作業ツリーでは既に削除され、実行・配布対象に存在しない。既存削除を保持。 |
| `settings.gradle` | いいえ | ビルド設定。ゲーム内GUI実行経路なし。 |
| `src/main/java/com/example/ha/HaAfkFarming.java` | いいえ（今回） | Full専用。ネットワーク処理は既存の非同期経路で、GUIオープン経路ではない。大規模クラスとして別途監視。 |
| `src/main/java/com/example/ha/HaAfkFarmingAutoMoveOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaAfkFarmingCircleOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaAfkFarmingDebugOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaAfkFarmingScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaAlchemyKilnAutomation.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaAlchemyKilnAutomationScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaAsyncFileWriter.java` | はい（完了） | ファイル単位のlatest-wins非同期書き込み、失敗通知、flush、JVM終了待機を共通化。 |
| `src/main/java/com/example/ha/HaAutoHealScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaBlockGalleryScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaButtonTooltips.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaCameraScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaChatFilter.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaChatFilterEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaChatFilterListScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaChatFilterManageScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaChestSearchIndex.java` | はい（完了） | チェストを開いた直後の索引JSON同期書き込みを非同期化。メモリ索引は即時更新を維持。 |
| `src/main/java/com/example/ha/HaChestSearchOverlay.java` | いいえ（今回） | 検索有効時の描画負荷。索引件数が極端に増えた場合のみ計測後に結果キャッシュを検討。 |
| `src/main/java/com/example/ha/HaChestSearchScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaChestSearchSlotHighlight.java` | いいえ（今回） | 最大コンテナスロットの名前/ID照合のみ。tooltip生成・永続化は行わない。 |
| `src/main/java/com/example/ha/HaChunkChestCounter.java` | いいえ（今回） | Full側HUD有効時のチャンク内BlockEntity走査。GUIオープン停止とは独立。 |
| `src/main/java/com/example/ha/HaChunkChestOverlay.java` | いいえ（今回） | 上記カウンタの描画呼び出し。常時負荷は別計測対象だが今回の瞬間停止原因ではない。 |
| `src/main/java/com/example/ha/HaChunkChestOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaChunkChestScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaClientMod.java` | いいえ | イベント登録のみ。GUI停止の実処理は各機能側にあり、登録構造の変更は不要。 |
| `src/main/java/com/example/ha/HaConfig.java` | いいえ（今回） | 状態・正規化のみ。保存最適化は専用のHaConfigPersistenceへ実装し、責務を増やさない。 |
| `src/main/java/com/example/ha/HaConfigPersistence.java` | はい（完了） | 設定JSON書き込みをクライアントスレッド外へ移し、頻繁な状態更新を10秒周期に集約。 |
| `src/main/java/com/example/ha/HaConfigSavedModels.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaConfigScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDamageTruncation.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaDamageTruncationScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDangerousFeaturesScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropNotifier.java` | いいえ（今回） | 有効時のItemEntity走査。画面オープンや同期永続化を行わない。 |
| `src/main/java/com/example/ha/HaDropNotifierEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropNotifierManageScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropNotifierScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropTracker.java` | はい（完了） | ドロップごとのJSON同期書き込みをスナップショット＋非同期書き込みへ変更。 |
| `src/main/java/com/example/ha/HaDropTrackerOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaDropTrackerOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropTrackerRegisteredEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropTrackerRegisteredListScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaDropTrackerScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaElementRarityScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaElementRaritySlotHighlight.java` | いいえ（今回） | 描画時の有界スロット走査のみ。全tooltip生成やディスクI/Oは行わない。 |
| `src/main/java/com/example/ha/HaElementTracker.java` | はい（完了） | 観測数・経過時間による設定全体の毎回保存を共有遅延保存へ変更。 |
| `src/main/java/com/example/ha/HaElementTrackerOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaElementTrackerOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaElementTrackerScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaElementTrackerTargetScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaEvolutionForgeHelper.java` | はい（完了） | 全スロットtooltip学習を18スロット単位（通常最大5tick）へ分割し、ホバー即時学習・全画面学習を維持。重複学習と同期保存も除去。 |
| `src/main/java/com/example/ha/HaEvolutionForgeScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaExpTracker.java` | はい（完了） | EXP加算・経過時間による設定全体の頻繁な保存を共有遅延保存へ変更。 |
| `src/main/java/com/example/ha/HaExpTrackerOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaExpTrackerOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaExpTrackerScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaExtrasOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaExtrasOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaExtrasScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaGearView.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaGearViewScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaGhostWall.java` | いいえ（今回） | Full専用かつ明示的な編集操作時の保存。既存の未コミットユーザー変更を保持し、今回のGUI経路では触れない。 |
| `src/main/java/com/example/ha/HaHpAlertEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaHpAlertListScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaHudEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaHudVisibility.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaItemLockHelper.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaItemLockOverlay.java` | いいえ（今回） | 描画時のスロット強調のみで、GUIを開く瞬間の重い処理はない。 |
| `src/main/java/com/example/ha/HaKeyCaptureHelper.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaLoreClipboard.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaMacroEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaMacroListScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaMacroStatusOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaMacroStatusOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaManaAlertEditScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaManaAlertListScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaMobEsp.java` | いいえ（今回） | Full専用・有効時のEntity走査。GUIオープン停止とは独立。 |
| `src/main/java/com/example/ha/HaMobEspOverlay.java` | いいえ（今回） | Full専用・有効時のワールド描画走査。別の継続フレーム負荷。 |
| `src/main/java/com/example/ha/HaMobEspScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaMobEspTracerOverlay.java` | いいえ（今回） | Full専用・有効時のトレーサ描画。GUI初期化には関与しない。 |
| `src/main/java/com/example/ha/HaMobHpDisplayOverlay.java` | いいえ（今回） | 有効時のEntity選択処理。GUIオープン固有経路ではない。 |
| `src/main/java/com/example/ha/HaMobHpDisplayOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaMobHpDisplayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaRitualBookTimer.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaRitualBookTimerOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaRitualBookTimerOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaRitualBookTimerScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaSoulbindProtection.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaSounds.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaSpotify.java` | いいえ（今回） | プロセス検出は既存の間隔制御・非同期経路。インベントリ/チェスト初期化とは独立。 |
| `src/main/java/com/example/ha/HaSpotifyOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaSpotifyOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaSpotifyScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaSubSkillTimer.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/HaSubSkillTimerOverlay.java` | いいえ | 描画専用。今回の画面オープン時同期学習・永続化経路には関与しない。 |
| `src/main/java/com/example/ha/HaSubSkillTimerOverlayScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaSubSkillTimerScreen.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/HaTickHandler.java` | はい（完了） | 共有遅延設定保存のtickを全トラッカー更新後に実行。 |
| `src/main/java/com/example/ha/mixin/ClientPlayNetworkHandlerMixin.java` | はい（完了） | 実切断時にForge学習データと設定保存キューをflushし、遅延保存のデータ損失を防止。 |
| `src/main/java/com/example/ha/mixin/ClientPlayerEntityMixin.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/mixin/EntityRendererMixin.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/mixin/GameMenuScreenMixin.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/mixin/HandledScreenAccessor.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/mixin/HandledScreenMixin.java` | いいえ | 入力時の保護処理。画面オープン時の全件処理はない。設定保存は共通非同期化の恩恵を受ける。 |
| `src/main/java/com/example/ha/mixin/HandledScreenRenderMixin.java` | いいえ | スロット強調を描画するだけで、全tooltip生成や同期I/Oはない。 |
| `src/main/java/com/example/ha/mixin/ItemStackTooltipMixin.java` | いいえ（直接変更不要） | tooltipフック自体は機能要件。重複学習と分割処理は呼び先のHelperで解消。 |
| `src/main/java/com/example/ha/mixin/KeyBindingAccessor.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/mixin/MinecraftClientAccessor.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/mixin/MinecraftClientDisconnectMixin.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/mixin/MinecraftClientGhostWallMixin.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/java/com/example/ha/mixin/ScreenButtonTooltipMixin.java` | いいえ | 画面UIまたはMixin。同期全スロットtooltip生成・ファイルI/OのGUIオープン経路なし。 |
| `src/main/java/com/example/ha/mixin/SlotAccessor.java` | いいえ | 内容走査済み。今回のGUIオープン時全tooltip生成・同期JSON書き込み経路なし。 |
| `src/main/resources/THIRD_PARTY_NOTICES.md` | いいえ | 文書・開発支援・CI。ゲーム内ランタイム負荷なし。 |
| `src/main/resources/assets/ha/sounds.json` | いいえ | 静的リソース。実行時のGUI処理ロジックなし。 |
| `src/main/resources/assets/ha/sounds/ritual_timer_ready.ogg` | いいえ | 静的リソース。実行時のGUI処理ロジックなし。 |
| `src/main/resources/fabric.mod.json` | いいえ | プロジェクト補助ファイル。今回のランタイム停止経路なし。 |
| `src/main/resources/ha.client.mixins.json` | いいえ | プロジェクト補助ファイル。今回のランタイム停止経路なし。 |

## 除外

`.tmp-ffmpeg/`、`.tmp-ffmpeg.zip`、`orb.mp3`、ローカルGradle配布物とzip、`build/`、`.gradle/` はHashimotoAddonsの追跡ソース・配布リソースではないため監査対象外。既存の未追跡/生成物には変更を加えていない。

## 未実施

- 実サーバー上でのフレームタイム/プロファイラ比較。サーバー固有tooltipと実データ量が必要なため、このワークスペースでは静的経路比較とビルド検証まで。

