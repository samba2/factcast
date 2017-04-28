package org.factcast.store.pgsql.internal;

import lombok.experimental.UtilityClass;

/**
 * Constants for metrics-names.
 * 
 * @author <uwe.schaefer@mercateo.com>
 *
 */
@UtilityClass
public class PGMetrics {

    String FACT_PUBLISHING_FAILED = "factstore.pg.publish.failure";

    String FACT_PUBLISHED = "factstore.pg.publish.fact.count";

    String FACT_PUBLISHING_LATENCY = "factstore.pg.publish.fact.latency";

}