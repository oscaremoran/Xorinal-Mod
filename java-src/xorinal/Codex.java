package xorinal;

import arc.Core;
import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.SectorCaptureEvent;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

public class Codex {
    private static class Entry {
        final String id, title, section, difficulty, unlockHint, body;
        Entry(String id, String title, String section, String difficulty, String unlockHint, String body) {
            this.id = id; this.title = title; this.section = section;
            this.difficulty = difficulty; this.unlockHint = unlockHint; this.body = body;
        }
    }

    private static final Seq<Entry> entries = Seq.with(
        new Entry("sporocyst-awakening", "Sporocyst Awakening", "Arrival", "Easy",
            "Land on Crash Site.",
            "Your dropship is a wreck, but the Sporocyst survived. Already its tendrils are threading into the soil, drinking the jungle's warmth. It is not a machine. It is alive, and it is hungry to grow."),
        new Entry("spore-canopy", "The Spore Canopy", "Xenobiology", "Easy",
            "Defeat a spore creature on Crash Site.",
            "The jungle is not empty. Pale, twitching things drift between the trees — spore creatures, drawn to heat and motion. They are not intelligent, but they are many, and they remember where the warmth is. The Sporocyst hums whenever they die nearby. You try not to think about why."),
        new Entry("wreckage-and-ritual", "Older Than Iron", "Mysteries", "Medium",
            "Capture Crash Site (survive wave 10).",
            "The first ten waves are over. The jungle is quieter now, but not silent. Among the spore husks you find scraps that were not yours: alloy fragments, scorched glyphs, a half-buried marker pointing inland. Something was here before your ship. Something that did not crash."),
        new Entry("first-bloom", "First Bloom", "Xenobiology", "Easy",
            "Build a Bloom Wall.",
            "The wall pulses faintly under your hand. It is part fungus, part scar tissue, part something the survey team did not have words for. When struck, it bleeds spores instead of dust — and the bleeding makes it stronger."),
        new Entry("lichen-tide", "The Lichen Tide", "Ecology", "Medium",
            "Let lichen spread across 3 tiles.",
            "Your generators leak warmth. The lichen drinks it. Once it has rooted, it will not yield to bullets, to time, or to your apologies — only to fire. You begin to understand why the Sporocyst is so patient."),
        new Entry("crux-vanguard", "Crux Vanguard", "Hostiles", "Medium",
            "Defeat an enemy on Spore Production Facility.",
            "They came in light skirmishers, testing the perimeter. The wreckage tells a story: outdated alloys, scavenged plasma cores, hand-welded armor plates. This was a probe, not an assault."),
        new Entry("the-drain", "The Drain", "Hostiles", "Hard",
            "Defeat an Infector on Crux Spaceport Outskirts.",
            "It drained your salvagers dry before anyone knew what it was. The Infector hunts at the edges of your formations, drinking warmth and venting it back as violet thunder. Whatever directs the Crux knew exactly which of your units to bleed first."),
        new Entry("spaceport-down", "Spaceport Down", "Mysteries", "Hard",
            "Capture Crux Spaceport.",
            "The marker you found at the crash site pointed inland. Inland led here. The glyphs on the spaceport gates match the glyphs on the marker — older than the Crux, older than their alloys. They were never speaking to the Crux. They were speaking to whoever the Crux replaced."),
        new Entry("the-bloomheart", "The Bloomheart", "Mysteries", "Hard",
            "Capture Bloomheart Hollow.",
            "The glade was quiet when you arrived, in the way a held breath is quiet. At its center, the Bloomheart — older than the Crux, older than the glyphs at the spaceport, older than any name still spoken in your language. The Crux had ringed it with watchposts, not to guard it, but to keep it from waking. You woke it anyway. When the last petal fell and the bloom opened, the air pulsed once, twice, and then the Sporocyst answered from across half the planet. Whatever speaks through Xorinal has finally heard you speaking back. You are not sure whether it is pleased."),
        new Entry("foothold", "Foothold", "Milestones", "Hard",
            "Capture 5 sectors on Xorinal or Tetra.",
            "Five sectors held. Five different patches of dirt where the Sporocyst has rooted and refused to leave. The colony is no longer a wreck — it is a pattern, and patterns attract attention.")
    );

    private static final String KEY = "xorinal-codex-";
    private static final String READ_KEY = "xorinal-codex-read-";

    private static BaseDialog dialog;
    private static Label btnLabel;

    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
        Events.on(WorldLoadEvent.class, e -> {
            if (currentSectorIs("xorinal-xorinal", 0)) unlock("sporocyst-awakening");
        });
        Events.on(UnitDestroyEvent.class, e -> {
            try {
                if (!Vars.state.isGame()) return;
                if (e.unit == null) return;
                if (e.unit.team() == Vars.player.team()) return;
                if (currentSectorIs("xorinal-xorinal", 0)) unlock("spore-canopy");
                if (currentSectorIs("xorinal-xorinal", 4)) unlock("crux-vanguard");
                if (currentSectorIs("xorinal-xorinal", 20)) {
                    if (e.unit.type != null && "xorinal-infector".equals(e.unit.type.name)) {
                        unlock("the-drain");
                    }
                }
            } catch (Exception ex) {}
        });
        Events.on(SectorCaptureEvent.class, e -> {
            try {
                if (e.sector == null || e.sector.planet == null) return;
                String pn = e.sector.planet.name;
                if (!"xorinal-xorinal".equals(pn) && !"xorinal-tetra".equals(pn)) return;
                if ("xorinal-xorinal".equals(pn) && e.sector.id == 0) unlock("wreckage-and-ritual");
                if ("xorinal-xorinal".equals(pn) && e.sector.id == 21) unlock("spaceport-down");
                if ("xorinal-xorinal".equals(pn) && e.sector.id == 22) unlock("the-bloomheart");
                String dedupeKey = "xorinal-codex-cap-" + pn + "-" + e.sector.id;
                if (!Core.settings.getBool(dedupeKey, false)) {
                    Core.settings.put(dedupeKey, true);
                    int cur = Core.settings.getInt("xorinal-codex-cap-count", 0) + 1;
                    Core.settings.put("xorinal-codex-cap-count", cur);
                    if (cur >= 5) unlock("foothold");
                }
            } catch (Exception ex) {}
        });
        Events.on(BlockBuildEndEvent.class, e -> {
            try {
                if (e.breaking) return;
                if (e.tile == null || e.tile.block() == null) return;
                String n = e.tile.block().name;
                if ("xorinal-bloom-wall".equals(n) || "xorinal-bloom-wall-large".equals(n)) {
                    unlock("first-bloom");
                }
            } catch (Exception ex) {}
        });
    }

    private static boolean currentSectorIs(String planetName, int id) {
        try {
            var s = Vars.state.rules.sector;
            if (s == null || s.planet == null) return false;
            return planetName.equals(s.planet.name) && s.id == id;
        } catch (Exception ex) { return false; }
    }

    private static boolean unlocked(String id) { return Core.settings.getBool(KEY + id, false); }
    private static boolean read(String id) { return Core.settings.getBool(READ_KEY + id, false); }

    private static int unreadCount() {
        int n = 0;
        for (Entry e : entries) if (unlocked(e.id) && !read(e.id)) n++;
        return n;
    }

    private static String btnText() {
        int n = unreadCount();
        return n > 0 ? "Codex [scarlet]•[]" : "Codex";
    }

    private static void refreshBtn() {
        try { if (btnLabel != null) btnLabel.setText(btnText()); } catch (Exception ex) {}
    }

    public static void unlock(String id) {
        if (unlocked(id)) return;
        Entry entry = null;
        for (Entry e : entries) if (e.id.equals(id)) { entry = e; break; }
        if (entry == null) return;
        Core.settings.put(KEY + id, true);
        Core.settings.put(READ_KEY + id, false);
        try { Vars.ui.showInfoToast("[#2eff78]New codex entry:[] " + entry.title, 4f); } catch (Exception ex) {
            try { Vars.ui.showInfoFade("New codex entry: " + entry.title); } catch (Exception ex2) {}
        }
        refreshBtn();
    }

    private static void markAllRead() {
        for (Entry e : entries) if (unlocked(e.id)) Core.settings.put(READ_KEY + e.id, true);
        refreshBtn();
    }

    private static void clearAll() {
        for (Entry e : entries) {
            Core.settings.put(KEY + e.id, false);
            Core.settings.put(READ_KEY + e.id, false);
        }
        Core.settings.put("xorinal-codex-cap-count", 0);
        Core.settings.put("xorinal-codex-lichen-spread", 0);
        refreshBtn();
    }

    private static void rebuildContent() {
        dialog.cont.clear();
        Table inner = new Table();
        inner.top().left().defaults().left().pad(6);
        for (Entry e : entries) {
            boolean u = unlocked(e.id);
            Table card = new Table();
            try { card.background(Tex.button); } catch (Exception ex) {}
            card.defaults().left().pad(4);
            if (u) {
                card.add("[#2eff78]" + e.title + "[]   [lightgray](" + e.section + " · " + e.difficulty + ")[]").left();
                card.row();
                Label lbl = new Label(e.body);
                lbl.setWrap(true);
                card.add(lbl).width(620f).left();
            } else {
                card.add("[gray]??? · " + e.section + " · " + e.difficulty + "[]").left();
                card.row();
                card.add("[lightgray]Locked. " + e.unlockHint + "[]").left();
            }
            inner.add(card).growX().pad(8);
            inner.row();
        }
        ScrollPane sp = new ScrollPane(inner);
        dialog.cont.add(sp).grow().minWidth(680f).minHeight(440f);
    }

    private static void buildDialog() {
        dialog = new BaseDialog("[#2eff78]Xorinal Codex[]");
        dialog.addCloseButton();
        rebuildContent();
        dialog.shown(Codex::rebuildContent);
        dialog.hidden(Codex::markAllRead);
    }

    private static void setup() {
        try {
            Log.info("[Xorinal] codex setup starting");
            buildDialog();

            Table hud = new Table();
            hud.setFillParent(true);
            hud.top().right();
            TextButton tb = hud.button(btnText(), () -> {
                try { dialog.show(); } catch (Exception ex) { Log.err("[Xorinal] codex show: " + ex); }
            }).size(140f, 40f).padTop(8f).padRight(180f).get();
            btnLabel = tb.getLabel();
            Vars.ui.hudGroup.addChild(hud);
            refreshBtn();
            Log.info("[Xorinal] codex HUD button added");

            try {
                var game = Vars.ui.settings.game;
                if (game != null) {
                    game.row();
                    game.button("Clear Xorinal Codex", () -> {
                        try {
                            Vars.ui.showConfirm("Clear all Xorinal codex entries? This cannot be undone.", () -> {
                                clearAll();
                                try { Vars.ui.showInfoFade("Xorinal codex cleared"); } catch (Exception ex) {}
                            });
                        } catch (Exception ex) {
                            clearAll();
                            try { Vars.ui.showInfoFade("Xorinal codex cleared"); } catch (Exception ex2) {}
                        }
                    }).size(260f, 50f).padTop(10f);
                    Log.info("[Xorinal] codex clear button added to settings game tab");
                }
            } catch (Exception ex) { Log.err("[Xorinal] settings codex clear setup: " + ex); }
        } catch (Exception ex) {
            Log.err("[Xorinal] codex setup: " + ex);
        }
    }

    public static void notifyLichenSpread() {
        try {
            int cur = Core.settings.getInt("xorinal-codex-lichen-spread", 0) + 1;
            Core.settings.put("xorinal-codex-lichen-spread", cur);
            if (cur >= 3) unlock("lichen-tide");
        } catch (Exception ex) {}
    }
}
