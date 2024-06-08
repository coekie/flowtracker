package com.coekie.flowtracker.web;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.coekie.flowtracker.web.CodeResource.lineToPartMapping;
import static java.util.stream.Collectors.toList;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.CodeResource.CodeResponse;
import com.coekie.flowtracker.web.CodeResource.Line;
import com.coekie.flowtracker.web.TrackerResource.TrackerPartResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Get source code from the actual source files, e.g. -sources.jar files */
public class SourceCodeGenerator {
  static CodeResponse getCode(ClassOriginTracker tracker) {
    String s = getCodeString(tracker);
    if (s == null) {
      return null;
    }
    Map<Integer, List<TrackerPartResponse>> lineToPartMapping = lineToPartMapping(tracker);
    List<Line> result = new ArrayList<>();
    List<String> lines = s.lines().collect(toList());
    for (int i = 0; i < lines.size(); i++) {
      result.add(new Line(i + 1, lines.get(i) + '\n',
          lineToPartMapping.getOrDefault(i + 1, List.of())));
    }
    return new CodeResponse(result);
  }

  static String getCodeString(ClassOriginTracker tracker) {
    URL classUrl = getURL(tracker.loader, tracker.className + ".class");
    if (classUrl == null) {
      return null;
    }
    URL sourceUrl = guessSourceUrl(classUrl, tracker.sourceFile);
    if (sourceUrl == null) {
      return null;
    }
    try (InputStream is = sourceUrl.openStream()) {
      return new String(is.readAllBytes());
    } catch (IOException e) {
      return null;
    }
  }

  static URL guessSourceUrl(URL classUrl, String sourceFile) {
    try {
      // if it's in a *.jar file, look for a *-sources.jar file (maven repository convention)
      if (classUrl.getProtocol().equals("jar") && classUrl.getPath().contains(".jar!")) {
        return sourceUrl(classUrl, ".jar!", "-sources.jar!", sourceFile);
        // for class in target, look for conventional maven source path
      } else if (classUrl.getProtocol().equals("file")
          && classUrl.getPath().contains("/target/classes/")) {
        return sourceUrl(classUrl, "/target/classes/", "/src/main/java/", sourceFile);
      } else if (classUrl.getProtocol().equals("file")
          && classUrl.getPath().contains("/target/test-classes/")) {
        return sourceUrl(classUrl, "/target/test-classes/", "/src/test/java/", sourceFile);
      } else if (classUrl.getProtocol().equals("jrt")) {
        String innerPath = toSourcePath(classUrl.getPath(), sourceFile);
        if (innerPath != null) {
          return new URL("jar", "", -1,
              "file:" + System.getProperty("java.home") + "/lib/src.zip!" + innerPath);
        }
      }
      return null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private static URL sourceUrl(URL classUrl, String replaceThis, String withThis, String sourceFile)
      throws MalformedURLException {
    String sourcePath = toSourcePath(classUrl.getPath().replace(replaceThis, withThis), sourceFile);
    if (sourcePath == null) {
      return null;
    } else {
      return new URL(classUrl.getProtocol(), classUrl.getHost(), classUrl.getPort(), sourcePath);
    }
  }

  /**
   * Fixup the filename (the last part of the given path, after the last /) to find the source of
   * the .class file. In practice usually replacing `.class` with `.java`. Uses `sourceFile`
   * (filename referenced in class) when available.
   */
  private static String toSourcePath(String classPath, String sourceFile) {
    int slash = classPath.lastIndexOf('/');
    String filename;
    if (sourceFile != null) { // usual case: use the sourceFile attribute
      filename = sourceFile;
    } else { // guess source file name based on the .class file name
      if (!classPath.endsWith(".class")) {
        return null;
      }
      // name of file, without extension
      String simpleName = classPath.substring(slash + 1, classPath.length() - 6);
      int dollar = simpleName.indexOf('$'); // inner class
      if (dollar != -1) {
        simpleName = simpleName.substring(0, dollar);
      }
      filename = simpleName + ".java";
    }
    return classPath.substring(0, slash + 1) + filename;
  }

  private static URL getURL(ClassLoader loader, String path) {
    if (loader == null) { // class from the bootstrap classloader
      return String.class.getResource('/' + path);
    } else {
      return loader.getResource(path);
    }
  }

}
