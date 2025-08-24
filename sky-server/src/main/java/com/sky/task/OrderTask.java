package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("process timeout order");
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if(orders != null && orders.size()>0){
            for(Orders o:orders){
                o.setStatus(Orders.CANCELLED);
                o.setCancelReason("Time out");
                o.setCancelTime(LocalDateTime.now());
                orderMapper.update(o);

            }
        }

    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processCompletedOrder(){
        log.info("process completed order");
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if(orders != null && !orders.isEmpty()){
            for(Orders o:orders){
                o.setStatus(Orders.COMPLETED);
                orderMapper.update(o);

            }
        }
    }
}
