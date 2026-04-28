package xorinal;

import arc.util.Log;
import mindustry.mod.Mod;

public class XorinalMod extends Mod {
    public XorinalMod() {
        Log.info("[Xorinal] Java mod constructor loaded");
    }

    @Override
    public void loadContent() {
        Log.info("[Xorinal] Java mod loadContent() — registering programmatic content");
        try { mindustry.world.meta.Attribute.add("crystal"); } catch (Exception e) { Log.err("[Xorinal] Attribute.add(crystal): " + e); }
        try { SporeClouds.registerContent(); } catch (Exception e) { Log.err("[Xorinal] SporeClouds.registerContent: " + e); }
    }

    @Override
    public void init() {
        Log.info("[Xorinal] Java mod init() — registering subsystems");
        try { InfectorDrain.init(); } catch (Exception e) { Log.err("[Xorinal] InfectorDrain.init: " + e); }
        try { LichenSystem.init(); } catch (Exception e) { Log.err("[Xorinal] LichenSystem.init: " + e); }
        try { PlanetMeshes.init(); } catch (Exception e) { Log.err("[Xorinal] PlanetMeshes.init: " + e); }
        try { MenuUI.init(); } catch (Exception e) { Log.err("[Xorinal] MenuUI.init: " + e); }
        try { Codex.init(); } catch (Exception e) { Log.err("[Xorinal] Codex.init: " + e); }
        try { Bloomheart.init(); } catch (Exception e) { Log.err("[Xorinal] Bloomheart.init: " + e); }
        try { SporeClouds.init(); } catch (Exception e) { Log.err("[Xorinal] SporeClouds.init: " + e); }
        try { BloomheartHUD.init(); } catch (Exception e) { Log.err("[Xorinal] BloomheartHUD.init: " + e); }
        try { HiddenPlanets.init(); } catch (Exception e) { Log.err("[Xorinal] HiddenPlanets.init: " + e); }
        Log.info("[Xorinal] Java mod init() complete");
    }
}
