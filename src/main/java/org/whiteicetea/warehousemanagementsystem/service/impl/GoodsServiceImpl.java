package org.whiteicetea.warehousemanagementsystem.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.whiteicetea.warehousemanagementsystem.entity.Goods;
import org.whiteicetea.warehousemanagementsystem.mapper.GoodsMapper;
import org.whiteicetea.warehousemanagementsystem.service.GoodsService;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {


    @Override
    public Goods getById(Long id) {
        Goods goods;
        goods = super.getById(id);
        if(goods!=null){
            return goods;
        }
        throw new RuntimeException("商品不存在");
    }
}
