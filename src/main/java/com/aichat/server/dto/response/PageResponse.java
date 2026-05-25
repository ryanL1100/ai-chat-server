package com.aichat.server.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 分页响应（对应前端 PaginationResponse<T>）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;
    private boolean hasMore;
}
