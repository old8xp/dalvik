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

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import java.lang.reflect.*;

public class ReflectionBenchmark extends SimpleBenchmark {
    public void timeObject_getClass(int reps) throws Exception {
        C c = new C();
        for (int rep = 0; rep < reps; ++rep) {
            c.getClass();
        }
    }

    public void timeClass_getField(int reps) throws Exception {
        Class<?> klass = C.class;
        for (int rep = 0; rep < reps; ++rep) {
            klass.getField("f");
        }
    }

    public void timeClass_getDeclaredField(int reps) throws Exception {
        Class<?> klass = C.class;
        for (int rep = 0; rep < reps; ++rep) {
            klass.getDeclaredField("f");
        }
    }

    public void timeClass_getMethod(int reps) throws Exception {
        Class<?> klass = C.class;
        for (int rep = 0; rep < reps; ++rep) {
            klass.getMethod("m");
        }
    }

    public void timeClass_getDeclaredMethod(int reps) throws Exception {
        Class<?> klass = C.class;
        for (int rep = 0; rep < reps; ++rep) {
            klass.getDeclaredMethod("m");
        }
    }

    public void timeField_setInt(int reps) throws Exception {
        Class<?> klass = C.class;
        Field f = klass.getDeclaredField("f");
        C instance = new C();
        for (int rep = 0; rep < reps; ++rep) {
            f.setInt(instance, 1);
        }
    }

    public void timeField_getInt(int reps) throws Exception {
        Class<?> klass = C.class;
        Field f = klass.getDeclaredField("f");
        C instance = new C();
        for (int rep = 0; rep < reps; ++rep) {
            f.getInt(instance);
        }
    }

    public void timeMethod_invoke(int reps) throws Exception {
        Class<?> klass = C.class;
        Method m = klass.getDeclaredMethod("setField", int.class);
        C instance = new C();
        for (int rep = 0; rep < reps; ++rep) {
            m.invoke(instance, 1);
        }
    }

    public void timeMethod_invokePreBoxed(int reps) throws Exception {
        Class<?> klass = C.class;
        Method m = klass.getDeclaredMethod("setField", int.class);
        C instance = new C();
        Integer one = Integer.valueOf(1);
        for (int rep = 0; rep < reps; ++rep) {
            m.invoke(instance, one);
        }
    }

    public void timeRegularMethodInvocation(int reps) throws Exception {
        C instance = new C();
        for (int rep = 0; rep < reps; ++rep) {
            instance.setField(1);
        }
    }

    public static class C {
        public int f = 0;

        public void m() {
        }

        public void setField(int value) {
            f = value;
        }
    }
}
