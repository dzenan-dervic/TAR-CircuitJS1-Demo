/*
    TAR-Dervic EGT: Hilfsschütz gekoppelt (S301 / MOD-SCH-02), Dump 457
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Gekoppeltes Hilfsschütz: Spule A1/A2 + n Schließer + m Öffner (0–4).
 * Publiziert EGTSchuetzLink → externe Hilfskontakte mit gleicher Bezeichnung.
 */
class EGTHilfsschuetzElm extends ChipElm implements EGTSchuetzCoilSource, EGTDesignatable {
    static final int MAX_CONTACTS = 4;
    static final int DEF_N_NO = 2;
    static final int DEF_N_NC = 2;
    static final int DEF_SIZE_Y = 5;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;
    static final int N_A1 = 0, N_A2 = 1;

    /** DIN Schließer-Paare */
    static final String[] NO_TOP = { "13", "33", "53", "73" };
    static final String[] NO_BOT = { "14", "34", "54", "74" };
    /** DIN Öffner-Paare */
    static final String[] NC_TOP = { "21", "31", "41", "51" };
    static final String[] NC_BOT = { "22", "32", "42", "52" };

    String designation = "K1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    int nNo = DEF_N_NO;
    int nNc = DEF_N_NC;
    double nom_pow = 15;
    double nom_v = 230;
    double resistance = 230 * 230 / 15.;
    boolean energized;
    EGTSchuetzCoilHold hold = new EGTSchuetzCoilHold();
    EGTSchuetzCoilL coilL = new EGTSchuetzCoilL();
    double contactCurrent[] = new double[MAX_CONTACTS * 2];
    double contactCurCount[] = new double[MAX_CONTACTS * 2];

    public EGTHilfsschuetzElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    public EGTHilfsschuetzElm(int xa, int ya, int xb, int yb, int f,
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
		    nNo = (int) Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    nNc = (int) Double.parseDouble(st.nextToken());
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
	clampCounts();
	updateSizeX();
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

    void clampCounts() {
	if (nNo < 0)
	    nNo = 0;
	if (nNo > MAX_CONTACTS)
	    nNo = MAX_CONTACTS;
	if (nNc < 0)
	    nNc = 0;
	if (nNc > MAX_CONTACTS)
	    nNc = MAX_CONTACTS;
    }

    int contactCount() { return nNo + nNc; }

    void updateSizeX() {
	int cols = 1 + contactCount();
	if (cols < 1)
	    cols = 1;
	// Kompakt wie Leistungsschütz: 5 Spalten → 11
	sizeX = Math.max(3, 2 * cols + 1);
    }

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
	if (!EGTSchuetzCoilL.lCustom(flags) || coilL.inductance <= 0)
	    coilL.inductance = EGTSchuetzCoilL.defaultHenries(nom_v, nom_pow);
    }

    /** Pin-Index oben für Kontakt i (0..nNo-1 NO, dann NC). */
    int contactTopPin(int i) { return 2 + i * 2; }
    int contactBotPin(int i) { return 3 + i * 2; }

    boolean contactIsNc(int i) { return i >= nNo; }

    boolean contactClosed(int i) {
	return contactIsNc(i) ? !energized : energized;
    }

    String contactTopLab(int i) {
	return contactIsNc(i) ? NC_TOP[i - nNo] : NO_TOP[i];
    }

    String contactBotLab(int i) {
	return contactIsNc(i) ? NC_BOT[i - nNo] : NO_BOT[i];
    }

    public String schuetzDesignation() { return designation; }
    public boolean coilEnergized() { return energized; }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Hilfsschütz"; }

    int getPostCount() { return 2 + contactCount() * 2; }
    int getInternalNodeCount() { return 1; }
    int getVoltageSourceCount() { return 0; }
    int coilMid() { return getPostCount(); }

    void setupPins() {
	updateSizeX();
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
	updateSizeX();
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	int cols = 1 + contactCount();
	int[] col = new int[cols];
	EGTStyle.evenPinPos(cols, sizeX, 1, col);
	pins[N_A1].pos = col[0];
	pins[N_A1].side0 = SIDE_N;
	pins[N_A1].side = SIDE_N;
	pins[N_A1].output = false;
	pins[N_A2].pos = col[0];
	pins[N_A2].side0 = SIDE_S;
	pins[N_A2].side = SIDE_S;
	pins[N_A2].output = false;
	for (int i = 0; i < contactCount(); i++) {
	    int c = col[1 + i];
	    int t = contactTopPin(i);
	    int b = contactBotPin(i);
	    pins[t].pos = c;
	    pins[t].side0 = SIDE_N;
	    pins[t].side = SIDE_N;
	    pins[t].output = false;
	    pins[b].pos = c;
	    pins[b].side0 = SIDE_S;
	    pins[b].side = SIDE_S;
	    pins[b].output = false;
	}
    }

    void syncEndpoints() {
	if (cspc2 < 1)
	    setSize(1);
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    void applyContactCounts() {
	clampCounts();
	setupPins();
	allocNodes();
	syncEndpoints();
    }

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

    boolean getMatrixConnection(int n1, int n2) {
	int mid = coilMid();
	if ((n1 == mid && (n2 == N_A1 || n2 == N_A2))
		|| (n2 == mid && (n1 == N_A1 || n1 == N_A2)))
	    return true;
	if (getConnection(n1, n2))
	    return true;
	for (int i = 0; i < contactCount(); i++) {
	    if (EGTSchuetzLink.pinPair(n1, n2, contactTopPin(i),
				      contactBotPin(i)))
		return true;
	}
	return false;
    }

    void stamp() {
	coilL.stamp(sim, nodes[N_A1], nodes[coilMid()], nodes[N_A2]);
	for (int i = 0; i < contactCount(); i++) {
	    sim.stampNonLinear(nodes[contactTopPin(i)]);
	    sim.stampNonLinear(nodes[contactBotPin(i)]);
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
	for (int i = 0; i < contactCount(); i++) {
	    if (!contactClosed(i)) {
		EGTStyle.stampOpenRef(sim, nodes[contactTopPin(i)]);
		EGTStyle.stampOpenRef(sim, nodes[contactBotPin(i)]);
		continue;
	    }
	    sim.stampResistor(nodes[contactTopPin(i)], nodes[contactBotPin(i)],
			      EGTSchuetzLink.R_ON);
	}
    }

    void calculateCurrent() {
	current = coilL.calculateCurrent(volts[N_A1], volts[coilMid()]);
	double iNom = nom_pow / nom_v;
	energized = hold.update(current, iNom, sim.timeStep);
	EGTStyle.clearPinCurrents(pins);
	EGTStyle.pinCurrentThru(pins, N_A1, N_A2, current);
	for (int i = 0; i < contactCount(); i++) {
	    if (!contactClosed(i)) {
		contactCurrent[i] = 0;
		continue;
	    }
	    contactCurrent[i] =
		    (volts[contactTopPin(i)] - volts[contactBotPin(i)])
			    / EGTSchuetzLink.R_ON;
	    EGTStyle.pinCurrentThru(pins, contactTopPin(i), contactBotPin(i),
				    contactCurrent[i]);
	}
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
	    drawZh(g, b[0], b[1], b[2], b[3], stroke);
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
	int wallPad = Math.max(4, cspc);
	int maxHalfL = Math.max(5, coilCx - left - wallPad);
	int maxHalfR = maxHalfL;
	if (contactCount() > 0) {
	    int nextX = pins[contactTopPin(0)].post.x;
	    maxHalfR = Math.max(5, (nextX - coilCx) / 2 - 1);
	} else {
	    maxHalfR = Math.max(5, right - coilCx - wallPad);
	}
	int maxHalf = Math.min(maxHalfL, maxHalfR);
	int coilH = Math.max(8, h * 14 / 100);
	int coilW = Math.min(2 * maxHalf, Math.max(coilH + 8, coilH * 5 / 3));

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(coilCx, pins[N_A1].post.y, coilCx, cy - coilH / 2);
	g.drawLine(coilCx, cy + coilH / 2, coilCx, pins[N_A2].post.y);
	EGTStyle.drawCoilRect(g, coilCx, cy, coilW, coilH, true, stroke);

	int n = contactCount();
	for (int i = 0; i < n; i++) {
	    int px = pins[contactTopPin(i)].post.x;
	    boolean closed = contactClosed(i);
	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.drawLine(px, pins[contactTopPin(i)].post.y, px, topY);
	    g.drawLine(px, botY, px, pins[contactBotPin(i)].post.y);
	    if (contactIsNc(i))
		EGTStyle.drawNcContact(g, px, topY, botY, closed, stroke);
	    else
		EGTStyle.drawNoContact(g, px, topY, botY, closed, stroke);
	}
	if (n > 0) {
	    int leftC = pins[contactTopPin(0)].post.x;
	    int rightC = pins[contactTopPin(n - 1)].post.x;
	    EGTStyle.drawMechLink(g, leftC, rightC, cy, stroke);
	    if (n >= 1)
		EGTStyle.drawMechLink(g, coilCx, leftC, cy, stroke);
	}

	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_A1].post.x, pins[N_A1].post.y,
				    "A1", whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, pins[N_A2].post.x, pins[N_A2].post.y,
				    "A2", whiteColor);
	for (int i = 0; i < n; i++) {
	    EGTStyle.drawPinLabelBeside(g, this,
		    pins[contactTopPin(i)].post.x,
		    pins[contactTopPin(i)].post.y, contactTopLab(i),
		    whiteColor);
	    EGTStyle.drawPinLabelBeside(g, this,
		    pins[contactBotPin(i)].post.x,
		    pins[contactBotPin(i)].post.y, contactBotLab(i),
		    whiteColor);
	}
	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation,
		energized ? "angezogen" : "abgefallen",
		nNo + "S / " + nNc + "Ö");
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);

	if (energized || n > 0) {
	    if (energized) {
		curcount = updateDotCount(current, curcount);
		drawDots(g, pins[N_A1].post, new Point(coilCx, cy - coilH / 2),
			 curcount);
		drawDots(g, new Point(coilCx, cy + coilH / 2), pins[N_A2].post,
			 curcount);
	    }
	    for (int i = 0; i < n; i++) {
		if (!contactClosed(i))
		    continue;
		contactCurCount[i] = updateDotCount(contactCurrent[i],
						    contactCurCount[i]);
		int px = pins[contactTopPin(i)].post.x;
		drawDots(g, pins[contactTopPin(i)].post, new Point(px, topY),
			 contactCurCount[i]);
		drawDots(g, new Point(px, botY), pins[contactBotPin(i)].post,
			 contactCurCount[i]);
	    }
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nNo + " " + nNc
		+ " " + nom_pow + " " + nom_v + " " + coilL.inductance + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "nno", nNo);
	XMLSerializer.dumpAttr(elem, "nnc", nNc);
	XMLSerializer.dumpAttr(elem, "np", nom_pow);
	XMLSerializer.dumpAttr(elem, "nv", nom_v);
	XMLSerializer.dumpAttr(elem, "L", coilL.inductance);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nNo = (int) xml.parseDoubleAttr("nno", nNo);
	nNc = (int) xml.parseDoubleAttr("nnc", nNc);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	coilL.inductance = xml.parseDoubleAttr("L", coilL.inductance);
	if (nom_pow <= 0)
	    nom_pow = 15;
	if (nom_v <= 0)
	    nom_v = 230;
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	setSize(1);
	updateResistance();
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 457; }
    String getXmlDumpType() { return "EGTHilfsschuetz"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = energized ? "angezogen" : "abgefallen";
	arr[2] = "Kontakte = " + nNo + " Schließer / " + nNc + " Öffner";
	arr[3] = "A1 = " + getVoltageText(volts[N_A1]);
	arr[4] = "A2 = " + getVoltageText(volts[N_A2]);
	arr[5] = "L = " + getUnitText(coilL.inductance, "H")
		+ (EGTSchuetzCoilL.lCustom(flags) ? " (frei)" : " (auto)");
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Anzahl Schließer (0–4)", nNo, 0, MAX_CONTACTS)
		    .setDimensionless();
	if (n == 2)
	    return new EditInfo("Anzahl Öffner (0–4)", nNc, 0, MAX_CONTACTS)
		    .setDimensionless();
	if (n == 3)
	    return EGTSchuetzCoilL.modelEditInfo(flags, nom_v, nom_pow);
	if (n == 4)
	    return new EditInfo("Nennleistung Spule (VA)", nom_pow, 0, 0)
		    .setPositive();
	if (n == 5)
	    return new EditInfo("Nennspannung Spule (V)", nom_v, 0, 0)
		    .setPositive();
	if (n == 6) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Hilfskontakt hinzufügen"));
	    return ei;
	}
	if (n == 7)
	    return EGTSchuetzCoilL.inductanceEditInfo(flags, coilL.inductance);
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    nNo = (int) Math.round(ei.value);
	    applyContactCounts();
	}
	if (n == 2) {
	    nNc = (int) Math.round(ei.value);
	    applyContactCounts();
	}
	if (n == 3 && ei.choice != null) {
	    double pu[] = { nom_pow, nom_v };
	    flags = EGTSchuetzCoilL.applyPreset(ei.choice.getSelectedIndex(),
					       pu, coilL, flags);
	    nom_pow = pu[0];
	    nom_v = pu[1];
	    updateResistance();
	    ei.newDialog = true;
	}
	if (n == 4 && ei.value > 0) {
	    nom_pow = ei.value;
	    if (!EGTSchuetzCoilL.lCustom(flags))
		coilL.inductance = 0;
	    updateResistance();
	}
	if (n == 5 && ei.value > 0) {
	    nom_v = ei.value;
	    if (!EGTSchuetzCoilL.lCustom(flags))
		coilL.inductance = 0;
	    updateResistance();
	}
	if (n == 6)
	    EGTSchuetzLink.placeHilfskontakt(this, designation);
	if (n == 7 && ei.value > 0) {
	    coilL.inductance = ei.value;
	    flags |= EGTStyle.FLAG_L_CUSTOM;
	    updateResistance();
	    ei.newDialog = true;
	}
    }
}
