/**
 * Copyright 2024 the original author or authors
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

import java.security.SecureRandom;
import java.util.Random;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.util.Assert;

public class HumanStringKeyGenerator implements StringKeyGenerator {

    private static final int DEFAULT_KEY_LENGTH = 12;
    private static final char[] DEFAULT_SPACE =
        "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-".toCharArray();

    private Random random = new SecureRandom();
    private int length;
    private final char[] space;

    public HumanStringKeyGenerator() {
        this(DEFAULT_KEY_LENGTH);
    }

    public HumanStringKeyGenerator(int length) {
        this(length, DEFAULT_SPACE);
    }

    public HumanStringKeyGenerator(int length, char[] space) {
        Assert.isTrue(length > 1, "length must be major than 1");
        Assert.notNull(space, "char space is required");

        this.length = length;
        this.space = space;
    }

    @Override
    public String generateKey() {
        //generate a random byte buffer
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        //convert random bytes to the valid char space
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = space[((bytes[i] & 0xFF) % space.length)];
        }

        //as string
        return new String(chars);
    }
}
