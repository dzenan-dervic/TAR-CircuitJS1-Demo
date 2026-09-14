/*
    TAR-Dervic EGT: Kondensatormotor (MOD-MOT-03 / M505), Dump 479.
    Didaktisches Einphasen-Asynchronmotor-Modell mit externer C_B-Verdrahtung.
*/

package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Einphasen-Kondensatormotor mit frei verdrahteten Wicklungsenden:
 * U1/U2 = Hauptwicklung, Z1/Z2 = Hilfswicklung. Der Betriebskondensator
 * bleibt ein normales, sichtbares CapacitorElm-Element in der Schaltung.
 *
 * Die Wicklungsströme werden als getrennte Serien-RL-Zweige integriert. Das Drehmoment folgt dem
 * zeitlichen Phasenversatz der beiden Wicklungsströme. Dies ist ein stabiles
 * Unterrichtsmodell, keine vollständige Maschinenkennlinie.
 */
class EGTKondensatormotorElm extends ChipElm implements EGTDesignatable {
    static final int SIZE_X = 7;
    static final int SIZE_Y = 7;
    static final double DEF_UN = 230;
    static final double DEF_PN = 250;
    static final double DEF_RU = 35;
    static final double DEF_RZ = 55;
    static final double DEF_LU = .18;
    static final double DEF_LZ = .24;
    static final double DEF_J = .12;
    static final double DEF_B = .65;
    static final double CURRENT_MIN = .035;
    static final double CURRENT_ENVELOPE_HZ = 30;
    static final double TORQUE_GAIN = .12;
    static final double TORQUE_FILTER_HZ = 45;
    static final double NOMINAL_FREQUENCY = 50;
    static final double SPEED_LIMIT = 95;
    static final double ARROW_RATE = .012;
    static final Color COL_ACCENT = new Color(0x4a, 0x9e, 0xff);
    static final Color COL_IDLE = new Color(0x66, 0x66, 0x66);

    String designation = "M1";
    String note = "";
    double nominalVoltage = DEF_UN;
    double nominalPower = DEF_PN;
    double rMain = DEF_RU;
    double rAux = DEF_RZ;
    double lMain = DEF_LU;
    double lAux = DEF_LZ;
    double inertia = DEF_J;
    double damping = DEF_B;

    double mainCurrent;
    double auxCurrent;
    double mainCurrentEnvelope;
    double auxCurrentEnvelope;
    double previousMainCurrent;
    double previousAuxCurrent;
    double torque;
    double speed;
    double angle = Math.PI / 2;

    public String egtDesignation() { return designation; }
    public void setEgtDesignation(String d) {
        if (d != null)
            designation = d;
    }
    public String egtNote() { return note; }
    public void setEgtNote(String s) { note = EGTNote.clamp(s); }

    public EGTKondensatormotorElm(int xx, int yy) {
        super(xx, yy);
        initDefaults();
        setSize(1);
        applySize();
        layoutPins();
        setPoints();
    }

    public EGTKondensatormotorElm(int xa, int ya, int xb, int yb, int f,
                                  StringTokenizer st) {
        super(xa, ya, xb, yb, f, st);
        initDefaults();
        readDump(st);
        setSize(1);
        applySize();
        layoutPins();
        allocNodes();
        setPoints();
    }

    void initDefaults() {
        // Parameter defaults are initialized with the field declarations.
    }

    void readDump(StringTokenizer st) {
        try {
            if (st.hasMoreTokens())
                Integer.parseInt(st.nextToken()); // sx, legacy-compatible
            if (st.hasMoreTokens())
                Integer.parseInt(st.nextToken()); // sy
            if (st.hasMoreTokens())
                designation = CustomLogicModel.unescape(st.nextToken());
            if (st.hasMoreTokens())
                nominalVoltage = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                nominalPower = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                rMain = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                rAux = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                lMain = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                lAux = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                inertia = Double.parseDouble(st.nextToken());
            if (st.hasMoreTokens())
                damping = Double.parseDouble(st.nextToken());
            note = EGTNote.readOptional(st);
        } catch (Exception e) {
            // Missing optional values keep the didactic defaults.
        }
        sanitizeParameters();
    }

    void sanitizeParameters() {
        if (nominalVoltage <= 0) nominalVoltage = DEF_UN;
        if (nominalPower <= 0) nominalPower = DEF_PN;
        if (rMain <= 0) rMain = DEF_RU;
        if (rAux <= 0) rAux = DEF_RZ;
        if (lMain <= 0) lMain = DEF_LU;
        if (lAux <= 0) lAux = DEF_LZ;
        if (inertia <= 0) inertia = DEF_J;
        if (damping <= 0) damping = DEF_B;
    }

    void applySize() {
        sizeX = SIZE_X;
        sizeY = SIZE_Y;
    }

    boolean isDigitalChip() { return false; }
    boolean nonLinear() { return false; }
    boolean isWireEquivalent() { return false; }
    boolean isRemovableWire() { return false; }
    String getChipName() { return "EGT-Kondensatormotor"; }
    int getPostCount() { return 4; }
    int getInternalNodeCount() { return 0; }
    int getVoltageSourceCount() { return 0; }

    void setupPins() {
        applySize();
        pins = new Pin[4];
        pins[0] = new Pin(2, SIDE_N, "U1");
        pins[1] = new Pin(2, SIDE_S, "U2");
        pins[2] = new Pin(5, SIDE_N, "Z1");
        pins[3] = new Pin(5, SIDE_S, "Z2");
    }

    void layoutPins() {
        if (pins == null || pins.length != 4) {
            setupPins();
            return;
        }
        applySize();
        pins[0].pos = 2; pins[0].side0 = SIDE_N; pins[0].text = "U1";
        pins[1].pos = 2; pins[1].side0 = SIDE_S; pins[1].text = "U2";
        pins[2].pos = 5; pins[2].side0 = SIDE_N; pins[2].text = "Z1";
        pins[3].pos = 5; pins[3].side0 = SIDE_S; pins[3].text = "Z2";
        for (int i = 0; i != 4; i++)
            pins[i].output = false;
    }

    boolean getConnection(int n1, int n2) {
        return (n1 == 0 && n2 == 1) || (n1 == 1 && n2 == 0)
                || (n1 == 2 && n2 == 3) || (n1 == 3 && n2 == 2);
    }

    void stamp() {
        // The matrix receives the 50-Hz impedance magnitude. The actual
        // series-RL winding currents are integrated in stepFinished().
        sim.stampResistor(nodes[0], nodes[1], windingImpedance(rMain, lMain));
        sim.stampResistor(nodes[2], nodes[3], windingImpedance(rAux, lAux));
    }

    void calculateCurrent() {
        current = mainCurrent + auxCurrent;
        EGTStyle.clearPinCurrents(pins);
        pins[0].current = -mainCurrent;
        pins[1].current = mainCurrent;
        pins[2].current = -auxCurrent;
        pins[3].current = auxCurrent;
    }

    void stepFinished() {
        double dt = sim.timeStep;
        if (dt <= 0)
            return;

        // Separate series-RL differential equations for main and auxiliary
        // winding. The external C_B changes the auxiliary terminal voltage.
        double mainVoltage = volts[0] - volts[1];
        double auxVoltage = volts[2] - volts[3];
        mainCurrent += dt * (mainVoltage - rMain * mainCurrent) / lMain;
        auxCurrent += dt * (auxVoltage - rAux * auxCurrent) / lAux;
        double startCurrentLimit = Math.max(2,
                6 * nominalPower / nominalVoltage);
        mainCurrent = clampCurrent(mainCurrent, startCurrentLimit);
        auxCurrent = clampCurrent(auxCurrent, startCurrentLimit);
        calculateCurrent();

        // The oriented current-area is proportional to the rotating field.
        // Swapping Z1/Z2 reverses this sign without a special direction flag.
        double dMain = (mainCurrent - previousMainCurrent) / dt;
        double dAux = (auxCurrent - previousAuxCurrent) / dt;
        double currentBlend = Math.min(1, dt * CURRENT_ENVELOPE_HZ);
        mainCurrentEnvelope += (Math.abs(mainCurrent) - mainCurrentEnvelope)
                * currentBlend;
        auxCurrentEnvelope += (Math.abs(auxCurrent) - auxCurrentEnvelope)
                * currentBlend;
        // The auxiliary winding is connected through the external run capacitor.
        // With the MOTOR-05 right-rotation wiring it leads the main winding;
        // this orientation is defined as positive (clockwise) rotation here.
        double rawTorque = auxCurrent * dMain - mainCurrent * dAux;
        double blend = Math.min(1, dt * TORQUE_FILTER_HZ);
        torque += (rawTorque - torque) * blend;
        if (!hasBothWindingCurrents())
            torque = 0;

        speed += dt * (TORQUE_GAIN * torque - damping * speed) / inertia;
        if (speed > SPEED_LIMIT) speed = SPEED_LIMIT;
        if (speed < -SPEED_LIMIT) speed = -SPEED_LIMIT;
        if (!hasRotatingField() && Math.abs(speed) < .015)
            speed = 0;
        angle += speed * dt;
        previousMainCurrent = mainCurrent;
        previousAuxCurrent = auxCurrent;
    }

    boolean hasBothWindingCurrents() {
        return mainCurrentEnvelope > CURRENT_MIN && auxCurrentEnvelope > CURRENT_MIN;
    }

    double windingImpedance(double resistance, double inductance) {
        double reactance = 2 * Math.PI * NOMINAL_FREQUENCY * inductance;
        return Math.sqrt(resistance * resistance + reactance * reactance);
    }

    double clampCurrent(double value, double limit) {
        if (value > limit) return limit;
        if (value < -limit) return -limit;
        return value;
    }

    boolean hasRotatingField() {
        return hasBothWindingCurrents() && Math.abs(torque) > .02;
    }

    int directionSign() {
        if (!hasRotatingField() || Math.abs(speed) < .03)
            return 0;
        return speed > 0 ? 1 : -1;
    }

    String statusText() {
        int direction = directionSign();
        if (direction > 0) return Locale.LS("Rechtslauf");
        if (direction < 0) return Locale.LS("Linkslauf");
        return Locale.LS("Stillstand");
    }

    Point motorRimToward(Point p, int cx, int cy, int r) {
        double dx = p.x - cx;
        double dy = p.y - cy;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1)
            return new Point(cx, cy);
        return new Point((int) Math.round(cx + dx * r / length),
                (int) Math.round(cy + dy * r / length));
    }

    void drawChip(Graphics g) {
        g.save();
        try {
            int left = rectPointsX[0], top = rectPointsY[0];
            int right = rectPointsX[2], bottom = rectPointsY[2];
            int cx = (left + right) / 2;
            int cy = (top + bottom) / 2;
            int r = Math.min(right - left, bottom - top) * 34 / 100;
            if (r < 22) r = 22;
            Color stroke = needsHighlight() ? selectColor : whiteColor;

            // All four free motor terminals visibly continue to the housing.
            for (int i = 0; i != 4; i++) {
                Pin pin = pins[i];
                setVoltageColor(g, volts[i]);
                drawThickLine(g, pin.post, pin.stub, 3);
                Point rim = motorRimToward(pin.stub, cx, cy, r);
                drawThickLine(g, pin.stub, rim, 3);
                pin.curcount = updateDotCount(pin.current, pin.curcount);
                drawDots(g, pin.stub, pin.post, pin.curcount);
            }

            // Fachübliches Motorkennzeichen. Im Stillstand bleibt die Fläche
            // bewusst ruhig; der Richtungspfeil erscheint nur bei Drehfeld.
            g.setColor(stroke);
            g.setLineWidth(3);
            g.context.setLineCap("butt");
            g.context.beginPath();
            g.context.arc(cx, cy, r, 0, 2 * Math.PI, false);
            g.context.stroke();
            g.setLineWidth(1);

            int direction = directionSign();
            if (direction != 0) {
                if (app != null && app.simIsRunning())
                    angle += direction * Math.abs(currentMult) * ARROW_RATE;
                drawArcArrow(g, cx, cy, r * .78, angle, direction > 0);
            }

            g.setColor(stroke);
            g.context.setTextAlign("center");
            g.context.setTextBaseline("middle");
            g.setFont(new Font("normal", 0, Math.max(15, r * 48 / 100)));
            g.drawString("M", cx, cy - r * 10 / 100);
            g.setFont(new Font("normal", 0, Math.max(10, r * 29 / 100)));
            g.drawString("1~", cx, cy + r * 29 / 100);
            g.context.setTextAlign("left");
            g.context.setTextBaseline("alphabetic");

            g.setColor(stroke);
            g.setFont(EGTStyle.pinLabelFont());
            for (int i = 0; i != 4; i++)
                EGTStyle.drawPinLabelToward(g, this, pins[i].post.x,
                        pins[i].post.y, cx, pins[i].text, whiteColor);
            drawPosts(g);

            String status = statusText();
            int tx = cx + r + 14;
            int extra = EGTStyle.drawDesigAndNote(g, this, designation, tx, cy - 7);
            int textWidth = (int) Math.max(EGTStyle.measureDesigNote(g, this, designation),
                    g.measureWidth(status));
            g.setColor(direction == 0 ? lightGrayColor : COL_ACCENT);
            g.drawString(status, tx, cy + 12 + extra);
            adjustBbox(left - 8, top - 24, tx + textWidth + 10, bottom + 8 + extra);
        } finally {
            g.restore();
        }
    }

    void drawArcArrow(Graphics g, int cx, int cy, double rr, double mid,
                      boolean clockwise) {
        double span = Math.PI * .55;
        double start = mid - span / 2;
        double end = mid + span / 2;
        g.setColor(COL_ACCENT);
        g.setLineWidth(2.2);
        g.context.beginPath();
        if (clockwise)
            g.context.arc(cx, cy, rr, start, end, false);
        else
            g.context.arc(cx, cy, rr, end, start, true);
        g.context.stroke();
        double tipAngle = clockwise ? end : start;
        double tangent = tipAngle + (clockwise ? Math.PI / 2 : -Math.PI / 2);
        double tx = cx + Math.cos(tipAngle) * rr;
        double ty = cy + Math.sin(tipAngle) * rr;
        double ah = Math.max(7, rr * .26);
        double bx = tx - Math.cos(tangent) * ah;
        double by = ty - Math.sin(tangent) * ah;
        double nx = -Math.sin(tangent) * ah * .45;
        double ny = Math.cos(tangent) * ah * .45;
        g.context.beginPath();
        g.context.moveTo(tx, ty);
        g.context.lineTo(bx + nx, by + ny);
        g.context.lineTo(bx - nx, by - ny);
        g.context.closePath();
        g.context.fill();
        g.setLineWidth(1);
    }

    void reset() {
        super.reset();
        mainCurrent = auxCurrent = previousMainCurrent = previousAuxCurrent = 0;
        mainCurrentEnvelope = auxCurrentEnvelope = 0;
        torque = speed = 0;
        angle = Math.PI / 2;
    }

    String dump() {
        return super.dump() + " " + sizeX + " " + sizeY + " "
                + CustomLogicModel.escape(designation) + " " + nominalVoltage
                + " " + nominalPower + " " + rMain + " " + rAux + " "
                + lMain + " " + lAux + " " + inertia + " " + damping + EGTNote.dumpSuffix(note);
    }

    void dumpXml(com.google.gwt.xml.client.Document doc,
                 com.google.gwt.xml.client.Element elem) {
        super.dumpXml(doc, elem);
        XMLSerializer.dumpAttr(elem, "sx", sizeX);
        XMLSerializer.dumpAttr(elem, "sy", sizeY);
        XMLSerializer.dumpAttr(elem, "des", designation);
        EGTNote.dumpXml(elem, note);
        XMLSerializer.dumpAttr(elem, "un", nominalVoltage);
        XMLSerializer.dumpAttr(elem, "pn", nominalPower);
        XMLSerializer.dumpAttr(elem, "ru", rMain);
        XMLSerializer.dumpAttr(elem, "rz", rAux);
        XMLSerializer.dumpAttr(elem, "lu", lMain);
        XMLSerializer.dumpAttr(elem, "lz", lAux);
        XMLSerializer.dumpAttr(elem, "j", inertia);
        XMLSerializer.dumpAttr(elem, "b", damping);
    }

    void undumpXml(XMLDeserializer xml) {
        super.undumpXml(xml);
        designation = xml.parseStringAttr("des", designation);
        note = EGTNote.undumpXml(xml, note);
        nominalVoltage = xml.parseDoubleAttr("un", nominalVoltage);
        nominalPower = xml.parseDoubleAttr("pn", nominalPower);
        rMain = xml.parseDoubleAttr("ru", rMain);
        rAux = xml.parseDoubleAttr("rz", rAux);
        lMain = xml.parseDoubleAttr("lu", lMain);
        lAux = xml.parseDoubleAttr("lz", lAux);
        inertia = xml.parseDoubleAttr("j", inertia);
        damping = xml.parseDoubleAttr("b", damping);
        sanitizeParameters();
        setSize(1);
        applySize();
        layoutPins();
        allocNodes();
        setPoints();
    }

    int getDumpType() { return 479; }
    String getXmlDumpType() { return "EGTKondensatormotor"; }

    void getInfo(String arr[]) {
        arr[0] = getChipName() + " (" + designation + ")";
        arr[1] = "Un = " + getUnitText(nominalVoltage, "V") + ", Pn = "
                + getUnitText(nominalPower, "W");
        arr[2] = "IU = " + getCurrentText(mainCurrent) + ", IZ = "
                + getCurrentText(auxCurrent);
        arr[3] = Locale.LS("Zustand") + " = " + statusText();
        arr[4] = "U1/U2 Hauptwicklung, Z1/Z2 Hilfswicklung";
        arr[5] = "C_B extern verdrahten";
    }

    public EditInfo getChipEditInfo(int n) {
        if (n == 0) return new EditInfo("Bezeichnung", designation);
        if (n == 1) return new EditInfo("Nennspannung (V)", nominalVoltage, 0, 0).setPositive();
        if (n == 2) return new EditInfo("Nennleistung (W)", nominalPower, 0, 0).setPositive();
        if (n == 3) return new EditInfo("Hauptwicklung R (Ohm)", rMain, 0, 0).setPositive();
        if (n == 4) return new EditInfo("Hilfswicklung R (Ohm)", rAux, 0, 0).setPositive();
        if (n == 5) return new EditInfo("Hauptwicklung L (H)", lMain, 0, 0).setPositive();
        if (n == 6) return new EditInfo("Hilfswicklung L (H)", lAux, 0, 0).setPositive();
        if (n == 7) return new EditInfo("Trägheit J", inertia, 0, 0).setPositive();
        if (n == 8) return new EditInfo("Reibung b", damping, 0, 0).setPositive();
        return null;
    }

    public void setChipEditValue(int n, EditInfo ei) {
        if (n == 0) designation = ei.textf.getText();
        if (n == 1) nominalVoltage = ei.value;
        if (n == 2) nominalPower = ei.value;
        if (n == 3) rMain = ei.value;
        if (n == 4) rAux = ei.value;
        if (n == 5) lMain = ei.value;
        if (n == 6) lAux = ei.value;
        if (n == 7) inertia = ei.value;
        if (n == 8) damping = ei.value;
        sanitizeParameters();
        if (app != null)
            app.needAnalyze();
    }
}
