package uk.ac.cam.cdk23.fjava.tick0;

public final class CountingUnit {
    public int count = 0; //Times the master byte is seen.
    public final int[] a = new int[256]; //Times it is seen in conjunction with each of the other bytes...
}
