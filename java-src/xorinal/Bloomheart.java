package xorinal;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.entities.Damage;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class Bloomheart {
    private static final String SECTOR_PLANET = "xorinal-xorinal";
    private static final int SECTOR_ID = 22;
    public static final String BOSS_UNIT = "xorinal-bloomheart";

    private static final float SPAWN_INTERVAL_TICKS = 60f * 12f;
    private static final float PULSE_INTERVAL_TICKS = 60f * 8f;
    private static final float PULSE_RADIUS = 220f;
    private static final float PULSE_DAMAGE = 320f;
    private static final float PULSE_INSTAKILL_RADIUS = 110f;
    private static final float PULSE_CLOUD_RADIUS = 240f;
    private static final float PULSE_CLOUD_TTL_SEC = 7f;
    private static final float PHASE2_HP_FRACTION = 0.5f;
    private static final int MAX_BOSS_UNITS = 12;

    private static UnitType infectorType, blasterType, overmindType;
    static UnitType bossType;

    private static boolean armed;
    private static int phase;
    private static float spawnTimer;
    private static float pulseTimer;
    private static float maxBossHp;
    private static Unit boss;
    private static boolean autoSpawnedThisWorld;

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> resetState());
        Events.run(Trigger.update, Bloomheart::tick);
    }

    public static Unit currentBoss() {
        return (boss != null && !boss.dead) ? boss : null;
    }

    public static float currentMaxHp() {
        return maxBossHp;
    }

    private static void resetState() {
        armed = false;
        phase = 0;
        spawnTimer = 0f;
        pulseTimer = 0f;
        maxBossHp = 0f;
        boss = null;
        autoSpawnedThisWorld = false;
    }

    private static void tick() {
        if (!Vars.state.isGame() || Vars.state.isPaused()) return;

        if (bossType == null) bossType = (UnitType) Vars.content.getByName(ContentType.unit, BOSS_UNIT);
        if (infectorType == null) infectorType = (UnitType) Vars.content.getByName(ContentType.unit, "xorinal-infector");
        if (blasterType == null) blasterType = (UnitType) Vars.content.getByName(ContentType.unit, "xorinal-blaster");
        if (overmindType == null) overmindType = (UnitType) Vars.content.getByName(ContentType.unit, "xorinal-overmind");
        if (bossType == null) return;

        if (boss == null || boss.dead) boss = findBoss();

        if (boss == null && !autoSpawnedThisWorld && isBloomheartHollow()) {
            boss = spawnAtEnemyCore();
            autoSpawnedThisWorld = true;
        }

        if (boss == null) {
            armed = false;
            return;
        }

        if (!armed) {
            armed = true;
            phase = 1;
            spawnTimer = 0f;
            pulseTimer = 0f;
            maxBossHp = boss.maxHealth;
            Log.info("[Xorinal] Bloomheart encounter armed (HP " + maxBossHp + ")");
        }
        if (maxBossHp <= 0f) maxBossHp = boss.maxHealth;

        if (phase == 1 && boss.health <= maxBossHp * PHASE2_HP_FRACTION) {
            phase = 2;
            pulseTimer = 0f;
            Log.info("[Xorinal] Bloomheart phase 2 (HP " + boss.health + "/" + maxBossHp + ")");
            announceOpens();
            pulse(boss);
        }

        if (phase == 1) {
            spawnTimer += Time.delta;
            if (spawnTimer >= SPAWN_INTERVAL_TICKS) {
                spawnTimer = 0f;
                spawnWave(boss, 2, 1);
            }
        } else {
            spawnTimer += Time.delta;
            if (spawnTimer >= SPAWN_INTERVAL_TICKS) {
                spawnTimer = 0f;
                spawnWave(boss, 1, 2);
            }

            pulseTimer += Time.delta;
            if (pulseTimer >= PULSE_INTERVAL_TICKS) {
                pulseTimer = 0f;
                pulse(boss);
                if (overmindType != null) spawnUnit(overmindType, boss.team(), boss.x, boss.y);
            }
        }
    }

    private static boolean isBloomheartHollow() {
        try {
            var s = Vars.state.rules.sector;
            return s != null && s.planet != null
                && SECTOR_PLANET.equals(s.planet.name) && s.id == SECTOR_ID;
        } catch (Exception ex) {
            return false;
        }
    }

    private static Unit findBoss() {
        for (Unit u : Groups.unit) {
            if (!u.dead && u.type == bossType) return u;
        }
        return null;
    }

    private static Unit spawnAtEnemyCore() {
        try {
            Building core = enemyCore();
            if (core == null) return null;
            Unit u = bossType.create(core.team);
            u.set(core.x, core.y);
            u.add();
            try { Fx.spawn.at(core.x, core.y); } catch (Exception ex) {}
            Log.info("[Xorinal] Bloomheart auto-spawned at enemy core");
            return u;
        } catch (Exception ex) {
            Log.err("[Xorinal] Bloomheart spawn boss: " + ex);
            return null;
        }
    }

    private static Building enemyCore() {
        try {
            for (Team t : Team.all) {
                if (t == Vars.player.team()) continue;
                var cores = t.cores();
                if (cores != null && cores.size > 0) return cores.first();
            }
        } catch (Exception ex) {}
        return null;
    }

    private static int countMinionUnits() {
        int n = 0;
        for (Unit u : Groups.unit) {
            if (u.dead) continue;
            if (u.type == infectorType || u.type == blasterType || u.type == overmindType) n++;
        }
        return n;
    }

    private static void spawnWave(Unit anchor, int infectors, int blasters) {
        if (countMinionUnits() >= MAX_BOSS_UNITS) return;
        if (infectorType != null) {
            for (int i = 0; i < infectors; i++) spawnUnit(infectorType, anchor.team(), anchor.x, anchor.y);
        }
        if (blasterType != null) {
            for (int i = 0; i < blasters; i++) spawnUnit(blasterType, anchor.team(), anchor.x, anchor.y);
        }
    }

    private static void spawnUnit(UnitType type, Team team, float ax, float ay) {
        try {
            float angle = Mathf.random(360f);
            float dist = Mathf.random(40f, 90f);
            float x = ax + Mathf.cosDeg(angle) * dist;
            float y = ay + Mathf.sinDeg(angle) * dist;
            Unit u = type.create(team);
            u.set(x, y);
            u.add();
            try { Fx.spawn.at(x, y); } catch (Exception ex) {}
        } catch (Exception ex) {
            Log.err("[Xorinal] Bloomheart spawn: " + ex);
        }
    }

    private static void pulse(Unit anchor) {
        try {
            float r2 = PULSE_INSTAKILL_RADIUS * PULSE_INSTAKILL_RADIUS;
            Team friendly = Vars.player.team();
            for (Unit u : Groups.unit) {
                if (u.dead || u.team == anchor.team()) continue;
                float dx = u.x - anchor.x, dy = u.y - anchor.y;
                if (dx * dx + dy * dy <= r2) u.kill();
            }
            Damage.damage(friendly, anchor.x, anchor.y, PULSE_RADIUS, PULSE_DAMAGE);
            Fx.shockwave.at(anchor.x, anchor.y, 0f, Color.valueOf("ff5577"));
            Fx.dynamicExplosion.at(anchor.x, anchor.y);
            try {
                SporeClouds.spawnBigCloud(anchor.x, anchor.y, anchor.team(), PULSE_CLOUD_RADIUS, PULSE_CLOUD_TTL_SEC);
            } catch (Exception ex) { Log.err("[Xorinal] pulse cloud: " + ex); }
            Log.info("[Xorinal] Bloomheart pulse fired");
        } catch (Exception ex) {
            Log.err("[Xorinal] Bloomheart pulse: " + ex);
        }
    }

    private static void announceOpens() {
        String msg = "[#ff5577]The Bloomheart opens.[]";
        try { Vars.ui.announce(msg, 4f); } catch (Exception ex) {}
        try { Vars.ui.showInfoToast(msg, 4f); } catch (Exception ex) {}
    }
}
