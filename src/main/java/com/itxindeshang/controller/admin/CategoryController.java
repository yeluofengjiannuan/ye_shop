package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CategoryDTO;
import com.itxindeshang.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/category")
public class CategoryController {
    @Resource
    private CategoryService categoryService;

    /**
     * 获取分类树
     *
     * @return
     */
    @GetMapping("/tree")
    public Result showCategorytree() {
        //TODO：看要不要改方法名称
        return categoryService.showCategorytree();
    }
    /**
     * 新增分类
     *
     * @param categoryDTO
     * @return
     */
    @PostMapping("/add")
    public Result addCategory(@RequestBody CategoryDTO categoryDTO) {
        return categoryService.addCategory(categoryDTO);
    }

    /**
     * 删除分类
     * TODO：后续再考虑逻辑删除
     * @param categoryId
     * @return
     */
    @DeleteMapping("//delete/{categoryId}")
    public Result deleteCategory(@PathVariable String categoryId) {
        return categoryService.deleteById(categoryId);
    }

    /**
     *
     * @param id
     * @param categoryDTO
     * @return
     */
    @PostMapping("/update/{id}")
    public Result updateCategory(@PathVariable String id, @RequestBody CategoryDTO categoryDTO) {
        return categoryService.updateCategory(id, categoryDTO);
    }
}
