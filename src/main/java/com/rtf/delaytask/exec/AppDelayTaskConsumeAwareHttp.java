package com.rtf.delaytask.exec;

import com.mzlion.easyokhttp.response.HttpResponse;
import com.rtf.delaytask.AppDelayTask;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * 默认的请求状态检查和请求结果响应检查
 * @Author : liupeng
 * @Date : 2020-05-20
 * @Modified By
 */
@Slf4j
public class AppDelayTaskConsumeAwareHttp implements AppDelayTaskConsumeAware {

    @Override
    public AppDelayTaskConsumeResult execResultCheck(AppDelayTask appDelayTask, Object singleResult, long duration) {
        // 默认执行结果为成功
        AppDelayTaskConsumeResult consumeResult = new AppDelayTaskConsumeResult() ;
        consumeResult.setSuccess( true ) ;

        if( singleResult==null ){
            consumeResult.setSuccess( false ) ;
            consumeResult.setMessage("http请求响应对象为空");
            return consumeResult ;
        }

        // 对httpResponse进行检查
        if( singleResult instanceof HttpResponse ){
            HttpResponse httpResponse = (HttpResponse) singleResult ;
            boolean success = httpResponse!=null && httpResponse.isSuccess() ;
            consumeResult.setSuccess( success );
            if( !success ){
                consumeResult.setMessage("status:"+httpResponse.getErrorCode()+",response:"+getRawResponseBody( httpResponse ));
            }
        }

        return consumeResult ;
    }

    /**
     * 获取响应编码
     * @param httpResponse
     * @return
     */
    public int getRawResponseCode(HttpResponse httpResponse){
        return httpResponse.isSuccess() ? 200 : httpResponse.getErrorCode() ;
    }

    /**
     * 获取响应内容
     * @param httpResponse
     * @return
     */
    public String getRawResponseBody(HttpResponse httpResponse){
        String responseBody = "" ;

        try{
            // 获取response属性
            Set<Field> fields = ReflectionUtils.getFields( HttpResponse.class , ReflectionUtils.withName("rawResponse") ) ;
            Field rawResponseField = fields.toArray( new Field[]{} )[0] ;
            rawResponseField.setAccessible(true);

            // 获取response对象
            Response rawResponse = (Response)rawResponseField.get( httpResponse ) ;

            responseBody = rawResponse!=null && rawResponse.body()!=null ? rawResponse.body().string() : "" ;
        }catch( Exception e ){
        }

        return responseBody ;
    }

    @Override
    public void execComplete(AppDelayTask appDelayTask, boolean result, String message) {
//        log.info("任务执行结束: {} , {}" , result , JSON.toJSONString( appDelayTask ));
    }

}
