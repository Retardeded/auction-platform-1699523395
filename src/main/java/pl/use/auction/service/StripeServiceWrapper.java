package pl.use.auction.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class StripeServiceWrapper {

    @Value("${stripe.api.secretkey}")
    private String stripeApiKey;

    public Session createCheckoutSession(SessionCreateParams params) throws StripeException {
        Stripe.apiKey = this.stripeApiKey;
        return Session.create(params);
    }

    public String createPaymentIntent(BigDecimal buyNowPrice, String currency) throws StripeException {
        Stripe.apiKey = this.stripeApiKey;

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(buyNowPrice.multiply(new BigDecimal(100)).longValue())
                .setCurrency(currency)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }
}