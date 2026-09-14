/*
    TAR-Dervic EGT extension: Ausschalter rastend (ZH + Übersicht), I202 / MOD-INST-02
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT SPST Ausschalter (rastend).
 * ZH: Gehäuse, L oben / 1 unten; Klick schaltet.
 * Übersicht: Kreis + Stem + 1 Arm; ein Kabel-Anschluss oben.
 * Größe per Mauszug beim Platzieren.
 */
class EGTAusschalterElm extends SwitchElm implements EGTDesignatable {
    static final int MIN_W = 48;
    static final int MIN_H = 64;
    static final int LEAD_OUT = 16;
    static final Color COL_OPEN = new Color(0xff, 0xb3, 0x47);

    String designation = "S1";
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

    public EGTAusschalterElm(int xx, int yy) {
	super(xx, yy);
	momentary = false;
	position = 0; // EIN (wie Workbench-Default)
	x2 = xx + 64;
	y2 = yy + 96;
	allocPosts();
	allocNodes();
	setPoints();
    }

    public EGTAusschalterElm(int xa, int ya, int xb, int yb, int f,
			     StringTokenizer st) {
	super(xa, ya, xb, yb, f, st); // liest position + momentary
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

    boolean closed() { return position == 0; }

    void allocPosts() {
	posts = new Point[getPostCount()];
	for (int i = 0; i < posts.length; i++)
	    posts[i] = new Point();
    }

    int getPostCount() { return uebersicht() ? 1 : 2; }

    Point getPost(int n) {
	if (posts == null || n < 0 || n >= posts.length)
	    return super.getPost(n);
	return posts[n];
    }

    int getDumpType() { return 444; }
    String getXmlDumpType() { return "EGTAusschalter"; }
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
	allocPosts();
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
	// Posts müssen auf dem Raster liegen, sonst greifen Leitungen nicht
	int cx = snapGrid((bodyLeft + bodyRight) / 2);
	int lead = Math.max(LEAD_OUT, (bodyBottom - bodyTop) / 8);
	lead = snapGrid(lead);
	if (lead < LEAD_OUT)
	    lead = LEAD_OUT;

	if (uebersicht()) {
	    posts[0].x = cx;
	    posts[0].y = snapGrid(bodyTop - lead);
	} else {
	    posts[0].x = cx;
	    posts[0].y = snapGrid(bodyTop - lead);
	    posts[1].x = cx;
	    posts[1].y = snapGrid(bodyBottom + lead);
	    point1 = posts[0];
	    point2 = posts[1];
	}
	setBbox(bodyLeft, Math.min(bodyTop, posts[0].y),
		bodyRight, Math.max(bodyBottom,
				    uebersicht() ? bodyBottom : posts[1].y));
    }

    boolean getConnection(int n1, int n2) {
	if (uebersicht())
	    return false;
	return position == 0;
    }

    boolean isWireEquivalent() { return false; }

    // Nicht als Wire wegoptimieren. Kurzschluss daher explizit stempeln.
    boolean isRemovableWire() { return false; }

    void stamp() {
	if (uebersicht() || position != 0)
	    return;
	// Geschlossen: R_ON 50 mΩ (wie Taster/Schütz). Nicht 1 mΩ — Solver-Geisterstrom.
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	sim.stampResistor(nodes[0], nodes[1], r);
    }

    void calculateCurrent() {
	if (uebersicht() || position == 1) {
	    current = 0;
	    return;
	}
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	current = (volts[0] - volts[1]) / r;
	if (Math.abs(current) < EGTStyle.I_DOT_MIN)
	    current = 0;
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
	int cx = posts[0].x;
	int h = bottom - top;
	int contactTop = top + Math.max(14, h * 18 / 100);
	int pivotY = bottom - Math.max(14, h * 18 / 100);
	boolean on = closed();
	int tipX = on ? cx : cx - Math.max(14, (right - left) * 28 / 100);
	int tipY = on ? contactTop : contactTop + Math.max(4, h / 25);
	int midX = (cx + tipX) / 2;
	int midY = (pivotY + tipY) / 2;

	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.drawRect(left, top, right - left, bottom - top);

	// L / 1: Post außerhalb → in die Box bis Kontakt
	g.drawLine(posts[0].x, posts[0].y, cx, contactTop);
	g.drawLine(cx, pivotY, posts[1].x, posts[1].y);
	g.drawLine(cx - 5, contactTop, cx + 5, contactTop);

	// Messer
	g.setColor(on ? stroke : COL_OPEN);
	g.setLineWidth(2.5);
	g.drawLine(cx, pivotY, tipX, tipY);
	EGTStyle.drawRastBetaetigung(g, midX, midY,
				     Math.max(22, (right - left) * 35 / 100),
				     true, stroke);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[0].x, posts[0].y, "L",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[1].x, posts[1].y, "1",
				    whiteColor);

	g.setFont(EGTStyle.pinLabelFont());
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, right + 8, top + 14);
	String st = on ? "EIN" : "AUS";
	g.drawString(st, right + 8, bottom - 6);
	adjustBbox(left, posts[0].y - 22, right + Math.max(tw, 28) + 16,
		   posts[1].y + 18);
	g.setLineWidth(1.0);

	if (on) {
	    curcount = updateDotCount(current, curcount);
	    drawDots(g, posts[0], posts[1], curcount);
	}
    }

    void drawUebersicht(Graphics g, Color stroke) {
	int left = bodyLeft, top = bodyTop, right = bodyRight, bottom = bodyBottom;
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int r = Math.min(right - left, bottom - top) / 5;
	if (r < 10)
	    r = 10;
	int stemTop = posts[0].y;
	int armLen = r + Math.max(14, (right - left) / 5);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(posts[0].x, posts[0].y, cx, cy - r);
	drawThickCircle(g, cx, cy, r);

	// Arm nach oben-rechts (~-75°), Haken nach unten
	double rad = -75 * Math.PI / 180;
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
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	int extra = EGTStyle.drawDesigAndNote(g, this, designation,
					     cx + r + 10, cy);
	g.drawString(closed() ? "EIN" : "AUS", cx + r + 10, cy + 16 + extra);
	adjustBbox(left, stemTop - 8, right + tw + 24, bottom);
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Ausschalter (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
	arr[1] = closed() ? "EIN" : "AUS";
	if (!uebersicht()) {
	    arr[2] = "L = " + getVoltageText(volts[0]);
	    arr[3] = "1 = " + getVoltageText(volts[1]);
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
