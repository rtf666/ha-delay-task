package com.rtf.delaytask.exec.consumer;

import com.alibaba.fastjson.JSON;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.exec.AppDelayTaskConsumeResult;
import com.rtf.delaytask.exec.AppDelayTaskConsumer;
import com.rtf.delaytask.exec.AppLocalControlDelayTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 *  
 * @Description 本地延迟任务消费
 * 				处理端可以修改输入参数和循环执行标志
 * @ClassName:  AppLocalControlDelayTaskConsumer
 * @author: sfl
 * @date:   2020-11-11 19:52:24
 * @since:  v1.0
 */
@SuppressWarnings("all")
@Slf4j
public class AppLocalControlDelayTaskConsumer implements AppDelayTaskConsumer {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public String getDelayTaskType() {
		return "local_control";
	}

	@Override
	public AppDelayTaskConsumeResult execDelayTask(AppDelayTask appDelayTask) {
		AppDelayTaskConsumeResult consumeResult = new AppDelayTaskConsumeResult();

		try {
			AppLocalControlDelayTaskService localTask = applicationContext.getBean(appDelayTask.getUrl(), AppLocalControlDelayTaskService.class);
			localTask.execute(appDelayTask);

		} catch (Exception e) {

			consumeResult.setSuccess(false);
			String message = "exception: " + e.getClass().getName() + " , message : " + e.getMessage();

			consumeResult.setMessage(message);
			log.error("本地延迟任务执行异常: {} , {}", JSON.toJSONString(appDelayTask), e.getClass().getName() + " , message : " + e.getMessage());
		}

		return consumeResult;
	}

}
