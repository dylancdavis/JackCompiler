public class JackVariable {

    private String kind;
    private String type;

    private int pos;


    public JackVariable(String kind, String type, int pos) {
        this.kind = kind;
        this.type = type;
        this.pos = pos;
    }

    public String getKind() {
        return kind;
    }

    public String getType() {
        return type;
    }

    public int getPos() {
        return pos;
    };
}
