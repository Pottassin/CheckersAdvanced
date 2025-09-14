public enum PieceType {
    p1(true, false),
    p1k(true, true),
    p2(false, false),
    p2k(false, true);
    public final boolean isP1;
    public final boolean isK;
    PieceType(boolean isP1, boolean isK) {
        this.isP1 = isP1;
        this.isK = isK;
    }
}