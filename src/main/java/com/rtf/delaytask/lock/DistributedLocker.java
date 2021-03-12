package com.rtf.delaytask.lock;

/**
 * 分布式锁服务<br>
 * @Author : liupeng
 * @Date : 2020-01-23
 * @Modified By
 */
public interface DistributedLocker {

    /**
     * 获取分布式锁
     * @param code
     * @param expiresMills 失效时间，毫秒
     * @return
     */
    DistributedLock acquire(String code , Long expiresMills) ;

    /**
     * 释放分布式锁
     * @param redisLock
     */
    void release(DistributedLock redisLock) ;

    /**
     * 释放分布式锁
     * @param redisLock
     * @param afterExpireTimeMills
     */
    void release(DistributedLock redisLock  , int afterExpireTimeMills ) ;

}
