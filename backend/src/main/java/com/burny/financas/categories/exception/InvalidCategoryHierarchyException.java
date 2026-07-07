package com.burny.financas.categories.exception;

/**
 * Thrown when a category hierarchy rule is violated, e.g. attempting to use a subcategory as the
 * parent of another category (max two levels).
 */
public class InvalidCategoryHierarchyException extends RuntimeException {
    public InvalidCategoryHierarchyException(String message) {
        super(message);
    }
}
