package com.pixar.pixojcodesanbox.security;

import java.security.Permission;

/**
 * 禁用所有的安全管理器
 */
public class DenySecurityManager extends SecurityManager {
    @Override
    // 检查所有的权限
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限异常" + perm.toString());

    }
}
