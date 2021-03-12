package com.rtf.delaytask.exec.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.exec.AppDelayTaskConsumeResult;
import com.rtf.delaytask.exec.AppDelayTaskConsumer;
import com.rtf.delaytask.exec.AppLocalDelayTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 *  
 * @Description 本地延迟任务消费
 * @ClassName:  AppDelayTaskLocalService
 * @author: sfl
 * @date:   2020-11-11 19:52:24
 * @since:  v1.0
 */
@SuppressWarnings("all")
@Slf4j
public class AppLocalDelayTaskConsumer implements AppDelayTaskConsumer {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public String getDelayTaskType() {
		return "local";
	}

	@Override
	public AppDelayTaskConsumeResult execDelayTask(AppDelayTask appDelayTask) {
		AppDelayTaskConsumeResult consumeResult = new AppDelayTaskConsumeResult();

		try {
			AppLocalDelayTaskService localTask = applicationContext.getBean(appDelayTask.getUrl(),
					AppLocalDelayTaskService.class);
			JSONObject initParams = null;
			if (StringUtils.isNotBlank(appDelayTask.getParams()))
				initParams = JSON.parseObject(appDelayTask.getParams());
			localTask.execute(initParams);

		} catch (Exception e) {

			consumeResult.setSuccess(false);
			String message = "exception: " + e.getClass().getName() + " , message : " + e.getMessage();

			consumeResult.setMessage(message);
			log.error("本地延迟任务执行异常: {} , {}", JSON.toJSONString(appDelayTask),
					e.getClass().getName() + " , message : " + e.getMessage());
		}

		return consumeResult;
	}

}
