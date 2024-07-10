package com.coekie.flowtracker.web;

import static com.coekie.flowtracker.web.CodeResourceTest.findLine;
import static com.coekie.flowtracker.web.SourceCodeGenerator.guessSourceUrl;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.CodeResource.CodeResponse;
import com.coekie.flowtracker.web.CodeResource.Line;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

public class SourceCodeGeneratorTest {
  @Test
  public void testGetCode() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(
        ExampleForSource.class.getClassLoader(),
        ExampleForSource.class.getName().replace('.', '/'), null);
    tracker.registerConstantString("line 9", 9);

    CodeResponse response = SourceCodeGenerator.getCode(tracker);
    assertThat(response).isNotNull();

    Line line9 = findLine(response, 9);
    assertThat(line9.content).isEqualTo("    System.out.println(\"line 9\");\n");
    assertThat(line9.parts).hasSize(1);
    assertThat(line9.parts.get(0).trackerId).isEqualTo(tracker.getTrackerId());
    assertThat(tracker.getContent().subSequence(
        line9.parts.get(0).offset,
        line9.parts.get(0).offset + line9.parts.get(0).length).toString())
        .isEqualTo("line 9");
  }

  /** Test all the ways we try to guess source URLs */
  @Test
  public void testGuessSourceUrl() throws MalformedURLException {
    // invalid stuff - don't really care about the result, just don't throw
    assertThat(guessSourceUrl(new URL("jar:file:whatever!/f"), null)).isNull();
    assertThat(guessSourceUrl(new URL("jar:file:/foo/bar-1.jar!/pkg/Hello.huh"), null)).isNull();

    // maven repository -sources.jar
    // use sourceFile
    assertThat(guessSourceUrl(new URL("jar:file:/foo/bar-1.jar!/pkg/Hello.class"), "Hey.java"))
        .isEqualTo(new URL("jar:file:/foo/bar-1-sources.jar!/pkg/Hey.java"));
    // if no sourceFile, guess based on .class name
    assertThat(guessSourceUrl(new URL("jar:file:/foo/bar-1.jar!/pkg/Hello.class"), null))
        .isEqualTo(new URL("jar:file:/foo/bar-1-sources.jar!/pkg/Hello.java"));
    // handle inner classes when guessing based on .class name
    assertThat(guessSourceUrl(new URL("jar:file:/foo/bar-1.jar!/pkg/Hello$1.class"), null))
        .isEqualTo(new URL("jar:file:/foo/bar-1-sources.jar!/pkg/Hello.java"));

    // maven src/main and src/test convention
    assertThat(guessSourceUrl(new URL("file:/foo/target/classes/bar/Hello.class"), "Hey.java"))
        .isEqualTo(new URL("file:/foo/src/main/java/bar/Hey.java"));
    assertThat(guessSourceUrl(new URL("file:/foo/target/test-classes/bar/Hello.class"), "Hey.java"))
        .isEqualTo(new URL("file:/foo/src/test/java/bar/Hey.java"));

    // jdk
    assertThat(guessSourceUrl(new URL("jrt:/java.base/java/lang/String.class"), "Hey.java"))
        .isEqualTo(new URL("jar:file:" + System.getProperty("java.home")
            + "/lib/src.zip!/java.base/java/lang/Hey.java"));
  }

  /** As far as we can, test actually finding sources in various ways. */
  @Test
  public void testGetCodeString() {
    // this only works if sources have been downloaded. "works for me", but too dependent on the
    // environment
    //assertThat(getSourceString(Opcodes.class)).contains("class Opcodes");

    assertThat(getCodeString(SourceCodeGenerator.class))
        .contains("class SourceCodeGenerator");
    assertThat(getCodeString(SourceCodeGeneratorTest.class))
        .contains("Whatever you want. This String.");

    assertThat(getCodeString(String.class)).contains("class String");
  }

  private static String getCodeString(Class<?> clazz) {
    return SourceCodeGenerator.getCodeString(ClassOriginTracker.get(clazz));
  }
}
