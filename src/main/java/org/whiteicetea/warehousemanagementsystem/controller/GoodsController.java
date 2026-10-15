package org.whiteicetea.warehousemanagementsystem.controller;


import org.springframework.web.bind.annotation.*;
import org.whiteicetea.warehousemanagementsystem.entity.Goods;
import org.whiteicetea.warehousemanagementsystem.service.GoodsService;

import java.util.List;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {
    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public List<Goods> list() {
        return goodsService.list();
    }

    @GetMapping("/{id}")
    public Goods getById(@PathVariable Long id) {
        return goodsService.getById(id);
    }

    @PostMapping
    public boolean save(@RequestBody Goods goods) {
        return goodsService.save(goods);
    }
}
