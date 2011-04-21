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

import android.util.JsonReader;
import android.util.JsonToken;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.json.JSONArray;
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
    private String json;
    private String xml;

    @Override protected void setUp() throws Exception {
        json = resourceToString("/tweets.json");
        xml = resourceToString("/tweets.xml");
    }

    public void timeParseStreamingJson(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            new TweetsJsonParser().parse(new StringReader(json));
        }
    }

    public void timeParseJsonObject(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            new JSONArray(json);
        }
    }

    public void timeParseStreamingXml(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            new TweetsXmlParser().parse(new StringReader(xml));
        }
    }

    public void timeParseXmlObject(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
        }
    }

    public void timeParseXmlSax(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            SAXParserFactory.newInstance().newSAXParser().parse(
                    new InputSource(new StringReader(xml)), new DefaultHandler());
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

    private static class TweetsJsonParser {
        public void parse(Reader reader) throws IOException {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                parseStatus(jsonReader);
            }
            jsonReader.endArray();
        }

        private void parseStatus(JsonReader jsonReader) throws IOException {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    continue;
                }

                if (name.equals("text")
                        || name.equals("created_at")
                        || name.equals("id_str")
                        || name.equals("source")
                        || name.equals("geo")
                        || name.equals("in_reply_to_status_id")
                        || name.equals("in_reply_to_user_id")
                        || name.equals("place")
                        || name.equals("in_reply_to_screen_name")
                        || name.equals("in_reply_to_status_id_str")
                        || name.equals("contributors")
                        || name.equals("in_reply_to_user_id_str")
                        || name.equals("coordinates")) {
                    jsonReader.nextString();
                } else if (name.equals("user")) {
                    parseUser(jsonReader);
                } else if (name.equals("favorited")
                        || name.equals("truncated")
                        || name.equals("retweeted")) {
                    jsonReader.nextBoolean();
                } else if (name.equals("retweet_count")) {
                    jsonReader.nextString(); // like '2' or "100+"
                } else if (name.equals("id")) {
                    jsonReader.nextLong();
                } else if (name.equals("retweeted_status")) {
                    parseStatus(jsonReader);
                } else {
                    throw new IllegalArgumentException("Unexpected name: " + name);
                }
            }
            jsonReader.endObject();
        }

        private void parseUser(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return;
            }
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    continue;
                }

                if (name.equals("friends_count")
                        || name.equals("favourites_count")
                        || name.equals("statuses_count")
                        || name.equals("followers_count")
                        || name.equals("id")
                        || name.equals("listed_count")
                        || name.equals("utc_offset")) {
                    jsonReader.nextInt();
                } else if (name.equals("profile_background_color")
                        || name.equals("profile_background_image_url")
                        || name.equals("created_at")
                        || name.equals("description")
                        || name.equals("lang")
                        || name.equals("id_str")
                        || name.equals("id_str")
                        || name.equals("profile_text_color")
                        || name.equals("profile_sidebar_fill_color")
                        || name.equals("profile_image_url")
                        || name.equals("url")
                        || name.equals("screen_name")
                        || name.equals("time_zone")
                        || name.equals("profile_link_color")
                        || name.equals("profile_sidebar_border_color")
                        || name.equals("location")
                        || name.equals("name")
                        || name.equals("display_url")
                        || name.equals("expanded_url")) {
                    jsonReader.nextString();
                } else if (name.equals("notifications")
                        || name.equals("default_profile_image")
                        || name.equals("default_profile_image")
                        || name.equals("show_all_inline_media")
                        || name.equals("contributors_enabled")
                        || name.equals("geo_enabled")
                        || name.equals("profile_background_tile")
                        || name.equals("follow_request_sent")
                        || name.equals("following")
                        || name.equals("protected")
                        || name.equals("verified")
                        || name.equals("is_translator")
                        || name.equals("default_profile")
                        || name.equals("profile_use_background_image")) {
                    jsonReader.nextBoolean();
                } else {
                    throw new IllegalArgumentException("Unexpected name: " + name);
                }
            }
            jsonReader.endObject();
        }

        private void parseValue(String name, JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return;
            }
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String k = jsonReader.nextName();
                JsonToken type = jsonReader.peek();
                jsonReader.skipValue();
                throw new IllegalArgumentException("unparsed value " + name + " " + k + " " + type);
            }
            jsonReader.endObject();
        }
    }

    private static class TweetsXmlParser {
        public void parse(Reader reader) throws Exception {
            XmlPullParser xmlParser = android.util.Xml.newPullParser();
            xmlParser.setInput(reader);
            xmlParser.nextTag();
            while (xmlParser.nextTag() == XmlPullParser.START_TAG) {
                if ("status".equals(xmlParser.getName())) {
                    parseStatus(xmlParser);
                } else {
                    throw new IllegalArgumentException("Unexpected name " + xmlParser.getName());
                }
            }
            reader.close();
        }

        private void parseStatus(XmlPullParser xmlParser) throws Exception {
            while (xmlParser.nextTag() == XmlPullParser.START_TAG) {
                String name = xmlParser.getName();
                if ("created_at".equals(name)
                        || "id".equals(name)
                        || "text".equals(name)
                        || "source".equals(name)
                        || "truncated".equals(name)
                        || "favorited".equals(name)
                        || "in_reply_to_status_id".equals(name)
                        || "in_reply_to_user_id".equals(name)
                        || "in_reply_to_screen_name".equals(name)
                        || "retweet_count".equals(name)
                        || "retweeted".equals(name)
                        || "geo".equals(name)
                        || "coordinates".equals(name)
                        || "place".equals(name)
                        || "contributors".equals(name)) {
                    parseString(name, xmlParser);
                } else if ("retweeted_status".equals(name)) {
                    parseStatus(xmlParser);
                } else if ("user".equals(name)) {
                    parseUser(xmlParser);
                } else {
                    throw new IllegalArgumentException("Unexpected name " + name);
                }
            }
        }

        private void parseUser(XmlPullParser xmlParser) throws Exception {
            while (xmlParser.nextTag() == XmlPullParser.START_TAG) {
                String name = xmlParser.getName();
                if ("id".equals(name)
                        || "name".equals(name)
                        || "screen_name".equals(name)
                        || "location".equals(name)
                        || "description".equals(name)
                        || "profile_image_url".equals(name)
                        || "url".equals(name)
                        || "protected".equals(name)
                        || "followers_count".equals(name)
                        || "profile_background_color".equals(name)
                        || "profile_text_color".equals(name)
                        || "profile_link_color".equals(name)
                        || "profile_sidebar_fill_color".equals(name)
                        || "profile_sidebar_border_color".equals(name)
                        || "friends_count".equals(name)
                        || "created_at".equals(name)
                        || "favourites_count".equals(name)
                        || "utc_offset".equals(name)
                        || "time_zone".equals(name)
                        || "profile_background_image_url".equals(name)
                        || "profile_background_tile".equals(name)
                        || "profile_use_background_image".equals(name)
                        || "notifications".equals(name)
                        || "geo_enabled".equals(name)
                        || "verified".equals(name)
                        || "following".equals(name)
                        || "statuses_count".equals(name)
                        || "lang".equals(name)
                        || "contributors_enabled".equals(name)
                        || "follow_request_sent".equals(name)
                        || "listed_count".equals(name)
                        || "show_all_inline_media".equals(name)
                        || "default_profile".equals(name)
                        || "default_profile_image".equals(name)
                        || "expanded_url".equals(name)
                        || "display_url".equals(name)
                        || "is_translator".equals(name)
                        ) {
                    parseString(name, xmlParser);
                } else {
                    throw new IllegalArgumentException("Unexpected name " + name);
                }
            }
        }

        private void parseString(String name, XmlPullParser xmlParser) throws Exception {
            int next = xmlParser.next();
            if (next == XmlPullParser.TEXT) {
                xmlParser.getText();
                next = xmlParser.next();
            }
            if (next != XmlPullParser.END_TAG) {
                throw new IllegalArgumentException("Unexpected token " + name + " " + next);
            }
        }
    }
}
