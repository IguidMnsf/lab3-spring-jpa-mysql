package com.example.lab3.controller;

import com.example.lab3.dto.CategoryRequest;
import com.example.lab3.dto.CategoryResponse;
import com.example.lab3.dto.ProductResponse;
import com.example.lab3.service.CategoryService;
import com.example.lab3.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @GetMapping("/{id}/products")
    public List<ProductResponse> getProducts(@PathVariable Long id) {
        return productService.findByCategory(id);
    }
}
