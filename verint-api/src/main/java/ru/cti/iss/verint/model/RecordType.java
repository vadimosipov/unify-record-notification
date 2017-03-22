package ru.cti.iss.verint.model;

public enum RecordType {

    DO_NOT_RECORD(0),          // "не записывать"
    RECORD_ALL(1),             // "всегда записывать"
    UNKNOWN(2),                // уточнить классификатор
    APPLICATION_CONTROLLED(3), // "управляется приложением"
    START_ON_TRIGGER(4);       // "записывать по запросу"

    private final int id;

    RecordType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    public static RecordType cast(int type) {
        for (RecordType recordType : values()) {
            if (recordType.getId() == type) {
                return recordType;
            }
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    public boolean isStartOnTrigger() {
        return equals(START_ON_TRIGGER) || equals(APPLICATION_CONTROLLED);
    }
}
