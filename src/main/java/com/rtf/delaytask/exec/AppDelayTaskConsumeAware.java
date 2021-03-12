package com.rtf.delaytask.exec;

import com.rtf.delaytask.AppDelayTask;

/**
 * 任务执行阶段检查
 * @Author : liupeng
 * @Date : 2020-05-16
 * @Modified By
 */
public interface AppDelayTaskConsumeAware {

    /**
     * 标识是否执行成功
     * @param appDelayTask 原始任务
     * @param singleResult 响应对象
     * @param duration 持续时间，单位毫秒
     * @return
     */
    AppDelayTaskConsumeResult execResultCheck(AppDelayTask appDelayTask , Object singleResult , long duration) ;

    /**
     * 标识是否执行成功
     * @param appDelayTask 原始任务
     * @param result 执行是否成功
     * @param message
     * @return
     */
    void execComplete(AppDelayTask appDelayTask, boolean result , String message) ;

}
