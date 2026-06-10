package com.knowbase.library.config;

import java.util.ArrayList;
import java.util.List;

public class GovernanceRulesSettings {

    /** auto | manual-review */
    private String ingestReviewMode = "auto";
    private boolean inheritLibraryPermissions = true;
    private int retentionDays = 0;
    private String archivePolicy = "none";
    private List<String> complianceTags = new ArrayList<>();
    private boolean auditLogEnabled = true;

    public String getIngestReviewMode() {
        return ingestReviewMode;
    }

    public void setIngestReviewMode(String ingestReviewMode) {
        this.ingestReviewMode = ingestReviewMode;
    }

    public boolean isInheritLibraryPermissions() {
        return inheritLibraryPermissions;
    }

    public void setInheritLibraryPermissions(boolean inheritLibraryPermissions) {
        this.inheritLibraryPermissions = inheritLibraryPermissions;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getArchivePolicy() {
        return archivePolicy;
    }

    public void setArchivePolicy(String archivePolicy) {
        this.archivePolicy = archivePolicy;
    }

    public List<String> getComplianceTags() {
        return complianceTags;
    }

    public void setComplianceTags(List<String> complianceTags) {
        this.complianceTags = complianceTags;
    }

    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    public void setAuditLogEnabled(boolean auditLogEnabled) {
        this.auditLogEnabled = auditLogEnabled;
    }
}
