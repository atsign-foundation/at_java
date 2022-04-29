package org.atsign.common;

public class AtSign {
    public final String atSign;
    private final String withoutPrefix;

    public AtSign(String atSign) {
        atSign = atSign.trim();
        if (! atSign.startsWith("@")) {
            atSign = "@" + atSign;
        }
        this.atSign = atSign;
        this.withoutPrefix = atSign.substring(1);
    }

    public String withoutPrefix() { return withoutPrefix; }

    @Override
    public String toString() {
        return atSign;
    }
}
