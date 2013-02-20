package uk.ac.cam.cdk23.fjava.tick0;

import java.util.LinkedList;

public final class ReadEntry { //Represents a single filling of the giantBuffer.
    public int readStart;
    public int readLength;
    public final LinkedList<FixerCombo> fixersHere = new LinkedList<>();
}
