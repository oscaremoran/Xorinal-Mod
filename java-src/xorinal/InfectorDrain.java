package xorinal;

import arc.Events;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.IntFloatMap;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class InfectorDrain {
    private static final float DRAIN_PER_SEC = 30f;
    private static final float DRAIN_RANGE = 80f;
    private static final float DRAIN_THRESHOLD = 250f;
    private static final float BLAST_RANGE = 200f;

    private static final IntFloatMap drainData = new IntFloatMap();
    private static UnitType infectorType;
    private static BasicBulletType blastBullet;

    public static void init() {
        blastBullet = new BasicBulletType(5f, 90f);
        blastBullet.width = 14f;
        blastBullet.height = 18f;
        blastBullet.lifetime = 40f;
        blastBullet.splashDamage = 70f;
        blastBullet.splashDamageRadius = 40f;
        blastBullet.frontColor = Color.valueOf("9B59B6");
        blastBullet.backColor = Color.valueOf("6C3483");
        blastBullet.trailColor = Color.valueOf("9B59B6");
        blastBullet.trailLength = 10;
        blastBullet.trailWidth = 3f;
        blastBullet.hitEffect = Fx.hitBulletBig;
        blastBullet.despawnEffect = Fx.hitBulletBig;

        Events.run(Trigger.update, InfectorDrain::tick);
    }

    private static void tick() {
        if (!Vars.state.isGame()) return;
        if (infectorType == null) {
            infectorType = (UnitType) Vars.content.getByName(ContentType.unit, "xorinal-infector");
            if (infectorType == null) return;
        }

        float drainTick = DRAIN_PER_SEC / 60f * Time.delta;
        UnitType infRef = infectorType;

        Groups.unit.each(u -> {
            if (u.type != infRef || u.dead) return;

            float accumulated = drainData.get(u.id, 0f);

            for (Unit other : Groups.unit) {
                if (other == u || other.dead) continue;
                if (other.team == u.team) continue;
                if (u.dst(other) > DRAIN_RANGE) continue;
                float d = Math.min(other.health, drainTick);
                other.damagePierce(d);
                accumulated += d;
                if (Mathf.chance(0.05f)) {
                    Fx.chainLightning.at(other.x, other.y, 0f, Color.valueOf("9B59B6"), u);
                }
            }

            if (accumulated >= DRAIN_THRESHOLD) {
                Unit target = Units.closestEnemy(u.team, u.x, u.y, BLAST_RANGE, t -> true);
                if (target != null) {
                    float angle = Angles.angle(u.x, u.y, target.x, target.y);
                    blastBullet.create(u, u.team, u.x, u.y, angle, 1f, 1f);
                    Fx.shockwave.at(u.x, u.y, 0f, Color.valueOf("9B59B6"));
                }
                accumulated -= DRAIN_THRESHOLD;
            }

            drainData.put(u.id, accumulated);
        });

        var keys = drainData.keys().toArray();
        for (int i = 0; i < keys.size; i++) {
            int id = keys.items[i];
            if (Groups.unit.getByID(id) == null) drainData.remove(id, 0f);
        }
    }
}
