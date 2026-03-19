package com.tamixa.application.auth

class ParentNotFoundException(email: String) : RuntimeException("Parent not found: $email")
