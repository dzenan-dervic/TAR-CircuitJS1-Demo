/*
    Copyright (C) Paul Falstad and Iain Sharp
    TAR-Dervic EGT extension: RS-/SR-Speicher (FUP), L402 / LOGIK-07

    This file is part of CircuitJS1.

    CircuitJS1 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    CircuitJS1 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CircuitJS1.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Asynchroner RS-/SR-Speicher im FUP-Stil (LOGIK-07 / L402).
 * Nur Funktionsblock — keine Übersicht. Maus-skalierbar (sizeX/sizeY).
 */
class RSSpeicherElm extends ChipElm {
    final int FLAG_RESET_DOMINANT = 2;
    final int FLAG_NO_QN = 4;

    static final int MIN_SX = 3;
    static final Color COL_HI = new Color(0x2e, 0xcc, 0x71);
    static final Color COL_LO = new Color(0x88, 0x88, 0x88);

    boolean justLoaded;

    public RSSpeicherElm(int xx, int yy) {
	super(xx, yy);
	// Standard: Reset-dominant — R löscht auch bei dauerhaftem S=1
	flags |= FLAG_RESET_DOMINANT;
    }
    public RSSpeicherElm(int xa, int ya, int xb, int yb, int f,
			 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	justLoaded = true;
	try {
	    if (st.hasMoreTokens()) {
		int sx = Integer.parseInt(st.nextToken());
		int sy = Integer.parseInt(st.nextToken());
		applySize(sx, sy, false);
	    }
	} catch (Exception e) { }
    }

    boolean useSmallGrid() { return true; }

    int minSy() { return hasQn() ? 4 : 3; }

    boolean resetDominant() { return (flags & FLAG_RESET_DOMINANT) != 0; }
    boolean hasQn() { return (flags & FLAG_NO_QN) == 0; }

    String getChipName() {
	return resetDominant()
		? Locale.LS("RS-Speicher (FUP)")
		: Locale.LS("SR-Speicher (FUP)");
    }

    void applySize(int sx, int sy, boolean keepState) {
	if (sx < MIN_SX)
	    sx = MIN_SX;
	if (sy < minSy())
	    sy = minSy();
	boolean qv = false, nqv = true;
	if (keepState && pins != null && pins.length > 2) {
	    qv = pins[2].value;
	    if (hasQn() && pins.length > 3)
		nqv = pins[3].value;
	}
	sizeX = sx;
	sizeY = sy;
	setupPins();
	if (keepState && pins != null && pins.length > 2) {
	    pins[2].value = qv;
	    volts[2] = qv ? highVoltage : 0;
	    if (hasQn() && pins.length > 3) {
		pins[3].value = nqv;
		volts[3] = nqv ? highVoltage : 0;
	    }
	}
	allocNodes();
	setPoints();
    }

    void setupPins() {
	if (sizeX < MIN_SX)
	    sizeX = MIN_SX;
	if (sizeY < minSy())
	    sizeY = minSy();
	pins = new Pin[getPostCount()];
	int yS = Math.max(1, sizeY / 4);
	int yR = Math.min(sizeY - 1, (sizeY * 3) / 4);
	if (yR <= yS)
	    yR = Math.min(sizeY - 1, yS + 1);
	pins[0] = new Pin(yS, SIDE_W, "S");
	pins[1] = new Pin(yR, SIDE_W, "R");
	pins[2] = new Pin(yS, SIDE_E, "Q");
	pins[2].output = pins[2].state = true;
	if (hasQn()) {
	    pins[3] = new Pin(yR, SIDE_E, "Q");
	    pins[3].output = true;
	    pins[3].lineOver = true;
	}
    }

    int getPostCount() { return hasQn() ? 4 : 3; }
    int getVoltageSourceCount() { return hasQn() ? 2 : 1; }

    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	if (cspc2 < 1)
	    setSize(1);
	int minW = MIN_SX * cspc2;
	int minH = minSy() * cspc2;
	if (xx < x + minW)
	    xx = x + minW;
	if (yy < y + minH)
	    yy = y + minH;
	x2 = xx;
	y2 = yy;
	int nsx = Math.max(MIN_SX, (x2 - x) / cspc2);
	int nsy = Math.max(minSy(), (y2 - y) / cspc2);
	if (nsx != sizeX || nsy != sizeY)
	    applySize(nsx, nsy, true);
	else
	    setPoints();
    }

    void reset() {
	super.reset();
	if (pins != null && pins.length > 2) {
	    pins[2].value = false;
	    volts[2] = 0;
	    if (hasQn()) {
		pins[3].value = true;
		volts[3] = highVoltage;
	    }
	}
    }

    void execute() {
	if (justLoaded) {
	    justLoaded = false;
	    return;
	}
	boolean s = pins[0].value;
	boolean r = pins[1].value;
	boolean q = pins[2].value;
	boolean nq;
	if (resetDominant()) {
	    if (r) nq = false;
	    else if (s) nq = true;
	    else nq = q;
	} else {
	    if (s) nq = true;
	    else if (r) nq = false;
	    else nq = q;
	}
	writeOutput(2, nq);
	if (hasQn())
	    writeOutput(3, !nq);
    }

    /** FUP-Block: geschlossener Rahmen (drawThickPolygon), skalierte Schrift. */
    void drawChip(Graphics g) {
	int i;
	g.save();

	int x0 = Math.min(rectPointsX[0], rectPointsX[2]);
	int y0 = Math.min(rectPointsY[0], rectPointsY[1]);
	int x1 = Math.max(rectPointsX[0], rectPointsX[2]);
	int y1 = Math.max(rectPointsY[0], rectPointsY[1]);
	int bh = Math.max(1, y1 - y0);
	int bw = Math.max(1, x1 - x0);

	g.setColor(needsHighlight() ? selectColor : whiteColor);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	int fsz = Math.max(9, Math.min(18, Math.min(bw, bh) / 6));
	for (i = 0; i != getPostCount(); i++) {
	    Pin p = pins[i];
	    if (p.busZ > 0)
		continue;
	    boolean hi = volts[i] > getThreshold();
	    if (p.output)
		hi = p.value;
	    g.setColor(hi ? COL_HI : COL_LO);
	    drawThickLine(g, p.post, p.stub);
	    p.curcount = updateDotCount(p.current, p.curcount);
	    drawDots(g, p.stub, p.post, p.curcount);

	    g.setColor(needsHighlight() ? selectColor : whiteColor);
	    g.setFont(new Font("normal", 0, fsz));
	    String text = p.text;
	    int sw = (int) g.context.measureText(text).getWidth();
	    int asc = (int) g.currentFontSize;
	    int tx;
	    if (p.side == flippedXSide(SIDE_W))
		tx = p.textloc.x - Math.max(4, cspc - 4);
	    else if (p.side == flippedXSide(SIDE_E))
		tx = p.textloc.x + Math.max(4, cspc - 4) - sw;
	    else
		tx = p.textloc.x - sw / 2;
	    g.drawString(text, tx, p.textloc.y + asc / 3);
	    if (p.lineOver) {
		int ya = p.textloc.y - asc + asc / 3;
		g.drawLine(tx, ya, tx + sw, ya);
	    }
	}

	String title = resetDominant() ? "RS" : "SR";
	int tsz = Math.max(10, Math.min(20, bh / 5));
	g.setColor(needsHighlight() ? selectColor : whiteColor);
	g.setFont(new Font("normal", 0, tsz));
	int tw = (int) g.context.measureText(title).getWidth();
	g.drawString(title, (x0 + x1) / 2 - tw / 2, y0 + tsz + 2);

	drawPosts(g);
	g.restore();
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY;
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	int sx = xml.parseIntAttr("sx", sizeX);
	int sy = xml.parseIntAttr("sy", sizeY);
	applySize(sx, sy, true);
    }

    int getDumpType() { return 437; }
    String getXmlDumpType() { return "RSSpeicher"; }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.checkbox = new Checkbox(
		    Locale.LS("Reset-dominant (RS)"), resetDominant());
	    return ei;
	}
	if (n == 1) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.checkbox = new Checkbox(Locale.LS("Q̅-Ausgang"), hasQn());
	    return ei;
	}
	return super.getChipEditInfo(n);
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0) {
	    flags = ei.changeFlag(flags, FLAG_RESET_DOMINANT);
	    applySize(sizeX, Math.max(sizeY, minSy()), true);
	}
	if (n == 1) {
	    flags = ei.changeFlagInverted(flags, FLAG_NO_QN);
	    applySize(sizeX, Math.max(sizeY, minSy()), true);
	}
	super.setChipEditValue(n, ei);
    }
}
