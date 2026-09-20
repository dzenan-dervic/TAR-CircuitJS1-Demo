package com.lushprojects.circuitjs1.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.lushprojects.circuitjs1.client.util.Locale;

/**
 * Splitscreen-FUP-Leiste: IEC-Symbol plus Kurztext.
 */
class EGTFupBar extends FlowPanel {
    EGTFupBar() {
	Style s = getElement().getStyle();
	s.setBackgroundColor("#dce8d8");
	s.setDisplay(Style.Display.FLEX);
	s.setProperty("alignItems", "center");
	s.setHeight(100, Style.Unit.PCT);
	s.setPaddingLeft(6, Style.Unit.PX);
	s.setOverflow(Style.Overflow.AUTO);

	Label head = new Label(Locale.LS("FUP"));
	Style hs = head.getElement().getStyle();
	hs.setFontWeight(Style.FontWeight.BOLD);
	hs.setFontSize(11, Style.Unit.PX);
	hs.setMarginRight(6, Style.Unit.PX);
	hs.setColor("#1e3a1e");
	hs.setProperty("whiteSpace", "nowrap");
	add(head);

	addBtn(svgFupI(), "I", "FUP-Eingang (I)", "EGTFupEingangElm");
	addBtn(svgFupQ(), "Q", "FUP-Ausgang (Q)", "EGTFupAusgangElm");
	addSep();
	addBtn(svgAnd(), "UND", "AndGateElm");
	addBtn(svgOr(), "ODER", "OrGateElm");
	addBtn(svgNot(), "NICHT", "InverterElm");
	addBtn(svgNand(), "NAND", "NandGateElm");
	addBtn(svgNor(), "NOR", "NorGateElm");
	addBtn(svgXor(), "XOR", "XorGateElm");
	addSep();
	addBtn(svgRs(), "RS", "RSSpeicherElm");
	addBtn(svgTimer("TON"), "TON", "FUP-Einschaltverzögerung (TON)", "EGTFupTonElm");
	addBtn(svgTimer("TOF"), "TOF", "FUP-Ausschaltverzögerung (TOF)", "EGTFupTofElm");
	addSep();
	addBtn(svgKlemme("I"), "Geber", "EGTHmiGeberElm");
	addBtn(svgKlemme("Q"), "Ausgang", "EGTHmiLastElm");
    }

    void addSep() {
	Label s = new Label();
	Style st = s.getElement().getStyle();
	st.setWidth(1, Style.Unit.PX);
	st.setHeight(20, Style.Unit.PX);
	st.setBackgroundColor("#8aa08a");
	st.setMarginLeft(5, Style.Unit.PX);
	st.setMarginRight(5, Style.Unit.PX);
	add(s);
    }

    void addBtn(String svg, String label, final String cls) {
	addBtn(svg, label, label, cls);
    }

    void addBtn(String svg, String label, String title, final String cls) {
	final FlowPanel b = new FlowPanel();
	Style st = b.getElement().getStyle();
	st.setDisplay(Style.Display.FLEX);
	st.setProperty("alignItems", "center");
	st.setCursor(Style.Cursor.POINTER);
	st.setPadding(1, Style.Unit.PX);
	st.setPaddingLeft(3, Style.Unit.PX);
	st.setPaddingRight(4, Style.Unit.PX);
	st.setMarginRight(2, Style.Unit.PX);
	st.setProperty("borderRadius", "3px");

	Label ic = new Label();
	ic.getElement().setInnerHTML(svg);
	Style ics = ic.getElement().getStyle();
	ics.setProperty("lineHeight", "0");
	ics.setColor("#222");
	ics.setMarginRight(2, Style.Unit.PX);

	Label tx = new Label(Locale.LS(label));
	Style ts = tx.getElement().getStyle();
	ts.setFontSize(11, Style.Unit.PX);
	ts.setColor("#1e3a1e");
	ts.setProperty("whiteSpace", "nowrap");

	b.add(ic);
	b.add(tx);
	b.setTitle(Locale.LS(title));
	b.addDomHandler(e -> b.getElement().getStyle().setBackgroundColor("#c5e0c5"),
		MouseOverEvent.getType());
	b.addDomHandler(e -> b.getElement().getStyle().setBackgroundColor("transparent"),
		MouseOutEvent.getType());
	b.addDomHandler(new ClickHandler() {
	    public void onClick(ClickEvent event) {
		new MyCommand("main", cls).execute();
	    }
	}, ClickEvent.getType());
	add(b);
    }

    static String svgWrap(String inner) {
	return "<svg xmlns='http://www.w3.org/2000/svg' width='22' height='22' viewBox='0 0 24 24'>"
		+ inner + "</svg>";
    }

    static String box() {
	return "<rect x='4' y='3.5' width='13' height='17' fill='#fff' stroke='currentColor' stroke-width='1.5'/>";
    }

    static String pins2() {
	return "<line x1='1' y1='8' x2='4' y2='8' stroke='currentColor' stroke-width='1.5'/>"
		+ "<line x1='1' y1='16' x2='4' y2='16' stroke='currentColor' stroke-width='1.5'/>"
		+ "<line x1='17' y1='12' x2='23' y2='12' stroke='currentColor' stroke-width='1.5'/>";
    }

    static String pins2Bubble() {
	return "<line x1='1' y1='8' x2='4' y2='8' stroke='currentColor' stroke-width='1.5'/>"
		+ "<line x1='1' y1='16' x2='4' y2='16' stroke='currentColor' stroke-width='1.5'/>"
		+ "<circle cx='19' cy='12' r='2' fill='#fff' stroke='currentColor' stroke-width='1.4'/>"
		+ "<line x1='21' y1='12' x2='23.5' y2='12' stroke='currentColor' stroke-width='1.5'/>";
    }

    static String txt(String t, String y) {
	return "<text x='10.5' y='" + y
		+ "' text-anchor='middle' font-size='8' font-family='sans-serif' fill='currentColor'>"
		+ t + "</text>";
    }

    static String svgAnd() { return svgWrap(box() + pins2() + txt("&amp;", "15")); }
    static String svgOr() { return svgWrap(box() + pins2() + txt("≥1", "15")); }
    static String svgXor() { return svgWrap(box() + pins2() + txt("=1", "15")); }
    static String svgNand() { return svgWrap(box() + pins2Bubble() + txt("&amp;", "15")); }
    static String svgNor() { return svgWrap(box() + pins2Bubble() + txt("≥1", "15")); }

    static String svgNot() {
	return svgWrap(
		"<rect x='4' y='3.5' width='13' height='17' fill='#fff' stroke='currentColor' stroke-width='1.5'/>"
		+ "<line x1='1' y1='12' x2='4' y2='12' stroke='currentColor' stroke-width='1.5'/>"
		+ "<circle cx='19' cy='12' r='2' fill='#fff' stroke='currentColor' stroke-width='1.4'/>"
		+ "<line x1='21' y1='12' x2='23.5' y2='12' stroke='currentColor' stroke-width='1.5'/>"
		+ txt("1", "15"));
    }

    static String svgRs() {
	return svgWrap(
		"<rect x='3' y='3.5' width='18' height='17' fill='#fff' stroke='currentColor' stroke-width='1.5'/>"
		+ "<text x='7' y='11' font-size='6.5' font-family='sans-serif' fill='currentColor'>S</text>"
		+ "<text x='7' y='18' font-size='6.5' font-family='sans-serif' fill='currentColor'>R</text>"
		+ "<text x='16' y='14.5' text-anchor='middle' font-size='6.5' font-family='sans-serif' fill='currentColor'>Q</text>"
		+ "<line x1='1' y1='9' x2='3' y2='9' stroke='currentColor' stroke-width='1.4'/>"
		+ "<line x1='1' y1='16' x2='3' y2='16' stroke='currentColor' stroke-width='1.4'/>"
		+ "<line x1='21' y1='12' x2='23.5' y2='12' stroke='currentColor' stroke-width='1.4'/>");
    }

    static String svgTimer(String type) {
	boolean ton = "TON".equals(type);
	String wave = ton
		? "<polyline points='5,10 7,10 7,5.5 18.5,5.5 18.5,10 20,10' fill='none' stroke='currentColor' stroke-width='1.3'/>"
		  + "<polyline points='5,17.5 7,17.5 7,15 7,17.5 13,17.5 13,11.5 16.5,11.5 16.5,17.5 20,17.5' fill='none' stroke='currentColor' stroke-width='1.3'/>"
		  + "<line x1='7' y1='10' x2='7' y2='12.5' stroke='currentColor' stroke-width='1.3'/>"
		: "<polyline points='5,10 7.7,10 7.7,5.5 10.4,5.5 10.4,10 20,10' fill='none' stroke='currentColor' stroke-width='1.3'/>"
		  + "<polyline points='5,17.5 7.7,17.5 7.7,11.5 18.1,11.5 18.1,17.5 20,17.5' fill='none' stroke='currentColor' stroke-width='1.3'/>"
		  + "<line x1='10.4' y1='11.5' x2='10.4' y2='14.2' stroke='currentColor' stroke-width='1.3'/>"
		  + "<line x1='10.4' y1='15.3' x2='10.4' y2='17.5' stroke='currentColor' stroke-width='1.3'/>";
	return svgWrap(
		"<rect x='3' y='3.5' width='18' height='17' fill='#fff' stroke='currentColor' stroke-width='1.5'/>"
		+ "<line x1='1' y1='12' x2='3' y2='12' stroke='currentColor' stroke-width='1.4'/>"
		+ "<line x1='21' y1='12' x2='23.5' y2='12' stroke='currentColor' stroke-width='1.4'/>"
		+ wave);
    }

    static String svgFupI() {
	return svgWrap(
		"<rect x='3' y='5' width='14' height='14' fill='#fff' stroke='#333' stroke-width='1.5'/>"
		+ "<text x='10' y='15.5' text-anchor='middle' font-size='10' font-family='sans-serif' font-weight='bold' fill='#2ecc71'>I</text>"
		+ "<path d='M 17 8.5 A 3.5 3.5 0 0 1 17 15.5' fill='#fff' stroke='#333' stroke-width='1.4'/>");
    }

    static String svgFupQ() {
	return svgWrap(
		"<rect x='7' y='5' width='14' height='14' fill='#fff' stroke='#333' stroke-width='1.5'/>"
		+ "<text x='14' y='15.5' text-anchor='middle' font-size='10' font-family='sans-serif' font-weight='bold' fill='#111'>Q</text>"
		+ "<path d='M 7 8.5 A 3.5 3.5 0 0 0 7 15.5' fill='#dce8d8' stroke='#333' stroke-width='1.4'/>");
    }

    static String svgKlemme(String letter) {
	return svgWrap(
		"<rect x='4' y='4' width='16' height='16' rx='2' fill='#fff' stroke='currentColor' stroke-width='1.5'/>"
		+ "<circle cx='8' cy='8' r='1.6' fill='#2ecc71'/>"
		+ "<text x='14' y='16' text-anchor='middle' font-size='8' font-family='sans-serif' font-weight='bold' fill='currentColor'>"
		+ letter + "</text>");
    }
}
