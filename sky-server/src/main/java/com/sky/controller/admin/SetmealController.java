package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminSetmealController")
@Slf4j
@Api(tags = "setmeal controller")
@RequestMapping("/admin/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @GetMapping("/page")
    @ApiOperation(value = "pageQuery")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("setmeal pageQuery");
        PageResult pageResult =setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }


    @PostMapping
    @ApiOperation(value = "add setmeal")
    @CacheEvict(cacheNames = "setmealCache",key = "#setmealDTO.categoryId")
    public Result addSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("add setmeal:{}",setmealDTO);
        setmealService.addSetmeal(setmealDTO);
        return Result.success();

    }

    @PutMapping
    @ApiOperation(value = "update setmeal")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("update setmeal:{}",setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "get by id")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("get by id:{}",id);
        SetmealVO setmealVO =  setmealService.getById(id);
        return Result.success(setmealVO);
    }

    @DeleteMapping
    @ApiOperation("delete setmeals")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("delete setmeals ids:{}",ids);
        setmealService.delete(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("Setmeal StartOrStop")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("setmeal startorstop id:{}",id);
        setmealService.startOrStop(status,id);
        return Result.success();
    }
}
