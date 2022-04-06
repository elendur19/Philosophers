import java.io.Serializable;

public class Message implements Serializable {
    private int source;
    private int forkId;
    private Fork fork;

    public Message(int source, int forkId, Fork fork) {
        this.source = source;
        this.forkId = forkId;
        this.fork = fork;
    }

    public int getSource() {
        return source;
    }

    public int getForkId() {
        return forkId;
    }

    public Fork getFork() {
        return fork;
    }
}
