package uk.ac.cam.cdk23.fjava.tick0;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public class TwiddleTester {

    private static final long[] b = {0x2L, 0xCL, 0xF0L, 0xFF00L, 0xFFFF0000L, 0xFFFFFFFF00000000L};
    private static final int[] S = {1,2,4,8,16,32};


    private static long[] bitmasks = {
            0x8000000000000000L,0x4000000000000000L,0x2000000000000000L,0x1000000000000000L,
            0x0800000000000000L,0x0400000000000000L,0x0200000000000000L,0x0100000000000000L,
            0x0080000000000000L,0x0040000000000000L,0x0020000000000000L,0x0010000000000000L,
            0x0008000000000000L,0x0004000000000000L,0x0002000000000000L,0x0001000000000000L,
            0x0000800000000000L,0x0000400000000000L,0x0000200000000000L,0x0000100000000000L,
            0x0000080000000000L,0x0000040000000000L,0x0000020000000000L,0x0000010000000000L,
            0x0000008000000000L,0x0000004000000000L,0x0000002000000000L,0x0000001000000000L,
            0x0000000800000000L,0x0000000400000000L,0x0000000200000000L,0x0000000100000000L,
            0x0000000080000000L,0x0000000040000000L,0x0000000020000000L,0x0000000010000000L,
            0x0000000008000000L,0x0000000004000000L,0x0000000002000000L,0x0000000001000000L,
            0x0000000000800000L,0x0000000000400000L,0x0000000000200000L,0x0000000000100000L,
            0x0000000000080000L,0x0000000000040000L,0x0000000000020000L,0x0000000000010000L,
            0x0000000000008000L,0x0000000000004000L,0x0000000000002000L,0x0000000000001000L,
            0x0000000000000800L,0x0000000000000400L,0x0000000000000200L,0x0000000000000100L,
            0x0000000000000080L,0x0000000000000040L,0x0000000000000020L,0x0000000000000010L,
            0x0000000000000008L,0x0000000000000004L,0x0000000000000002L,0x0000000000000001L};
    private static final int[] LogTable512= {0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8};
    public static void main(String[] args) {
        System.out.println(md5("spoonerism"));
    }



    private static String md5(String s) {
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder s1 = new StringBuilder();
            String r;
            for(byte n : md.digest(s.getBytes("UTF-8"))) {
                r = Integer.toHexString(n);
                if (r.length() == 8) {
                    r = r.substring(6);
                }
                s1.append(r);
            }
            return s1.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    private static long[] getTestLongArray(int n) {
        //Produce an array of n random ints.
        long[] r = new long[n];
        int i = 0;
        while (i < n) {
            r[i] = (long) (Math.random()*Long.MAX_VALUE-Math.random()*Long.MAX_VALUE);
            i++;
        }
        return r;
    }

   /* public void sortTwo(byte[] buffer, int s, int e) {//s and e represent the first and last bytes of the complete
        //ints to sort, regardless of the actual sorting policy being used.
        //This is also counting-sort-able.

        //Perform counting sort against the final two bytes. Assumption is that all ints in range provided have the same
        //first and second byte, either via the action of sortOne or the MSByte phase.
        for(int i = s+2; i < e; i+=4) {
            short b3;
            if(buffer[i] < 0) {
                b3 = (short) (buffer[i]+256);
            } else {
                b3 = buffer[i];
            }
            short b4;
            if(buffer[i+1] < 0) {
                b4 = (short) (buffer[i+1]+256);
            } else {
                b4 = buffer[i+1];
            }
            counts[b3][b4]++;
            countsHad[b3] = true;
            deeperCountsHad[b3][b4>>>4] = true;
        }
        //Now consume counts to construct answer - remember to reset the counting structures as you go.
        int outPointer = s+2; //First byte to edit.
        int posCount;
        int b;
        int targ;
        byte fix2;
        byte fix3;
        for(int i = 0; i < 256; i++) {
            if(!countsHad[i]) {
                continue;
            }
            if(i >= 128) {
                fix2 = (byte) (i-256);
            } else {
                fix2 = (byte) i;
            }
            posCount = 0;
            while(posCount != 16) {
                if(deeperCountsHad[i][posCount]) {
                    targ = ((posCount+1)<<4);

                    for(b = posCount<<4; b < targ; b++) {
                        while(counts[i][b] > 0) {
                            if(b >= 128) {
                                fix3 = (byte) (b-256);
                            } else {
                                fix3 = (byte) b;
                            }
                            buffer[outPointer] = fix2;
                            buffer[outPointer+1] = fix3;
                            outPointer += 4;
                            counts[i][b]--;
                        }
                    }
                    deeperCountsHad[i][posCount] = false;
                }
                posCount++;
            }
            countsHad[i] = false;
        }
//        assert(outPointer == e+1);
    }*/
}
