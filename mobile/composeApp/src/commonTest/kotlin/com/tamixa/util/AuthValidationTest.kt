package com.tamixa.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [AuthValidation] (gap-fill: non-Auth coverage).
 */
class AuthValidationTest {

    @Test
    fun isEmailValid_emptyOrBlank_returnsFalse() {
        assertFalse(AuthValidation.isEmailValid(""))
        assertFalse(AuthValidation.isEmailValid("   "))
    }

    @Test
    fun isEmailValid_noAt_returnsFalse() {
        assertFalse(AuthValidation.isEmailValid("user"))
        assertFalse(AuthValidation.isEmailValid("user.domain.com"))
    }

    @Test
    fun isEmailValid_atOnlyAtStartOrEnd_returnsFalse() {
        assertFalse(AuthValidation.isEmailValid("@domain.com"))
        assertFalse(AuthValidation.isEmailValid("user@"))
    }

    @Test
    fun isEmailValid_validFormat_returnsTrue() {
        assertTrue(AuthValidation.isEmailValid("a@b.co"))
        assertTrue(AuthValidation.isEmailValid("user@example.com"))
        assertTrue(AuthValidation.isEmailValid("  user@example.com  "))
    }

    @Test
    fun isOtpCodeValid_tooShortOrTooLong_returnsFalse() {
        assertFalse(AuthValidation.isOtpCodeValid("123"))
        assertFalse(AuthValidation.isOtpCodeValid("123456789"))
        assertFalse(AuthValidation.isOtpCodeValid(""))
    }

    @Test
    fun isOtpCodeValid_nonDigits_returnsFalse() {
        assertFalse(AuthValidation.isOtpCodeValid("12a4"))
        assertFalse(AuthValidation.isOtpCodeValid("12 34"))
    }

    @Test
    fun isOtpCodeValid_validLengthDigits_returnsTrue() {
        assertTrue(AuthValidation.isOtpCodeValid("1234"))
        assertTrue(AuthValidation.isOtpCodeValid("123456"))
        assertTrue(AuthValidation.isOtpCodeValid("12345678"))
        assertTrue(AuthValidation.isOtpCodeValid("  5678  "))
    }
}
