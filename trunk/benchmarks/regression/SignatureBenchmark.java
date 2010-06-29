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
import com.google.caliper.SimpleBenchmark;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;
import org.apache.harmony.xnet.provider.jsse.OpenSSLSignature;

/**
 * The OpenSSL implementation can currently only verify signatures,
 * not compute them, so we use the BouncyCastle implementation during
 * setup to do that part of the test. Typically that computation is
 * done by something like the jarsigner on the host.
 */
public class SignatureBenchmark extends SimpleBenchmark {

    private static final int DATA_SIZE = 8192;
    private static final byte[] DATA = new byte[DATA_SIZE];
    static {
        for (int i = 0; i < DATA_SIZE; i++) {
            DATA[i] = (byte)i;
        }
    }
    @Param private Algorithm algorithm;

    public enum Algorithm {
        MD5WithRSA,
        SHA1WithRSA,
        SHA256WithRSA,
        SHA384WithRSA,
        SHA512WithRSA,
        SHA1withDSA
    };

    @Param private Implementation implementation;

    public enum Implementation { OpenSSL, BouncyCastle };

    // Key generation and signing aren't part of the benchmark so cache the results
    private static Map<String,KeyPair> KEY_PAIRS = new HashMap<String,KeyPair>();
    private static Map<String,byte[]> SIGNATURES = new HashMap<String,byte[]>();

    private String signatureAlgorithm;
    private byte[] signature;
    private PublicKey publicKey;

    @Override protected void setUp() throws Exception {
        this.signatureAlgorithm = algorithm.toString();

        String keyAlgorithm = signatureAlgorithm.substring(signatureAlgorithm.length() - 3 ,
                                                           signatureAlgorithm.length());
        KeyPair keyPair = KEY_PAIRS.get(keyAlgorithm);
        if (keyPair == null) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(keyAlgorithm);
            keyPair = generator.generateKeyPair();
            KEY_PAIRS.put(keyAlgorithm, keyPair);
        }
        this.publicKey = keyPair.getPublic();

        this.signature = SIGNATURES.get(signatureAlgorithm);
        if (this.signature == null) {
            Signature signer = Signature.getInstance(signatureAlgorithm);
            signer.initSign(keyPair.getPrivate());
            signer.update(DATA);
            this.signature = signer.sign();
            SIGNATURES.put(signatureAlgorithm, signature);
        }
    }

    public void time(int reps) throws Exception {
        for (int i = 0; i < reps; ++i) {
            Signature verifier;
            switch (implementation) {
                case OpenSSL:
                    verifier = OpenSSLSignature.getInstance(signatureAlgorithm);
                    break;
                case BouncyCastle:
                    verifier = Signature.getInstance(signatureAlgorithm);
                    break;
                default:
                    throw new RuntimeException(implementation.toString());
            }
            verifier.initVerify(publicKey);
            verifier.update(DATA);
            verifier.verify(signature);
        }
    }
}
