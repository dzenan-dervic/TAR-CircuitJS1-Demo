/*
    TAR-Dervic EGT extension: Meldeleuchte P1 (ZH + Stromlaufplan), I203
    Eigenes Menü-Element; elektrisch = EGTLeuchteElm mit FLAG_MELD.
*/

package com.lushprojects.circuitjs1.client;

/**
 * Meldeleuchte Dump 450. Default: Stromlaufplan (Kreis+X, zwei Pole, P1 links).
 * ZH: Kreis + X + Goldring. Kein Edit-Haken „Meldeleuchte“.
 */
class EGTMeldeleuchteElm extends EGTLeuchteElm {

    public EGTMeldeleuchteElm(int xx, int yy) {
	super(xx, yy);
	flags |= EGTStyle.FLAG_MELD | EGTStyle.FLAG_UEBERSICHT;
	designation = "P1";
	setSize(1);
	sizeX = MELD_US_SIZE_X;
	sizeY = MELD_US_SIZE_Y;
	layoutPins();
	allocNodes();
	x2 = x + sizeX * cspc2;
	y2 = y + sizeY * cspc2;
	setPoints();
    }

    public EGTMeldeleuchteElm(int xa, int ya, int xb, int yb, int f,
			      StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	flags |= EGTStyle.FLAG_MELD;
	if (designation == null || designation.length() == 0
		|| "E1".equals(designation) || "H1".equals(designation))
	    designation = "P1";
	if (!uebersicht() && sizeX == 3 && sizeY == 4) {
	    sizeX = MELD_ZH_SIZE_X;
	    sizeY = MELD_ZH_SIZE_Y;
	    setSize(2);
	    x2 = x + sizeX * cspc2;
	    y2 = y;
	}
	layoutPins();
	setPoints();
    }

    boolean meldeleuchte() { return true; }

    public EditInfo getChipEditInfo(int n) {
	if (n <= 1)
	    return super.getChipEditInfo(n);
	return super.getChipEditInfo(n + 1);
    }

    public void setChipEditValue(int n, EditInfo ei) {
	if (n <= 1)
	    super.setChipEditValue(n, ei);
	else
	    super.setChipEditValue(n + 1, ei);
	flags |= EGTStyle.FLAG_MELD;
    }

    void undumpXml(XMLDeserializer xml) {
	super.undumpXml(xml);
	flags |= EGTStyle.FLAG_MELD;
	layoutPins();
	setPoints();
    }

    int getDumpType() { return 450; }
    String getXmlDumpType() { return "EGTMeldeleuchte"; }
}
