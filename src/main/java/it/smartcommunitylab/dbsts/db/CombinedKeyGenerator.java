/**
 * Copyright 2025 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.dbsts.db;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.util.Assert;

public class CombinedKeyGenerator implements StringKeyGenerator {

    private static final String DEFAULT_SPACE = "_";

    private List<StringKeyGenerator> generators;

    public CombinedKeyGenerator() {
        this(new HumanStringKeyGenerator());
    }

    public CombinedKeyGenerator(StringKeyGenerator... generators) {
        this(Arrays.asList(generators));
    }

    public CombinedKeyGenerator(List<StringKeyGenerator> generators) {
        Assert.notNull(generators, "generators are required");
        Assert.isTrue(!generators.isEmpty(), "generators are required");

        this.generators = generators;
    }

    @Override
    public String generateKey() {
        return String.join(DEFAULT_SPACE, generators.stream().map(g -> g.generateKey()).toList());
    }
}
