package uk.ac.cam.cdk23.fjava.tick0;

//A chunk of memory that may be daisy-chained to represent a much larger array.
public final class Chunk {
    public static int CHUNK_SIZE = 3300; //TODO: Tunable.
    public final byte[] a = new byte[CHUNK_SIZE];
    public Chunk previous;
}
