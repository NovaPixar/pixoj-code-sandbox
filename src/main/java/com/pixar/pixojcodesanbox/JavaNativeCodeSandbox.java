package com.pixar.pixojcodesanbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.pixar.pixojcodesanbox.model.ExecuteCodeRequest;
import com.pixar.pixojcodesanbox.model.ExecuteCodeResponse;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandbox implements CodeSandbox {
    public static final  String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1,2","1,3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        //1. 获取用户传过来的代码
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        String userDir = System.getProperty("user.dir");
        // 为了兼容不同的系统就需要用到 File.separator
        String globalCodePathName = userDir+ File.separator+GLOBAL_CODE_DIR_NAME;
        // 判断父级目录是否存在,没有则新建
        if (!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String  userCodePath = userCodeParentPath+File.separator+GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2.编译代码，得到class文件
        String compiledCmd = String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compiledCmd);
            // 等待程序执行，直到程序运行完成得到一个错误码
            int exitValue = compileProcess.waitFor();
            //正常退出
            if (exitValue == 0){
                System.out.println("编译成功");
                // 分批获取控制台的输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine())!=null){
                    System.out.println(compileOutputLine);
                }


            }else {
                //异常退出
                System.out.println("编译失败，错误码："+exitValue);
                // 分批获取程序的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine())!=null){
                    System.out.println(compileOutputLine);
                }
                // 分批获取控制台的输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine())!=null){
                    System.out.println(errorCompileOutputLine);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
