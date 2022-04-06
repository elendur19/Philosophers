import java.io.Serializable;

public class Fork implements Serializable {
    private int id;
    private boolean isDirty;


    public Fork (boolean isDirty, int id) {
        this.isDirty = isDirty;
        this.id = id;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
