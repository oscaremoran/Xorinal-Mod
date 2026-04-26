package xorinal;

import arc.util.Log;
import mindustry.mod.Mod;

public class XorinalMod extends Mod {
    public XorinalMod() {
        Log.info("[Xorinal] Java mod constructor loaded");
    }

    @Override
    public void init() {
        Log.info("[Xorinal] Java mod init() — registering subsystems");
        try { InfectorDrain.init(); } catch (Exception e) { Log.err("[Xorinal] InfectorDrain.init: " + e); }
        try { LichenSystem.init(); } catch (Exception e) { Log.err("[Xorinal] LichenSystem.init: " + e); }
        try { PlanetMeshes.init(); } catch (Exception e) { Log.err("[Xorinal] PlanetMeshes.init: " + e); }
        try { MenuUI.init(); } catch (Exception e) { Log.err("[Xorinal] MenuUI.init: " + e); }
        try { Codex.init(); } catch (Exception e) { Log.err("[Xorinal] Codex.init: " + e); }
        Log.info("[Xorinal] Java mod init() complete");
    }
}
