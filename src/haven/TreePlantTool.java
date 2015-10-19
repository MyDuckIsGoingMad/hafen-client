package haven;

public class TreePlantTool extends TileGrabber {
    private boolean clicked;
    private double t;

    public TreePlantTool(GameUI gui) {
        super("Select tile...", gui);
    }

    @Override
    protected void done(Coord tile) {
        ui.gui.map.
        ui.gui.map.wdgmsg("itemact", Coord.z, tile.mul(MCache.tilesz).add(MCache.tilesz.div(2)), ui.modflags());
        clicked = true;
    }

    @Override
    public void tick(double dt) {
        if (clicked) {
            FlowerMenu menu = ui.root.findchild(FlowerMenu.class);
            if (menu != null) {
                FlowerMenu.Petal opt = getPlantTreeOption(menu);
                if (opt != null) {
                    menu.choose(menu.opts[0]);
                    clicked = false;
                    destroy();
                }
            } else {
                t += dt;
                // wait for a response
                if (t > 1) {
                    ui.gui.error("Couldn't plant tree!");
                    clicked = false;
                    destroy();
                }
            }
        }
    }

    @Override
    public void destroy() {
        // allow widget to be destroyed only when there is no waiting for a click response
        if (!clicked)
            super.destroy();
    }

    private static FlowerMenu.Petal getPlantTreeOption(FlowerMenu menu) {
        return menu.opts.length > 0 && "Plant tree".equals(menu.opts[0].name) ? menu.opts[0] : null;
    }
}
