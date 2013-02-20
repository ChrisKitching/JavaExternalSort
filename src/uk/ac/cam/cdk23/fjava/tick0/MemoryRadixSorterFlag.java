package uk.ac.cam.cdk23.fjava.tick0;

public final class MemoryRadixSorterFlag {
    private int[][] counts = new int[4][256];
    public int[][] slightlyFancierCount;
    private final int[] blanks = new int[256];
    private static final int MIRROR_START = ExternalSort.HUGE_BUFFER_SIZE >>>1;

    public void exclusiveDoubleRadixSort(byte[] buffer, int s, int e) {
        //LSB radix sort the bottom two bytes using two bytes as the radix.
        int[][] sfcs = slightlyFancierCount;
        for(int i = 0; i < 256; i++) {
            System.arraycopy(blanks, 0, sfcs[i], 0, 256);
        }
        for(int i = s; i < e; i+=4) {
            sfcs[buffer[i+2]+128][buffer[i+3]+128]++;
        }
        int runningTotal = e+1;
        for(int b = 127; b >= 0; b--) {
            for(int i = 127; i >= 0; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
            for(int i = 255; i >= 128; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
        }
        for(int b = 255; b >= 128; b--) {
            for(int i = 127; i >= 0; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
            for(int i = 255; i >= 128; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
        }

        System.arraycopy(buffer, s, buffer, MIRROR_START, e-s+1);

        //Write everything back to buffer...
        final int mirrorEnd = MIRROR_START+e-s+1;
        for(int i = MIRROR_START; i < mirrorEnd; i+=4) {
            int b2 = buffer[i+2]+128;
            int b3 = buffer[i+3]+128;
            int ind = sfcs[b2][b3];
            buffer[ind] = buffer[i];
            buffer[ind+1] = buffer[i+1];
            buffer[ind+2] = buffer[i+2];
            buffer[ind+3] = buffer[i+3];
            sfcs[b2][b3] += 4;
        }
    }

    public void doubleRadixSort(byte[] buffer, int s, int e) {
        //LSB radix sort the bottom two bytes using two bytes as the radix.
        //long sst = System.nanoTime();
        int[][] sfcs = slightlyFancierCount;
        for(int i = 0; i < 256; i++) {
            System.arraycopy(blanks, 0, sfcs[i], 0, 256);
        }
        int[][] cs = counts;
        for(int i = s; i < e; i+=4) {
            sfcs[buffer[i+2]+128][buffer[i+3]+128]++;
            cs[1][buffer[i+1]+128]++;
        }


        //long eet = System.nanoTime();
        //total1 += (eet-sst);
        //sst = System.nanoTime();
        int runningTotal = e+1;
        int runningTotal2 = e+1;
        for(int b = 127; b >= 0; b--) {
            runningTotal2 -=  cs[1][b]<<2;
            cs[1][b] = runningTotal2;
            for(int i = 127; i >= 0; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
            for(int i = 255; i >= 128; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
        }
        for(int b = 255; b >= 128; b--) {
            runningTotal2 -=  cs[1][b]<<2;
            cs[1][b] = runningTotal2;
            for(int i = 127; i >= 0; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
            for(int i = 255; i >= 128; i--) {
                runningTotal -=  sfcs[b][i]<<2;
                sfcs[b][i] = runningTotal;
            }
        }
        //eet = System.nanoTime();
        //total2 += (eet-sst);
        //sst = System.nanoTime();
        System.arraycopy(buffer, s, buffer, MIRROR_START, e-s+1);


        //Write everything back to buffer...
        final int mirrorEnd = MIRROR_START+e-s+1;
        for(int i = MIRROR_START; i < mirrorEnd;) {
            final int b2 = buffer[i+2]+128;
            final int b3 = buffer[i+3]+128;
            final int ind = sfcs[b2][b3];
            buffer[ind] = buffer[i];
            buffer[ind+1] = buffer[i+1];
            buffer[ind+2] = buffer[i+2];
            buffer[ind+3] = buffer[i+3];
            sfcs[b2][b3] += 4;
            i+=4;
        }
        //eet = System.nanoTime();
        //total3 += (eet-sst);
    }

    public void upperRadixsort(byte[] buffer, int s, int e) {
        int[][] cs = counts;
        System.arraycopy(buffer, s, buffer, MIRROR_START, e-s+1);

        int[] cnts = cs[1];
        final int mirrorEnd = MIRROR_START+e-s+1;
        for(int i = MIRROR_START; i < mirrorEnd;) {
            int b2 = buffer[i+1]+128;
            int ind = cnts[b2];
            buffer[ind] = buffer[i++];
            buffer[ind+1] = buffer[i++];
            buffer[ind+2] = buffer[i++];
            buffer[ind+3] = buffer[i++];
            cnts[b2] += 4;
        }
        System.arraycopy(blanks, 0, cs[1], 0, 256);
    }

    //TODO: Fiddle with sorting two bytes at once MSB style.

    public int[] veryUpperRadixSort(byte[] buffer, int e) { //MSB sort by first byte. Only useful for strictly
        //first order inputs.
        int[] cnts = counts[0];
        System.arraycopy(blanks, 0, cnts, 0, 256);
        for(int i = 0; i < e; i+=4) {
            cnts[buffer[i]+128]++;
        }
        int runningTotal = e+1;
        for(int b = 255; b > 0; b--) {
            runningTotal -=  cnts[b]<<2;
            cnts[b] = runningTotal;
        }
        cnts[0] = 0;
        int[] cntsr = new int[256];
        System.arraycopy(cnts, 0, cntsr, 0, 256);


        System.arraycopy(buffer, 0, buffer, MIRROR_START, e +1);
        System.out.flush();

        final int mirrorEnd = MIRROR_START+e +1;
        for(int i = MIRROR_START; i < mirrorEnd;) {
            int b2 = buffer[i]+128;
            int ind = cnts[b2];
            buffer[ind] = buffer[i++];
            buffer[ind+1] = buffer[i++];
            buffer[ind+2] = buffer[i++];
            buffer[ind+3] = buffer[i++];
            cnts[b2] += 4;
        }
        return cntsr;
    }

    public void veryMostlySomewhatLazyUpperRadixSort(byte[] buffer, int e, Array chunks) { //chunks-aware MSB radix sort.
        System.arraycopy(buffer, 0, buffer, MIRROR_START, e +1);
        Array entry;
        int[] cnts = chunks.counts;
        final int mirrorEnd = MIRROR_START+e;
        for(int i = MIRROR_START; i < mirrorEnd; i+=4) {
            int b1 = buffer[i] + 128;
            if(chunks.radixOrders[b1] == 1) {
                cnts[b1]++;
            } else {
                int b2;
                if(buffer[i+1] < 0) {
                    b2 = buffer[i+1]+256;
                } else {
                    b2 = buffer[i+1];
                }
                chunks.higherChunks[b1].counts[b2]++;
            }
        }
        int runningTotal = e+1;
        //Transform counts into indicies of first byte of corresponding stuff in the buffer.
        for(int b = 255; b >= 0; b--) {
            if(chunks.radixOrders[b] == 0) {
                continue;
            }
            if(chunks.radixOrders[b] == 1) {
                runningTotal -=  cnts[b]<<2;
                cnts[b] = runningTotal;
            } else {
                Array cch = chunks.higherChunks[b];
                for(int c = 255; c >= 0; c--) {
                    if(cch.radixOrders[c] == 0) {
                        continue;
                    }
                    if(cch.radixOrders[c] == 1) {
                        runningTotal -=  cch.counts[c]<<2;
                        cch.counts[c] = runningTotal;
                    }
                }
                cnts[b] = runningTotal;
            }
        }
        for(int i = MIRROR_START; i < mirrorEnd;) {
            int b1 = buffer[i] + 128;
            if(chunks.radixOrders[b1] == 1) {
                int ind = cnts[b1]; //First index to write this stuff.
                buffer[ind] = buffer[i++];
                buffer[ind+1] = buffer[i++];
                buffer[ind+2] = buffer[i++];
                buffer[ind+3] = buffer[i++];
                cnts[b1] += 4;
            } else {
                int b2;
                if(buffer[i+1] < 0) {
                    b2 = buffer[i+1]+256;
                } else {
                    b2 = buffer[i+1];
                }
                entry = chunks.higherChunks[b1];
                if(entry.radixOrders[b2] == 1) {
                    int ind = entry.counts[b2];
                    buffer[ind] = buffer[i++];
                    buffer[ind+1] = buffer[i++];
                    buffer[ind+2] = buffer[i++];
                    buffer[ind+3] = buffer[i++];
                    entry.counts[b2] += 4;
                }
            }
        }
    }
    public void veryMostlyNotQuiteSoLazyUpperRadixSort(byte[] buffer, int e, Array chunks) { //chunks-aware MSB radix sort.
        //This one doesn't explode when faced with things that are 3rd or 4th order, but is slower.
        System.arraycopy(buffer, 0, buffer, MIRROR_START, e +1);
        Array entry;
        int[] cnts = chunks.counts;
        final int mirrorEnd = MIRROR_START+e;
        Array upEnt;
        for(int i = MIRROR_START; i < mirrorEnd; i+=4) {
            int b1 = buffer[i] + 128;
            if(chunks.radixOrders[b1] == 1) {
                cnts[b1]++;
            } else {
                int b2;
                if(buffer[i+1] < 0) {
                    b2 = buffer[i+1]+256;
                } else {
                    b2 = buffer[i+1];
                }
                upEnt = chunks.higherChunks[b1];
                if(upEnt.radixOrders[b2] == 1) {
                    upEnt.counts[b2]++;
                } else {
                    int b3;
                    if(buffer[i+2] < 0) {
                        b3 = buffer[i+2]+256;
                    } else {
                        b3 = buffer[i+2];
                    }
                    Array upUpEnt = upEnt.higherChunks[b2];
                    if(upUpEnt.radixOrders[b2] == 1) {
                        upUpEnt.counts[b3]++;
                    } //else { Nothing to do! }
                }
            }
        }
        int runningTotal = e+1;
        //Transform counts into indicies of first byte of corresponding stuff in the buffer.
        for(int b = 255; b >= 0; b--) {
            if(chunks.radixOrders[b] == 0) {
                continue;
            }
            if(chunks.radixOrders[b] == 1) {
                runningTotal -=  cnts[b]<<2;
                cnts[b] = runningTotal;
            } else {
                Array cch = chunks.higherChunks[b];
                for(int c = 255; c >= 0; c--) {
                    if(cch.radixOrders[c] == 0) {
                        continue;
                    }
                    if(cch.radixOrders[c] == 1) {
                        runningTotal -=  cch.counts[c]<<2;
                        cch.counts[c] = runningTotal;
                    } else {
                        Array ccch = cch.higherChunks[c];
                        for(int d = 255; d >= 0; d--) {
                            if(cch.radixOrders[d] == 0) {
                                continue;
                            }
                            if(ccch.radixOrders[d] == 1) {
                                runningTotal -=  ccch.counts[d]<<2;
                                ccch.counts[d] = runningTotal;
                            } //else {Nothing to do!}
                        }
                    }
                    cch.counts[c] = runningTotal;
                }
                cnts[b] = runningTotal;
            }
        }
        for(int i = MIRROR_START; i < mirrorEnd;) {
            int b1 = buffer[i] + 128;
            if(chunks.radixOrders[b1] == 1) {
                int ind = cnts[b1]; //First index to write this stuff.
                buffer[ind] = buffer[i++];
                buffer[ind+1] = buffer[i++];
                buffer[ind+2] = buffer[i++];
                buffer[ind+3] = buffer[i++];
                cnts[b1] += 4;
            } else {
                int b2;
                if(buffer[i+1] < 0) {
                    b2 = buffer[i+1]+256;
                } else {
                    b2 = buffer[i+1];
                }
                entry = chunks.higherChunks[b1];
                if(entry.radixOrders[b2] == 1) {
                    int ind = entry.counts[b2];
                    buffer[ind] = buffer[i++];
                    buffer[ind+1] = buffer[i++];
                    buffer[ind+2] = buffer[i++];
                    buffer[ind+3] = buffer[i++];
                    entry.counts[b2] += 4;
                } else {
                    Array bob = entry.higherChunks[b2];
                    int b3;
                    if(buffer[i+2] < 0) {
                        b3 = buffer[i+2]+256;
                    } else {
                        b3 = buffer[i+2];
                    }
                    if(bob.radixOrders[b3] == 1) {
                        int ind = bob.counts[b3];
                        buffer[ind] = buffer[i++];
                        buffer[ind+1] = buffer[i++];
                        buffer[ind+2] = buffer[i++];
                        buffer[ind+3] = buffer[i++];
                        bob.counts[b3] += 4;
                    } //else {Do nothing!}
                }
            }
        }
    }



    public void repeatedlyRadixSort(byte[] buffer, int s, int e, int halt) {
        //long stt = System.nanoTime();
        int[][] cs = counts;
        if(halt == 0) {
            System.arraycopy(blanks, 0, cs[1], 0, 256);
            System.arraycopy(blanks, 0, cs[2], 0, 256);
            System.arraycopy(blanks, 0, cs[3], 0, 256);
            for(int i = s; i < e; i+=4) {
                int b2;
                b2 = buffer[i+3]+128;
                cs[3][b2]++;
                b2 = buffer[i+2]+128;
                cs[2][b2]++;
                b2 = buffer[i+1]+128;
                cs[1][b2]++;
            }
            int runningTotal = e+1;
            int runningTotal1 = e+1;
            int runningTotal2 = e+1;
            for(int b = 127; b >= 0; b--) {
                runningTotal -=  cs[3][b]<<2;
                cs[3][b] = runningTotal;
                runningTotal1 -=  cs[2][b]<<2;
                cs[2][b] = runningTotal1;
                runningTotal2 -=  cs[1][b]<<2;
                cs[1][b] = runningTotal2;
            }
            for(int b = 255; b > 128; b--) {
                runningTotal -=  cs[3][b]<<2;
                cs[3][b] = runningTotal;
                runningTotal1 -=  cs[2][b]<<2;
                cs[2][b] = runningTotal1;
                runningTotal2 -=  cs[1][b]<<2;
                cs[1][b] = runningTotal2;
            }
            cs[3][128] = s;
            cs[2][128] = s;
            cs[1][128] = s;
        } else if(halt == 1) {
            System.arraycopy(blanks, 0, cs[2], 0, 256);
            System.arraycopy(blanks, 0, cs[3], 0, 256);
            for(int i = s; i < e; i+=4) {
                int b2;
                b2 = buffer[i+3]+128;
                cs[3][b2]++;
                b2 = buffer[i+2]+128;
                cs[2][b2]++;
            }
            int runningTotal = e+1;
            int runningTotal1 = e+1;
            for(int b = 127; b >= 0; b--) {
                runningTotal -=  cs[3][b]<<2;
                cs[3][b] = runningTotal;
                runningTotal1 -=  cs[2][b]<<2;
                cs[2][b] = runningTotal1;
            }
            for(int b = 255; b > 128; b--) {
                runningTotal -=  cs[3][b]<<2;
                cs[3][b] = runningTotal;
                runningTotal1 -=  cs[2][b]<<2;
                cs[2][b] = runningTotal1;
            }
            cs[3][128] = s;
            cs[2][128] = s;
        } else if(halt == 2) {
            System.arraycopy(blanks, 0, cs[3], 0, 256);
            for(int i = s; i < e; i+=4) {
                int b2;
                b2 = buffer[i+3]+128;
                cs[3][b2]++;
            }
            int runningTotal = e+1;
            for(int b = 127; b >= 0; b--) {
                runningTotal -=  cs[3][b]<<2;
                cs[3][b] = runningTotal;
            }
            for(int b = 255; b > 128; b--) {
                runningTotal -=  cs[3][b]<<2;
                cs[3][b] = runningTotal;
            }
            cs[3][128] = s;
        }
        ///  timeUsed1 += (eet-stt);
        //// stt = System.nanoTime();

        int times = 3;
        while(times != halt) {
            System.arraycopy(buffer, s, buffer, MIRROR_START, e-s+1);
            //System.out.println(Arrays.toString(cs));
            //Now counts[0] has the start of the stuff to go into there..
            //counts[1] stores the number of bytes that all the stuff in [0] occupy. This is the index of the first
            //byte of the stuff to go into 1.
            //Yay.

            //Write everything back to buffer...
            final int mirrorEnd = MIRROR_START+e-s+1;
            int[] cnts = cs[times];
            for(int i = MIRROR_START; i < mirrorEnd; i+=4) {
                int b2 = buffer[i+times]+128;
                int ind = cnts[b2];
                buffer[ind] = buffer[i];
                buffer[ind+1] = buffer[i+1];
                buffer[ind+2] = buffer[i+2];
                buffer[ind+3] = buffer[i+3];
                cnts[b2] += 4;
            }
            times--;
        }
        // eet = System.nanoTime();
        //timeUsed2 += (eet-stt);
    }


}
