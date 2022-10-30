package be.coekaerts.wouter.flowtracker.test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import java.io.InputStream;
import org.junit.Test;

public class ClassTest {
  @Test public void getResourceAsStream() {
    InputStream stream = URLTest.class.getResourceAsStream("ClassTest.class");
    String descriptor =
        requireNonNull(InputStreamHook.getInputStreamTracker(stream)).getDescriptor();
    assertTrue(descriptor.startsWith("FileInputStream for "));
    assertTrue(descriptor.endsWith(ClassTest.class.getName().replace(".", "/") + ".class"));
  }
}
