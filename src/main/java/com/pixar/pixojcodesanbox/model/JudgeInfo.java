package com.pixar.pixojcodesanbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 程序执行的信息
     */
    private String message;

    /**
     * 消耗内存（kb）
     */
    private long memory;

    /**
     * 消耗时间
     */
    private long time;
}
