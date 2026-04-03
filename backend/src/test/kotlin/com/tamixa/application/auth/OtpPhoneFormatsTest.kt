package com.tamixa.application.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OtpPhoneFormatsTest {

    @Test
    fun normalize_indianTenDigits_adds91() {
        assertEquals("+919876543210", OtpPhoneFormats.normalize("9876543210"))
        assertEquals("+919876543210", OtpPhoneFormats.normalize(" 9876543210 "))
    }

    @Test
    fun normalize_leadingZeroIndian_stripsThenAdds91() {
        assertEquals("+919876543210", OtpPhoneFormats.normalize("09876543210"))
        assertEquals("+919876543210", OtpPhoneFormats.normalize("009876543210"))
    }

    @Test
    fun normalize_alreadyWithCountryCode() {
        assertEquals("+919876543210", OtpPhoneFormats.normalize("919876543210"))
        assertEquals("+919876543210", OtpPhoneFormats.normalize("+919876543210"))
    }

    @Test
    fun normalize_nonIndianTenDigits_usesPlusOnly() {
        assertEquals("+14155552671", OtpPhoneFormats.normalize("14155552671"))
    }

    @Test
    fun normalize_tooShort_null() {
        assertNull(OtpPhoneFormats.normalize("12345"))
    }

    @Test
    fun variants_includeNationalFor91() {
        val v = OtpPhoneFormats.variantsForStoredPhoneLookup("+919876543210")
        assert(v.contains("+919876543210"))
        assert(v.contains("9876543210"))
    }
}
