package uk.ac.cam.cdk23.fjava.tick0;

public final class FixerCombo {
    public int firstIndex; //The first index in the swapfile where this set of fixes should start being applied.
    public byte order;
    public byte fix1;
    public byte fix2;
    public byte fix3;
    public int[] counts;
}
