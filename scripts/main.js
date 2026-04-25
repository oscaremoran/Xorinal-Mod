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

// ==================== Mycelium Lichen ====================
// Power producers spread lichen 1 tile/60s. Slows units. Player can't
// deconstruct on lichen. Only flamethrower (5s sustained) or enemies clear.
const LICHEN_NAME = "xorinal-mycelium-lichen";
const FLAME_NAME = "xorinal-flamethrower";
const SPREAD_SECONDS = 5;
const BURN_TICKS = 300; // 5s @ 60fps
const FLAME_RANGE = 70;
const FLAME_CONE_DEG = 28;
const SPREAD_SCAN_RADIUS = 30;

const FLAME_ARRAY_NAME = "xorinal-flame-array";
const FLAME_ARRAY_RANGE = 130;
const SEAR_NAME = "xorinal-sear";
const SEAR_RANGE = 80;
let _lichenFloor = null;
let _flameBlock = null;
let _flameArrayBlock = null;
let _searType = null;
const tileOriginalFloor = {}; // key -> original Floor
const producerTimer = {};     // building.id -> seconds accumulated
const burnTimer = {};         // tileKey -> ticks burning

function _tileKey(x, y, W) { return y * W + x; }

function _resolveLichen() {
    if (_lichenFloor == null) {
        _lichenFloor = Vars.content.getByName(ContentType.block, LICHEN_NAME);
    }
    return _lichenFloor;
}

function _spreadFrom(b, world, W, H, lichen) {
    const sz = b.block.size;
    const cx = Math.floor(b.x / 8);
    const cy = Math.floor(b.y / 8);
    const half = Math.floor((sz - 1) / 2);
    const candidates = [];
    const seen = {};

    const tryNeighbors = (sx, sy) => {
        const dirs = [[1,0],[-1,0],[0,1],[0,-1]];
        for (let i = 0; i < 4; i++) {
            const nx = sx + dirs[i][0], ny = sy + dirs[i][1];
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
            const k = _tileKey(nx, ny, W);
            if (seen[k]) continue;
            seen[k] = true;
            const t = world.tile(nx, ny);
            if (t == null) continue;
            const f = t.floor();
            if (f === lichen) continue;
            if (f.isDeep()) continue;
            if (f.solid) continue;
            candidates.push([nx, ny, t, f, k]);
        }
    };

    // seed with producer footprint
    const x0 = cx - half, y0 = cy - half;
    for (let dy = 0; dy < sz; dy++) {
        for (let dx = 0; dx < sz; dx++) {
            tryNeighbors(x0 + dx, y0 + dy);
        }
    }
    // plus lichen tiles nearby (so growth expands outward)
    for (let dy = -SPREAD_SCAN_RADIUS; dy <= SPREAD_SCAN_RADIUS; dy++) {
        for (let dx = -SPREAD_SCAN_RADIUS; dx <= SPREAD_SCAN_RADIUS; dx++) {
            const x = cx + dx, y = cy + dy;
            if (x < 0 || y < 0 || x >= W || y >= H) continue;
            const t = world.tile(x, y);
            if (t != null && t.floor() === lichen) tryNeighbors(x, y);
        }
    }

    if (candidates.length === 0) return;
    const pick = candidates[Math.floor(Math.random() * candidates.length)];
    tileOriginalFloor[pick[4]] = pick[3];
    pick[2].setFloor(lichen);
}

Events.on(WorldLoadEvent, () => {
    try {
        const r = Vars.state.rules;
        const sName = (r.sector != null && r.sector.planet != null) ? (r.sector.planet.name + "") : "(no-sector)";
        const pName = (r.planet != null) ? (r.planet.name + "") : "(no-rules-planet)";
        Log.info("[Xorinal] WorldLoad: sector-planet=" + sName + " rules-planet=" + pName + " lichenPlanet=" + _isLichenPlanet());
        const s = Vars.state.rules.sector;
        if (s == null || s.planet == null) return;
        const n = s.planet.name + "";
        if (n === "xorinal-xorinal" || n === "xorinal-tetra") return;
        // Not our planet — purge any lichen tiles that leaked there
        const lichen = Vars.content.getByName(ContentType.block, LICHEN_NAME);
        if (lichen == null) return;
        const W = Vars.world.width(), H = Vars.world.height();
        let purged = 0;
        for (let y = 0; y < H; y++) {
            for (let x = 0; x < W; x++) {
                const tl = Vars.world.tile(x, y);
                if (tl != null && tl.floor() === lichen) {
                    tl.setFloor(Blocks.stone);
                    purged++;
                }
            }
        }
        if (purged > 0) Log.info("[Xorinal] Purged " + purged + " stray lichen tiles on " + n);
    } catch (e) { Log.err("lichen purge: " + e); }
});

function _isLichenPlanet() {
    try {
        const r = Vars.state.rules;
        // 1) Sector-based play: check sector's planet
        const s = r.sector;
        if (s != null && s.planet != null) {
            const n = s.planet.name + "";
            return n === "xorinal-xorinal" || n === "xorinal-tetra";
        }
        // 2) Custom game: rules may carry a planet directly
        if (r.planet != null) {
            const n = r.planet.name + "";
            return n === "xorinal-xorinal" || n === "xorinal-tetra";
        }
        // 3) Unknown context (custom map without planet tag) — fail-open so
        //    test maps still spread. WorldLoadEvent only purges when it can
        //    positively identify a non-Xorinal/Tetra planet.
        return true;
    } catch (e) { return true; }
}

Events.run(Trigger.update, () => {
    if (!Vars.state.isGame()) return;
    const lichen = _resolveLichen();
    if (lichen == null) return;
    if (_flameBlock == null) _flameBlock = Vars.content.getByName(ContentType.block, FLAME_NAME);

    const world = Vars.world;
    const W = world.width();
    const H = world.height();
    const playerTeam = Vars.player.team();
    const dtSec = Time.delta / 60;
    const onLichenPlanet = _isLichenPlanet();

    // 1) spread from power producers — ONLY on Xorinal/Tetra.
    // Accept if currently producing (>0) OR if the block type's max power
    // production is >0 (so idle combustors still spread). Power nodes are
    // excluded because they have no powerProduction field / value is 0.
    if (onLichenPlanet) Groups.build.each(b => {
        if (b.team !== playerTeam) return;
        if (b.block == null || !b.block.outputsPower) return;
        let curProd = 0, maxProd = 0;
        try { curProd = b.getPowerProduction(); } catch (e) {}
        try { maxProd = +b.block.powerProduction; } catch (e) {}
        if (isNaN(maxProd)) maxProd = 0;
        if (curProd <= 0 && maxProd <= 0) return;
        const id = b.id;
        const t = (producerTimer[id] || 0) + dtSec;
        if (t >= SPREAD_SECONDS) {
            producerTimer[id] = t - SPREAD_SECONDS;
            _spreadFrom(b, world, W, H, lichen);
        } else {
            producerTimer[id] = t;
        }
    });

    // 2) guard player deconstruct on lichen
    try {
        const unit = Vars.player.unit();
        if (unit != null && unit.plans != null) {
            const toRemove = [];
            unit.plans.each(p => {
                if (p.breaking) {
                    const tl = world.tile(p.x, p.y);
                    if (tl != null && tl.floor() === lichen) toRemove.push(p);
                }
            });
            for (let i = 0; i < toRemove.length; i++) unit.plans.remove(toRemove[i]);
        }
    } catch (e) {}

    // 3a) Flame Array clears ALL lichen tiles in its 130u range in parallel, fast (~0.5s each)
    if (_flameArrayBlock == null) _flameArrayBlock = Vars.content.getByName(ContentType.block, FLAME_ARRAY_NAME);
    if (_flameArrayBlock != null) {
        const ar = Math.ceil(FLAME_ARRAY_RANGE / 8) + 1;
        Groups.build.each(b => {
            if (b.block !== _flameArrayBlock) return;
            if (b.team !== playerTeam) return;
            let hasAmmo = false;
            try { hasAmmo = b.hasAmmo(); } catch (e) { return; }
            if (!hasAmmo) return;
            const btx = Math.floor(b.x / 8), bty = Math.floor(b.y / 8);
            for (let dy = -ar; dy <= ar; dy++) {
                for (let dx = -ar; dx <= ar; dx++) {
                    const x = btx + dx, y = bty + dy;
                    if (x < 0 || y < 0 || x >= W || y >= H) continue;
                    const tl = world.tile(x, y);
                    if (tl == null || tl.floor() !== lichen) continue;
                    const wx = x * 8 + 4, wy = y * 8 + 4;
                    const ddx = wx - b.x, ddy = wy - b.y;
                    const d = Math.sqrt(ddx * ddx + ddy * ddy);
                    if (d > FLAME_ARRAY_RANGE) continue;
                    const key = _tileKey(x, y, W);
                    const cur = (burnTimer[key] || 0) + Time.delta * 10; // 10x rate
                    if (cur >= BURN_TICKS) {
                        const orig = tileOriginalFloor[key];
                        if (orig != null) tl.setFloor(orig);
                        else tl.setFloor(Blocks.stone);
                        delete tileOriginalFloor[key];
                        delete burnTimer[key];
                    } else {
                        burnTimer[key] = cur;
                        if (Mathf.chance(0.08)) Fx.burning.at(wx, wy);
                    }
                }
            }
        });
    }

    // 3b) flamethrower clears lichen (sustained burn 5s, 360° while it has ammo)
    if (_flameBlock != null) {
        const tr = Math.ceil(FLAME_RANGE / 8) + 1;
        Groups.build.each(b => {
            if (b.block !== _flameBlock) return;
            if (b.team !== playerTeam) return;
            let hasAmmo = false;
            try { hasAmmo = b.hasAmmo(); } catch (e) { return; }
            if (!hasAmmo) return;
            const btx = Math.floor(b.x / 8), bty = Math.floor(b.y / 8);
            let bestKey = -1, bestDist = 1e9, bestX = 0, bestY = 0;
            for (let dy = -tr; dy <= tr; dy++) {
                for (let dx = -tr; dx <= tr; dx++) {
                    const x = btx + dx, y = bty + dy;
                    if (x < 0 || y < 0 || x >= W || y >= H) continue;
                    const tl = world.tile(x, y);
                    if (tl == null || tl.floor() !== lichen) continue;
                    const wx = x * 8 + 4, wy = y * 8 + 4;
                    const ddx = wx - b.x, ddy = wy - b.y;
                    const d = Math.sqrt(ddx * ddx + ddy * ddy);
                    if (d > FLAME_RANGE) continue;
                    if (d < bestDist) {
                        bestDist = d; bestKey = _tileKey(x, y, W);
                        bestX = x; bestY = y;
                    }
                }
            }
            if (bestKey >= 0) {
                const cur = (burnTimer[bestKey] || 0) + Time.delta;
                if (cur >= BURN_TICKS) {
                    const tl = world.tile(bestX, bestY);
                    const orig = tileOriginalFloor[bestKey];
                    if (orig != null) tl.setFloor(orig);
                    else tl.setFloor(Blocks.stone);
                    delete tileOriginalFloor[bestKey];
                    delete burnTimer[bestKey];
                } else {
                    burnTimer[bestKey] = cur;
                    if (Mathf.chance(0.25)) {
                        Fx.burning.at(bestX * 8 + 4, bestY * 8 + 4);
                    }
                }
            }
        });
    }

    // 3c) Sear units clear lichen in their aim cone at 2x flamethrower rate
    if (_searType == null) _searType = Vars.content.getByName(ContentType.unit, SEAR_NAME);
    if (_searType != null) {
        const sr = Math.ceil(SEAR_RANGE / 8) + 1;
        Groups.unit.each(u => {
            if (u.type !== _searType || u.dead) return;
            if (u.team !== playerTeam) return;
            let aimX = 0, aimY = 0, hasAim = false;
            try {
                const tgt = u.target;
                if (tgt != null) { aimX = tgt.x; aimY = tgt.y; hasAim = true; }
            } catch (e) {}
            if (!hasAim) {
                try { aimX = u.aimX; aimY = u.aimY; hasAim = (aimX !== 0 || aimY !== 0); } catch (e) {}
            }
            if (!hasAim) return;
            const aimAngle = Angles.angle(u.x, u.y, aimX, aimY);
            const utx = Math.floor(u.x / 8), uty = Math.floor(u.y / 8);
            for (let dy = -sr; dy <= sr; dy++) {
                for (let dx = -sr; dx <= sr; dx++) {
                    const x = utx + dx, y = uty + dy;
                    if (x < 0 || y < 0 || x >= W || y >= H) continue;
                    const tl = world.tile(x, y);
                    if (tl == null || tl.floor() !== lichen) continue;
                    const wx = x * 8 + 4, wy = y * 8 + 4;
                    const ddx = wx - u.x, ddy = wy - u.y;
                    const d = Math.sqrt(ddx * ddx + ddy * ddy);
                    if (d > SEAR_RANGE) continue;
                    const a = Angles.angle(u.x, u.y, wx, wy);
                    if (Math.abs(Angles.angleDist(a, aimAngle)) > 30) continue;
                    const key = _tileKey(x, y, W);
                    const cur = (burnTimer[key] || 0) + Time.delta * 2;
                    if (cur >= BURN_TICKS) {
                        const orig = tileOriginalFloor[key];
                        if (orig != null) tl.setFloor(orig);
                        else tl.setFloor(Blocks.stone);
                        delete tileOriginalFloor[key];
                        delete burnTimer[key];
                    } else {
                        burnTimer[key] = cur;
                    }
                }
            }
        });
    }
});


// ==================== Planet meshes ====================
// meshLoader is a Prov<PlanetMesh>, so it must be assigned from JS, not hjson.
function _setupPlanetMeshes() {
    try {
        const NoiseMesh = Packages.mindustry.graphics.g3d.NoiseMesh;
        const HexSkyMesh = Packages.mindustry.graphics.g3d.HexSkyMesh;
        const MultiMesh = Packages.mindustry.graphics.g3d.MultiMesh;
        const HexMesh = Packages.mindustry.graphics.g3d.HexMesh;

        // NoiseMesh(planet, seed, divisions, radius, octaves, persistence, scale, mag, ...colors)
        function noiseMesh(planet, seed, divisions, radius, mag, colorHexes) {
            const cols = colorHexes.map(c => Color.valueOf(c));
            const args = [planet, seed, divisions, radius, 6, 0.55, 1.6, mag];
            for (let i = 0; i < cols.length; i++) args.push(cols[i]);
            return NoiseMesh.class.getConstructors()[0].newInstance(args);
        }

        // NoiseMesh(planet, seed, divisions, radius, octaves, persistence, scale, mag, ...colors)
        function makeNoise(planet, seed, divisions, octaves, persistence, scale, mag, colorHexes) {
            const c = colorHexes.map(h => Color.valueOf(h));
            // expand to 8 colors max — pad with last if shorter
            while (c.length < 8) c.push(c[c.length - 1]);
            return new NoiseMesh(planet, seed, divisions, 0.6,
                octaves, persistence, scale, mag,
                c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]);
        }

        // HexSkyMesh(planet, seed, speed, radius, divisions, color, octaves, persistence, scale, threshold)
        function clouds(planet, layers) {
            const meshes = layers.map(L =>
                new HexSkyMesh(planet, L.seed, L.speed, L.radius, L.divisions,
                    Color.valueOf(L.color), L.octaves, L.persistence, L.scale, L.threshold));
            return new MultiMesh(meshes);
        }

        const tetra = Vars.content.getByName(ContentType.planet, "xorinal-tetra");
        const xorinal = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");

        if (tetra != null) {
            // 8 stops: deep crevasse → trench ice → ocean → frozen sea → cracked ice → snow → glacier → peaks
            tetra.meshLoader = () => makeNoise(tetra, 7, 8, 7, 0.55, 1.5, 0.55,
                ["0e2440","1a3d63","265e85","4ea8d4","8ec8e8","b8dcef","e6f4ff","ffffff"]);
            tetra.cloudMeshLoader = () => clouds(tetra, [
                // dense low cloud
                { seed: 2, speed: 0.20, radius: 0.205, divisions: 6, color: "ffffff90", octaves: 3, persistence: 0.55, scale: 1.0, threshold: 0.60 },
                // mid wispy
                { seed: 5, speed: 0.12, radius: 0.225, divisions: 6, color: "cde9ff70", octaves: 3, persistence: 0.6,  scale: 1.5, threshold: 0.62 },
                // high faint cirrus
                { seed: 11, speed: 0.06, radius: 0.245, divisions: 5, color: "e6f4ff40", octaves: 2, persistence: 0.5,  scale: 2.2, threshold: 0.7 }
            ]);
        }
        if (xorinal != null) {
            // 8 stops: deep moss → swamp → spore floor → mid spore → bright spore → bloom → highland → bright highland
            xorinal.meshLoader = () => makeNoise(xorinal, 13, 8, 7, 0.55, 1.5, 0.6,
                ["0c1f10","17371a","244a26","3E7B2F","5fa840","8ec860","b6dc7c","e0f0a8"]);
            xorinal.cloudMeshLoader = () => clouds(xorinal, [
                // spore drift dense
                { seed: 4, speed: 0.18, radius: 0.205, divisions: 6, color: "9fd47880", octaves: 3, persistence: 0.55, scale: 1.1, threshold: 0.58 },
                // mid wisp
                { seed: 9, speed: 0.10, radius: 0.225, divisions: 6, color: "c8e6a060", octaves: 3, persistence: 0.6,  scale: 1.6, threshold: 0.62 },
                // high bloom haze
                { seed: 17, speed: 0.05, radius: 0.245, divisions: 5, color: "e0f0a830", octaves: 2, persistence: 0.5,  scale: 2.3, threshold: 0.7 }
            ]);
        }
    } catch (e) {
        Log.err("[Xorinal] planet mesh setup failed: " + e);
        try {
            const HexMesh = Packages.mindustry.graphics.g3d.HexMesh;
            const tetra = Vars.content.getByName(ContentType.planet, "xorinal-tetra");
            const xorinal = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");
            if (tetra != null) tetra.meshLoader = () => new HexMesh(tetra, 6);
            if (xorinal != null) xorinal.meshLoader = () => new HexMesh(xorinal, 6);
        } catch (e2) {}
    }
}

Events.on(ClientLoadEvent, () => {
    _setupPlanetMeshes();

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
