package com.pixar.pixojcodesanbox.unsafe;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限占用空间，占用系统内存
 */
public class MemoryError {
    public static void main(String[] args) throws InterruptedException {
        List<byte[]> bytes = new ArrayList<>();
        while (true){
            bytes.add(new byte[100]);
        }
    }
}
