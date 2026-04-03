package com.tamixa.application.auth

/**
 * Canonical phone formats for OTP send/verify and parent lookup.
 * Indian mobiles: UI often sends 10 digits; we store E.164 (+91…).
 * Leading zeros (e.g. 0987…) must match the same canonical form as send and verify.
 */
internal object OtpPhoneFormats {

    fun normalize(phone: String): String? {
        var digits = phone.filter { it.isDigit() }
        while (digits.startsWith("0")) {
            digits = digits.drop(1)
        }
        if (digits.length !in 10..15) return null
        return if (digits.length == 10 && digits[0] in '6'..'9') {
            "+91$digits"
        } else {
            "+$digits"
        }
    }

    /**
     * Values to try when matching [Parent.phone] (legacy rows may omit country code).
     */
    fun variantsForStoredPhoneLookup(e164OrRaw: String): List<String> {
        val digits = e164OrRaw.filter { it.isDigit() }
        if (digits.length !in 10..15) return emptyList()
        val formats = mutableListOf<String>()
        formats += "+$digits"
        if (digits.length == 10 && digits[0] in '6'..'9') {
            formats += "+91$digits"
            formats += digits
        }
        if (digits.length == 11 && digits.startsWith("91")) {
            formats += digits.drop(2)
        }
        if (digits.length == 12 && digits.startsWith("91") && digits.getOrNull(2) in '6'..'9') {
            formats += digits.drop(2)
        }
        return formats.distinct()
    }
}
