/*
    TAR-Dervic EGT extension: farbige Leitung L1/L2/L3/N/PE (I201, I204)
    Upstream WireElm / RoutedWireElm bleiben unverändert.
*/

package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * EGT-Leitung: elektrisch {@link WireElm} ({@code w}), DIN-Farbe.
 * Nicht achsparallel → orthogonaler Pfad (L/Z). Enden bleiben an den Klemmen;
 * Knick ziehen = Pfad durch den Punkt, wie {@code W}-rerouteVia, ohne WireRouter.
 */
class EGTLeitungElm extends WireElm {
    ArrayList<Point> routePoints;
    /** Zwischenpunkte außer den Dump-Enden {@code x,y}/{@code x2,y2}. */
    ArrayList<Point> mids;
    double[] segI;
    double[] segCurCount;
    static final int ROLE_L1 = 0;
    static final int ROLE_L2 = 1;
    static final int ROLE_L3 = 2;
    static final int ROLE_N = 3;
    static final int ROLE_PE = 4;
    static final int ROLE_CUSTOM = 5;

    static final String[] ROLE_NAMES = {
	"L1", "L2", "L3", "N", "PE", "Beliebige Farbe"
    };
    static final String[] ROLE_LABELS = {
	"L1", "L2", "L3", "N", "PE", "L"
    };
    static final String[] ROLE_MENU = {
	"L1  Braun", "L2  Schwarz", "L3  Grau",
	"N  Blau", "PE  Grün-Gelb", "Beliebige Farbe"
    };

    static final int FLAG_SHOW_BEZEICHNUNG = 1 << 16;
    /** L: erst waagerecht, Ecke bei (x2, y1). */
    static final int FLAG_CORNER_H_FIRST = 1 << 17;
    /** L: erst senkrecht, Ecke bei (x1, y2). */
    static final int FLAG_CORNER_V_FIRST = 1 << 18;

    /** Leiter für die nächste gezeichnete EGT-Leitung (Menüwahl, bleibt bis zur nächsten Wahl). */
    static int pendingLeiterArt = ROLE_L1;
    /**
     * Flexibel: Knick ziehen ({@code rerouteVia}). Einfach: klassisches L/Z aus den
     * Endpunkten, ohne Ecke ziehen — Verbindungspunkte unverändert.
     */
    static boolean pendingFlexible = true;

    int leiterArt = ROLE_L1;
    String bezeichnung = "L1";
    Color customColor = new Color("#c0392b");
    double current01, current12, curcount12;
    /** 0-V-Quellen je Schenkel (wie Amperemeter): echter KCL-Strom, kein V/R-Geisterstrom. */
    VoltageSource[] cornerVs;

    static void setPendingLeiterArt(int art) {
	pendingLeiterArt = safeRole(art);
    }

    static void setPendingFlexible(boolean flex) {
	pendingFlexible = flex;
    }

    /** Knick-Ziehen nur im flexiblen Modus. L-Ecke bleibt elektrischer Post. */
    boolean allowsKnickDrag() {
	return pendingFlexible && busWidth == 1;
    }

    static String drawModeLabel() {
	return "EGT-Leitung " + ROLE_NAMES[safeRole(pendingLeiterArt)]
		+ (pendingFlexible ? " Flexibel" : " Einfach");
    }

    public EGTLeitungElm(int xx, int yy) {
	super(xx, yy);
	setLeiterArt(pendingLeiterArt);
    }

    public EGTLeitungElm(int xa, int ya, int xb, int yb, int f,
			 StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	if (st.hasMoreTokens())
	    leiterArt = parseRole(st.nextToken());
	bezeichnung = ROLE_LABELS[safeRole(leiterArt)];
	if (st.hasMoreTokens())
	    bezeichnung = CustomLogicModel.unescape(st.nextToken());
	if (st.hasMoreTokens()) {
	    String hex = st.nextToken();
	    if (hex != null && hex.startsWith("#"))
		customColor = new Color(hex);
	}
	if (st.hasMoreTokens()) {
	    try {
		int nv = Integer.parseInt(st.nextToken());
		if (nv > 0 && nv < 32) {
		    mids = new ArrayList<Point>();
		    for (int i = 0; i < nv && st.hasMoreTokens(); i++) {
			int mx = Integer.parseInt(st.nextToken());
			if (!st.hasMoreTokens())
			    break;
			int my = Integer.parseInt(st.nextToken());
			mids.add(new Point(mx, my));
		    }
		}
	    } catch (Exception e) {}
	}
	setPoints();
    }

    static int safeRole(int art) {
	if (art < ROLE_L1 || art > ROLE_CUSTOM)
	    return ROLE_L1;
	return art;
    }

    static int parseRole(String s) {
	try {
	    int v = Integer.parseInt(s);
	    if (v >= ROLE_L1 && v <= ROLE_CUSTOM)
		return v;
	} catch (Exception e) {}
	if ("L1".equalsIgnoreCase(s))
	    return ROLE_L1;
	if ("L2".equalsIgnoreCase(s))
	    return ROLE_L2;
	if ("L3".equalsIgnoreCase(s))
	    return ROLE_L3;
	if ("N".equalsIgnoreCase(s))
	    return ROLE_N;
	if ("PE".equalsIgnoreCase(s))
	    return ROLE_PE;
	if ("CUSTOM".equalsIgnoreCase(s) || "L_ROT".equalsIgnoreCase(s)
		|| "LROT".equalsIgnoreCase(s))
	    return ROLE_CUSTOM;
	return ROLE_L1;
    }

    boolean mustShowBezeichnung() {
	return (flags & FLAG_SHOW_BEZEICHNUNG) != 0;
    }

    boolean bezeichnungIsDefault() {
	return bezeichnung == null || bezeichnung.length() == 0
		|| bezeichnung.equals(ROLE_LABELS[safeRole(leiterArt)]);
    }

    void setLeiterArt(int art) {
	art = safeRole(art);
	boolean sync = bezeichnungIsDefault();
	leiterArt = art;
	if (sync)
	    bezeichnung = ROLE_LABELS[leiterArt];
    }

    void setPoints() {
	super.setPoints();
	if (routePoints == null)
	    routePoints = new ArrayList<Point>();
	routePoints.clear();
	routePoints.add(point1);
	if (mids != null && mids.size() > 0) {
	    for (int i = 0; i < mids.size(); i++)
		routePoints.add(mids.get(i));
	    routePoints.add(point2);
	    orthogonalizeRoute();
	} else if (point1.x != point2.x && point1.y != point2.y) {
	    boolean hFirst;
	    if ((flags & FLAG_CORNER_H_FIRST) != 0)
		hFirst = true;
	    else if ((flags & FLAG_CORNER_V_FIRST) != 0)
		hFirst = false;
	    else
		hFirst = Math.abs(point2.x - point1.x)
			> Math.abs(point2.y - point1.y);
	    if (hFirst)
		routePoints.add(new Point(point2.x, point1.y));
	    else
		routePoints.add(new Point(point1.x, point2.y));
	    routePoints.add(point2);
	} else
	    routePoints.add(point2);
	if (nodes == null || volts == null
		|| nodes.length != routePoints.size()
		|| volts.length != routePoints.size())
	    allocNodes();
    }

    void orthogonalizeRoute() {
	ArrayList<Point> pts = new ArrayList<Point>();
	pts.add(routePoints.get(0));
	for (int i = 1; i < routePoints.size(); i++) {
	    Point a = pts.get(pts.size() - 1);
	    Point b = routePoints.get(i);
	    if (a.x != b.x && a.y != b.y)
		pts.add(new Point(b.x, a.y));
	    if (a.x != b.x || a.y != b.y)
		pts.add(new Point(b.x, b.y));
	}
	collapseCollinear(pts);
	routePoints = pts;
	syncMidsFromRoute();
    }

    static void collapseCollinear(ArrayList<Point> pts) {
	int i = 1;
	while (i < pts.size() - 1) {
	    Point a = pts.get(i - 1);
	    Point b = pts.get(i);
	    Point c = pts.get(i + 1);
	    boolean same = b.x == a.x && b.y == a.y;
	    boolean col = (a.x == b.x && b.x == c.x)
		    || (a.y == b.y && b.y == c.y);
	    if (same || col)
		pts.remove(i);
	    else
		i++;
	}
    }

    void syncMidsFromRoute() {
	if (routePoints == null || routePoints.size() <= 2) {
	    mids = null;
	    return;
	}
	mids = new ArrayList<Point>();
	for (int i = 1; i < routePoints.size() - 1; i++) {
	    Point p = routePoints.get(i);
	    mids.add(new Point(p.x, p.y));
	}
    }

    void adoptRoute(ArrayList<Point> pts) {
	if (pts == null || pts.size() < 2)
	    return;
	collapseCollinear(pts);
	if (pts.size() < 2)
	    return;
	Point a = pts.get(0);
	Point b = pts.get(pts.size() - 1);
	if (a.x == b.x && a.y == b.y)
	    return;
	x = a.x;
	y = a.y;
	x2 = b.x;
	y2 = b.y;
	if (pts.size() == 3) {
	    Point c = pts.get(1);
	    boolean h = c.x == x2 && c.y == y;
	    boolean v = c.x == x && c.y == y2;
	    if (h || v) {
		mids = null;
		flags &= ~(FLAG_CORNER_H_FIRST | FLAG_CORNER_V_FIRST);
		flags |= h ? FLAG_CORNER_H_FIRST : FLAG_CORNER_V_FIRST;
		initBoundingBox();
		setPoints();
		if (app != null)
		    app.needAnalyze();
		return;
	    }
	}
	mids = new ArrayList<Point>();
	for (int i = 1; i < pts.size() - 1; i++) {
	    Point p = pts.get(i);
	    mids.add(new Point(p.x, p.y));
	}
	initBoundingBox();
	setPoints();
	if (app != null)
	    app.needAnalyze();
    }

    /** Enden fest; Pfad orthogonal durch (vx,vy) — ohne WireRouter. */
    void rerouteVia(int vx, int vy) {
	vx = snapGrid(vx);
	vy = snapGrid(vy);
	ArrayList<Point> pts = new ArrayList<Point>();
	pts.add(new Point(x, y));
	if (vx != x || vy != y) {
	    if (vx != x && vy != y)
		pts.add(new Point(vx, y));
	    pts.add(new Point(vx, vy));
	}
	if (vx != x2 || vy != y2) {
	    if (x2 != vx && y2 != vy)
		pts.add(new Point(x2, vy));
	    pts.add(new Point(x2, y2));
	} else if (pts.get(pts.size() - 1).x != x2
		|| pts.get(pts.size() - 1).y != y2)
	    pts.add(new Point(x2, y2));
	adoptRoute(pts);
    }

    boolean hasCorner() {
	return routePoints != null && routePoints.size() > 2 && busWidth == 1;
    }

    /** Knick: elektrischer Knoten, aber kein sichtbarer Post (Knick ≠ Verbindung). */
    boolean isBendPost(int n) {
	return hasCorner() && n > 0 && n < getPostCount() - 1;
    }

    int countOtherPostsAt(Point p) {
	if (p == null || app == null || app.elmList == null)
	    return 0;
	int n = 0;
	for (int i = 0; i < app.elmList.size(); i++) {
	    CircuitElm ce = app.elmList.get(i);
	    if (ce == this)
		continue;
	    int pc = ce.getPostCount();
	    for (int j = 0; j < pc; j++) {
		Point q = ce.getPost(j);
		if (q != null && q.x == p.x && q.y == p.y)
		    n++;
	    }
	}
	return n;
    }

    int getPostCount() {
	if (hasCorner())
	    return routePoints.size();
	return super.getPostCount();
    }

    int getNumHandles() {
	if (!allowsKnickDrag() || routePoints == null || routePoints.size() < 2)
	    return 2;
	return routePoints.size();
    }

    Point cornerPoint() {
	if (hasCorner())
	    return routePoints.get(1);
	return null;
    }

    void move(int dx, int dy) {
	if (mids != null) {
	    for (int i = 0; i < mids.size(); i++) {
		Point p = mids.get(i);
		p.x += dx;
		p.y += dy;
	    }
	}
	super.move(dx, dy);
    }

    void flipX(int center2, int count) {
	if (mids != null) {
	    for (int i = 0; i < mids.size(); i++)
		mids.get(i).x = center2 - mids.get(i).x;
	}
	super.flipX(center2, count);
    }

    void flipY(int center2, int count) {
	if (mids != null) {
	    for (int i = 0; i < mids.size(); i++)
		mids.get(i).y = center2 - mids.get(i).y;
	}
	super.flipY(center2, count);
    }

    void movePoint(int n, int dx, int dy) {
	int last = (routePoints == null) ? 1 : routePoints.size() - 1;
	if (n <= 0) {
	    super.movePoint(0, dx, dy);
	    return;
	}
	if (n >= last) {
	    super.movePoint(1, dx, dy);
	    return;
	}
	if (!allowsKnickDrag())
	    return;
	Point c = routePoints.get(n);
	rerouteVia(c.x + dx, c.y + dy);
    }

    int getHandleGrabbedClose(int xtest, int ytest, int deltaSq, int minSize) {
	if (!allowsKnickDrag() || routePoints == null || routePoints.size() < 2)
	    return super.getHandleGrabbedClose(xtest, ytest, deltaSq, minSize);
	int grab = deltaSq < 256 ? 256 : deltaSq;
	lastHandleGrabbed = -1;
	int best = grab + 1;
	int bestI = -1;
	for (int i = 0; i < routePoints.size(); i++) {
	    Point p = routePoints.get(i);
	    int d = Graphics.distanceSq(p.x, p.y, xtest, ytest);
	    if (d <= grab && d < best) {
		best = d;
		bestI = i;
	    }
	}
	lastHandleGrabbed = bestI;
	return lastHandleGrabbed;
    }

    void drawHandles(Graphics g, Color c) {
	if (!allowsKnickDrag() || routePoints == null || routePoints.size() < 2) {
	    super.drawHandles(g, c);
	    return;
	}
	g.setColor(c);
	for (int i = 0; i < routePoints.size(); i++) {
	    Point p = routePoints.get(i);
	    int s = lastHandleGrabbed == i ? 4 : 3;
	    g.fillRect(p.x - s, p.y - s, s * 2 + 1, s * 2 + 1);
	}
    }

    Point getPost(int n) {
	if (hasCorner() && n >= 0 && n < routePoints.size())
	    return routePoints.get(n);
	return super.getPost(n);
    }

    Point getConnectedPost(int n) {
	if (!hasCorner())
	    return super.getConnectedPost(n);
	if (n == 0)
	    return routePoints.get(1);
	if (n == 1)
	    return routePoints.get(2);
	return routePoints.get(0);
    }

    boolean isRemovableWire() {
	if (hasCorner())
	    return false;
	return super.isRemovableWire();
    }

    int cornerSegCount() {
	if (busWidth != 1)
	    return 0;
	setPoints();
	if (routePoints == null || routePoints.size() <= 2)
	    return 0;
	return routePoints.size() - 1;
    }

    int getVoltageSourceCount() {
	return cornerSegCount();
    }

    void setVoltageSource(int n, VoltageSource v) {
	int need = cornerSegCount();
	if (need <= 0) {
	    super.setVoltageSource(n, v);
	    return;
	}
	if (cornerVs == null || cornerVs.length != need)
	    cornerVs = new VoltageSource[need];
	if (n >= 0 && n < need)
	    cornerVs[n] = v;
    }

    void stamp() {
	if (!hasCorner()) {
	    super.stamp();
	    return;
	}
	int n = getPostCount();
	if (cornerVs == null || cornerVs.length != n - 1)
	    return;
	for (int i = 0; i < n - 1; i++) {
	    if (cornerVs[i] == null)
		continue;
	    cornerVs[i].setNodes(nodes[i], nodes[i + 1]);
	    sim.stampVoltageSource(nodes[i], nodes[i + 1], cornerVs[i], 0);
	}
    }

    void setCurrent(VoltageSource vs, double c) {
	if (cornerVs == null) {
	    super.setCurrent(vs, c);
	    return;
	}
	int n = cornerVs.length;
	if (segI == null || segI.length != n)
	    segI = new double[n];
	for (int i = 0; i < n; i++) {
	    if (vs != cornerVs[i])
		continue;
	    segI[i] = c;
	    if (i == 0)
		current = current01 = c;
	    if (i == 1)
		current12 = c;
	    return;
	}
	super.setCurrent(vs, c);
    }

    void calculateCurrent() {
	if (!hasCorner())
	    super.calculateCurrent();
    }

    double getCurrentIntoNode(int n) {
	if (!hasCorner())
	    return super.getCurrentIntoNode(n);
	int pc = getPostCount();
	if (segI == null || segI.length != pc - 1)
	    return 0;
	if (n == 0)
	    return -segI[0];
	if (n == pc - 1)
	    return segI[pc - 2];
	if (n > 0 && n < pc - 1)
	    return segI[n - 1] - segI[n];
	return 0;
    }

    Color roleColor() {
	switch (leiterArt) {
	case ROLE_L2:
	    return EGTStyle.COL_L2;
	case ROLE_L3:
	    return EGTStyle.COL_L3;
	case ROLE_N:
	    return EGTStyle.COL_N;
	case ROLE_CUSTOM:
	    return customColor;
	case ROLE_L1:
	default:
	    return EGTStyle.COL_L1;
	}
    }

    void draw(Graphics g) {
	if (routePoints == null || routePoints.size() < 2)
	    setPoints();
	if (currents != null) {
	    current = 0;
	    for (int i = 0; i < currents.length; i++)
		current += currents[i];
	}
	double w = (busWidth > 1) ? 5 : 3;
	double v = (volts != null && volts.length > 0) ? volts[0] : 0;
	boolean pe = leiterArt == ROLE_PE;
	boolean mark = needsHighlight();
	Color base = pe ? EGTStyle.COL_PE_G : roleColor();
	for (int i = 0; i < routePoints.size() - 1; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    if (mark)
		EGTStyle.drawConductor(g, a.x, a.y, b.x, b.y, selectColor, w);
	    else
		EGTStyle.drawLiveConductor(g, a.x, a.y, b.x, b.y, base, v, w, pe);
	}
	doDots(g);
	int m = 5;
	int minX = point1.x, minY = point1.y, maxX = point1.x, maxY = point1.y;
	for (int i = 1; i < routePoints.size(); i++) {
	    Point p = routePoints.get(i);
	    minX = min(minX, p.x);
	    minY = min(minY, p.y);
	    maxX = max(maxX, p.x);
	    maxY = max(maxY, p.y);
	}
	setBbox(minX - m, minY - m, maxX + m, maxY + m);
	String s = "";
	if (mustShowBezeichnung() && bezeichnung != null
		&& bezeichnung.length() > 0)
	    s = bezeichnung;
	if (busWidth == 1) {
	    if (mustShowCurrent())
		s = (s.length() > 0 ? s + " " : "")
			+ getShortUnitText(Math.abs(getCurrent()), "A");
	    if (mustShowVoltage())
		s = (s.length() > 0 ? s + " " : "")
			+ getShortUnitText(volts[0], "V");
	}
	if (s.length() > 0)
	    drawValues(g, s, 4);
	if (hasCorner()) {
	    for (int i = 1; i < routePoints.size() - 1; i++) {
		if (countOtherPostsAt(routePoints.get(i)) >= 1)
		    drawPost(g, routePoints.get(i));
	    }
	}
	drawPosts(g);
    }

    /**
     * Bei L/Z nicht die Sehne (x,y)–(x2,y2) nutzen — das legt den Text auf die
     * Diagonale der Ecke. Gerade Schenkel: wie Upstream; Knick: längstes Segment.
     */
    void drawValues(Graphics g, String s, double hs) {
	if (s == null || s.length() == 0)
	    return;
	if (routePoints != null && routePoints.size() > 2) {
	    drawValuesOnLongestSegment(g, s);
	    return;
	}
	super.drawValues(g, s, hs);
    }

    void drawValuesOnLongestSegment(Graphics g, String s) {
	double bestLen = -1;
	int bestSeg = 0;
	for (int i = 0; i < routePoints.size() - 1; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    double dx = b.x - a.x;
	    double dy = b.y - a.y;
	    double len = dx * dx + dy * dy;
	    if (len > bestLen) {
		bestLen = len;
		bestSeg = i;
	    }
	}
	Point a = routePoints.get(bestSeg);
	Point b = routePoints.get(bestSeg + 1);
	int mx = (a.x + b.x) / 2;
	int my = (a.y + b.y) / 2;
	g.save();
	g.setFont(valueFont);
	int tw = (int) g.context.measureText(s).getWidth();
	int ya = (int) g.currentFontSize / 2;
	g.setColor(whiteColor);
	if (a.y == b.y)
	    g.drawString(s, mx - tw / 2, my - 6);
	else
	    g.drawString(s, mx + 4, my + ya);
	g.restore();
    }

    void drawPosts(Graphics g) {
	if (!isCreating() && !needsHighlight())
	    return;
	if (app.mouse.mouseMode == MouseManager.MODE_DRAG_ROW ||
	    app.mouse.mouseMode == MouseManager.MODE_DRAG_COLUMN)
	    return;
	int i;
	for (i = 0; i != getPostCount(); i++) {
	    if (isBendPost(i) && countOtherPostsAt(getPost(i)) < 1)
		continue;
	    drawPost(g, getPost(i));
	}
	drawScopeTerminalLabels(g);
    }

    void doDots(Graphics g) {
	if (isCreating() || routePoints == null || routePoints.size() < 2)
	    return;
	int nseg = routePoints.size() - 1;
	if (hasCorner() && segI != null && segI.length == nseg) {
	    if (segCurCount == null || segCurCount.length != nseg)
		segCurCount = new double[nseg];
	    for (int i = 0; i < nseg; i++) {
		double ii = segI[i];
		if (Math.abs(ii) < EGTStyle.I_DOT_MIN) {
		    segCurCount[i] = 0;
		    continue;
		}
		segCurCount[i] = updateDotCount(ii, segCurCount[i]);
		if (segCurCount[i] != 0)
		    drawDots(g, routePoints.get(i), routePoints.get(i + 1),
			     segCurCount[i]);
	    }
	    return;
	}
	if (Math.abs(current) < EGTStyle.I_DOT_MIN) {
	    curcount = 0;
	    return;
	}
	updateDotCount();
	if (curcount == 0)
	    return;
	double cc = curcount;
	for (int i = 0; i < nseg; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    drawDots(g, a, b, cc);
	    double ddx = b.x - a.x;
	    double ddy = b.y - a.y;
	    cc = addCurCount(cc, Math.sqrt(ddx * ddx + ddy * ddy));
	}
    }

    boolean pointOnWireInterior(int px, int py) {
	if (routePoints == null || routePoints.size() < 2)
	    return super.pointOnWireInterior(px, py);
	return pointOnWireInteriorForPoints(px, py, routePoints);
    }

    boolean pointOnPath(Point p) {
	if (routePoints == null || routePoints.size() < 2 || p == null)
	    return false;
	if (p.equals(point1) || p.equals(point2))
	    return false;
	if (hasCorner()) {
	    for (int i = 1; i < routePoints.size() - 1; i++) {
		if (p.equals(routePoints.get(i)))
		    return false;
	    }
	}
	return pointOnWireInterior(p.x, p.y);
    }

    int getMouseDistance(int gx, int gy) {
	if (routePoints == null || routePoints.size() < 2)
	    return super.getMouseDistance(gx, gy);
	int thresh = 10;
	int best = Integer.MAX_VALUE;
	for (int i = 0; i < routePoints.size() - 1; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    int d = RoutedWireElm.segmentDistanceSq(a.x, a.y, b.x, b.y, gx, gy);
	    if (d < best)
		best = d;
	}
	if (best <= thresh * thresh)
	    return best;
	return -1;
    }

    void addRoutingObstacle(WireRouter router) {
	if (routePoints == null || routePoints.size() < 2) {
	    super.addRoutingObstacle(router);
	    return;
	}
	for (int i = 0; i < routePoints.size() - 1; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    router.addWire(a.x, a.y, b.x, b.y);
	}
    }

    String dump() {
	String s = super.dump() + " " + leiterArt + " "
		+ CustomLogicModel.escape(bezeichnung) + " "
		+ customColor.getHexValue();
	if (mids != null && mids.size() > 0) {
	    s += " " + mids.size();
	    for (int i = 0; i < mids.size(); i++) {
		Point p = mids.get(i);
		s += " " + p.x + " " + p.y;
	    }
	}
	return s;
    }

    void dumpXml(Document doc, Element elem) {
	super.dumpXml(doc, elem);
	XMLSerializer.dumpAttr(elem, "role", leiterArt);
	XMLSerializer.dumpAttr(elem, "bez", bezeichnung);
	XMLSerializer.dumpAttr(elem, "col", customColor.getHexValue());
	if (mids != null && mids.size() > 0) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < mids.size(); i++) {
		if (i > 0)
		    sb.append(';');
		Point p = mids.get(i);
		sb.append(p.x).append(',').append(p.y);
	    }
	    XMLSerializer.dumpAttr(elem, "mids", sb.toString());
	}
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	leiterArt = safeRole((int) xml.parseDoubleAttr("role", leiterArt));
	bezeichnung = xml.parseStringAttr("bez", ROLE_LABELS[leiterArt]);
	String hex = xml.parseStringAttr("col", customColor.getHexValue());
	if (hex != null && hex.startsWith("#"))
	    customColor = new Color(hex);
	String ms = xml.parseStringAttr("mids", "");
	if (ms != null && ms.length() > 0) {
	    mids = new ArrayList<Point>();
	    String[] pairs = ms.split(";");
	    for (int i = 0; i < pairs.length; i++) {
		String[] xy = pairs[i].split(",");
		if (xy.length >= 2) {
		    try {
			mids.add(new Point(Integer.parseInt(xy[0]),
					   Integer.parseInt(xy[1])));
		    } catch (Exception e) {}
		}
	    }
	    if (mids.size() == 0)
		mids = null;
	}
	setPoints();
    }

    int getDumpType() { return 440; }
    String getXmlDumpType() { return "egtleitung"; }
    int getShortcut() { return 0; }

    void getInfo(String arr[]) {
	super.getInfo(arr);
	arr[0] = "EGT-Leitung (" + ROLE_NAMES[safeRole(leiterArt)] + ")";
	if (bezeichnung != null && bezeichnung.length() > 0
		&& !bezeichnung.equals(ROLE_LABELS[safeRole(leiterArt)]))
	    arr[0] += " \"" + bezeichnung + "\"";
    }

    int extraEditCount() {
	return (leiterArt == ROLE_CUSTOM) ? 4 : 3;
    }

    public EditInfo getEditInfo(int n) {
	if (n == 0) {
	    EditInfo ei = new EditInfo("Leiterart", 0);
	    ei.choice = new Choice();
	    for (int i = 0; i < ROLE_NAMES.length; i++)
		ei.choice.add(Locale.LS(ROLE_NAMES[i]));
	    ei.choice.select(safeRole(leiterArt));
	    return ei;
	}
	if (n == 1) {
	    EditInfo ei = new EditInfo("Bezeichnung", 0, -1, -1);
	    ei.text = bezeichnung;
	    return ei;
	}
	if (n == 2) {
	    EditInfo ei = new EditInfo("", 0, -1, -1);
	    ei.checkbox = new Checkbox(Locale.LS("Bezeichnung anzeigen"),
				      mustShowBezeichnung());
	    return ei;
	}
	if (n == 3 && leiterArt == ROLE_CUSTOM)
	    return new EditInfo("Farbe", customColor.getHexValue()).setIsColor();
	int wireN = n - extraEditCount();
	if (wireN == 0 || wireN == 1)
	    return super.getEditInfo(wireN);
	return null;
    }

    public void setEditValue(int n, EditInfo ei) {
	if (n == 0) {
	    setLeiterArt(ei.choice.getSelectedIndex());
	    ei.newDialog = true;
	    return;
	}
	if (n == 1) {
	    bezeichnung = ei.textf.getText();
	    if (bezeichnung == null || bezeichnung.length() == 0)
		bezeichnung = ROLE_LABELS[safeRole(leiterArt)];
	    return;
	}
	if (n == 2) {
	    flags = ei.changeFlag(flags, FLAG_SHOW_BEZEICHNUNG);
	    return;
	}
	if (n == 3 && leiterArt == ROLE_CUSTOM) {
	    String val = ei.textf.getText();
	    if (val != null && val.length() > 0)
		customColor = new Color(val);
	    return;
	}
	int wireN = n - extraEditCount();
	if (wireN == 0 || wireN == 1)
	    super.setEditValue(wireN, ei);
    }

    WireElm split(int px, int py) {
	if (routePoints == null || routePoints.size() < 2) {
	    EGTLeitungElm nw = new EGTLeitungElm(px, py);
	    copyStyleTo(nw);
	    nw.drag(x2, y2);
	    drag(px, py);
	    return nw;
	}
	int bestSeg = -1;
	int bestDist = Integer.MAX_VALUE;
	for (int i = 0; i < routePoints.size() - 1; i++) {
	    Point a = routePoints.get(i);
	    Point b = routePoints.get(i + 1);
	    int d = lineDistanceSq(a.x, a.y, b.x, b.y, px, py);
	    if (d < bestDist) {
		bestDist = d;
		bestSeg = i;
	    }
	}
	if (bestSeg < 0)
	    return null;
	Point a = routePoints.get(bestSeg);
	Point b = routePoints.get(bestSeg + 1);
	int sx, sy;
	if (a.x == b.x) {
	    sx = a.x;
	    sy = snapGrid(py);
	    sy = Math.max(Math.min(a.y, b.y), Math.min(sy, Math.max(a.y, b.y)));
	} else {
	    sy = a.y;
	    sx = snapGrid(px);
	    sx = Math.max(Math.min(a.x, b.x), Math.min(sx, Math.max(a.x, b.x)));
	}
	if ((sx == x && sy == y) || (sx == x2 && sy == y2))
	    return null;
	ArrayList<Point> rp1 = new ArrayList<Point>();
	for (int i = 0; i <= bestSeg; i++)
	    rp1.add(new Point(routePoints.get(i).x, routePoints.get(i).y));
	rp1.add(new Point(sx, sy));
	ArrayList<Point> rp2 = new ArrayList<Point>();
	rp2.add(new Point(sx, sy));
	for (int i = bestSeg + 1; i < routePoints.size(); i++)
	    rp2.add(new Point(routePoints.get(i).x, routePoints.get(i).y));
	adoptRoute(rp1);
	EGTLeitungElm nw = new EGTLeitungElm(sx, sy);
	copyStyleTo(nw);
	nw.adoptRoute(rp2);
	return nw;
    }

    void copyStyleTo(EGTLeitungElm nw) {
	nw.leiterArt = leiterArt;
	nw.bezeichnung = bezeichnung;
	nw.customColor = customColor;
	nw.flags = flags & ~(FLAG_CORNER_H_FIRST | FLAG_CORNER_V_FIRST);
    }
}
