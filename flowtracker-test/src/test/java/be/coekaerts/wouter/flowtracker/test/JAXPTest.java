package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

// test more like an integration test. perhaps it should live in a different module.
public class JAXPTest {

  @Test
  public void test() throws Exception {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser saxParser = spf.newSAXParser();

    XMLReader xmlReader = saxParser.getXMLReader();
    MyHandler handler = new MyHandler();
    xmlReader.setContentHandler(handler);

    String input = trackCopy("<test>hello</test>");
    xmlReader.parse(new InputSource(new ByteArrayInputStream(input.getBytes())));

    // in com.sun.org.apache.xerces.internal.util.SymbolTable, the names of tags are interned, which
    // makes them very hard to track
    assertThat(StringHook.getStringTracker(handler.startedElement)).isNull();

    // but content is tracked
    assertThatTracker(StringHook.getStringTracker(handler.characters)).matches(
        snapshot().trackString(5, input, 6));
  }

  private static class MyHandler extends DefaultHandler {
    String startedElement;
    String characters;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      startedElement = localName;
      super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      characters = new String(ch, start, length);
      super.characters(ch, start, length);
    }
  }
}
