package com.rtf.delaytask.config;

import com.rtf.delaytask.AppDelayTaskLogService;
import com.rtf.delaytask.AppDelayTaskService;
import com.rtf.delaytask.director.AppDelayTaskDirector;
import com.rtf.delaytask.director.AppDelayTaskDirectorDefault;
import com.rtf.delaytask.exec.*;
import com.rtf.delaytask.exec.consumer.AppHttpDelayTaskConsumer;
import com.rtf.delaytask.exec.consumer.AppLocalControlDelayTaskConsumer;
import com.rtf.delaytask.exec.consumer.AppLocalDelayTaskConsumer;
import com.rtf.delaytask.impl.AppDelayTaskLogServiceImpl;
import com.rtf.delaytask.impl.AppDelayTaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 延迟队列回调。<br>
 * 测试26w+数据，单节点部署回调接口。一次重试完成的数量：21w+，重试完成的数量5k+；没有回调失败的记录。
 * @Author : liupeng
 * @Date : 2021-03-11
 * @Modified By
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "com.rtf.delaytask")
@EnableConfigurationProperties(AppDelayTaskProperties.class)
public class AppDelayTaskAutoConfiguration {

	@ConditionalOnMissingBean(AppDelayTaskService.class)
	@Bean
	public AppDelayTaskService appDelayTaskService() {
		return new AppDelayTaskServiceImpl();
	}

	@ConditionalOnMissingBean(AppDelayTaskLogService.class)
	@Bean
	public AppDelayTaskLogService appDelayTaskLogService() {
		return new AppDelayTaskLogServiceImpl();
	}

	/**
	 * 执行回调
	 * @return
	 */
	@ConditionalOnMissingBean(AppDelayTaskDirector.class)
	@Bean
	public AppDelayTaskDirector appDelayTaskDirector() {
		return new AppDelayTaskDirectorDefault();
	}

	@Bean
	public AppDelayTaskExecutor appDelayTaskExecutor(ObjectProvider<List<AppDelayTaskConsumer>> delayTaskConsumerProvider) {
		List<AppDelayTaskConsumer> appDelayTaskConsumers = delayTaskConsumerProvider.getIfAvailable();

		log.debug("延迟任务消费任务的数量: {}", appDelayTaskConsumers != null ? appDelayTaskConsumers.size() : 0);

		// 设置消费任务
		AppDelayTaskExecutor appDelayTaskExecutor = new AppDelayTaskExecutor();
		appDelayTaskExecutor.setDelayTaskConsumers(appDelayTaskConsumers);

		return appDelayTaskExecutor;
	}

	/**
	 * http请求发起
	 * @return
	 */
	@ConditionalOnMissingBean(AppHttpDelayTaskConsumer.class)
	@Bean
	public AppHttpDelayTaskConsumer appHttpDelayTaskConsumer() {
		return new AppHttpDelayTaskConsumer();
	}

	/**
	 * 本地服务发起
	 * @return
	 */
	@ConditionalOnMissingBean(AppLocalDelayTaskConsumer.class)
	@Bean
	public AppLocalDelayTaskConsumer appLocalDelayTaskConsumer() {
		return new AppLocalDelayTaskConsumer();
	}

	/**
	 * 本地服务发起
	 * 处理端可以修改输入参数和循环执行标志
	 * @return
	 */
	@ConditionalOnMissingBean(AppLocalControlDelayTaskConsumer.class)
	@Bean
	public AppLocalControlDelayTaskConsumer appLocalControlDelayTaskConsumer() {
		return new AppLocalControlDelayTaskConsumer();
	}

	/**
	 * 请求结果检查
	 * @return
	 */
	@ConditionalOnMissingBean(AppDelayTaskConsumeAware.class)
	@Bean
	public AppDelayTaskConsumeAware appDelayTaskConsumeAware() {
		return new AppDelayTaskConsumeAwareHttp();
	}

	/**
	 * 处理中的请求消费任务
	 * @return
	 */
	@Bean
	public AppDelayTaskConsumerRecord appDelayTaskConsumerRecord() {
		return new AppDelayTaskConsumerRecord();
	}

	/**
	 * 恢复未执行完成的任务
	 * @return
	 */
	@Bean
	public AppDelayTaskResumeService appDelayTaskResumeService() {
		return new AppDelayTaskResumeService();
	}

}
