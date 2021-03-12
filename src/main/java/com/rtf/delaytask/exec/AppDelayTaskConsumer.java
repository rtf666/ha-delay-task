package com.rtf.delaytask.exec;

import com.rtf.delaytask.AppDelayTask;

public interface AppDelayTaskConsumer {

    /**
     * 获取延迟任务类型
     * @return
     */
    String getDelayTaskType() ;

    /**
     * 执行延迟任务
     * @param appDelayTask
     * @return 返回延迟任务结果
     */
    AppDelayTaskConsumeResult execDelayTask(AppDelayTask appDelayTask) ;

}
