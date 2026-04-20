// Infector drain mechanic
const drainData = {};
const DRAIN_PER_SEC = 30;
const DRAIN_RANGE = 80;
const DRAIN_THRESHOLD = 250;
const BLAST_RANGE = 200;

const blastBullet = (() => {
    const b = new BasicBulletType(5, 90);
    b.width = 14;
    b.height = 18;
    b.lifetime = 40;
    b.splashDamage = 70;
    b.splashDamageRadius = 40;
    b.frontColor = Color.valueOf("9B59B6");
    b.backColor = Color.valueOf("6C3483");
    b.trailColor = Color.valueOf("9B59B6");
    b.trailLength = 10;
    b.trailWidth = 3;
    b.hitEffect = Fx.hitBulletBig;
    b.despawnEffect = Fx.hitBulletBig;
    return b;
})();

Events.run(Trigger.update, () => {
    if (!Vars.state.isGame()) return;

    const infectorType = Vars.content.getByName(ContentType.unit, "xorinal-infector");
    if (infectorType == null) return;

    Groups.unit.each(u => {
        if (u.type !== infectorType || u.dead) return;

        const id = u.id;
        if (drainData[id] === undefined) drainData[id] = 0;

        const drainTick = DRAIN_PER_SEC / 60;

        Groups.unit.each(other => {
            if (other === u || other.dead) return;
            if (other.team === u.team) return;
            if (u.dst(other) > DRAIN_RANGE) return;

            const drain = Math.min(other.health, drainTick);
            other.damagePierce(drain);
            drainData[id] += drain;

            if (Mathf.chance(0.05)) {
                Fx.chainLightning.at(other.x, other.y, 0, Color.valueOf("9B59B6"), u);
            }
        });

        if (drainData[id] >= DRAIN_THRESHOLD) {
            const target = Units.closestEnemy(u.team, u.x, u.y, BLAST_RANGE, boolf(e => true));
            if (target != null) {
                const angle = Angles.angle(u.x, u.y, target.x, target.y);
                blastBullet.create(u, u.team, u.x, u.y, angle, 1, 1);
                Fx.shockwave.at(u.x, u.y, 0, Color.valueOf("9B59B6"));
            }
            drainData[id] -= DRAIN_THRESHOLD;
        }
    });

    // Clean up dead units
    var ids = Object.keys(drainData);
    for (var i = 0; i < ids.length; i++) {
        if (Groups.unit.getByID(parseInt(ids[i])) == null) {
            delete drainData[ids[i]];
        }
    }
});

Events.on(ClientLoadEvent, () => {
    // Xorinal planet icon + label in the main menu (top-right)
    try {
        const wrap = new Table();
        wrap.setFillParent(true);
        wrap.top().right();

        const inner = new Table();
        const region = Core.atlas.find("xorinal-planet-icon");
        inner.image(region).size(250, 250).pad(4);
        inner.row();
        const lbl = inner.add("[#2eff78]XORINAL[]");
        try { lbl.style(Styles.outlineLabel); } catch (e) {}
        lbl.pad(4);

        wrap.add(inner).pad(20);
        Vars.ui.menuGroup.addChild(wrap);
    } catch (e) {
        Log.err("Xorinal menu icon failed: " + e);
    }

    const t = new Table();
    t.setFillParent(true);
    t.top().right();
    t.button("Win Sector", () => {
        if (!Vars.state.isGame() || Vars.state.rules.sector == null) return;
        Groups.unit.each(u => {
            if (u.team != Vars.player.team()) u.kill();
        });
        if (Vars.state.rules.winWave > 0) {
            Vars.state.wave = Math.max(Vars.state.wave, Vars.state.rules.winWave + 1);
        }
    }).size(140, 40).pad(8);
    Vars.ui.hudGroup.addChild(t);
});
