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

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;

/**
 * What's the difference between a manual loop and System.arraycopy?
 */
public class ArrayCopyBenchmark extends SimpleBenchmark {
    public void timeArrayCopyManual(int reps) {
        char[] src = new char[8192];
        char[] dst = new char[8192];
        for (int rep = 0; rep < reps; ++rep) {
            for (int i = 0; i < 8192; ++i) {
                dst[i] = src[i];
            }
        }
    }
    public void timeArrayCopySystem(int reps) {
        char[] src = new char[8192];
        char[] dst = new char[8192];
        for (int rep = 0; rep < reps; ++rep) {
            System.arraycopy(src, 0, dst, 0, 8192);
        }
    }
}
