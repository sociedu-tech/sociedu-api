package com.unishare.api.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Test
    void convertToEntityAttributePlainTextLegacyValueReturnsOriginalValue() {
        assertEquals("9876543210", converter.convertToEntityAttribute("9876543210"));
    }

    @Test
    void convertRoundTripEncryptedValue() {
        String encrypted = converter.convertToDatabaseColumn("123456789");

        assertNotEquals("123456789", encrypted);
        assertEquals("123456789", converter.convertToEntityAttribute(encrypted));
    }

    @Test
    void convertNullValue() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
