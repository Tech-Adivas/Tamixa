package com.tamixa.ui.viewmodel

import com.tamixa.domain.SubscriptionInfo
import com.tamixa.domain.UsageInfo
import com.tamixa.repository.ReferralCodeInfo
import com.tamixa.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
    private val scope: CoroutineScope
) {
    private val _subscription = MutableStateFlow<SubscriptionInfo?>(null)
    val subscription: StateFlow<SubscriptionInfo?> = _subscription.asStateFlow()

    private val _usage = MutableStateFlow<UsageInfo?>(null)
    val usage: StateFlow<UsageInfo?> = _usage.asStateFlow()

    private val _canceling = MutableStateFlow(false)
    val canceling: StateFlow<Boolean> = _canceling.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _appliedReferral = MutableStateFlow<ReferralCodeInfo?>(null)
    val appliedReferral: StateFlow<ReferralCodeInfo?> = _appliedReferral.asStateFlow()

    private val _referralError = MutableStateFlow<String?>(null)
    val referralError: StateFlow<String?> = _referralError.asStateFlow()

    fun loadSubscription() {
        scope.launch {
            val hasCached = _subscription.value != null
            if (!hasCached) _loading.value = true
            _loadError.value = null
            try {
                _subscription.value = repository.getSubscription()
                _usage.value = repository.getUsage()
            } catch (e: Throwable) {
                _loadError.value = e.message ?: "Failed to load subscription"
            } finally {
                _loading.value = false
            }
        }
    }

    fun applyReferralCode(code: String) {
        scope.launch {
            _referralError.value = null
            val trimmed = code.trim().uppercase()
            if (trimmed.isBlank()) {
                _appliedReferral.value = null
                return@launch
            }
            val info = repository.validateReferralCode(trimmed)
            if (info != null) {
                _appliedReferral.value = info
                _referralError.value = null
            } else {
                _appliedReferral.value = null
                _referralError.value = "Invalid or expired code"
            }
        }
    }

    fun clearReferralCode() {
        _appliedReferral.value = null
        _referralError.value = null
    }

    fun createCheckoutAndOpen(referralCode: String?, onUrl: (String) -> Unit, onFallback: () -> Unit) {
        scope.launch {
            val url = repository.createCheckoutSession(
                successUrl = null,
                cancelUrl = null,
                referralCode = referralCode?.takeIf { it.isNotBlank() }
            )
            if (!url.isNullOrBlank()) {
                onUrl(url)
            } else {
                onFallback()
            }
        }
    }

    fun cancelSubscription(onComplete: () -> Unit = {}) {
        scope.launch {
            _canceling.value = true
            if (repository.cancelSubscription()) {
                _subscription.value = repository.getSubscription()
            }
            _canceling.value = false
            onComplete()
        }
    }
}
