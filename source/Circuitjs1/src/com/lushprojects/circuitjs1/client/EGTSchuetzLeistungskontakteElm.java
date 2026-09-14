/*
    TAR-Dervic EGT: Schütz-Leistungskontakte 1–2/3–4/5–6 (S301), Dump 455
*/

package com.lushprojects.circuitjs1.client;

/**
 * Drei Hauptstrom-Schließer als Block, gekoppelt über Bezeichnung.
 */
class EGTSchuetzLeistungskontakteElm extends ChipElm implements EGTDesignatable {
    static final int DEF_SIZE_X = 7;
    static final int DEF_SIZE_Y = 4;
    static final int MIN_SIZE_X = DEF_SIZE_X;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;
    static final int POLES = 3;
    static final String[] TOP_LAB = { "1", "3", "5" };
    static final String[] BOT_LAB = { "2", "4", "6" };

    String designation = "Q1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double poleCurrent[] = new double[POLES];
    double poleCurCount[] = new double[POLES];

    public EGTSchuetzLeistungskontakteElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    }

    public EGTSchuetzLeistungskontakteElm(int xa, int ya, int xb, int yb, int f,
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
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return true; }

    String getChipName() { return "EGT-Schütz-Leistungskontakte"; }

    int getPostCount() { return POLES * 2; }
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
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	int[] pos = new int[POLES];
	EGTStyle.evenPinPos(POLES, sizeX, 1, pos);
	for (int p = 0; p < POLES; p++) {
	    pins[p * 2].pos = pos[p];
	    pins[p * 2].side0 = SIDE_N;
	    pins[p * 2].side = SIDE_N;
	    pins[p * 2].output = false;
	    pins[p * 2 + 1].pos = pos[p];
	    pins[p * 2 + 1].side0 = SIDE_S;
	    pins[p * 2 + 1].side = SIDE_S;
	    pins[p * 2 + 1].output = false;
	}
    }

    /** x2/y2 = volle Chip-Box (Höhe mitnehmen — sonst Ghost-Rail beim Move). */
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

    boolean getConnection(int n1, int n2) { return false; }

    boolean getMatrixConnection(int n1, int n2) {
	for (int p = 0; p < POLES; p++) {
	    if (EGTSchuetzLink.pinPair(n1, n2, p * 2, p * 2 + 1))
		return true;
	}
	return false;
    }

    void stamp() {
	for (int p = 0; p < POLES; p++) {
	    sim.stampNonLinear(nodes[p * 2]);
	    sim.stampNonLinear(nodes[p * 2 + 1]);
	}
    }

    boolean polesClosed() {
	EGTSchuetzLink.refresh(sim);
	return EGTSchuetzLink.isEnergized(designation);
    }

    void startIteration() {}
    void execute() {}

    void doStep() {
	boolean closed = polesClosed();
	for (int p = 0; p < POLES; p++)
	    EGTStyle.stampPotentialFreeContact(sim, this,
		    nodes[p * 2], nodes[p * 2 + 1],
		    closed, EGTSchuetzLink.R_ON);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	current = 0;
	boolean closed = polesClosed();
	for (int p = 0; p < POLES; p++) {
	    poleCurrent[p] = 0;
	    if (!closed)
		continue;
	    poleCurrent[p] = EGTStyle.potentialFreeContactCurrent(this,
		    nodes[p * 2], nodes[p * 2 + 1],
		    volts[p * 2], volts[p * 2 + 1],
		    EGTSchuetzLink.R_ON);
	    EGTStyle.pinCurrentThru(pins, p * 2, p * 2 + 1, poleCurrent[p]);
	    current += poleCurrent[p];
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
	    int left = b[0], top = b[1], right = b[2], bottom = b[3];
	    int topY = top + Math.max(10, (bottom - top) * 22 / 100);
	    int botY = bottom - Math.max(10, (bottom - top) * 22 / 100);
	    int cy = (topY + botY) / 2;
	    boolean closed = polesClosed();

	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");

	    int leftP = pins[0].post.x;
	    int rightP = pins[4].post.x;
	    for (int p = 0; p < POLES; p++) {
		int px = pins[p * 2].post.x;
		g.setColor(stroke);
		g.setLineWidth(2.5);
		g.drawLine(px, pins[p * 2].post.y, px, topY);
		g.drawLine(px, botY, px, pins[p * 2 + 1].post.y);
		EGTStyle.drawContactorNoContact(g, px, topY, botY, closed,
						stroke);
	    }
	    EGTStyle.drawMechLink(g, leftP, rightP, cy, stroke);
	    g.setColor(whiteColor);
	    for (int p = 0; p < POLES; p++) {
		EGTStyle.drawPinLabelBeside(g, this, pins[p * 2].post.x,
			pins[p * 2].post.y, TOP_LAB[p], whiteColor);
		EGTStyle.drawPinLabelBeside(g, this, pins[p * 2 + 1].post.x,
			pins[p * 2 + 1].post.y, BOT_LAB[p], whiteColor);
	    }
	    int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right,
		    bottom, whiteColor, designation,
		    closed ? "angezogen" : "abgefallen", null);
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    if (closed) {
		for (int p = 0; p < POLES; p++) {
		    poleCurCount[p] = updateDotCount(poleCurrent[p],
						     poleCurCount[p]);
		    int px = pins[p * 2].post.x;
		    drawDots(g, pins[p * 2].post, new Point(px, topY),
			     poleCurCount[p]);
		    drawDots(g, new Point(px, botY), pins[p * 2 + 1].post,
			     poleCurCount[p]);
		}
	    }
	    drawPosts(g);
	} finally {
	    g.restore();
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "sx", sizeX);
	XMLSerializer.dumpAttr(elem, "sy", sizeY);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	setSize(1);
	layoutPins();
	syncEndpoints();
    }

    int getDumpType() { return 455; }
    String getXmlDumpType() { return "EGTSchuetzLeistungskontakte"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = polesClosed() ? "angezogen" : "abgefallen";
	arr[2] = "I1 = " + getCurrentText(poleCurrent[0]);
	arr[3] = "I2 = " + getCurrentText(poleCurrent[1]);
	arr[4] = "I3 = " + getCurrentText(poleCurrent[2]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
    }
}
