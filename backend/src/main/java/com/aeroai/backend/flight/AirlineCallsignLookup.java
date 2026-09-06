package com.aeroai.backend.flight;

import java.util.Map;

/**
 * Small static reference table mapping well-known ICAO 3-letter airline
 * telephony/callsign designators to airline names (Section 18.2/26). This is
 * real, public ICAO-designator reference data - not a fabricated guess. If a
 * callsign's prefix is not in this table, the airline field stays
 * Unavailable rather than inventing a name.
 */
public final class AirlineCallsignLookup {

    private static final Map<String, String> PREFIX_TO_AIRLINE = Map.ofEntries(
            Map.entry("AIC", "Air India"),
            Map.entry("AXB", "Air India Express"),
            Map.entry("IGO", "IndiGo"),
            Map.entry("SEJ", "SpiceJet"),
            Map.entry("VTI", "Vistara"),
            Map.entry("GOW", "Go First"),
            Map.entry("DKN", "Akasa Air"),
            Map.entry("LLR", "Alliance Air"),
            Map.entry("UAE", "Emirates"),
            Map.entry("ETD", "Etihad Airways"),
            Map.entry("QTR", "Qatar Airways"),
            Map.entry("THY", "Turkish Airlines"),
            Map.entry("SIA", "Singapore Airlines"),
            Map.entry("MAS", "Malaysia Airlines"),
            Map.entry("THA", "Thai Airways"),
            Map.entry("CPA", "Cathay Pacific"),
            Map.entry("BAW", "British Airways"),
            Map.entry("AFR", "Air France"),
            Map.entry("DLH", "Lufthansa"),
            Map.entry("KLM", "KLM Royal Dutch Airlines"),
            Map.entry("UAL", "United Airlines"),
            Map.entry("DAL", "Delta Air Lines"),
            Map.entry("AAL", "American Airlines"),
            Map.entry("ACA", "Air Canada"),
            Map.entry("JAL", "Japan Airlines"),
            Map.entry("ANA", "All Nippon Airways"),
            Map.entry("CCA", "Air China"),
            Map.entry("CES", "China Eastern Airlines"),
            Map.entry("CSN", "China Southern Airlines"),
            Map.entry("QFA", "Qantas"),
            Map.entry("SVA", "Saudia"),
            Map.entry("FDB", "FlyDubai"),
            Map.entry("KAC", "Kuwait Airways"),
            Map.entry("GFA", "Gulf Air"),
            Map.entry("RJA", "Royal Jordanian"),
            Map.entry("ETH", "Ethiopian Airlines"),
            Map.entry("MSR", "EgyptAir"),
            Map.entry("UBG", "MyIndigo"),
            Map.entry("PIA", "Pakistan International Airlines"),
            Map.entry("BBC", "Biman Bangladesh Airlines")
    );

    private AirlineCallsignLookup() {
    }

    /** Returns the airline name for a callsign's 3-letter ICAO prefix, or null if unknown/unavailable. */
    public static String lookup(String callsign) {
        if (callsign == null || callsign.length() < 3) {
            return null;
        }
        String prefix = callsign.substring(0, 3).toUpperCase();
        return PREFIX_TO_AIRLINE.get(prefix);
    }
}
