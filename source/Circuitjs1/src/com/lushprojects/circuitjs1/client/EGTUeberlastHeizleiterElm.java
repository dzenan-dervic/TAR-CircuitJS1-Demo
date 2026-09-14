/*
    TAR-Dervic EGT: Überlastrelais nur Heizleiter 1–6 (MOD-SCH-03), Dump 482
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Drei Heizleiter 1/3/5–2/4/6, immer leitend. Auslösung nur über Kopplung
 * gleicher Bezeichnung zu getrennten Öffnern/Schließern.
 */
class EGTUeberlastHeizleiterElm extends ChipElm implements EGTUeberlastSource {
    static final int DEF_SIZE_X = 7;
    static final int DEF_SIZE_Y = 4;
    static final int N_1 = 0, N_2 = 1, N_3 = 2, N_4 = 3, N_5 = 4, N_6 = 5;
    static final String[] LABELS = { "1", "2", "3", "4", "5", "6" };
    static final double DEF_ISET = 4;
    static final double DEF_TRIP_T = 5;
    static final double R_HEAT = EGTUeberlastrelaisElm.R_HEAT;
    static final double MAX_WALL_DT = EGTUeberlastrelaisElm.MAX_WALL_DT;

    String designation = "F1";
    String note = "";
    double iSet = DEF_ISET;
    double tripTime = DEF_TRIP_T;
    boolean tripped;
    double thermal;
    long lastWallMs;
    double poleCurrent[] = new double[3];
    double poleCurCount[] = new double[3];

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    public boolean ueberlastTripped() { return tripped; }

    public EGTUeberlastHeizleiterElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    }

    public EGTUeberlastHeizleiterElm(int xa, int ya, int xb, int yb, int f,
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
		    iSet = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
		    tripTime = Double.parseDouble(st.nextToken());
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	if (iSet <= 0)
	    iSet = DEF_ISET;
	if (tripTime <= 0)
	    tripTime = DEF_TRIP_T;
	layoutPins();
	syncEndpoints();
	note = EGTNote.readOptional(st);
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return false; }

    String getChipName() { return "EGT-Überlastrelais (Heizleiter)"; }

    int getPostCount() { return 6; }
    int getVoltageSourceCount() { return 0; }

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
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	int[] col = new int[3];
	EGTStyle.evenPinPos(3, sizeX, 1, col);
	int[] topIdx = { N_1, N_3, N_5 };
	int[] botIdx = { N_2, N_4, N_6 };
	for (int i = 0; i < 3; i++) {
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
	return EGTSchuetzLink.pinPair(n1, n2, N_1, N_2)
		|| EGTSchuetzLink.pinPair(n1, n2, N_3, N_4)
		|| EGTSchuetzLink.pinPair(n1, n2, N_5, N_6);
    }

    void stamp() {
	sim.stampResistor(nodes[N_1], nodes[N_2], R_HEAT);
	sim.stampResistor(nodes[N_3], nodes[N_4], R_HEAT);
	sim.stampResistor(nodes[N_5], nodes[N_6], R_HEAT);
    }

    void reset() {
	super.reset();
	tripped = false;
	thermal = 0;
	lastWallMs = 0;
	poleCurrent[0] = poleCurrent[1] = poleCurrent[2] = 0;
    }

    void clearTrip() {
	tripped = false;
	thermal = 0;
	lastWallMs = System.currentTimeMillis();
    }

    void tickThermal() {
	long now = System.currentTimeMillis();
	boolean running = CircuitElm.app != null
		&& CircuitElm.app.simIsRunning();
	double dt = 0;
	if (running && lastWallMs != 0) {
	    dt = (now - lastWallMs) / 1000.0;
	    if (dt < 0)
		dt = 0;
	    if (dt > MAX_WALL_DT)
		dt = MAX_WALL_DT;
	}
	lastWallMs = now;
	if (!running || dt <= 0 || tripped)
	    return;
	double iMax = Math.abs(poleCurrent[0]);
	if (Math.abs(poleCurrent[1]) > iMax)
	    iMax = Math.abs(poleCurrent[1]);
	if (Math.abs(poleCurrent[2]) > iMax)
	    iMax = Math.abs(poleCurrent[2]);
	double r = iMax / Math.max(iSet, 1e-6);
	if (r >= 1.2) {
	    thermal += dt / tripTime;
	    if (thermal >= 1) {
		thermal = 1;
		tripped = true;
	    }
	} else if (r < 1.0) {
	    thermal -= dt / Math.max(tripTime * 4, 1);
	    if (thermal < 0)
		thermal = 0;
	}
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	poleCurrent[0] = (volts[N_1] - volts[N_2]) / R_HEAT;
	poleCurrent[1] = (volts[N_3] - volts[N_4]) / R_HEAT;
	poleCurrent[2] = (volts[N_5] - volts[N_6]) / R_HEAT;
	tickThermal();
	EGTStyle.pinCurrentThru(pins, N_1, N_2, poleCurrent[0]);
	EGTStyle.pinCurrentThru(pins, N_3, N_4, poleCurrent[1]);
	EGTStyle.pinCurrentThru(pins, N_5, N_6, poleCurrent[2]);
	current = poleCurrent[0];
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
	    g.setColor(stroke);
	    drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	    int h = bottom - top;
	    int topY = top + Math.max(12, h * 20 / 100);
	    int botY = bottom - Math.max(12, h * 20 / 100);
	    int cy = (topY + botY) / 2;
	    int[] heatTop = { N_1, N_3, N_5 };
	    int[] heatBot = { N_2, N_4, N_6 };
	    for (int i = 0; i < 3; i++) {
		int px = pins[heatTop[i]].post.x;
		g.setColor(stroke);
		g.setLineWidth(2.5);
		g.drawLine(px, pins[heatTop[i]].post.y, px, topY);
		g.drawLine(px, botY, px, pins[heatBot[i]].post.y);
		EGTStyle.drawBimetalHeater(g, px, topY, botY, stroke);
	    }
	    EGTStyle.drawMechLink(g, pins[N_1].post.x, pins[N_5].post.x, cy,
				  stroke);

	    g.setColor(whiteColor);
	    for (int i = 0; i < 6; i++)
		EGTStyle.drawPinLabelBeside(g, this, pins[i].post.x,
					    pins[i].post.y, LABELS[i],
					    whiteColor);
	    String heatTxt = tripped ? "ausgelöst" : "bereit";
	    int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right,
		    bottom, whiteColor, designation, heatTxt, null);
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    for (int i = 0; i < 3; i++) {
		poleCurCount[i] = updateDotCount(poleCurrent[i],
						 poleCurCount[i]);
		int px = pins[heatTop[i]].post.x;
		drawDots(g, pins[heatTop[i]].post, new Point(px, topY),
			 poleCurCount[i]);
		drawDots(g, new Point(px, botY), pins[heatBot[i]].post,
			 poleCurCount[i]);
	    }
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + iSet + " "
		+ tripTime + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "iset", iSet);
	XMLSerializer.dumpAttr(elem, "tt", tripTime);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	iSet = xml.parseDoubleAttr("iset", iSet);
	tripTime = xml.parseDoubleAttr("tt", tripTime);
	if (iSet <= 0)
	    iSet = DEF_ISET;
	if (tripTime <= 0)
	    tripTime = DEF_TRIP_T;
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 482; }
    String getXmlDumpType() { return "EGTUeberlastHeizleiter"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = tripped ? "ausgelöst — Reset im Edit" : "bereit";
	arr[2] = "Iset = " + getCurrentText(iSet)
		+ ", t = " + getUnitText(tripTime, "s");
	arr[3] = "I1 = " + getCurrentText(poleCurrent[0])
		+ ", I2 = " + getCurrentText(poleCurrent[1])
		+ ", I3 = " + getCurrentText(poleCurrent[2]);
	arr[4] = "Wärme " + (int) Math.round(thermal * 100) + " %";
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return new EditInfo("Einstellstrom Iset (A)", iSet, 0, 0)
		    .setPositive();
	if (n == 2)
	    return new EditInfo("Auslösezeit bei 1,2×Iset (s)", tripTime, 0, 0)
		    .setPositive();
	if (n == 3 && tripped) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Auslösung zurücksetzen"));
	    return ei;
	}
	int k = tripped ? n : n + 1;
	if (k == 4) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Öffner hinzufügen"));
	    return ei;
	}
	if (k == 5) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.button = new Button(Locale.LS("Schließer hinzufügen"));
	    return ei;
	}
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.textf != null)
	    designation = ei.textf.getText();
	if (n == 1) {
	    iSet = ei.value;
	    if (iSet <= 0)
		iSet = DEF_ISET;
	}
	if (n == 2) {
	    tripTime = ei.value;
	    if (tripTime <= 0)
		tripTime = DEF_TRIP_T;
	}
	if (n == 3 && ei.button != null && tripped) {
	    clearTrip();
	    ei.newDialog = true;
	    return;
	}
	int k = tripped ? n : n + 1;
	if (k == 4 && ei.button != null)
	    EGTUeberlastLink.placeKontakt(this, designation, true);
	if (k == 5 && ei.button != null)
	    EGTUeberlastLink.placeKontakt(this, designation, false);
    }
}
