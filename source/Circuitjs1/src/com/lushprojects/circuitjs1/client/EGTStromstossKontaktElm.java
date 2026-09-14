/*
    TAR-Dervic EGT: Stromstoß-Kontakt Schließer/Öffner (S305), Dump 462
*/

package com.lushprojects.circuitjs1.client;

/**
 * Getrennter Kontakt, gekoppelt über Bezeichnung. FLAG_NC = Öffner.
 */
class EGTStromstossKontaktElm extends ChipElm implements EGTDesignatable {
    static final int FLAG_NC = 2;
    static final int DEF_SIZE_X = 3;
    static final int DEF_SIZE_Y = 4;
    static final int MIN_SIZE_X = DEF_SIZE_X;
    static final int MIN_SIZE_Y = DEF_SIZE_Y;

    String designation = "E1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double switchCurrent, switchCurCount;

    public EGTStromstossKontaktElm(int xx, int yy) {
	super(xx, yy);
	flags &= ~EGTStyle.FLAG_UEBERSICHT;
	setSize(1);
	sizeX = DEF_SIZE_X;
	sizeY = DEF_SIZE_Y;
	layoutPins();
	syncEndpoints();
    }

    public EGTStromstossKontaktElm(int xa, int ya, int xb, int yb, int f,
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
    boolean isNC() { return (flags & FLAG_NC) != 0; }
    boolean nonLinear() { return true; }

    boolean latched() { return EGTStromstossLink.isLatched(designation); }

    String getChipName() {
	return isNC() ? "EGT-Stromstoß-Öffner" : "EGT-Stromstoß-Schließer";
    }

    int getPostCount() { return 2; }
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
	int mid = sizeX / 2;
	pins[0].pos = mid;
	pins[0].side0 = SIDE_N;
	pins[0].side = SIDE_N;
	pins[0].output = false;
	pins[1].pos = mid;
	pins[1].side0 = SIDE_S;
	pins[1].side = SIDE_S;
	pins[1].output = false;
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

    boolean getConnection(int n1, int n2) { return false; }

    boolean getMatrixConnection(int n1, int n2) {
	return EGTSchuetzLink.pinPair(n1, n2, 0, 1);
    }

    void stamp() {
	sim.stampNonLinear(nodes[0]);
	sim.stampNonLinear(nodes[1]);
    }

    boolean contactClosed() {
	return isNC() ? !latched() : latched();
    }

    void startIteration() {}
    void execute() {}

    void doStep() {
	EGTStyle.stampPotentialFreeContact(sim, this, nodes[0], nodes[1],
		contactClosed(), EGTStromstossLink.R_ON);
    }

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	switchCurrent = 0;
	current = 0;
	if (!contactClosed())
	    return;
	switchCurrent = EGTStyle.potentialFreeContactCurrent(this,
		nodes[0], nodes[1], volts[0], volts[1],
		EGTStromstossLink.R_ON);
	current = switchCurrent;
	EGTStyle.pinCurrentThru(pins, 0, 1, current);
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
	    int cx = (left + right) / 2;
	    int topY = top + Math.max(10, (bottom - top) * 22 / 100);
	    int botY = bottom - Math.max(10, (bottom - top) * 22 / 100);
	    boolean closed = contactClosed();

	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.context.setLineCap("butt");
	    g.drawLine(pins[0].post.x, pins[0].post.y, cx, topY);
	    g.drawLine(cx, botY, pins[1].post.x, pins[1].post.y);
	    if (isNC())
		EGTStyle.drawNcContact(g, cx, topY, botY, closed, stroke);
	    else
		EGTStyle.drawNoContact(g, cx, topY, botY, closed, stroke);

	    g.setFont(EGTStyle.pinLabelFont());
	    g.setColor(whiteColor);
	    int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right,
		    bottom, whiteColor, designation, "Stromstoß",
		    latched() ? "EIN (gerastet)" : "AUS");
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
	    g.setLineWidth(1.0);

	    if (closed && Math.abs(switchCurrent) >= EGTStyle.I_DOT_MIN) {
		switchCurCount = updateDotCount(switchCurrent, switchCurCount);
		drawDots(g, pins[0].post, new Point(cx, topY), switchCurCount);
		drawDots(g, new Point(cx, botY), pins[1].post, switchCurCount);
	    } else
		switchCurCount = 0;
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

    int getDumpType() { return 462; }
    String getXmlDumpType() { return "EGTStromstossKontakt"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = contactClosed() ? "geschlossen" : "offen";
	arr[2] = latched() ? "EIN (gerastet)" : "AUS";
	arr[3] = "V = " + getVoltageText(volts[0] - volts[1]);
	arr[4] = "I = " + getCurrentText(switchCurrent);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EGTStyle.kontaktartInfo(isNC());
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    flags = EGTStyle.applyKontaktart(flags, FLAG_NC, ei);
	    setPoints();
	}
    }
}
