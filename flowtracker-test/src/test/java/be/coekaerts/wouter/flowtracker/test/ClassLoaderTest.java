package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class ClassLoaderTest {
  @Test public void testHideClassLoading() {
    // make sure the Before class is loaded
    //noinspection ResultOfMethodCallIgnored
    Before.class.getMethods();

    forEachNode(TrackerTree.ROOT, node -> {
      // test that the loading of classes was hidden.
      // "Before.class" to test loading of classes in a jar (handled in ZipFileHook),
      // and "ClassLoaderTest.class" to test loading outside a jar (handled in FileInputStreamHook).
      // this assumes that "hideInternals" is enabled when running this test.
      if (node.name.equals("Before.class") || node.name.equals("ClassLoaderTest.class")) {
        // if this fails, enable trackCreation to debug why
        throw new AssertionError(
            "Should have been hidden: " + node.path().toString() + creationStackTrace(node));
      }
    });
  }

  private void forEachNode(Node node, Consumer<Node> consumer) {
    consumer.accept(node);
    for (Node child : node.children()) {
      forEachNode(child, consumer);
    }
  }

  private static String creationStackTrace(Node node) {
    if (!node.trackers().isEmpty() && node.trackers().get(0).getCreationStackTrace() != null) {
      return Stream.of(node.trackers().get(0).getCreationStackTrace())
          .map(StackTraceElement::toString)
          .collect(Collectors.joining("\n", "\n", "\n"));
    } else {
      return "";
    }
  }
}
