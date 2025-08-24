package com.sky.controller.user;


import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api("ShoppingCart Controller")
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("add shoppingcart")
    public Result save(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("add shopping cart:{}",shoppingCartDTO);

        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("get shoppingcart")
    public Result<List<ShoppingCart>> list(){
        log.info("get shopping cart list");
        List<ShoppingCart> shoppingCarts =  shoppingCartService.list();
        return Result.success(shoppingCarts);

    }

    @DeleteMapping("/clean")
    @ApiOperation("clean shopping cart")
    public Result clean(){
        log.info("clean the shopping cart");
        shoppingCartService.clean();
        return Result.success();
    }

    @PostMapping("/sub")
    @ApiOperation("sub item in the shoppingcart")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.sub(shoppingCartDTO);
        return  Result.success();

    }
}
