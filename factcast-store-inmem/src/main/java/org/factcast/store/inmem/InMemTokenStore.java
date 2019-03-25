/*
 * Copyright © 2018 Mercateo AG (http://www.mercateo.com)
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
package org.factcast.store.inmem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.factcast.core.store.StateToken;
import org.factcast.core.store.TokenStore;

class InMemTokenStore implements TokenStore {

    final Map<StateToken, Map<UUID, Optional<UUID>>> tokens = new HashMap<>();

    final Map<StateToken, String> namespaces = new HashMap<>();

    public StateToken create(String ns, Map<UUID, Optional<UUID>> state) {
        StateToken token = new StateToken();
        tokens.put(token, state);
        namespaces.put(token, ns);
        return token;
    }

    public void invalidate(StateToken token) {
        tokens.remove(token);
        namespaces.remove(token);
    }

    @Override
    public Map<UUID, Optional<UUID>> getState(StateToken token) {
        return tokens.get(token);
    }

    @Override
    public String getNs(StateToken token) {
        return namespaces.get(token);
    }
}
