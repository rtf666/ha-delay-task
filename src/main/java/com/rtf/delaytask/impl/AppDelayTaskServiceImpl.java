package com.rtf.delaytask.impl;

import com.alibaba.fastjson.JSON;
import com.rtf.delaytask.*;
import com.rtf.delaytask.config.AppDelayTaskProperties;
import com.rtf.delaytask.director.AppDelayTaskDirector;
import com.rtf.delaytask.impl.dao.AppDelayTaskDao;
import com.rtf.delaytask.impl.task.AppDelayTaskUpdateThread;
import com.rtf.delaytask.lock.DistributedLock;
import com.rtf.delaytask.lock.DistributedLocker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AppDelayTaskServiceImpl implements AppDelayTaskService, InitializingBean {

	// 记录任务失败次数
	private static String TASK_FAIL_NUM = "task:fail:num";

	private ThreadPoolExecutor updateThreadPoolExecutor = null;

	/**
	 * 调度的时间间隔
	 */
	public static final int SCHEDULE_PERIOD = 2;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private AppDelayTaskDao appDelayTaskDao ;

	@Autowired
	private AppDelayTaskProperties appDelayTaskProperties;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private DistributedLocker distributedLocker;

	@Autowired
	private AppDelayTaskDirector appDelayTaskDirector;

	@Autowired
	private AppDelayTaskLogService appDelayTaskLogService;

	@Transactional
	@Override
	public void stopDelayTask(Long delayTaskId) {
		if (delayTaskId == null) {
			return;
		}
		AppDelayTask appDelayTask = get(delayTaskId);
		if (appDelayTask == null || !appDelayTask.getCircle()) {
			return;
		}
		// 更新延迟任务的循环状态
		appDelayTask.setCircle(false);

		this.appDelayTaskDao.updateCircle( appDelayTask.getId() , appDelayTask.getCircle() ) ;
//		this.update(appDelayTask, Lists.newArrayList("circle"));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 初始化更新任务的线程
		updateThreadPoolExecutor = new ThreadPoolExecutor(2, 2, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(100));

		updateThreadPoolExecutor.submit(new AppDelayTaskUpdateThread(applicationContext));
	}

	@Transactional
	@Override
	public Long createDelayTask(String businessId, String url, Map<String, Object> params) {
		if (StringUtils.isBlank(url)) {
			return 0L;
		}
		AppDelayTask appDelayTask = new AppDelayTask();
		appDelayTask.setBusinessId(businessId);
		appDelayTask.setUrl(url);
		appDelayTask.setParams(params != null ? JSON.toJSONString(params) : null);

		return createDelayTask(appDelayTask);
	}

	@Transactional
	@Override
	public Long createLocalDelayTask(String businessId, String beanName, Map<String, Object> params) {
		if (StringUtils.isBlank(beanName)) {
			return 0L;
		}
		AppDelayTask appDelayTask = new AppDelayTask();
		appDelayTask.setBusinessId(businessId);
		appDelayTask.setUrl(beanName);
		appDelayTask.setParams(params != null ? JSON.toJSONString(params) : null);
		appDelayTask.setType("local");
		appDelayTask.setMaxRetry(0);
		return createDelayTask(appDelayTask);
	}

	@Transactional
	@Override
	public Long createDelayTask(String url, Map<String, Object> params) {
		return createDelayTask(null, url, params);
	}

	@Transactional
	@Override
	public Long createDelayTask(AppDelayTask delayTask) {
		Assert.notNull(delayTask, "对象不能为空");

		//        delayTask.setFailReason( null ) ;
		delayTask.setRetryNum(0);

		// 设置为未完成，未完成状态
		delayTask.setComplete(0);
		delayTask.setSuccess(false);

		// 设置最大重试次数，为空或小于0，则设置为默认最大重试次数
		if (delayTask.getMaxRetry() == null || delayTask.getMaxRetry() < 0) {
			delayTask.setMaxRetry(appDelayTaskProperties.getMaxRetry());
		}
		// 失败任务执行时间间隔，不能小于最小时间间隔
		if (delayTask.getDelayStep() == null || delayTask.getDelayStep() < appDelayTaskProperties.getMinDelayStep()) {
			delayTask.setDelayStep(appDelayTaskProperties.getMinDelayStep());
		}

		// 设置延迟队列名称，暂不允许自定义
		delayTask.setQueueName(appDelayTaskProperties.getDelayQueueName());
		if (delayTask.getStartDelay() == null || delayTask.getStartDelay() < 1) {
			delayTask.setStartDelay(0);
		}

		// 保存任务
		delayTask = appDelayTaskDao.save(delayTask);

		pushToDelayQueue(delayTask, 0);

		return delayTask.getId();
	}

	/**
	 * 将延迟任务放到等待队列
	 * @param delayTask
	 */
	private void pushToDelayQueue(AppDelayTask delayTask, long failNum) {
		Assert.hasText(delayTask.getQueueName(), "延迟队列名称不能为空");
		// 排序分数使用当前时间
		double score = 0;
		if (delayTask.getPriority() == null) {
			delayTask.setPriority(AppDelayTaskPriority.MIDDLE);
		}
		if (failNum < 1) {
			score = Long.parseLong(delayTask.getPriority().getRemark() + (System.currentTimeMillis() + delayTask.getStartDelay() * 1000));
		} else {
			score = Long.parseLong(AppDelayTaskPriority.LOW.getRemark() + (System.currentTimeMillis() + failNum * delayTask.getDelayStep() * 1000));
		}

		score = score - (SCHEDULE_PERIOD * 1000);

		//        // 删除已有的延迟任务，避免无法正常创建
		//        if( deleteExist ){
		//            appDelayTaskDirector.deleteDelayTask( delayTask ) ;
		//        }
		appDelayTaskDirector.createTask(delayTask, score);
	}

	/**
	 * 获取失败任务的key
	 * @param delayTask
	 * @return
	 */
	protected String getFailNumCacheKey(AppDelayTask delayTask) {
		return TASK_FAIL_NUM + ":" + delayTask.getId();
	}

	/**
	 * 是否之后重试
	 * @param delayTask
	 * @return 返回是否继续重试
	 */
	@Override
	public boolean retryAfterFail(AppDelayTask delayTask) {

		String taskFailNumKey = getFailNumCacheKey(delayTask);
		long failNums = stringRedisTemplate.opsForValue().increment(taskFailNumKey, 1);

		// 记录key的失效时间 , 为了保证不失效，失败次数存储1小时
		stringRedisTemplate.expire(taskFailNumKey, 1, TimeUnit.HOURS);

		// 最大重试次数，小于实际失败次数
		boolean continueRetry = failNums <= delayTask.getMaxRetry();
		// 设置重试次数
		delayTask.setRetryNum(Long.valueOf(failNums).intValue());
		// 是否继续重试
		if (continueRetry) {
			// 允许重试，则保留失败任务数量1个小时
			pushToDelayQueue(delayTask, failNums);
		}

		return continueRetry;
	}

	/**
	 * 恢复任务
	 * @param delayTaskId
	 * @return 返回是否继续重试
	 */
	@Transactional
	public void resumeDelayTask(Long delayTaskId) {
		if (delayTaskId == null) {
			return;
		}

		AppDelayTask appDelayTask = get(delayTaskId);
		if (appDelayTask == null) {
			return;
		}

		pushToDelayQueue(appDelayTask, 0);
	}

	@Scheduled(fixedRate = 1000 * SCHEDULE_PERIOD)
	public void scheduleDelayTask() {
		if (!appDelayTaskProperties.getEnableScheduleTask()) {
			return;
		}
		// 获取延迟队列的分布式锁，默认20秒
		DistributedLock distributedLock = distributedLocker.acquire(appDelayTaskProperties.getDelayQueueName() + "lock", 20000L);
		if (distributedLock == null) {
			log.error("延迟队列获取分布式锁失败");
			return;
		}

		try {
			// 获取分数
			double score = Long.parseLong(AppDelayTaskPriority.LOW.getRemark() + System.currentTimeMillis());
			// 等待队列的个数
			//            long zSetSize = stringRedisTemplate.opsForZSet().size( appDelayTaskProperties.getDelayQueueName() ) ;
			//            log.info("等待队列中任务个数: {}" , zSetSize);

			// 清理已经获取到的延时队列
			appDelayTaskDirector.dispatchTasks(score);

		} catch (Exception e) {
			log.error("获取等待队列任务异常: {}", e.getMessage());
		} finally {
			// 释放分布式锁
			distributedLocker.release(distributedLock);
		}
	}

	/**
	 * 查询任务最新的运行状态
	 * @param delayTaskId
	 * @return
	 */
	@Override
	public AppDelayTaskResult queryLastExecResult(Long delayTaskId) {
		if (delayTaskId == null) {
			return null;
		}

		String resultJson = stringRedisTemplate.opsForValue().get(appDelayTaskProperties.getExecResultName() + ":" + delayTaskId);

		AppDelayTaskResult appDelayTaskResult = null;
		if (StringUtils.isNotBlank(resultJson)) {
			appDelayTaskResult = JSON.parseObject(resultJson, AppDelayTaskResult.class);
		}

		return appDelayTaskResult;
	}

	@Override
	public AppDelayTask get(Long delayTaskId) {
		Optional<AppDelayTask> result = appDelayTaskDao.findById( delayTaskId ) ;

		return result.isPresent() ? result.get() : null ;
	}

	@Override
	public void completeDelayTask(boolean execSuccess, AppDelayTask appDelayTask, String error) {
		// 1. 设置任务执行完成的信息
		appDelayTask.setSuccess(execSuccess);
		appDelayTask.setFailReason(error != null && error.length() > 1000 ? error.substring(0, 1000) : error);
		appDelayTask.setEndTime(new Date());

		// 2. 如果执行过重试，则清理记录的失败次数信息
		if (appDelayTask.getRetryNum() > 0) {
			String taskFailNumKey = getFailNumCacheKey(appDelayTask);
			stringRedisTemplate.delete(taskFailNumKey);
		}

		// 2.1 保存最新的执行结果，将执行结果保存到redis中。最新的执行结果保存60分钟
		AppDelayTaskResult delayTaskResult = new AppDelayTaskResult();
		delayTaskResult.setSuccess(execSuccess);
		delayTaskResult.setEndTime(appDelayTask.getEndTime());
		stringRedisTemplate.opsForValue().set(appDelayTaskProperties.getExecResultName() + ":" + appDelayTask.getId(),
				JSON.toJSONString(delayTaskResult), 60, TimeUnit.MINUTES);

		// 2.2 将已经执行完成的任务推送到更新队列
		if (StringUtils.equalsIgnoreCase(appDelayTaskProperties.getExecLogLevel(), "all")
				|| (StringUtils.equalsIgnoreCase(appDelayTaskProperties.getExecLogLevel(), "error") && !appDelayTask.getSuccess())) {
			stringRedisTemplate.opsForList().rightPush(appDelayTaskProperties.getUpdateQueueName(), JSON.toJSONString(appDelayTask));
		}

		// 3. 查询最新的任务信息是否为循环任务。如果任务存在并且是循环任务，则更新当前的任务状态
		AppDelayTask currentAppDelayTask = get(appDelayTask.getId());

		// 4. 将完成的任务推送的更新队列
		appDelayTask.setCircle(false);
		if (currentAppDelayTask != null) {
			// 4.1 如果是循环任务则重新推送
			if (currentAppDelayTask.getCircle()) {
				appDelayTask.setCircle(true);
				pushToDelayQueue(currentAppDelayTask, 0);
			}
		}

	}

	@Transactional
	@Override
	public void syncDelayTaskToDB(AppDelayTask appDelayTask) {
		// 1. 如果不是循环任务，则在任务执行完成之后设置为完成状态
		if (!appDelayTask.getCircle()) {
			appDelayTask.setComplete(1);

			this.appDelayTaskDao.updateComplete( appDelayTask.getId() , appDelayTask.getComplete() ) ;
//			this.update(appDelayTask, Lists.newArrayList("complete"));
		}

		// 2. 保存任务执行日志。针对all 或 error级别的日志记录
		if (StringUtils.equalsIgnoreCase(appDelayTaskProperties.getExecLogLevel(), "all")
				|| (StringUtils.equalsIgnoreCase(appDelayTaskProperties.getExecLogLevel(), "error") && !appDelayTask.getSuccess())) {
			appDelayTaskLogService.saveDelayTaskLog(appDelayTask);
		}
	}

}