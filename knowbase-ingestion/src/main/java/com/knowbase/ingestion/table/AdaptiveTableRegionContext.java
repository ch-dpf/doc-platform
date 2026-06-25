package com.knowbase.ingestion.table;

/**
 * Tracks logical table regions within one worksheet (e.g. weekly vs next-week sections).
 */
public final class AdaptiveTableRegionContext {

    private int regionId;
    private String regionLabel = "";

    public int currentRegionId() {
        return regionId;
    }

    public String currentRegionLabel() {
        return regionLabel;
    }

    public void startRegion(int rowIndex, String label) {
        regionId++;
        regionLabel = label == null ? "" : label.trim();
    }

    public void resetRegion() {
        regionId = 0;
        regionLabel = "";
    }
}
