package com.pixar.pixojcodesanbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 写服务器文件（文件泄露） 植入危险程序
 */
public class  WriteFileError {
    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        String errorProgram = "java-version 2>&1";
        Files.write(Paths.get(filePath), Collections.singletonList(errorProgram));
        System.out.println("写木马成功，你完了,嘻嘻嘻！");
    }
}
