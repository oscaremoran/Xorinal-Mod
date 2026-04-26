package xorinal;

import arc.Core;
import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Groups;
import mindustry.ui.Styles;

public class MenuUI {
    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static void setup() {
        try {
            Table wrap = new Table();
            wrap.setFillParent(true);
            wrap.top().right();

            Table inner = new Table();
            var region = Core.atlas.find("xorinal-planet-icon");
            inner.image(region).size(250f, 250f).pad(4f);
            inner.row();
            Label lbl = inner.add("[#2eff78]XORINAL[]").get();
            try { lbl.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            inner.row();

            wrap.add(inner).pad(20f);
            Vars.ui.menuGroup.addChild(wrap);
        } catch (Exception ex) {
            Log.err("[Xorinal] menu icon failed: " + ex);
        }

        try {
            Table t = new Table();
            t.setFillParent(true);
            t.top().right();
            t.button("Win Sector", () -> {
                if (!Vars.state.isGame() || Vars.state.rules.sector == null) return;
                Groups.unit.each(u -> { if (u.team != Vars.player.team()) u.kill(); });
                if (Vars.state.rules.winWave > 0) {
                    Vars.state.wave = Math.max(Vars.state.wave, Vars.state.rules.winWave + 1);
                }
            }).size(140f, 40f).pad(8f);
            Vars.ui.hudGroup.addChild(t);
        } catch (Exception ex) {
            Log.err("[Xorinal] win-sector button failed: " + ex);
        }
    }
}
