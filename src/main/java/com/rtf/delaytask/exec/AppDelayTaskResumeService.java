package com.rtf.delaytask.exec;

import com.alibaba.fastjson.JSON;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.config.AppDelayQueueProperties;
import com.rtf.delaytask.impl.AppDelayTaskServiceImpl;
import com.rtf.delaytask.lock.DistributedLock;
import com.rtf.delaytask.lock.DistributedLocker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Set;

/**
 * 延迟任务恢复线程
 * @Author : liupeng
 * @Date : 2020-06-10
 * @Modified By
 */
@Slf4j
public class AppDelayTaskResumeService {
    // 恢复任务检查时间间隔
    public static final Long RESUME_CHECK_INTERVAL  = 1000*60*2L ;

    @Autowired
    private AppDelayQueueProperties delayQueueProperties ;

    @Autowired
    private AppDelayTaskServiceImpl delayTaskService ;

    @Autowired
    private AppDelayTaskConsumerRecord appDelayTaskConsumerRecord ;

    @Autowired
    private DistributedLocker distributedLocker ;

    /**
     * 每隔固定时间扫描一次未执行完成的任务
     */
    @Scheduled(fixedRate = 1000*60*2)
    public void scheduleDelayTask(){
        // 获取延迟队列的分布式锁，默认20秒
        DistributedLock distributedLock = distributedLocker.acquire(
                delayQueueProperties.getDelayQueueName()+"resumelock" , RESUME_CHECK_INTERVAL - 1000 ) ;
        if( distributedLock == null ){
            log.error("延迟任务恢复获取分布式锁失败" );
            return;
        }

        try{
            // 时间追溯到2分钟前，查询最近2分钟内一直未执行完成的任务
            long score = System.currentTimeMillis() - RESUME_CHECK_INTERVAL ;
            // 延迟任务
            Set<String> delayTaskJsons = appDelayTaskConsumerRecord.getResumeTasks( score ) ;
            if( delayTaskJsons!=null && delayTaskJsons.size()>0 ){
                log.info("待恢复的任务数量:{}" , delayTaskJsons==null || delayTaskJsons.size()<1 ? 0 : delayTaskJsons.size() ) ;
            }

            if( delayTaskJsons==null || delayTaskJsons.size()<1 ){
                return;
            }

            try{
                for (String delayTaskJson : delayTaskJsons) {
                    AppDelayTask appDelayTask = JSON.parseObject( delayTaskJson , AppDelayTask.class ) ;
                    delayTaskService.resumeDelayTask( appDelayTask.getId() ) ;
                }
            }catch( Exception e ){
                log.error( "恢复任务失败: {}" , e.getClass().getName()+",message:"+e.getMessage()) ;
            }

            // 移除已经恢复的任务
            appDelayTaskConsumerRecord.removeResumedTasks( delayTaskJsons ) ;

        }catch( Exception e ){
            log.error("等待队列任务异常: {}" , e.getMessage() ) ;
        }finally {
            // 释放分布式锁
            distributedLocker.release( distributedLock ) ;
        }
    }


}
