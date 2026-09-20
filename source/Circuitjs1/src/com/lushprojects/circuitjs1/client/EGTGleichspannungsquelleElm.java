/*
    TAR-Dervic EGT extension: Gleichspannungsquelle +/Masse (ZH), M505 / Q602
    Upstream DCVoltageElm bleibt unverändert.
*/

package com.lushprojects.circuitjs1.client;

/**
 * EGT DC supply: ZH box with + (red) / Masse (blue). Posts on the right.
 * No Übersicht (Workbench: ue = ignore). Masse = 0 V vs ground.
 */
class EGTGleichspannungsquelleElm extends ChipElm implements EGTDesignatable {
    static final String[] PIN_LABELS = { "+", "Masse" };

    double voltage = 24;
    String designation = "G1";
    String note = "";

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
	if (d != null)
	    designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTGleichspannungsquelleElm(int xx, int yy) {
	super(xx, yy);
    }

    public EGTGleichspannungsquelleElm(int xa, int ya, int xb, int yb, int f,
				       StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	if (st.hasMoreTokens())
	    voltage = Double.parseDouble(st.nextToken());
	if (st.hasMoreTokens())
	    designation = CustomLogicModel.unescape(st.nextToken());
    
	note = EGTNote.readOptional(st);
}

    boolean isDigitalChip() { return false; }

    String getChipName() { return "EGT-Gleichspannungsquelle"; }

    void setupPins() {
	sizeX = 2;
	sizeY = 2;
	pins = new Pin[2];
	for (int i = 0; i < 2; i++) {
	    pins[i] = new Pin(i, SIDE_E, "");
	    pins[i].output = true;
	}
    }

    int getPostCount() { return 2; }
    int getVoltageSourceCount() { return 2; }

    void startIteration() {}
    void execute() {}

    void doStep() {
	sim.updateVoltageSource(CircuitNode.ground, nodes[0], pins[0].voltSource,
				voltage);
	sim.updateVoltageSource(CircuitNode.ground, nodes[1], pins[1].voltSource,
				0);
    }

    void drawChip(Graphics g) {
	g.save();
	int left = Math.min(rectPointsX[0], rectPointsX[2]);
	int top = Math.min(rectPointsY[0], rectPointsY[1]);
	int right = Math.max(rectPointsX[0], rectPointsX[2]);

	Color stroke = needsHighlight() ? selectColor : lightGrayColor;
	drawZh(g, left, top, right, stroke);

	for (int i = 0; i < 2; i++) {
	    Pin p = pins[i];
	    Color c = (i == 0) ? EGTStyle.COL_LIVE_PULSE : EGTStyle.COL_N;
	    EGTStyle.drawConductor(g, p.stub.x, p.stub.y, p.post.x, p.post.y,
				   c, 3.5);
	    p.curcount = updateDotCount(p.current, p.curcount);
	    drawDots(g, p.stub, p.post, p.curcount);
	}
	drawPosts(g);
	g.restore();
    }

    void drawZh(Graphics g, int left, int top, int right, Color stroke) {
	g.setColor(stroke);
	drawThickPolygon(g, rectPointsX, rectPointsY, 4);

	Font fLabel = new Font("normal", 0, 9 * csize);
	g.setFont(fLabel);
	g.setColor(whiteColor);
	int titleW = EGTStyle.measureDesigNote(g, this, designation);
	int titleY = EGTNote.of(this).length() > 0 ? top - 24 : top - 10;
	EGTStyle.drawDesigAndNote(g, this, designation,
				 (left + right) / 2 - titleW / 2, titleY);
	adjustBbox(left, titleY - 12, right, top);

	int labelPad = 8;
	for (int i = 0; i < 2; i++) {
	    Pin p = pins[i];
	    int y = p.stub.y;
	    int xStart;
	    Color c;
	    if (i == 0) {
		g.setColor(whiteColor);
		g.setFont(fLabel);
		int tw = (int) g.context.measureText(PIN_LABELS[i]).getWidth();
		g.context.setTextBaseline("middle");
		g.drawString(PIN_LABELS[i], left + labelPad, y);
		g.context.setTextBaseline("alphabetic");
		xStart = left + labelPad + tw + 4;
		c = EGTStyle.COL_LIVE_PULSE;
	    } else {
		c = EGTStyle.COL_N;
		int stem = 4;
		int bar = 5;
		int massX = left + labelPad + bar;
		int barY = y + stem;
		EGTStyle.drawConductor(g, massX, y - stem, massX, barY,
				       whiteColor, 2);
		EGTStyle.drawConductor(g, massX - bar, barY, massX + bar, barY,
				       whiteColor, 2);
		xStart = massX + bar + 5;
	    }
	    EGTStyle.drawConductor(g, xStart, y, p.stub.x, y, c, 3.5);
	}
    }

    String dump() {
	return super.dump() + " " + voltage + " "
		+ CustomLogicModel.escape(designation) + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
		 com.google.gwt.xml.client.Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "v", voltage);
	XMLSerializer.dumpAttr(elem, "des", designation);
	EGTNote.dumpXml(elem, note);
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	voltage = xml.parseDoubleAttr("v", voltage);
	designation = xml.parseStringAttr("des", designation);
	note = EGTNote.undumpXml(xml, note);
    }

    int getDumpType() { return 441; }
    String getXmlDumpType() { return "EGTGleichspannungsquelle"; }

    void getInfo(String arr[]) {
	arr[0] = getChipName() + " (" + designation + ")";
	arr[1] = "U = " + getUnitText(voltage, "V");
	arr[2] = "+ = " + getVoltageText(volts[0]);
	arr[3] = "Masse = " + getVoltageText(volts[1]);
    }

    public EditInfo getChipEditInfo(int n) {
	if (n == 0)
	    return new EditInfo("Spannung (V)", voltage).setPositive();
	if (n == 1)
	    return new EditInfo("Bezeichnung", designation);
	return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n == 0 && ei.value > 0)
	    voltage = ei.value;
	if (n == 1)
	    designation = ei.textf.getText();
    }
}
