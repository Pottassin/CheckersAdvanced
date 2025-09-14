public enum Mode {
    checkers(true),
    swordfight(false);
    public boolean isCheckers;
    Mode(boolean c) {
        isCheckers = c;
    }
}
