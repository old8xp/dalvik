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

import java.util.ArrayList;

/**
 * Is a hand-coded counted loop through an ArrayList cheaper than enhanced for?
 */
public class DalvikBenchmark extends SimpleBenchmark {
    ArrayList<Foo> mList = new ArrayList<Foo>();
    {
        for (int i = 0; i < 27; ++i) mList.add(new Foo());
    }
    public void timeArrayListIterationIndexed(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            int sum = 0;
            ArrayList<Foo> list = mList;
            int len = list.size();
            for (int i = 0; i < len; ++i) {
                sum += list.get(i).mSplat;
            }
        }
    }
    public void timeArrayListIterationForEach(int reps) {
        for (int rep = 0; rep < reps; ++rep) {
            int sum = 0;
            for (Foo a : mList) {
                sum += a.mSplat;
            }
        }
    }
}
