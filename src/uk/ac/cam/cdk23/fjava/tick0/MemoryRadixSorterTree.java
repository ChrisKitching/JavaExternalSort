package uk.ac.cam.cdk23.fjava.tick0;

public final class MemoryRadixSorterTree {
    public Chunk[] chunkPool;
    private int chunkCounter = 0;
    private int[] counts = new int[65536];
    private int[] placesToGo = new int[65536];
    private long[][] deeperCountsHad = new long[256][4];
    private final Chunk[] endChunks = new Chunk[256];
    private final Chunk[] chunks = new Chunk[256];
    private final int[] chunkCounts = new int[256];
    private static final byte NEGONE = (byte) -1;

    public final void sortFour(byte[] buffer, int s, int e) {
        repeatedlyRadixSort(buffer, s, e, 0);
        //Prepare for sortThree and pass to it.

        final int[] cc = chunkCounts;
        Chunk c;
        Chunk c2;
        for(int i = s; i < e; i+= 4) { //Iterate across the second byte..
            int b2 = buffer[i]+128;
            c = endChunks[b2];
            if(c == null) {
                //No handling for running out of memory because that ought never to happen.
                //Hopefully.
                //Ha.
                //assert(chunkCounter != numChunks);
                endChunks[b2] = chunkPool[chunkCounter];
                chunks[b2] = chunkPool[chunkCounter];
                //assert(chunks[b2].previous == null);
                chunkCounter++;
                c = endChunks[b2];
               // System.out.println("Setting "+b2+" true");
            } else if(cc[b2] > Chunk.CHUNK_SIZE-4) {
                //assert(chunkCounter != numChunks);
                c2 = chunkPool[chunkCounter];
                //assert(c2.previous == null);
                chunkCounter++;
                c.previous = c2;
                endChunks[b2] = c2;
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
        //Now have input categorised into chunks. Bytes 3 and 4 are stored in chunks indexed by byte 2.
        //Reconstitute into buffer and invoke sortTwo (The counting sort) to finish the job.
        //Don't forget to reset all structures as you go!
        int toCopy;
        int outPointer = s;
        for(int i = 0; i < 256; i++) {
            c = chunks[i];
            chunks[i] = null;
            endChunks[i] = null;
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
        chunkCounter = 0;
    }

    public final void threeSort(byte[] buffer, int s, int e) {
        //Change of plan for counting sort - want to MSB sort here now (Once only)
        Chunk c;
        final int[] cc = chunkCounts;
        Chunk c2;
        for(int i = s; i < e; i+= 4) {
            int b2;
            if(buffer[i+1] < 0) {
                b2 = buffer[i+1]+256;
            } else {
                b2 = buffer[i+1];
            }
            c = endChunks[b2];
            if(c == null) {
                //No handling for running out of memory because that ought never to happen.
                //Hopefully.
                //Ha.
                //assert(chunkCounter != numChunks);
                chunks[b2] = chunkPool[chunkCounter];
                endChunks[b2] = chunkPool[chunkCounter];
                //assert(chunks[b2].previous == null);
                chunkCounter++;
                c = endChunks[b2];
            } else if(cc[b2] > Chunk.CHUNK_SIZE-4) {
                //assert(chunkCounter != numChunks);
                c2 = chunkPool[chunkCounter];
                //assert(c2.previous == null);
                chunkCounter++;
                c.previous = c2;
                endChunks[b2] = c2;
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
        //Don't forget to reset all structures as you go!
        int toCopy;
        int outPointer = s;
        int radixStart;
        for(int i = 0; i < 256; i++) {
            c = chunks[i];
            if(c != null) { //Ew.
                chunks[i] = null;
                endChunks[i] = null;
                radixStart = outPointer;
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
                //System.out.println("sortTwo:"+radixStart+","+(outPointer-1));
                sortTwo(buffer, radixStart, outPointer - 1);
            }
        }
        chunkCounter = 0;
    }
    public final void repeatedlyOneSort(byte[] buffer, int s, int e, byte fix1, byte fix2, byte fix3) {
        Chunk c;
        final int[] cc = chunkCounts;
        Chunk c2;
        byte times = 3;
        while(times != 2) {
            for(int i = s; i < e; i+= 4) { //Iterate across the offset'th byte....
                int b2;
                if(buffer[i+times] < 0) {
                    b2 = buffer[i+times]+256;
                } else {
                    b2 = buffer[i+times];
                }
                c = endChunks[b2];
                if(c == null) {
                    //No handling for running out of memory because that ought never to happen.
                    //Hopefully.
                    //Ha.
                    //assert(chunkCounter != numChunks);
                    chunks[b2] = chunkPool[chunkCounter];
                    endChunks[b2] = chunkPool[chunkCounter];
                    //assert(chunks[b2].previous == null);
                    chunkCounter++;
                    c = endChunks[b2];
                } else if(cc[b2] > Chunk.CHUNK_SIZE-4) {
                    //assert(chunkCounter != numChunks);
                    c2 = chunkPool[chunkCounter];
                    //assert(c2.previous == null);
                    chunkCounter++;
                    c.previous = c2;
                    endChunks[b2] = c2;
                    cc[b2] = 0;
                    c = c2;
                }
                //Consider memcpy a lump of input to save writing bytes 1 and 2...
                c.a[cc[b2]] = fix1; //1
                c.a[cc[b2]+1] = fix2;   //2
                c.a[cc[b2]+2] = fix3; //Store all bytes in chunk to enable memcpy later..
                c.a[cc[b2]+3] = buffer[i+3];
                cc[b2] += 4;
            }
            //Don't forget to reset all structures as you go!
            int toCopy;
            int outPointer = s;
            for(int i = 0; i < 256; i++) {
                c = chunks[i];
                chunks[i] = null;
                endChunks[i] = null;
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


    private void doCounting(byte[] buffer, int s, int e) {
        for(int i = s+2; i < e; i+=4) {
            // System.out.println(Integer.toHexString(buffer[i]));
            // System.out.println(Integer.toHexString(buffer[i+1]));
            int pos = ((buffer[i] & 0xff) << 8) | (buffer[i+1] & 0xff);
            //System.out.println(Integer.toHexString(pos));
            //System.out.println(pos);
            if(pos < 0) {
                pos +=256;
            }
            // System.out.println(Integer.toHexString(pos));
            // System.out.println(pos);
            //Let's try this craaazy tree business, then..
            //Set the value in the tree...
            if(counts[pos] == -1) {
                counts[pos] = 1;
            } else {
                counts[pos]++;
            }
            //System.out.println(pos+":Goodies");
            while(pos != 0) {//TODO: Can be unrolled, methinks.
                pos--;
                pos >>>= 1;
                if(counts[pos] == 0) {
                    counts[pos]--; //Go up the tree setting the sentinel values as needed.
                    //System.out.println(pos+":Sentinel");
                } else {
                    //Woot! Civilisation!
                    break;
                }
            }
        }
    }


    private int writify(byte[] buffer, int i, int outPointer) {

        int by1 = i>>>8;
        int by2 = i&0b00000000000000000000000011111111;
        if(by1 >= 128) {
            fix2 = (byte) (by1-256);
        } else {
            fix2 = (byte) by1;
        }
        if(by2 >= 128) {
            fix3 = (byte) (by2-256);
        } else {
            fix3 = (byte) by2;
        }

        while(counts[i]> 0) {
            buffer[outPointer] = fix2;
            buffer[outPointer+1] = fix3;
            outPointer += 4;
            counts[i]--;
        }
        return outPointer;
    }



    private byte fix3;
    private byte fix2;
    public void sortTwo(byte[] buffer, int s, int e) {
        //assert(buffer[s] == buffer[e-3]);
        //assert(buffer[s+1] == buffer[e-2]);
        //assert(buffer[s+1] != buffer[e+2]);
        //s and e represent the first and last bytes of the complete
        //ints to sort, regardless of the actual sorting policy being used.
        //This is also counting-sort-able.
        //Perform counting sort against the final two bytes. Assumption is that all ints in range provided have the same
        //first and second byte, either via the action of sortOne or the MSByte phase.
        //int zeros
        doCounting(buffer, s, e);

        //Now consume counts to construct answer - remember to reset the counting structures as you go.
        int outPointer = s+2; //First byte to edit.
        //byte fix2;
        //byte fix3;

        int placesCount = 1; //Next free slot in placesToGo
        int placeWeAreAt = 0;
        //int by1;
        //int by2;
        int i;
        int iC = 0;
        while(placeWeAreAt < placesCount) {
            iC++;
            i = placesToGo[placeWeAreAt];
           // System.out.println("At:"+placeWeAreAt);
            placesToGo[placeWeAreAt] = 0;
            placeWeAreAt++;
            if(counts[i] > 0) {
                outPointer = writify(buffer, i, outPointer);
            }
            if(i < 32767) {//(Provided there ARE subtrees);
                //Traverse into any nonzero subtrees.
                //System.out.println("i:"+i);
                int childI = (i<<1)+1;
                if(counts[childI] != 0) {
                    placesToGo[placesCount] = childI;
                    //System.out.println("Should go left to "+childI);
                    placesCount++;
                }
                childI++;
                if(counts[childI] != 0) {
                    placesToGo[placesCount] = childI;
                    //System.out.println("Should go right to "+childI);
                    placesCount++;
                }
            }
            counts[i] = 0;
        }
        System.out.println("Iterations:"+iC);
        //Handle the special one
        while(counts[65535]> 0) {
            buffer[outPointer] = NEGONE;
            buffer[outPointer+1] = NEGONE;
            outPointer += 4;
            counts[65535]--;
        }
    }



    public final void repeatedlyRadixSort(byte[] buffer, int s, int e, int halt) {
        Chunk c;
        final int[] cc = chunkCounts;
        Chunk c2;
        byte times = 3;
        while(times != halt) {
            for(int i = s; i < e; i+= 4) { //Iterate across the offset'th byte....
                int b2;
                if(buffer[i+times] < 0) {
                    b2 = buffer[i+times]+256;
                } else {
                    b2 = buffer[i+times];
                }
                c = endChunks[b2];
                if(c == null) {
                    //No handling for running out of memory because that ought never to happen.
                    //Hopefully.
                    //Ha.
                    //assert(chunkCounter != numChunks);
                    chunks[b2] = chunkPool[chunkCounter];
                    endChunks[b2] = chunkPool[chunkCounter];
                    //assert(chunks[b2].previous == null);
                    chunkCounter++;
                    c = endChunks[b2];
                } else if(cc[b2] > Chunk.CHUNK_SIZE-4) {
                    //assert(chunkCounter != numChunks);
                    c2 = chunkPool[chunkCounter];
                    //assert(c2.previous == null);
                    chunkCounter++;
                    c.previous = c2;
                    endChunks[b2] = c2;
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
            //Don't forget to reset all structures as you go!
            int toCopy;
            int outPointer = s;
            for(int i = 0; i < 256; i++) {
                c = chunks[i];
                chunks[i] = null;
                endChunks[i] = null;
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
