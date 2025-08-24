package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
    void  addSetmeal(SetmealDTO setmealDTO);
    void update(SetmealDTO setmealDTO);
    SetmealVO getById(Long id);
    void delete(List<Long> ids);
    void startOrStop(Integer status, Long id);
    List<Setmeal> getByCategoryId(Long id);
}
