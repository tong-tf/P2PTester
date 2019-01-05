package com.shangyun.p2ptester;

import java.util.Arrays;
import java.util.Random;

public class P2P_StringEncDec {

    public static int iPN_StringEnc(byte[] keystr, byte[] src, byte[] dest, int maxsize) {
        String tmp = new String(src);
//        System.out.printf("EncSrc = %s   %d%n", tmp, tmp.length());
        int[] Key = new int[17];
        int i;
        int s, v;
        if (maxsize < src.length * 2 + 3) {
            return -1;
        }
        for (i = 0; i < 16; i++) {
            Key[i] = keystr[i];
        }

        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        s = (random.nextInt() & Integer.MAX_VALUE) % 256;
        Arrays.fill(dest, (byte) 0);
        dest[0] = (byte) ('A' + ((s & 0xF0) >> 4));
        dest[1] = (byte) ('a' + (s & 0x0F));
        for (i = 0; i < src.length; i++) {
            v = s ^ Key[(i + s * (s % 23)) % 16] ^ src[i];
            dest[2 * i + 2] = (byte) ('A' + ((v & 0xF0) >> 4));
            dest[2 * i + 3] = (byte) ('a' + (v & 0x0F));
            s = v;
        }
        tmp = new String(dest);
//        System.out.println("EncDest = "+tmp+"   "+tmp.length());
        return 0;
    }

    public static int iPN_StringDnc(byte[] keystr, byte[] src, byte[] dest, int maxsize) {
        String tmp = new String(src);
        int[] Key = new int[17];
        int i, count = 0;
        int s, v;
        for (i = 0; i < src.length; i++) {
            if (src[i] == 0) {
                break;
            }
            count++;
        }
//        System.out.println("DncSrc = " + tmp + "count =" + count);
        if ((maxsize < count / 2) || (count % 2 == 1)) {
            return -1;
        }
        for (i = 0; i < 16; i++) {
            Key[i] = keystr[i];
        }
        Arrays.fill(dest, (byte) 0);
        s = ((src[0] - 'A') << 4) + (src[1] - 'a');
        for (i = 0; i < count / 2 - 1; i++) {
            v = ((src[i * 2 + 2] - 'A') << 4) + (src[i * 2 + 3] - 'a');
            dest[i] = (byte) (v ^ Key[(i + s * (s % 23)) % 16] ^ s);
            if (dest[i] > 127 || dest[i] < 32) return -1; // not a valid character string
            s = v;
        }
        tmp = new String(dest);
//        System.out.println("DncDest = " + tmp + "   " + tmp.length());
        return 0;
    }

}
