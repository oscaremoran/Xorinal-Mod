package xorinal;

import arc.Events;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.IntSet;
import arc.struct.IntFloatMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.power.PowerGenerator;

public class LichenSystem {
    private static final String LICHEN_NAME = "xorinal-mycelium-lichen";
    private static final String FLAME_NAME = "xorinal-flamethrower";
    private static final String FLAME_ARRAY_NAME = "xorinal-flame-array";
    private static final String SEAR_NAME = "xorinal-sear";

    private static final float SPREAD_SECONDS = 5f;
    private static final float BURN_TICKS = 300f;
    private static final float FLAME_RANGE = 70f;
    private static final float FLAME_ARRAY_RANGE = 130f;
    private static final float SEAR_RANGE = 80f;
    private static final int SPREAD_SCAN_RADIUS = 30;

    private static Floor lichenFloor;
    private static Block flameBlock, flameArrayBlock;
    private static UnitType searType;

    private static final IntMap<Floor> tileOriginalFloor = new IntMap<>();
    private static final IntFloatMap producerTimer = new IntFloatMap();
    private static final IntFloatMap burnTimer = new IntFloatMap();

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> purgeStrayLichen());
        Events.run(Trigger.update, LichenSystem::tick);
    }

    private static int tileKey(int x, int y, int W) { return y * W + x; }

    private static boolean isLichenPlanet() {
        try {
            var r = Vars.state.rules;
            var s = r.sector;
            if (s != null && s.planet != null) {
                String n = s.planet.name;
                return "xorinal-xorinal".equals(n) || "xorinal-tetra".equals(n);
            }
            if (r.planet != null) {
                String n = r.planet.name;
                return "xorinal-xorinal".equals(n) || "xorinal-tetra".equals(n);
            }
            return true;
        } catch (Exception ex) { return true; }
    }

    private static Floor resolveLichen() {
        if (lichenFloor == null) {
            Block b = Vars.content.getByName(ContentType.block, LICHEN_NAME);
            if (b instanceof Floor) lichenFloor = (Floor) b;
        }
        return lichenFloor;
    }

    private static void purgeStrayLichen() {
        try {
            var r = Vars.state.rules;
            var s = r.sector;
            String sName = (s != null && s.planet != null) ? s.planet.name : "(no-sector)";
            String pName = r.planet != null ? r.planet.name : "(no-rules-planet)";
            Log.info("[Xorinal] WorldLoad: sector-planet=" + sName + " rules-planet=" + pName + " lichenPlanet=" + isLichenPlanet());

            if (s == null || s.planet == null) return;
            String n = s.planet.name;
            if ("xorinal-xorinal".equals(n) || "xorinal-tetra".equals(n)) return;

            Block lichen = Vars.content.getByName(ContentType.block, LICHEN_NAME);
            if (lichen == null) return;
            int W = Vars.world.width(), H = Vars.world.height();
            int purged = 0;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    Tile t = Vars.world.tile(x, y);
                    if (t != null && t.floor() == lichen) {
                        t.setFloor((Floor) Blocks.stone);
                        purged++;
                    }
                }
            }
            if (purged > 0) Log.info("[Xorinal] Purged " + purged + " stray lichen tiles on " + n);
        } catch (Exception ex) { Log.err("lichen purge: " + ex); }
    }

    private static void tick() {
        if (!Vars.state.isGame()) return;
        Floor lichen = resolveLichen();
        if (lichen == null) return;
        if (flameBlock == null) flameBlock = Vars.content.getByName(ContentType.block, FLAME_NAME);
        if (flameArrayBlock == null) flameArrayBlock = Vars.content.getByName(ContentType.block, FLAME_ARRAY_NAME);
        if (searType == null) searType = (UnitType) Vars.content.getByName(ContentType.unit, SEAR_NAME);

        final int W = Vars.world.width();
        final int H = Vars.world.height();
        final var playerTeam = Vars.player.team();
        final float dtSec = Time.delta / 60f;
        final boolean onLichenPlanet = isLichenPlanet();

        if (onLichenPlanet) {
            Groups.build.each(b -> {
                if (b.team != playerTeam) return;
                if (b.block == null || !b.block.outputsPower) return;
                float curProd = 0f, maxProd = 0f;
                try { curProd = b.getPowerProduction(); } catch (Exception ex) {}
                try {
                    if (b.block instanceof PowerGenerator) maxProd = ((PowerGenerator) b.block).powerProduction;
                } catch (Exception ex) {}
                if (curProd <= 0f && maxProd <= 0f) return;
                int id = b.id;
                float t = producerTimer.get(id, 0f) + dtSec;
                if (t >= SPREAD_SECONDS) {
                    producerTimer.put(id, t - SPREAD_SECONDS);
                    spreadFrom(b, W, H, lichen);
                } else {
                    producerTimer.put(id, t);
                }
            });
        }

        try {
            Unit unit = Vars.player.unit();
            if (unit != null && unit.plans != null) {
                Seq<BuildPlan> toRemove = new Seq<>();
                unit.plans.each(p -> {
                    if (p.breaking) {
                        Tile tl = Vars.world.tile(p.x, p.y);
                        if (tl != null && tl.floor() == lichen) toRemove.add(p);
                    }
                });
                for (BuildPlan p : toRemove) unit.plans.remove(p);
            }
        } catch (Exception ex) {}

        if (flameArrayBlock != null) {
            final int ar = (int) Math.ceil(FLAME_ARRAY_RANGE / 8f) + 1;
            final Block faRef = flameArrayBlock;
            final Floor lichenRef = lichen;
            Groups.build.each(b -> {
                if (b.block != faRef) return;
                if (b.team != playerTeam) return;
                if (!(b instanceof Turret.TurretBuild)) return;
                if (!((Turret.TurretBuild) b).hasAmmo()) return;
                int btx = (int)(b.x / 8f), bty = (int)(b.y / 8f);
                for (int dy = -ar; dy <= ar; dy++) {
                    for (int dx = -ar; dx <= ar; dx++) {
                        int x = btx + dx, y = bty + dy;
                        if (x < 0 || y < 0 || x >= W || y >= H) continue;
                        Tile tl = Vars.world.tile(x, y);
                        if (tl == null || tl.floor() != lichenRef) continue;
                        float wx = x * 8f + 4f, wy = y * 8f + 4f;
                        float ddx = wx - b.x, ddy = wy - b.y;
                        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                        if (d > FLAME_ARRAY_RANGE) continue;
                        int key = tileKey(x, y, W);
                        float cur = burnTimer.get(key, 0f) + Time.delta * 10f;
                        if (cur >= BURN_TICKS) {
                            Floor orig = tileOriginalFloor.get(key);
                            tl.setFloor(orig != null ? orig : (Floor) Blocks.stone);
                            tileOriginalFloor.remove(key);
                            burnTimer.remove(key, 0f);
                        } else {
                            burnTimer.put(key, cur);
                            if (Mathf.chance(0.08f)) Fx.burning.at(wx, wy);
                        }
                    }
                }
            });
        }

        if (flameBlock != null) {
            final int tr = (int) Math.ceil(FLAME_RANGE / 8f) + 1;
            final Block fbRef = flameBlock;
            final Floor lichenRef = lichen;
            Groups.build.each(b -> {
                if (b.block != fbRef) return;
                if (b.team != playerTeam) return;
                if (!(b instanceof Turret.TurretBuild)) return;
                if (!((Turret.TurretBuild) b).hasAmmo()) return;
                int btx = (int)(b.x / 8f), bty = (int)(b.y / 8f);
                int bestKey = -1, bestX = 0, bestY = 0;
                float bestDist = Float.MAX_VALUE;
                for (int dy = -tr; dy <= tr; dy++) {
                    for (int dx = -tr; dx <= tr; dx++) {
                        int x = btx + dx, y = bty + dy;
                        if (x < 0 || y < 0 || x >= W || y >= H) continue;
                        Tile tl = Vars.world.tile(x, y);
                        if (tl == null || tl.floor() != lichenRef) continue;
                        float wx = x * 8f + 4f, wy = y * 8f + 4f;
                        float ddx = wx - b.x, ddy = wy - b.y;
                        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                        if (d > FLAME_RANGE) continue;
                        if (d < bestDist) { bestDist = d; bestKey = tileKey(x, y, W); bestX = x; bestY = y; }
                    }
                }
                if (bestKey >= 0) {
                    float cur = burnTimer.get(bestKey, 0f) + Time.delta;
                    if (cur >= BURN_TICKS) {
                        Tile tl = Vars.world.tile(bestX, bestY);
                        Floor orig = tileOriginalFloor.get(bestKey);
                        if (tl != null) tl.setFloor(orig != null ? orig : (Floor) Blocks.stone);
                        tileOriginalFloor.remove(bestKey);
                        burnTimer.remove(bestKey, 0f);
                    } else {
                        burnTimer.put(bestKey, cur);
                        if (Mathf.chance(0.25f)) Fx.burning.at(bestX * 8f + 4f, bestY * 8f + 4f);
                    }
                }
            });
        }

        if (searType != null) {
            final int sr = (int) Math.ceil(SEAR_RANGE / 8f) + 1;
            final UnitType seRef = searType;
            final Floor lichenRef = lichen;
            Groups.unit.each(u -> {
                if (u.type != seRef || u.dead) return;
                if (u.team != playerTeam) return;
                float aimX = u.aimX();
                float aimY = u.aimY();
                if (aimX == 0f && aimY == 0f) return;
                float aimAngle = Angles.angle(u.x, u.y, aimX, aimY);
                int utx = (int)(u.x / 8f), uty = (int)(u.y / 8f);
                for (int dy = -sr; dy <= sr; dy++) {
                    for (int dx = -sr; dx <= sr; dx++) {
                        int x = utx + dx, y = uty + dy;
                        if (x < 0 || y < 0 || x >= W || y >= H) continue;
                        Tile tl = Vars.world.tile(x, y);
                        if (tl == null || tl.floor() != lichenRef) continue;
                        float wx = x * 8f + 4f, wy = y * 8f + 4f;
                        float ddx = wx - u.x, ddy = wy - u.y;
                        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                        if (d > SEAR_RANGE) continue;
                        float a = Angles.angle(u.x, u.y, wx, wy);
                        if (Math.abs(Angles.angleDist(a, aimAngle)) > 30f) continue;
                        int key = tileKey(x, y, W);
                        float cur = burnTimer.get(key, 0f) + Time.delta * 2f;
                        if (cur >= BURN_TICKS) {
                            Floor orig = tileOriginalFloor.get(key);
                            tl.setFloor(orig != null ? orig : (Floor) Blocks.stone);
                            tileOriginalFloor.remove(key);
                            burnTimer.remove(key, 0f);
                        } else {
                            burnTimer.put(key, cur);
                        }
                    }
                }
            });
        }
    }

    private static void spreadFrom(Building b, int W, int H, Floor lichen) {
        int sz = b.block.size;
        int cx = (int)(b.x / 8f), cy = (int)(b.y / 8f);
        int half = (sz - 1) / 2;
        Seq<int[]> candidates = new Seq<>();
        IntSet seen = new IntSet();

        int x0 = cx - half, y0 = cy - half;
        for (int dy = 0; dy < sz; dy++) {
            for (int dx = 0; dx < sz; dx++) {
                tryNeighbors(x0 + dx, y0 + dy, W, H, lichen, candidates, seen);
            }
        }
        for (int dy = -SPREAD_SCAN_RADIUS; dy <= SPREAD_SCAN_RADIUS; dy++) {
            for (int dx = -SPREAD_SCAN_RADIUS; dx <= SPREAD_SCAN_RADIUS; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x < 0 || y < 0 || x >= W || y >= H) continue;
                Tile t = Vars.world.tile(x, y);
                if (t != null && t.floor() == lichen) tryNeighbors(x, y, W, H, lichen, candidates, seen);
            }
        }

        if (candidates.size == 0) return;
        int[] pick = candidates.random();
        Tile t = Vars.world.tile(pick[0], pick[1]);
        if (t == null) return;
        tileOriginalFloor.put(pick[2], t.floor());
        t.setFloor(lichen);
        Codex.notifyLichenSpread();
    }

    private static void tryNeighbors(int sx, int sy, int W, int H, Floor lichen, Seq<int[]> candidates, IntSet seen) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = sx + d[0], ny = sy + d[1];
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
            int k = tileKey(nx, ny, W);
            if (seen.contains(k)) continue;
            seen.add(k);
            Tile t = Vars.world.tile(nx, ny);
            if (t == null) continue;
            Floor f = t.floor();
            if (f == lichen) continue;
            if (f.isDeep()) continue;
            if (f.solid) continue;
            candidates.add(new int[]{nx, ny, k});
        }
    }
}
