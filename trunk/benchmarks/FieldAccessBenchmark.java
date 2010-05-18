/*
 * Copyright (C) 2010 Google Inc.
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

package benchmarks;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * What does field access cost?
 */
public class FieldAccessBenchmark extends SimpleBenchmark {
    int intVal = 42;
    final int finalIntVal = 42;
    static int staticIntVal = 42;
    static final int staticFinalIntVal = 42;
    public int timeField(int reps) {
        int result = 0;
        for (int rep = 0; rep < reps; ++rep) {
            result = intVal;
        }
        return result;
    }
    public int timeFieldFinal(int reps) {
        int result = 0;
        for (int rep = 0; rep < reps; ++rep) {
            result = finalIntVal;
        }
        return result;
    }
    public int timeFieldStatic(int reps) {
        int result = 0;
        for (int rep = 0; rep < reps; ++rep) {
            result = staticIntVal;
        }
        return result;
    }
    public int timeFieldStaticFinal(int reps) {
        int result = 0;
        for (int rep = 0; rep < reps; ++rep) {
            result = staticFinalIntVal;
        }
        return result;
    }
    public int timeFieldCached(int reps) {
        int result = 0;
        int cachedIntVal = this.intVal;
        for (int rep = 0; rep < reps; ++rep) {
            result = cachedIntVal;
        }
        return result;
    }
}
