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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateToStringBenchmark extends SimpleBenchmark {
    public void timeDateToString(int reps) throws Exception {
        Date d = new Date(0);
        for (int i = 0; i < reps; ++i) {
            d.toString();
        }
    }

    public void timeDateToString_Formatter(int reps) throws Exception {
        Date d = new Date(0);
        for (int i = 0; i < reps; ++i) {
            new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").format(d);
        }
    }
    
    public void timeDateToString_ClonedFormatter(int reps) throws Exception {
        Date d = new Date(0);
        SimpleDateFormat f = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        for (int i = 0; i < reps; ++i) {
            ((SimpleDateFormat) f.clone()).format(d);
        }
    }
}
