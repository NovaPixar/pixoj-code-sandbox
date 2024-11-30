package com.pixar.pixojcodesanbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.pixar.pixojcodesanbox.model.ExecuteCodeRequest;
import com.pixar.pixojcodesanbox.model.ExecuteCodeResponse;
import com.pixar.pixojcodesanbox.model.ExecuteMessage;
import com.pixar.pixojcodesanbox.model.JudgeInfo;
import com.pixar.pixojcodesanbox.security.DefaultSecurityManager;
import com.pixar.pixojcodesanbox.security.DenySecurityManager;
import com.pixar.pixojcodesanbox.utils.ProcessUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import javax.rmi.CORBA.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JavaNativeCodeSandbox implements CodeSandbox {
    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final List<String> blackList = Arrays.asList("Files","exec");

    public static final WordTree WORD_TREE;

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    private static final long TIME_OUT = 5000L;

    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        // String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("testCode/unsafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        // String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.setSecurityManager(new DenySecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        //1. 获取用户传过来的代码
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        // 校验黑名单代码代码
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if(foundWord != null){
//            System.out.println("存在敏感词：" + foundWord.getFoundWord());
//            return null;
//        }

        // 1. 把用户的代码保存为文件;
        String userDir = System.getProperty("user.dir");
        // 为了兼容不同的系统就需要用到 File.separator
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断父级目录是否存在,没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        System.out.println(userCodeParentPath);


        // 2.编译代码，得到class文件
        String compiledCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compiledCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }

        // 3.执行代码
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=GBK -cp %s Main %s", userCodeParentPath, inputArgs);

            // System.out.println(runCmd);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 生成一个子进程
                new Thread(()->{
                    // 先让守护程序睡一会，如果主进程还没有执行完毕，就杀死主进程
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        System.out.println("超时中断");
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                // ExecuteMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }

        // 4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 默认用例使用的最长时间，便于讨论是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 执行中存在错误
                executeCodeResponse.setStatus(3);
                // todo 定义状态枚举值
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if( time != null ){
                maxTime = Math.max(maxTime,time);
            }
        }
        executeCodeResponse.setOutputList(outputList);
        //  正常运行完成，没有错误的话 将状态值设置为 1
        if(outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        // todo 可以设置每个测试用例都有一个独立时间和内存
        judgeInfo.setTime(maxTime);
        // todo 需要借助第三方库获取java程序的内存占用
        // judgeInfo.setMemory();

        executeCodeResponse.setJudgeInfo(judgeInfo);
        // 5.文件清理
        if(userCodeFile.getParentFile() != null)
        {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del ? "成功" :"失败"));
        }
        return executeCodeResponse;
    }

    /**
     * 获取错误相应
     * @param e Error
     * @return executeCodeResponse
     */

    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 2 表示代码沙箱错误 比如编译错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
