package com.treinamento.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PaginatedResponse<T> {

    private List<T> data;
    private int page;
    private int limit;
    private long total;

    @JsonProperty("totalPages")
    private int totalPages;

    public PaginatedResponse(List<T> data, int page, int limit, long total) {
        this.data = data;
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.totalPages = (int)(total / limit);
    }

    public List<T> getData() { return data; }
    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public long getTotal() { return total; }

    @JsonProperty("totalPages")
    public int getTotalPages() { return totalPages; }
}
