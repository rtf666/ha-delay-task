package com.rtf.delaytask;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 应用延迟队列
 */
@SuppressWarnings("all")
@Entity
@Table(name = "app_delay_queue")
@Setter
@Getter
public class AppDelayTask implements Serializable {

	private static final long serialVersionUID = 1L ;

	@Id
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@GeneratedValue(generator = "system-uuid")
	@Column
	protected Long id ;

	/**
	 * 队列名称
	 */
	@Column(name = "queue_name")
	private String queueName;

	/**
	 * 业务系统的id
	 */
	@Column(name = "business_id")
	private String businessId;

	/**
	 * 请求类型，默认为http
	 */
	@Column(name = "type")
	private String type = "http";

	/**
	 * 请求url
	 */
	@Column(name = "url", length = 1000)
	private String url;

	/**
	 * 请求方法，默认为post请求
	 */
	@Column(name = "method")
	private String method = "post";

	/**
	 * 内容类型， json 或 form表单
	 */
	@Column(name = "content_type")
	private String contentType = "json";

	/**
	 * 请求参数
	 */
	@Column(name = "params", columnDefinition = "text")
	private String params;

	/**
	 * 初始延迟，单位秒，默认为0
	 */
	@Column(name = "start_delay")
	private Integer startDelay = 0;

	/**
	 * 延迟步长，单位秒
	 */
	@Column(name = "delay_step")
	private Integer delayStep = 10;

	/**
	 * 最大重试次数
	 */
	@Column(name = "max_retry")
	private Integer maxRetry = 10;

	/**
	 * 队列重试次数
	 */
	@Column(name = "retry_queue_num")
	private Integer retryQueueNum = 0;

	/**
	 * 标识是否完成，0未完成，1完成（包括失败和成功）
	 */
	@Column(name = "complete")
	private Integer complete = 0;

	/**
	 * 优先级 越小优先
	 */
	@Column(name = "priority")
	private AppDelayTaskPriority priority = AppDelayTaskPriority.MIDDLE;

	/**
	 * 是否为循环
	 */
	@Column(name = "circle")
	private Boolean circle = false;

	/**
	 * 请求来源数据
	 */
	@Column(name = "source", length = 1000)
	private String source = "";

	/**
	 * 实际重试次数
	 */
	@Transient
	private Integer retryNum = 0;

	/**
	 * 是否成功，不用于存储数据库表字段
	 */
	@Transient
	private Boolean success = false;

	/**
	 * 失败原因
	 */
	@Transient
	private String failReason;

	/**
	 * 结束时间
	 */
	@Transient
	private Date endTime;

	public Boolean getCircle() {
		return this.circle == null ? false : this.circle;
	}

}
