package org.factcast.store.pgsql.internal;

import org.factcast.core.Fact;
import org.factcast.core.subscription.Subscription;
import org.factcast.core.subscription.SubscriptionImpl;
import org.factcast.core.subscription.SubscriptionRequestTO;
import org.factcast.core.subscription.Subscriptions;
import org.factcast.core.subscription.observer.FactObserver;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.eventbus.EventBus;

import lombok.RequiredArgsConstructor;

/**
 * Creates Subscription
 * 
 * @author uwe.schaefer@mercateo.com
 *
 */
// TODO integrate with PGQuery
@RequiredArgsConstructor
class PGSubscriptionFactory {
    private final JdbcTemplate jdbcTemplate;

    private final EventBus eventBus;

    private final PGFactIdToSerMapper idToSerialMapper;

    public Subscription subscribe(SubscriptionRequestTO req, FactObserver observer) {
        final SubscriptionImpl<Fact> subscription = Subscriptions.on(observer);

        PGFactStream pgsub = new PGFactStream(jdbcTemplate, eventBus, idToSerialMapper);
        pgsub.connect(req, subscription);

        return subscription.onClose(pgsub::close);
    }

}
