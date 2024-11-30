package com.pixar.pixojcodesanbox.security;

import java.security.Permission;

/**
 * 默认的安全管理器
 */
public class MySecurityManager extends SecurityManager {
    @Override
    //检测程序是否可以执行
    public void checkExec(String cmd) {
        super.checkExec(cmd);
    }

    @Override
    //检测程序是否可以读取
    public void checkRead(String file) {
        super.checkRead(file);
    }

    @Override
    //检测程序是否可以写入
    public void checkWrite(String file) {
        super.checkWrite(file);
    }

    @Override
    //检测程序是否可以删除
    public void checkDelete(String file) {
        super.checkDelete(file);
    }

    @Override
    //检测程序是否可以连接网络
    public void checkConnect(String host, int port) {
        super.checkConnect(host, port);
    }
}
