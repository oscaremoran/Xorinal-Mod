Events.on(ClientLoadEvent, () => {
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
