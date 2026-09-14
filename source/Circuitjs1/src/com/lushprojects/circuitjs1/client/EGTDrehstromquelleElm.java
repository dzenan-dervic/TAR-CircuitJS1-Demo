/*
    Copyright (C) Paul Falstad and Iain Sharp
    TAR-Dervic EGT extension: Drehstrom-Spannungsquelle L1/L2/L3/N/PE, I201 / M501

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

/**
 * EGT three-phase supply: ZH box L1/L2/L3/N/PE or Übersicht circle with 3~.
 * Phase voltage U_LN (default 230 V); 120° sequence L1-L2-L3; N/PE = 0 V.
 */
class EGTDrehstromquelleElm extends ChipElm implements EGTDesignatable {
    static final String[] PIN_LABELS = { "L1", "L2", "L3", "N", "PE" };
    static final Color[] PIN_COLORS = {
	EGTStyle.COL_L1, EGTStyle.COL_L2, EGTStyle.COL_L3, EGTStyle.COL_N, null
    };

    double voltageRMS = 230;
    double frequency = 50;
    String designation = "U3";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTDrehstromquelleElm(int xx, int yy) {
	super(xx, yy);
    }

    public EGTDrehstromquelleElm(int xa, int ya, int xb, int yb, int f,
				 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	if (st.hasMoreTokens())
	    voltageRMS = Double.parseDouble(st.nextToken());
	if (st.hasMoreTokens())
	    frequency = Double.parseDouble(st.nextToken());
	if (st.hasMoreTokens())
	    designation = CustomLogicModel.unescape(st.nextToken());
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }
    boolean uebersicht() { return (flags & EGTStyle.FLAG_UEBERSICHT) != 0; }

    String getChipName() { return "EGT-Drehstromquelle"; }

    void setupPins() {
	// gleiche Rasterlogik wie 1ph (size 1/2 per Drag); nur mehr Zeilen für 5 Pins
	sizeX = 2;
	sizeY = 5;
	pins = new Pin[5];
	for (int i = 0; i < 5; i++) {
	    pins[i] = new Pin(i, SIDE_E, "");
	    pins[i].output = true;
	}
    }

    int getPostCount() { return 5; }
    int getVoltageSourceCount() { return 5; }

    double getPeakVoltage() { return voltageRMS * Math.sqrt(2); }

    /** Phase voltages: L1 @ 0°, L2 @ −120°, L3 @ +120° (positive sequence). */
    double getPhaseVoltage(int phase) {
	if (doDcAnalysis())
	    return 0;
	double w = 2 * pi * sim.t * frequency;
	double shift = 0;
	if (phase == 1)
	    shift = -2 * pi / 3;
	else if (phase == 2)
	    shift = 2 * pi / 3;
	return Math.sin(w + shift) * getPeakVoltage();
    }

    void startIteration() {}
    void execute() {}

    void doStep() {
	for (int i = 0; i < 3; i++)
	    sim.updateVoltageSource(CircuitNode.ground, nodes[i], pins[i].voltSource,
				    getPhaseVoltage(i));
	sim.updateVoltageSource(CircuitNode.ground, nodes[3], pins[3].voltSource, 0);
	sim.updateVoltageSource(CircuitNode.ground, nodes[4], pins[4].voltSource, 0);
    }

    void drawChip(Graphics g) {
	g.save();
	int left = Math.min(rectPointsX[0], rectPointsX[2]);
	int top = Math.min(rectPointsY[0], rectPointsY[1]);
	int right = Math.max(rectPointsX[0], rectPointsX[2]);
	int bottom = Math.max(rectPointsY[0], rectPointsY[1]);
	Color stroke = needsHighlight() ? selectColor : lightGrayColor;

	if (uebersicht())
	    drawUebersicht(g, left, top, right, bottom, stroke);
	else
	    drawZh(g, left, top, right, stroke);

	for (int i = 0; i < 5; i++) {
	    Pin p = pins[i];
	    Color base = PIN_COLORS[i] != null ? PIN_COLORS[i]
		    : EGTStyle.COL_PE_G;
	    EGTStyle.drawLiveConductor(g, p.stub.x, p.stub.y, p.post.x, p.post.y,
				       base, volts[i], 3.5, i == 4);
	    p.curcount = updateDotCount(p.current, p.curcount);
	    drawDots(g, p.stub, p.post, p.curcount);
	}
	drawPosts(g);
	g.restore();
    }

    void drawZh(Graphics g, int left, int top, int right, Color stroke) {
	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	Font fLabel = new Font("normal", 0, 9 * csize);
	g.setFont(fLabel);
	g.setColor(whiteColor);
	int titleW = EGTStyle.measureDesigNote(g, this, designation);
	int titleY = EGTNote.of(this).length() > 0 ? top - 24 : top - 10;
	EGTStyle.drawDesigAndNote(g, this, designation,
				 (left + right) / 2 - titleW / 2, titleY);
	adjustBbox(left, titleY - 12, right, top);

	int labelPad = 10;
	for (int i = 0; i < 5; i++) {
	    Pin p = pins[i];
	    int y = p.stub.y;
	    g.setColor(whiteColor);
	    g.setFont(fLabel);
	    int tw = (int) g.context.measureText(PIN_LABELS[i]).getWidth();
	    g.drawString(PIN_LABELS[i], left + labelPad, y + 4);

	    int xStart = left + labelPad + tw + 5;
	    Color base = PIN_COLORS[i] != null ? PIN_COLORS[i]
		    : EGTStyle.COL_PE_G;
	    EGTStyle.drawLiveConductor(g, xStart, y, p.stub.x, y, base,
				       volts[i], 3.5, i == 4);
	}
    }

    void drawUebersicht(Graphics g, int left, int top, int right, int bottom,
			Color stroke) {
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int r = Math.min(right - left, bottom - top) / 2 - 4;
	if (r < 12)
	    r = 12;
	EGTStyle.drawOverviewSource(g, this, cx, cy, r, true, designation, stroke);
	adjustBbox(left, cy - r - 22, right, bottom);
    }

    String dump() {
	return super.dump() + " " + voltageRMS + " " + frequency + " "
		+ CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "urms", voltageRMS);
	XMLSerializer.dumpAttr(elem, "fr", frequency);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	voltageRMS = xml.parseDoubleAttr("urms", voltageRMS);
	frequency = xml.parseDoubleAttr("fr", frequency);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
    }

    int getDumpType() { return 439; }
    String getXmlDumpType() { return "EGTDrehstromquelle"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	arr[1] = "U_LN = " + getUnitText(voltageRMS, "V");
	arr[2] = "f = " + getUnitText(frequency, "Hz");
	arr[3] = "L1 = " + getVoltageText(volts[0]);
	arr[4] = "L2 = " + getVoltageText(volts[1]);
	arr[5] = "L3 = " + getVoltageText(volts[2]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("U_LN eff (V)", voltageRMS).setPositive();
	if (n == 1)
	    return new EditInfo("Frequenz (Hz)", frequency).setPositive();
	if (n == 2)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 3)
	    return EditInfo.createCheckbox("Übersichtsdarstellung", uebersicht());
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.value > 0)
	    voltageRMS = ei.value;
	if (n == 1 && ei.value > 0)
	    frequency = ei.value;
	if (n == 2)
	    designation = ei.textf.getText();
	if (n == 3)
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_UEBERSICHT);
    }
}
