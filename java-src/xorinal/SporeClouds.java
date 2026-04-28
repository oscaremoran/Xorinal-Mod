package xorinal;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;

public class SporeClouds {
    public static StatusEffect poisoned;

    private static final float CLOUD_LIFETIME_TICKS = 60f * 4f;
    private static final float DEFAULT_CLOUD_RADIUS = 64f;
    private static final float DAMAGE_FRACTION_PER_SEC = 0.04f;
    private static final float POISON_TICK_DURATION = 14f;
    private static final Color CLOUD_COLOR = Color.valueOf("5fff6f");

    private static UnitType bloomheartType;

    private static final Seq<Cloud> clouds = new Seq<>();
    private static final ObjectMap<Bullet, Vec2> tracked = new ObjectMap<>();
    private static final ObjectSet<Bullet> seenBuf = new ObjectSet<>();
    private static final Seq<Bullet> removeBuf = new Seq<>();

    private static class Cloud {
        float x, y, ttl;
        Team team;
        float radius = DEFAULT_CLOUD_RADIUS;
    }

    public static void registerContent() {
        try {
            poisoned = new StatusEffect("poisoned");
            poisoned.color = CLOUD_COLOR.cpy();
            poisoned.show = true;
            poisoned.permanent = false;
            poisoned.speedMultiplier = 0.9f;
            Log.info("[Xorinal] registered status: " + poisoned.name);
        } catch (Exception e) {
            Log.err("[Xorinal] poisoned status register: " + e);
        }
    }

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> { clouds.clear(); tracked.clear(); });
        Events.run(Trigger.update, SporeClouds::tick);
        Events.run(Trigger.drawOver, SporeClouds::draw);
    }

    private static void tick() {
        if (!Vars.state.isGame() || Vars.state.isPaused()) return;
        if (bloomheartType == null) {
            bloomheartType = (UnitType) Vars.content.getByName(ContentType.unit, Bloomheart.BOSS_UNIT);
            if (bloomheartType == null) return;
        }

        seenBuf.clear();
        for (Bullet b : Groups.bullet) {
            Entityc o = b.owner;
            if (o instanceof Unit && ((Unit) o).type == bloomheartType) {
                Vec2 v = tracked.get(b);
                if (v == null) { v = new Vec2(); tracked.put(b, v); }
                v.set(b.x, b.y);
                seenBuf.add(b);
            }
        }
        removeBuf.clear();
        for (var entry : tracked.entries()) {
            if (!seenBuf.contains(entry.key)) {
                spawnCloud(entry.value.x, entry.value.y, entry.key.team);
                removeBuf.add(entry.key);
            }
        }
        for (int i = 0; i < removeBuf.size; i++) tracked.remove(removeBuf.get(i));

        for (int i = clouds.size - 1; i >= 0; i--) {
            Cloud c = clouds.get(i);
            c.ttl -= Time.delta;
            if (c.ttl <= 0f) { clouds.remove(i); continue; }

            int puffs = Math.max(1, (int) (c.radius / 28f));
            for (int p = 0; p < puffs; p++) {
                if (Mathf.chance(0.35)) {
                    float ang = Mathf.random(360f);
                    float r = Mathf.random(c.radius);
                    Fx.smoke.at(c.x + Mathf.cosDeg(ang) * r, c.y + Mathf.sinDeg(ang) * r, 0f, CLOUD_COLOR);
                }
            }

            for (Unit u : Groups.unit) {
                if (u.dead || u.team == c.team) continue;
                float dx = u.x - c.x, dy = u.y - c.y;
                if (dx * dx + dy * dy <= c.radius * c.radius) {
                    float dmg = u.maxHealth * DAMAGE_FRACTION_PER_SEC * Time.delta / 60f;
                    u.damagePierce(dmg);
                    if (poisoned != null) u.apply(poisoned, POISON_TICK_DURATION);
                }
            }
        }
    }

    private static void draw() {
        if (clouds.isEmpty()) return;
        float prevA = Draw.getColor().a;
        for (int i = 0; i < clouds.size; i++) {
            Cloud c = clouds.get(i);
            float fade = Mathf.clamp(c.ttl / CLOUD_LIFETIME_TICKS);
            float pulse = 0.85f + Mathf.absin(Time.time + i * 7f, 8f, 0.15f);
            float r = c.radius * pulse;
            Draw.color(CLOUD_COLOR, 0.18f * fade);
            Fill.circle(c.x, c.y, r);
            Draw.color(CLOUD_COLOR, 0.32f * fade);
            Fill.circle(c.x, c.y, r * 0.55f);
        }
        Draw.color();
        Draw.alpha(prevA);
    }

    private static void spawnCloud(float x, float y, Team team) {
        Cloud c = new Cloud();
        c.x = x; c.y = y; c.ttl = CLOUD_LIFETIME_TICKS; c.team = team;
        clouds.add(c);
        try { Fx.greenCloud.at(x, y); } catch (Exception ex) {}
    }

    public static void spawnBigCloud(float x, float y, Team team, float radius, float ttlSeconds) {
        Cloud c = new Cloud();
        c.x = x; c.y = y; c.ttl = ttlSeconds * 60f; c.team = team;
        c.radius = radius;
        clouds.add(c);
        try {
            Fx.greenCloud.at(x, y);
            Fx.shockwave.at(x, y, 0f, CLOUD_COLOR);
        } catch (Exception ex) {}
    }
}
