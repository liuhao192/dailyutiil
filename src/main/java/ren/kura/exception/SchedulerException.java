package com.chq.qft.inspect.exception;



/**
 * 定时器的相关错误
 * @author: liuha
 * @Date: 2018/8/30
 * @Time: 9:45
 * @Description: liuha 2018/8/30 9:45
 *
 */
public class SchedulerException extends RuntimeException {
    public SchedulerException(String message) {
        super(message);
    }

    public SchedulerException(String message, Throwable cause) {
        super(message, cause);
    }
}
