package com.knowbase.domain.security;

public enum AclPermission {
    READ,
    WRITE,
    ADMIN;

    public boolean satisfies(AclPermission required) {
        if (required == null || required == READ) {
            return this == READ || this == WRITE || this == ADMIN;
        }
        if (required == WRITE) {
            return this == WRITE || this == ADMIN;
        }
        return this == ADMIN;
    }
}
