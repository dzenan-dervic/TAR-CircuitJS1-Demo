/*
    Copyright (C) Paul Falstad and Iain Sharp

    This file is part of CircuitJS1.

    CircuitJS1 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    CircuitJS1 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CircuitJS1.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lushprojects.circuitjs1.client;

import java.util.Vector;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lushprojects.circuitjs1.client.util.Locale;

public class UndoManager {

    private static final String LEGACY_RECOVERY_KEY = "circuitRecovery";
    private static final String PREVIOUS_RECOVERY_KEY_PREFIX = "circuitRecovery:";
    private static final String RECOVERY_KEY_PREFIX = "circuitRecovery:v2:";
    private static final String RECOVERY_TIME_SUFFIX = ":savedAt";

    CirSim sim;
    Vector<UndoItem> undoStack, redoStack;
    String recoveryKey;
    boolean recoveryPromptHandled;

    UndoManager(CirSim sim) {
	this.sim = sim;
	undoStack = new Vector<UndoItem>();
	redoStack = new Vector<UndoItem>();
    }

    class UndoItem {
	public String dump;
	public double scale, transform4, transform5;
	UndoItem(String d) {
	    dump = d;
	    scale = sim.transform[0];
	    transform4 = sim.transform[4];
	    transform5 = sim.transform[5];
	}
    }

    void pushUndo() {
    	redoStack.removeAllElements();
    	String s = sim.dumpCircuit();
    	if (undoStack.size() > 0 &&
    			s.compareTo(undoStack.lastElement().dump) == 0)
    	    return;
    	undoStack.add(new UndoItem(s));
    	enableUndoRedo();
    	sim.savedFlag = false;
    }

    void doUndo() {
    	if (undoStack.size() == 0)
    		return;
    	redoStack.add(new UndoItem(sim.dumpCircuit()));
    	UndoItem ui = undoStack.remove(undoStack.size()-1);
    	loadUndoItem(ui);
    	enableUndoRedo();
    }

    void doRedo() {
    	if (redoStack.size() == 0)
    		return;
    	undoStack.add(new UndoItem(sim.dumpCircuit()));
    	UndoItem ui = redoStack.remove(redoStack.size()-1);
    	loadUndoItem(ui);
    	enableUndoRedo();
    }

    void loadUndoItem(UndoItem ui) {
	sim.loader.readCircuit(ui.dump, CircuitLoader.RC_NO_CENTER);
	sim.transform[0] = sim.transform[3] = ui.scale;
	sim.transform[4] = ui.transform4;
	sim.transform[5] = ui.transform5;
    }

    void doRecover() {
	if (sim.recovery == null)
	    return;
	pushUndo();
	sim.loader.readCircuit(sim.recovery);
	sim.allowSave(false);
	sim.unsavedChanges = true;
	sim.menus.recoverItem.setEnabled(false);
    }

    void enableUndoRedo() {
    	sim.menus.redoItem.setEnabled(redoStack.size() > 0);
    	sim.menus.undoItem.setEnabled(undoStack.size() > 0);
    }

    void writeRecoveryToStorage() {
	sim.console("write recovery");
    	Storage stor = Storage.getLocalStorageIfSupported();
    	if (stor == null)
    		return;
    	String s = sim.dumpCircuit();
	stor.setItem(getRecoveryKey(), s);
	stor.setItem(getRecoveryKey() + RECOVERY_TIME_SUFFIX,
		Long.toString(System.currentTimeMillis()));
	sim.recovery = s;
	if (sim.menus != null && sim.menus.recoverItem != null)
	    sim.menus.recoverItem.setEnabled(true);
    }

    void readRecovery() {
	Storage stor = Storage.getLocalStorageIfSupported();
	if (stor == null)
		return;
	clearObsoleteRecovery(stor);
	sim.recovery = stor.getItem(getRecoveryKey());
	if (sim.menus != null && sim.menus.recoverItem != null)
	    sim.menus.recoverItem.setEnabled(sim.recovery != null);
    }

    void clearObsoleteRecovery(Storage stor) {
	stor.removeItem(LEGACY_RECOVERY_KEY);
	String previousKey = PREVIOUS_RECOVERY_KEY_PREFIX + getRecoveryContext();
	stor.removeItem(previousKey);
	stor.removeItem(previousKey + RECOVERY_TIME_SUFFIX);
    }

    void scheduleRecoveryPrompt() {
	if (sim.recovery == null) {
	    recoveryPromptHandled = true;
	    return;
	}
	new Timer() {
	    int attempts;

	    @Override
	    public void run() {
		if (recoveryPromptHandled)
		    return;
		if (sim.elmList.size() == 0 && attempts++ < 20) {
		    schedule(250);
		    return;
		}
		promptForRecovery();
	    }
	}.schedule(300);
    }

    void promptForRecovery() {
	if (recoveryPromptHandled || sim.recovery == null)
	    return;
	recoveryPromptHandled = true;
	if (sim.recovery.equals(sim.dumpCircuit())) {
	    clearRecovery();
	    return;
	}

	Storage stor = Storage.getLocalStorageIfSupported();
	String savedAt = stor == null ? null
		: formatRecoveryTimestamp(stor.getItem(getRecoveryKey() + RECOVERY_TIME_SUFFIX));
	showRecoveryDialog(savedAt);
    }

    void showRecoveryDialog(String savedAt) {
	final Dialog dialog = new Dialog();
	dialog.setText(Locale.LS("Restore Auto-Save"));
	dialog.setGlassEnabled(true);

	VerticalPanel panel = new VerticalPanel();
	panel.setSpacing(12);
	panel.setWidth("340px");
	panel.add(new Label(Locale.LS("An auto-saved draft was found.")));
	if (savedAt != null)
	    panel.add(new Label(Locale.LS("Saved:") + " " + savedAt));
	panel.add(new Label(Locale.LS("Restore this draft?")));

	HorizontalPanel buttons = new HorizontalPanel();
	buttons.setSpacing(8);
	Button discardButton = new Button(Locale.LS("Discard"));
	Button restoreButton = new Button(Locale.LS("Restore"));
	discardButton.setWidth("120px");
	restoreButton.setWidth("120px");
	buttons.add(discardButton);
	buttons.add(restoreButton);
	panel.add(buttons);
	panel.setCellHorizontalAlignment(buttons, HasHorizontalAlignment.ALIGN_RIGHT);

	discardButton.addClickHandler(new ClickHandler() {
	    @Override
	    public void onClick(ClickEvent event) {
		dialog.hide();
		clearRecovery();
	    }
	});
	restoreButton.addClickHandler(new ClickHandler() {
	    @Override
	    public void onClick(ClickEvent event) {
		dialog.hide();
		doRecover();
	    }
	});

	dialog.setWidget(panel);
	dialog.center();
	restoreButton.setFocus(true);
    }

    void clearRecovery() {
	Storage stor = Storage.getLocalStorageIfSupported();
	if (stor != null) {
	    stor.removeItem(getRecoveryKey());
	    stor.removeItem(getRecoveryKey() + RECOVERY_TIME_SUFFIX);
	}
	sim.recovery = null;
	if (sim.menus != null && sim.menus.recoverItem != null)
	    sim.menus.recoverItem.setEnabled(false);
    }

    String getRecoveryKey() {
	if (recoveryKey == null)
	    recoveryKey = RECOVERY_KEY_PREFIX + getRecoveryContext();
	return recoveryKey;
    }

    private static native String getRecoveryContext() /*-{
	var path = $wnd.location.pathname || "circuitjs";
	var search = $wnd.location.search || "";
	try {
	    if ($wnd.parent && $wnd.parent !== $wnd) {
		var parentPath = $wnd.parent.location.pathname || "";
		if (parentPath.indexOf("egt-hmi-tor") >= 0)
		    return "egt-hmi-tor";
		if (parentPath.indexOf("egt-hmi-pumpe") >= 0)
		    return "egt-hmi-pumpe";
		if (parentPath.indexOf("egt-hmi-stecken") >= 0)
		    return "egt-hmi-stecken";
	    }
	} catch (e) {
	}
	var match = search.match(/[?&]startCircuit=([^&]+)/);
	if (match)
	    return path + "|startCircuit=" + decodeURIComponent(match[1]);
	return path + "|workspace";
    }-*/;

    private static native String formatRecoveryTimestamp(String timestamp) /*-{
	if (!timestamp)
	    return null;
	var value = Number(timestamp);
	if (!isFinite(value))
	    return null;
	return new Date(value).toLocaleString();
    }-*/;
}
