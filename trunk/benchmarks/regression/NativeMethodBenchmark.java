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

package benchmarks.regression;

import com.google.caliper.SimpleBenchmark;
import org.apache.harmony.dalvik.NativeTestTarget;

public class NativeMethodBenchmark extends SimpleBenchmark {
    public void timeEmptyJniStaticMethod0(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            NativeTestTarget.emptyJniStaticMethod0();
        }
    }

    public void timeEmptyJniStaticMethod6(int reps) throws Exception {
        int a = -1;
        int b = 0;
        for (int i = 0; i < reps; ++i) {
            NativeTestTarget.emptyJniStaticMethod6(a, b, 1, 2, 3, i);
        }
    }

    public void timeEmptyJniStaticMethod6L(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            NativeTestTarget.emptyJniStaticMethod6L(null, null, null, null, null, null);
        }
    }

    public void timeEmptyInlineMethod(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            NativeTestTarget.emptyInlineMethod();
        }
    }

    public void timeEmptyInternalStaticMethod(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            NativeTestTarget.emptyInternalStaticMethod();
        }
    }
}
