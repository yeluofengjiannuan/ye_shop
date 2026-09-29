package com.itxindeshang.controller.admin;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.pojo.dto.CategoryDTO;
import com.itxindeshang.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "分类管理")
public class CategoryController {
    @Resource
    private CategoryService categoryService;

    /**
     * 获取分类树
     *
     * @return
     */
    @GetMapping("/category/tree")
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
    @PostMapping("/admin/category/add")
    public Result addCategory(@RequestBody CategoryDTO categoryDTO) {
        return categoryService.addCategory(categoryDTO);
    }

    /**
     * 删除分类
     * TODO：后续再考虑逻辑删除
     * @param categoryId
     * @return
     */
    @DeleteMapping("/admin/category/{categoryId}")
    public Result deleteCategory(@PathVariable Long categoryId) {
        return categoryService.deleteById(categoryId);
    }

    /**
     * 更新分类
     * @param categoryId
     * @param categoryDTO
     * @return
     */
    @PostMapping("/admin/category/{categoryId}")
    public Result updateCategory(@PathVariable Long categoryId, @RequestBody CategoryDTO categoryDTO) {
        return categoryService.updateCategory(categoryId, categoryDTO);
    }
}
