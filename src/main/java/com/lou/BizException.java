package com.lou;

public class BizException extends RuntimeException {
    private String bizMessage;

    public BizException(String bizMessage) {
        this.bizMessage = bizMessage;
    }
}
