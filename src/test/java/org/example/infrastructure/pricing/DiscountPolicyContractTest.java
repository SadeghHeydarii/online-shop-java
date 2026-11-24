package org.example.infrastructure.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.exception.InvalidInputException;
import org.example.model.vo.Money;
import org.example.service.port.DiscountPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract Design-by-Contract (LSP) test suite for {@link DiscountPolicy} implementations.
 */
public abstract class DiscountPolicyContractTest {

    protected abstract DiscountPolicy createPolicy();

    private DiscountPolicy policy;
    private Money subtotal;
    private LocalDate evaluationDate;

    @BeforeEach
    void initContract() {
        policy = createPolicy();
        subtotal = new Money(new BigDecimal("100000.00"), "IRR");
        evaluationDate = LocalDate.of(2026, 11, 24);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankCouponCodeReturnsZeroInSubtotalCurrency(String blankCode) {
        Money discount = policy.calculateDiscount(subtotal, blankCode, evaluationDate);
        assertNotNull(discount);
        assertEquals("IRR", discount.currency());
        assertEquals(0, discount.amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void rejectsNullSubtotalOrEvaluationDate() {
        assertThrows(InvalidInputException.class, () -> policy.calculateDiscount(null, null, evaluationDate));
        assertThrows(InvalidInputException.class, () -> policy.calculateDiscount(subtotal, null, null));
    }
}
