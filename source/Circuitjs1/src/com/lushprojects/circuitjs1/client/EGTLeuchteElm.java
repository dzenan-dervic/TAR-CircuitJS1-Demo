/*
    TAR-Dervic EGT extension: Installationsleuchte L/N/PE (ZH + Übersicht), I203
*/

package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Leuchte: ZH-Box mit Kreis+X, L/N oben, PE als Punkt auf Gehäuserand.
 * Übersicht Leuchte: Senkrechte + X (ein Pol).
 * Meldeleuchte Übersicht: DIN EN 60617 Stromlaufplan (Kreis+X, zwei Pole, P1 links).
 * Elektrisch: Widerstand zwischen den Lampenpolen; PE passiv. ZH skalierbar per Mauszug.
 */
class EGTLeuchteElm extends ChipElm implements EGTDesignatable {
    static final String[] PIN_LABELS = { "L", "PE", "N" };
    static final int MIN_SIZE_X = 3;
    static final int MIN_SIZE_Y = 2;
    /** Stromlaufplan: gleiche Chipklasse wie Schützspule 3×3. */
    static final int MELD_US_SIZE_X = 3;
    static final int MELD_US_SIZE_Y = 3;
    /** ZH-Gehäuse wie eine kompakt gezogene Leuchte (nicht 3×4 aus der Übersicht). */
    static final int MELD_ZH_SIZE_X = 4;
    static final int MELD_ZH_SIZE_Y = 3;
    static final Color COL_MELD_RING = new Color(0xc9, 0xa2, 0x27);
    static final Color COL_LIT = new Color(0xff, 0xe0, 0x66);

    String designation = "E1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    double nom_pow = 60;
    double nom_v = 230;
    double resistance = 230 * 230 / 60.;

    public EGTLeuchteElm(int xx, int yy) {
	super(xx, yy);
	setSize(2);
	sizeX = 6;
	sizeY = 5;
	updateResistance();
	layoutPins();
    }

    public EGTLeuchteElm(int xa, int ya, int xb, int yb, int f,
			 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	setSize((f & FLAG_SMALL) != 0 ? 1 : 2);
	// Dump: sizeX sizeY designation [nom_pow nom_v]
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
	    } catch (Exception e) {
		designation = CustomLogicModel.unescape(t);
	    }
	}
	if (sizeX < MIN_SIZE_X)
	    sizeX = MIN_SIZE_X;
	if (sizeY < MIN_SIZE_Y)
	    sizeY = MIN_SIZE_Y;
	if (nom_pow <= 0)
	    nom_pow = 60;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	setPoints();
    
	note = EGTNote.readOptional(st);
}

    void updateResistance() {
	resistance = nom_v * nom_v / nom_pow;
	if (resistance < 1)
	    resistance = 1;
    }

    boolean isDigitalChip() { return false; }
    boolean uebersicht() { return (flags & EGTStyle.FLAG_UEBERSICHT) != 0; }
    boolean meldeleuchte() { return (flags & EGTStyle.FLAG_MELD) != 0; }
    /** Steuerstromkreis: Kreis, Anschlüsse oben/unten. X nur an der Leuchte. */
    boolean stromlaufMeld() { return meldeleuchte() && uebersicht(); }
    int lampReturnPin() { return stromlaufMeld() ? 1 : 2; }

    String getChipName() {
	return meldeleuchte() ? "EGT-Meldeleuchte" : "EGT-Leuchte";
    }

    int getPostCount() {
	if (stromlaufMeld())
	    return 2;
	return uebersicht() ? 1 : 3;
    }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
	if (stromlaufMeld()) {
	    sizeX = MELD_US_SIZE_X;
	    sizeY = MELD_US_SIZE_Y;
	} else {
	    if (sizeX < MIN_SIZE_X)
		sizeX = 6;
	    if (sizeY < MIN_SIZE_Y)
		sizeY = 5;
	}
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
	if (stromlaufMeld()) {
	    sizeX = MELD_US_SIZE_X;
	    sizeY = MELD_US_SIZE_Y;
	    int mid = sizeX / 2;
	    pins[0].pos = mid;
	    pins[0].side0 = SIDE_N;
	    pins[0].side = SIDE_N;
	    pins[0].output = false;
	    pins[1].pos = mid;
	    pins[1].side0 = SIDE_S;
	    pins[1].side = SIDE_S;
	    pins[1].output = false;
	    return;
	}
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

    void drag(int xx, int yy) {
	if (stromlaufMeld()) {
	    super.drag(xx, yy);
	    sizeX = MELD_US_SIZE_X;
	    sizeY = MELD_US_SIZE_Y;
	    layoutPins();
	    x2 = x + sizeX * cspc2;
	    y2 = y + sizeY * cspc2;
	    setPoints();
	    return;
	}
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
	if (stromlaufMeld()) {
	    if (cspc2 < 1)
		setSize(1);
	    sizeX = MELD_US_SIZE_X;
	    sizeY = MELD_US_SIZE_Y;
	    x2 = x + sizeX * cspc2;
	    y2 = y + sizeY * cspc2;
	    layoutPins();
	    setPoints();
	    return false;
	}
	if (x == x2 && y == y2) {
	    if (cspc2 < 1)
		setSize(2);
	    if (sizeX < MIN_SIZE_X)
		sizeX = MIN_SIZE_X;
	    if (sizeY < MIN_SIZE_Y)
		sizeY = MIN_SIZE_Y;
	    x2 = x + sizeX * cspc2;
	    layoutPins();
	    setPoints();
	}
	return sizeX < MIN_SIZE_X || sizeY < MIN_SIZE_Y;
    }

    /**
     * L–N müssen matrixverbunden sein (wie Widerstand). ChipElm-Default false
     * trennt die Matrizen → stampResistor wirkt wie L gegen Masse → falsch „ein“.
     * PE bleibt isoliert.
     */
    boolean getConnection(int n1, int n2) {
	if (stromlaufMeld())
	    return (n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0);
	if (uebersicht())
	    return false;
	return (n1 == 0 && n2 == 2) || (n1 == 2 && n2 == 0);
    }

    void stamp() {
	if (uebersicht() && !stromlaufMeld())
	    return;
	sim.stampResistor(nodes[0], nodes[lampReturnPin()], resistance);
    }

    void startIteration() {}
    void execute() {}
    void doStep() {}

    void calculateCurrent() {
	EGTStyle.clearPinCurrents(pins);
	if ((uebersicht() && !stromlaufMeld()) || resistance <= 0) {
	    current = 0;
	    return;
	}
	int n = lampReturnPin();
	current = (volts[0] - volts[n]) / resistance;
	EGTStyle.pinCurrentThru(pins, 0, n, current);
    }

    /** Nur bei geschlossenem Stromkreis (sinnvoller Strom), nicht bei offenem N/L. */
    boolean isLit() {
	if (uebersicht() && !stromlaufMeld())
	    return false;
	double iNom = nom_v / resistance;
	return Math.abs(current) > iNom * 0.05;
    }

    void boxBounds(int out[]) {
	if (rectPointsX != null && rectPointsY != null
		&& rectPointsX.length >= 4) {
	    int minx = rectPointsX[0], maxx = rectPointsX[0];
	    int miny = rectPointsY[0], maxy = rectPointsY[0];
	    for (int i = 1; i < 4; i++) {
		minx = min(minx, rectPointsX[i]);
		maxx = max(maxx, rectPointsX[i]);
		miny = min(miny, rectPointsY[i]);
		maxy = max(maxy, rectPointsY[i]);
	    }
	    out[0] = minx;
	    out[1] = miny;
	    out[2] = maxx;
	    out[3] = maxy;
	    return;
	}
	int xr = x + cspc2 - cspc;
	int yr = y - cspc;
	int fsx = isFlippedXY() ? sizeY : sizeX;
	int fsy = isFlippedXY() ? sizeX : sizeY;
	out[0] = xr;
	out[1] = yr;
	out[2] = xr + fsx * cspc2;
	out[3] = yr + fsy * cspc2;
    }

    /** Klemme oben/unten (sonst links/rechts) — nach Flip aus der Post-Lage. */
    static boolean pinOnNorthSouth(Point p, int cx, int cy) {
	return Math.abs(p.y - cy) >= Math.abs(p.x - cx);
    }

    /** L/N: vom Post rechtwinklig zur Kreistangente (nicht fest links/rechts). */
    void appendLeadToLamp(ArrayList<Point> pts, Point post, int cx, int cy,
			  int r) {
	pts.add(post);
	if (pinOnNorthSouth(post, cx, cy)) {
	    int hx = (post.x <= cx) ? cx - r : cx + r;
	    pts.add(new Point(post.x, cy));
	    pts.add(new Point(hx, cy));
	} else {
	    int hy = (post.y <= cy) ? cy - r : cy + r;
	    pts.add(new Point(cx, post.y));
	    pts.add(new Point(cx, hy));
	}
    }

    void drawPolyline(Graphics g, ArrayList<Point> pts) {
	for (int i = 0; i < pts.size() - 1; i++) {
	    Point a = pts.get(i);
	    Point b = pts.get(i + 1);
	    g.drawLine(a.x, a.y, b.x, b.y);
	}
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
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int w = right - left;
	int h = bottom - top;
	int r = Math.min(w, h) * 21 / 100;
	if (r < 10)
	    r = 10;

	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	ArrayList<Point> leadL = new ArrayList<Point>();
	ArrayList<Point> leadN = new ArrayList<Point>();
	appendLeadToLamp(leadL, pins[0].post, cx, cy, r);
	appendLeadToLamp(leadN, pins[2].post, cx, cy, r);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	drawPolyline(g, leadL);
	drawPolyline(g, leadN);

	Point pe = pins[1].post;
	g.setColor(stroke);
	if (pinOnNorthSouth(pe, cx, cy)) {
	    int edgeY = (pe.y < cy) ? top : bottom;
	    g.drawLine(pe.x, pe.y, pe.x, edgeY);
	    g.fillOval(pe.x - 3, edgeY - 3, 7, 7);
	} else {
	    int edgeX = (pe.x < cx) ? left : right;
	    g.drawLine(pe.x, pe.y, edgeX, pe.y);
	    g.fillOval(edgeX - 3, pe.y - 3, 7, 7);
	}

	// Leuchtensymbol
	if (isLit()) {
	    g.setColor(COL_LIT);
	    g.fillOval(cx - r, cy - r, r * 2, r * 2);
	}
	g.setColor(stroke);
	g.setLineWidth(2.0);
	drawThickCircle(g, cx, cy, r);
	EGTStyle.drawLampX(g, cx, cy, r, stroke);
	if (meldeleuchte()) {
	    g.setColor(COL_MELD_RING);
	    g.setLineWidth(1.5);
	    drawThickCircle(g, cx, cy, r);
	}

	g.setColor(whiteColor);
	for (int i = 0; i < 3; i++)
	    EGTStyle.drawPinLabelBeside(g, this, pins[i].post.x, pins[i].post.y,
					PIN_LABELS[i], whiteColor);

	String st = (isLit() ? "ein" : "aus")
		+ (meldeleuchte() ? " · Melde" : "");
	int[] abL = EGTStyle.drawHousingAnno(g, this, left, top, right, bottom,
		whiteColor, designation, st, null);
	adjustBbox(abL[0], abL[1], abL[2], abL[3]);
	g.setLineWidth(1.0);

	curcount = updateDotCount(current, curcount);
	if (isLit()) {
	    for (int i = 0; i < leadL.size() - 1; i++)
		drawDots(g, leadL.get(i), leadL.get(i + 1), curcount);
	    for (int i = leadN.size() - 2; i >= 0; i--)
		drawDots(g, leadN.get(i + 1), leadN.get(i), -curcount);
	}
    }

    void drawUebersicht(Graphics g, int left, int top, int right, int bottom,
			Color stroke) {
	if (stromlaufMeld()) {
	    drawStromlaufMeld(g, stroke);
	    return;
	}
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int arm = Math.max(12, Math.min(right - left, bottom - top) / 5);
	Point p = pins[0].post;
	double dx = cx - p.x;
	double dy = cy - p.y;
	double len = Math.sqrt(dx * dx + dy * dy);
	if (len < 1)
	    len = 1;
	int mx = (int) (cx - dx / len * arm);
	int my = (int) (cy - dy / len * arm);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(p.x, p.y, mx, my);
	EGTStyle.drawLampX(g, cx, cy, arm, stroke);
	g.setLineWidth(1.0);

	if (designation != null && designation.length() > 0) {
	    g.setFont(EGTStyle.pinLabelFont());
	    g.setColor(whiteColor);
	    int tw = EGTStyle.measureDesigNote(g, this, designation);
	    EGTStyle.drawDesigAndNote(g, this, designation,
				     cx + arm + 10, cy + arm + 4);
	    adjustBbox(left, min(p.y, top) - 8, right + tw + 24,
		       max(p.y, bottom));
	}
    }

    /** DIN EN 60617 Meldeleuchte im Stromlaufplan (SCHUETZ-10 P1). */
    void drawStromlaufMeld(Graphics g, Color stroke) {
	Point a = pins[0].post;
	Point b = pins[1].post;
	int cx = (a.x + b.x) / 2;
	int cy = (a.y + b.y) / 2;
	double vx = b.x - a.x;
	double vy = b.y - a.y;
	double span = Math.sqrt(vx * vx + vy * vy);
	if (span < 1) {
	    vx = 0;
	    vy = 1;
	    span = 1;
	}
	vx /= span;
	vy /= span;
	int[] cs = EGTStyle.standardCoilSize(sizeX * cspc2, sizeY * cspc2);
	int r = Math.max(6, cs[1]);
	int tpx = (int) Math.round(cx - vx * r);
	int tpy = (int) Math.round(cy - vy * r);
	int bpx = (int) Math.round(cx + vx * r);
	int bpy = (int) Math.round(cy + vy * r);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.context.setLineCap("butt");
	g.drawLine(a.x, a.y, tpx, tpy);
	g.drawLine(bpx, bpy, b.x, b.y);

	if (isLit()) {
	    g.setColor(COL_LIT);
	    g.fillOval(cx - r, cy - r, r * 2, r * 2);
	}
	g.setColor(stroke);
	g.setLineWidth(2.0);
	drawThickCircle(g, cx, cy, r);
	EGTStyle.drawLampX(g, cx, cy, r, stroke);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	double nx = -vy;
	int lx = (int) Math.round(cx + nx * (r + 6));
	int ly = (int) Math.round(cy + 4);
	if (nx < 0)
	    lx -= tw;
	else if (Math.abs(nx) < 0.35)
	    lx -= tw / 2;
	EGTStyle.drawDesigAndNote(g, this, designation, lx, ly);

	int extra = tw + r + 16;
	adjustBbox(min(a.x, b.x) - extra, min(a.y, b.y) - 8,
		   max(a.x, b.x) + extra, max(a.y, b.y) + 8);

	curcount = updateDotCount(current, curcount);
	if (isLit()) {
	    drawDots(g, a, new Point(tpx, tpy), curcount);
	    drawDots(g, new Point(bpx, bpy), b, curcount);
	}
    }

    String dump() {
	return super.dump() + " " + sizeX + " " + sizeY + " "
		+ CustomLogicModel.escape(designation) + " " + nom_pow + " "
		+ nom_v + EGTNote.dumpSuffix(note);
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
	if (meldeleuchte())
	    XMLSerializer.dumpAttr(elem, "meld", 1);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	sizeX = (int) xml.parseDoubleAttr("sx", sizeX);
	sizeY = (int) xml.parseDoubleAttr("sy", sizeY);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	nom_pow = xml.parseDoubleAttr("np", nom_pow);
	nom_v = xml.parseDoubleAttr("nv", nom_v);
	if (xml.parseIntAttr("meld", 0) != 0)
	    flags |= EGTStyle.FLAG_MELD;
	else
	    flags &= ~EGTStyle.FLAG_MELD;
	if (nom_pow <= 0)
	    nom_pow = 60;
	if (nom_v <= 0)
	    nom_v = 230;
	updateResistance();
	layoutPins();
	setPoints();
    }

    int getDumpType() { return 449; }
    String getXmlDumpType() { return "EGTLeuchte"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	if (stromlaufMeld()) {
	    arr[1] = isLit() ? "ein" : "aus";
	    arr[2] = getVoltageText(volts[0]);
	    arr[3] = getVoltageText(volts[1]);
	    arr[4] = "P = " + getUnitText(getPower(), "W");
	} else if (uebersicht()) {
	    arr[1] = "Anschluss = " + getVoltageText(volts[0]);
	    arr[2] = null;
	} else {
	    arr[1] = isLit() ? "ein" : "aus";
	    arr[2] = "L = " + getVoltageText(volts[0]);
	    arr[3] = "PE = " + getVoltageText(volts[1]);
	    arr[4] = "N = " + getVoltageText(volts[2]);
	    arr[5] = "P = " + getUnitText(getPower(), "W");
	}
    }

    double getPower() {
	if (uebersicht() && !stromlaufMeld())
	    return 0;
	return current * (volts[0] - volts[lampReturnPin()]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EditInfo.createCheckbox(Locale.LS("Übersichtsdarstellung"),
					  uebersicht());
	if (n == 2)
	    return EditInfo.createCheckbox(Locale.LS("Meldeleuchte"),
					  meldeleuchte());
	if (n == 3)
	    return new EditInfo("Nennleistung (W)", nom_pow, 0, 0).setPositive();
	if (n == 4)
	    return new EditInfo("Nennspannung (V)", nom_v, 0, 0).setPositive();
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_UEBERSICHT);
	    applyViewSize();
	    setupPins();
	    allocNodes();
	    setPoints();
	}
	if (n == 2) {
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_MELD);
	    if (meldeleuchte()
		    && ("E1".equals(designation) || "H1".equals(designation)))
		designation = "P1";
	    else if (!meldeleuchte()
		    && ("P1".equals(designation) || "H1".equals(designation)))
		designation = "E1";
	    if (uebersicht()) {
		applyViewSize();
		setupPins();
		allocNodes();
		setPoints();
	    }
	}
	if (n == 3 && ei.value > 0) {
	    nom_pow = ei.value;
	    updateResistance();
	}
	if (n == 4 && ei.value > 0) {
	    nom_v = ei.value;
	    updateResistance();
	}
    }

    void applyViewSize() {
	if (stromlaufMeld()) {
	    setSize(1);
	    sizeX = MELD_US_SIZE_X;
	    sizeY = MELD_US_SIZE_Y;
	    x2 = x + sizeX * cspc2;
	    y2 = y + sizeY * cspc2;
	} else if (meldeleuchte()) {
	    setSize(2);
	    sizeX = MELD_ZH_SIZE_X;
	    sizeY = MELD_ZH_SIZE_Y;
	    x2 = x + sizeX * cspc2;
	    y2 = y;
	} else {
	    setSize(2);
	    if (sizeX < MIN_SIZE_X)
		sizeX = 6;
	    if (sizeY < MIN_SIZE_Y)
		sizeY = 5;
	    x2 = x + sizeX * cspc2;
	    y2 = y;
	}
    }
}
