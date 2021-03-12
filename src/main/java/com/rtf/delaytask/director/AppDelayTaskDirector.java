package com.rtf.delaytask.director;

import com.rtf.delaytask.AppDelayTask;

/**
 * 延迟队列引导任务
 * @Author : liupeng
 * @Date : 2020-05-16
 * @Modified By
 */
public interface AppDelayTaskDirector {

    // 记录任务id
    String TASK_IDS  = "task:ids" ;

    /**
     * 分配任务
     * @param score
     */
    void dispatchTasks(double score) ;

    /**
     * 创建任务
     * @param delayTask
     * @param score
     */
    void createTask(AppDelayTask delayTask , double score) ;

//    /**
//     * 删除任务
//     * @param appDelayTask
//     */
//    void deleteDelayTask(AppDelayTask appDelayTask) ;
//
//    /**
//     * 是否存在延迟任务
//     * @param appDelayTask
//     * @return
//     */
//    boolean hasDelayTask(AppDelayTask appDelayTask) ;

}
