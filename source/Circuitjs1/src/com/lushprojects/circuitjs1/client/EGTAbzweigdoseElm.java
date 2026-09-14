/*
    TAR-Dervic EGT extension: Abzweigdose (ZH Kasten / Übersicht Kreis+Punkt), I203 / INST-03
    Rein grafisch — keine Klemmen am Symbol; Verdrahtung über EGT-Leitungen.
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT junction box: ZH empty square with designation; Übersicht circle + center dot.
 * Zero posts (wires meet by shared coordinates, not via built-in terminals).
 */
class EGTAbzweigdoseElm extends GraphicElm implements EGTDesignatable {
    String designation = "X1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTAbzweigdoseElm(int xx, int yy) {
	super(xx, yy);
	x2 = xx;
	y2 = yy;
	setBbox(x, y, x2, y2);
    }

    public EGTAbzweigdoseElm(int xa, int ya, int xb, int yb, int f,
			     StringTokenizer st) {
	super(xa, ya, xb, yb, f);
	x2 = xb;
	y2 = yb;
	setBbox(x, y, x2, y2);
	if (st.hasMoreTokens())
	    designation = CustomLogicModel.unescape(st.nextToken());
    
	note = EGTNote.readOptional(st);
}

    boolean uebersicht() {
	return (flags & EGTStyle.FLAG_UEBERSICHT) != 0;
    }

    int getDumpType() { return 442; }
    String getXmlDumpType() { return "EGTAbzweigdose"; }
    int getShortcut() { return 0; }

    /** Zwei Eck-Handles trotz 0 Posts — sonst ist die Dose nach dem Platzieren nicht mehr skalierbar. */
    int getNumHandles() { return 2; }

    /**
     * Quadrat (min. 32 px) von festem Eckpunkt {@code ox,oy} zur Maus {@code tx,ty}.
     * {@code originIsFirst}: (x,y) bleibt der Ursprung, sonst (x2,y2).
     */
    void sizeFromCorner(int ox, int oy, int tx, int ty, boolean originIsFirst) {
	int s = Math.max(Math.abs(tx - ox), Math.abs(ty - oy));
	if (s < 32)
	    s = 32;
	int sx = ox + ((tx >= ox) ? s : -s);
	int sy = oy + ((ty >= oy) ? s : -s);
	if (originIsFirst) {
	    x = ox;
	    y = oy;
	    x2 = sx;
	    y2 = sy;
	} else {
	    x = sx;
	    y = sy;
	    x2 = ox;
	    y2 = oy;
	}
    }

    void drag(int xx, int yy) {
	sizeFromCorner(x, y, xx, yy, true);
    }

    void movePoint(int n, int dx, int dy) {
	if (n == 0)
	    sizeFromCorner(x2, y2, x + dx, y + dy, false);
	else
	    sizeFromCorner(x, y, x2 + dx, y2 + dy, true);
	setPoints();
    }

    boolean creationFailed() {
	return Math.abs(x2 - x) < 32 || Math.abs(y2 - y) < 32;
    }

    void draw(Graphics g) {
	setBbox(x, y, x2, y2);
	Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	int left = Math.min(x, x2);
	int top = Math.min(y, y2);
	int right = Math.max(x, x2);
	int bottom = Math.max(y, y2);
	int cx = (left + right) / 2;
	int cy = (top + bottom) / 2;

	g.setColor(stroke);
	if (uebersicht()) {
	    int r = Math.min(right - left, bottom - top) / 6;
	    if (r < 6)
		r = 6;
	    drawThickCircle(g, cx, cy, r);
	    g.setColor(stroke);
	    g.fillOval(cx - r / 3, cy - r / 3, (2 * r) / 3, (2 * r) / 3);
	} else {
	    g.setLineWidth(2.0);
	    g.drawRect(left, top, right - left, bottom - top);
	    g.setLineWidth(1.0);
	}

	if (designation != null && designation.length() > 0) {
	    g.setFont(EGTStyle.pinLabelFont());
	    g.setColor(whiteColor);
	    int tw = EGTStyle.measureDesigNote(g, this, designation);
	    if (uebersicht()) {
		int r = Math.min(right - left, bottom - top) / 6;
		if (r < 6)
		    r = 6;
		EGTStyle.drawDesigAndNote(g, this, designation,
					  cx - tw / 2, cy - r - 10);
		adjustBbox(left, cy - r - 22, right, bottom);
	    } else {
		EGTStyle.drawDesigAndNote(g, this, designation,
					  right + 8, top + 12);
		adjustBbox(left, top, right + tw + 16, bottom);
	    }
	}
    }

    String dump() {
	return super.dump() + " " + CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
    }

    void getInfo(String arr[]) {
	arr[0] = "EGT-Abzweigdose (" + designation + ")"
		+ (uebersicht() ? " [Übersicht]" : " [ZH]");
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
	if (n == 1)
	    flags = ei.changeFlag(flags, EGTStyle.FLAG_UEBERSICHT);
    }

    int getMouseDistance(int gx, int gy) {
	int thresh = 10;
	int left = Math.min(x, x2);
	int top = Math.min(y, y2);
	int right = Math.max(x, x2);
	int bottom = Math.max(y, y2);
	if (gx >= left - thresh && gx <= right + thresh
		&& gy >= top - thresh && gy <= bottom + thresh) {
	    // Distanz zum Rand (innen = 0)
	    int dx = Math.min(Math.abs(gx - left), Math.abs(gx - right));
	    int dy = Math.min(Math.abs(gy - top), Math.abs(gy - bottom));
	    if (gx > left && gx < right && gy > top && gy < bottom)
		return Math.min(dx, dy) * Math.min(dx, dy);
	    return 0;
	}
	return -1;
    }
}
