package com.pixar.pixojcodesanbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.pixar.pixojcodesanbox.model.ExecuteCodeRequest;
import com.pixar.pixojcodesanbox.model.ExecuteCodeResponse;
import com.pixar.pixojcodesanbox.model.ExecuteMessage;
import com.pixar.pixojcodesanbox.model.JudgeInfo;
import com.pixar.pixojcodesanbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        System.setSecurityManager(new DenySecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


        // 1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        // 2. 编译代码，得到Class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        // System.out.println(compileFileExecuteMessage);

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runCode(userCodeFile, inputList);

        // 4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5. 删除文件
        boolean b = deleteFile(userCodeFile);
        if(b){
            log.error("deleteFile Error, userCodeFile = {}",userCodeFile.getParentFile().getAbsolutePath());

        }
        return outputResponse;
    }

    /**
     * 把用户代码保存为文件
     * @param code 用户代码
     * @return userCodeFile
     */
    public File saveCodeToFile (String code){
        // 把用户代码保存成文件
        String userDir = System.getProperty("user.dir");
        // 为了兼容不同的系统就需要用到 File.separator
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;

    }

    /**
     * 编译代码文件到Class文件
     * @param userCodeFile 用户的代码文件
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile){
        //2. 编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
            if(executeMessage.getExitValue()!=0){
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            // return getErrorResponse(e);
            throw  new RuntimeException(e);
        }
    }

    /**
     * 3. 执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runCode(File userCodeFile,List<String> inputList){
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",
                    userCodeParentPath,
                    inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
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
//                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                // return getErrorResponse(e);
                throw new RuntimeException("执行错误" + e);
            }
        }
        return executeMessageList;
    }

    /**
     * 获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 默认用例使用的最长时间，便于讨论是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                // todo 定义状态枚举值
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        executeCodeResponse.setOutputList(outputList);
        //  正常运行完成，没有错误的话 将状态值设置为 1
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        // todo 可以设置每个测试用例都有一个独立时间和内存
        judgeInfo.setTime(maxTime);
        // todo 需要借助第三方库获取java程序的内存占用
        // judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if(userCodeFile.getParentFile() != null){
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }



    /**
     * 6. 获取错误相应
     *
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
