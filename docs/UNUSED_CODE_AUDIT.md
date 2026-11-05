# 未使用コード監査

- 実施日: 2026-11-06
- ブランチ: `codex/remove-proven-unused-code`
- 削除前の基準: `fb00f265acc01657902235dd8fd1e0f7144d3e81` (Panic Mode を先にコミット)
- 対象: Safe / Full の両方。バージョンや機能の公開範囲は変更しない。
- 既存の `codex/skills/hashimotoaddons-development/SKILL.md` の未コミット変更は対象外。

## 調査範囲と判定方法

`src/main/java` の162ファイル、`src/test/java` の11ファイル、リソース、Gradle、バッチ、Python、CI、隠しディレクトリ内の管理ファイルを確認した。依存物・生成物 (`.git`, `.gradle`, `build`, `gradle-*`, ZIP) はソース検索から除外し、ビルド済みクラスは別途解析した。

入口は `fabric.mod.json` の `HaClientMod` と、同ファイルから登録される `ha.client.mixins.json` の17クラス。そこからイベント登録、メソッド参照、GUI、設定・保存用の型参照をたどった。ファイル名だけではなく、同一ファイル内の複数の型と内部クラスも対応付けた。保守的な型参照グラフでは162ファイルすべてが接続されるため、丸ごと未使用と証明できた Java ファイルはない。これは全メソッドの実行可能性を証明するものではない。

削除するメソッドについては、(1) リポジトリ全体で宣言以外の呼び出し・名前参照がないこと、(2) private かつ override / Mixin 注入対象ではないこと、(3) テストからも参照されないこと、(4) Full / Safe の各 dev JAR 内314クラスの定数プールで、所有クラス・メソッド名・記述子が一致する参照がないことを確認した。定数プールの Methodref / InterfaceMethodref を集計し、メソッドハンドルが指す参照も含めた。名前だけの集計では見落とすオーバーロードも区別した。

この判定はリポジトリとそのビルドを対象とする。外部 Mod、外部リフレクション利用まで不存在と断言しない。初回は public API と動的な利用経路のある候補を残した。その後、所有者の追加指示により、public 8件も両バリアントのビルドを条件として削除対象に変更した。

## バッチ1: 削除前の証拠

以下の行番号は基準コミット時点。

| 対象 | リポジトリ検索・テスト | 入口からの到達性 / 判断 |
| --- | --- | --- |
| `HaButtonTooltips.addMacroListTooltip` (406–412) | 完全一致の名前は宣言1件のみ、テスト0件。両JARのメソッド参照0件 | `ScreenButtonTooltipMixin → HaButtonTooltips.get → addScreenTooltip` の実際の分岐は `addMacroListTooltipV2` を呼ぶ。旧 private 版への辺はない |
| `HaButtonTooltips.addMacroEditTooltip` (414–424) | 完全一致の名前は宣言1件のみ、テスト0件。両JARのメソッド参照0件 | 同じ分岐は `addMacroEditTooltipV2` を呼ぶ。旧 private 版への辺はない |
| `HaDropTrackerOverlay.getFooterHeight` (118–128) | 宣言1件のみ、テスト0件。両JARのメソッド参照0件 | HUD 登録から `render` は呼ばれるが、パネル高さの処理は `getPanelHeight` を使用。この private 計算関数は呼ばれない |
| `HaEvolutionForgeHelper.applyStatAdjustments` (969–977) | 宣言1件のみ、テスト0件。両JARのメソッド参照0件 | パケット・ツールチップ経路では `applyTrackedStatAdjustments` と `applyStatRangeAdjustments` を使用。旧 private 版への辺はない |
| `HaTickHandler.pressHotbarSlot(MinecraftClient,int)` (479–481) | 名前検索は4件だが、2引数版を呼ぶ箇所は0件。テスト0件。両JARの `(Lnet/minecraft/client/MinecraftClient;I)V` 参照0件 | 2引数版の本体は3引数版を呼ぶだけ。実際の `simulateHotbarKeyPress` のフォールバックも3引数版を呼ぶ。3引数版は残す |
| `HaConfig.GSON` (14) と専用 import 2行 | `HaConfig` 内ではフィールド名は宣言のみ。全体の `GSON` 利用は他クラスの別の private フィールド。テストから当該フィールドの参照なし | 設定読み書きは `HaConfigPersistence` の独立した `GSON` を使用。`HaConfig` 側は未読の Gson を生成するだけで、登録・ファイル操作の副作用はない。`Gson` / `GsonBuilder` import もこの初期化専用 |

ソース削除前に実行した検索:

```powershell
rg -n --hidden -g '!.git/**' -g '!build/**' -g '!.gradle/**' -g '!gradle-*/**' -g '!*.zip' '\b(addMacroListTooltip|addMacroEditTooltip|getFooterHeight|applyStatAdjustments|pressHotbarSlot)\b' .
```

結果 (パス共通部分 `src/main/java/com/example/ha/` を省略):

```text
HaButtonTooltips.java:406: private static void addMacroListTooltip(...)
HaButtonTooltips.java:414: private static void addMacroEditTooltip(...)
HaDropTrackerOverlay.java:118: private static int getFooterHeight()
HaEvolutionForgeHelper.java:969: private static double applyStatAdjustments(...)
HaTickHandler.java:479: private void pressHotbarSlot(MinecraftClient client, int slot)
HaTickHandler.java:480:     pressHotbarSlot(client, slot, 0);
HaTickHandler.java:483: private void pressHotbarSlot(MinecraftClient client, int slot, int holdTicks)
HaTickHandler.java:505:     pressHotbarSlot(client, slot, holdTicks);
```

監査レポート追加後の検索には、この文書自身もヒットする。元の検索は基準コミットに対する `git grep -n -w -E 'addMacroListTooltip|addMacroEditTooltip|getFooterHeight|applyStatAdjustments|pressHotbarSlot' fb00f26 -- .` でも再現できる。

`Class.forName`, `loadClass(`, `getDeclaredMethod`, `getMethod(`, `getDeclaredField`, `java.lang.reflect`, `ServiceLoader`, `MethodHandles`, `ScriptEngine`, `URLClassLoader` の全体検索は該当なし。削除対象に注釈や外部インターフェース実装はない。Gson のフィールド反射、Mixin、画面名による分岐は実在するため別途維持した。

## バッチ2: 呼び出しは存在するが到達不能な分岐

バッチ1後の `HaButtonTooltips.java` の検索結果:

```text
15: if (screen == null || rawLabel == null || !isSupportedScreen(screen)) {
24: addScreenTooltip(lines, screenName, label);
37: private static boolean isSupportedScreen(Screen screen) {
43: return !(screen instanceof HaDangerousFeaturesScreen)
71: private static void addScreenTooltip(List<String> lines, String screenName, String label) {
86: } else if ("HaDangerousFeaturesScreen".equals(screenName)) {
87:     addDangerousFeaturesTooltip(lines, label);
177: private static void addDangerousFeaturesTooltip(List<String> lines, String label) {
```

`rg -n --hidden` の全体検索で `addScreenTooltip` の呼び出しは `get` 内1件、`addDangerousFeaturesTooltip` は上記分岐内1件、テストからはいずれも0件。`HaDangerousFeaturesScreen` のクラス宣言はリポジトリ内1件、`public final class ... extends Screen`。入口でその型は必ず null を返して終了する。そのため、同じオブジェクトの `getClass().getSimpleName()` からこの画面名になり、かつ後続の専用分岐まで到達する経路はない。private なディスパッチ関数を外から呼ぶリフレクションも検出されていない。

この分岐2行と、その分岐だけが参照する private 関数11行 (区切り空行込み) を削除する。画面本体、`isSupportedScreen` の除外、他画面のツールチップは維持する。こちらは単純なメソッド参照数0ではなく、入口の型ガードに基づく削除である。削除前の `HaButtonTooltips.java` は `build/unused-audit/before-batch2/` に退避する。

## バッチ3: 追加指示による public メソッド8件の削除

以下はいずれも全 Java の名前検索で宣言のみ、テスト参照0件。クラス自体は実行経路につながっている。初回は外部利用を否定できないため保留したが、所有者から「ビルドできたら削除」と追加指示があった。Full / Safe 両方のテスト・ビルドを実施し、失敗時はこのバッチだけを復元する。

- `HaAfkFarming.getLastWebhookStatus`
- `HaDropTracker.getRegisteredItemCount`
- `HaItemProtect.ensureProtectId`
- `HaMobEsp.shouldGlow`
- `HaSubSkillTimer.getDurationMillis`
- `HaWaypointManager.setActiveColorSlot`
- `HaWaypointManager.clearCurrentDimension`
- `HaWaypointManager.getEditModeLabel`

削除直前の全体検索でも上記8件は各宣言1件と本レポートへの言及のみ。Full / Safe の各 dev JAR の定数プール参照は8件すべて0件。いずれも static メソッドで、Mixin 注釈やインターフェース実装ではない。入口からこれらのメソッドへの呼び出し辺はない。削除前ソース・行番号・参照数は `build/unused-audit/before-batch3/` に保存した。

`ensureProtectId` だけが `java.util.UUID` を使用するため、その import も削除する。既存アイテムの保護ID読込 (`getProtectId`)、通常の保護処理、Waypoint の他の編集経路、Webhook の送信・エラー処理には変更を加えない。ビルド成功は外部 Mod からの互換性や実際の Minecraft 動作の証明ではない。

結果: 8件と import 1行を削除した状態で Full / Safe とも `test build` 成功。各36テスト、失敗・エラー・スキップ0件。復元不要。両 dev JAR を再解析し、8メソッドの宣言がすべてなくなったこと、両配布 JAR がそれぞれ正しい製品名と version `1.13.0` を保持していることを確認した。このバッチは6ファイルから63行 (区切り空行込み) を削除した。

## 残した候補と理由

### 動的登録・互換性・別の入口

- Mixin クラス、注入メソッド、Accessor / Invoker: JSON と注釈による登録で実行される。通常の Java 呼び出し0件は削除根拠にしない。
- GUI の `init`, `render`, `onClose` 等と `HaGhostWall` の配置コンテキスト override: Minecraft の仮想呼び出しから使う。`HaButtonTooltips` は `getSimpleName()` による画面名判定も使う。
- `HaConfigSavedModels.java` / `HaWaypointSavedModels.java`: ファイル名と同名のクラスがなくても、`SavedConfig` / `SavedWaypointState` などが保存処理から利用される。Gson が JSON のフィールド名を使用するため、DTO のフィールドや旧設定互換フィールドは維持。
- `HaBuildFlags.DANGEROUS_FEATURES_ENABLED`: Safe では false、Full では true。Safe だけで参照が消える Full の処理を「固定でオフ」とは扱わない。両バリアントを比較すると、今回の5件以外に private の未参照メソッドとして Full 側で検出されたものはない (コンストラクター・合成メソッド・Mixin 除外)。
- 初期値 false の設定: GUI / 設定読込で変更可能。永続的な到達不能の証明にはならない。
- `HaSounds.init` 等の空に見える初期化: クラス初期化によるイベント登録に意味があるため維持。
- `review_stat_names.py`: `if __name__ == '__main__'` の独立した開発ツール。ゲームの入口からの参照は不要。
- `.github/scripts/validate_skills.py`: CI の独立した入口。`build-full.bat` / `build-safe.bat` も手動ビルド用入口。
- `crit.mp3`, `orb.mp3`, リソース内の MP3 / OGG: 音源原本、配布方針 (`docs/decisions/0008...`)、帰属表示があるため維持。Minecraft が直接再生するファイルだけに狭めない。
- ローカル Gradle 配布物、キャッシュ、生成物、ローカル履歴: 今回のプロジェクトコード削除には含めない。

### 到達不能分岐・コメント

ソース全体の `if (true)`, `if (false)`, `while (false)`, `static final boolean` 検索に該当なし。ビルド生成フラグは上記のバリアント依存。コメント開始・行コメントの41箇所を確認し、説明・Javadoc・互換性の理由であり、削除対象になるコメントアウト済みコードのブロックは見つからなかった。

## 検証と集計

削除前: Full / Safe とも `test build` 成功、各36テスト、失敗・エラー・スキップ0件。スキル検証と `git diff --check` も成功。

削除は上の6対象を1バッチとして行い、変更前の対象ファイルを `build/unused-audit/before/` にバイト単位で退避する。検証が失敗した場合はこのバッチだけを復元する。

バッチ1後: Full / Safe とも `test build` 成功、各36テスト、失敗・エラー・スキップ0件。ソース5ファイルから48行を削除。失敗がないため復元なし。

バッチ2後: Full / Safe とも `test build` 成功、各36テスト、失敗・エラー・スキップ0件。13行を追加削除。失敗がないため復元なし。

各段階で実行したコマンド:

```powershell
.\gradle-9.4.1\bin\gradle.bat test build -PhaVariant=full --console=plain
.\gradle-9.4.1\bin\gradle.bat test build -PhaVariant=safe --console=plain
```

| 段階 | Full | Safe |
| --- | --- | --- |
| 削除前 (Panic Mode コミット前) | 36件成功、BUILD SUCCESSFUL | 36件成功、BUILD SUCCESSFUL |
| バッチ1後 | 36件成功、BUILD SUCCESSFUL | 36件成功、BUILD SUCCESSFUL |
| バッチ2後 | 36件成功、BUILD SUCCESSFUL | 36件成功、BUILD SUCCESSFUL |
| バッチ3後 (public 8件) | 36件成功、BUILD SUCCESSFUL | 36件成功、BUILD SUCCESSFUL |

初回の制限環境内の起動は `native-platform.dll` をロードできず、ソースコンパイル前に失敗した。通常権限環境での実行に切り替え、上の全検証が成功した。Java 8 オプションと Gradle の非推奨警告はあるが、ビルドエラーはない。

最終確認:

- `git -c core.safecrlf=false diff --check`: 成功。
- `python .github/scripts/validate_skills.py`: `Validated 1 skill(s).`
- 両 dev JAR の314クラスを再走査: Full 側の未参照 private メソッド候補は0件 (コンストラクター・合成メソッド・Mixin 除外)。Safe 側だけに残る候補は Full の呼び出しがあるため維持。
- `javap -constants com.example.ha.HaBuildFlags` (各 dev JAR): Full は `VARIANT="full"` / `DANGEROUS_FEATURES_ENABLED=true`、Safe は `"safe"` / `false`。
- 両配布 JAR の `fabric.mod.json`: version `1.13.0`、製品名もそれぞれ正しい。Mixin 登録17件を維持。
- 生の定数プール監査結果はローカルの `build/unused-audit/before-bytecode.txt` と `after-bytecode.txt`、解析スクリプトは同ディレクトリの `bytecode_audit.py` に保存。生成物として Git 管理対象外。
- Minecraft の起動、GUI 操作、マルチプレイ、実環境での切断・終了は未検証。今回の削除は状態遷移・保存スキーマ・ライフサイクル処理を変更しない。

削除行数は Panic Mode コミット後の `git diff --numstat -- src/main/java` で集計した。ドキュメント追加と既存のスキル変更は含めない。

| ソースファイル | 削除行数 |
| --- | ---: |
| `HaAfkFarming.java` | 4 |
| `HaButtonTooltips.java` | 32 |
| `HaConfig.java` | 3 |
| `HaDropTracker.java` | 5 |
| `HaDropTrackerOverlay.java` | 12 |
| `HaEvolutionForgeHelper.java` | 10 |
| `HaItemProtect.java` | 20 |
| `HaMobEsp.java` | 4 |
| `HaSubSkillTimer.java` | 4 |
| `HaTickHandler.java` | 4 |
| `HaWaypointManager.java` | 26 |
| 合計 | 124 |

削除ファイル0、削除ディレクトリ0、ソース変更ファイル11。関数14件 (private 6件 / public 8件)、未読フィールド1件、到達不能分岐1件、関連 import 3行を除去。初回61行 + 追加指示63行 = 124行 (区切り空行込み)。
