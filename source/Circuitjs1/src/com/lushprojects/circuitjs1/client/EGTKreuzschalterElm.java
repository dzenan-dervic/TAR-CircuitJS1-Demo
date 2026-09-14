/*
    TAR-Dervic EGT extension: Kreuzschalter rastend (ZH + Übersicht), I202
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Kreuzschalter: Posts 1/2 oben, 3/4 unten.
 * Stellung 1 (gerade): 1↔3, 2↔4. Stellung 2 (Kreuz): 1↔4, 2↔3.
 * ZH: 4 Posts. Übersicht: 1 Kabel-Anschluss.
 * position 0 = Stellung 1, position 1 = Stellung 2.
 */
class EGTKreuzschalterElm extends SwitchElm implements EGTDesignatable {
    static final int MIN_W = 80;
    static final int MIN_H = 80;
    static final int LEAD_OUT = 16;

    String designation = "S3";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    Point posts[];
    int bodyLeft, bodyTop, bodyRight, bodyBottom;
    double currents[] = new double[2];
    double curcounts[] = new double[2];

    public EGTKreuzschalterElm(int xx, int yy) {
	super(xx, yy);
	momentary = false;
	position = 0;
	posCount = 2;
	x2 = xx + 96;
	y2 = yy + 96;
	allocPosts();
	allocNodes();
	setPoints();
    }

    public EGTKreuzschalterElm(int xa, int ya, int xb, int yb, int f,
			       StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	momentary = false;
	posCount = 2;
	if (position > 1)
	    position = 0;
	if (st.hasMoreTokens()) {
	    try {
		int w = Integer.parseInt(st.nextToken());
		int h = Integer.parseInt(st.nextToken());
		if (w > 0 && h > 0) {
		    x2 = x + w;
		    y2 = y + h;
		}
		if (st.hasMoreTokens())
		    designation = CustomLogicModel.unescape(st.nextToken());
	    } catch (Exception e) {
		// alt / unvollständig
	    }
	}
	allocPosts();
	allocNodes();
	setPoints();
    
	note = EGTNote.readOptional(st);
}

    boolean uebersicht() {
	return (flags & EGTStyle.FLAG_UEBERSICHT) != 0;
    }

    int stellung() { return position == 0 ? 1 : 2; }

    void allocPosts() {
	posts = new Point[getPostCount()];
	for (int i = 0; i < posts.length; i++)
	    posts[i] = new Point();
    }

    int getPostCount() { return uebersicht() ? 1 : 4; }

    Point getPost(int n) {
	if (posts == null || n < 0 || n >= posts.length)
	    return super.getPost(n);
	return posts[n];
    }

    int getDumpType() { return 447; }
    String getXmlDumpType() { return "EGTKreuzschalter"; }
    int getShortcut() { return 0; }

    String dump() {
	int w = Math.abs(x2 - x);
	int h = Math.abs(y2 - y);
	return super.dump() + " " + position + " " + momentary + " " + w + " "
		+ h + " " + CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "w", Math.abs(x2 - x));
	XMLSerializer.dumpAttr(elem, "h", Math.abs(y2 - y));
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
	int w = (int) xml.parseDoubleAttr("w", Math.abs(x2 - x));
	int h = (int) xml.parseDoubleAttr("h", Math.abs(y2 - y));
	if (w < MIN_W)
	    w = MIN_W;
	if (h < MIN_H)
	    h = MIN_H;
	x2 = x + w;
	y2 = y + h;
	if (position > 1)
	    position = 0;
	allocPosts();
	allocNodes();
	setPoints();
    }

    void drag(int xx, int yy) {
	xx = snapGrid(xx);
	yy = snapGrid(yy);
	int w = Math.abs(xx - x);
	int h = Math.abs(yy - y);
	if (w < MIN_W)
	    w = MIN_W;
	if (h < MIN_H)
	    h = MIN_H;
	x2 = x + ((xx >= x) ? w : -w);
	y2 = y + ((yy >= y) ? h : -h);
	setPoints();
    }

    boolean creationFailed() {
	return Math.abs(x2 - x) < MIN_W || Math.abs(y2 - y) < MIN_H;
    }

    void setPoints() {
	super.setPoints();
	if (posts == null || posts.length != getPostCount())
	    allocPosts();

	bodyLeft = Math.min(x, x2);
	bodyRight = Math.max(x, x2);
	bodyTop = Math.min(y, y2);
	bodyBottom = Math.max(y, y2);
	int lead = Math.max(LEAD_OUT, (bodyBottom - bodyTop) / 8);
	lead = snapGrid(lead);
	if (lead < LEAD_OUT)
	    lead = LEAD_OUT;
	int inset = Math.max(16, snapGrid((bodyRight - bodyLeft) / 5));

	if (uebersicht()) {
	    posts[0].x = snapGrid((bodyLeft + bodyRight) / 2);
	    posts[0].y = snapGrid(bodyTop - lead);
	    setBbox(bodyLeft, Math.min(bodyTop, posts[0].y),
		    bodyRight, bodyBottom);
	} else {
	    // 1 / 2 oben, 3 / 4 unten
	    posts[0].x = snapGrid(bodyLeft + inset);
	    posts[0].y = snapGrid(bodyTop - lead);
	    posts[1].x = snapGrid(bodyRight - inset);
	    posts[1].y = posts[0].y;
	    if (posts[1].x <= posts[0].x)
		posts[1].x = snapGrid(posts[0].x + 16);
	    posts[2].x = posts[0].x;
	    posts[2].y = snapGrid(bodyBottom + lead);
	    posts[3].x = posts[1].x;
	    posts[3].y = posts[2].y;
	    point1 = posts[0];
	    point2 = posts[2];
	    setBbox(bodyLeft, Math.min(bodyTop, posts[0].y),
		    bodyRight,
		    Math.max(bodyBottom, posts[2].y));
	}
    }

    boolean getConnection(int n1, int n2) {
	if (uebersicht())
	    return false;
	if (position == 0)
	    return comparePair(n1, n2, 0, 2) || comparePair(n1, n2, 1, 3);
	return comparePair(n1, n2, 0, 3) || comparePair(n1, n2, 1, 2);
    }

    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }

    void stamp() {
	if (uebersicht())
	    return;
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	if (position == 0) {
	    sim.stampResistor(nodes[0], nodes[2], r);
	    sim.stampResistor(nodes[1], nodes[3], r);
	} else {
	    sim.stampResistor(nodes[0], nodes[3], r);
	    sim.stampResistor(nodes[1], nodes[2], r);
	}
    }

    void calculateCurrent() {
	if (uebersicht()) {
	    currents[0] = currents[1] = 0;
	    current = 0;
	    return;
	}
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	if (position == 0) {
	    currents[0] = (volts[0] - volts[2]) / r;
	    currents[1] = (volts[1] - volts[3]) / r;
	} else {
	    currents[0] = (volts[0] - volts[3]) / r;
	    currents[1] = (volts[1] - volts[2]) / r;
	}
	if (Math.abs(currents[0]) < EGTStyle.I_DOT_MIN)
	    currents[0] = 0;
	if (Math.abs(currents[1]) < EGTStyle.I_DOT_MIN)
	    currents[1] = 0;
	current = currents[0] + currents[1];
    }

    double getCurrentIntoNode(int n) {
	if (uebersicht())
	    return 0;
	if (n == 0)
	    return -currents[0];
	if (n == 1)
	    return -currents[1];
	if (position == 0) {
	    if (n == 2)
		return currents[0];
	    if (n == 3)
		return currents[1];
	} else {
	    if (n == 2)
		return currents[1];
	    if (n == 3)
		return currents[0];
	}
	return 0;
    }

    Rectangle getSwitchRect() {
	return new Rectangle(bodyLeft, bodyTop,
			     bodyRight - bodyLeft, bodyBottom - bodyTop);
    }

    void draw(Graphics g) {
	setPoints();
	Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	if (uebersicht())
	    drawUebersicht(g, stroke);
	else
	    drawZh(g, stroke);
	drawPosts(g);
    }

    void drawZh(Graphics g, Color stroke) {
	int left = bodyLeft, top = bodyTop, right = bodyRight, bottom = bodyBottom;
	int h = bottom - top;
	int w = right - left;
	int contactY = top + Math.max(18, h * 28 / 100);
	int pivotY = bottom - Math.max(16, h * 18 / 100);
	int bridgeY = top + Math.max(12, h * 14 / 100);
	int bridgeY2 = top + Math.max(16, h * 20 / 100);

	// vier Kontakt-X: C1..C4
	int c1 = left + Math.max(10, w * 12 / 100);
	int c2 = left + w * 38 / 100;
	int c3 = left + w * 62 / 100;
	int c4 = right - Math.max(10, w * 12 / 100);
	int nAx = posts[0].x;
	int nBx = posts[1].x;
	int pAx = posts[2].x;
	int pBx = posts[3].x;

	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.drawRect(left, top, w, h);

	// Zuleitungen 1/2 und feste Brücken (A→C1/C4, B→C2/C3)
	g.setLineWidth(2.0);
	g.drawLine(nAx, posts[0].y, nAx, bridgeY);
	g.drawLine(nBx, posts[1].y, nBx, bridgeY2);
	g.drawLine(c1, bridgeY, c4, bridgeY);
	g.drawLine(c1, bridgeY, c1, contactY);
	g.drawLine(c4, bridgeY, c4, contactY);
	g.drawLine(c2, bridgeY2, c3, bridgeY2);
	g.drawLine(c2, bridgeY2, c2, contactY);
	g.drawLine(c3, bridgeY2, c3, contactY);
	g.drawLine(nBx, bridgeY2, c3, bridgeY2);

	int[] cx = { c1, c2, c3, c4 };
	for (int i = 0; i < 4; i++)
	    g.drawLine(cx[i] - 5, contactY, cx[i] + 5, contactY);

	g.drawLine(pAx, posts[2].y, pAx, pivotY);
	g.drawLine(pBx, posts[3].y, pBx, pivotY);

	// Messer gekoppelt: St.1 → C1+C3, St.2 → C2+C4
	int tipAx = position == 0 ? c1 : c2;
	int tipBx = position == 0 ? c3 : c4;
	g.setLineWidth(2.5);
	g.drawLine(pAx, pivotY, tipAx, contactY);
	g.drawLine(pBx, pivotY, tipBx, contactY);

	int linkY = pivotY - Math.max(10, h * 10 / 100);
	int attachA = pAx + (tipAx - pAx) * 45 / 100;
	int attachB = pBx + (tipBx - pBx) * 45 / 100;
	g.setLineDash(5, 3);
	g.setLineWidth(2.0);
	g.drawLine(attachA, linkY, attachB, linkY);
	g.setLineDash(0, 0);
	EGTStyle.drawRastBetaetigung(g, attachA, linkY,
				     Math.max(22, w / 4), true, stroke);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[0].x, posts[0].y, "1",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[1].x, posts[1].y, "2",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[2].x, posts[2].y, "3",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[3].x, posts[3].y, "4",
				    whiteColor);

	String stLabel = position == 0 ? "Stellung 1 (gerade)"
		: "Stellung 2 (Kreuz)";
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	int twSt = (int) g.context.measureText(stLabel).getWidth();
	int labelX = right + 20;
	EGTStyle.drawDesigAndNote(g, this, designation, labelX, top + 14);
	g.drawString(stLabel, labelX, bottom - 6);
	adjustBbox(left, posts[0].y - 22,
		   labelX + Math.max(tw, twSt) + 8, posts[2].y + 18);
	g.setLineWidth(1.0);

	curcounts[0] = updateDotCount(currents[0], curcounts[0]);
	curcounts[1] = updateDotCount(currents[1], curcounts[1]);
	Point p1b = new Point(nAx, bridgeY);
	Point p2b = new Point(nBx, bridgeY2);
	Point c1t = new Point(c1, contactY);
	Point c2t = new Point(c2, contactY);
	Point c3t = new Point(c3, contactY);
	Point c4t = new Point(c4, contactY);
	Point pv3 = new Point(pAx, pivotY);
	Point pv4 = new Point(pBx, pivotY);
	if (position == 0) {
	    if (curcounts[0] != 0)
		EGTStyle.drawDotsAlong(this, g, curcounts[0], new Point[] {
		    posts[0], p1b, new Point(c1, bridgeY), c1t, pv3, posts[2]
		});
	    if (curcounts[1] != 0)
		EGTStyle.drawDotsAlong(this, g, curcounts[1], new Point[] {
		    posts[1], p2b, new Point(c3, bridgeY2), c3t, pv4, posts[3]
		});
	} else {
	    if (curcounts[0] != 0)
		EGTStyle.drawDotsAlong(this, g, curcounts[0], new Point[] {
		    posts[0], p1b, new Point(c4, bridgeY), c4t, pv4, posts[3]
		});
	    if (curcounts[1] != 0)
		EGTStyle.drawDotsAlong(this, g, curcounts[1], new Point[] {
		    posts[1], p2b, new Point(c2, bridgeY2), c2t, pv3, posts[2]
		});
	}
    }

    void drawCentered(Graphics g, String s, int x, int y) {
	int tw = (int) g.context.measureText(s).getWidth();
	g.drawString(s, x - tw / 2, y);
    }

    void drawUebersicht(Graphics g, Color stroke) {
	int left = bodyLeft, top = bodyTop, right = bodyRight, bottom = bodyBottom;
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int r = Math.min(right - left, bottom - top) / 5;
	if (r < 10)
	    r = 10;
	int hook = Math.max(14, (right - left) / 6);
	int armLen = r + 2 + hook;
	int stemLen = hook + Math.max(6, r / 3);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(posts[0].x, posts[0].y, cx, cy - r - stemLen);
	g.drawLine(cx, cy - r - stemLen, cx, cy - r);
	drawThickCircle(g, cx, cy, r);
	drawUsArm(g, cx, cy, r, -75, armLen, true);   // cw
	drawUsArm(g, cx, cy, r, -105, armLen, false);  // ccw
	drawUsArm(g, cx, cy, r, 75, armLen, false);
	drawUsArm(g, cx, cy, r, 105, armLen, true);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, cx + r + 10, cy);
	adjustBbox(left, posts[0].y - 8, right + Math.max(tw, 70) + 24, bottom);
    }

    /** tickCw: Haken im Uhrzeigersinn um den Kreis (Workbench cw/ccw). */
    void drawUsArm(Graphics g, int cx, int cy, int r, double angleDeg,
		     int armLen, boolean tickCw) {
	double rad = angleDeg * Math.PI / 180;
	double c = Math.cos(rad);
	double s = Math.sin(rad);
	double r0 = r + 2.5;
	int x1 = (int) (cx + c * r0);
	int y1 = (int) (cy + s * r0);
	int x2 = (int) (cx + c * armLen);
	int y2 = (int) (cy + s * armLen);
	g.drawLine(x1, y1, x2, y2);
	double tick = Math.max(8, r * 0.7);
	double tx = -s;
	double ty = c;
	if (!tickCw) {
	    tx = -tx;
	    ty = -ty;
	}
	g.drawLine(x2, y2, (int) (x2 + tx * tick), (int) (y2 + ty * tick));
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Kreuzschalter (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	arr[1] = position == 0 ? "Stellung 1 (gerade)" : "Stellung 2 (Kreuz)";
	if (!uebersicht()) {
	    arr[2] = "1 = " + getVoltageText(volts[0]);
	    arr[3] = "2 = " + getVoltageText(volts[1]);
	    arr[4] = "3 = " + getVoltageText(volts[2]);
	    arr[5] = "4 = " + getVoltageText(volts[3]);
	} else {
	    arr[2] = "Anschluss = " + getVoltageText(volts[0]);
	}
    }

    public EditInfo getEditInfo(int n) {
	EditInfo noteEi = EGTNote.editAt(this, n, 1);
	if (noteEi != null)
	    return noteEi;
	n = EGTNote.shiftAfter(n, 1);
	if (n == 0)
	    return new EditInfo("Bezeichnung", designation);
	if (n == 1)
	    return EditInfo.createCheckbox(Locale.LS("Übersichtsdarstellung"),
					  uebersicht());
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (EGTNote.applyAt(this, n, 1, ei))
	    return;
	n = EGTNote.shiftAfter(n, 1);
	if (n == 0)
	    designation = ei.textf.getText();
	if (n == 1) {
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_UEBERSICHT);
	    allocPosts();
	    allocNodes();
	    setPoints();
	}
    }
}
