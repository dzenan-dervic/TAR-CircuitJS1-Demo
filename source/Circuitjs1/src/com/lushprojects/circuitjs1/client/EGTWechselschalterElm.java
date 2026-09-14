/*
    TAR-Dervic EGT extension: Wechselschalter rastend (ZH + Übersicht), I202
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT Wechselschalter (SPDT): L unten, 1/2 oben; Klick wechselt Stellung.
 * ZH: 3 Posts. Übersicht: je ein mittiger Kabel-Anschluss oben und unten.
 * position 0 = Stellung 1, position 1 = Stellung 2.
 */
class EGTWechselschalterElm extends SwitchElm implements EGTDesignatable {
    static final int MIN_W = 64;
    static final int MIN_H = 64;
    static final int LEAD_OUT = 16;
    static final int FLAG_UEBERSICHT_ABGANG = 1 << 20;

    String designation = "S2";
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

    public EGTWechselschalterElm(int xx, int yy) {
	super(xx, yy);
	momentary = false;
	position = 0; // Stellung 1
	posCount = 2;
	x2 = xx + 80;
	y2 = yy + 96;
	allocPosts();
	allocNodes();
	setPoints();
    }

    public EGTWechselschalterElm(int xa, int ya, int xb, int yb, int f,
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

    boolean uebersichtAbgang() {
	return uebersicht() && (flags & FLAG_UEBERSICHT_ABGANG) != 0;
    }

    /** Stellung 1 oder 2 (Arbeitsblatt). */
    int stellung() { return position == 0 ? 1 : 2; }

    void allocPosts() {
	posts = new Point[getPostCount()];
	for (int i = 0; i < posts.length; i++)
	    posts[i] = new Point();
    }

    int getPostCount() { return uebersicht() ? (uebersichtAbgang() ? 2 : 1) : 3; }

    Point getPost(int n) {
	if (posts == null || n < 0 || n >= posts.length)
	    return super.getPost(n);
	return posts[n];
    }

    int getDumpType() { return 446; }
    String getXmlDumpType() { return "EGTWechselschalter"; }
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
	int cx = snapGrid((bodyLeft + bodyRight) / 2);
	int lead = Math.max(LEAD_OUT, (bodyBottom - bodyTop) / 8);
	lead = snapGrid(lead);
	if (lead < LEAD_OUT)
	    lead = LEAD_OUT;
	int inset = Math.max(16, snapGrid((bodyRight - bodyLeft) / 4));

	if (uebersicht()) {
	    posts[0].x = cx;
	    posts[0].y = snapGrid(bodyTop - lead);
	    if (uebersichtAbgang()) {
		posts[1].x = cx;
		posts[1].y = snapGrid(bodyBottom + lead);
	    }
	} else {
	    // 1 / 2 oben, L unten
	    posts[0].x = snapGrid(bodyLeft + inset);
	    posts[0].y = snapGrid(bodyTop - lead);
	    posts[1].x = snapGrid(bodyRight - inset);
	    posts[1].y = posts[0].y;
	    if (posts[1].x <= posts[0].x)
		posts[1].x = snapGrid(posts[0].x + 16);
	    posts[2].x = cx;
	    posts[2].y = snapGrid(bodyBottom + lead);
	    point1 = posts[2];
	    point2 = posts[position]; // aktiver Wurf
	}
	setBbox(bodyLeft, Math.min(bodyTop, posts[0].y),
		bodyRight, Math.max(bodyBottom,
				    uebersicht()
					    ? (uebersichtAbgang() ? posts[1].y : bodyBottom)
					    : posts[2].y));
    }

    boolean getConnection(int n1, int n2) {
	if (uebersicht())
	    return false;
	int active = position; // 0→Post1, 1→Post2
	return comparePair(n1, n2, active, 2);
    }

    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }

    void stamp() {
	if (uebersicht())
	    return;
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	sim.stampResistor(nodes[position], nodes[2], r);
    }

    void calculateCurrent() {
	if (uebersicht()) {
	    current = 0;
	    return;
	}
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	current = (volts[position] - volts[2]) / r;
	if (Math.abs(current) < EGTStyle.I_DOT_MIN)
	    current = 0;
    }

    double getCurrentIntoNode(int n) {
	if (uebersicht())
	    return 0;
	if (n == position)
	    return -current;
	if (n == 2)
	    return current;
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
	int pivotY = bottom - Math.max(14, h * 16 / 100);
	int p1x = posts[0].x;
	int p2x = posts[1].x;
	int lx = posts[2].x;
	int tipX = position == 0 ? p1x : p2x;

	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.drawRect(left, top, right - left, bottom - top);

	g.drawLine(posts[0].x, posts[0].y, p1x, contactTop);
	g.drawLine(posts[1].x, posts[1].y, p2x, contactTop);
	g.drawLine(posts[2].x, posts[2].y, lx, pivotY);
	g.drawLine(p1x - 5, contactTop, p1x + 5, contactTop);
	g.drawLine(p2x - 5, contactTop, p2x + 5, contactTop);
	// aktiver Kontakt betont
	g.setLineWidth(2.5);
	g.drawLine(tipX - 5, contactTop, tipX + 5, contactTop);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(lx, pivotY, tipX, contactTop);
	int midX = (lx + tipX) / 2;
	int midY = (pivotY + contactTop) / 2;
	EGTStyle.drawRastBetaetigung(g, midX, midY,
				     Math.max(22, (right - left) / 4),
				     true, stroke);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[0].x, posts[0].y, "1",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[1].x, posts[1].y, "2",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[2].x, posts[2].y, "L",
				    whiteColor);

	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, right + 8, top + 14);
	g.drawString("Stellung " + stellung(), right + 8, bottom - 6);
	adjustBbox(left, posts[0].y - 22, right + Math.max(tw, 70) + 16,
		   posts[2].y + 18);
	g.setLineWidth(1.0);

	curcount = updateDotCount(current, curcount);
	if (curcount != 0)
	    EGTStyle.drawDotsAlong(this, g, curcount, new Point[] {
		posts[2], new Point(lx, pivotY),
		new Point(tipX, contactTop), posts[position]
	    });
    }

    void drawUebersicht(Graphics g, Color stroke) {
	int left = bodyLeft, top = bodyTop, right = bodyRight, bottom = bodyBottom;
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int r = Math.min(right - left, bottom - top) / 5;
	if (r < 10)
	    r = 10;
	int armLen = r + Math.max(14, (right - left) / 5);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(posts[0].x, posts[0].y, cx, cy - r);
	if (uebersichtAbgang())
	    g.drawLine(cx, cy + r, posts[1].x, posts[1].y);
	drawThickCircle(g, cx, cy, r);
	// Beide Arme gleich weit im Uhrzeigersinn gedreht; Abstand bleibt 180°.
	drawUsArm(g, cx, cy, r, -60, armLen);
	drawUsArm(g, cx, cy, r, 120, armLen);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, cx + r + 10, cy);
	adjustBbox(left, posts[0].y - 8, right + Math.max(tw, 70) + 24,
		   uebersichtAbgang() ? posts[1].y + 8 : bottom);
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
	if (angleDeg > 0) {
	    // unten: Haken nach Workbench-Seite
	    tx = -tx;
	    ty = -ty;
	}
	g.drawLine(x2, y2, (int) (x2 + tx * tick), (int) (y2 + ty * tick));
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Wechselschalter (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	arr[1] = "Stellung " + stellung();
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
	if (n == 2 && uebersicht())
	    return EditInfo.createCheckbox(Locale.LS("Unterer Anschluss"),
					  uebersichtAbgang());
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
	if (n == 2 && uebersicht()) {
	    flags = ei.changeFlag(flags, FLAG_UEBERSICHT_ABGANG);
	    allocPosts();
	    allocNodes();
	    setPoints();
	}
    }
}
