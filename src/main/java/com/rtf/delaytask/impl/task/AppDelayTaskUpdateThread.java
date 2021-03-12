package com.rtf.delaytask.impl.task;

import com.alibaba.fastjson.JSON;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.AppDelayTaskService;
import com.rtf.delaytask.config.AppDelayTaskProperties;
import com.rtf.delaytask.lock.DistributedLock;
import com.rtf.delaytask.lock.DistributedLocker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 执行更新任务
 * @Author : liupeng
 * @Date : 2020-06-21
 * @Modified By
 */
@Slf4j
public class AppDelayTaskUpdateThread extends Thread {

    private AppDelayTaskService appDelayTaskService ;

    private StringRedisTemplate stringRedisTemplate ;

    private AppDelayTaskProperties appDelayTaskProperties ;

    private DistributedLocker distributedLocker ;

    public AppDelayTaskUpdateThread(ApplicationContext applicationContext){
        super("数据库更新任务");
        this.appDelayTaskService = applicationContext.getBean( AppDelayTaskService.class ) ;
        this.stringRedisTemplate = applicationContext.getBean( StringRedisTemplate.class ) ;
        this.appDelayTaskProperties = applicationContext.getBean( AppDelayTaskProperties.class ) ;
        this.distributedLocker = applicationContext.getBean( DistributedLocker.class ) ;
    }

    /**
     * 暂停
     */
    public void pauseAndWait(){
        try {
            Thread.currentThread().sleep( appDelayTaskProperties.getEmptyTaskInterval() );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // 每次获取元素的数量
        int updateNumOnce = appDelayTaskProperties.getUpdateRecordSize() ;

        while (true){
            DistributedLock updateLock = null ;
            try{
                updateLock = distributedLocker.acquire("dbupdate" , appDelayTaskProperties.getUpdateLockTimeout()*1000L) ;
                if( updateLock == null ){
                    pauseAndWait() ;
                    continue ;
                }
                // 1. 从redis中获取更新任务。如果获取失败，则暂停固定时间间隔
                List<String> delayTaskJsonList = stringRedisTemplate.opsForList().range( appDelayTaskProperties.getUpdateQueueName() ,
                        0 , updateNumOnce - 1 ) ;
                // 如果没有任务则等待
                if( delayTaskJsonList==null || delayTaskJsonList.size()<1 ){
                    pauseAndWait() ;
                    continue;
                }
                log.debug("执行{}记录数量:{}" , getName() , delayTaskJsonList.size());
                // 2. 解析任务对象，执行保存
                for (String delayTaskJson : delayTaskJsonList) {
                    AppDelayTask appDelayTask = JSON.parseObject( delayTaskJson , AppDelayTask.class ) ;
                    appDelayTaskService.syncDelayTaskToDB( appDelayTask ) ;
                    // 如果每次更新允许数据库存在时间间隔，则暂停
                    if( appDelayTaskProperties.getUpdateDbInterval()>0 ){
                        Thread.currentThread().sleep( appDelayTaskProperties.getUpdateDbInterval() ) ;
                    }
                }
                // 3. 移除已经更新的元素，仅保留后续的所有的元素
                stringRedisTemplate.opsForList().trim( appDelayTaskProperties.getUpdateQueueName() , delayTaskJsonList.size()  , -1 ) ;

                // 4. 等待固定的时间间隔后，再开始下一次消费
                Thread.currentThread().sleep( appDelayTaskProperties.getConsumeInterval() ) ;
            }catch( Exception e ){
                log.error( "同步更新任务状态失败: {}" , e.getClass().getName()+",message:"+e.getMessage() ) ;
            }finally {
                // 释放分布式锁
                try{
                    if( updateLock!=null ){
                        distributedLocker.release( updateLock ) ;
                    }
                }catch( Exception e ){
                    log.error( "释放分布式锁失败: {}" , e.getClass().getName()+",message:"+e.getMessage() ) ;
                }
            }
        }

    }
}
