package com.rtf.delaytask.exec;

import com.alibaba.fastjson.JSONObject;

/**
 *  
 * @Description 本地延迟任务接口
 * @ClassName:  AppDelayTaskLocalService
 * @author: sfl
 * @date:   2020-11-11 19:52:24
 * @since:  v1.0
 */
public interface AppLocalDelayTaskService {

	/**
	 * 执行本地任务
	 * @param json
	 */
	void execute(JSONObject json);
}
