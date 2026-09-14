/*
    TAR-Dervic EGT extension: Schuko-Steckdose L/PE/N (ZH + Übersicht), I203
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Schuko outlet: ZH box with L/PE/N ∩ contacts; Übersicht T+∩.
 * ZH: 3 posts on top (L, PE, N). Übersicht: 1 post (Kabel).
 * Size via mouse drag (like EGTAbzweigdoseElm).
 */
class EGTSteckdoseElm extends ChipElm implements EGTDesignatable {
    static final String[] PIN_LABELS = { "L", "PE", "N" };
    static final int MIN_SIZE_X = 3;
    static final int MIN_SIZE_Y = 2;

    String designation = "ST";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTSteckdoseElm(int xx, int yy) {
	super(xx, yy);
	setSize(2);
	sizeX = 7;
	sizeY = 5;
	layoutPins();
    }

    public EGTSteckdoseElm(int xa, int ya, int xb, int yb, int f,
			   StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	setSize((f & FLAG_SMALL) != 0 ? 1 : 2);
	// Dump: sizeX sizeY designation  — alt: nur designation
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
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	layoutPins();
	setPoints();
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }
    boolean uebersicht() { return (flags & EGTStyle.FLAG_UEBERSICHT) != 0; }

    String getChipName() { return "EGT-Steckdose"; }

    int getPostCount() { return uebersicht() ? 1 : 3; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	if (sizeX < MIN_SIZE_X)
	    sizeX = 7;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = 5;
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

    /** L/PE/N bzw. ein mittiger Kabel-Anschluss, abhängig von sizeX. */
    void placePins() {
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	if (uebersicht()) {
	    pins[0].pos = sizeX / 2;
	    pins[0].side0 = SIDE_N;
	    pins[0].side = SIDE_N;
	    pins[0].output = false;
	    return;
	}
	int inset = Math.max(0, (sizeX - 1) / 6);
	pins[0].pos = inset;
	pins[1].pos = sizeX / 2;
	pins[2].pos = sizeX - 1 - inset;
	if (pins[0].pos >= pins[1].pos)
	    pins[0].pos = Math.max(0, pins[1].pos - 1);
	if (pins[2].pos <= pins[1].pos)
	    pins[2].pos = Math.min(sizeX - 1, pins[1].pos + 1);
	for (int i = 0; i < 3; i++) {
	    pins[i].side0 = SIDE_N;
	    pins[i].side = SIDE_N;
	    pins[i].output = false;
	}
    }

    /** Maus ziehen = Größe (wie Abzweigdose). */
    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	if (cspc2 < 1)
	    setSize(2);
	int dx = Math.abs(xx - x);
	int dy = Math.abs(yy - y);
	sizeX = Math.max(MIN_SIZE_X, (dx + cspc2 / 2) / cspc2);
	sizeY = Math.max(MIN_SIZE_Y, (dy + cspc2 / 2) / cspc2);
	layoutPins();
	x2 = x + sizeX * cspc2;
	y2 = y;
	setPoints();
    }

    boolean creationFailed() {
	return sizeX < MIN_SIZE_X || sizeY < MIN_SIZE_Y;
    }

    /** L/PE/N absichtlich isoliert (keine Brücke im Gerät) — Last separat. */
    boolean getConnection(int n1, int n2) { return false; }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void boxBounds(int out[]) {
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	int xs = sizeX * cspc2;
	int ys = sizeY * cspc2;
	out[0] = xr;
	out[1] = yr;
	out[2] = xr + xs;
	out[3] = yr + ys;
    }

    void drawChip(Graphics g) {
	g.save();
	int[] b = new int[4];
	boxBounds(b);
	int left = b[0], top = b[1], right = b[2], bottom = b[3];
	Color stroke = needsHighlight() ? selectColor : lightGrayColor;

	if (uebersicht())
	    drawUebersicht(g, left, top, right, bottom, stroke);
	else
	    drawZh(g, left, top, right, bottom, stroke);

	drawPosts(g);
	g.restore();
    }

    void drawZh(Graphics g, int left, int top, int right, int bottom,
		Color stroke) {
	int h = bottom - top;

	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	Font fLabel = EGTStyle.pinLabelFont();
	g.setFont(fLabel);
	g.setColor(whiteColor);
	int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation, null, null);
	adjustBbox(ab[0], ab[1], ab[2], ab[3]);

	int arcR = Math.max(10, h / 8);
	int yPeak = top + (h * 68) / 100;
	int yDash = top + (h * 40) / 100;

	int[] xs = new int[3];
	for (int i = 0; i < 3; i++)
	    xs[i] = pins[i].post.x;

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	for (int i = 0; i < 3; i++) {
	    int px = xs[i];
	    g.setColor(stroke);
	    g.setLineWidth(2.5);
	    g.drawLine(pins[i].post.x, pins[i].post.y, px, top);
	    g.drawLine(px, top, px, yPeak);
	    EGTStyle.drawOpenDownU(g, px, yPeak, arcR);
	    EGTStyle.drawPinLabelBeside(g, this, pins[i].post.x, pins[i].post.y,
					PIN_LABELS[i], whiteColor);
	}

	g.setLineWidth(2.5);
	g.drawLine(xs[1] - (int) (arcR * 0.95), yPeak,
		   xs[1] + (int) (arcR * 0.95), yPeak);

	g.setLineDash(7, 5);
	g.setLineWidth(2.0);
	g.drawLine(xs[0], yDash, xs[2], yDash);
	g.setLineDash(0, 0);
	g.setLineWidth(1.0);
    }

    void drawUebersicht(Graphics g, int left, int top, int right, int bottom,
			Color stroke) {
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int barW = Math.max(14, (right - left) / 5);
	int stemH = Math.max(22, (bottom - top) / 3);
	int arcR = barW;
	int barY = cy + (bottom - top) / 10;
	int stemTop = barY - stemH;

	g.setColor(stroke);
	g.setLineWidth(2.5);
	// Ein Kabel-Anschluss oben → Stiel des T
	g.drawLine(pins[0].post.x, pins[0].post.y, cx, stemTop);
	g.drawLine(cx, stemTop, cx, barY);
	g.drawLine(cx - barW, barY, cx + barW, barY);
	EGTStyle.drawOpenDownU(g, cx, barY, arcR);
	g.setLineWidth(1.0);

	if (designation != null && designation.length() > 0) {
	    int[] ab = EGTStyle.drawHousingAnno(g, this, left, top, right,
		    bottom, whiteColor, designation, null, null);
	    adjustBbox(ab[0], ab[1], ab[2], ab[3]);
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
	sizeX = (int) xml.parseDoubleAttr("sx", sizeX);
	sizeY = (int) xml.parseDoubleAttr("sy", sizeY);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	layoutPins();
	setPoints();
    }

    int getDumpType() { return 443; }
    String getXmlDumpType() { return "EGTSteckdose"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	if (uebersicht()) {
	    arr[1] = "Anschluss = " + getVoltageText(volts[0]);
	    arr[2] = null;
	} else {
	    arr[1] = "L = " + getVoltageText(volts[0]);
	    arr[2] = "PE = " + getVoltageText(volts[1]);
	    arr[3] = "N = " + getVoltageText(volts[2]);
	}
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EditInfo.createCheckbox(Locale.LS("Übersichtsdarstellung"),
					  uebersicht());
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_UEBERSICHT);
	    setupPins();
	    allocNodes();
	    setPoints();
	}
    }
}
