/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class LocalMiniMap extends Widget {
	public static final Resource plarrow = Resource.local().loadwait("gfx/hud/mmap/plarrow");

    public final MapView mv;
	private final MinimapCache cache;
    private Coord cc = null;
    private MinimapTile cur = null;

    public LocalMiniMap(Coord sz, MapView mv) {
	super(sz);
	this.mv = mv;
	this.cache = new MinimapCache(new MinimapRenderer(mv.ui.sess.glob.map));
    }
    
    public Coord p2c(Coord pc) {
	return(pc.div(tilesz).sub(cc).add(sz.div(2)));
    }

    public Coord c2p(Coord c) {
	return(c.sub(sz.div(2)).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    public void drawicons(GOut g) {
	OCache oc = ui.sess.glob.oc;
	synchronized(oc) {
	    for(Gob gob : oc) {
		try {
		    GobIcon icon = gob.getattr(GobIcon.class);
		    if(icon != null) {
			Coord gc = p2c(gob.rc);
			Tex tex = icon.tex();
			g.image(tex, gc.sub(tex.sz().div(2)));
		    }
		} catch(Loading l) {}
	    }
	}
    }

    public Gob findicongob(Coord c) {
	OCache oc = ui.sess.glob.oc;
	synchronized(oc) {
	    for(Gob gob : oc) {
		try {
		    GobIcon icon = gob.getattr(GobIcon.class);
		    if(icon != null) {
			Coord gc = p2c(gob.rc);
			Coord sz = icon.tex().sz();
			if(c.isect(gc.sub(sz.div(2)), sz))
			    return(gob);
		    }
		} catch(Loading l) {}
	    }
	}
	return(null);
    }

    public void tick(double dt) {
	Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
	if(pl == null) {
	    this.cc = null;
	    return;
	}
	this.cc = pl.rc.div(tilesz);
    }

    public void draw(GOut g) {
	if(cc == null)
	    return;
	final Coord plg = cc.div(cmaps);
	synchronized (cache) {
		cache.checkSession(plg);
	}

	Coord ulg = cc.sub(sz.div(2)).div(cmaps);
	Coord blg = cc.add(sz.div(2)).div(cmaps);
	Coord cg = new Coord();

	synchronized (cache) {
		for (cg.y = ulg.y; cg.y <= blg.y; cg.y++) {
			for (cg.x = ulg.x; cg.x <= blg.x; cg.x++) {
				Defer.Future<MinimapTile> f = cache.get(cg);
				if (!f.done())
					continue;
				MinimapTile tile = f.get();
				g.image(tile.img, cg.mul(cmaps).sub(cc).add(sz.div(2)));
			}
		}
	}

	try {
		synchronized(ui.sess.glob.party.memb) {
		    for(Party.Member m : ui.sess.glob.party.memb.values()) {
			Coord ptc;
			try {
			    ptc = m.getc();
			} catch(MCache.LoadingMap e) {
			    ptc = null;
			}
			if(ptc == null)
			    continue;
			ptc = p2c(ptc);
			double angle = m.getangle() + Math.PI / 2;
			Coord origin = plarrow.layer(Resource.negc).cc;
			g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 180);
			g.image(plarrow.layer(Resource.imgc).tex(), ptc.sub(origin), origin, angle);
			g.chcolor();
		    }
		}
	} catch(Loading l) {}
	drawicons(g);
    }

    public boolean mousedown(Coord c, int button) {
	if(cc == null)
	    return(false);
	Gob gob = findicongob(c);
	if(gob == null)
	    mv.wdgmsg("click", rootpos().add(c), c2p(c), button, ui.modflags());
	else
	    mv.wdgmsg("click", rootpos().add(c), c2p(c), button, ui.modflags(), 0, (int)gob.id, gob.rc, 0, -1);
	return(true);
    }
}
