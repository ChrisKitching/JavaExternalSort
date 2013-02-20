package uk.ac.cam.cdk23.fjava.tick0;

public final class IntroSort {
    private final static class Range {
        public final int left;
        public final int right;
        public final int depth;

        public Range(int l, int r, int d) {
            left = l;
            right = r;
            depth = d;
        }
    }


    //private static byte[] input;
    private static int recursionDepth;

    private static final int[] leonardoNumbers = {1,1,3,5,9,15,25,41,67,109,177,287,465,753,1219,1973,3193,5167,8361,13529,21891,35421, 57313,92735,150049,242785,392835,635621,1028457,1664079,2692537,4356617,7049155,11405773,18454929,29860703,48315633,78176337,126491971,204668309,331160281,535828591,866988873};

    public static final void smoothSort(byte[] input, final int start, int end) {
        //SmoothSort the range specified (inclusive).
        //Since this function will be called with larger ranges the more sorted the array was (Since quicksort isn't
        //terribly good at dealing with sorted arrays), the adaptive properties of smoothSort should be rather handy.

        int head = start;

        int p = 1; // the bitmap of the current standard concatenation >> pshift
        int pshift = 1;

        while (head < end-3) {
            //System.out.println("Processing head:"+head);
            if ((p & 3) == 3) {
                // Add 1 by merging the first two blocks into a larger one.
                // The next Leonardo number is one bigger.
                //System.out.println("p&3 == 3");
                sift(input, pshift, head);
                p >>>= 2;
                pshift += 2;
            } else {
                // adding a new block of length 1
                if ((leonardoNumbers[pshift - 1])*4 >= end-3 - head) {//TODO?
                    // this block is its final size.
                    trinkle(input, p, pshift, head, false);
                } else {
                    // this block will get merged. Just make it trusty.
                    sift(input, pshift, head);
                }

                if (pshift == 1) {
                    // LP[1] is being used, so we add use LP[0]
                    p <<= 1;
                    pshift--;
                } else {
                    // shift out to position 1, add LP[1]
                    p <<= (pshift - 1);
                    pshift = 1;
                }
            }
            p |= 1;
            head+=4;
        }
        //head -= 4;

        trinkle(input, p, pshift, head, false);


        while (pshift != 1 || p != 1) {
            if (pshift <= 1) {
                // block of length 1. No fiddling needed
                //int trail;// = Integer.numberOfTrailingZeros(p & ~1);
                final int pa1 = p & ~1;
                int trail = 32;
                int v = pa1 & -pa1;
                if (v != 0) trail--;
                if ((v & 0x0000FFFF) != 0) trail -= 16;
                if ((v & 0x00FF00FF) != 0) trail -= 8;
                if ((v & 0x0F0F0F0F) != 0) trail -= 4;
                if ((v & 0x33333333) != 0) trail -= 2;
                if ((v & 0x55555555) != 0) trail -= 1;
                p >>>= trail;
                pshift += trail;
            } else {
                p <<= 2;
                p ^= 7;
                pshift -= 2;

                // This block gets broken into three bits. The rightmost
                // bit is a block of length 1. The left hand part is split into
                // two, a block of length LP[pshift+1] and one of LP[pshift].
                // Both these two are appropriately heapified, but the root
                // nodes are not necessarily in order. We therefore semitrinkle
                // both of them
                //   System.out.println("head:"+head);
                //   System.out.println("leo:"+(leonardoNumbers[pshift])*4 );
                //   System.out.println("Passing:"+( head - (leonardoNumbers[pshift])*4 - 4));

                trinkle(input, p >>> 1, pshift + 1, (head - 4) - ((leonardoNumbers[pshift])*4)  , true);
                trinkle(input, p, pshift, head - 4, true);
            }

            head-=4;
        }
        //sift(2, end-11);

    }

    private static final void sift(byte[] input, int pshift,int head) {
        //Considering head as the root node of a heap of size L(pshift), restoe it's heap property.

        final byte byte1 = input[head];
        final byte byte2 = input[head+1];
        final byte byte3 = input[head+2];
        final byte byte4 = input[head+3];
        int val = (((byte1 & 0xff) << 24) | ((byte2 & 0xff) << 16) | ((byte3 & 0xff) << 8) | (byte4 & 0xff)); //The value
        //starting at head.

        while (pshift > 1) {
            int rt = head - 4;
            int lf = head - 4 - (leonardoNumbers[pshift - 2])*4;

            //Left child
            final byte lbyte1 = input[lf];
            final byte lbyte2 = input[lf+1];
            final byte lbyte3 = input[lf+2];
            final byte lbyte4 = input[lf+3];
            final int lfval = (((lbyte1 & 0xff) << 24) | ((lbyte2 & 0xff) << 16) | ((lbyte3 & 0xff) << 8) | (lbyte4 & 0xff)); //The value

            //Right child
            final byte rbyte1 = input[rt];
            final byte rbyte2 = input[rt+1];
            final byte rbyte3 = input[rt+2];
            final byte rbyte4 = input[rt+3];
            final int rtval = (((rbyte1 & 0xff) << 24) | ((rbyte2 & 0xff) << 16) | ((rbyte3 & 0xff) << 8) | (rbyte4 & 0xff)); //The value

            if (val >= lfval && val >= rtval) {
                //Heap satisfactory.
                break;
            }
            //Swap head with largest child...
            if (lfval >= rtval) {
                input[head] = lbyte1;
                input[head+1] = lbyte2;
                input[head+2] = lbyte3;
                input[head+3] = lbyte4;
                head = lf;
                pshift -= 1;
            } else {
                input[head] = rbyte1;
                input[head+1] = rbyte2;
                input[head+2] = rbyte3;
                input[head+3] = rbyte4;
                head = rt;
                pshift -= 2;
            }
        }

        input[head] = byte1;
        input[head+1] = byte2;
        input[head+2] = byte3;
        input[head+3] = byte4;
    }

    private static final void trinkle(byte[] input, int p,int pshift, int head, boolean isTrusty) {
        //Heap with root at head has the heap property - now restoring the string property.


        final byte byte1 = input[head];
        final byte byte2 = input[head+1];
        final byte byte3 = input[head+2];
        final byte byte4 = input[head+3];
        int val = (((byte1 & 0xff) << 24) | ((byte2 & 0xff) << 16) | ((byte3 & 0xff) << 8) | (byte4 & 0xff)); //The value

        while (p != 1) {
            int stepson = head - (leonardoNumbers[pshift])*4;

            final byte sbyte1 = input[stepson];
            final byte sbyte2 = input[stepson+1];
            final byte sbyte3 = input[stepson+2];
            final byte sbyte4 = input[stepson+3];
            int stepval = (((sbyte1 & 0xff) << 24) | ((sbyte2 & 0xff) << 16) | ((sbyte3 & 0xff) << 8) | (sbyte4 & 0xff)); //The value

            if (stepval <= val) {
                break; // current node is greater than head. Sift.
            }

            // no need to check this if we know the current node is trusty,
            // because we just checked the head (which is val, in the first
            // iteration)
            if (!isTrusty && pshift > 1) {
                int rt = head - 4;
                int lf = head - 4 - (leonardoNumbers[pshift - 2])*4;

                //Left child
                final byte lbyte1 = input[lf];
                final byte lbyte2 = input[lf+1];
                final byte lbyte3 = input[lf+2];
                final byte lbyte4 = input[lf+3];
                final int lfval = (((lbyte1 & 0xff) << 24) | ((lbyte2 & 0xff) << 16) | ((lbyte3 & 0xff) << 8) | (lbyte4 & 0xff)); //The value

                //Right child
                final byte rbyte1 = input[rt];
                final byte rbyte2 = input[rt+1];
                final byte rbyte3 = input[rt+2];
                final byte rbyte4 = input[rt+3];
                final int rtval = (((rbyte1 & 0xff) << 24) | ((rbyte2 & 0xff) << 16) | ((rbyte3 & 0xff) << 8) | (rbyte4 & 0xff)); //The value

                if (rtval >= stepval || lfval  >= stepval) {
                    break;
                }
            }

            input[head] = sbyte1;
            input[head+1] = sbyte2;
            input[head+2] = sbyte3;
            input[head+3] = sbyte4;

            head = stepson;
            //int trail; = Integer.numberOfTrailingZeros(p & ~1);

            final int pa1 = p & ~1;
            int trail = 32;
            int v = pa1 & -pa1;
            if (v != 0) trail--;
            if ((v & 0x0000FFFF) != 0) trail -= 16;
            if ((v & 0x00FF00FF) != 0) trail -= 8;
            if ((v & 0x0F0F0F0F) != 0) trail -= 4;
            if ((v & 0x33333333) != 0) trail -= 2;
            if ((v & 0x55555555) != 0) trail -= 1;
            p >>>= trail;
            pshift += trail;
            isTrusty = false;
        }

        if (!isTrusty) {
            input[head] = byte1;
            input[head+1] = byte2;
            input[head+2] = byte3;
            input[head+3] = byte4;
            sift(input, pshift, head);
        }
    }

    public  final void Sort(byte[] a, int s, int e) {
       // input = a;
        //int iLength = (e+1-s)/4;
        if(a.length <= 42) { //The answer to life, the universe, and everything.
            insertSort(a, s, e);
        } else {
            smoothSort(a, s,e);
        }
    }
/*
    private final void quickSortInPlace(final int l, final int r) {  //In-place quicksort from l to r
        //inclusive with recursion depth d.
        Stack<Range> ranges = new Stack<>();
        ranges.push(new Range(l, r, 0));

        while(!ranges.isEmpty()) {
            Range current = ranges.pop();
            //System.out.println("Range:"+current.left+"-"+current.right);
            assert((current.right+1-current.left) % 4 == 0);
            final int left = current.left;
            final int right = current.right;
            int pointer = left;
            int outPointer = left;
            //Median of three...
            //Let's do stupid pivot selection - maybe this area is the fault.
            final byte byte1 = input[left];
            final byte byte2 = input[left+1];
            final byte byte3 = input[left+2];
            final byte byte4 = input[left+3];

            final int pivot =  (((byte1 & 0xff) << 24) | ((byte2 & 0xff) << 16) | ((byte3 & 0xff) << 8) | (byte4 & 0xff));

            /*

            final byte rbyte1 = input[right-3];
            final byte rbyte2 = input[right-2];
            final byte rbyte3 = input[right-1];
            final byte rbyte4 = input[right];
            final int rInt =  (((rbyte1 & 0xff) << 24) | ((rbyte2 & 0xff) << 16) | ((rbyte3 & 0xff) << 8) | (rbyte4 & 0xff));
            final byte lbyte1 = input[left];
            final byte lbyte2 = input[left+1];
            final byte lbyte3 = input[left+2];
            final byte lbyte4 = input[left+3];
            final int lInt =  (((lbyte1 & 0xff) << 24) | ((lbyte2 & 0xff) << 16) | ((lbyte3 & 0xff) << 8) | (lbyte4 & 0xff));
            final int middle = left+(((right+1)-left)>>>1);
            //System.out.println("Middle:"+middle);
            final byte mbyte1 = input[middle];
            final byte mbyte2 = input[middle+1];
            final byte mbyte3 = input[middle+2];
            final byte mbyte4 = input[middle+3];
            final int mInt =  (((mbyte1 & 0xff) << 24) | ((mbyte2 & 0xff) << 16) | ((mbyte3 & 0xff) << 8) | (mbyte4 & 0xff));


            final int pivotIndex;
            final int pivot;
            final byte byte1;
            final byte byte2;
            final byte byte3;
            final byte byte4;
            //Choose pivot and swap it with rightmost element.
            if(lInt > mInt) {
                if(lInt > rInt) {
                    //lInt is biggest.
                    if(rInt > mInt) {
                        //rInt is pivot
                        pivot = rInt;
                        byte1 = rbyte1;
                        byte2 = rbyte2;
                        byte3 = rbyte3;
                        byte4 = rbyte4;
                        pivotIndex = right-3;
                    } else {
                        //mInt is pivot.
                        pivot = mInt;
                        byte1 = mbyte1;
                        byte2 = mbyte2;
                        byte3 = mbyte3;
                        byte4 = mbyte4;
                        pivotIndex = middle;
                    }
                } else {
                    //lInt is bigger than mInt and smaller than rInt.
                    //lInt is pivot.
                    pivot = lInt;
                    byte1 = lbyte1;
                    byte2 = lbyte2;
                    byte3 = lbyte3;
                    byte4 = lbyte4;
                    pivotIndex = left;
                }
            } else {
                //lInt smaller than mInt
                if(mInt > rInt) {
                    //mInt is largest
                    if(rInt > lInt) {
                        //rInt pivot
                        pivot = rInt;
                        byte1 = rbyte1;
                        byte2 = rbyte2;
                        byte3 = rbyte3;
                        byte4 = rbyte4;
                        pivotIndex = right-3;
                    } else {
                        //lInt pivot.
                        pivot = lInt;
                        byte1 = lbyte1;
                        byte2 = lbyte2;
                        byte3 = lbyte3;
                        byte4 = lbyte4;
                        pivotIndex = left;
                    }
                } else {
                    //mInt is pivot
                    pivot = mInt;
                    byte1 = mbyte1;
                    byte2 = mbyte2;
                    byte3 = mbyte3;
                    byte4 = mbyte4;
                    pivotIndex = middle;
                }
            }

            //Swap pivot and the rightmost element.
            // if(pivot != rInt) {
            input[left] = input[right-3];
            input[left +1] = input[right-2];
            input[left +2] = input[right-1];
            input[left +3] = input[right];
            input[right-3] = byte1;
            input[right-2] = byte2;
            input[right-1] = byte3;
            input[right] = byte4;
            // }

            while (pointer < right-3) {
                if ((((input[pointer] & 0xff) << 24) | ((input[pointer+1] & 0xff) << 16) | ((input[pointer+2] & 0xff) << 8) | (input[pointer+3] & 0xff)) <= pivot) {
                    //Swap this with outPointer.
                    final byte b1 = input[pointer];
                    final byte b2 = input[pointer+1];
                    final byte b3 = input[pointer+2];
                    final byte b4 = input[pointer+3];
                    input[pointer] = input[outPointer];
                    input[pointer+1] = input[outPointer+1];
                    input[pointer+2] = input[outPointer+2];
                    input[pointer+3] = input[outPointer+3];
                    input[outPointer++] = b1;
                    input[outPointer++] = b2;
                    input[outPointer++] = b3;
                    input[outPointer++] = b4;
                    //outPointer+=4;
                }
                pointer+=4;
            }
            //Swap the rightmost element (Pivot) with the element currently on the end of the elements smaller than
            //the pivot.
            input[right-3] = input[outPointer];
            input[right-2] = input[outPointer+1];
            input[right-1] = input[outPointer+2];
            input[right] = input[outPointer+3];
            input[outPointer] = byte1;
            input[outPointer+1] = byte2;
            input[outPointer+2] = byte3;
            input[outPointer+3] = byte4;
            //outPointer points to pivot.
            //Now sort range outPointer+4 to right
            //Now sort range left to outPointer-1

            //Length of left range is outPointer-left
            //if outPointer-left < 5 - no point.

            //Length of right range is (right+1)-(outPointer+4)
            //If that < 5 - no point.

            //left points to the first index to sort here.
            //right points to the last index to sort here.
            final int leftLen = outPointer-left;
            final int rightLen = (right+1)-(outPointer);

            /*if(leftLen > 4) {
                ranges.push(new Range(left, outPointer-1, current.depth+1));
            }
            if(rightLen > 4) {
                ranges.push(new Range(outPointer+4, right, current.depth+1));
            }*

            if(leftLen > 128) {
                if(current.depth < recursionDepth) {
                    ranges.push(new Range(left, outPointer-1, current.depth+1));
                } else {
                    smoothSort(left, outPointer-1);
                   /* if(!verifyRange(left, outPointer-1)) {
                        return;
                    }
                }
            } else if(leftLen > 4) {
                insertSort(left, outPointer-1);
            }

            if(rightLen > 128) {
                if(current.depth < recursionDepth) {
                    ranges.push(new Range(outPointer, right, current.depth+1));
                } else {
                    smoothSort(outPointer, right);
                    /*if(!verifyRange(outPointer, right)) {
                        return;
                    }
                }
            } else if(rightLen > 4) {
                insertSort(outPointer, right);
            }
        }
    }*/

    public static final void insertSort(byte[] input, int start, int en) {  //Binary insort from start to en inclusive.
        //Sort range from start to en inclusive.
        int pointer = start+4;
        while (pointer < en+1) {
            final byte byte1 = input[pointer];
            final byte byte2 = input[pointer+1];
            final byte byte3 = input[pointer+2];
            final byte byte4 = input[pointer+3];
            final int p = (((byte1 & 0xff) << 24) | ((byte2 & 0xff) << 16) | ((byte3 & 0xff) << 8) | (byte4 & 0xff));
            final int end = pointer - 4;  //End of sorted range.
            int l = start;
            int r = pointer - 4;
            while (l != r) {
                //Binary search in the already sorted range to find the proper position of the next unsorted element, p.
                int middle = (l + r) >>> 1;
                while (middle % 4 != 0) {
                    middle++;
                }
                if (middle == r) {
                    middle -= 4;
                }
                final int middleInt = (((input[middle] & 0xff) << 24) | ((input[middle+1] & 0xff) << 16) | ((input[middle+2] & 0xff) << 8) | (input[middle+3] & 0xff));
                if (p > middleInt) {
                    l = middle + 4;
                } else {
                    r = middle;
                }
            }
            if (r == end && p > (((input[end] & 0xff) << 24) | ((input[end+1] & 0xff) << 16) | ((input[end+2] & 0xff) << 8) | (input[end+3] & 0xff))) {
                l+=4;
            }
            //Now to put p into it's proper place...
            System.arraycopy(input, l, input, l + 4, end - l + 4);
            input[l] = byte1;
            input[l+1] = byte2;
            input[l+2] = byte3;
            input[l+3] = byte4;
            pointer+=4;
        }
    }
}
