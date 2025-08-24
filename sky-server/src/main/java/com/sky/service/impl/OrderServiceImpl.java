package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //exception
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException("address is null");
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if(shoppingCarts==null || shoppingCarts.size()==0){
            throw new ShoppingCartBusinessException("shoppingcart is null");
        }
        //add to order table
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
//        orders.setDeliveryStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orderMapper.insert(orders);
        //add to order detail table

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart sc:shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(sc,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //clean the shoppingcart
        shoppingCartMapper.clean(shoppingCart);
        //return
        OrderSubmitVO orderSubmitVO =  OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        Integer OrderPaidStatus = Orders.PAID;
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;
        LocalDateTime check_out_time = LocalDateTime.now();
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        orderMapper.updateStatus(OrderStatus,OrderPaidStatus,check_out_time,orderNumber);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        Map map =new HashMap<>();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","outTradeNo:"+outTradeNo);
        String json =  JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    @Override
    public PageResult pageQuery4User(int page, int pageSize, Integer status) {
        PageHelper.startPage(page,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOS = new ArrayList<>();
        for(Orders o:orders){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(o,orderVO);
            List<OrderDetail> orderDetailList =  orderDetailMapper.getByOrderId(o.getId());
            orderVO.setOrderDetailList(orderDetailList);
            orderVOS.add(orderVO);
        }
        return  new PageResult(orders.getTotal(),orderVOS);
    }

    @Override
    public OrderVO details(Long id) {
        List<OrderDetail> orderDetailList =  orderDetailMapper.getByOrderId(id);
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO =new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null){
            throw  new OrderBusinessException("The order does not exist");
        }
        Integer status =  orders.getStatus();
        if (status>Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException("You should contact xxxx...");
        }
        Orders newo = new Orders();
        newo.setId(orders.getId());
        newo.setStatus(Orders.CANCELLED);
        newo.setCancelReason("the user canceles");
        orderMapper.update(newo);

    }

    @Override
    @Transactional
    public void repeat(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        List<ShoppingCart> shoppingCarts = orderDetailList.stream().map(x->{
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(x,shoppingCart);
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCart.setUserId(BaseContext.getCurrentId());
                return shoppingCart;
                }
        ).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }
    private List<OrderVO> getOrderVOList(Page<Orders> page){
        List<Orders> ordersList = page.getResult();
        List<OrderVO> orderVOList = ordersList.stream().map(x->{
            OrderVO orderVO= new OrderVO();
            BeanUtils.copyProperties(x,orderVO);
            String orderDishes = getOrderDishesStr(x);
            orderVO.setOrderDishes(orderDishes);
            return orderVO;
        }).collect(Collectors.toList());
        return orderVOList;
    }
    private String getOrderDishesStr(Orders orders){
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    @Override
    public OrderStatisticsVO orderStatistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Long userId = BaseContext.getCurrentId();
        Long orderId = ordersConfirmDTO.getId();
        Orders orders = Orders.builder()
                .id(orderId)
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) {
        Long userId = BaseContext.getCurrentId();
        Long orderId = ordersRejectionDTO.getId();
        Orders orders  =orderMapper.getById(orderId);
        if(orders!=null && !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException("cannot reject");
        }

        Orders orders1 = new Orders();
        orders1.setId(orderId);
        orders1.setStatus(Orders.CANCELLED);
        orders1.setRejectionReason("rejected by the owner");
        orders1.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders1);

    }

    @Override
    public void cancelWithReason(OrdersCancelDTO ordersCancelDTO) {
        Long userId = BaseContext.getCurrentId();
        Long orderId = ordersCancelDTO.getId();
        Orders orders  =orderMapper.getById(orderId);

        Orders orders1 = new Orders();
        orders1.setId(orderId);
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelReason(ordersCancelDTO.getCancelReason());
        orders1.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders1);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException("order does not exist or it hasn't been confirmed");
        }
        Orders orders2 = new Orders();
        orders2.setId(orders.getId());
        orders2.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders2);

    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException("order is not being delivered");
        }
        Orders orders2 = new Orders();
        orders2.setId(orders.getId());
        orders2.setStatus(Orders.COMPLETED);
        orders2.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders2);

    }

    @Override
    public void remind(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders!=null && !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException("SOMTHING IS WRONG");
        }
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","outTradeNo:"+orders.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }
}
