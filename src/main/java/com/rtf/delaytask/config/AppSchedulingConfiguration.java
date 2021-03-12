package com.rtf.delaytask.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 *
 * @Author : liupeng
 * @Date : 2020-03-11
 * @Modified By
 */
@SuppressWarnings("all")
@Slf4j
@Configuration
public class AppSchedulingConfiguration implements SchedulingConfigurer {

    @Autowired
    private AppDelayQueueProperties delayQueueProperties ;

    public AppSchedulingConfiguration(){
        log.debug("创建调度资源配置");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        //设定一个长度的定时任务线程池
        taskRegistrar.setScheduler( Executors.newScheduledThreadPool( delayQueueProperties.getConsumeTaskNum() ) ) ;
    }

}
