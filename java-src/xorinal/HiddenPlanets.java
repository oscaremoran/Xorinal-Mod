package xorinal;

import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Planet;

public class HiddenPlanets {
    private static final String[] HIDE = {"gier", "verilus", "notva", "tantros"};

    public static void init() {
        try {
            Seq<Planet> hidden = new Seq<>();
            for (String name : HIDE) {
                Planet p = Vars.content.getByName(ContentType.planet, name);
                if (p == null) continue;
                p.accessible = false;
                p.visible = false;
                hidden.add(p);
            }
            if (hidden.isEmpty()) {
                Log.info("[Xorinal] HiddenPlanets: no vanilla planets to hide");
                return;
            }

            int scrubbed = 0;
            for (Seq<Content> seq : Vars.content.getContentMap()) {
                for (Content c : seq) {
                    if (!(c instanceof UnlockableContent)) continue;
                    UnlockableContent uc = (UnlockableContent) c;
                    if (uc.databaseTabs == null) continue;
                    for (Planet p : hidden) {
                        if (uc.databaseTabs.remove(p)) scrubbed++;
                    }
                }
            }
            Log.info("[Xorinal] HiddenPlanets: hid " + hidden.size + " planets, scrubbed " + scrubbed + " tab refs");
        } catch (Exception e) {
            Log.err("[Xorinal] HiddenPlanets.init: " + e);
        }
    }
}
