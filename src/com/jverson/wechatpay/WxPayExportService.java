package com.jverson.wechatpay;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.jd.payment.wxbankcom.domain.WxCheckPayResultVo;
import com.jd.payment.wxbankcom.domain.WxPayResultVo;
import com.jd.payment.wxbankcom.domain.WxPayVo;


/**
 * 微信支付接口<br/>
 */
@Path("/service/pap")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface WxPayExportService {
    
    /**
     * 微信支付接口
     */
    @POST
    @Path("pay")
    WxPayResultVo payRequestWithoutPwd(WxPayVo wxPayVo);
    
    /**
     * 查询支付结果
     * transaction_id,out_trade_no两者传其一
     * @param transaction_id
     * @param out_trade_no
     * @return
     */
    @POST
    @Path("check")
    WxCheckPayResultVo checkWeChatPay(WxPayVo wxPayVo);
    
	/**
	 * 支付回调处理服务
	 * @param reqStr
	 * @return Object
	 * @throws IOException 
	 */
    @POST
    @Consumes({ "application/xml" })
    @Produces({ "application/xml" })
    @Path("payNotify")
    String handlePayNotify(String req) throws IOException;

}
