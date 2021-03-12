package com.rtf.delaytask.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis的分布式锁服务
 * @Author : liupeng
 * @Date : 2020-01-23
 * @Modified By
 */
@Slf4j
public class RedisLocker implements DistributedLocker {

    // Redis获取锁:setnx+设置过期时间的执行命令
    private final static String LUA_SCRIPT_LOCK = "return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) ";

    private static RedisScript<String> scriptLock = new DefaultRedisScript<String>(LUA_SCRIPT_LOCK, String.class);

    //Redis释放锁：通过value判定
    private static final String LUA_SCRIPT_UNLOCK =
            "if (redis.call('GET', KEYS[1]) == ARGV[1]) "
                    + "then return redis.call('DEL',KEYS[1]) "
                    + "else " + "return 0 " + "end" ;

    private static RedisScript<Long> scriptUnlock = new DefaultRedisScript<Long>(LUA_SCRIPT_UNLOCK, Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;

    /**
     * 获取锁的key
     * @param code
     * @return
     */
    private String getLockKey(String code){
        return "lock:" + code.toLowerCase() ;
    }

    @Override
    public DistributedLock acquire(String code , Long expiresMills) {
        String flagKey = getLockKey( code ) ;
        // 生成随机的uuid
        String uuid = UUID.randomUUID().toString() ;
        // 执行lua脚本
        String result = stringRedisTemplate.execute( scriptLock ,
                stringRedisTemplate.getStringSerializer() , stringRedisTemplate.getStringSerializer() ,
                Collections.singletonList(flagKey) , uuid , expiresMills+"" ) ;

        // 获取分布式锁成功
        if(StringUtils.equalsIgnoreCase( result , "ok" )){
            DistributedLock distributedLock = new DistributedLock() ;
            distributedLock.setKey( code.toLowerCase() ) ;
            distributedLock.setValue( uuid ) ;
            return distributedLock ;
        }

        return null;
    }

    @Override
    public void release(DistributedLock distributedLock) {
        release( distributedLock , 0 ) ;
    }

    @Override
    public void release(DistributedLock distributedLock  , int afterExpireTimeMills ) {
        if( distributedLock==null || StringUtils.isBlank( distributedLock.getKey() ) ||
                StringUtils.isBlank( distributedLock.getValue() )){
            return ;
        }
        String key = getLockKey( distributedLock.getKey() ) ;
        String existValue = stringRedisTemplate.opsForValue().get( key ) ;
        if( !StringUtils.equalsIgnoreCase( existValue , distributedLock.getValue() ) ){
            log.error("key与redis中存储的key不相同，释放锁失败");
            return;
        }

        if( afterExpireTimeMills <= 0 ){
            stringRedisTemplate.delete( key ) ;
        }else{
            stringRedisTemplate.expire( key , afterExpireTimeMills , TimeUnit.MILLISECONDS ) ;
        }

//        log.info("释放redis锁成功: {} = {}" , distributedLock.getKey() , distributedLock.getValue());
    }

}
