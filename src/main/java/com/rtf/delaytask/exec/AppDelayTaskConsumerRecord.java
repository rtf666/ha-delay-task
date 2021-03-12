package com.rtf.delaytask.exec;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rtf.delaytask.config.AppDelayTaskProperties;
import com.rtf.delaytask.lock.DistributedLock;
import com.rtf.delaytask.lock.DistributedLocker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * 记录正在进行中的任务
 * @Author : liupeng
 * @Date : 2020-06-22
 * @Modified By
 */
@Slf4j
public class AppDelayTaskConsumerRecord  {

    @Autowired
    private AppDelayTaskProperties appDelayTaskProperties ;

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;

    @Autowired
    private DistributedLocker distributedLocker ;

    /**
     * 开始消费
     * @return
     */
    public List<String> startConsumes(){
        // 1. 获取分布式锁，保证只有一个消费任务执行
        DistributedLock distributedLock = distributedLocker.acquire(appDelayTaskProperties.getExecutingQueueName()+":locker" , 30*1000L) ;
        if( distributedLock==null ){
            return Lists.newArrayListWithExpectedSize(1) ;
        }

        List<String> targetTasks = Lists.newArrayListWithExpectedSize(1) ;

        try{
            // 2.1 获取指定数量的任务
            targetTasks = stringRedisTemplate.opsForList().range( appDelayTaskProperties.getExecQueueName() ,
                    0 , appDelayTaskProperties.getConsumePrefetch() - 1 ) ;
            if( targetTasks==null || targetTasks.size()<1 ){
                return Lists.newArrayListWithExpectedSize(1) ;
            }

            // 2.2 将任务添加到正在执行的队列
            int currentIndex = 0 ;
            for (String targetTask : targetTasks) {
                if(StringUtils.isBlank( targetTask ) || StringUtils.equalsIgnoreCase( targetTask , "null" )){
                    continue;
                }
                currentIndex ++ ;
                stringRedisTemplate.opsForZSet().add( appDelayTaskProperties.getExecutingQueueName() , targetTask ,
                        System.currentTimeMillis() + currentIndex * 2000 ) ;
            }

            // 2.3 移除已有的数据
            stringRedisTemplate.opsForList().trim( appDelayTaskProperties.getExecQueueName() , targetTasks.size() , -1 ) ;
        }catch( Exception e ){
            log.error( "获取消费任务异常 : {}" , e.getMessage() ) ;
        }finally {
            distributedLocker.release( distributedLock ) ;
        }

        return targetTasks ;
    }

    /**
     * 完成消费，移除指定的消息任务
     * @param delayTaskJson
     */
    public void completeConsume(String delayTaskJson){
        stringRedisTemplate.opsForZSet().remove( appDelayTaskProperties.getExecutingQueueName() , delayTaskJson ) ;
    }

    /**
     * 获取截止到指定时间前的未执行的任务
     * @param targetTime
     * @return
     */
    public Set<String> getResumeTasks(long targetTime){
        Set<String> delayTaskJsons = stringRedisTemplate.opsForZSet().rangeByScore( appDelayTaskProperties.getExecutingQueueName() ,
                0 , targetTime , 0 , 100 ) ;
        if( delayTaskJsons==null || delayTaskJsons.size()<1 ){
            return Sets.newHashSetWithExpectedSize( 1 ) ;
        }

        return delayTaskJsons ;
    }

    /**
     * 移除已经恢复的任务
     * @param delayTaskJsons
     */
    public void removeResumedTasks(Set<String> delayTaskJsons){
        if( delayTaskJsons==null || delayTaskJsons.size()<1 ){
            return;
        }

        // 移除已经执行完成的数据
        stringRedisTemplate.opsForZSet().remove( appDelayTaskProperties.getExecutingQueueName() ,
                delayTaskJsons.toArray( new String[]{} ) ) ;

    }

}
