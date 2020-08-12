/*
 * Copyright © 2017-2020 factcast.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.factcast.factus.lock;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.factcast.core.Fact;
import org.factcast.core.FactCast;
import org.factcast.core.lock.Attempt;
import org.factcast.core.lock.AttemptAbortedException;
import org.factcast.core.lock.IntermediatePublishResult;
import org.factcast.core.lock.PublishingResult;
import org.factcast.core.spec.FactSpec;
import org.factcast.factus.EventPojo;
import org.factcast.factus.Factus;
import org.factcast.factus.projection.Aggregate;
import org.factcast.factus.projection.ManagedProjection;
import org.factcast.factus.projection.Projection;
import org.factcast.factus.projection.SnapshotProjection;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@RequiredArgsConstructor
@Slf4j
@Data
public class Locked<I extends Projection> {
    @NonNull
    private final FactCast fc;

    @NonNull
    private final Factus factus;

    private final I projection;

    @NonNull
    private final List<FactSpec> specs;

    private Consumer<List<Fact>> andThen;// TODO

    int retries = 10;

    long intervalMillis = 0;

    public void attempt(BiConsumer<I, RetryableTransaction> tx) {
        attempt(tx, result -> null);
    }

    public <R> R attempt(BiConsumer<I, RetryableTransaction> tx, Function<List<Fact>, R> resultFn) {

        try {
            PublishingResult result = fc.lock(specs)
                    .optimistic()
                    .retry(retries())
                    .interval(intervalMillis())
                    .attempt(() -> {

                        try {
                            val p = update(projection);
                            List<Supplier<Fact>> toPublish = Collections.synchronizedList(
                                    new LinkedList<>());
                            RetryableTransaction lockedFactus = createTransaction(factus,
                                    toPublish);
                            tx.accept(p, lockedFactus);
                            IntermediatePublishResult intermediatePublishResult = Attempt.publish(
                                    toPublish.stream()
                                            .map(Supplier::get)
                                            .collect(Collectors.toList()));

                            return intermediatePublishResult;
                        } catch (LockedOperationAbortedException aborted) {
                            throw aborted;
                        } catch (Throwable e) {
                            throw LockedOperationAbortedException.wrap(e);
                        }

                    });

            return resultFn.apply(result.publishedFacts());

        } catch (AttemptAbortedException e) {
            throw LockedOperationAbortedException.wrap(e);
        }
    }

    private I update(I projection) {
        if (projection instanceof Aggregate) {
            Class<? extends Aggregate> projectionClass = (Class<? extends Aggregate>) projection
                    .getClass();
            return (I) factus.fetch(projectionClass, ((Aggregate) projection).id());
        }
        if (projection instanceof SnapshotProjection) {
            Class<? extends SnapshotProjection> projectionClass = (Class<? extends SnapshotProjection>) projection
                    .getClass();
            return (I) factus.fetch(projectionClass);
        }
        if (projection instanceof ManagedProjection) {
            factus.update((ManagedProjection) projection);
            return (I) projection;
        }
        throw new IllegalStateException("Don't know how to update " + projection);
    }

    private RetryableTransaction createTransaction(Factus factus, List<Supplier<Fact>> toPublish) {
        return new RetryableTransaction() {
            @Override
            public void publish(@NonNull EventPojo e) {
                toPublish.add(() -> factus.toFact(e));
            }

            @Override
            public void publish(@NonNull List<EventPojo> eventPojos) {
                eventPojos.forEach(this::publish);
            }

            @Override
            public void publish(@NonNull Fact e) {
                toPublish.add(() -> e);
            }

            @Override
            public <P extends SnapshotProjection> P fetch(@NonNull Class<P> projectionClass) {
                return factus.fetch(projectionClass);
            }

            @Override
            public <A extends Aggregate> Optional<A> find(
                    @NonNull Class<A> aggregateClass,
                    @NonNull UUID aggregateId) {
                return factus.find(aggregateClass, aggregateId);
            }

            @Override
            public <P extends ManagedProjection> void update(
                    @NonNull P managedProjection,
                    @NonNull Duration maxWaitTime) throws TimeoutException {
                factus.update(managedProjection, maxWaitTime);
            }
        };
    }
}
