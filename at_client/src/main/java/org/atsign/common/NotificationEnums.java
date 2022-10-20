package org.atsign.common;

public class NotificationEnums {
    
    public enum Operation {
        UPDATE, DELETE, APPEND, REMOVE,;

        public String toString() {
            return this.name().toLowerCase();
        }

    }

    public enum Priority {
        LOW, MEDIUM, HIGH,;

        public String toString() {
            return this.name().toLowerCase();
        }

    }

    public enum Strategy {
        ALL, LATEST,;

        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum MessageType {
        KEY, TEXT,;

        public String toString() {
            return "MessageType." + this.name().toLowerCase();
        }
    }
}
