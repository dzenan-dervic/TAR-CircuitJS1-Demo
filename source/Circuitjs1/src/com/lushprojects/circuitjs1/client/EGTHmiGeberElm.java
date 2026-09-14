/*
    TAR-Dervic EGT: HMI-Geber (Klemme I1…I8 aus der HTML-Anlage), Dump 469
    Elektrisch = ExtVoltageElm (setExtVoltage über JSInterface, unverändert).
    Optik: Klemmenkasten mit Adresse, Klartext und Zustands-LED statt dünner Schiene.
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Geber/Taster der HTML-Anlage an einer SPS-Eingangsklemme. Die HMI schreibt
 * über {@code setExtVoltage(name, v)}; gesucht wird per {@code instanceof
 * ExtVoltageElm} und Name — deshalb genügt die Ableitung ohne JS-Änderung.
 */
class EGTHmiGeberElm extends ExtVoltageElm implements EGTDesignatable {
    static final Color COL_HI = new Color(0x2e, 0xcc, 0x71);
    static final Color COL_LED_OFF = new Color(0x33, 0x41, 0x55);
    /** Alles deutlich über 0 gilt als Signal 1 — 230 V Klemme wie 24 V Geber. */
    static final double V_SIGNAL = 1.0;

    /** Klartext der Betriebsmittelfunktion, z. B. „S1 Taster EIN“. */
    String funktion;

    public String egtDesignation() { return name; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    name = d;
    }
    public String egtNote() { return ""; }
    public void setEgtNote(String s) {}

    public EGTHmiGeberElm(int xx, int yy) {
	super(xx, yy);
	name = "I1";
	funktion = "";
    }

    public EGTHmiGeberElm(int xa, int ya, int xb, int yb, int f,
			  StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	funktion = st.hasMoreTokens()
		? CustomLogicModel.unescape(st.nextToken()) : "";
    }

    int getDumpType() { return 469; }
    String getXmlDumpType() { return "EGTHmiGeber"; }
    int getShortcut() { return 0; }

    /**
     * Volles Legacy-Token-Format schreiben — ExtVoltageElm verlässt sich auf
     * XML und verlöre Name und Klartext beim Text-Export.
     */
    String dump() {
	return super.dump() + " " + waveform + " " + frequency + " "
		+ maxVoltage + " " + bias + " " + phaseShift + " " + dutyCycle
		+ " " + CustomLogicModel.escape(name == null ? "" : name)
		+ " " + CustomLogicModel.escape(funktion == null ? "" : funktion);
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "fkt", funktion == null ? "" : funktion);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	funktion = xml.parseStringAttr("fkt", "");
    }

    boolean signalHigh() {
	return Math.abs(getVoltage()) >= V_SIGNAL;
    }

    /**
     * Kompakter Klemmenkasten (LED + Adresse) direkt am Post; der Klartext steht
     * bei senkrechtem Stiel gedreht darüber — sonst kollidieren die Kästen im
     * 48-px-Raster der Mini-SPS-Klemmenleiste.
     */
    void draw(Graphics g) {
	g.save();
	try {
	    boolean hi = signalHigh();
	    String addr = (name == null) ? "" : name;
	    String fkt = (funktion == null) ? "" : funktion;

	    int fsAddr = 13;
	    int fsFkt = 11;
	    int ledD = 9;
	    int pad = 6;
	    g.setFont(new Font("bold", 0, fsAddr));
	    int wAddr = (int) g.context.measureText(addr).getWidth();
	    int boxW = Math.max(40, wAddr + ledD + 3 * pad);
	    int boxH = 22;

	    double dx = point2.x - point1.x;
	    double dy = point2.y - point1.y;
	    double len = Math.sqrt(dx * dx + dy * dy);
	    if (len < 1)
		len = 1;
	    double ux = dx / len;
	    double uy = dy / len;
	    boolean vertical = Math.abs(uy) >= Math.abs(ux);
	    double tx = Math.abs(ux) > 1e-6 ? (boxW / 2.0) / Math.abs(ux)
					    : Double.MAX_VALUE;
	    double ty = Math.abs(uy) > 1e-6 ? (boxH / 2.0) / Math.abs(uy)
					    : Double.MAX_VALUE;
	    double t = Math.min(tx, ty);
	    if (t > len)
		t = len;
	    lead1 = new Point((int) Math.round(point2.x - ux * t),
			      (int) Math.round(point2.y - uy * t));

	    int bx = point2.x - boxW / 2;
	    int by = point2.y - boxH / 2;

	    setVoltageColor(g, volts[0]);
	    drawThickLine(g, point1, lead1);

	    g.setColor(Color.white);
	    g.fillRect(bx, by, boxW, boxH);
	    g.setColor(needsHighlight() ? selectColor
					: (hi ? COL_HI : Color.black));
	    g.setLineWidth(hi ? 2.2 : 1.6);
	    g.drawRect(bx, by, boxW, boxH);

	    int ledX = bx + pad;
	    int ledY = point2.y - ledD / 2;
	    g.setColor(hi ? COL_HI : COL_LED_OFF);
	    g.fillOval(ledX, ledY, ledD, ledD);
	    g.setColor(Color.black);
	    g.setLineWidth(1.0);
	    g.drawRect(ledX, ledY, ledD, ledD);

	    g.setColor(Color.black);
	    g.setFont(new Font("bold", 0, fsAddr));
	    g.drawString(addr, ledX + ledD + pad, point2.y + fsAddr / 3);

	    int fx1 = bx, fy1 = by, fx2 = bx + boxW, fy2 = by + boxH;
	    if (fkt.length() > 0) {
		g.setFont(new Font("normal", 0, fsFkt));
		int wFkt = (int) g.context.measureText(fkt).getWidth();
		g.setColor(needsHighlight() ? selectColor : whiteColor);
		g.context.setTextAlign("center");
		g.context.setTextBaseline("middle");
		if (vertical) {
		    // Klemmenleisten-Beschriftung: senkrecht, weg vom Post
		    int dir = (uy < 0) ? -1 : 1;
		    int anchor = (dir < 0) ? by - 6 : by + boxH + 6;
		    g.context.save();
		    g.context.translate(point2.x, anchor + dir * wFkt / 2);
		    g.context.rotate(-Math.PI / 2);
		    g.drawString(fkt, 0, 0);
		    g.context.restore();
		    if (dir < 0)
			fy1 = anchor - wFkt - 4;
		    else
			fy2 = anchor + wFkt + 4;
		} else {
		    int dir = (ux < 0) ? -1 : 1;
		    int tx2 = (dir < 0) ? bx - 6 - wFkt / 2
					: bx + boxW + 6 + wFkt / 2;
		    g.drawString(fkt, tx2, point2.y);
		    if (dir < 0)
			fx1 = bx - 10 - wFkt;
		    else
			fx2 = bx + boxW + 10 + wFkt;
		}
		g.context.setTextAlign("left");
		g.context.setTextBaseline("alphabetic");
	    }

	    setBbox(Math.min(point1.x, fx1) - 2, Math.min(point1.y, fy1) - 2,
		    Math.max(point1.x, fx2) + 2, Math.max(point1.y, fy2) + 2);

	    g.setLineWidth(1.0);
	    drawPosts(g);
	    curcount = updateDotCount(-current, curcount);
	    if (!isCreating())
		drawDots(g, point1, lead1, curcount);
	} finally {
	    g.restore();
	}
    }

    String getElmType() { return Locale.LS("EGT-HMI-Geber"); }

    /** Kein AC-Block aus VoltageElm — die HMI setzt einen konstanten Pegel. */
    void getInfo(String arr[]) {
	String fkt = (funktion == null || funktion.length() == 0)
		? "" : " — " + funktion;
	arr[0] = Locale.LS("EGT-HMI-Geber") + " " + name + fkt;
	arr[1] = Locale.LS("Signal") + " = " + (signalHigh() ? "1" : "0");
	arr[2] = "V = " + getVoltageText(getVoltage());
	arr[3] = "I = " + getCurrentText(getCurrent());
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) {
	    EditInfo ei = new EditInfo(Locale.LS("Klemme (HMI-Name)"), 0, -1, -1);
	    ei.text = name;
	    return ei;
	}
	if (n == 1) {
	    EditInfo ei = new EditInfo(Locale.LS("Klartext"), 0, -1, -1);
	    ei.text = funktion == null ? "" : funktion;
	    return ei;
	}
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0)
	    name = ei.textf.getText();
	if (n == 1)
	    funktion = ei.textf.getText();
    }
}
