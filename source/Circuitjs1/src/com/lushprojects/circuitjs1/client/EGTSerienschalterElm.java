/*
    TAR-Dervic EGT extension: Serienschalter rastend (ZH + Übersicht), I202
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Serienschalter: gemeinsamer L, Ausgänge 1 und 2 (unabhängig, rastend).
 * ZH: 3 Posts (1, 2, L). Übersicht: 1 Kabel-Anschluss.
 * Klick links = Wippe 1, rechts = Wippe 2.
 */
class EGTSerienschalterElm extends SwitchElm implements EGTDesignatable {
    static final int MIN_W = 64;
    static final int MIN_H = 64;
    static final int LEAD_OUT = 16;
    static final Color COL_OPEN = new Color(0xff, 0xb3, 0x47);

    String designation = "S";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    /** true = EIN (geschlossen) */
    boolean closed1 = true;
    boolean closed2 = true;
    Point posts[];
    int bodyLeft, bodyTop, bodyRight, bodyBottom;
    double current1, current2, curcount1, curcount2;

    public EGTSerienschalterElm(int xx, int yy) {
	super(xx, yy);
	momentary = false;
	position = 0;
	x2 = xx + 80;
	y2 = yy + 96;
	allocPosts();
	allocNodes();
	setPoints();
    }

    public EGTSerienschalterElm(int xa, int ya, int xb, int yb, int f,
				StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	momentary = false;
	if (st.hasMoreTokens()) {
	    try {
		int w = Integer.parseInt(st.nextToken());
		int h = Integer.parseInt(st.nextToken());
		if (w > 0 && h > 0) {
		    x2 = x + w;
		    y2 = y + h;
		}
		if (st.hasMoreTokens())
		    closed1 = Integer.parseInt(st.nextToken()) != 0;
		if (st.hasMoreTokens())
		    closed2 = Integer.parseInt(st.nextToken()) != 0;
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

    void allocPosts() {
	posts = new Point[getPostCount()];
	for (int i = 0; i < posts.length; i++)
	    posts[i] = new Point();
    }

    // ZH: 1, 2, L  |  Übersicht: 1 Kabel
    int getPostCount() { return uebersicht() ? 1 : 3; }

    Point getPost(int n) {
	if (posts == null || n < 0 || n >= posts.length)
	    return super.getPost(n);
	return posts[n];
    }

    int getDumpType() { return 445; }
    String getXmlDumpType() { return "EGTSerienschalter"; }
    int getShortcut() { return 0; }

    String dump() {
	int w = Math.abs(x2 - x);
	int h = Math.abs(y2 - y);
	return super.dump() + " " + position + " " + momentary + " " + w + " "
		+ h + " " + (closed1 ? 1 : 0) + " " + (closed2 ? 1 : 0) + " "
		+ CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
	XMLSerializer.dumpAttr(elem, "w", Math.abs(x2 - x));
	XMLSerializer.dumpAttr(elem, "h", Math.abs(y2 - y));
	XMLSerializer.dumpAttr(elem, "c1", closed1 ? 1 : 0);
	XMLSerializer.dumpAttr(elem, "c2", closed2 ? 1 : 0);
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
	closed1 = xml.parseIntAttr("c1", closed1 ? 1 : 0) != 0;
	closed2 = xml.parseIntAttr("c2", closed2 ? 1 : 0) != 0;
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
	int cx = snapGrid((bodyLeft + bodyRight) / 2);
	int lead = Math.max(LEAD_OUT, (bodyBottom - bodyTop) / 8);
	lead = snapGrid(lead);
	if (lead < LEAD_OUT)
	    lead = LEAD_OUT;
	int inset = Math.max(16, snapGrid((bodyRight - bodyLeft) / 4));

	if (uebersicht()) {
	    posts[0].x = cx;
	    posts[0].y = snapGrid(bodyTop - lead);
	} else {
	    // Workbench: 1/2 oben, L unten
	    posts[0].x = snapGrid(bodyLeft + inset);
	    posts[0].y = snapGrid(bodyTop - lead);
	    posts[1].x = snapGrid(bodyRight - inset);
	    posts[1].y = posts[0].y;
	    if (posts[1].x <= posts[0].x)
		posts[1].x = snapGrid(posts[0].x + 16);
	    posts[2].x = cx;
	    posts[2].y = snapGrid(bodyBottom + lead);
	    point1 = posts[2];
	    point2 = posts[0];
	}
	setBbox(bodyLeft, Math.min(bodyTop, posts[0].y),
		bodyRight, Math.max(bodyBottom,
				    uebersicht() ? bodyBottom : posts[2].y));
    }

    void toggleAt(int mx, int my) {
	TestManager.recordSwitchToggle(this);
	int mid = (bodyLeft + bodyRight) / 2;
	if (mx < mid)
	    closed1 = !closed1;
	else
	    closed2 = !closed2;
    }

    boolean getConnection(int n1, int n2) {
	if (uebersicht())
	    return false;
	if (comparePair(n1, n2, 0, 2))
	    return closed1;
	if (comparePair(n1, n2, 1, 2))
	    return closed2;
	return false;
    }

    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }

    void stamp() {
	if (uebersicht())
	    return;
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	if (closed1)
	    sim.stampResistor(nodes[0], nodes[2], r);
	if (closed2)
	    sim.stampResistor(nodes[1], nodes[2], r);
    }

    void calculateCurrent() {
	if (uebersicht()) {
	    current1 = current2 = current = 0;
	    return;
	}
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	current1 = closed1 ? (volts[0] - volts[2]) / r : 0;
	current2 = closed2 ? (volts[1] - volts[2]) / r : 0;
	if (Math.abs(current1) < EGTStyle.I_DOT_MIN)
	    current1 = 0;
	if (Math.abs(current2) < EGTStyle.I_DOT_MIN)
	    current2 = 0;
	current = current1 + current2;
    }

    double getCurrentIntoNode(int n) {
	if (uebersicht())
	    return 0;
	if (n == 0)
	    return -current1;
	if (n == 1)
	    return -current2;
	if (n == 2)
	    return current1 + current2;
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
	int contactTop = top + Math.max(14, h * 16 / 100);
	int pivotY = bottom - Math.max(14, h * 18 / 100);
	int p1x = posts[0].x;
	int p2x = posts[1].x;
	int lx = posts[2].x;

	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.drawRect(left, top, right - left, bottom - top);

	// L unten → Brücke → beide Drehpunkte; 1/2 von oben
	g.drawLine(posts[2].x, posts[2].y, lx, pivotY);
	g.drawLine(p1x, pivotY, p2x, pivotY);
	g.drawLine(posts[0].x, posts[0].y, p1x, contactTop);
	g.drawLine(posts[1].x, posts[1].y, p2x, contactTop);
	g.drawLine(p1x - 5, contactTop, p1x + 5, contactTop);
	g.drawLine(p2x - 5, contactTop, p2x + 5, contactTop);

	drawPole(g, stroke, p1x, pivotY, contactTop, closed1);
	drawPole(g, stroke, p2x, pivotY, contactTop, closed2);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[0].x, posts[0].y, "1",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[1].x, posts[1].y, "2",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[2].x, posts[2].y, "L",
				    whiteColor);

	g.setFont(EGTStyle.pinLabelFont());
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, right + 8, top + 14);
	String st = (closed1 ? "1 EIN" : "1 AUS") + " · "
		+ (closed2 ? "2 EIN" : "2 AUS");
	g.drawString(st, right + 8, bottom - 6);
	adjustBbox(left, posts[0].y - 22, right + Math.max(tw, 90) + 16,
		   posts[2].y + 18);
	g.setLineWidth(1.0);

	Point lPivot = new Point(lx, pivotY);
	if (closed1) {
	    curcount1 = updateDotCount(current1, curcount1);
	    EGTStyle.drawDotsAlong(this, g, curcount1, new Point[] {
		posts[2], lPivot, new Point(p1x, pivotY),
		new Point(p1x, contactTop), posts[0]
	    });
	}
	if (closed2) {
	    curcount2 = updateDotCount(current2, curcount2);
	    EGTStyle.drawDotsAlong(this, g, curcount2, new Point[] {
		posts[2], lPivot, new Point(p2x, pivotY),
		new Point(p2x, contactTop), posts[1]
	    });
	}
    }

    void drawPole(Graphics g, Color stroke, int px, int pivotY, int contactTop,
		  boolean on) {
	int tipX = on ? px : px - Math.max(12, (bodyRight - bodyLeft) / 8);
	int tipY = on ? contactTop : contactTop + 4;
	int midX = (px + tipX) / 2;
	int midY = (pivotY + tipY) / 2;
	g.setColor(on ? stroke : COL_OPEN);
	g.setLineWidth(2.5);
	g.drawLine(px, pivotY, tipX, tipY);
	EGTStyle.drawRastBetaetigung(g, midX, midY,
				     Math.max(18, (bodyRight - bodyLeft) / 5),
				     true, stroke);
    }

    void drawUebersicht(Graphics g, Color stroke) {
	int left = bodyLeft, top = bodyTop, right = bodyRight, bottom = bodyBottom;
	// Derselbe gerasterte Mittelpunkt wie der Kabel-Post. Ohne diese Zuordnung
	// konnte die mittlere Zuleitung bei ungeraden Abmessungen leicht schräg
	// zwischen Post und Symbolkreis verlaufen.
	int cx = posts[0].x;
	int cy = (top + bottom) / 2;
	int r = Math.min(right - left, bottom - top) / 5;
	if (r < 10)
	    r = 10;
	int armLen = r + Math.max(14, (right - left) / 5);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(posts[0].x, posts[0].y, cx, cy - r);
	drawThickCircle(g, cx, cy, r);
	drawUsArm(g, cx, cy, r, -105, armLen);
	drawUsArm(g, cx, cy, r, -75, armLen);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, cx + r + 10, cy);
	adjustBbox(left, posts[0].y - 8, right + tw + 24, bottom);
    }

    void drawUsArm(Graphics g, int cx, int cy, int r, double angleDeg,
		     int armLen) {
	double rad = angleDeg * Math.PI / 180;
	double c = Math.cos(rad);
	double s = Math.sin(rad);
	double r0 = r + 2.5;
	int x1 = (int) (cx + c * r0);
	int y1 = (int) (cy + s * r0);
	int x2 = (int) (cx + c * armLen);
	int y2 = (int) (cy + s * armLen);
	g.drawLine(x1, y1, x2, y2);
	double tick = Math.max(8, r * 0.55);
	double tx = -s;
	double ty = c;
	if (ty < 0) {
	    tx = -tx;
	    ty = -ty;
	}
	g.drawLine(x2, y2, (int) (x2 + tx * tick), (int) (y2 + ty * tick));
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Serienschalter (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	arr[1] = (closed1 ? "1 EIN" : "1 AUS") + " · "
		+ (closed2 ? "2 EIN" : "2 AUS");
	if (!uebersicht()) {
	    arr[2] = "1 = " + getVoltageText(volts[0]);
	    arr[3] = "2 = " + getVoltageText(volts[1]);
	    arr[4] = "L = " + getVoltageText(volts[2]);
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
