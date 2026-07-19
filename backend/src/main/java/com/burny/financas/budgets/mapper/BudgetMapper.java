package com.burny.financas.budgets.mapper;

import com.burny.financas.budgets.dto.BudgetResponse;
import com.burny.financas.budgets.entity.Budget;
import java.math.BigDecimal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    /** {@code spentAmount} isn't a column on {@code Budget} — it's computed per request, so it's passed in rather than mapped. */
    default BudgetResponse toResponse(Budget budget, BigDecimal spentAmount) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getCategory().getIcon(),
                budget.getCategory().getColor(),
                budget.getLimitAmount(),
                spentAmount,
                budget.getBudgetMonth(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
