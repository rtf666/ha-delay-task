package com.rtf.delaytask;

/**
 * 延迟队列服务的执行日志
 * @Author : liupeng
 * @Date : 2020-07-15
 * @Modified By
 */
public interface AppDelayTaskLogService {

    /**
     * 创建延迟队列的日志
     * @param delayTask
     * @return 返回任务的id
     */
    void saveDelayTaskLog(AppDelayTask delayTask) ;

}
