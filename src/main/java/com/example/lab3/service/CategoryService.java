package com.example.lab3.service;

import com.example.lab3.dto.CategoryRequest;
import com.example.lab3.dto.CategoryResponse;
import com.example.lab3.entity.Category;
import com.example.lab3.exception.ConflictException;
import com.example.lab3.exception.ResourceNotFoundException;
import com.example.lab3.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Une catégorie porte déjà ce nom");
        }
        return toResponse(repository.save(new Category(name)));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(findCategory(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public Category findCategory(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id : " + id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
