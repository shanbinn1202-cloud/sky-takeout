package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
//import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController("adminDishController")
@Slf4j
@RequestMapping("/admin/dish")
@Api(tags = "dish controller")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("add dish")
    public Result save(@RequestBody DishDTO dishDTO){
        dishService.saveWithFlavor(dishDTO);

        String key = "dish_" + dishDTO.getCategoryId();
        redisTemplate.delete(key);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation(value = "page query for dished")
    public  Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("dish  list:{}",dishPageQueryDTO);
        PageResult pageResult =  dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation(value = "delete a dish")
    public Result deleteDishes(@RequestParam List<Long> ids){
        log.info("delete dishes:{}",ids);
        dishService.deleteDishes(ids);

        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("get dish by id")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("get dish by id:{}",id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation(value = "modify a dish")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("update the dish:{}",dishDTO);
        dishService.update(dishDTO);

        cleanCache("dish_*");

        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation(value = "start of stop")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("change the dish status");
        dishService.startOrStop(status,id);
        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("query by category id")
    public Result<List<Dish>> getByCategoryId(Long categoryId){
        log.info("get by categoryId:{}",categoryId);
        List<Dish> dishes =  dishService.getByCategoryId(categoryId);
        return Result.success(dishes);
    }

    private void cleanCache(String pattern){
        Set set = redisTemplate.keys(pattern);
        redisTemplate.delete(set);
    }
}
