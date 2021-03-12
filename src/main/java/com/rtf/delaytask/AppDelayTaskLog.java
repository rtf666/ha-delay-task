package com.rtf.delaytask;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 应用延迟队列的日志
 */
@SuppressWarnings("all")
@Entity
@Table(name = "app_delay_queue_log")
@Setter
@Getter
public class AppDelayTaskLog implements Serializable {

    private static final long serialVersionUID = 1L ;

    @Id
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @GeneratedValue(generator = "system-uuid")
    @Column
    protected Long id ;

    /**
     * 所属任务的id
     */
    @Column(name = "task_id")
    private Long taskId ;

    /**
     * 业务系统的id
     */
    @Column(name = "business_id")
    private String businessId ;

    /**
     * 是否成功
     */
    @Column(name = "success")
    private Boolean success = false ;

    /**
     * 实际重试次数
     */
    @Column(name = "retry_num")
    private Integer retryNum = 0 ;

    /**
     * 失败原因
     */
    @Column(name = "fail_reason" , length = 2000)
    private String failReason ;

    /**
     * 结束时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_time")
    private Date endTime ;

}
