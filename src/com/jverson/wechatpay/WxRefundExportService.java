package com.jverson.wechatpay;

/**
 * 微信退款接口<br/>
 */
@Path("/service/weixin")
@Consumes({ "application/json" })
@Produces({ "application/json" })
public interface WxRefundExportService {
    
    /**
     * 退款请求接口
     * @param wxRefundVo 退款单号等信息
     * @return 退款结果<br/>
     */
    @POST
    @Path("refundRequest")
    public WxRefundResultVo refundRequest(WxRefundVo wxRefundVo);

}
