package org.example.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category Record Tests")
class CategoryTest {

    @Nested
    @DisplayName("Creation and Invariants")
    class CreationAndInvariants {

        @Test
        @DisplayName("Valid category should construct properly and trim whitespace")
        void validCategoryConstructsProperly() {
            Category category = new Category(" CAT-1 ", " Electronics ", " Gadgets and devices ");
            assertEquals("CAT-1", category.id());
            assertEquals("Electronics", category.name());
            assertEquals("Gadgets and devices", category.description());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Null or blank ID should throw IllegalArgumentException")
        void invalidIdThrows(String invalidId) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Category(invalidId, "Electronics", "Devices"));
            assertTrue(ex.getMessage().toLowerCase().contains("id"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Null or blank name should throw IllegalArgumentException")
        void invalidNameThrows(String invalidName) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Category("CAT-1", invalidName, "Devices"));
            assertTrue(ex.getMessage().toLowerCase().contains("name"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Null or blank description should throw IllegalArgumentException")
        void invalidDescriptionThrows(String invalidDesc) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Category("CAT-1", "Electronics", invalidDesc));
            assertTrue(ex.getMessage().toLowerCase().contains("description"));
        }
    }

    @Nested
    @DisplayName("Value Equality and HashCode")
    class ValueEquality {

        @Test
        @DisplayName("Categories with equal values should be equal and have identical hash codes")
        void equalCategories() {
            Category cat1 = new Category("CAT-1", "Books", "Reading materials");
            Category cat2 = new Category("CAT-1", "Books", "Reading materials");
            Category cat3 = new Category("CAT-2", "Books", "Reading materials");

            assertEquals(cat1, cat2);
            assertEquals(cat1.hashCode(), cat2.hashCode());
            assertNotEquals(cat1, cat3);
            assertNotEquals(cat1, null);
            assertNotEquals(cat1, "someString");
        }

        @Test
        @DisplayName("toString should contain category fields")
        void testToString() {
            Category cat = new Category("CAT-1", "Books", "Reading materials");
            String str = cat.toString();
            assertTrue(str.contains("CAT-1"));
            assertTrue(str.contains("Books"));
            assertTrue(str.contains("Reading materials"));
        }
    }
}
