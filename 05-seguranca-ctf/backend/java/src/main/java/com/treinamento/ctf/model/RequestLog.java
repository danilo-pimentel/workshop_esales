package com.treinamento.ctf.model;

import java.time.LocalDateTime;

public class RequestLog {

    private Integer id;
    private String method;
    private String path;
    private String queryParams;
    private String body;
    private Integer statusCode;
    private String sqlQuery;
    private String responsePreview;
    private String ip;
    private LocalDateTime createdAt;

    public RequestLog() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getQueryParams() { return queryParams; }
    public void setQueryParams(String queryParams) { this.queryParams = queryParams; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getSqlQuery() { return sqlQuery; }
    public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }

    public String getResponsePreview() { return responsePreview; }
    public void setResponsePreview(String responsePreview) { this.responsePreview = responsePreview; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
