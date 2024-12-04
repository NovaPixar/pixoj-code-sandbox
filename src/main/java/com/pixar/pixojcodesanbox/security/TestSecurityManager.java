package com.pixar.pixojcodesanbox.security;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.util.List;

public class TestSecurityManager {
    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());
        FileUtil.writeString("aaa", "test.txt", "utf-8");
        System.out.println();
    }
}
