package ru.quipy.payments.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.quipy.OnlineShopApplication
import ru.quipy.common.utils.NamedThreadFactory
import ru.quipy.core.EventSourcingService
import ru.quipy.orders.api.OrderAggregate
import ru.quipy.orders.api.OrderPaymentStartedEvent
import ru.quipy.orders.logic.setPaymentResults
import ru.quipy.payments.api.PaymentAggregate
import ru.quipy.payments.api.PaymentProcessedEvent
import ru.quipy.payments.config.ExternalServicesConfig
import ru.quipy.payments.logic.PaymentAggregateState
import ru.quipy.payments.logic.PaymentExternalServiceImpl
import ru.quipy.payments.logic.PaymentService
import ru.quipy.payments.logic.create
import ru.quipy.streams.AggregateSubscriptionsManager
import ru.quipy.streams.annotation.RetryConf
import ru.quipy.streams.annotation.RetryFailedStrategy
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct

@Service
class OrderPaymentSubscriber {

    var counter2 = AtomicInteger(0)
    var counter3 = AtomicInteger(0)
    var counter4 = AtomicInteger(0)
    val acc2speed: Float = 10F
    val acc3speed: Float = 3F
    val acc4speed: Float = 0.8F

    val logger: Logger = LoggerFactory.getLogger(OrderPaymentSubscriber::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager

    @Autowired
    private lateinit var paymentESService: EventSourcingService<UUID, PaymentAggregate, PaymentAggregateState>

    @Autowired
    @Qualifier(ExternalServicesConfig.ACC1_PAYMENT_BEAN)
    private lateinit var paymentService1: PaymentService

    @Autowired
    @Qualifier(ExternalServicesConfig.ACC2_PAYMENT_BEAN)
    private lateinit var paymentService2: PaymentService

    @Autowired
    @Qualifier(ExternalServicesConfig.ACC3_PAYMENT_BEAN)
    private lateinit var paymentService3: PaymentService

    @Autowired
    @Qualifier(ExternalServicesConfig.ACC4_PAYMENT_BEAN)
    private lateinit var paymentService4: PaymentService

    private val paymentExecutor = Executors.newFixedThreadPool(16, NamedThreadFactory("payment-executor"))

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(OrderAggregate::class, "payments:order-subscriber", retryConf = RetryConf(1, RetryFailedStrategy.SKIP_EVENT)) {
            `when`(OrderPaymentStartedEvent::class) { event ->
                paymentExecutor.submit {
                    val createdEvent = paymentESService.create {
                        it.create(
                            event.paymentId,
                            event.orderId,
                            event.amount
                        )
                    }
                    logger.info("Payment ${createdEvent.paymentId} for order ${event.orderId} created.")
                    val timeWeHave: Long = 80000 - (System.currentTimeMillis() - event.createdAt)
                    val time2accNeeded: Long = (counter2.get().toLong() * 1000 / acc2speed + 10000).toLong()
                    val time3accNeeded: Long = (counter3.get().toLong() * 1000 / acc3speed + 10000).toLong()
                    val time4accNeeded: Long = (counter4.get().toLong() * 1000 / acc4speed + 10000).toLong()

                    if (timeWeHave > time4accNeeded){
                        counter4.incrementAndGet()
                        paymentService4.submitPaymentRequest(createdEvent.paymentId, event.amount, event.createdAt)
                    }else if (timeWeHave > time3accNeeded){
                        counter3.incrementAndGet()
                        paymentService3.submitPaymentRequest(createdEvent.paymentId, event.amount, event.createdAt)
                    }else if (timeWeHave > time2accNeeded){
                        counter2.incrementAndGet()
                        paymentService2.submitPaymentRequest(createdEvent.paymentId, event.amount, event.createdAt)
                    }else {
                        paymentService1.submitPaymentRequest(createdEvent.paymentId, event.amount, event.createdAt)
                    }
                }
            }
        }

        subscriptionsManager.createSubscriber(PaymentAggregate::class, "payments:payment-subscriber", retryConf = RetryConf(1, RetryFailedStrategy.SKIP_EVENT)) {
            `when`(PaymentProcessedEvent::class) { event ->
                when (event.account) {
                    "default-2" -> {
                        PaymentExternalServiceImpl.logger.warn("!!!!! ${event.account} have ${counter2.get()}")
                        counter2.decrementAndGet()
                    }
                    "default-3" -> {
                        PaymentExternalServiceImpl.logger.warn("!!!!! ${event.account} have ${counter3.get()}")
                        counter3.decrementAndGet()
                    }
                    "default-4" -> {
                        PaymentExternalServiceImpl.logger.warn("!!!!! ${event.account} have ${counter4.get()}")
                        counter4.decrementAndGet()
                    }
                }
            }
        }
    }
}