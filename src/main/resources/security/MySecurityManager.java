import java.security.Permission;

/**
 * 默认的安全管理器
 */
public class MySecurityManager extends SecurityManager {
    @Override
    //检查所有的权限
    public void checkPermission(Permission perm) {
        // super.checkPermission(perm);
    }

    @Override
    //检测程序是否可以执行
    public void checkExec(String cmd) {
        super.checkExec(cmd);
        throw new SecurityException("checkExec权限异常" + cmd);
    }

    @Override
    //检测程序是否可以读取
    public void checkRead(String file) {
        System.out.println(file);
        if (file.contains("E:\\RapidProject\\pixoj-code-sanbox")){
            return;
        }
        // throw new SecurityException("checkRead权限异常" + file);
    }

    @Override
    //检测程序是否可以写入
    public void checkWrite(String file) {
        super.checkWrite(file);
        // throw new SecurityException("checkWrite权限异常" + file);
    }

    @Override
    //检测程序是否可以删除
    public void checkDelete(String file) {
        super.checkDelete(file);
        throw new SecurityException("checkDelete权限异常" + file);
    }

    @Override
    //检测程序是否可以连接网络
    public void checkConnect(String host, int port) {
        super.checkConnect(host, port);
        //throw new SecurityException("checkConnect权限异常" + host + ":" + port);
    }
}
