package com.pixar.pixojcodesanbox.utils;

import com.pixar.pixojcodesanbox.model.ExecuteMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 进程工具类
 */
public class ProcessUtils {
    /**
     * 执行进程并获取信息
     *
     * @param runProcess 运行进程
     * @param opName     操作名称
     * @return 进程信息
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            // 等待程序执行，直到程序运行完成得到一个错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            //正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 分批获取控制台的输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                // 整体输出
                System.out.println(compileOutputStringBuilder);
            } else {
                //异常退出
                System.out.println(opName + "失败，错误码：" + exitValue);
                // 分批获取程序的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());

                // 分批获取控制台的输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeMessage;
    }

}
