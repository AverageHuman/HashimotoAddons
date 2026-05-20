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
            && !(screen instanceof HaMacroListScreen)
            && !(screen instanceof HaMacroEditScreen)
            && !(screen instanceof HaMacroStatusOverlayScreen)
            && !(screen instanceof HaChunkChestScreen)
            && !(screen instanceof HaChunkChestOverlayScreen)
            && !(screen instanceof HaExtrasScreen)
            && !(screen instanceof HaExtrasOverlayScreen)
            && !(screen instanceof HaBlockGalleryScreen)
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
        if ("HaConfigScreen".equals(screenName)) {
            addMainMenuTooltip(lines, label);
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
        } else if ("HaExpTrackerScreen".equals(screenName)
            || "HaExpTrackerOverlayScreen".equals(screenName)) {
            addExpTrackerTooltip(lines, label);
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
        } else if ("Chat Filter".equals(label)) {
            lines.add("指定した文字を含むチャット表示を非表示にします。");
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
}
