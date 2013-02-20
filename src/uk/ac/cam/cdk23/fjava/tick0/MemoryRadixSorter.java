package uk.ac.cam.cdk23.fjava.tick0;

public final class MemoryRadixSorter {
    public Chunk[] chunkPool;
    //private int chunkCounter = 0;
    //private int[] counts = new int[65536];
    //private int[] placesToGo = new int[65536];
    //private long[][] deeperCountsHad = new long[256][4];
    //private int[][]counts = new int[256][256];
    private final Chunk[] endChunks = new Chunk[256];
    private final Chunk[] chunks = new Chunk[256];
    private final int[] chunkCounts = new int[256];

    public final void sortFour(byte[] buffer, int s, int e) {
        repeatedlyRadixSort(buffer, s, e, 0);
        //Prepare for sortThree and pass to it.

        final int[] cc = chunkCounts;
        final Chunk[] cp = chunkPool;
        final Chunk[] ec = endChunks;
        final Chunk[] ch = chunks;
        int chunkCounter = 0;
        Chunk c;
        Chunk c2;
        for(int i = s; i < e; i+= 4) { //Iterate across the second byte..
            int b2 = buffer[i]+128;
            c = ec[b2];
            if(c == null) {
                //No handling for running out of memory because that ought never to happen.
                //Hopefully.
                //Ha.
                ec[b2] = cp[chunkCounter];
                ch[b2] = cp[chunkCounter];
                chunkCounter++;
                c = endChunks[b2];
            } else if(cc[b2] > Chunk.CHUNK_SIZE-4) {
                c2 = cp[chunkCounter];
                chunkCounter++;
                c.previous = c2;
                ec[b2] = c2;
                cc[b2] = 0;
                c = c2;
            }
            //Consider memcpy a lump of input to save writing bytes 1 and 2...
            c.a[cc[b2]] = buffer[i]; //1
            c.a[cc[b2]+1] = buffer[i+1];   //2
            c.a[cc[b2]+2] = buffer[i+2]; //Store all bytes in chunk to enable memcpy later..
            c.a[cc[b2]+3] = buffer[i+3];
            cc[b2] += 4;
        }
        System.out.flush();
        //Now have input categorised into chunks. Bytes 3 and 4 are stored in chunks indexed by byte 2.
        //Reconstitute into buffer and invoke sortTwo (The counting sort) to finish the job.
        //Don't forget to reset all structures as you go!
        int toCopy;
        int outPointer = s;
        for(int i = 0; i < 256; i++) {
            c = ch[i];
            ch[i] = null;
            ec[i] = null;
            while(c != null) {
                if(c.previous == null) {
                    toCopy = cc[i];
                    cc[i] = 0;
                } else {
                    toCopy = Chunk.CHUNK_SIZE;
                }
                //System.out.println(Arrays.toString(c.a));
                System.arraycopy(c.a, 0, buffer, outPointer, toCopy);
                outPointer += toCopy;
                c2 = c.previous;
                c.previous = null;
                c = c2;
            }
        }
    }

    public final void repeatedlyRadixSort(byte[] buffer, int s, int e, int halt) {
        Chunk c;
        final Chunk[] ec = endChunks;
        final int[] cc = chunkCounts;
        final Chunk[] cp = chunkPool;
        final Chunk[] ch = chunks;
        int chunkCounter = 0;
        Chunk c2;
        byte[] cdota;
        byte times = 3;
        while(times != halt) {
            for(int i = s; i < e; i+= 4) { //Iterate across the offset'th byte....
                int b2;
                if(buffer[i+times] < 0) {
                    b2 = buffer[i+times]+256;
                } else {
                    b2 = buffer[i+times];
                }
                c = ec[b2];
                if(c == null) {
                    //No handling for running out of memory because that ought never to happen.
                    //Hopefully.
                    //Ha.
                    ch[b2] = cp[chunkCounter];
                    ec[b2] = cp[chunkCounter];
                    chunkCounter++;
                    c = ec[b2];
                } else if(cc[b2] > Chunk.CHUNK_SIZE-4) {
                    c2 = cp[chunkCounter];
                    chunkCounter++;
                    c.previous = c2;
                    ec[b2] = c2;
                    cc[b2] = 0;
                    c = c2;
                }
                cdota = c.a;
                //Consider memcpy a lump of input to save writing bytes 1 and 2...
                final int ccb2 = cc[b2];
                cdota[ccb2] = buffer[i]; //1
                cdota[ccb2+1] = buffer[i+1];   //2
                cdota[ccb2+2] = buffer[i+2]; //Store all bytes in chunk to enable memcpy later..
                cdota[ccb2+3] = buffer[i+3];
                cc[b2] += 4;
            }
            //Don't forget to reset all structures as you go!
            int toCopy;
            int outPointer = s;
            for(int i = 0; i < 256; i++) {
                c = ch[i];
                ch[i] = null;
                ec[i] = null;
                while(c != null) {
                    if(c.previous == null) {
                        toCopy = cc[i];
                        cc[i] = 0;
                    } else {
                        toCopy = Chunk.CHUNK_SIZE;
                    }
                    System.arraycopy(c.a, 0, buffer, outPointer, toCopy);
                    outPointer += toCopy;
                    c2 = c.previous;
                    c.previous = null;
                    c = c2;
                }
            }
            chunkCounter = 0;
            times--;
        }
    }

}
