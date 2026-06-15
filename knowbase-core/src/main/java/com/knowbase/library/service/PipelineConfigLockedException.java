package com.knowbase.library.service;

public class PipelineConfigLockedException extends RuntimeException {

    public PipelineConfigLockedException() {
        super("库内已有分块数据，索引管道配置不可修改");
    }
}
