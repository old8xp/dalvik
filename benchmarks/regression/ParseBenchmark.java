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
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;

/**
 * Measure throughput of various parsers.
 *
 * <p>This benchmark requires that ParseBenchmarkData.zip is on the classpath.
 * That file contains Twitter feed data, which is representative of what
 * applications will be parsing.
 *
 * <p>The parsers attempt to do what a real parser would do: match properties by
 * name and read their values as their proper type. Unlike a real parser, this
 * benchmark discards the values as they're read.
 */
public final class ParseBenchmark extends SimpleBenchmark {

    @Param Document document;
    @Param Api api;

    private enum Document {
        TWEETS,
        READER_SHORT,
        READER_LONG
    }

    private enum Api {
        GSON_STREAM("json") {
            @Override Parser newParser() {
                return new GeneralGsonStreamingParser();
            }
        },
        GSON_DOM("json") {
            @Override Parser newParser() {
                return new GsonDomParser();
            }
        },
        ORG_JSON("json") {
            @Override Parser newParser() {
                return new OrgJsonParser();
            }
        },
        XML_PULL("xml") {
            @Override Parser newParser() {
                return new GeneralXmlPullParser();
            }
        },
        XML_DOM("xml") {
            @Override Parser newParser() {
                return new XmlDomParser();
            }
        },
        XML_SAX("xml") {
            @Override Parser newParser() {
                return new XmlSaxParser();
            }
        };

        final String extension;

        private Api(String extension) {
            this.extension = extension;
        }

        abstract Parser newParser();
    }

    private String text;
    private Parser parser;

    @Override protected void setUp() throws Exception {
        text = resourceToString("/" + document.name() + "." + api.extension);
        parser = api.newParser();
    }

    public void timeParse(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            parser.parse(text);
        }
    }

    public static void main(String... args) throws Exception {
        Runner.main(ParseBenchmark.class, args);
    }

    private static String resourceToString(String path) throws Exception {
        InputStream in = ParseBenchmark.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalArgumentException("No such file: " + path);
        }

        Reader reader = new InputStreamReader(in, "UTF-8");
        char[] buffer = new char[8192];
        StringWriter writer = new StringWriter();
        int count;
        while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
        }
        reader.close();
        return writer.toString();
    }

    // TODO: Use Java 7 String switch in the inner parsers

    interface Parser {
        void parse(String data) throws Exception;
    }

    private static class GeneralGsonStreamingParser implements Parser {
        @Override public void parse(String data) throws Exception {
            com.google.gson.stream.JsonReader jsonReader
                    = new com.google.gson.stream.JsonReader(new StringReader(data));
            readToken(jsonReader);
        }

        public void readObject(com.google.gson.stream.JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                reader.nextName();
                readToken(reader);
            }
            reader.endObject();
        }

        public void readArray(com.google.gson.stream.JsonReader reader) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                readToken(reader);
            }
            reader.endArray();
        }

        private void readToken(com.google.gson.stream.JsonReader reader) throws IOException {
            switch (reader.peek()) {
            case BEGIN_ARRAY:
                readArray(reader);
                break;
            case BEGIN_OBJECT:
                readObject(reader);
                break;
            case BOOLEAN:
                reader.nextBoolean();
                break;
            case NULL:
                reader.nextNull();
                break;
            case NUMBER:
                reader.nextLong();
                break;
            case STRING:
                reader.nextString();
                break;
            default:
                throw new IllegalArgumentException("Unexpected token" + reader.peek());
            }
        }
    }

    private static class GsonDomParser implements Parser {
        @Override public void parse(String data) throws Exception {
            new JsonParser().parse(data);
        }
    }

    private static class OrgJsonParser implements Parser {
        @Override public void parse(String data) throws Exception {
        }
    }

    private static class GeneralXmlPullParser implements Parser {
        @Override public void parse(String data) throws Exception {
        }
    }

    private static class XmlDomParser implements Parser {
        @Override public void parse(String data) throws Exception {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(data)));
        }
    }

    private static class XmlSaxParser implements Parser {
        @Override public void parse(String data) throws Exception {
            SAXParserFactory.newInstance().newSAXParser().parse(
                    new InputSource(new StringReader(data)), new DefaultHandler());
        }
    }
}
