package org.example.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.example.model.enums.PaymentMethod;
import org.example.model.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentTransaction Value Object Record Tests")
class PaymentTransactionTest {

    private final Money standardAmount = new Money(new BigDecimal("150000.00"), "IRR");
    private final LocalDateTime fixedTimestamp = LocalDateTime.of(2026, 10, 31, 10, 30, 0);

    @Nested
    @DisplayName("Creation and Invariants")
    class CreationAndInvariants {

        @Test
        @DisplayName("Valid payment transaction should construct properly and trim strings")
        void validTransactionConstructsProperly() {
            PaymentTransaction tx = new PaymentTransaction(
                    "  TXN-001  ",
                    "  ORD-100  ",
                    standardAmount,
                    PaymentMethod.ONLINE_GATEWAY,
                    PaymentStatus.SUCCESS,
                    fixedTimestamp,
                    "  REF-998877  "
            );

            assertEquals("TXN-001", tx.transactionId());
            assertEquals("ORD-100", tx.orderId());
            assertEquals(standardAmount, tx.amount());
            assertEquals(PaymentMethod.ONLINE_GATEWAY, tx.method());
            assertEquals(PaymentStatus.SUCCESS, tx.status());
            assertEquals(fixedTimestamp, tx.timestamp());
            assertEquals("REF-998877", tx.referenceCode());
        }

        @Test
        @DisplayName("Transaction with null or blank referenceCode should normalize to null")
        void nullOrBlankReferenceCodeNormalized() {
            PaymentTransaction txNull = new PaymentTransaction(
                    "TXN-001",
                    "ORD-100",
                    standardAmount,
                    PaymentMethod.WALLET,
                    PaymentStatus.SUCCESS,
                    fixedTimestamp,
                    null
            );
            assertNull(txNull.referenceCode());

            PaymentTransaction txBlank = new PaymentTransaction(
                    "TXN-001",
                    "ORD-100",
                    standardAmount,
                    PaymentMethod.WALLET,
                    PaymentStatus.SUCCESS,
                    fixedTimestamp,
                    "   "
            );
            assertNull(txBlank.referenceCode());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Null or blank transactionId should throw IllegalArgumentException")
        void invalidTransactionIdThrows(String invalidId) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new PaymentTransaction(invalidId, "ORD-100", standardAmount,
                            PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("transaction id"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Null or blank orderId should throw IllegalArgumentException")
        void invalidOrderIdThrows(String invalidOrderId) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new PaymentTransaction("TXN-001", invalidOrderId, standardAmount,
                            PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("order id"));
        }

        @Test
        @DisplayName("Null amount should throw IllegalArgumentException")
        void nullAmountThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new PaymentTransaction("TXN-001", "ORD-100", null,
                            PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("amount"));
        }

        @Test
        @DisplayName("Zero amount is valid and non-negative")
        void zeroAmountIsValid() {
            PaymentTransaction tx = new PaymentTransaction(
                    "TXN-001", "ORD-100", Money.ZERO,
                    PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"
            );
            assertEquals(Money.ZERO, tx.amount());
        }

        @Test
        @DisplayName("Null payment method should throw IllegalArgumentException")
        void nullMethodThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new PaymentTransaction("TXN-001", "ORD-100", standardAmount,
                            null, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("method"));
        }

        @Test
        @DisplayName("Null payment status should throw IllegalArgumentException")
        void nullStatusThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new PaymentTransaction("TXN-001", "ORD-100", standardAmount,
                            PaymentMethod.ONLINE_GATEWAY, null, fixedTimestamp, "REF-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("status"));
        }

        @Test
        @DisplayName("Null timestamp should throw IllegalArgumentException")
        void nullTimestampThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new PaymentTransaction("TXN-001", "ORD-100", standardAmount,
                            PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, null, "REF-1"));
            assertTrue(ex.getMessage().toLowerCase().contains("timestamp"));
        }
    }

    @Nested
    @DisplayName("Value Equality and HashCode")
    class ValueEquality {

        @Test
        @DisplayName("Transactions with equal attributes should be equal and have identical hash codes")
        void equalTransactions() {
            PaymentTransaction tx1 = new PaymentTransaction(
                    "TXN-001", "ORD-100", standardAmount,
                    PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"
            );
            PaymentTransaction tx2 = new PaymentTransaction(
                    "TXN-001", "ORD-100", standardAmount,
                    PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"
            );
            PaymentTransaction txDiff = new PaymentTransaction(
                    "TXN-002", "ORD-100", standardAmount,
                    PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-1"
            );

            assertEquals(tx1, tx2);
            assertEquals(tx1.hashCode(), tx2.hashCode());
            assertNotEquals(tx1, txDiff);
            assertNotEquals(tx1, null);
            assertNotEquals(tx1, "string");
        }

        @Test
        @DisplayName("toString should contain transaction attributes")
        void testToString() {
            PaymentTransaction tx = new PaymentTransaction(
                    "TXN-001", "ORD-100", standardAmount,
                    PaymentMethod.ONLINE_GATEWAY, PaymentStatus.SUCCESS, fixedTimestamp, "REF-99"
            );
            String str = tx.toString();
            assertTrue(str.contains("TXN-001"));
            assertTrue(str.contains("ORD-100"));
            assertTrue(str.contains("ONLINE_GATEWAY"));
            assertTrue(str.contains("SUCCESS"));
            assertTrue(str.contains("REF-99"));
        }
    }
}
