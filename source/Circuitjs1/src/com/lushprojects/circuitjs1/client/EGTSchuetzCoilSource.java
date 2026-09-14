/*
    TAR-Dervic EGT: Schütz-Kopplung über Betriebsmittelkennzeichen (S301)
*/

package com.lushprojects.circuitjs1.client;

/** Spulenquelle für EGTSchuetzLink (getrennte Spule oder gekoppeltes Schütz). */
interface EGTSchuetzCoilSource {
    String schuetzDesignation();
    boolean coilEnergized();
}
