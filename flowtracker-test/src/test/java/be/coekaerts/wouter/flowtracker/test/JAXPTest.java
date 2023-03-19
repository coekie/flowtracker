package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

// test more like an integration test. perhaps it should live in a different module.
public class JAXPTest {
  @Ignore
  @Test
  public void test() throws Exception {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser saxParser = spf.newSAXParser();

    XMLReader xmlReader = saxParser.getXMLReader();
    MyHandler handler = new MyHandler();
    xmlReader.setContentHandler(handler);

    String input = trackCopy("<test></test>");
    xmlReader.parse(new InputSource(new ByteArrayInputStream(input.getBytes())));

    snapshotBuilder().trackString(input, 1, 4).assertTrackerOf(handler.startedElement);
  }

  private static class MyHandler extends DefaultHandler {
    String startedElement;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      startedElement = localName;
      super.startElement(uri, localName, qName, attributes);
    }
  }
}
