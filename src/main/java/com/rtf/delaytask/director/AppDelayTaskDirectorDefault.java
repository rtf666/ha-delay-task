package com.rtf.delaytask.director;

import com.alibaba.fastjson.JSON;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.config.AppDelayTaskProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

/**
 * 延迟队列引导任务
 * @Author : liupeng
 * @Date : 2020-05-16
 * @Modified By
 */
@Slf4j
public class AppDelayTaskDirectorDefault implements AppDelayTaskDirector {

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;

    @Autowired
    private AppDelayTaskProperties appDelayTaskProperties ;

    /**
     * 获取任务的唯一标识
     * @param appDelayTask
     * @return
     */
    protected String getDelayTaskId(AppDelayTask appDelayTask){
        if( appDelayTask==null ){
            return "" ;
        }

        return appDelayTask.getId()+"";
    }

    @Override
    public void createTask( AppDelayTask delayTask , double score ){
        // 校验待创建的任务数据是否为空，如果为空则不提交任务
        if( delayTask == null ){
            return;
        }

//        if( hasDelayTask( delayTask ) ){
//            log.info("已经存在延迟任务:{}" , JSON.toJSONString( delayTask )) ;
//            return ;
//        }

        String delayTaskJson = JSON.toJSONString( delayTask ) ;
        if( StringUtils.isBlank( delayTaskJson ) || StringUtils.equalsIgnoreCase( delayTaskJson , "null" ) ){
            return;
        }

        stringRedisTemplate.opsForZSet().add( delayTask.getQueueName() , delayTaskJson , score ) ;

        // 存储已经创建&待运行的任务
//        stringRedisTemplate.opsForSet().add( TASK_IDS ,  getDelayTaskId( delayTask ) ) ;

        try{
            doAfterTaskCreated( delayTask ) ;
        }catch( Exception e ){
            log.error( "任务创建完成之后执行doAfterTaskCreated异常 : {} , {} " , JSON.toJSONString( delayTask ) , e.getMessage() ) ;
        }
    }

    /**
     * 任务创建完成会处理
     * @param delayTask
     */
    public void doAfterTaskCreated( AppDelayTask delayTask ){
//        log.info("创建任务: {} , {}" , delayTask.getId() , delayTask.getRetryNum());
    }

    @Override
    public void dispatchTasks(double score) {
        // 获取延迟任务列表 , 最多获取100个
        Set<String> delayTasks = stringRedisTemplate.opsForZSet().rangeByScore(
                appDelayTaskProperties.getDelayQueueName() , 0 ,score , 0 ,100) ;
        if( delayTasks==null || delayTasks.size()<1 ){
            return ;
        }

        // 将延迟任务加到执行队列
        stringRedisTemplate.opsForList().rightPushAll( appDelayTaskProperties.getExecQueueName() , delayTasks ) ;

        // 清理已经获取到的延时队列
        for (String delayTask : delayTasks) {
            // 移除延迟队列和等待任务中的id
            AppDelayTask appDelayTask = JSON.parseObject( delayTask , AppDelayTask.class ) ;

            // 任务分派之后就把当前待运行中的任务删除
//            deleteDelayTask( appDelayTask ) ;

            // 删除待分配的任务
            stringRedisTemplate.opsForZSet().remove( appDelayTaskProperties.getDelayQueueName() , delayTask ) ;

            try{
                // 派发任务后，执行任务
                doAfterTaskDispatched( appDelayTask , appDelayTask.getRetryNum() + 1 ) ;
            }catch( Exception e ){
                log.error( "任务分配完成之后执行doAfterTaskDispatched异常 : {} , {} " , delayTask , e.getMessage() ) ;
            }
        }
    }

    /**
     * 任务分派完成之后的处理，如果失败任务允许重复执行，任务可能被触发多次
     * @param appDelayTask
     * @param dispatchCount 分派次数，分派次数从1开始。可以在任务首次派发后，创建循环任务；
     */
    public void doAfterTaskDispatched( AppDelayTask appDelayTask , int dispatchCount ){
//        log.info("{} 被分派次数: {}" , appDelayTask.getId() , dispatchCount);
    }

}
