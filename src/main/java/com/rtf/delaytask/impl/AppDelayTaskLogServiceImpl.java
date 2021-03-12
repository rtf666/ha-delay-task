package com.rtf.delaytask.impl;

import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.AppDelayTaskLog;
import com.rtf.delaytask.AppDelayTaskLogService;
import com.rtf.delaytask.impl.dao.AppDelayTaskLogDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class AppDelayTaskLogServiceImpl implements AppDelayTaskLogService {

    @Autowired
    private AppDelayTaskLogDao appDelayTaskLogDao ;


    @Transactional
    @Override
    public void saveDelayTaskLog(AppDelayTask delayTask) {
        AppDelayTaskLog appDelayTaskLog = new AppDelayTaskLog() ;
        // 设置任务属性
        appDelayTaskLog.setTaskId( delayTask.getId() ) ;
        appDelayTaskLog.setBusinessId( delayTask.getBusinessId() ) ;
        appDelayTaskLog.setRetryNum( delayTask.getRetryNum() ) ;
        appDelayTaskLog.setFailReason( delayTask.getFailReason() ) ;
        appDelayTaskLog.setSuccess( delayTask.getSuccess() ) ;
        appDelayTaskLog.setStartTime( delayTask.getStartTime() ) ;
        appDelayTaskLog.setEndTime( delayTask.getEndTime() ) ;

        appDelayTaskLogDao.save( appDelayTaskLog ) ;
    }
}