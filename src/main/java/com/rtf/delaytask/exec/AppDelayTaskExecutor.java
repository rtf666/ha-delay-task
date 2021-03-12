package com.rtf.delaytask.exec;

import com.rtf.delaytask.config.AppDelayTaskProperties;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AppDelayTaskExecutor implements InitializingBean {

    @Autowired
    private AppDelayTaskProperties appDelayTaskProperties ;

    private ThreadPoolExecutor threadPoolExecutor = null ;

    private Map<String,AppDelayTaskConsumer> appDelayTaskConsumers = Maps.newHashMap();

    @Autowired
    private ApplicationContext applicationContext ;

    @Override
    public void afterPropertiesSet() throws Exception {
        startConsumeTask() ;
    }

    public void setDelayTaskConsumers(List<AppDelayTaskConsumer> appDelayTaskConsumerList){
        if( appDelayTaskConsumerList==null || appDelayTaskConsumerList.size()<1 ){
            return;
        }
        for (AppDelayTaskConsumer appDelayTaskConsumer : appDelayTaskConsumerList) {
            this.appDelayTaskConsumers.put( appDelayTaskConsumer.getDelayTaskType().toLowerCase().trim() , appDelayTaskConsumer ) ;
        }
    }

    /**
     * 开启消费任务
     */
    public void startConsumeTask(){
        if ( !appDelayTaskProperties.getEnableConsume() ) {
            return;
        }

        threadPoolExecutor = new ThreadPoolExecutor( appDelayTaskProperties.getConsumeTaskNum() , appDelayTaskProperties.getConsumeTaskNum() ,
                5, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(100)) ;

        // 创建消费任务
        for (int i = 0 ; i < appDelayTaskProperties.getConsumeTaskNum() ; i++ ){
            AppDelayTaskConsumerThread appDelayTaskConsumerThread = new AppDelayTaskConsumerThread("delaytask-"+(i+1) ,
                    applicationContext,
                    appDelayTaskConsumers
            );
            threadPoolExecutor.submit( appDelayTaskConsumerThread ) ;
        }
    }

}
