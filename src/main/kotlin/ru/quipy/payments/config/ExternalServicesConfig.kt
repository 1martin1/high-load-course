package ru.quipy.payments.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.payments.logic.ExternalServiceProperties
import ru.quipy.payments.logic.PaymentExternalServiceImpl
import java.time.Duration


@Configuration
class ExternalServicesConfig {
    companion object {
        const val ACC1_PAYMENT_BEAN = "ACC1_PAYMENT_BEAN"
        const val ACC2_PAYMENT_BEAN = "ACC2_PAYMENT_BEAN"
        const val ACC3_PAYMENT_BEAN = "ACC3_PAYMENT_BEAN"
        const val ACC4_PAYMENT_BEAN = "ACC4_PAYMENT_BEAN"

        // Ниже приведены готовые конфигурации нескольких аккаунтов провайдера оплаты.
        // Заметьте, что каждый аккаунт обладает своими характеристиками и стоимостью вызова.

        private val accountProps_1 = ExternalServiceProperties(
            // most expensive. Call costs 100 s=100 r/s
            "test",
            "default-1",
            parallelRequests = 10000,
            rateLimitPerSec = 100,
            request95thPercentileProcessingTime = Duration.ofMillis(1000),
        )

        private val accountProps_2 = ExternalServiceProperties(
            // Call costs 70, s=10 r/s
            "test",
            "default-2",
            parallelRequests = 100,
            rateLimitPerSec = 30,
            request95thPercentileProcessingTime = Duration.ofMillis(10_000),
        )

        private val accountProps_3 = ExternalServiceProperties(
            // Call costs 40 s=3 r/s
            "test",
            "default-3",
            parallelRequests = 30,
            rateLimitPerSec = 8,
            request95thPercentileProcessingTime = Duration.ofMillis(10_000),
        )

        // Call costs 30 s=0.8 r/s
        private val accountProps_4 = ExternalServiceProperties(
            "test",
            "default-4",
            parallelRequests = 8,
            rateLimitPerSec = 5,
            request95thPercentileProcessingTime = Duration.ofMillis(10_000),
        )
    }

    @Bean(ACC1_PAYMENT_BEAN)
    fun fastExternalService() =
        PaymentExternalServiceImpl(
            accountProps_1,
        )

    @Bean(ACC2_PAYMENT_BEAN)
    fun mediumExternalService() =
        PaymentExternalServiceImpl(
            accountProps_2,
        )

    @Bean(ACC3_PAYMENT_BEAN)
    fun slowExternalService() =
        PaymentExternalServiceImpl(
            accountProps_3,
        )

    @Bean(ACC4_PAYMENT_BEAN)
    fun superSlowExternalService() =
        PaymentExternalServiceImpl(
            accountProps_4,
        )
}