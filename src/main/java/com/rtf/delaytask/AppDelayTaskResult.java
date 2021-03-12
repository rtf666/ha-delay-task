package com.rtf.delaytask;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用延迟队列的执行结果
 */
@SuppressWarnings("all")
@Setter
@Getter
public class AppDelayTaskResult implements Serializable {

    private static final long serialVersionUID = 1L ;

    /**
     * 是否成功
     */
    private Boolean success = false ;

    /**
     * 执行时间
     */
    private Date endTime ;

}
