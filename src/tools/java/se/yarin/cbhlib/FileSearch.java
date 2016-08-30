package se.yarin.cbhlib;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileSearch {
    public static void main2(String[] args) throws IOException {
        FileInputStream stream = new FileInputStream("testbases/Cbase13.exe");
        FileChannel channel = stream.getChannel();
        ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
        channel.read(buf);
        channel.close();
        byte[] array = buf.array();

        for (int i = 0; i < array.length - 16; i++) {
            if (array[i] == (byte) 0x1E && array[i+7] == (byte) 0xAC && array[i+8] == (byte) 0x68 && array[i+15] == (byte) 0xD0) {
//                System.out.println(String.format("%08X", i));
                System.out.println(i);
            }
        }
    }

    private static int[] encryptKey = new int[] {
            0x98, 0x08, 0xB5, 0x3C, 0xCF, 0x94, 0x8F, 0xBB, 0x56, 0x4C, 0xF4, 0x2E, 0xFB, 0x24, 0x1F, 0xD9,
            0x50, 0x9F, 0xD4, 0x49, 0x30, 0x99, 0x85, 0xD5, 0x48, 0xEA, 0x11, 0x01, 0x6C, 0xB6, 0xAA, 0x6B,
            0x78, 0x67, 0xE8, 0x07, 0x5A, 0x26, 0x75, 0x7C, 0x4D, 0xDC, 0x92, 0x5D, 0x31, 0xBA, 0xC8, 0xE1,
            0xC0, 0x12, 0x60, 0xA0, 0xA9, 0xD1, 0x5F, 0x09, 0x4B, 0xA6, 0xDD, 0xE3, 0x4E, 0x70, 0x96, 0x22,
            0x6E, 0x21, 0x83, 0xEB, 0x5C, 0x3F, 0x8E, 0x74, 0x05, 0x65, 0xDE, 0x66, 0xB1, 0xCC, 0xB4, 0x7B,
            0x0C, 0xF7, 0xF9, 0x76, 0xDB, 0x37, 0x20, 0x32, 0xB3, 0xF3, 0x72, 0x69, 0x13, 0x0E, 0x46, 0x43,
            0x8B, 0xD6, 0x40, 0xC4, 0xD8, 0x6F, 0xF2, 0x62, 0xBE, 0x34, 0xE5, 0xDF, 0xF6, 0xCB, 0xEC, 0x86,
            0x2A, 0x1C, 0x3D, 0x39, 0x81, 0x1A, 0x0D, 0x6A, 0x16, 0x5B, 0x35, 0x8A, 0x3A, 0xAB, 0xB8, 0x61,
            0x10, 0x57, 0xA3, 0xC3, 0xA4, 0x95, 0x03, 0xC6, 0x28, 0x29, 0xCD, 0x51, 0x54, 0xD7, 0x41, 0xA1,
            0x9D, 0xF8, 0xF0, 0xE4, 0x55, 0x9E, 0x9B, 0x02, 0xB9, 0x2C, 0x2F, 0x80, 0x90, 0xFC, 0x33, 0x88,
            0xA7, 0x1E, 0x04, 0x19, 0xE7, 0xDA, 0x2D, 0x58, 0xAC, 0x68, 0xE6, 0xFD, 0xC2, 0x38, 0x7D, 0x42,
            0xD0, 0x97, 0xB7, 0x1B, 0xC1, 0x4A, 0x7F, 0xA5, 0x82, 0x89, 0xFE, 0xE0, 0x15, 0x4F, 0xC9, 0xF5,
            0x45, 0x5E, 0x8D, 0xB2, 0x25, 0x3B, 0xA2, 0x93, 0xD2, 0xD3, 0xBD, 0xBF, 0x44, 0x0F, 0x9C, 0xCA,
            0xAD, 0x9A, 0x79, 0x91, 0x77, 0xBC, 0xCE, 0x87, 0x7A, 0xE2, 0x17, 0xEF, 0x2B, 0x53, 0x8C, 0x36,
            0x00, 0x1D, 0xAF, 0x23, 0x63, 0x18, 0x27, 0xFF, 0xA8, 0x52, 0xAE, 0x0B, 0xC5, 0xF1, 0xC7, 0xB0,
            0x06, 0xED, 0xFA, 0x47, 0xE9, 0x3E, 0x6D, 0xEE, 0x73, 0x71, 0x0A, 0x7E, 0x84, 0x64, 0x59, 0x14
    };

    public static void main3(String[] args) throws IOException {
        FileOutputStream fos = new FileOutputStream("moveEncryptionKey2.bin");
        byte[] key = new byte[256];
        boolean[] seen = new boolean[256];
        for (int i = 0; i < encryptKey.length; i++) {
            if (seen[encryptKey[i]]) throw new RuntimeException();
            seen[encryptKey[i]] = true;
            key[i] = (byte) encryptKey[i];
        }
        fos.write(key);
        fos.close();
    }

    public static void main(String[] args) throws IOException {
        FileInputStream stream = new FileInputStream("testbases/Cbase13.exe");
        FileChannel channel = stream.getChannel();
        ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
        channel.read(buf);
        channel.close();
        byte[] array = buf.array();

        int[] last = new int[256];
        for (int i = 0; i < array.length-256; i++) {
            boolean ok = true;

            for (int j = 0; j < 256; j++) {
                int b = (array[i+j] + 256) % 256;
                if (last[b] == i) {
                    ok = false;
                } else {
                    last[b] = i;
                }
            }
            if (ok) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 10; j++) {
                    sb.append(String.format("%02X ", array[i+j]));
                }
                System.out.println(String.format("Unique key at %d (%X) starting with %s", i, i, sb.toString()));
            }
        }
    }
}
