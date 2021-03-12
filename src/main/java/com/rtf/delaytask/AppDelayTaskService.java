package com.rtf.delaytask;

import java.util.Map;

/**
 * 延迟队列服务
 * @Author : liupeng
 * @Date : 2020-03-10
 * @Modified By
 */
public interface AppDelayTaskService {

	/**
	 * 查询最新的执行状态
	 * @param delayTaskId
	 * @return
	 */
	AppDelayTaskResult queryLastExecResult(Long delayTaskId);

	/**
	 * 根据延迟任务id获取延迟任务
	 * @param delayTaskId
	 * @return
	 */
	AppDelayTask get(Long delayTaskId);

	/**
	 * 停止延迟任务
	 * @param delayTaskId
	 */
	void stopDelayTask(Long delayTaskId);

	/**
	 * 创建延迟队列
	 * @param delayTask
	 * @return 返回任务的id
	 */
	Long createDelayTask(AppDelayTask delayTask);

	/**
	 * 创建延迟队列
	 * @param url
	 * @param params
	 * @return
	 */
	Long createDelayTask(String url, Map<String, Object> params);

	/**
	 * 创建延迟队列
	 * @param businessId 业务系统id
	 * @param url
	 * @param params
	 * @return
	 */
	Long createDelayTask(String businessId, String url, Map<String, Object> params);

	/**
	 * 创建延迟队列
	 * @param businessId 业务系统id
	 * @param beanName  spring bean name  
	 * @param params
	 * @return
	 */
	Long createLocalDelayTask(String businessId, String beanName, Map<String, Object> params);

	/**
	 * 完成任务，记录任务的失败或成功信息
	 * @param execSuccess
	 * @param appDelayTask
	 * @param error
	 */
	void completeDelayTask(boolean execSuccess, AppDelayTask appDelayTask, String error);

	/**
	 * 失败后是否重试
	 * @param delayTask
	 * @return
	 */
	boolean retryAfterFail(AppDelayTask delayTask);

	/**
	 * 同步延迟任务状态到数据库
	 * @param appDelayTask
	 */
	void syncDelayTaskToDB(AppDelayTask appDelayTask);

}
