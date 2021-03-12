package com.rtf.delaytask.exec;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.AppDelayTaskService;
import com.rtf.delaytask.config.AppDelayQueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 延迟任务消费
 */
@SuppressWarnings("all")
@Slf4j
public class AppDelayTaskConsumerThread extends Thread {

//    private RedisScript<String> scriptConsume = new DefaultRedisScript<String>(AppDelayTaskConsumerRecord.CONSUME_SCRIPT, String.class) ;

    private AppDelayQueueProperties delayQueueProperties ;

    private StringRedisTemplate stringRedisTemplate ;

    private AppDelayTaskService delayTaskService ;

    private AppDelayTaskConsumeAware appDelayTaskConsumeAware ;

    private Map<String, AppDelayTaskConsumer> appDelayTaskConsumers ;

    private AppDelayTaskConsumerRecord appDelayTaskConsumerRecord ;

    private LinkedList<String> delayTaskJsons = Lists.newLinkedList() ;

    public AppDelayTaskConsumerThread(String name ,
                                      ApplicationContext applicationContext ,
                                      Map<String, AppDelayTaskConsumer> appDelayTaskConsumers ){
        super(name) ;
        this.delayQueueProperties = applicationContext.getBean( AppDelayQueueProperties.class ) ;
        this.stringRedisTemplate = applicationContext.getBean( StringRedisTemplate.class ) ;
        this.delayTaskService = applicationContext.getBean( AppDelayTaskService.class ) ;
        this.appDelayTaskConsumeAware = applicationContext.getBean( AppDelayTaskConsumeAware.class ) ;
        this.appDelayTaskConsumers = appDelayTaskConsumers ;
        this.appDelayTaskConsumerRecord =  applicationContext.getBean( AppDelayTaskConsumerRecord.class ) ;
    }

    /**
     * 获取消费任务
     * @param appDelayTask
     * @return
     */
    public AppDelayTaskConsumer getAppDelayTaskConsumer(AppDelayTask appDelayTask){
        String type = StringUtils.isBlank( appDelayTask.getType() ) ? "http" : appDelayTask.getType().toLowerCase().trim() ;
        return this.appDelayTaskConsumers.get( type ) ;
    }

    /**
     * 查询需要执行的任务
     * @return
     */
    public String queryTodoAppDelayTask(){
        String delayTaskJson = null ;
        // 任务列表为空，则重新获取列表
        if( delayTaskJsons==null || delayTaskJsons.size()<1 ){
            List<String> consumeTasks = appDelayTaskConsumerRecord.startConsumes() ;
            if( consumeTasks!=null && consumeTasks.size()>0 ){
                delayTaskJsons.addAll( consumeTasks ) ;
            }
        }

        // 获取一条任务数据
        if( delayTaskJsons.size()>0 ){
            delayTaskJson = delayTaskJsons.pop() ;
        }

        return delayTaskJson ;
    }

    @Override
    public void run() {
        while (true){
            AppDelayTask appDelayTask = null ;
            String delayTaskJson = null ;
            // 1. 获取需要执行的延迟任务
            try{
                delayTaskJson = queryTodoAppDelayTask() ;
                if( StringUtils.isBlank( delayTaskJson ) || StringUtils.equalsIgnoreCase( delayTaskJson , "null" ) ){
                    Thread.currentThread().sleep( delayQueueProperties.getEmptyTaskInterval() );
                    continue;
                }
                appDelayTask = JSON.parseObject( delayTaskJson , AppDelayTask.class ) ;
//                // 2. 设置任务的实际开始时间
//                appDelayTask.setStartTime( new Date()) ;
            }catch( Exception e ){
                log.error( "获取消费任务失败: {}" , e.getClass().getName()+",message:"+e.getMessage() ) ;
                continue;
            }

//            log.info("执行任务-> {}" , appDelayTask.getBusinessId()) ;

            // 2. 执行任务回调
            try{
                // 2.1 发起请求
                AppDelayTaskConsumer appDelayTaskConsumer = getAppDelayTaskConsumer( appDelayTask ) ;
                AppDelayTaskConsumeResult consumeResult = appDelayTaskConsumer.execDelayTask( appDelayTask ) ;
                // 2.2 重试或完成
                retryOrCompleteTask( consumeResult.isSuccess() , appDelayTask , consumeResult.getMessage() ) ;
            }catch( Exception e ){
                retryOrCompleteTask( false , appDelayTask , "exception: "+e.getMessage() ) ;
            }finally {
                if( appDelayTask != null ){
                    appDelayTaskConsumerRecord.completeConsume( delayTaskJson ) ;
                }
            }

            try{
                Thread.currentThread().sleep( delayQueueProperties.getConsumeInterval() ) ;
            }catch( Exception e ){
                log.error( "任务执行完成后等待异常: {}" , e.getMessage() ) ;
            }
        }
    }

    /**
     * 记录
     * @param execSuccess
     * @param appDelayTask
     * @param error
     */
    public void retryOrCompleteTask(boolean execSuccess , AppDelayTask appDelayTask , String error){
        if( !execSuccess ){
            // 执行失败，判断是否需要重试
            boolean retry = delayTaskService.retryAfterFail( appDelayTask ) ;
            if( !retry ){
                delayTaskService.completeDelayTask( execSuccess, appDelayTask, error ) ;
                // 记录延迟任务结果
                try{
                    appDelayTaskConsumeAware.execComplete( appDelayTask , appDelayTask.getSuccess() , error ) ;
                }catch( Exception e ){
                    log.error( "完成消费任务失败 : {}" , e.getMessage() ) ;
                }
            }
        }else{
            // 记录完成状态信息
            delayTaskService.completeDelayTask( execSuccess, appDelayTask, error ) ;
            // 记录延迟任务结果
            try{
                appDelayTaskConsumeAware.execComplete( appDelayTask , appDelayTask.getSuccess()  , error ) ;
            }catch( Exception e ){
                log.error( "完成消费任务失败 : {}" , e.getMessage() ) ;
            }
        }

    }
}
