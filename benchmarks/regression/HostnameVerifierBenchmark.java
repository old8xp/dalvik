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
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public final class HostnameVerifierBenchmark extends SimpleBenchmark {

    @Param({"www.amazon.com", "www.google.com", "www.ubs.com"}) String host;

    private String hostname;
    private SSLSession sslSession;
    private HostnameVerifier hostnameVerifier;

    @Override protected void setUp() throws Exception {
        URL url = new URL("https", host, "/");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        hostnameVerifier = connection.getHostnameVerifier();
        connection.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession sslSession) {
                HostnameVerifierBenchmark.this.hostname = hostname;
                HostnameVerifierBenchmark.this.sslSession = sslSession;
                return true;
            }
        });
        connection.getInputStream();
    }

    public void timeVerify(int reps) {
        for (int i = 0; i < reps; i++) {
            hostnameVerifier.verify(hostname, sslSession);
        }
    }

    public static void main(String[] args) {
        Runner.main(HostnameVerifierBenchmark.class, args);
    }
}
