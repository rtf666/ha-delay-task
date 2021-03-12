package com.rtf.delaytask.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.delayqueue")
public class AppDelayQueueProperties {

    /**
     * 是否开启延迟队列的消费任务
     */
    private Boolean enableConsume = false ;

    /**
     * 是否开启延迟队列的调度任务
     */
    private Boolean enableScheduleTask = false ;

    /**
     * 是否开启对于失败任务进行重试的调度任务
     */
    private Boolean enableScheduleRetryFailTask = false ;

    /**
     * 提前取得任务的数量
     */
    private Integer consumePrefetch = 10 ;

    /**
     * 消费任务线程数量
     */
    private Integer consumeTaskNum = 20 ;

    /**
     * 延迟队列名称
     */
    private String delayQueueName = "delay:tasks";

    /**
     * 更新list的名称
     */
    private String updateQueueName = "delay:update:tasks";

    /**
     * 更新锁的有效时间，单位秒
     */
    private Integer updateLockTimeout = 2*60 ;

    /**
     * 更新每次更新获取记录数量
     */
    private Integer updateRecordSize = 100 ;

    /**
     * db更新的间隔，单位毫秒
     */
    private Integer updateDbInterval = 50 ;

    /**
     * 记录日志的级别，包括：all(所有日志)、error(错误日志)
     */
    private String execLogLevel = "error" ;

    /**
     * 执行队列的名称
     */
    private String execQueueName = "delay:exec:tasks";

    /**
     * 执行结果的名称
     */
    private String execResultName = "delay:exec:result";

    /**
     * 正在执行的任务名称
     */
    private String executingQueueName = "delay:executing:tasks";

    /**
     * 任务消费执行的时间间隔，单位毫秒
     */
    private Integer consumeInterval = 1000 ;

    /**
     * 空任务时，等待的时间间隔
     */
    private Integer emptyTaskInterval = 2000 ;

    /**
     * 最小延迟步长，单位秒
     */
    private Integer minDelayStep = 10 ;

    /**
     * 最大重试次数
     */
    private Integer maxRetry = 10 ;

    /**
     * http连接超时时间
     */
    private Integer httpConnectTimeout = 5 ;

    /**
     * http读超时时间
     */
    private Integer httpReadTimeout = 5 ;



}
