package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;


public interface DishService {
    public void saveWithFlavor(DishDTO dishDTO);
    public PageResult page(DishPageQueryDTO dishPageQueryDTO);
    public void deleteDishes(List<Long> ids);
    public DishVO getById(Long id);
    public void update(DishDTO dishDTO);
    public void startOrStop(Integer status, Long id);
    public List<Dish> getByCategoryId(Long categoryId);
    public List<DishVO> getByCategoryIdWithFlavors(Long categoryId);
    public List<DishItemVO> getBySetmealId(Long id);
}
