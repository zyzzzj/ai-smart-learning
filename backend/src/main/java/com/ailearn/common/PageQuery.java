package com.ailearn.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基类。
 */
@Data
public class PageQuery implements Serializable {

    /** 页码,从 1 开始 */
    private long page = 1;
    /** 每页条数 */
    private long size = 10;

    public long getPage() {
        return page < 1 ? 1 : page;
    }

    public long getSize() {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 200);
    }
}
