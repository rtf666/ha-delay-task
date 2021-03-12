package com.rtf.delaytask.exec.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.mzlion.easyokhttp.HttpClient;
import com.mzlion.easyokhttp.request.AbsHttpRequest;
import com.mzlion.easyokhttp.response.HttpResponse;
import com.rtf.delaytask.AppDelayTask;
import com.rtf.delaytask.config.AppDelayQueueProperties;
import com.rtf.delaytask.exec.AppDelayTaskConsumeAware;
import com.rtf.delaytask.exec.AppDelayTaskConsumeResult;
import com.rtf.delaytask.exec.AppDelayTaskConsumer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * @Author : liupeng
 * @Date : 2020-05-20
 * @Modified By
 */
@SuppressWarnings("all")
@Slf4j
public class AppHttpDelayTaskConsumer implements AppDelayTaskConsumer, InitializingBean {

    private static HttpClient httpClient ;

    @Autowired
    private AppDelayQueueProperties delayQueueProperties ;

    @Autowired
    private AppDelayTaskConsumeAware appDelayTaskConsumeAware ;

    @Override
    public void afterPropertiesSet() throws Exception {
        httpClient = HttpClient.Instance ;
        httpClient.setCookieStore( new AppHttpNoCookieStore() ) ;
        ConnectionPool connectionPool = new ConnectionPool(200 , 5 , TimeUnit.MINUTES );
        httpClient.getOkHttpClientBuilder().connectionPool( connectionPool ) ;
    }

    @Override
    public String getDelayTaskType() {
        return "http";
    }

    @Override
    public AppDelayTaskConsumeResult execDelayTask(AppDelayTask appDelayTask) {
        AppDelayTaskConsumeResult consumeResult = new AppDelayTaskConsumeResult() ;

        try{
            AbsHttpRequest absHttpRequest = createHttpRequest( appDelayTask ) ;
            long startTime = System.currentTimeMillis() ;
            // 发起请求
            HttpResponse httpResponse = absHttpRequest.execute() ;
            long endTime = System.currentTimeMillis() ;
            // 记录请求结果
            consumeResult = appDelayTaskConsumeAware.execResultCheck( appDelayTask , httpResponse , ( endTime - startTime ) ) ;
        }catch( Exception e ){

            consumeResult.setSuccess( false ) ;
            String message = "exception: "+ e.getClass().getName() +" , message : "+ e.getMessage() ;
            // 应用连接超时
            if( StringUtils.indexOfIgnoreCase( e.getMessage() , "SocketTimeoutException" ) != -1 ){
                message = "连接超时，超时时间: "+delayQueueProperties.getHttpConnectTimeout()+"s。" ;
            }
            consumeResult.setMessage( message ) ;
            log.error("请求执行异常: {} , {}" , JSON.toJSONString(appDelayTask) , e.getClass().getName() +" , message : "+ e.getMessage());
        }

        return consumeResult ;
    }

    /**
     * 创建http请求任务
     * @param appDelayTask
     * @return
     */
    public AbsHttpRequest createHttpRequest(AppDelayTask appDelayTask){
        AbsHttpRequest absHttpRequest = null ;

        // 1. 创建请求信息
        if( StringUtils.isBlank( appDelayTask.getMethod() ) || StringUtils.equalsIgnoreCase( appDelayTask.getMethod() , "post" ) ){
            // 默认使用json提交
            if( StringUtils.isBlank( appDelayTask.getContentType() ) || StringUtils.equalsIgnoreCase( appDelayTask.getContentType() , "json" )){
                absHttpRequest = httpClient.textBody( appDelayTask.getUrl() )
                        .json( getRequestParams(appDelayTask) ) ;
            }else{
                // 使用form表单提交
                absHttpRequest = httpClient.post( appDelayTask.getUrl() )
                        .param( getRequestParams(appDelayTask) ) ;
            }
        }else if( StringUtils.equalsIgnoreCase( appDelayTask.getMethod() , "get" ) ){
            absHttpRequest = httpClient.get( appDelayTask.getUrl() ) ;
        }else{
            throw new IllegalArgumentException("请求调用不支持请求方法:"+appDelayTask.getMethod()) ;
        }

        // 2. 设置请求头认证信息
        URI hostInfo = URI.create( appDelayTask.getUrl() ) ;
        if( StringUtils.isNotBlank( hostInfo.getUserInfo() ) ){
            absHttpRequest.header("Authorization", "Basic " + Base64.encodeBase64String((hostInfo.getUserInfo()).getBytes())) ;
        }

        // 3. 设置连接超时和读超时时间
        absHttpRequest.connectTimeout( delayQueueProperties.getHttpConnectTimeout() ) ;
        absHttpRequest.readTimeout( delayQueueProperties.getHttpReadTimeout() ) ;

        return absHttpRequest ;
    }

    /**
     * 构造请求参数
     * @param appDelayTask
     * @return
     */
    public Map<String,String> getRequestParams(AppDelayTask appDelayTask){
        // 构造请求参数
        Map<String,String> params = Maps.newHashMap() ;
        if( StringUtils.isNotBlank( appDelayTask.getParams() ) ){
            JSONObject initParams = JSON.parseObject( appDelayTask.getParams() ) ;
            if( initParams != null ){
                for (String key : initParams.keySet()) {
                    params.put( key , initParams.getString( key ) ) ;
                }
            }
        }
        return params ;
    }



}
