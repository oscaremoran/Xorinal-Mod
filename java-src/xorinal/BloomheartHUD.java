package xorinal;

import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Unit;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

public class BloomheartHUD {
    private static final Color PURPLE_FILL = Color.valueOf("c04fff");
    private static final Color PURPLE_OUTLINE = Color.valueOf("4a1670");

    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static void setup() {
        try {
            Table root = new Table();
            root.setFillParent(true);
            root.top();
            root.marginTop(64f);

            Table card = new Table();
            try { card.setBackground(Styles.black6); } catch (Exception ex) {}
            card.margin(10f);

            Label title = card.add("[#c04fff]BLOOMHEART LIFE[]").get();
            try { title.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            title.setFontScale(1.4f);
            card.row();

            Bar bar = new Bar("", PURPLE_FILL, () -> {
                Unit b = Bloomheart.currentBoss();
                float max = Bloomheart.currentMaxHp();
                if (b == null || max <= 0f) return 0f;
                return Math.max(0f, Math.min(1f, b.health / max));
            });
            try { bar.outline(PURPLE_OUTLINE, 2.4f); } catch (Exception ex) {}
            card.add(bar).size(520f, 28f).padTop(6f);

            root.add(card);
            root.visible(() -> Bloomheart.currentBoss() != null);

            Vars.ui.hudGroup.addChild(root);
            Log.info("[Xorinal] Bloomheart HUD installed");
        } catch (Exception ex) {
            Log.err("[Xorinal] BloomheartHUD: " + ex);
        }
    }
}
