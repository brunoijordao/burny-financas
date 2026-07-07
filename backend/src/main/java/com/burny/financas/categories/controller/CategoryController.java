package com.burny.financas.categories.controller;

import com.burny.financas.categories.dto.CategoryKeywordResponse;
import com.burny.financas.categories.dto.CategoryResponse;
import com.burny.financas.categories.dto.CreateCategoryKeywordRequest;
import com.burny.financas.categories.dto.CreateCategoryRequest;
import com.burny.financas.categories.dto.UpdateCategoryRequest;
import com.burny.financas.categories.service.CategoryKeywordService;
import com.burny.financas.categories.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categories", description = "Category and subcategory management, and keyword-based auto-categorization rules")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryKeywordService categoryKeywordService;

    @Operation(summary = "Create a new category or subcategory owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Invalid category data"),
            @ApiResponse(responseCode = "404", description = "Parent category not found or not owned by the caller"),
            @ApiResponse(responseCode = "422", description = "Parent category is itself a subcategory")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request, Authentication authentication) {
        return categoryService.create(currentUserId(authentication), request);
    }

    @Operation(summary = "List the authenticated user's active categories, with active subcategories nested")
    @GetMapping
    public List<CategoryResponse> list(Authentication authentication) {
        return categoryService.list(currentUserId(authentication));
    }

    @Operation(summary = "Get a single category or subcategory owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found or not owned by the caller")
    })
    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id, Authentication authentication) {
        return categoryService.get(currentUserId(authentication), id);
    }

    @Operation(summary = "Edit a category's or subcategory's name, icon, or color")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "404", description = "Category not found or not owned by the caller")
    })
    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request,
            Authentication authentication
    ) {
        return categoryService.update(currentUserId(authentication), id, request);
    }

    @Operation(summary = "Delete (soft-delete) a category or subcategory; deleting a top-level category cascades to its subcategories")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deactivated"),
            @ApiResponse(responseCode = "404", description = "Category not found or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        categoryService.delete(currentUserId(authentication), id);
    }

    @Operation(summary = "Register a new auto-categorization keyword on a category owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Keyword registered"),
            @ApiResponse(responseCode = "404", description = "Category not found or not owned by the caller"),
            @ApiResponse(responseCode = "409", description = "Keyword already associated with another of the caller's categories")
    })
    @PostMapping("/{id}/keywords")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryKeywordResponse createKeyword(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryKeywordRequest request,
            Authentication authentication
    ) {
        return categoryKeywordService.create(currentUserId(authentication), id, request);
    }

    @Operation(summary = "List the keywords registered on a category owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Keywords listed"),
            @ApiResponse(responseCode = "404", description = "Category not found or not owned by the caller")
    })
    @GetMapping("/{id}/keywords")
    public List<CategoryKeywordResponse> listKeywords(@PathVariable Long id, Authentication authentication) {
        return categoryKeywordService.list(currentUserId(authentication), id);
    }

    @Operation(summary = "Remove a keyword from a category owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Keyword removed"),
            @ApiResponse(responseCode = "404", description = "Category or keyword not found or not owned by the caller")
    })
    @DeleteMapping("/{id}/keywords/{keywordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKeyword(@PathVariable Long id, @PathVariable Long keywordId, Authentication authentication) {
        categoryKeywordService.delete(currentUserId(authentication), id, keywordId);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
