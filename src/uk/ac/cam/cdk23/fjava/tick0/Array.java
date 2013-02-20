package uk.ac.cam.cdk23.fjava.tick0;

import java.util.LinkedList;

public final class Array {
    public final int[] radixStarts = new int[256]; //Swapfile geometry.
    public int[] counts = new int[256]; //How far through final chunk is filled.

    public final byte[] radixOrders = new byte[256]; //0 - not present. 1 - resolves here. 2 - resolves later.
    public Array[] higherChunks;

    public LinkedList<int[]> lastResort; //Waste of a pointer most of the time.
  //  public LinkedList<boolean[]> lastResortHadness; //Waste of a pointer most of the time.
}
