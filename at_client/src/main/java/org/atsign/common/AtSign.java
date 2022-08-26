package org.atsign.common;

public class AtSign {
    public final String atSign;
    private final String withoutPrefix;

    public AtSign(String atSign) {
        if (atSign == null || atSign.trim().isEmpty()) {
            throw new IllegalArgumentException ("atSign may not be null or empty");
        }

        this.atSign = formatAtSign(atSign);

        if ("@".equals(atSign)) {
            throw new IllegalArgumentException ("'" + atSign + "' is not a valid atSign");
        }

        this.withoutPrefix = this.atSign.substring(1);
    }

    public String withoutPrefix() { return withoutPrefix; }

    @Override
    public String toString() {
        return atSign;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AtSign atSign1 = (AtSign) o;

        return atSign.equals(atSign1.atSign);
    }

    @Override
    public int hashCode() {
        return atSign.hashCode();
    }

    /**
     * Returns a formatted atSign
     * @param atSignStr e.g. "@bob"
     * @return formatted atSign (e.g. "alice " --> "@alice")
     */
    public static String formatAtSign(String atSignStr) {
        atSignStr = atSignStr.trim();
        if (!atSignStr.startsWith("@")) {
            atSignStr = "@" + atSignStr;
        }
        return atSignStr;
    }
    
}
