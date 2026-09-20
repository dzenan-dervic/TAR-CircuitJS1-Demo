package com.lushprojects.circuitjs1.client;

import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Additive JS-Bridge for the HTML contact-workshop. CircuitJS remains the only
 * electrical solver. Layout coordinates never become nodes.
 */
class EGTWorkbenchBridge {
    static final String VERSION = "1";
    static final int SCHEMA = 1;
    static final int ORIGIN_X = 48;
    static final int ORIGIN_Y = 80;
    static final int STEP_X = 240;
    static final int STEP_Y = 176;
    static final int COLS = 5;
    static final double COIL_V = 24;
    static final double COIL_P = 4;
    static final double MOTOR_V = 24;
    static final double MOTOR_P = 50;

    static class Term {
	CircuitElm elm;
	int post;
	Term(CircuitElm e, int p) {
	    elm = e;
	    post = p;
	}
    }

    CirSim app;
    int loadedRevision;
    int placeCol, placeRow;
    HashMap<String, Term> terminals;
    HashMap<String, EGTTasterElm> tasters;
    HashMap<String, EGTTasterElm> tastersNc;
    HashMap<String, EGTTasterElm[]> selectors;
    HashMap<String, EGTLeuchteElm> lamps;
    HashMap<String, EGTLeistungsschuetzElm> contactors;
    HashMap<String, EGTSchuetzHilfskontaktElm> auxContacts;
    HashMap<String, EGTGleichstrommotorElm> motors;
    HashMap<String, EGTWechselschalterElm> driveSensors;
    HashMap<String, EGTGleichspannungsquelleElm> psus;
    HashMap<String, Double> psuSetpoint;
    HashMap<String, WireElm> wireElms;
    boolean supplyOn = true;

    EGTWorkbenchBridge(CirSim app) {
	this.app = app;
	resetMaps();
    }

    String version() { return VERSION; }

    void resetMaps() {
	terminals = new HashMap<String, Term>();
	tasters = new HashMap<String, EGTTasterElm>();
	tastersNc = new HashMap<String, EGTTasterElm>();
	selectors = new HashMap<String, EGTTasterElm[]>();
	lamps = new HashMap<String, EGTLeuchteElm>();
	contactors = new HashMap<String, EGTLeistungsschuetzElm>();
	auxContacts = new HashMap<String, EGTSchuetzHilfskontaktElm>();
	motors = new HashMap<String, EGTGleichstrommotorElm>();
	driveSensors = new HashMap<String, EGTWechselschalterElm>();
	psus = new HashMap<String, EGTGleichspannungsquelleElm>();
	psuSetpoint = new HashMap<String, Double>();
	wireElms = new HashMap<String, WireElm>();
	placeCol = 0;
	placeRow = 0;
    }

    String loadProject(JavaScriptObject project) {
	if (project == null)
	    return fail("Kein Projekt");
	int schema = (int) getNum(project, "schema", 0);
	if (schema != SCHEMA)
	    return fail("Unbekanntes Schema " + schema);
	JavaScriptObject components = getObj(project, "components");
	JavaScriptObject wires = getObj(project, "wires");
	if (components == null)
	    return fail("components fehlt");
	if (wires == null)
	    return fail("wires fehlt");

	String check = validate(project, components, wires);
	if (check != null)
	    return fail(check);

	boolean wantRun = getBool(project, "running", false);
	boolean wantSupply = getBool(project, "supplyOn", true);
	int revision = (int) getNum(project, "revision", 0);
	String previous = app.dumpCircuit();
	boolean wasRunning = app.simIsRunning();
	app.setSimRunning(false);
	try {
	    build(components, wires, wantSupply);
	    loadedRevision = revision;
	    supplyOn = wantSupply;
	    CircuitElm.voltageRange = 24;
	    app.ui.setLayoutLocked(true);
	    app.loader.finishReadCircuit(CircuitLoader.RC_NO_CENTER);
	    app.sim.analyzeCircuit();
	    EGTSchuetzLink.refresh(app.sim);
	    enableRealtime();
	    updateVoltageRange();
	    if (wantRun)
		app.setSimRunning(true);
	    return ok();
	} catch (Exception e) {
	    try {
		if (previous != null)
		    app.loader.readCircuit(previous, CircuitLoader.RC_NO_CENTER);
	    } catch (Exception ignored) {}
	    app.setSimRunning(wasRunning);
	    return fail(e.getMessage() == null ? "Import fehlgeschlagen" : e.getMessage());
	}
    }

    boolean setPressed(String id, boolean pressed) {
	if (id == null)
	    return false;
	EGTTasterElm no = tasters.get(id);
	EGTTasterElm nc = tastersNc.get(id);
	if (no == null && nc == null)
	    return false;
	if (no != null)
	    no.setWorkbenchPressed(pressed);
	if (nc != null)
	    nc.setWorkbenchPressed(pressed);
	app.needAnalyze();
	app.repaint();
	return true;
    }

    void setRunning(boolean running) {
	app.setSimRunning(running);
    }

    boolean setSelector(String id, int position) {
        EGTTasterElm[] pair = selectors.get(id);
        if (pair == null || position < -1 || position > 1) return false;
        pair[0].setWorkbenchPressed(false);
        pair[1].setWorkbenchPressed(false);
        if (position != 0) pair[position < 0 ? 0 : 1].setWorkbenchPressed(true);
        app.needAnalyze();
        app.repaint();
        return true;
    }

    boolean setDrivePosition(String id, double position) {
        if (!driveSensors.containsKey(id + ".B1")) return false;
        boolean[] active = { position <= .03, Math.abs(position - .5) <= .035, position >= .97 };
        boolean changed = false;
        for (int i = 0; i < 3; i++) {
            EGTWechselschalterElm sensor = driveSensors.get(id + ".B" + (i + 1));
            int next = active[i] ? 1 : 0;
            if (sensor.position != next) {
                sensor.position = next;
                sensor.setPoints();
                changed = true;
            }
        }
        if (changed) { app.needAnalyze(); app.repaint(); }
        return true;
    }

    String resetSimulation(JavaScriptObject project) {
	if (project == null)
	    return fail("Kein Aufbau zum Zurücksetzen");
	return loadProject(resetProjectCopy(project));
    }

    private native JavaScriptObject resetProjectCopy(JavaScriptObject project) /*-{
	var copy = JSON.parse(JSON.stringify(project));
	copy.running = false;
	return copy;
    }-*/;

    void enableRealtime() {
	app.ui.setEgtRealtimeAvailable(true);
	app.ui.setEgtRealtimeMode(true);
    }

    void setSupplyOn(boolean on) {
	supplyOn = on;
	for (String id : psus.keySet()) {
	    EGTGleichspannungsquelleElm g = psus.get(id);
	    g.voltage = on ? setpointOf(id) : 0;
	}
	updateVoltageRange();
	app.needAnalyze();
	app.repaint();
    }

    boolean setPsuVoltage(String id, double v) {
	if (id == null || !psus.containsKey(id))
	    return false;
	if (v < 1)
	    v = 1;
	psuSetpoint.put(id, Double.valueOf(v));
	if (supplyOn)
	    psus.get(id).voltage = v;
	updateVoltageRange();
	app.needAnalyze();
	app.repaint();
	return true;
    }

    boolean setRatings(String id, double nomV, double nomP) {
	if (id == null)
	    return false;
	if (nomV < 1)
	    nomV = 1;
	if (nomP <= 0)
	    nomP = 1;
	EGTLeistungsschuetzElm k = contactors.get(id);
	if (k != null) {
	    k.nom_v = nomV;
	    k.nom_pow = nomP;
	    k.updateResistance();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	EGTGleichstrommotorElm m = motors.get(id);
	if (m != null) {
	    m.nom_v = nomV;
	    m.nom_p = nomP;
	    m.updateResistance();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	EGTLeuchteElm light = lamps.get(id);
	if (light != null) {
	    light.nom_v = nomV;
	    light.nom_pow = nomP;
	    light.updateResistance();
	    app.needAnalyze();
	    app.repaint();
	    return true;
	}
	return false;
    }

    private double setpointOf(String id) {
	Double v = psuSetpoint.get(id);
	return v == null ? 24 : v.doubleValue();
    }

    private void updateVoltageRange() {
	double max = 24;
	for (String id : psuSetpoint.keySet()) {
	    double v = setpointOf(id);
	    if (v > max)
		max = v;
	}
	CircuitElm.voltageRange = supplyOn ? max : 24;
    }

    private double supplyCurrent(EGTGleichspannungsquelleElm g) {
	if (g == null || g.pins == null || g.pins.length == 0)
	    return 0;
	return g.pins[0].current;
    }

    String readSnapshot() {
	StringBuffer sb = new StringBuffer();
	sb.append("{\"ok\":true,\"version\":\"").append(VERSION);
	sb.append("\",\"revision\":").append(loadedRevision);
	sb.append(",\"time\":").append(app.sim.t);
	sb.append(",\"running\":").append(app.simIsRunning());
	sb.append(",\"currentMult\":").append(jsonNum(CircuitElm.currentMult));
	sb.append(",\"supplyOn\":").append(supplyOn);
	sb.append(",\"fault\":");
	if (app.stopMessage == null) {
	    sb.append("null");
	} else {
	    sb.append("{\"code\":\"SIMULATION_STOPPED\",\"message\":\"");
	    sb.append(esc(app.stopMessage)).append("\",\"componentIds\":[");
	    HashMap<String, Boolean> reported = new HashMap<String, Boolean>();
	    boolean firstFault = true;
	    for (String key : terminals.keySet()) {
		if (terminals.get(key).elm != app.stopElm)
		    continue;
		String id = key.substring(0, key.indexOf('.'));
		if (reported.containsKey(id))
		    continue;
		reported.put(id, Boolean.TRUE);
		if (!firstFault)
		    sb.append(',');
		firstFault = false;
		sb.append('"').append(esc(id)).append('"');
	    }
	    sb.append("]}");
	}
	sb.append(",\"coils\":{");
	boolean first = true;
	for (String id : contactors.keySet()) {
	    EGTLeistungsschuetzElm k = contactors.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"energized\":").append(k.coilEnergized());
	    sb.append(",\"a1\":").append(jsonNum(k.getVoltageJS(EGTLeistungsschuetzElm.N_A1)));
	    sb.append(",\"a2\":").append(jsonNum(k.getVoltageJS(EGTLeistungsschuetzElm.N_A2)));
	    sb.append(",\"current\":").append(jsonNum(k.getCurrent()));
	    sb.append('}');
	}
	sb.append("},\"contacts\":{");
	first = true;
	for (String id : contactors.keySet()) {
	    EGTLeistungsschuetzElm k = contactors.get(id);
	    boolean on = k.coilEnergized();
	    first = contactPair(sb, first, id + ".1", on);
	    first = contactPair(sb, first, id + ".3", on);
	    first = contactPair(sb, first, id + ".5", on);
	    first = contactPair(sb, first, id + ".13", on);
	}
	for (String key : auxContacts.keySet()) {
	    EGTSchuetzHilfskontaktElm c = auxContacts.get(key);
	    first = contactPair(sb, first, key, c.contactClosed());
	}
        for (String id : selectors.keySet()) {
            EGTTasterElm[] pair = selectors.get(id);
            first = contactPair(sb, first, id + ".13", pair[0].position == 0);
            first = contactPair(sb, first, id + ".23", pair[1].position == 0);
        }
        for (String key : driveSensors.keySet()) {
            boolean active = driveSensors.get(key).position == 1;
            first = contactPair(sb, first, key + "_12", !active);
            first = contactPair(sb, first, key + "_14", active);
        }
	sb.append("},\"terminals\":{");
	first = true;
	for (String key : terminals.keySet()) {
	    Term t = terminals.get(key);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(key)).append("\":");
	    sb.append(jsonNum(t.elm.getVoltageJS(t.post)));
	}
	sb.append("},\"wireCurrents\":{");
	first = true;
	for (String key : wireElms.keySet()) {
	    WireElm w = wireElms.get(key);
	    if (w == null)
		continue;
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(key)).append("\":").append(jsonNum(w.getCurrent()));
	}
	sb.append("},\"motors\":{");
	first = true;
	for (String id : motors.keySet()) {
	    EGTGleichstrommotorElm m = motors.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    double i = m.getCurrent();
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"current\":").append(jsonNum(i));
	    sb.append(",\"running\":").append(Math.abs(i) >= EGTGleichstrommotorElm.I_RUN);
	    sb.append(",\"reverse\":").append(i < -EGTGleichstrommotorElm.I_RUN);
	    sb.append('}');
	}
	sb.append("},\"supplies\":{");
	first = true;
	for (String id : psus.keySet()) {
	    EGTGleichspannungsquelleElm g = psus.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(id)).append("\":{");
	    sb.append("\"set\":").append(jsonNum(setpointOf(id)));
	    sb.append(",\"voltage\":").append(jsonNum(g.voltage));
	    sb.append(",\"current\":").append(jsonNum(supplyCurrent(g)));
	    sb.append('}');
	}
	sb.append("},\"lamps\":{");
	first = true;
	for (String id : lamps.keySet()) {
	    EGTLeuchteElm p = lamps.get(id);
	    if (!first)
		sb.append(',');
	    first = false;
	    sb.append('"').append(esc(id)).append("\":{\"on\":").append(p.isLit()).append('}');
	}
	sb.append("}}");
	return sb.toString();
    }

    private boolean contactPair(StringBuffer sb, boolean first, String key, boolean closed) {
	if (!first)
	    sb.append(',');
	sb.append('"').append(esc(key)).append("\":").append(closed);
	return false;
    }

    private String validate(JavaScriptObject project, JavaScriptObject components,
			    JavaScriptObject wires) {
	HashMap<String, String> types = new HashMap<String, String>();
	HashMap<String, String> parents = new HashMap<String, String>();
	int n = getLen(components);
	for (int i = 0; i < n; i++) {
	    JavaScriptObject c = getAt(components, i);
	    if (c == null)
		return "Leeres Bauteil";
	    String id = getStr(c, "id", "");
	    String type = getStr(c, "type", "");
	    if (id.length() == 0)
		return "Bauteil ohne id";
	    if (types.containsKey(id))
		return "Doppelte id " + id;
	    if (!validType(type))
		return "Unbekannter Typ " + type;
	    types.put(id, type);
	    if ("auxiliary".equals(type)) {
		String parent = getStr(c, "parentId", getStr(c, "parent", ""));
		if (parent.length() == 0)
		    return id + " ohne parentId";
		parents.put(id, parent);
	    }
	}
	for (String auxId : parents.keySet()) {
	    String parent = parents.get(auxId);
	    if (!"contactor".equals(types.get(parent)))
		return auxId + " braucht ein Leistungsschütz";
	}
	HashMap<String, Boolean> seenWire = new HashMap<String, Boolean>();
	int nw = getLen(wires);
	for (int i = 0; i < nw; i++) {
	    JavaScriptObject w = getAt(wires, i);
	    if (w == null)
		return "Leere Leitung";
	    String a = normalizeTerm(getStr(w, "a", ""));
	    String b = normalizeTerm(getStr(w, "b", ""));
	    if (a.length() == 0 || b.length() == 0)
		return "Leitung ohne Klemmen";
	    if (a.equals(b))
		return "Leitung auf derselben Klemme " + a;
	    String ta = termOwner(a);
	    String tb = termOwner(b);
	    if (!types.containsKey(ta))
		return "Unbekannte Klemme " + a;
	    if (!types.containsKey(tb))
		return "Unbekannte Klemme " + b;
	    String key = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
	    if (seenWire.containsKey(key))
		return "Doppelte Leitung " + a + "–" + b;
	    seenWire.put(key, Boolean.TRUE);
	}
	return null;
    }

    private void build(JavaScriptObject components, JavaScriptObject wires, boolean wantSupply) {
	resetMaps();
	app.loader.clearCircuit();
	int n = getLen(components);
	for (int i = 0; i < n; i++)
	    addComponent(getAt(components, i), wantSupply);
	for (String key : terminals.keySet()) {
	    Term t = terminals.get(key);
	    t.elm.setPoints();
	}
	int nw = getLen(wires);
	for (int i = 0; i < nw; i++) {
	    JavaScriptObject w = getAt(wires, i);
	    addWire(normalizeTerm(getStr(w, "a", "")), normalizeTerm(getStr(w, "b", "")));
	}
    }

    private void addComponent(JavaScriptObject c, boolean wantSupply) {
	String id = getStr(c, "id", "");
	String type = getStr(c, "type", "");
	String designation = getStr(c, "designation", id);
	if ("contactor".equals(type)) {
	    EGTLeistungsschuetzElm k = new EGTLeistungsschuetzElm(nextX(), nextY());
	    k.setEgtDesignation(designation.length() == 0 ? id : designation);
	    k.nom_v = getNum(c, "nomV", COIL_V);
	    k.nom_pow = getNum(c, "nomP", COIL_P);
	    k.updateResistance();
	    k.syncEndpoints();
	    addElm(k);
	    contactors.put(id, k);
	    bind(id + ".A1", k, EGTLeistungsschuetzElm.N_A1);
	    bind(id + ".A2", k, EGTLeistungsschuetzElm.N_A2);
	    bind(id + ".1", k, EGTLeistungsschuetzElm.N_1);
	    bind(id + ".2", k, EGTLeistungsschuetzElm.N_2);
	    bind(id + ".3", k, EGTLeistungsschuetzElm.N_3);
	    bind(id + ".4", k, EGTLeistungsschuetzElm.N_4);
	    bind(id + ".5", k, EGTLeistungsschuetzElm.N_5);
	    bind(id + ".6", k, EGTLeistungsschuetzElm.N_6);
	    bind(id + ".13", k, EGTLeistungsschuetzElm.N_13);
	    bind(id + ".14", k, EGTLeistungsschuetzElm.N_14);
	    bump();
	    return;
	}
	if ("auxiliary".equals(type)) {
	    String parent = getStr(c, "parentId", getStr(c, "parent", designation));
	    addAux(id, parent, 5, false);
	    addAux(id, parent, 6, true);
	    addAux(id, parent, 7, true);
	    addAux(id, parent, 8, false);
	    return;
	}
	if ("psu".equals(type)) {
	    EGTGleichspannungsquelleElm g = new EGTGleichspannungsquelleElm(nextX(), nextY());
            g.setEgtDesignation(designation.length() == 0 ? id : designation);
	    double set = getNum(c, "voltage", 24);
	    if (set < 1)
		set = 1;
	    g.voltage = wantSupply ? set : 0;
	    g.setPoints();
	    addElm(g);
	    psus.put(id, g);
	    psuSetpoint.put(id, Double.valueOf(set));
	    bind(id + ".+24", g, 0);
	    bind(id + ".0V", g, 1);
	    bump();
	    return;
	}
        if ("selector".equals(type)) {
            EGTTasterElm[] pair = new EGTTasterElm[2];
            for (int i = 0; i < 2; i++) {
                pair[i] = new EGTTasterElm(nextX(), nextY());
                pair[i].setEgtDesignation(designation + (i == 0 ? " A" : " B"));
                addElm(pair[i]);
                bind(id + (i == 0 ? ".13" : ".23"), pair[i], 0);
                bind(id + (i == 0 ? ".14" : ".24"), pair[i], 1);
                bump();
            }
            selectors.put(id, pair);
            setSelector(id, (int)getNum(c, "selection", 0));
            return;
        }
	if ("start".equals(type) || "stop".equals(type) || "emergency".equals(type)) {
	    addTasterStation(id, designation.length() == 0 ? id : designation, !"emergency".equals(type));
	    return;
	}
        if ("lamp".equals(type) || "pilot".equals(type)) {
            boolean pilot = "pilot".equals(type);
            EGTLeuchteElm light = pilot ? new EGTMeldeleuchteElm(nextX(), nextY()) : new EGTLeuchteElm(nextX(), nextY());
            light.setEgtDesignation(designation);
            light.nom_v = getNum(c, "nomV", 24);
            light.nom_pow = getNum(c, "nomP", pilot ? 1.5 : 10);
            light.updateResistance();
            addElm(light);
            lamps.put(id, light);
            bind(id + (pilot ? ".X1" : ".L"), light, 0);
            bind(id + (pilot ? ".X2" : ".N"), light, light.lampReturnPin());
            if (!pilot) bind(id + ".PE", light, 1);
            bump();
            return;
        }
	if ("motor".equals(type) || "lineardrive".equals(type)) {
	    EGTGleichstrommotorElm m = new EGTGleichstrommotorElm(nextX(), nextY());
	    m.setEgtDesignation(designation.length() == 0 ? id : designation);
	    m.nom_v = getNum(c, "nomV", MOTOR_V);
	    m.nom_p = getNum(c, "nomP", MOTOR_P);
	    m.updateResistance();
	    m.syncEndpoints();
	    addElm(m);
	    motors.put(id, m);
	    bind(id + ".+", m, 0);
	    bind(id + ".−", m, 1);
	    bind(id + ".-", m, 1);
	    bump();
            if ("lineardrive".equals(type)) {
                for (int i = 1; i <= 3; i++) {
                    String sensorId = id + ".B" + i;
                    EGTWechselschalterElm sensor = new EGTWechselschalterElm(nextX(), nextY());
                    sensor.setEgtDesignation(designation + " B" + i);
                    addElm(sensor);
                    driveSensors.put(sensorId, sensor);
                    bind(sensorId + "_11", sensor, 2);
                    bind(sensorId + "_12", sensor, 0);
                    bind(sensorId + "_14", sensor, 1);
                    bump();
                }
                // Unidentified sensor sockets remain independent passive test points.
                String[] spare = { "PE", "24V", "0V", "B4", "B5", "B6", "B7" };
                for (String pin : spare) {
                    OutputElm socket = new OutputElm(nextX(), nextY());
                    socket.x2 = socket.x + 32;
                    addElm(socket);
                    bind(id + "." + pin, socket, 0);
                    bump();
                }
                setDrivePosition(id, getNum(c, "slide", .5));
            }
	}
    }

    private void addTasterStation(String id, String designation, boolean withLamp) {
	EGTTasterElm no = new EGTTasterElm(nextX(), nextY());
	no.setEgtDesignation(designation);
	no.setPoints();
	addElm(no);
	tasters.put(id, no);
	bind(id + ".13", no, 0);
	bind(id + ".14", no, 1);
	bump();

	EGTTasterElm nc = new EGTTasterElm(nextX(), nextY());
	nc.setEgtDesignation(designation);
	nc.flags |= EGTStyle.FLAG_TASTER_NC;
	nc.setPoints();
	addElm(nc);
	tastersNc.put(id, nc);
	bind(id + ".21", nc, 0);
	bind(id + ".22", nc, 1);
	bump();
	if (!withLamp) return;

	EGTMeldeleuchteElm p = new EGTMeldeleuchteElm(nextX(), nextY());
	p.setEgtDesignation(lampName(id));
	p.nom_v = 24;
	p.nom_pow = 1.5;
	p.updateResistance();
	p.setPoints();
	addElm(p);
	lamps.put(id, p);
	bind(id + ".X1", p, 0);
	bind(id + ".X2", p, 1);
	bump();
    }

    private String lampName(String id) {
	StringBuffer d = new StringBuffer();
	for (int i = 0; i < id.length(); i++) {
	    char c = id.charAt(i);
	    if (c >= '0' && c <= '9')
		d.append(c);
	}
	return d.length() == 0 ? "P1" : "P" + d.toString();
    }

    private void addAux(String htmlId, String designation, int contactNo, boolean nc) {
	EGTSchuetzHilfskontaktElm c = new EGTSchuetzHilfskontaktElm(nextX(), nextY());
	c.setEgtDesignation(designation);
	c.contactNo = contactNo;
	if (nc)
	    c.flags |= EGTSchuetzHilfskontaktElm.FLAG_NC;
	else
	    c.flags &= ~EGTSchuetzHilfskontaktElm.FLAG_NC;
	c.clampContactNo();
	c.syncEndpoints();
	addElm(c);
	String a = htmlId + "." + c.labTop();
	String b = htmlId + "." + c.labBot();
	bind(a, c, 0);
	bind(b, c, 1);
	auxContacts.put(a, c);
	bump();
    }

    private void addWire(String a, String b) {
	Term ta = terminals.get(a);
	Term tb = terminals.get(b);
	if (ta == null || tb == null)
	    throw new RuntimeException("Klemme fehlt: " + a + " / " + b);
	ta.elm.setPoints();
	tb.elm.setPoints();
	Point pa = ta.elm.getPost(ta.post);
	Point pb = tb.elm.getPost(tb.post);
	if (pa == null || pb == null)
	    throw new RuntimeException("Post fehlt: " + a + " / " + b);
	if (pa.x == pb.x && pa.y == pb.y)
	    return;
	WireElm w = new WireElm(pa.x, pa.y, pb.x, pb.y, 0, new StringTokenizer(""));
	w.setPoints();
	addElm(w);
	wireElms.put(a + "|" + b, w);
    }

    private void addElm(CircuitElm elm) {
	elm.setPoints();
	app.elmList.addElement(elm);
    }

    private void bind(String key, CircuitElm elm, int post) {
	terminals.put(key, new Term(elm, post));
    }

    private int nextX() { return ORIGIN_X + placeCol * STEP_X; }
    private int nextY() { return ORIGIN_Y + placeRow * STEP_Y; }

    private void bump() {
	placeCol++;
	if (placeCol >= COLS) {
	    placeCol = 0;
	    placeRow++;
	}
    }

    private static boolean validType(String type) {
	return "contactor".equals(type) || "auxiliary".equals(type)
		|| "psu".equals(type) || "start".equals(type)
		|| "stop".equals(type) || "motor".equals(type) || "lineardrive".equals(type)
                || "lamp".equals(type) || "pilot".equals(type) || "emergency".equals(type) || "selector".equals(type);
    }

    private static String termOwner(String key) {
	int d = key.indexOf('.');
	return d < 0 ? key : key.substring(0, d);
    }

    private static String normalizeTerm(String key) {
	if (key == null)
	    return "";
	if (key.endsWith(".-"))
	    return key.substring(0, key.length() - 2) + ".−";
	return key;
    }

    private String ok() {
	return "{\"ok\":true,\"version\":\"" + VERSION + "\",\"revision\":"
		+ loadedRevision + "}";
    }

    private String fail(String error) {
	return "{\"ok\":false,\"error\":\"" + esc(error) + "\"}";
    }

    private static String esc(String s) {
	if (s == null)
	    return "";
	return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String jsonNum(double v) {
	if (Double.isNaN(v) || Double.isInfinite(v))
	    return "null";
	return String.valueOf(v);
    }

    private native JavaScriptObject getObj(JavaScriptObject o, String k) /*-{
	var v = o[k];
	return (v && typeof v === 'object') ? v : null;
    }-*/;

    private native String getStr(JavaScriptObject o, String k, String d) /*-{
	var v = o[k];
	if (v == null || v === undefined)
	    return d;
	return String(v);
    }-*/;

    private native double getNum(JavaScriptObject o, String k, double d) /*-{
	var v = o[k];
	if (typeof v === 'number' && isFinite(v))
	    return v;
	if (typeof v === 'string' && v !== '' && isFinite(+v))
	    return +v;
	return d;
    }-*/;

    private native boolean getBool(JavaScriptObject o, String k, boolean d) /*-{
	var v = o[k];
	if (typeof v === 'boolean')
	    return v;
	return d;
    }-*/;

    private native int getLen(JavaScriptObject a) /*-{
	return a && a.length ? a.length : 0;
    }-*/;

    private native JavaScriptObject getAt(JavaScriptObject a, int i) /*-{
	return a[i];
    }-*/;
}
