package ru.cti.iss.verint.model;


public class Channel {

    private final String extension;

    private final RecordType type;

    public Channel (String extension, RecordType type) {
        this.extension = extension;
        this.type = type;
    }

    public String getExtension () {
        return extension;
    }

    public RecordType getType () {
        return type;
    }

    public boolean isStartOnTriggerType () {
        return type.isStartOnTrigger ();
    }

    @Override
    public boolean equals (final Object o) {
        if (this == o) return true;
        if (o == null || getClass () != o.getClass ()) return false;

        final Channel channel = (Channel) o;

        if (extension != null ? !extension.equals (channel.extension) : channel.extension != null) return false;

        return true;
    }

    @Override
    public int hashCode () {
        return extension != null ? extension.hashCode () : 0;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "extension='" + extension + '\'' +
                ", type=" + type +
                '}';
    }
}
