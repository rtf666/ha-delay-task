package com.rtf.delaytask.lock;

import lombok.Data;

import java.io.Serializable;

/**
 * 分布式锁
 * @Author : liupeng
 * @Date : 2020-01-23
 * @Modified By
 */
@Data
public class DistributedLock implements Serializable {

    private static final long serialVersionUID = 1L ;

    private String key ;

    private String value ;

}
