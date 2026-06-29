package com.example.ha;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HaButtonTooltips {
    private HaButtonTooltips() {
    }

    public static List<Text> get(Screen screen, String rawLabel) {
        if (screen == null || rawLabel == null || !isSupportedScreen(screen)) {
            return null;
        }

        String screenName = screen.getClass().getSimpleName();
        String label = normalize(rawLabel);
        List<String> lines = new ArrayList<String>();

        addGenericTooltip(lines, label);
        addScreenTooltip(lines, screenName, label);

        if (lines.isEmpty()) {
            return null;
        }

        List<Text> result = new ArrayList<Text>();
        for (String line : lines) {
            result.add(new LiteralText(line));
        }
        return result;
    }

    private static boolean isSupportedScreen(Screen screen) {
        Package pkg = screen.getClass().getPackage();
        if (pkg == null || !"com.example.ha".equals(pkg.getName())) {
            return false;
        }

        return !(screen instanceof HaDangerousFeaturesScreen)
            && !(screen instanceof HaAutoHealScreen)
            && !(screen instanceof HaMacroStatusOverlayScreen)
            && !(screen instanceof HaChunkChestScreen)
            && !(screen instanceof HaChunkChestOverlayScreen)
            && !(screen instanceof HaExtrasScreen)
            && !(screen instanceof HaExtrasOverlayScreen)
            && !(screen instanceof HaBlockGalleryScreen)
            && !(screen instanceof HaEvolutionForgeScreen)
            && !(screen instanceof HaMobEspScreen);
    }

    private static void addGenericTooltip(List<String> lines, String label) {
        if ("Go Back".equals(label)) {
            lines.add("前の画面に戻ります。");
        } else if ("Done".equals(label)) {
            lines.add("変更を確定して画面を閉じます。");
        } else if ("< Back".equals(label) || "Back <".equals(label)) {
            lines.add("前のページを表示します。");
        } else if ("Next >".equals(label)) {
            lines.add("次のページを表示します。");
        } else if ("Save".equals(label)) {
            lines.add("入力した内容を保存します。");
        } else if ("Delete".equals(label)) {
            lines.add("この設定を削除します。");
        }
    }

    private static void addScreenTooltip(List<String> lines, String screenName, String label) {
        if ("HaConfigScreen".equals(screenName) && "Element Rarity".equals(label)) {
            lines.add("エレメント系アイテムのスロットを名前色と同じ色で強調表示します。");
            return;
        }
        if ("HaConfigScreen".equals(screenName) && "Damage Truncation".equals(label)) {
            lines.add("1000000以上の数値をm、b、t表記に短縮して表示します。Expを含む表示はそのままです。");
            return;
        }
        if ("HaConfigScreen".equals(screenName)) {
            addMainMenuTooltip(lines, label);
        } else if ("HaMacroListScreen".equals(screenName)) {
            addMacroListTooltipV2(lines, label);
        } else if ("HaMacroEditScreen".equals(screenName)) {
            addMacroEditTooltipV2(lines, label);
        } else if ("HaDangerousFeaturesScreen".equals(screenName)) {
            addDangerousFeaturesTooltip(lines, label);
        } else if ("HaTriggerBotScreen".equals(screenName)) {
            addTriggerBotTooltip(lines, label);
        } else if ("HaCameraScreen".equals(screenName)) {
            addCameraTooltip(lines, label);
        } else if ("HaChestSearchScreen".equals(screenName)) {
            addChestSearchTooltip(lines, label);
        } else if ("HaChatFilterListScreen".equals(screenName)
            || "HaChatFilterManageScreen".equals(screenName)
            || "HaChatFilterEditScreen".equals(screenName)) {
            addChatFilterTooltip(lines, screenName, label);
        } else if ("HaHpAlertListScreen".equals(screenName)
            || "HaHpAlertEditScreen".equals(screenName)) {
            addAlertTooltip(lines, screenName, label, "HP");
        } else if ("HaManaAlertListScreen".equals(screenName)
            || "HaManaAlertEditScreen".equals(screenName)) {
            addAlertTooltip(lines, screenName, label, "Mana");
        } else if ("HaDropTrackerScreen".equals(screenName)
            || "HaDropTrackerRegisteredListScreen".equals(screenName)
            || "HaDropTrackerRegisteredEditScreen".equals(screenName)
            || "HaDropTrackerOverlayScreen".equals(screenName)) {
            addDropTrackerTooltip(lines, screenName, label);
        } else if ("HaDropNotifierScreen".equals(screenName)
            || "HaDropNotifierManageScreen".equals(screenName)
            || "HaDropNotifierEditScreen".equals(screenName)) {
            addDropNotifierTooltip(lines, screenName, label);
        } else if ("HaExpTrackerScreen".equals(screenName)
            || "HaExpTrackerOverlayScreen".equals(screenName)) {
            addExpTrackerTooltip(lines, label);
        } else if ("HaElementTrackerScreen".equals(screenName)
            || "HaElementTrackerTargetScreen".equals(screenName)
            || "HaElementTrackerOverlayScreen".equals(screenName)) {
            addElementTrackerTooltip(lines, screenName, label);
        } else if ("HaMobHpDisplayScreen".equals(screenName)
            || "HaMobHpDisplayOverlayScreen".equals(screenName)) {
            addMobHpDisplayTooltip(lines, label);
        } else if ("HaSubSkillTimerScreen".equals(screenName)
            || "HaSubSkillTimerOverlayScreen".equals(screenName)) {
            addSubSkillTimerTooltip(lines, label);
        } else if ("HaRitualBookTimerScreen".equals(screenName)
            || "HaRitualBookTimerOverlayScreen".equals(screenName)) {
            addRitualBookTimerTooltip(lines, label);
        } else if ("HaElementRarityScreen".equals(screenName)) {
            lines.add("エレメント名を含むアイテム枠をレアリティ色で見やすくします。");
        } else if ("HaDamageTruncationScreen".equals(screenName)) {
            lines.add("1000000以上の数値をm、b、t表記に短縮して表示します。Expを含む表示はそのままです。");
        } else if ("HaAfkFarmingScreen".equals(screenName)) {
            addAfkFarmingTooltip(lines, label);
        }
    }

    private static void addMainMenuTooltip(List<String> lines, String label) {
        if (label.startsWith("Item Lock")) {
            lines.add("インベントリ内のロック済みスロットを保護します。");
        } else if ("HP Alert".equals(label)) {
            lines.add("HPが指定割合以下になった時の警告を設定します。");
        } else if ("Mana Alert".equals(label)) {
            lines.add("Manaが指定割合以下になった時の警告を設定します。");
        } else if (label.startsWith("Soulbind Protection")) {
            lines.add("Soulbind中の危険な切断を確認画面で防ぎます。");
        } else if ("Chest Search".equals(label)) {
            lines.add("記録済みチェストから指定アイテムを探します。");
        } else if ("Drop Tracker".equals(label)) {
            lines.add("拾得アイテムと推定利益の記録を設定します。");
        } else if ("Exp Tracker".equals(label)) {
            lines.add("EXP獲得量と時間あたりEXPの記録を設定します。");
        } else if ("Element Tracker".equals(label)) {
            lines.add("エレメントの収集進捗と目標レアリティ到達までの予想時間を表示します。");
        } else if ("Evolution Forge Helper".equals(label)) {
            lines.add("\u30a8\u30dc\u30ea\u30e5\u30fc\u30b7\u30e7\u30f3\u30d5\u30a9\u30fc\u30b8\u7d20\u6750\u306eLore\u306bEvo?: Yes\u3092\u8ffd\u52a0\u8868\u793a\u3057\u307e\u3059\u3002");
            lines.add("フォージ画面を開いて固定ステータス範囲を読み取り、通常アイテムのツールチップをホバーして暫定ステータス範囲を学習します。");
        } else if ("Mob HP Display".equals(label)) {
            lines.add("クロスヘア先のMobのHPと残り割合を表示します。");
        } else if ("Sub Skill Timer".equals(label)) {
            lines.add("サブスキルの再使用時間を画面上に表示します。");
        } else if ("Ritual Book Timer".equals(label)) {
            lines.add("\u5100\u5f0f\u66f8\u7269\u8cfc\u5165\u30c1\u30e3\u30c3\u30c8\u3092\u691c\u77e5\u3057\u306610\u5206\u30bf\u30a4\u30de\u30fc\u3092\u8868\u793a\u3057\u307e\u3059\u3002");
        } else if ("Drop Notifier".equals(label)) {
            lines.add("指定したアイテムのドロップを検知したときに通知します。");
        } else if ("Chat Filter".equals(label)) {
            lines.add("指定した文字を含むチャット表示を非表示にします。");
        }
    }

    private static void addDangerousFeaturesTooltip(List<String> lines, String label) {
        if ("Item Macro".equals(label)) {
            lines.add("登録済みMacroの一覧を開きます。");
        } else if (label.startsWith("Item Macro Sync")) {
            lines.add("Item MacroがグローバルなMacro Toggleに従うかを切り替えます。");
            lines.add("OFFにするとItem MacroはMacro Toggleと独立して動きます。");
        } else if ("TriggerBot".equals(label)) {
            lines.add("HP 50000超えの対象に対してMacroを発動する設定画面を開きます。");
        }
    }

    private static void addTriggerBotTooltip(List<String> lines, String label) {
        if (label.startsWith("TriggerBot")) {
            lines.add("TriggerBot全体のON/OFFです。OFFにするとMacro発動を停止します。");
        } else if (label.startsWith("Macro:")) {
            lines.add("TriggerBotで発動する登録済みMacroを選びます。");
            lines.add("クリックでAFK Farmingと同じMacro一覧を順番に切り替えます。");
        } else if (label.startsWith("Cooldown Seconds")) {
            lines.add("対象のHPが50000を超えている間、何秒おきにMacroを再実行するかを設定します。");
        }
    }

    private static void addCameraTooltip(List<String> lines, String label) {
        if (label.startsWith("Change Camera Toggle Key") || label.startsWith("Press any key")) {
            lines.add("カメラ切替に使うキーを変更します。");
        }
    }

    private static void addChestSearchTooltip(List<String> lines, String label) {
        if (label.startsWith("Chest Search Status")) {
            lines.add("チェスト検索の有効/無効を切り替えます。");
        } else if (label.startsWith("Open Menu Key") || label.startsWith("Press any key")) {
            lines.add("Chest Search画面を直接開くキーを設定します。");
        } else if ("Clear Index".equals(label)) {
            lines.add("記録済みチェスト索引を確認後に削除します。");
        }
    }

    private static void addChatFilterTooltip(List<String> lines, String screenName, String label) {
        if (label.startsWith("Chat Filter Status")) {
            lines.add("チャットフィルターの有効/無効を切り替えます。");
        } else if ("Edit Filters".equals(label)) {
            lines.add("非表示にする文字列の一覧を編集します。");
        } else if ("Add New Filter".equals(label)) {
            lines.add("新しい非表示文字列を追加します。");
        } else if (label.startsWith("Enabled")) {
            lines.add("このフィルター項目の有効/無効を切り替えます。");
        } else if ("HaChatFilterManageScreen".equals(screenName) && isLikelyEntryButton(label)) {
            lines.add("このフィルター項目を編集します。");
        }
    }

    private static void addAlertTooltip(List<String> lines, String screenName, String label, String kind) {
        if (label.startsWith("Add ")) {
            lines.add(kind + "警告の新しい条件を追加します。");
        } else if (label.startsWith("Enabled")) {
            lines.add("この" + kind + "警告の有効/無効を切り替えます。");
        } else if (label.startsWith("Color")) {
            lines.add("警告タイトルに使う色を切り替えます。");
        } else if (isLikelyEntryButton(label) && screenName.endsWith("ListScreen")) {
            lines.add("この" + kind + "警告設定を編集します。");
        }
    }

    private static void addDropTrackerTooltip(List<String> lines, String screenName, String label) {
        if (label.startsWith("Drop Tracker")) {
            lines.add("Drop Trackerの有効/無効を切り替えます。");
        } else if (label.startsWith("Tracking Mode")) {
            lines.add("記録対象のアイテム範囲を切り替えます。");
        } else if ("Adjust HUD Position".equals(label)) {
            lines.add("Drop Tracker HUDの表示位置を調整します。");
        } else if ("Edit Registered Items".equals(label)) {
            lines.add("価格付きで記録する登録アイテムを編集します。");
        } else if (label.startsWith("Show Timer")) {
            lines.add("HUDに経過時間を表示するか切り替えます。");
        } else if (label.startsWith("Auto Stop")) {
            lines.add("ONならSoulbind終了で停止、OFFなら開始後も継続します。");
        } else if (label.startsWith("Show Profit/hour")) {
            lines.add("HUDに時間あたり推定利益を表示するか切り替えます。");
        } else if (label.startsWith("Compact Profit")) {
            lines.add("利益表示をk/m/b形式に省略するか切り替えます。");
        } else if ("Reset Counts".equals(label)) {
            lines.add("記録数、推定利益、タイマーをリセットします。");
        } else if ("HaDropTrackerRegisteredListScreen".equals(screenName) && isLikelyEntryButton(label)) {
            lines.add("この登録アイテムを編集します。");
        }
    }

    private static void addDropNotifierTooltip(List<String> lines, String screenName, String label) {
        if (label.startsWith("Drop Notifier")) {
            lines.add("Drop Notifierの有効/無効を切り替えます。");
        } else if (label.startsWith("Auto Stop")) {
            lines.add("ONならSoulbind終了で停止、OFFなら開始後も継続します。");
        } else if ("Edit Notifiers".equals(label)) {
            lines.add("通知するアイテム名の部分一致条件を編集します。");
        } else if ("Add New Item".equals(label)) {
            lines.add("新しい通知対象アイテム名を追加します。");
        } else if (label.startsWith("Enabled")) {
            lines.add("この通知対象の有効/無効を切り替えます。");
        } else if ("HaDropNotifierManageScreen".equals(screenName) && isLikelyEntryButton(label)) {
            lines.add("この通知対象を編集します。");
        }
    }

    private static void addExpTrackerTooltip(List<String> lines, String label) {
        if (label.startsWith("Exp Tracker")) {
            lines.add("Exp Trackerの有効/無効を切り替えます。");
        } else if ("Adjust Overlay Position".equals(label)) {
            lines.add("Exp Tracker HUDの表示位置を調整します。");
        } else if (label.startsWith("Show Timer")) {
            lines.add("HUDに経過時間を表示するか切り替えます。");
        } else if (label.startsWith("Auto Stop")) {
            lines.add("ONならSoulbind終了で停止、OFFなら開始後も継続します。");
        } else if (label.startsWith("Show EXP/hour")) {
            lines.add("HUDに時間あたりEXPを表示するか切り替えます。");
        } else if (label.startsWith("Compact XP")) {
            lines.add("EXP表示をk/m/b形式に省略するか切り替えます。");
        } else if ("Reset Total".equals(label)) {
            lines.add("合計EXPとタイマーをリセットします。");
        }
    }

    private static void addElementTrackerTooltip(List<String> lines, String screenName, String label) {
        if (label.startsWith("Element Tracker")) {
            lines.add("Element Trackerの有効/無効を切り替えます。");
        } else if ("Select Target Element".equals(label)) {
            lines.add("追跡するエレメントと目標レアリティを設定します。");
        } else if ("Adjust Overlay Position".equals(label)) {
            lines.add("Element Tracker HUDの表示位置を調整します。");
        } else if (label.startsWith("Show Timer")) {
            lines.add("HUDに経過時間を表示するか切り替えます。");
        } else if (label.startsWith("Auto Stop")) {
            lines.add("Soulbindでなくなった後も計測を続けるか切り替えます。");
        } else if ("Reset Data".equals(label)) {
            lines.add("収集済みのエレメント個数と経過時間をリセットします。");
        } else if ("HaElementTrackerTargetScreen".equals(screenName)
            && (label.contains("Legendary") || label.contains("Transcendent") || label.contains("Untouchable") || label.contains("Unique"))) {
            lines.add("このエレメントの目標レアリティを切り替えます。");
        } else if ("HaElementTrackerTargetScreen".equals(screenName) && (label.endsWith(": Enabled") || label.endsWith(": Disabled"))) {
            lines.add("このエレメントを追跡対象にするか切り替えます。");
        }
    }

    private static void addMobHpDisplayTooltip(List<String> lines, String label) {
        if (label.startsWith("Mob HP Display")) {
            lines.add("Mob HP Displayの有効/無効を切り替えます。");
        } else if (label.startsWith("Position")) {
            lines.add("HUD固定表示かクロスヘア付近表示かを切り替えます。");
        } else if (label.startsWith("Display")) {
            lines.add("Full表示とSlim表示を切り替えます。");
        } else if (label.startsWith("Show Percentage")) {
            lines.add("HPの割合表示を出すか切り替えます。");
        } else if (label.startsWith("Compact HP")) {
            lines.add("大きなHPをk/m/b形式で短く表示します。");
        } else if ("Adjust Overlay Position".equals(label)) {
            lines.add("Mob HP Display HUDの表示位置を調整します。");
        }
    }

    private static void addSubSkillTimerTooltip(List<String> lines, String label) {
        if (label.startsWith("Sub Skill Timer")) {
            lines.add("Sub Skill Timerの有効/無効を切り替えます。");
        } else if (label.startsWith("Display")) {
            lines.add("オンスクリーン表示の見た目を切り替えます。");
        } else if (label.startsWith("Cooldown Seconds")) {
            lines.add("クールダウン秒数の表示/非表示を切り替えます。");
        } else if ("Adjust Overlay Position".equals(label)) {
            lines.add("Sub Skill Timer HUDの表示位置を調整します。");
        }
    }

    private static void addAfkFarmingTooltip(List<String> lines, String label) {
        if (label.startsWith("機能")) {
            lines.add("AFK Farming全体のON/OFFです。OFFにすると監視も停止します。");
        } else if ("監視開始".equals(label) || "監視停止".equals(label)) {
            lines.add("プレイヤー検知、Admin検知、定期Discord通知、Mob発動を開始/停止します。");
        } else if (label.startsWith("他プレイヤー")) {
            lines.add("描画範囲内に他プレイヤーがいたらゲーム内とDiscordに通知します。");
        } else if (label.startsWith("Admin検知")) {
            lines.add("Tabリストに指定Admin名がいたらゲーム内とDiscordに通知します。");
        } else if (label.startsWith("Living発動")) {
            lines.add("前方3ブロック地点の周囲3ブロックのLivingEntity数が条件以上なら、選択Macroを実行します。");
        } else if (label.startsWith("発動Macro")) {
            lines.add("Living発動時に使うItem Macroを選びます。クリックで登録済みMacroを順番に切り替えます。");
        } else if (label.startsWith("判定サークル")) {
            lines.add("LivingEntity数を数える水平サークルをワールド上に表示します。円の内側だけを数えます。");
        } else if (label.startsWith("テストHUD")) {
            lines.add("現在のLivingEntity数、発動しきい値、再発動CDをHUDに表示します。");
        }
    }

    private static void addRitualBookTimerTooltip(List<String> lines, String label) {
        if (label.startsWith("Ritual Book Timer")) {
            lines.add("\u5100\u5f0f\u66f8\u7269\u30bf\u30a4\u30de\u30fc\u306e\u6709\u52b9/\u7121\u52b9\u3092\u5207\u308a\u66ff\u3048\u307e\u3059\u3002");
        } else if (label.startsWith("Display")) {
            lines.add("Slim\u8868\u793a\u3068Full\u8868\u793a\u3092\u5207\u308a\u66ff\u3048\u307e\u3059\u3002");
        } else if ("Adjust Overlay Position".equals(label)) {
            lines.add("\u5100\u5f0f\u66f8\u7269\u30bf\u30a4\u30de\u30fcHUD\u306e\u8868\u793a\u4f4d\u7f6e\u3092\u8abf\u6574\u3057\u307e\u3059\u3002");
        }
    }

    private static boolean isLikelyEntryButton(String label) {
        return !label.isEmpty()
            && !label.equals("Save")
            && !label.equals("Delete")
            && !label.equals("Go Back")
            && !label.equals("< Back")
            && !label.equals("Next >")
            && !label.equals("Done");
    }

    private static String normalize(String value) {
        String stripped = Formatting.strip(value);
        if (stripped == null) {
            stripped = value;
        }
        return stripped.trim();
    }

    private static void addMacroListTooltip(List<String> lines, String label) {
        if ("Add macro".equals(label)) {
            lines.add("新しいmacroを追加します。");
        } else if (isLikelyEntryButton(label)) {
            lines.add("このmacroの編集画面を開きます。");
        }
    }

    private static void addMacroEditTooltip(List<String> lines, String label) {
        if (label.startsWith("Enabled")) {
            lines.add("Item Macroでこのmacroを使うか切り替えます。");
        } else if (label.startsWith("Slot")) {
            lines.add("このmacroが押すホットバー枠を選びます。");
        } else if (label.startsWith("Add")) {
            lines.add("新しいmacroを保存します。");
        } else if (isLikelyEntryButton(label)) {
            lines.add("このmacroの設定を編集します。");
        }
    }
    private static void addMacroListTooltipV2(List<String> lines, String label) {
        if ("Add macro".equals(label)) {
            lines.add("新しいmacroを追加します。");
        } else if (isLikelyEntryButton(label)) {
            lines.add("このmacroの編集画面を開きます。");
        }
    }

    private static void addMacroEditTooltipV2(List<String> lines, String label) {
        if (label.startsWith("Enabled")) {
            lines.add("Item Macroでこのmacroを使うか切り替えます。");
            lines.add("動作するにはMacro ToggleとAuto Healが両方Enabledである必要があります。");
        } else if (label.startsWith("Slot")) {
            lines.add("このmacroが押すホットバー枠を選びます。");
        } else if (label.startsWith("Add")) {
            lines.add("新しいmacroを保存します。");
        } else if (isLikelyEntryButton(label)) {
            lines.add("このmacroの設定を編集します。");
        }
    }
}
