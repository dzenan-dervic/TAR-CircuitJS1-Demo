/*
    TAR-Dervic EGT: Hilfsschalterblock (S301), Dump 458
*/

package com.lushprojects.circuitjs1.client;

/**
 * Mechanischer Hilfskontaktblock ohne Spule: n Schließer + m Öffner (0–4).
 * Zustand über EGTSchuetzLink / gleiche Bezeichnung wie Spule oder Schütz.
 */
class EGTHilfsschalterblockElm extends ChipElm implements EGTDesignatable {
    static final int MAX_CONTACTS = 4;
    static final int DEF_N_NO = 2;
    static final int DEF_N_NC = 2;
    static final int DEF_SIZE_Y = 5;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;

    static final String[] NO_TOP = { "13", "33", "53", "73" };
    static final String[] NO_BOT = { "14", "34", "54", "74" };
    static final String[] NC_TOP = { "21", "31", "41", "51" };
    static final String[] NC_BOT = { "22", "32", "42", "52" };

    String designation = "Q1";
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
    double contactCurrent[] = new double[MAX_CONTACTS * 2];
    double contactCurCount[] = new double[MAX_CONTACTS * 2];

    public EGTHilfsschalterblockElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    }

    public EGTHilfsschalterblockElm(int xa, int ya, int xb, int yb, int f,
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
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
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
	if (nNo + nNc < 1) {
	    nNo = 1;
	    nNc = 0;
	}
    }

    int contactCount() { return nNo + nNc; }

    void updateSizeX() {
	int cols = contactCount();
	if (cols < 1)
	    cols = 1;
	sizeX = Math.max(3, 2 * cols + 1);
    }

    int contactTopPin(int i) { return i * 2; }
    int contactBotPin(int i) { return i * 2 + 1; }

    boolean contactIsNc(int i) { return i >= nNo; }

    boolean polesEnergized() {
	EGTSchuetzLink.refresh(sim);
	return EGTSchuetzLink.isEnergized(designation);
    }

    boolean contactClosed(int i) {
	boolean en = polesEnergized();
	return contactIsNc(i) ? !en : en;
    }

    String contactTopLab(int i) {
	return contactIsNc(i) ? NC_TOP[i - nNo] : NO_TOP[i];
    }

    String contactBotLab(int i) {
	return contactIsNc(i) ? NC_BOT[i - nNo] : NO_BOT[i];
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Hilfsschalterblock"; }

    int getPostCount() { return contactCount() * 2; }
    int getVoltageSourceCount() { return 0; }

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
	int cols = contactCount();
	int[] col = new int[cols];
	EGTStyle.evenPinPos(cols, sizeX, 1, col);
	for (int i = 0; i < cols; i++) {
	    int t = contactTopPin(i);
	    int b = contactBotPin(i);
	    pins[t].pos = col[i];
	    pins[t].side0 = SIDE_N;
	    pins[t].side = SIDE_N;
	    pins[t].output = false;
	    pins[b].pos = col[i];
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

    boolean getConnection(int n1, int n2) { return false; }

    boolean getMatrixConnection(int n1, int n2) {
	for (int i = 0; i < contactCount(); i++) {
	    if (EGTSchuetzLink.pinPair(n1, n2, contactTopPin(i),
				      contactBotPin(i)))
		return true;
	}
	return false;
    }

    void stamp() {
	for (int i = 0; i < contactCount(); i++) {
	    sim.stampNonLinear(nodes[contactTopPin(i)]);
	    sim.stampNonLinear(nodes[contactBotPin(i)]);
	}
    }

    void startIteration() {}
    void execute() {}

    void doStep() {
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
	EGTStyle.clearPinCurrents(pins);
	current = 0;
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
	    current += contactCurrent[i];
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
	boolean en = polesEnergized();
	int n = contactCount();

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
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
	}

	g.setColor(whiteColor);
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
		en ? "angezogen" : "abgefallen",
		nNo + "S / " + nNc + "Ö");
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	g.setLineWidth(1.0);

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

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nNo + " " + nNc + EGTNote.dumpSuffix(note);
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
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nNo = (int) xml.parseDoubleAttr("nno", nNo);
	nNc = (int) xml.parseDoubleAttr("nnc", nNc);
	clampCounts();
	updateSizeX();
	sizeY = DEF_SIZE_Y;
	setSize(1);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 458; }
    String getXmlDumpType() { return "EGTHilfsschalterblock"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = polesEnergized() ? "angezogen" : "abgefallen";
	arr[2] = "Kontakte = " + nNo + " Schließer / " + nNc + " Öffner";
	arr[3] = "Igesamt = " + getCurrentText(getCurrent());
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
    }
}
