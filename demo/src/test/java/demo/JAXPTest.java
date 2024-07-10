package demo;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

// not really a demo, more an integration test that uses our demo testing infra
public class JAXPTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  static void go() throws Exception {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser saxParser = spf.newSAXParser();

    XMLReader xmlReader = saxParser.getXMLReader();
    MyHandler handler = new MyHandler();
    xmlReader.setContentHandler(handler);

    xmlReader.parse(new InputSource(new ByteArrayInputStream("<test>hello</test>".getBytes())));

    System.out.println(handler.startedElement);
    System.out.println(handler.characters);
  }

  @Test
  public void test() throws Exception {
    go();

    // in com.sun.org.apache.xerces.internal.util.SymbolTable, the names of tags are interned, which
    // makes them very hard to track
    demo.out().assertThatPart("test").isNotTracked();

    // but content is tracked
    demo.out().assertThatPart("hello").comesFromConstantInClass(JAXPTest.class);
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
