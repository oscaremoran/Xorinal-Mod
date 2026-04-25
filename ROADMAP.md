# Roadmap to 2.0 — Tetra Complete

**Goal:** Ship 2.0 with Tetra fully playable as an Erekir-style attack campaign. Xorinal stays as-is (already shipped at 1.3). Cross-planet unlocks and new planets are deferred to 3.0.

---

## Definition of done for 2.0

- [ ] 6 connected Tetra sectors, attack-mode by default, with a final capture sector
- [ ] Tetra tech tree filled out: every unit/block reachable through `research:` chain off Core: Frost
- [ ] Three unit tiers per role (worker / ground / air), buildable from Tetra factories
- [ ] At least 4 Tetra turrets covering basic / splash / piercing / anti-air
- [ ] Production chain: ice → cryonite → glacium → (new) advanced tier
- [ ] Tetra has a liquid system (e.g. cryofluid analog) for cooling/turret ammo
- [ ] Launch loadout + launch schematics enabled and tested
- [ ] All new content has bundle entries (name + description) and sprites
- [ ] Tested end-to-end on Steam build, no missing-sprite errors in console

---

## Milestone 1 — Content gaps (block + unit tree)

**Units** (current: frost-scouter T1 worker, cryolith T1 mech, rimewing T2 flying):
- [X] T2 ground mech (heavier than Cryolith — pierce/AoE role)
- [ ] T3 ground walker or boss-tier mech (Mycelith-equivalent for Tetra)
- [ ] T1 flying scout's T2/T3 successors (Rimewing already T2; needs T3)
- [ ] Optional: support/healer unit (cryo-medic style)

**Turrets** (current: frost-flak only):
- [ ] T1 basic ice turret (low cost, ice ammo)
- [ ] T2 splash/mortar (cryonite ammo)
- [ ] T3 long-range pierce or beam (glacium ammo)
- [ ] Anti-air dedicated (flak already covers this — verify)

**Production / logistics:**
- [ ] Advanced T3 material (e.g. "Permafrost Alloy") — combine glacium + new resource
- [ ] Liquid: Cryofluid (extracted or produced) + frost-conduit + frost-liquid-router
- [ ] Frost crafter that consumes liquid (cooled smelter)
- [ ] Power: T2 power source (cryo-reactor or solar variant for Tetra)

**Storage / walls:** mostly done — verify Frost Vault tier, large/huge walls.

---

## Milestone 2 — Sector campaign

Six sectors, each unlocked by completing the previous. All attack-mode unless noted.

| # | Name (working) | Type | Unlocks |
|---|----------------|------|---------|
| 1 | Frost Landing | Survival (intro) | Tetra basics |
| 2 | Glacial Ridge | Attack | T2 turrets / Cryolith |
| 3 | Cryonite Quarry | Attack | Cryonite chain |
| 4 | Frozen Outpost | Attack | T3 unit tier |
| 5 | Permafrost Spire | Attack | Glacium / liquid system |
| 6 | Crux Glacier Citadel | Attack (final) | 2.0 victory |

Each sector needs:
- `.msav` map file
- `content/sectors/<name>.hjson` with `planet: tetra`, sector index, attack rules, research parent
- Bundle entries for sector name + description
- Enemy base layout (use existing Crux/Mycelium AI units; verify they spawn cold-themed if applicable)

---

## Milestone 3 — Polish & ship

- [ ] Launch loadout: set `defaultCore: xorinal-core-frost` works on launch (already configured)
- [ ] Launch schematics: enable `allowLaunchSchematics: true` on Tetra (currently true)
- [ ] Test cross-sector progression end-to-end on Steam build
- [ ] Tech tree visualization sanity check — no orphans, no unreachable nodes
- [ ] Bump `mod.hjson` to `2.0`, update description
- [ ] Sync to Steam, restart Mindustry, verify no missing-sprite or hjson parse errors
- [ ] Tag release on GitHub

---

## Out of scope for 2.0 (deferred to 3.0)

- Cross-planet unlock scripting (sector-on-Xorinal completion → unlock new planet)
- Additional planets beyond Xorinal/Tetra
- Multiplayer-specific tuning
- Translations beyond English bundle

---

## Open questions

- Does Tetra need its own enemy faction (frost Crux variants), or reuse Xorinal's Crux Myceloids?
- Final sector boss: scripted spawn (like Overmind on Xorinal) or just a fortified base?
- Liquid system: is Cryofluid worth the scope, or skip and go straight to advanced alloy?
