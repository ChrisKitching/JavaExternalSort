package uk.ac.cam.cdk23.fjava.tick0;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReadingThread implements Runnable {
    //A thread that is given two buffers, buffer1 and buffer2 and a file to read.
    //The thread then repeatedly fills the buffers, notifying the consuming thread after each fill.
    private byte[] buffer1;
    private byte[] buffer2;

    private byte[] currentBuffer;

    public final AtomicBoolean full1 = new AtomicBoolean(false);
    public final AtomicBoolean full2 = new AtomicBoolean(false);

    public int read1;
    public int read2;

    public final AtomicBoolean isRunning = new AtomicBoolean(false);
    private int readAmount;
    private int BUFFER_SIZE;

    private FileInputStream inStream;  //Stream to read from.
    public void run() {
        int amountRead = 0;
        while(true) {
            try{
                while (amountRead < readAmount) {
                    final int readA = readAmount-amountRead > BUFFER_SIZE ? BUFFER_SIZE : readAmount-amountRead;
                    int result;
                    if (currentBuffer == buffer1) {
                        synchronized (full1) {
                            //System.out.println("IO waiting 1");
                            while (full1.get()) {
                                full1.wait();
                            }
                            //Thread.sleep(1000);
                        }
                        synchronized (buffer1) {
                            //System.out.println("Starting read 1...");
                            // long l = System.nanoTime();
                            result = inStream.read(buffer1, 0, readA);
                            // long done = System.nanoTime() - l;
                           // System.out.println("Read complete "+result);
                            read1 = result;
                            amountRead += result;
                            //System.out.println("Total read:"+amountRead);
                            full1.set(true);
                            synchronized (full1) {
                                full1.notify();
                            }
                        }
                        currentBuffer = buffer2;
                    } else {
                        synchronized (full2) {
                            //System.out.println("IO waiting 2");
                            while (full2.get()) {
                                full2.wait();
                            }
                            //Thread.sleep(1000);
                        }
                        synchronized (buffer2) {
                            //System.out.println("Starting read 2...");
                            //  long l = System.nanoTime();
                            result = inStream.read(buffer2, 0, readA);
                            //    long done = System.nanoTime() - l;
                            //System.out.println("Read complete "+result);
                            read2 = result;
                            amountRead += result;
                            //System.out.println("Total read:"+amountRead);
                            full2.set(true);
                            synchronized (full2) {
                                full2.notify();
                            }
                        }
                        currentBuffer = buffer1;
                    }
                    if (result == -1) {
                        System.out.println("Breaking on EOF");
                        break;
                    }
                }
            } catch  (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            isRunning.set(false);
            //System.out.println("Reading thread sleeping.");
            try{
                synchronized(isRunning) {
                    while(!isRunning.get()) {
                        isRunning.wait();
                    }
                }
                amountRead = 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
           // System.out.println("Reading thread woken.");
        }
    }
    public void prime(byte[] b1, byte[] b2, FileInputStream inS, int len, int BS) {
        inStream = inS;
        buffer1 = b1;
        buffer2 = b2;
        currentBuffer = buffer1;
        isRunning.set(true);
        readAmount = len;
        BUFFER_SIZE = BS;
    }
    public void release() {
        inStream = null;
        buffer1 = null;
        buffer2 = null;
        currentBuffer = null;
        isRunning.set(false);
        full1.set(false);
        full2.set(false);
    }
}
