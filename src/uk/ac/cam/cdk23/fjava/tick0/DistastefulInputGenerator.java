package uk.ac.cam.cdk23.fjava.tick0;

import java.io.FileOutputStream;
import java.io.IOException;

public class DistastefulInputGenerator {
    public static void main(String[] args) throws IOException {
        for(int i = 0; i < 256; i++) {
            System.out.println("i:"+i+","+(i>>>4));
        }


        FileOutputStream d = new FileOutputStream("/home/chris/Flex/Flex3/ExternalSort/MixedUn.dat");
        for(int i = 0; i < 14856; i++) {
            d.write((int) (Math.random()*127));
            d.write((int) (Math.random()*127));
            d.write((int) (Math.random()*127));
            d.write((int) (Math.random()*127));
        }

        for(int i = 0; i < 564160; i++) {
            for(int j = -128; j < 256; j++) {
                d.write(5);
                d.write(i);
                d.write(3);
                d.write(3);
                i++;
            }
        }
        for(int i = 0; i < 3219809; i++) {
            for(int j = -128; j < 256; j++) {
                d.write(10);
                d.write(i);
                d.write(3);
                d.write(3);
                i++;
            }
        }
        for(int i = 0; i < 12459; i++) {
            d.write((int) (Math.random()*127));
            d.write((int) (Math.random()*127));
            d.write((int) (Math.random()*127));
            d.write((int) (Math.random()*127));
        }
    }
}
