package com.rtf.delaytask.exec;

import com.rtf.delaytask.AppDelayTask;

/**
 *  
 * @Description 本地可控延迟任务接口
 * 				处理端可以修改输入参数和循环执行标志
 * @ClassName:  AppLocalControlDelayTaskService
 * @author: sfl
 * @date:   2020-11-11 19:52:24
 * @since:  v1.0
 */
public interface AppLocalControlDelayTaskService {

	void excute(AppDelayTask task);
}
