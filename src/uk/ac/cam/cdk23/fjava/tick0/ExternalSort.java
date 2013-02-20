package uk.ac.cam.cdk23.fjava.tick0;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

public final class ExternalSort {
    //Tunables
    public static final int HUGE_BUFFER_SIZE = 7004000;
    public static final int HUGE_BUFFER_DIV_2 = HUGE_BUFFER_SIZE >>> 1;

    private static final int MAX_IN_MEM_INTRO_SORTABLE = 30000;   //TODO: TUNABLE
    private static final int MAX_IN_MEM_RADIX_SORTABLE = 5000000; //TODO: TUNABLE

    private static RandomAccessFile inputFile;
    private static RandomAccessFile swapFile;
    private static LinkedList[] placesToResort;
    private static byte[] howToResortThere;
    //Buffers
    private static byte[] buffer;
    private static int size; //Size of input;
    private static Array chunks;
    private static int higherOrderFound = 1;

    private static final void sort(String f1, String f2) throws IOException {
        //Create the various IO objects...
        RandomAccessFile inFile = new RandomAccessFile(f1, "rw");
        int siz = (int) inFile.length();
        if(siz < 8) {
            return;
        }
        //System.out.println("Input size: "+size);
        if(siz <= MAX_IN_MEM_INTRO_SORTABLE) {
            //Sort in memory
            byte[] giantBuffer = new byte[siz];
            inFile.read(giantBuffer);
            if(siz <= 168) {
                SmoothSort.insertSort(giantBuffer, 0, siz - 1);
            } else {
                SmoothSort.smoothSort(giantBuffer, 0, siz - 1);
            }
            inFile.seek(0);
            inFile.write(giantBuffer, 0, siz);
            inFile.close();
        } else if(siz <= MAX_IN_MEM_RADIX_SORTABLE) {
            //Sort in memory in a more amusing manner
            byte[] giantBuffer = new byte[siz];
            inFile.read(giantBuffer);
            MemoryRadixSorter radixSorter = new MemoryRadixSorter();
            int numChunks = (siz/(Chunk.CHUNK_SIZE))+256;
            Chunk[] chunkPool = new Chunk[numChunks];
            for(int i = 0; i < numChunks; i++) {
                chunkPool[i] = new Chunk();
            }
            radixSorter.chunkPool = chunkPool;
            radixSorter.sortFour(giantBuffer, 0, siz - 1);
            inFile.seek(0);
            inFile.write(giantBuffer, 0, siz);
            inFile.close();
        } else {
            inputFile = inFile;
            size = siz;
            chunks = new Array();
            //Begin count phase.
            buffer = new byte[HUGE_BUFFER_SIZE];

            int[][] moreInts = new int[256][256];
            boolean notVictoryByMistake = countPhase(moreInts);
            inFile.seek(0);
            if(!notVictoryByMistake) {
                iAccidentallyYourData(buffer);
                return;
            }
            swapFile = new RandomAccessFile(f2, "rw");

            MemoryRadixSorterFlag radixSorter = new MemoryRadixSorterFlag();
            radixSorter.slightlyFancierCount = moreInts;

            MSBytePhase(radixSorter, higherOrderFound);

            inputFile.seek(0);
            swapFile.seek(0);

            //Swap read time!
            swapReadPhase(radixSorter);

            inputFile.close();
            swapFile.close();
            //Job done.
        }
    }

    private static final boolean countPhase(int[][] moreInts) throws IOException {//One pass through file counting first
        //two bytes and constructing plan of attack for rest of sort. If it all goes wrong, do a second pass counting
        //last two bytes for any giant spans found.
        //Return true if anything resolved before the fourth order.
        //In the case of false, we've performed a counting sort on the data by accident, so can skip most of
        //the rest of the algorithm.
        //long s = System.nanoTime();
        int[] ints = new int[256];
        byte[] buf = buffer;
        short furtherCountingCount = 0;
        //Stores first and second byte of everything that needs to be counted more than twice.
        int bytesRead = 0;
        FurtherCount[] needFurtherCounting = null;
        int sz = size;
        while(bytesRead < sz) {
            int bufferFull = inputFile.read(buf);
            bytesRead += bufferFull;
            //Do the counting...
            for(int i = bufferFull-4; i >= 0; i -= 4) {
                final int thisIndex = buf[i] + 128;
                ints[thisIndex]++;
                moreInts[thisIndex][buf[i+1]+128]++;
            }
        }
        System.out.flush(); //You really, really don't want to know.
        //Now need to look at results of count and determine if we need to do a second run. Hopefully we don't.
        byte[] ro = chunks.radixOrders;
        int[] rs = chunks.radixStarts;
        Array[] cs = chunks.higherChunks;
        int swapLocation = 0;
        for (int i = 0; i < 256; i++) {
            if(ints[i] == 0) {
                continue;
            }
            rs[i] = swapLocation;
            if(ints[i]<<2 <= HUGE_BUFFER_DIV_2) {
                //It fits!
                //Need to record somehow the divisions that will end up in the swapfile.
                //Also need info about how to perform the radix sort - in some cases it'll have to go by
                //more than just one byte..
                ro[i] = 1;
                swapLocation += ints[i]<<2;
            } else {
                //Oh dear!
                //Process second byte counts and see if things get any more hopeful...
                ro[i] = 2;
                if(cs == null) {
                    cs = new Array[256];
                    chunks.higherChunks = cs;
                }
                Array entry = new Array();
                //Indexed by parent byte.
                cs[i] = entry;
                int jj = 0;
                for(int j = 128; j < 256; j++) {
                    int co = moreInts[i][j];
                    if(co != 0) {
                        entry.radixStarts[jj] = swapLocation;
                        if(co<<2 <= HUGE_BUFFER_DIV_2) {
                            //It fits! (At second order)
                            if(higherOrderFound == 1) {
                                higherOrderFound++;
                            }
                            entry.radixOrders[jj] = 1; //Yay!
                            //And the values themselves will end up in endChunks here (The last two bytes).
                            swapLocation += co<<2;
                        } else {
                            if(entry.higherChunks == null) {
                                entry.higherChunks = new Array[256];
                            }
                            //System.out.println("No good - going to have to count again...");
                            entry.radixOrders[jj] = 2;
                            FurtherCount targ = new FurtherCount();
                            targ.swapLocation = swapLocation;
                            targ.b1 = i;
                            targ.b2 = jj;
                            targ.mb2 = j;
                            for(int t = 0; t < 256; t++) {
                                targ.counts[t] = new CountingUnit();
                            }
                            if(needFurtherCounting == null) {
                                needFurtherCounting = new FurtherCount[256]; //Hopefully never used!
                            }
                            needFurtherCounting[furtherCountingCount] = targ;
                            furtherCountingCount++;
                            swapLocation += co<<2; //Allocate space for it all at level 3. Compact as required.
                        }
                    }
                    jj++;
                }
                for(int j = 0; j < 128; j++) {
                    int co = moreInts[i][j];
                    if(co != 0) {
                        entry.radixStarts[jj] = swapLocation;
                        if(co<<2 <= HUGE_BUFFER_DIV_2) {
                            //It fits! (At second order)
                            if(higherOrderFound == 1) {
                                higherOrderFound++;
                            }
                            entry.radixOrders[jj] = 1; //Yay!
                            //And the values themselves will end up in endChunks here (The last two bytes).
                            swapLocation += co<<2;
                        } else {
                            if(entry.higherChunks == null) {
                                entry.higherChunks = new Array[256];
                            }
                            //System.out.println("No good - going to have to count again...");
                            entry.radixOrders[jj] = 2;
                            FurtherCount targ = new FurtherCount();
                            targ.swapLocation = swapLocation;
                            targ.b1 = i;
                            targ.b2 = jj;
                            targ.mb2 = j;
                            for(int t = 0; t < 256; t++) {
                                targ.counts[t] = new CountingUnit();
                            }
                            if(needFurtherCounting == null) {
                                needFurtherCounting = new FurtherCount[256]; //Hopefully never used!
                            }
                            needFurtherCounting[furtherCountingCount] = targ;
                            furtherCountingCount++;
                            swapLocation += co<<2;
                        }
                    }
                    jj++;
                }
            }
        }
        //e = System.nanoTime();
        //System.out.println("Struct in "+(e-s)/1000000+"ms");
        return furtherCountingCount == 0 || secondaryCountPhase(needFurtherCounting, furtherCountingCount);
    }
    private static int resortCount;
    private static final boolean secondaryCountPhase(FurtherCount[] targets, short numTargets) throws IOException {
        byte[] buf = buffer;
        boolean ret = false;
        resortCount = 0;
        int howCount = 0;
        placesToResort = new LinkedList[10];
        byte[] htr = new byte[30];
        howToResortThere = htr;
        //Reset input stream and have the reading thread do a repeat pass.
        inputFile.seek(0);

        int bytesRead = 0;
        FurtherCount fc;
        while(bytesRead < size) {
            int bufferFull = inputFile.read(buffer);
            bytesRead += bufferFull;
            //System.out.println("bytesRead:"+bytesRead);
            //Do the counting...
            for(int i = bufferFull-4; i >= 0; i -= 4) {
                int b1 = buf[i] + 128;
                int b2 = buf[i+1]+128;
                for(int j = 0; j < numTargets; j++) {
                    fc = targets[j];
                    if(fc.b1 == b1 && fc.mb2 == b2) {
                        //Hit
                        //Populate the count arrays...
                        int b3 = buf[i+2]+128;
                        //NOTE: BENDY NOW!
                        fc.counts[b3].count++;
                        fc.counts[b3].a[buf[i+3]+128]++;
                        break;
                    }
                }
            }
        }
        System.out.flush(); //Please don't ask.
        //Analyse results - need to construct the appropriate tomfoolery in chunks.
        //System.out.println("second count scan");

        //TODO: Some way of counting before we get to the radix sorting phase?

        CountingUnit c;
        //Array deepEntry;
        Array thisEntry;
        for(int i = 0; i < numTargets; i++) {
            fc = targets[i];
            int jj = 0;
            for(int j = 128; j < 256; j++) {
                c = fc.counts[j];
                if(c == null || c.count == 0) {
                    continue;
                }
                int b1 = fc.b1;
                int b2 = fc.b2;
                thisEntry = chunks.higherChunks[b1].higherChunks[b2];
                if(thisEntry == null) {
                    thisEntry = new Array();
                    chunks.higherChunks[b1].higherChunks[b2] = thisEntry;
                }
                if(c.count<<2 <= HUGE_BUFFER_DIV_2) {
                    if(higherOrderFound <= 2) {
                        higherOrderFound = 3;
                    }
                    thisEntry.radixOrders[jj] = 1;
                    thisEntry.radixStarts[jj] = fc.swapLocation;
                    fc.swapLocation += c.count;
                    ret = true;
                } else {
                    thisEntry.radixOrders[jj] = 2;
                    if(higherOrderFound <= 3) {
                        higherOrderFound = 4;
                    }
                    if(thisEntry.lastResort == null) {
                        thisEntry.lastResort = new LinkedList<>();
                      //  thisEntry.lastResortHadness = new LinkedList<>();
                        placesToResort[resortCount] = thisEntry.lastResort;
                        //System.out.println("resortCount:"+resortCount);
                        resortCount++;
                    }
                    htr[howCount++] = (byte) (b1-128);
                    if(b2 >= 128) {
                        htr[howCount++] = (byte)(b2-256);
                    } else {
                        htr[howCount++] = (byte) b2;
                    }
                    if(b2 >= 128) {
                        htr[howCount++] = (byte)(jj-256);
                    } else {
                        htr[howCount++] = (byte) jj;
                    }
                    thisEntry.lastResort.offer(c.a);
                }
                jj++;
            }
            for(int j = 0; j < 128; j++) {
                c = fc.counts[j];
                if(c == null || c.count == 0) {
                    continue;
                }
                int b1 = fc.b1;
                int b2 = fc.b2;
                thisEntry = chunks.higherChunks[b1].higherChunks[b2];
                if(thisEntry == null) {
                    thisEntry = new Array();
                    chunks.higherChunks[b1].higherChunks[b2] = thisEntry;
                }
                if(c.count<<2 <= HUGE_BUFFER_DIV_2) {
                    if(higherOrderFound <= 2) {
                        higherOrderFound = 3;
                    }
                    System.out.println("Resolved at level 3!");
                    thisEntry.radixOrders[jj] = 1;
                    thisEntry.radixStarts[jj] = fc.swapLocation;
                    fc.swapLocation += c.count;
                    ret = true;
                } else {
                    thisEntry.radixOrders[jj] = 2;
                    if(higherOrderFound <= 3) {
                        higherOrderFound = 4;
                    }
                    if(thisEntry.lastResort == null) {
                        thisEntry.lastResort = new LinkedList<>();
                        placesToResort[resortCount] = thisEntry.lastResort;
                        resortCount++;
                    }
                    htr[howCount++] = (byte) (b1-128);
                    if(b2 >= 128) {
                        htr[howCount++] = (byte)(b2-256);
                    } else {
                        htr[howCount++] = (byte) b2;
                    }
                    if(b2 >= 128) {
                        htr[howCount++] = (byte)(j-256);
                    } else {
                        htr[howCount++] = (byte) j;
                    }
                    thisEntry.lastResort.offer(c.a);
                }
                jj++;
            }
        }
        System.out.flush();
        return ret;
    }

    private static void MSBytePhase(MemoryRadixSorterFlag radixSorter, int highest) throws IOException {//Process the entire file radix sorting by first byte, or, depending on what was
        //found in the counting phase, doing something somewhat different. When memory is full make swap writing
        //do it's thing. When input depleted, return.
        int bytesRead = 0;
        int bufferFull;
        int sz = size;
        if(highest == 1) {//Simple! We can just cheat and forget the chunks structure entirely.
            while(bytesRead < sz) {
                bufferFull = inputFile.read(buffer, 0, HUGE_BUFFER_DIV_2); //Now we are always out of memory.
                bytesRead += bufferFull;
                swapWriteFirst(radixSorter.veryUpperRadixSort(buffer, bufferFull-1), bufferFull-1);
            }
        } else if(highest == 2) {
            //Well, it was worth a try...
            while(bytesRead < sz) {
                bufferFull = inputFile.read(buffer, 0, HUGE_BUFFER_DIV_2); //Now we are always out of memory.
                bytesRead += bufferFull;
                //Loaded a chunk - now MSB radix sort it just as much as we need to...
                radixSorter.veryMostlySomewhatLazyUpperRadixSort(buffer, bufferFull-1, chunks);
                swapWrite();
            }
        } else {
            //Feh..
            while(bytesRead < sz) {
                bufferFull = inputFile.read(buffer, 0, HUGE_BUFFER_DIV_2); //Now we are always out of memory.
                bytesRead += bufferFull;
                //Loaded a chunk - now MSB radix sort it just as much as we need to...
                radixSorter.veryMostlyNotQuiteSoLazyUpperRadixSort(buffer, bufferFull - 1, chunks);
                swapWrite();
            }
        }
    }

    private static void swapWriteFirst(int[] bufferCounts, int bufferEnd) throws IOException {//Shortcut for swapfile
        //writing of first order.
        for(int i = 0; i < 255; i++) {//Handle last one specially
            if(bufferCounts[i] == bufferCounts[i+1]) {
                continue;
            }
            swapFile.seek(chunks.radixStarts[i]);
            swapFile.write(buffer, bufferCounts[i], bufferCounts[i+1]-bufferCounts[i]);
            chunks.radixStarts[i] += bufferCounts[i+1]-bufferCounts[i];
        }
        //System.out.println(bufferCounts[255]);
        if(bufferCounts[255] != bufferEnd+1) {
            swapFile.seek(chunks.radixStarts[255]); //First free byte.
            swapFile.write(buffer, bufferCounts[255], bufferEnd+1-bufferCounts[255]);
            chunks.radixStarts[255] += bufferEnd+1-bufferCounts[255];
        }
    }

    private static void swapWrite() throws IOException {//Write to swapfile, flush buffers.
        //This one supports higher order inputs, but is slower.
        //Appropriate count values stored in chunks.
        Array cs = chunks;
        int[] bufferCounts = cs.counts;
        Array entry;

        int firstByte = 0;
        for(int i = 0; i < 256; i++) { //Last one needs special treatment.
            //Count at i now holds the index of the first byte of the radix corresponding to i+1.
            if(cs.radixOrders[i] == 0) {
                continue;
            }
            if(cs.radixOrders[i] == 1) {
                swapFile.seek(cs.radixStarts[i]);
                //Index of our first byte is firstByte (initially zero).
                //Last byte is bufferCounts[i]-1
                swapFile.write(buffer, firstByte, bufferCounts[i]-firstByte);
                cs.radixStarts[i] += bufferCounts[i]-firstByte;
                firstByte = bufferCounts[i];
            } else {
                //Second order sadness.
                entry = cs.higherChunks[i];
                int[] entryCounts = entry.counts;
                for(int j = 0; j < 256; j++) {
                    if(entry.radixOrders[j] == 0) {
                        continue;
                    }
                    if(entry.radixOrders[j] == 1) {
                        //Do second order writing...
                        swapFile.seek(entry.radixStarts[j]);
                        swapFile.write(buffer, firstByte, entryCounts[j]-firstByte);
                        entry.radixStarts[j] += entryCounts[j]-firstByte;
                        firstByte = entryCounts[j];
                    } else {
                        Array deepEntry = entry.higherChunks[j];
                        int[] deepEntryCounts = deepEntry.counts;
                        for(int k = 0; k < 256; k++) {
                            if(entry.radixOrders[k] == 0) {
                                continue;
                            }
                            if(entry.radixOrders[k] == 1) {
                                //Do third order writing... (sigh)
                                swapFile.seek(entry.radixStarts[j]);
                                swapFile.write(buffer, firstByte, deepEntryCounts[k]-firstByte);
                                entry.radixStarts[k] += deepEntryCounts[k]-firstByte;
                                firstByte = deepEntryCounts[k];
                            } // else {Nothing to do! We counted it all.}
                        }
                    }
                    entryCounts[j] = 0;
                }
            }
            bufferCounts[i] = 0; //Perhaps reset with a memcpy?
        }
    }

    private static void swapReadPhase(MemoryRadixSorterFlag radixSorter) throws IOException {//Read the swapfile as per the plan (Or rather, the reading thread will). This
        //function has to process the blocks loaded and put them, sorted, into the giant buffer, before finally getting
        //that flushed to disk.
        //System.out.println("Starting swap read phase...");
       // SmoothSort introSorter = new SmoothSort();
        //Build a LinkedList expressing the sequence of reads needed, then follow it.

        //radixSorter.mirror = mirror;

        //numChunks = (short) ((runtime.freeMemory()*0.9) / (Chunk.CHUNK_SIZE));

        /*System.out.println("Populating pool with "+SWAP_READ_CHUNKS+" chunks");
        chunkPool = new Chunk[SWAP_READ_CHUNKS];
        for(int i = 0; i < SWAP_READ_CHUNKS; i++) {
            chunkPool[i] = new Chunk();
        }*/
        //radixSorter.chunkPool = chunkPool;
        //chunkCounter = 0;

        LinkedList<ReadEntry> thingsToDo = new LinkedList<>();

        //But wait! Multiple fixer combos per load. Perhaps one entry per load would follow.

        ReadEntry entry = new ReadEntry();
        entry.readStart = 0;
        int bytesAvailable = HUGE_BUFFER_DIV_2;
        int previousRadixStartEntry = 0;
        for(int i = 0; i < 256; i++) {
            if(chunks.radixOrders[i] == 0) {
                continue;
            }
            if(chunks.radixOrders[i] == 1) {
                //Seems mad, but remember how the swap write phase actually moved all of these up to the next radix?
                final int radixLength = chunks.radixStarts[i]-previousRadixStartEntry;
                FixerCombo f = new FixerCombo();
                f.order = 1;
                f.firstIndex = previousRadixStartEntry;
                if(radixLength < bytesAvailable) {
                    entry.readLength += radixLength;
                    entry.fixersHere.offer(f);
                    bytesAvailable -= radixLength;
                } else {
                    //System.out.println("Full with "+bytesAvailable+" to spare.");
                    thingsToDo.offer(entry);
                    entry = new ReadEntry();
                    entry.readStart = previousRadixStartEntry;
                    entry.readLength = radixLength;
                    entry.fixersHere.offer(f);
                    bytesAvailable = HUGE_BUFFER_DIV_2-radixLength;
                }
                previousRadixStartEntry = chunks.radixStarts[i];
            } else {
                Array aEntry = chunks.higherChunks[i];
                for(int j = 0; j < 256; j++) {
                    if(aEntry.radixOrders[j] == 0) {
                        continue;
                    }
                    if(aEntry.radixOrders[j] == 1) {
                        //Resolves here, thankfully.
                        final int radixLength = aEntry.radixStarts[j]-previousRadixStartEntry;//Read length;
                        FixerCombo f = new FixerCombo();
                        f.order = 2;
                        f.firstIndex = previousRadixStartEntry;
                        if(radixLength < bytesAvailable) {
                            entry.readLength += radixLength;
                            entry.fixersHere.offer(f);
                            bytesAvailable -= radixLength;
                        } else {
                            //System.out.println("Full with "+bytesAvailable+" to spare.");
                            thingsToDo.offer(entry);
                            entry = new ReadEntry();
                            entry.readStart = previousRadixStartEntry;
                            entry.readLength = radixLength;
                            entry.fixersHere.offer(f);
                            bytesAvailable = HUGE_BUFFER_DIV_2-radixLength;
                        }
                        previousRadixStartEntry = aEntry.radixStarts[j];
                    } else {
                        Array deepEntry = chunks.higherChunks[i];
                        for(int k = 0; k < 256; k++) {
                            if(deepEntry.radixOrders[k] == 0) {
                                continue;
                            }
                            if(deepEntry.radixOrders[k] == 1) {
                                //Resolves level 3. Yay...
                                final int radixLength = deepEntry.radixStarts[k]-previousRadixStartEntry;//Read length;
                                FixerCombo f = new FixerCombo();
                                f.order = 3;
                                f.firstIndex = previousRadixStartEntry;
                                if(radixLength < bytesAvailable) {
                                    entry.readLength += radixLength;
                                    entry.fixersHere.offer(f);
                                    bytesAvailable -= radixLength;
                                } else {
                                    thingsToDo.offer(entry);
                                    entry = new ReadEntry();
                                    entry.readStart = previousRadixStartEntry;
                                    entry.readLength = radixLength;
                                    entry.fixersHere.offer(f);
                                    bytesAvailable = HUGE_BUFFER_DIV_2-radixLength;
                                }
                                previousRadixStartEntry = deepEntry.radixStarts[k];
                            } else {
                                //AAA! A fourth order one!
                                //Tack the counts on and hope for the best..
                                FixerCombo f = new FixerCombo();
                                f.order = 4;
                                f.firstIndex = previousRadixStartEntry;
                                f.fix1 = (byte) (i-128);
                                if(j > 128) {
                                    f.fix2 = (byte) (j-256);
                                } else {
                                    f.fix2 = (byte) j;
                                }
                                if(k > 128) {
                                    f.fix3 = (byte) (k-256);
                                } else {
                                    f.fix3 = (byte) k;
                                }
                                f.counts = deepEntry.lastResort.poll();
                                entry.fixersHere.offer(f);
                            }
                        }
                    }
                }
            }
        }
        thingsToDo.offer(entry);


        int fixerStart;
        int fixerEnd;

        int bytesRead = 0;

        while(!thingsToDo.isEmpty()) {
            entry = thingsToDo.poll();
            swapFile.read(buffer, 0, entry.readLength);
            FixerCombo fixer = entry.fixersHere.poll(); //Indicies are relative to file, not array!
            //fixerStart = fixer.firstIndex-bytesRead; //(That's zero to you);
            FixerCombo nextFixer = entry.fixersHere.poll();
            if(nextFixer != null) {
                fixerEnd = (nextFixer.firstIndex-bytesRead)-1;
            } else {
                fixerEnd = entry.readLength-1; //Really.
            }

            while(fixer != null) {
                fixerStart = fixer.firstIndex-bytesRead;

                if(fixer.order == 1) {
                    radixSorter.doubleRadixSort(buffer, fixerStart, fixerEnd);

                    radixSorter.upperRadixsort(buffer, fixerStart, fixerEnd);
                } else if(fixer.order == 2) {
                    radixSorter.exclusiveDoubleRadixSort(buffer, fixerStart, fixerEnd);
                } else if(fixer.order == 3) {
                    radixSorter.repeatedlyRadixSort(buffer, fixerStart, fixerEnd, 2);
                } else {
                    make_snafucated(fixer, inputFile);
                }

                fixer = nextFixer;
                nextFixer = entry.fixersHere.poll();
                if(nextFixer != null) {
                    fixerEnd = (nextFixer.firstIndex-bytesRead)-1;
                } else {
                    fixerEnd = entry.readLength-1;
                }
            }
            bytesRead += entry.readLength;
            inputFile.write(buffer, 0, entry.readLength);
        }
        System.out.flush();
    }

    //
    //Special cases:
    //
    //We counted it all. Whoops.
    private static void iAccidentallyYourData(byte[] buffer) throws IOException {
        LinkedList<int[]> lastResort;
        int giantCounter = 0;
        int[] resort;
        int byteCount = 0;
        //System.out.println(Arrays.toString(howToResortThere));
        //System.out.println(Arrays.toString(placesToResort));
        for(int i = 0; i < resortCount; i++) {
            lastResort = placesToResort[i];
            final byte byte1 = howToResortThere[byteCount];
            final byte byte2 = howToResortThere[byteCount+1];
            resort = lastResort.poll();
            System.out.flush();
            while(resort != null) {
                final byte byte3 = howToResortThere[byteCount+2];
                byteCount+=3;
                for(int l = 128; l < 256; l++) {
                    if(resort[l] == 0) {
                        continue;
                    }
                    int p = resort[l];
                    while(p > 0) {
                        int bytesConsumed = p<<2;
                        int end;
                        if(giantCounter + bytesConsumed >= HUGE_BUFFER_SIZE) {
                            end = HUGE_BUFFER_SIZE-giantCounter;
                        } else {
                            end = resort[l];
                        }
                        p -= end;
                        while(end > 0) {
                            buffer[giantCounter] = byte1;
                            buffer[giantCounter+1] = byte2;
                            buffer[giantCounter+2] = byte3;
                            buffer[giantCounter+3] = (byte) (l-128);
                            giantCounter += 4;
                            end--;
                        }
                        inputFile.write(buffer, 0, giantCounter);
                        giantCounter = 0;
                    }
                }
                for(int l = 0; l < 128; l++) {
                    if(resort[l] == 0) {
                        continue;
                    }
                    int p = resort[l];
                    while(p > 0) {
                        int bytesConsumed = p<<2;
                        int end;
                        if(giantCounter + bytesConsumed >= HUGE_BUFFER_SIZE) {
                            end = HUGE_BUFFER_SIZE-giantCounter;
                        } else {
                            end = resort[l];
                        }
                        p -= end;
                        while(end > 0) {
                            buffer[giantCounter] = byte1;
                            buffer[giantCounter+1] = byte2;
                            buffer[giantCounter+2] = byte3;
                            buffer[giantCounter+3] = (byte) (l-128);
                            giantCounter += 4;
                            end--;
                        }
                        inputFile.write(buffer, 0, giantCounter);
                        giantCounter = 0;
                    }
                }
                resort = lastResort.poll();
            }
        }
        inputFile.write(buffer, 0, giantCounter);
        inputFile.close();
    }


    private static String byteToHex(byte b) {
        String r = Integer.toHexString(b);
        if (r.length() == 8) {
            return r.substring(6);
        }
        return r;
    }
    private static String checkSum(String f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream ds = new DigestInputStream(
                    new FileInputStream(f), md);
            byte[] b = new byte[512];
            while (ds.read(b) != -1);
            String computed = "";
            for (byte v : md.digest()) {
                computed += byteToHex(v);
            }

            return computed;
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return "<error computing checksum>";
    }
    public static void main(String[] args) throws Exception {
        String f1 = args[0];
        String f2 = args[1];
        long noww = System.nanoTime();
        sort(f1, f2);
        long done = System.nanoTime()-noww;
        System.out.println("Entire shaboodle took "+done/1000000+"ms");
        System.out.println("The checksum is: " + checkSum(f1));
    }

    //Evil fourth order stuff (Grumblemumble)
    public static void make_snafucated(FixerCombo fixer, RandomAccessFile file) throws IOException {
        for(int i = 0; i < 256; i++) {
            while(fixer.counts[i] > 0) {
                file.writeByte(fixer.fix1);
                file.writeByte(fixer.fix2);
                file.writeByte(fixer.fix3);
                if(i > 128) {
                    file.writeByte(i-256);
                } else {
                    file.writeByte(i);
                }
            }
        }
    }
}
