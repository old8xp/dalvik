/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.regression;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import java.util.BitSet;

public class BitSetBenchmark extends SimpleBenchmark {
    @Param({ "1000", "10000" })
    private int size;

    public void timeIsEmptyTrue(int reps) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < reps; ++i) {
            if (!bs.isEmpty()) throw new RuntimeException();
        }
    }

    public void timeIsEmptyFalse(int reps) {
        BitSet bs = new BitSet(size);
        bs.set(bs.size() - 1);
        for (int i = 0; i < reps; ++i) {
            if (bs.isEmpty()) throw new RuntimeException();
        }
    }

    public void timeGet(int reps) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < reps; ++i) {
            bs.get(i % size);
        }
    }

    public void timeClear(int reps) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < reps; ++i) {
            bs.clear(i % size);
        }
    }

    public void timeSet(int reps) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < reps; ++i) {
            bs.set(i % size);
        }
    }

    public void timeSetOn(int reps) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < reps; ++i) {
            bs.set(i % size, true);
        }
    }

    public void timeSetOff(int reps) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < reps; ++i) {
            bs.set(i % size, false);
        }
    }
}