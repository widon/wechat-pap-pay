package com.jverson.wechatpay;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.sun.xml.internal.txw2.Document;

/**
 * 微信委托代扣公用方法
 * docs: https://pay.weixin.qq.com/wiki/doc/api/micropay.php?chapter=4_3
 * @version  v1.0
 */
public class WeChatPayUtil {
	
	//JDK中的日期时间格式（年-月-日 时:分:秒） 
	public final static String DateTimeFormat = "yyyy-MM-dd HH:mm:ss"; 
	private  static Logger logger = LoggerFactory.getLogger(WeChatPayUtil.class);
	
	/**
	 * 微信签名生成算法  
	 * @param parameters 参数按照accsii排序（升序）
	 * @return String
	 */
	public static String getSignature(SortedMap<Object,Object> parameters){  
        StringBuffer sb = new StringBuffer();  
        Set<Entry<Object, Object>> es = parameters.entrySet(); 
        Iterator<Entry<Object, Object>> it = es.iterator();  
        while(it.hasNext()) {  
            @SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry)it.next();  
            String k = (String)entry.getKey();  
            Object v = entry.getValue();  
            if(null != v && !"".equals(v)) {  
                sb.append(k + "=" + v + "&");  
            }  
        }  
        sb.append("key=" + WeChatConstants.KEY);  
        String sign = MD5Util.getMD5String(sb.toString()).toUpperCase();  
        return sign;  
    } 
	
	/**
	 * 指定长度随机字符串生成
	 * @param length
	 * @return String
	 */
	public static String createRandomString(int length) {
		String base = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";  
        Random random = new Random();  
        StringBuffer sb = new StringBuffer();  
        for (int i = 0; i < length; i++) {  
            int number = random.nextInt(base.length());  
            sb.append(base.charAt(number));  
        }  
        return sb.toString();  
	}
	
	/**
	 * 将请求参数转换为微信要求的xml格式
	 * @param params
	 * @return String
	 */
	public static String mapParseToXml(SortedMap<Object, Object> params){
		StringBuffer sb = new StringBuffer();
		sb.append("<xml>");
		Set<Entry<Object, Object>> es = params.entrySet();
		Iterator<Entry<Object, Object>> it = es.iterator();
		while(it.hasNext()){
			Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
			String k = (String)entry.getKey();
			String v = String.valueOf(entry.getValue());
			sb.append("<"+k+">"+"<![CDATA["+v+"]]></"+k+">");
		}
		sb.append("</xml>");
		return sb.toString();
	}
	
	/**
	 *  解析微信返回的xml消息
	 * @param request
	 * @return
	 * @throws Exception Map<String,String>
	 */
	public static Map<String, String> xmlParseToMap(HttpServletRequest request) throws Exception { 
	    Map<String, String> map = new HashMap<String, String>(); 
	    ServletInputStream inputStream = request.getInputStream(); 
	    SAXReader reader = new SAXReader(); 
	    Document document = reader.read(inputStream); 
	    Element root = document.getRootElement(); 
	    @SuppressWarnings("unchecked")
		List<Element> elementList = root.elements(); 
	    for (Element e : elementList) 
	        map.put(e.getName(), e.getText()); 
	    inputStream.close(); 
	    inputStream = null; 
	    return map; 
	} 
	
	/**
	 * xml解析为map
	 * @param xmlString
	 * @return Map<String,String>
	 */
	public static Map<Object, Object> xmlParse(String xmlString){ 
		    Map<Object, Object> map = new HashMap<Object, Object>(); 
		    Document document = null;
			try {
				document = DocumentHelper.parseText(xmlString);
			} catch (DocumentException e1) {
				e1.printStackTrace();
			}
		    Element root = document.getRootElement(); 
		    @SuppressWarnings("unchecked")
			List<Element> elementList = root.elements(); 
		    for (Element e : elementList){
		    	if(null != e.getText() && !"".equals(e.getText())){
	        		map.put(e.getName(), e.getText());
	        	}
		    }
		    return map; 
		}
	
	/**
	 * map转get url请求头
	 * @param map
	 * @return String
	 */
	public static String getUrlParamsByMap(Map<String, Object> map) {  
	    if (map == null) {  
	        return "";  
	    }  
	    StringBuffer sb = new StringBuffer();  
	    for (Map.Entry<String, Object> entry : map.entrySet()) {  
	        sb.append(entry.getKey() + "=" + entry.getValue());  
	        sb.append("&");  
	    }  
	    String s = sb.toString();  
	    if (s.endsWith("&")) {  
	        s = org.apache.commons.lang.StringUtils.substringBeforeLast(s, "&");  
	    }  
	    return s;  
	}  
	
	/**
	 * 生成回调确认信息xml
	 * @author jiwenxing
	 * @param return_code
	 * @param return_msg
	 * @return String
	 */
	public static String getConfirmXML(String return_code, String return_msg) {
        return "<xml><return_code><![CDATA[" + return_code
                + "]]></return_code><return_msg><![CDATA[" + return_msg
                + "]]></return_msg></xml>";
	}
	
	/**
	 * 日期格式转换方法
	 * @author jiwenxing
	 * @param strDate
	 * @return Date
	 */
	public static Date strToDate(String strDate){ 
		SimpleDateFormat dateFormat = new SimpleDateFormat(DateTimeFormat); 
		Date date = null; 
		try{ 
		date = dateFormat.parse(strDate); 
		}catch(ParseException e){ 
		System.out.println(e.getMessage()); 
		} 
		return date; 
	} 

	/**
	 * http post方法，封装了签名
	 * @param map
	 * @return 返回值先判断valid，fail说明校验签名失败，true说明校验成功，这时其余返回值才有效
	 */
	public static Map<Object, Object> HttpPostMethod(SortedMap<Object, Object> params, String httpPostUrl, Integer socketTimeout, Integer connectTimeout) {
		Map<Object, Object> resultMap = new HashMap<>();
		String sign = WeChatPayUtil.getSignature(params); //生成签名
		params.put("sign", sign);
		String xmlString = WeChatPayUtil.mapParseToXml(params); //参数转换为xml格式
		CloseableHttpClient httpClient = HttpClients.createDefault(); 
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout).setConnectTimeout(connectTimeout).build();//设置请求和传输超时时间（待定）
		HttpPost httpPost = new HttpPost(httpPostUrl);
		httpPost.addHeader("Content-Type", "text/xml"); 
		httpPost.setConfig(requestConfig);
		StringEntity entity = new StringEntity(xmlString,"UTF-8");
		httpPost.setEntity(entity);
		logger.error("post请求参数： "+xmlString);
		try {
			CloseableHttpResponse response = httpClient.execute(httpPost);
			String xmlResult = "";
			if(response.getStatusLine().getStatusCode()==200){  
				xmlResult = EntityUtils.toString(response.getEntity()); 
				logger.error("post获取的xml返回值： "+xmlResult);
				resultMap = WeChatPayUtil.xmlParse(xmlResult);
				logger.info("返回值转换为json格式： "+JSONUtils.valueToString(resultMap));
				if("SUCCESS".equals(resultMap.get("return_code"))){
					if("SUCCESS".equals(resultMap.get("result_code"))){
						//返回结果每次都需要校验签名
						if(WeChatPayUtil.validateSign(resultMap)){
							resultMap.put("valid", "SUCCESS");
						}else{
							resultMap.put("valid", "FAIL");
						}
					}else{
						logger.error("错误代码:"+resultMap.get("err_code")+" 错误原因描述："+resultMap.get("err_code_des"));
					}
				}else{
					logger.error("调用接口通信失败！");
				}
	        } 
		} catch (IOException e) {
			logger.error("调用接口发生异常: ", e); 
		}finally{
			httpPost.releaseConnection();
		}
		return resultMap;
	}

	/**
	 * 校验微信请求返回值的签名
	 * @param resultMap
	 * @return boolean
	 */
	public static boolean validateSign(Map<Object, Object> params) {
		SortedMap<Object,Object> sortMap = new TreeMap<Object,Object>(params);
		String returnSign = (String) sortMap.get("sign");
		sortMap.remove("sign");
		if(returnSign.equals(WeChatPayUtil.getSignature(sortMap))){
			return true;
		}
		return false;
	}
	
}

