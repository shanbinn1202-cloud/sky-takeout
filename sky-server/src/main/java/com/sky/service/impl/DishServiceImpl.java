package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishItemVO;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Options;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Transactional

    public void saveWithFlavor(DishDTO dishDTO){
        //insert into dish table
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();

        if(flavors!=null && flavors.size()>0){
            flavors.forEach(flavor->{
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
        //insert into flavor table
    }

    public PageResult page(DishPageQueryDTO dishPageQueryDTO){
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> pg = dishMapper.page(dishPageQueryDTO);
        return new PageResult(pg.getTotal(),pg.getResult());
    }

    @Transactional
    public void deleteDishes(List<Long> ids){
        //status
        for(Long id:ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException("You cannot delete them.");
            }
        }
        //related?
        //delete dishes
        List<Long> setMealsIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(!setMealsIds.isEmpty()){
            throw new DeletionNotAllowedException("Some dishes are in a set meal.");
        }
        //delete flavors
        for(Long id:ids){
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }

    }

    @Override
    public DishVO getById(Long id){
        //dish
         Dish dish =  dishMapper.getById(id);
        //flavor
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //vo
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);

        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    public void update(DishDTO dishDTO){
        //dish table
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //flavor table
        dishFlavorMapper.deleteByDishId(dish.getId());
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        for(DishFlavor dishFlavor:dishFlavors){
            dishFlavor.setDishId(dish.getId());
        }
        dishFlavorMapper.insertBatch(dishFlavors);

    }
    @Override
    @AutoFill(OperationType.UPDATE)
    public void startOrStop(Integer status, Long id){
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId){
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        List<Dish> dishes = dishMapper.getByCategoryId(dish);
        return dishes;
    }

    @Override
    @Transactional
    public List<DishVO> getByCategoryIdWithFlavors(Long categoryId){
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        List<Dish> dishes = dishMapper.getByCategoryId(dish);
        List<DishVO> dishVOS =  new ArrayList<>();
        for(Dish dish1:dishes){
            DishVO dishVO= new DishVO();
            BeanUtils.copyProperties(dish1,dishVO);

            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(dish1.getId());
            dishVO.setFlavors(flavors);
            dishVOS.add(dishVO);
        }
        return dishVOS;
    }

    @Override
    public List<DishItemVO> getBySetmealId(Long id){
        List<SetmealDish> setmealDishes= setmealDishMapper.getBySetmealId(id);
        List<Long> dishids = new ArrayList<>();
        for(SetmealDish setmealDish:setmealDishes){
            dishids.add(setmealDish.getDishId());
        }

        List<DishItemVO> dishItemVOS = new ArrayList<>();
        for(Long thisid:dishids){
            DishItemVO dishItemVO = new DishItemVO();
            Dish dish = dishMapper.getById(thisid);
            BeanUtils.copyProperties(dish,dishItemVO);
            dishItemVOS.add(dishItemVO);
        }
        return dishItemVOS;
    }
}
