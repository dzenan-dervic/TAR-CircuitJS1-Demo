/*
    TAR-Dervic EGT: Leistungsschütz gekoppelt (S301 / MOD-SCH-01), Dump 453
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Gekoppeltes Leistungsschütz: Spule A1/A2 + 3× Haupt + Hilfs 13/14.
 * Publiziert EGTSchuetzLink → externe Hilfskontakte mit gleicher Bezeichnung.
 */
class EGTLeistungsschuetzElm extends ChipElm implements EGTSchuetzCoilSource, EGTDesignatable {
    static final int DEF_SIZE_X = 11;
    static final int DEF_SIZE_Y = 5;
    static final int MIN_SIZE_X = DEF_SIZE_X;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;
    static final int N_A1 = 0, N_A2 = 1;
    static final int N_1 = 2, N_2 = 3, N_3 = 4, N_4 = 5, N_5 = 6, N_6 = 7;
    static final int N_13 = 8, N_14 = 9;
    static final String[] LABELS = {
	"A1", "A2", "1", "2", "3", "4", "5", "6", "13", "14"
    };

    String designation = "Q1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double nom_pow = 15;
    double nom_v = 230;
    double resistance = 230 * 230 / 15.;
    boolean energized;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();
    EGTSchuetzCoilL coilL = new EGTSchuetzCoilL();
    double poleCurrent[] = new double[4];
    double poleCurCount[] = new double[4];

    public EGTLeistungsschuetzElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTLeistungsschuetzElm(int xa, int ya, int xb, int yb, int f,
				  StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	if (st.hasMoreTokens()) {
	    String t = st.nextToken();
	    try {
		sizeX = Integer.parseInt(t);
		if (st.hasMoreTokens())
		    sizeY = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
		if (st.hasMoreTokens())
		    nom_pow = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nom_v = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    coilL.inductance = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
	if (!EGTSchuetzCoilL.lCustom(flags) || coilL.inductance <= 0)
	    coilL.inductance = EGTSchuetzCoilL.defaultHenries(nom_v, nom_pow);
    }

    public String schuetzDesignation() { return designation; }
    public boolean coilEnergized() { return energized; }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Leistungsschütz"; }

    int getPostCount() { return 10; }
    int getInternalNodeCount() { return 1; }
    int getVoltageSourceCount() { return 0; }
    int coilMid() { return getPostCount(); }

    void setupPins() {
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	pins = new Pin[getPostCount()];
	for (int i = 0; i < pins.length; i++) {
	    pins[i] = new Pin(0, SIDE_N, "");
	    pins[i].output = false;
	}
	placePins();
    }

    void layoutPins() {
	if (pins == null || pins.length != getPostCount())
	    setupPins();
	else
	    placePins();
    }

    void placePins() {
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	// 5 Spalten gleichmäßig: Spule | 1 | 3 | 5 | 13 — Randabstand 1
	int[] col = new int[5];
	EGTStyle.evenPinPos(5, sizeX, 1, col);
	int[] topIdx = { N_A1, N_1, N_3, N_5, N_13 };
	int[] botIdx = { N_A2, N_2, N_4, N_6, N_14 };
	for (int i = 0; i < 5; i++) {
	    pins[topIdx[i]].pos = col[i];
	    pins[topIdx[i]].side0 = SIDE_N;
	    pins[topIdx[i]].side = SIDE_N;
	    pins[topIdx[i]].output = false;
	    pins[botIdx[i]].pos = col[i];
	    pins[botIdx[i]].side0 = SIDE_S;
	    pins[botIdx[i]].side = SIDE_S;
	    pins[botIdx[i]].output = false;
	}
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    /** ChipElm.drag — feste Größe. */

    void setPoints() {
	super.setPoints();
	if (cspc < 1)
	    return;
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	int xs = sizeX * cspc2;
	int ys = sizeY * cspc2;
	setBbox(xr - 8, yr - 28, xr + xs + 100, yr + ys + 24);
    }

    boolean getConnection(int n1, int n2) {
	return EGTSchuetzLink.pinPair(n1, n2, N_A1, N_A2);
    }

    /** Offen: kein Durchgang, Massebezug 1e12. Paare in einer Matrix. */
    boolean getMatrixConnection(int n1, int n2) {
	int mid = coilMid();
	if ((n1 == mid && (n2 == N_A1 || n2 == N_A2))
		|| (n2 == mid && (n1 == N_A1 || n1 == N_A2)))
	    return true;
	if (getConnection(n1, n2))
	    return true;
	return EGTSchuetzLink.pinPair(n1, n2, N_1, N_2)
		|| EGTSchuetzLink.pinPair(n1, n2, N_3, N_4)
		|| EGTSchuetzLink.pinPair(n1, n2, N_5, N_6)
		|| EGTSchuetzLink.pinPair(n1, n2, N_13, N_14);
    }

    void stamp() {
	coilL.stamp(sim, nodes[N_A1], nodes[coilMid()], nodes[N_A2]);
	int[][] pairs = {
	    { N_1, N_2 }, { N_3, N_4 }, { N_5, N_6 }, { N_13, N_14 }
	};
	for (int i = 0; i < pairs.length; i++) {
	    sim.stampNonLinear(nodes[pairs[i][0]]);
	    sim.stampNonLinear(nodes[pairs[i][1]]);
	}
    }

    void startIteration() {
	coilL.startIteration(volts[N_A1], volts[coilMid()]);
    }

    void execute() {}

    void reset() {
	super.reset();
	hold.reset();
	coilL.reset();
	energized = false;
    }

    void doStep() {
	coilL.doStep(volts[N_A1], volts[coilMid()]);
	int[][] pairs = {
	    { N_1, N_2 }, { N_3, N_4 }, { N_5, N_6 }, { N_13, N_14 }
	};
	if (!energized) {
	    for (int i = 0; i < pairs.length; i++) {
		EGTStyle.stampOpenRef(sim, nodes[pairs[i][0]]);
		EGTStyle.stampOpenRef(sim, nodes[pairs[i][1]]);
	    }
	    return;
	}
	for (int i = 0; i < pairs.length; i++)
	    sim.stampResistor(nodes[pairs[i][0]], nodes[pairs[i][1]],
			      EGTSchuetzLink.R_ON);
    }

    void calculateCurrent() {
	current = coilL.calculateCurrent(volts[N_A1], volts[coilMid()]);
	double iNom = nom_pow / nom_v;
	energized = hold.update(current, iNom, sim.timeStep);
	EGTStyle.clearPinCurrents(pins);
	EGTStyle.pinCurrentThru(pins, N_A1, N_A2, current);
	if (!energized) {
	    poleCurrent[0] = poleCurrent[1] = poleCurrent[2] = poleCurrent[3] = 0;
	    return;
	}
	poleCurrent[0] = (volts[N_1] - volts[N_2]) / EGTSchuetzLink.R_ON;
	poleCurrent[1] = (volts[N_3] - volts[N_4]) / EGTSchuetzLink.R_ON;
	poleCurrent[2] = (volts[N_5] - volts[N_6]) / EGTSchuetzLink.R_ON;
	poleCurrent[3] = (volts[N_13] - volts[N_14]) / EGTSchuetzLink.R_ON;
	EGTStyle.pinCurrentThru(pins, N_1, N_2, poleCurrent[0]);
	EGTStyle.pinCurrentThru(pins, N_3, N_4, poleCurrent[1]);
	EGTStyle.pinCurrentThru(pins, N_5, N_6, poleCurrent[2]);
	EGTStyle.pinCurrentThru(pins, N_13, N_14, poleCurrent[3]);
    }

    void boxBounds(int out[]) {
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	out[0] = xr;
	out[1] = yr;
	out[2] = xr + sizeX * cspc2;
	out[3] = yr + sizeY * cspc2;
    }

    void drawChip(Graphics g) {
	g.save();
	try {
	    int[] b = new int[4];
	    boxBounds(b);
	    Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	    int left = b[0], top = b[1], right = b[2], bottom = b[3];

	    drawZh(g, left, top, right, bottom, stroke);

	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    void drawZh(Graphics g, int left, int top, int right, int bottom,
		Color stroke) {
	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	int h = bottom - top;
	int topY = top + Math.max(14, h * 22 / 100);
	int botY = bottom - Math.max(14, h * 22 / 100);
	int cy = (topY + botY) / 2;
	int coilCx = pins[N_A1].post.x;
	// Flaches Rechteck (nicht Würfel), mit Abstand zur linken Wand / nächster Spalte
	int wallPad = Math.max(4, cspc);
	int maxHalfL = Math.max(5, coilCx - left - wallPad);
	int nextX = pins[N_1].post.x;
	int maxHalfR = Math.max(5, (nextX - coilCx) / 2 - 1);
	int maxHalf = Math.min(maxHalfL, maxHalfR);
	int coilH = Math.max(8, h * 14 / 100);
	int coilW = Math.min(2 * maxHalf, Math.max(coilH + 8, coilH * 5 / 3));

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(coilCx, pins[N_A1].post.y, coilCx, cy - coilH / 2);
	g.drawLine(coilCx, cy + coilH / 2, coilCx, pins[N_A2].post.y);
	EGTStyle.drawCoilRect(g, coilCx, cy, coilW, coilH, true, stroke);

	int[] topPins = { N_1, N_3, N_5 };
	int[] botPins = { N_2, N_4, N_6 };
	for (int i = 0; i < 3; i++) {
	    int px = pins[topPins[i]].post.x;
	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.drawLine(px, pins[topPins[i]].post.y, px, topY);
	    g.drawLine(px, botY, px, pins[botPins[i]].post.y);
	    EGTStyle.drawContactorNoContact(g, px, topY, botY, energized,
					    stroke);
	}
	EGTStyle.drawMechLink(g, pins[N_1].post.x, pins[N_5].post.x, cy,
			      stroke);

	int ax = pins[N_13].post.x;
	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(ax, pins[N_13].post.y, ax, topY);
	g.drawLine(ax, botY, ax, pins[N_14].post.y);
	EGTStyle.drawNoContact(g, ax, topY, botY, energized, stroke, false);
	EGTStyle.drawMechLink(g, pins[N_5].post.x, ax, cy, stroke);

	g.setColor(whiteColor);
	for (int i = 0; i < 10; i++)
	    EGTStyle.drawPinLabelBeside(g, this, pins[i].post.x, pins[i].post.y,
					LABELS[i], whiteColor);
	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation,
		energized ? "angezogen" : "abgefallen", null);
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);

	if (energized) {
	    curcount = updateDotCount(current, curcount);
	    drawDots(g, pins[N_A1].post, new Point(coilCx, cy - coilH / 2),
		     curcount);
	    drawDots(g, new Point(coilCx, cy + coilH / 2), pins[N_A2].post,
		     curcount);
	    for (int i = 0; i < 3; i++) {
		poleCurCount[i] = updateDotCount(poleCurrent[i],
						 poleCurCount[i]);
		int px = pins[topPins[i]].post.x;
		drawDots(g, pins[topPins[i]].post, new Point(px, topY),
			 poleCurCount[i]);
		drawDots(g, new Point(px, botY), pins[botPins[i]].post,
			 poleCurCount[i]);
	    }
	    poleCurCount[3] = updateDotCount(poleCurrent[3], poleCurCount[3]);
	    drawDots(g, pins[N_13].post, new Point(ax, topY), poleCurCount[3]);
	    drawDots(g, new Point(ax, botY), pins[N_14].post, poleCurCount[3]);
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nom_pow + " "
		+ nom_v + " " + coilL.inductance + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "np", nom_pow);
	XMLSerializer.dumpAttr(elem, "nv", nom_v);
	XMLSerializer.dumpAttr(elem, "L", coilL.inductance);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	coilL.inductance = xml.parseDoubleAttr("L", coilL.inductance);
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 453; }
    String getXmlDumpType() { return "EGTLeistungsschuetz"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = energized ? "angezogen" : "abgefallen";
	arr[2] = "A1 = " + getVoltageText(volts[N_A1]);
	arr[3] = "A2 = " + getVoltageText(volts[N_A2]);
	arr[4] = "Pcoil = "
		+ getUnitText(current * (volts[N_A1] - volts[N_A2]), "W");
	arr[5] = "L = " + getUnitText(coilL.inductance, "H")
		+ (EGTSchuetzCoilL.lCustom(flags) ? " (frei)" : " (auto)");
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EGTSchuetzCoilL.modelEditInfo(flags, nom_v, nom_pow);
	if (n == 2)
	    return new EditInfo("Nennleistung Spule (VA)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 3)
	    return new EditInfo("Nennspannung Spule (V)", nom_v, 0, 0)
		    .setPositive();
	if (n == 4) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Hilfskontakt hinzufügen"));
	    return ei;
	}
	if (n == 5)
	    return EGTSchuetzCoilL.inductanceEditInfo(flags, coilL.inductance);
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1 && ei.choice != null) {
	    double pu[] = { nom_pow, nom_v };
	    flags = EGTSchuetzCoilL.applyPreset(ei.choice.getSelectedIndex(),
					       pu, coilL, flags);
	    nom_pow = pu[0];
	    nom_v = pu[1];
	    updateResistance();
	    ei.newDialog = true;
	}
	if (n == 2 && ei.value > 0) {
	    nom_pow = ei.value;
	    if (!EGTSchuetzCoilL.lCustom(flags))
		coilL.inductance = 0;
	    updateResistance();
	}
	if (n == 3 && ei.value > 0) {
	    nom_v = ei.value;
	    if (!EGTSchuetzCoilL.lCustom(flags))
		coilL.inductance = 0;
	    updateResistance();
	}
	if (n == 4)
	    EGTSchuetzLink.placeHilfskontakt(this, designation);
	if (n == 5 && ei.value > 0) {
	    coilL.inductance = ei.value;
	    flags |= EGTStyle.FLAG_L_CUSTOM;
	    updateResistance();
	    ei.newDialog = true;
	}
    }
}
