package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult pageQuery4User(int page,int pageSize,Integer status);

    OrderVO details(Long id);
    void cancel(Long id);

    void repeat(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);
    OrderStatisticsVO orderStatistics();
    void confirm(OrdersConfirmDTO ordersConfirmDTO);
    void reject(OrdersRejectionDTO ordersRejectionDTO);
    void cancelWithReason(OrdersCancelDTO ordersCancelDTO);
    void delivery(Long id);
    void complete(Long id);
    void remind(Long id);
}
