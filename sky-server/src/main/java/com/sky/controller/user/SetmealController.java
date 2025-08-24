package com.sky.controller.user;


import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@Slf4j
@Api("Uset Setmeal Controller")
@RequestMapping("/user/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private DishService dishService;

    @GetMapping("/list")
    @ApiOperation("get by category id")
    @Cacheable(cacheNames = "setmealCache",key = "#categoryId")
    public Result<List<Setmeal>> getByCategoryId(@RequestParam Long categoryId){
        log.info("get by category id:{}",categoryId);
        List<Setmeal> setmeals =  setmealService.getByCategoryId(categoryId);
        return null;
    }

    @GetMapping("/dish/{id}")
    public Result<List<DishItemVO>> getBySetmealId(@PathVariable Long id){
        log.info("get by setmeal id:{}",id);
        List<DishItemVO> dishItemVOS  =  dishService.getBySetmealId(id);
        return Result.success(dishItemVOS);
    }

}
