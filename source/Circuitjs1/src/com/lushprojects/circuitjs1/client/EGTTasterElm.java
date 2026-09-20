/*
    TAR-Dervic EGT extension: Taster tastend (ZH + Übersicht), I202
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.user.client.Timer;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT SPST Taster (momentary).
 * ZH: 1 oben / L unten (wie Serie/Wechsel). Default Schließer:
 * gedrückt = geschlossen, Loslassen öffnet. Optional Öffner (FLAG_TASTER_NC).
 * Übersicht: Doppelkreis + Stem; optional Leuchttaster (X im Innenkreis).
 * position 0 = gedrückt, position 1 = Ruhe.
 */
class EGTTasterElm extends SwitchElm implements EGTDesignatable {
    static final int MIN_W = 48;
    static final int MIN_H = 64;
    static final int LEAD_OUT = 16;
    static final int REALTIME_MIN_PRESS_MS = 120;

    String designation = "S";
    String note = "";
    long realtimePressStartedMs;
    Timer realtimeReleaseTimer;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }
    Point posts[];
    int bodyLeft, bodyTop, bodyRight, bodyBottom;

    public EGTTasterElm(int xx, int yy) {
	super(xx, yy, true); // momentary
	momentary = true;
	position = 1; // offen
	posCount = 2;
	x2 = xx + 64;
	y2 = yy + 96;
	allocPosts();
	allocNodes();
	setPoints();
    }

    public EGTTasterElm(int xa, int ya, int xb, int yb, int f,
			StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	momentary = true;
	posCount = 2;
	if (position > 1)
	    position = 1;
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

    boolean leuchttaster() {
	return (flags & EGTStyle.FLAG_LEUCHT) != 0;
    }

    boolean isNC() {
	return (flags & EGTStyle.FLAG_TASTER_NC) != 0;
    }

    /** Gedrückt halten. */
    boolean pressed() { return position == 0; }

    /** Leitend: Schließer nur gedrückt, Öffner in Ruhe. */
    boolean contactClosed() {
	return isNC() ? !pressed() : pressed();
    }

    void toggle() {
	cancelRealtimeRelease();
	super.toggle();
	realtimePressStartedMs = pressed() ? System.currentTimeMillis() : 0;
    }

    void setWorkbenchPressed(boolean down) {
	cancelRealtimeRelease();
	position = down ? 0 : 1;
	realtimePressStartedMs = down ? System.currentTimeMillis() : 0;
    }

    void mouseUp() {
	if (!momentary || app == null || app.ui == null || !app.ui.isEgtRealtimeMode()) {
	    super.mouseUp();
	    return;
	}
	long elapsed = Math.max(0, System.currentTimeMillis() - realtimePressStartedMs);
	int remaining = (int) Math.max(0, REALTIME_MIN_PRESS_MS - elapsed);
	if (remaining == 0) {
	    super.mouseUp();
	    return;
	}
	realtimeReleaseTimer = new Timer() {
	    public void run() {
		realtimeReleaseTimer = null;
		if (pressed()) {
		    EGTTasterElm.super.toggle();
		    realtimePressStartedMs = 0;
		    if (app != null) {
			app.needAnalyze();
			app.repaint();
		    }
		}
	    }
	};
	realtimeReleaseTimer.schedule(remaining);
    }

    void reset() {
	cancelRealtimeRelease();
	position = 1;
	realtimePressStartedMs = 0;
	super.reset();
    }

    private void cancelRealtimeRelease() {
	if (realtimeReleaseTimer != null) {
	    realtimeReleaseTimer.cancel();
	    realtimeReleaseTimer = null;
	}
    }

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

    int getDumpType() { return 448; }
    String getXmlDumpType() { return "EGTTaster"; }
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
	if (leuchttaster())
	    XMLSerializer.dumpAttr(elem, "leucht", 1);
	if (isNC())
	    XMLSerializer.dumpAttr(elem, "nc", 1);
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
	if (xml.parseIntAttr("leucht", 0) != 0)
	    flags |= EGTStyle.FLAG_LEUCHT;
	else
	    flags &= ~EGTStyle.FLAG_LEUCHT;
	if (xml.parseIntAttr("nc", 0) != 0)
	    flags |= EGTStyle.FLAG_TASTER_NC;
	else
	    flags &= ~EGTStyle.FLAG_TASTER_NC;
	momentary = true;
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
	return contactClosed();
    }

    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }

    void stamp() {
	if (uebersicht() || !contactClosed())
	    return;
	double r = resistance > 0 ? resistance : EGTSchuetzLink.R_ON;
	sim.stampResistor(nodes[0], nodes[1], r);
    }

    void calculateCurrent() {
	if (uebersicht() || !contactClosed()) {
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
	boolean closed = contactClosed();
	int openDx = Math.max(14, (right - left) * 28 / 100);
	int tipX;
	int tipY;
	if (isNC()) {
	    tipX = EGTStyle.ncTipX(cx, contactTop, pivotY, closed);
	    tipY = EGTStyle.ncTipY(contactTop, pivotY, closed);
	} else {
	    tipX = closed ? cx : cx - openDx;
	    tipY = closed ? contactTop : contactTop + Math.max(4, h / 25);
	}
	int midX = (cx + tipX) / 2;
	int midY = (pivotY + tipY) / 2;

	g.setColor(stroke);
	g.setLineWidth(2.0);
	g.drawRect(left, top, right - left, bottom - top);

	g.drawLine(posts[0].x, posts[0].y, cx, contactTop);
	g.drawLine(cx, pivotY, posts[1].x, posts[1].y);
	if (isNC())
	    EGTStyle.drawNcContact(g, cx, contactTop, pivotY, closed, stroke);
	else
	    EGTStyle.drawNoContact(g, cx, contactTop, pivotY, closed, stroke);
	EGTStyle.drawTastBetaetigung(g, midX, midY,
				     Math.max(22, (right - left) * 35 / 100),
				     stroke);

	if (leuchttaster()) {
	    int lr = Math.max(6, (right - left) / 10);
	    int lx = right - Math.max(12, (right - left) / 6);
	    int ly = top + Math.max(12, h / 6);
	    g.setColor(stroke);
	    g.setLineWidth(1.8);
	    drawThickCircle(g, lx, ly, lr);
	    EGTStyle.drawLampX(g, lx, ly, lr, stroke);
	}

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[0].x, posts[0].y, "1",
				    whiteColor);
	EGTStyle.drawPinLabelBeside(g, this, posts[1].x, posts[1].y, "L",
				    whiteColor);

	EGTStyle.drawDesigAndNote(g, this, designation, right + 8, top + 14);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	String st = (isNC() ? "Öffner" : "Schließer") + " · "
		+ (pressed() ? "gedrückt"
			     : (isNC() ? "geschlossen" : "offen"));
	if (leuchttaster())
	    st += " · beleuchtet";
	int twSt = (int) g.context.measureText(st).getWidth();
	g.drawString(st, right + 8, bottom - 6);
	adjustBbox(left, posts[0].y - 22,
		   right + Math.max(tw, twSt) + 16, posts[1].y + 18);
	g.setLineWidth(1.0);

	if (closed && Math.abs(current) >= EGTStyle.I_DOT_MIN) {
	    curcount = updateDotCount(current, curcount);
	    drawDots(g, posts[0], posts[1], curcount);
	} else
	    curcount = 0;
    }

    void drawUebersicht(Graphics g, Color stroke) {
	int left = bodyLeft, top = bodyTop, right = bodyRight, bottom = bodyBottom;
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;
	int r = Math.min(right - left, bottom - top) / 5;
	if (r < 10)
	    r = 10;
	int stemLen = Math.max(14, r + 8);

	g.setColor(stroke);
	g.setLineWidth(2.5);
	g.drawLine(posts[0].x, posts[0].y, cx, cy - r - stemLen);
	g.drawLine(cx, cy - r - stemLen, cx, cy - r);
	drawThickCircle(g, cx, cy, r);
	drawThickCircle(g, cx, cy, Math.max(4, (r * 45) / 100));
	if (leuchttaster())
	    EGTStyle.drawLampX(g, cx, cy, (r * 45) / 100, stroke);
	g.setLineWidth(1.0);

	g.setFont(EGTStyle.pinLabelFont());
	g.setColor(whiteColor);
	int tw = EGTStyle.measureDesigNote(g, this, designation);
	EGTStyle.drawDesigAndNote(g, this, designation, cx + r + 10, cy);
	if (isNC()) {
	    g.setColor(whiteColor);
	    g.drawString("Öffner", cx + r + 10, cy + 16);
	}
	adjustBbox(left, posts[0].y - 8, right + Math.max(tw, 90) + 24, bottom);
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Taster (" + designation + ")"
		+ (isNC() ? " [Öffner]" : " [Schließer]")
		+ (uebersicht() ? " [Übersicht]" : " [ZH]")
		+ (leuchttaster() ? " [Leuchttaster]" : "");
	arr[1] = pressed() ? "gedrückt"
			  : (isNC() ? "geschlossen" : "offen");
	if (!uebersicht()) {
	    arr[2] = "1 = " + getVoltageText(volts[0]);
	    arr[3] = "L = " + getVoltageText(volts[1]);
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
	if (n == 2)
	    return EGTStyle.kontaktartInfo(isNC());
	if (n == 3)
	    return EditInfo.createCheckbox(Locale.LS("Leuchttaster"),
					  leuchttaster());
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
	if (n == 2) {
	    flags = EGTStyle.applyKontaktart(flags, EGTStyle.FLAG_TASTER_NC, ei);
	    if (CircuitElm.app != null)
		CircuitElm.app.needAnalyze();
	}
	if (n == 3)
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_LEUCHT);
    }
}
