package com.googlecode.dalvik.stax;

import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


public class HelloStax extends Activity {

  @Override public void onCreate(Bundle savedInstanceState) {

    /*
     * Configure which implementation of StAX that will be used by our
     * application. These properties and their values are obtained from the
     * META-INF/services/ directory inside the implementation .jar file.
     */
    System.setProperty("javax.xml.stream.XMLInputFactory",
        "com.sun.xml.stream.ZephyrParserFactory");
    System.setProperty("javax.xml.stream.XMLOutputFactory",
        "com.sun.xml.stream.ZephyrWriterFactory");
    System.setProperty("javax.xml.stream.XMLEventFactory",
        "com.sun.xml.stream.events.ZephyrEventFactory");

    /*
     * Ensure the factory implementation is loaded from the application
     * classpath (which contains the implementation classes), rather than the
     * system classpath (which doesn't).
     */
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

    try {
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      XMLEventReader reader = inputFactory.createXMLEventReader(
          new StringReader("<doc att=\"value\">some text</doc>"));
      while (reader.hasNext()) {
        XMLEvent e = reader.nextEvent();
        Log.e("HelloStax", "Event:[" + e + "]");
      }
    } catch (XMLStreamException e) {
      Log.e("HelloStax", "Error parsing XML", e);
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

}
