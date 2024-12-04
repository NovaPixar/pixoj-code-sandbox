package com.pixar.pixojcodesanbox.security;

import java.security.Permission;

/**
 * 默认的安全管理器
 */
public class DefaultSecurityManager extends SecurityManager {
    @Override
    // 检查所有的权限
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何权限限制"+perm);
        // super.checkPermission(perm);
        throw new SecurityException("权限不足");
    }


}
