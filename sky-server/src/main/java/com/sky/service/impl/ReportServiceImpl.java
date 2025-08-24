package com.sky.service.impl;

import com.sky.dto.OrderRankDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WorkspaceService workspaceService;
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        localDateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }
        String s =  StringUtils.join(localDateList,",");

        List<Double> turnovers = new ArrayList<>();
        for(LocalDate date:localDateList){
            LocalDateTime begintime =  LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endtime = LocalDateTime.of(date,LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",begintime);
            map.put("end",endtime);
            map.put("status",Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            if(turnover == null){
                turnover = 0.0;
            }
            turnovers.add(turnover);
        }
        String turnoverString =  StringUtils.join(turnovers,",");

        return TurnoverReportVO.builder()
                .dateList(s)
                .turnoverList(turnoverString)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        localDateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }
        String s =  StringUtils.join(localDateList,",");
        List<Integer> userList = new ArrayList<>();
        List<Integer> allUserList = new ArrayList<>();
        for(LocalDate date:localDateList){
            LocalDateTime begintime =  LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endtime = LocalDateTime.of(date,LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endtime);
            Integer a = userMapper.sumByMap(map);
            allUserList.add(a);
            map.put("begin",begintime);
            Integer usernum = userMapper.sumByMap(map);
            userList.add(usernum);
        }
        String newuserlist = StringUtils.join(userList,",");
        String alluserlist = StringUtils.join(allUserList,",");
        return UserReportVO.builder()
                .dateList(s)
                .newUserList(newuserlist)
                .totalUserList(alluserlist)
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        localDateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }
        List<Integer> orderCountList  = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for(LocalDate date:localDateList){
            LocalDateTime begintime =  LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endtime = LocalDateTime.of(date,LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",begintime);
            map.put("end",endtime);
            Integer orderNum = orderMapper.getCountByMap(map);
            orderCountList.add(orderNum);
            map.put("status",Orders.COMPLETED);
            Integer validNum = orderMapper.getCountByMap(map);
            validOrderCountList.add(validNum);
        }
        Integer ordersum = orderCountList.stream()
                .filter(Objects::nonNull) // 防止 NPE
                .mapToInt(Integer::intValue)
                .sum();
        Integer validsum = validOrderCountList.stream()
                .filter(Objects::nonNull) // 防止 NPE
                .mapToInt(Integer::intValue)
                .sum();
        String s =  StringUtils.join(localDateList,",");


        return OrderReportVO.builder()
                .dateList(s)
                .orderCompletionRate(divide(validsum,ordersum))
                .totalOrderCount(ordersum)
                .validOrderCount(validsum)
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .build();
    }
    public static Double divide(Integer a, Integer b) {
        return (a != null && b != null && b != 0) ? a.doubleValue() / b.doubleValue() : null;
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {

        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",Orders.COMPLETED);
        List<Orders> orders =  new ArrayList<>();
        orders =  orderMapper.getByMap(map);
        log.info("maps:{}",map);
        log.info("ordernums:{}",orders);
        List<Integer> orderids = orders.stream().map(x->{
            return x.getId().intValue();
        }).collect(Collectors.toList());

        map.put("orderids",orderids);
        log.info("orderids:{}",orderids);
        List<OrderRankDTO> rankDTO =  orderDetailMapper.rankByMap(map);

        List<String> strings= rankDTO.stream().map(x->{
            return x.getName();
        }).collect(Collectors.toList());

        List<Integer> counts = rankDTO.stream().map(x->{
            return x.getCount();
        }).collect(Collectors.toList());


        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(strings,","))
                .numberList(StringUtils.join(counts,","))
                .build();
    }

    @Override
    public void export(HttpServletResponse response) {
        LocalDate datebegin = LocalDate.now().minusDays(30);
        LocalDate dateend = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO =  workspaceService.getBusinessData(LocalDateTime.of(datebegin,LocalTime.MIN),LocalDateTime.of(dateend,LocalTime.MAX));
        InputStream is =  this.getClass().getClassLoader().getResourceAsStream("template/excel.xlsx");
        try{
            XSSFWorkbook excel =  new XSSFWorkbook(is);
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            XSSFRow row = sheet1.getRow(1);
            XSSFCell cell  =row.getCell(1);
            cell.setCellValue("time: "+datebegin+" to "+dateend);
            sheet1.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet1.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet1.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            sheet1.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            sheet1.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for(int i=0;i<30;i++){
                LocalDate date = datebegin.plusDays(i);
                businessDataVO =  workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN),LocalDateTime.of(date,LocalTime.MAX));
                row = sheet1.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3) .setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }

            ServletOutputStream outputStream =  response.getOutputStream();
            excel.write(outputStream);
            outputStream.close();
            excel.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
