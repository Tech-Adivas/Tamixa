package com.araro.ui.viewmodel

import com.araro.domain.SubscriptionInfo
import com.araro.domain.UsageInfo
import com.araro.repository.SubscriptionRepository
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

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadSubscription() {
        scope.launch {
            _loading.value = true
            _subscription.value = repository.getSubscription()
            _usage.value = repository.getUsage()
            _loading.value = false
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
