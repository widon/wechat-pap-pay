package com.jverson.wechatpay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;


/**
 * 微信退款接口<br/>
 */
@Controller
public class WxRefundExportServiceImpl implements WxRefundExportService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 退款请求接口
     * @param wxRefundVo 退款单号等信息
     * @return 退款结果<br/>
     */
    @Profiled(tag = "refundRequest")
    @Override
    public WxRefundResultVo refundRequest(final WxRefundVo wxRefundVo){
        WxRefundResultVo wxRefundResultVo = new WxRefundResultVo();
        /** --------------------------------------------------
         * 
         * 
         * 校验入参
         * 
         * 
         * ---------------------------------------------------
         */
        ErrorInfo errorInfo = this.checkData(wxRefundVo);
        if (errorInfo != null) {
            wxRefundResultVo.setIsSuccess(false);
            wxRefundResultVo.setCode(errorInfo.getCode());
            wxRefundResultVo.setInfo(errorInfo.getInfo());
            return wxRefundResultVo;
        }
        
        /** --------------------------------------------------
         * 
         * 
         * 获取微信配置文件信息
         * 
         * 
         * ---------------------------------------------------
         */
        Properties prop = PropertiesInfoHelper.readPropertiesFile("/props/wx_info_" + wxRefundVo.getPayEnum() + ".properties");
        String appId = prop.getProperty("appid");
        String mchId = prop.getProperty("mch_id");
        String wxKey = prop.getProperty("key");
        String nonceStr = RandomStringUtils.randomAlphabetic(30);
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(mchId) || StringUtils.isBlank(wxKey)) {
            wxRefundResultVo.setIsSuccess(false);
            wxRefundResultVo.setCode(ErrorInfo.ERROR_90002.getCode());
            wxRefundResultVo.setInfo(ErrorInfo.ERROR_90002.getInfo());
            return wxRefundResultVo;
        }
        /** --------------------------------------------------
         * 
         * 
         * 获取证书
         * 
         * 
         * ---------------------------------------------------
         */
        CloseableHttpClient httpclient = null;
        FileInputStream instream = null;
        try {
            KeyStore keyStore;
            keyStore = KeyStore.getInstance("PKCS12");
            String path = this.getClass().getResource("/").getPath();
            path = path.substring(0, path.indexOf("classes")) + "classes/props/" + mchId + ".p12";
            instream = new FileInputStream(new File(path));
            keyStore.load(instream, mchId.toCharArray());
            SSLContext sslcontext;
            sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, mchId.toCharArray())
                    .build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslcontext,
                    new String[] { "TLSv1" },
                    null,
                    SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
            httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
        } catch (Exception e) {
            logger.error("读取证书发生异常，详细信息如下：", e);
            wxRefundResultVo.setIsSuccess(false);
            wxRefundResultVo.setCode(ErrorInfo.ERROR_90007.getCode());
            wxRefundResultVo.setInfo(ErrorInfo.ERROR_90007.getInfo());
            return wxRefundResultVo;
        } finally {
            try {
                if (instream != null) {
                    instream.close();
                }
            } catch (IOException e) {
                wxRefundResultVo.setIsSuccess(false);
                wxRefundResultVo.setInfo("微信证书文件流关闭异常");
                logger.error("微信证书文件流关闭异常:", e);
            }
        }

        /** --------------------------------------------------
         * 
         * 
         * 请求微信退款接口
         * 
         * 
         * ---------------------------------------------------
         */
        StringBuffer responseBuffer = new StringBuffer();
        if (httpclient != null) {
            CloseableHttpResponse response = null;
            BufferedReader bufferedReader = null;
            try {
                String xmlData = generateXML(appId, mchId, nonceStr, wxKey, wxRefundVo);
                HttpPost post = new HttpPost(ConfigConstants.REFUND_CHARGE_HTTP);
                RequestConfig config = RequestConfig.custom()
                  .setConnectionRequestTimeout(10000).setConnectTimeout(10000)
                  .setSocketTimeout(10000).build();
                post.setEntity(new StringEntity(xmlData, "UTF-8"));
                post.setConfig(config);
                if (logger.isInfoEnabled()) {
                    logger.info("微信退款请求参数：" + xmlData);
                }
                
                response = httpclient.execute(post);
                HttpEntity entity = response.getEntity();
                
                if (logger.isInfoEnabled()) {
                    logger.info("微信退款请求响应码：" + response.getStatusLine());
                }
                if (entity != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String text;
                    while ((text = bufferedReader.readLine()) != null) {
                        responseBuffer.append(text);
                    }
                }
                EntityUtils.consume(entity);
            } catch (Exception e) {
                logger.error("请求微信退款接口发生异常，详细信息如下：", e);
                wxRefundResultVo.setIsSuccess(false);
                wxRefundResultVo.setCode(ErrorInfo.ERROR_90008.getCode());
                wxRefundResultVo.setInfo(ErrorInfo.ERROR_90008.getInfo());
                return wxRefundResultVo;
            } finally {
                try {
                    if (response != null) {
                        response.close();
                    }
                    if (httpclient != null) {
                        httpclient.close();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    wxRefundResultVo.setIsSuccess(false);
                    wxRefundResultVo.setInfo("微信请求流关闭异常");
                    logger.error("微信请求流关闭异常:", e);
                }
            }
        }
        
        /** --------------------------------------------------
         * 
         * 
         * 返回结果
         * 
         * 
         * ---------------------------------------------------
         */
        if (responseBuffer.length() > 0) {
            try {
                String result = new String(responseBuffer.toString().getBytes("UTF-8"), "UTF-8");
                if (logger.isInfoEnabled()) {
                    logger.info("微信退款请求结果：" + result);
                }
                Map<Object, Object> resultMap = WxPayUtil.xmlParse(result);
                if ("SUCCESS".equals(resultMap.get("return_code").toString()) && "SUCCESS".equals(resultMap.get("result_code").toString())) {
                    if (WxPayUtil.validateSign(resultMap, wxKey)) {
                        wxRefundResultVo.setIsSuccess(true);
                        wxRefundResultVo.setPayId(wxRefundVo.getPayId());
                    } else {
                        wxRefundResultVo.setIsSuccess(false);
                        wxRefundResultVo.setCode(ErrorInfo.ERROR_90009.getCode());
                        wxRefundResultVo.setInfo(ErrorInfo.ERROR_90009.getInfo());
                    }
                } else {
                    wxRefundResultVo.setIsSuccess(false);
                    wxRefundResultVo.setCode(resultMap.get("err_code").toString());
                    wxRefundResultVo.setInfo(resultMap.get("err_code_des").toString());
                }
            } catch (UnsupportedEncodingException e) {
                logger.error("微信退款转码退款结果发生异常：", e);
                wxRefundResultVo.setIsSuccess(false);
                wxRefundResultVo.setInfo("微信退款转码退款结果发生异常");
            }
        }
        return wxRefundResultVo;
    }


    /**
     * 校验入参
     * @param params 入参map
     * @return 校验结果<br/>
     */
    private ErrorInfo checkData(WxRefundVo wxRefundVo) {
        if (wxRefundVo == null) {
            return ErrorInfo.ERROR_90000;
        }
        if (StringUtils.isBlank(wxRefundVo.getPayEnum())) {
            return ErrorInfo.ERROR_90001;
        }
        
        if (StringUtils.isBlank(wxRefundVo.getPayId())) {
            return ErrorInfo.ERROR_90003;
        }
        
        if (StringUtils.isBlank(wxRefundVo.getRfId())) {
            return ErrorInfo.ERROR_90004;
        }
        
        if (wxRefundVo.getAmount() == null) {
            return ErrorInfo.ERROR_90005;
        } 
        
        if (wxRefundVo.getRefAmount() == null) {
            return ErrorInfo.ERROR_90006;
        }
        
        return null;
    }
    
    private Integer getFen(BigDecimal yuan) {
        return Long.valueOf(Math.round(yuan.doubleValue() * 100)).intValue();
    }
    
}
