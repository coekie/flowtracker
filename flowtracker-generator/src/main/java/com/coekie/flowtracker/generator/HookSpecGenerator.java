package com.coekie.flowtracker.generator;

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

import com.coekie.flowtracker.annotation.Arg;
import com.coekie.flowtracker.annotation.Hook;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates "GeneratedHookSpecs" from annotations in Hook classes.
 * <p>
 * An alternative to this would have been to do this at runtime.
 * But this way searching for the classes and processing them is avoided at runtime. That also
 * limits how many classes we need to load before we can start instrumenting.
 * And having the generated code that can be inspected makes it a bit easier to see through the
 * annotation magic.
 * <p>
 * This is deliberately kept relatively primitive: we don't need a generally usable instrumentation
 * (~AOP) framework. This is arguably already overkill (but writing the equivalent of
 * GeneratedHookSpecs manually was getting annoying).
 */
public class HookSpecGenerator {
  private static final String root =
      Paths.get("").toAbsolutePath().getFileName().toString().equals("flowtracker-generator")
          ? "../" : "";
  static final Path OUTPUT_FILE =  Path.of(root +
      "flowtracker-weaver/target/generated-sources/hook-specs/com/coekie/flowtracker/"
      + "weaver/GeneratedHookSpecs.java");

  public static void main(String[] args) throws IOException {
    Files.createDirectories(OUTPUT_FILE.getParent());
    Files.write(OUTPUT_FILE, generate().getBytes());
  }

  static String generate() throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("package com.coekie.flowtracker.weaver;\n"
        + "\n"
        + "import static com.coekie.flowtracker.annotation.HookLocation.ON_ENTER;\n"
        + "import static com.coekie.flowtracker.annotation.HookLocation.ON_RETURN;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.ARG0;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.ARG1;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.ARG2;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.ARG3;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.INVOCATION;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.RETURN;\n"
        + "import static com.coekie.flowtracker.weaver.HookSpec.THIS;\n"
        + "import static com.coekie.flowtracker.weaver.HookArgs.*;\n"
        + "\n"
        + "import com.coekie.flowtracker.util.Config;\n"
        + "import javax.annotation.processing.Generated;\n"
        + "\n"
        + "@Generated(\"com.coekie.flowtracker.generator.HookSpecGenerator\")\n"
        + "class GeneratedHookSpecs {\n"
        + "  static HookSpecTransformer createTransformer(Config config) {\n"
        + "    int version = Runtime.version().feature();\n"
        + "    HookSpecTransformer t = new HookSpecTransformer();\n");

    Path sourceRoot = Path.of(root + "flowtracker-core/src/main/java");
    try (var list = Files.list(sourceRoot.resolve("com/coekie/flowtracker/hook"))) {
      list.sorted().forEach(path -> {
        try {
          String className = sourceRoot.relativize(path).toString()
              .replace(".java", "").replace('/', '.');
          Class<?> clazz =
              HookSpecGenerator.class.getClassLoader().loadClass(className);
          Stream.of(clazz.getDeclaredMethods()).sorted(Comparator.comparing(Method::getName))
              .forEach(method -> {
                for (Hook annotation : method.getAnnotationsByType(Hook.class)) {
                  generateHook(sb, method, annotation);
                }
              });
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      });
    }

    sb.append("    return t;\n"
        + "  }\n"
        + "}");
    return sb.toString();
  }

  private static void generateHook(StringBuilder sb, Method method, Hook annotation) {
    var targetMethod = org.objectweb.asm.commons.Method.getMethod(annotation.method());
    var hookMethod = org.objectweb.asm.commons.Method.getMethod(method);
    List<String> fixedArgs = Arrays.asList(
        str(annotation.target().replace('.', '/')),
        str(targetMethod.getName()),
        str(targetMethod.getDescriptor()),
        str(method.getDeclaringClass().getName().replace('.', '/')),
        str(hookMethod.getName()),
        str(hookMethod.getDescriptor()),
        annotation.location().name());

    List<String> varArgs = Stream.of(method.getParameterAnnotations())
        .map(parameterAnnotation -> Stream.of(parameterAnnotation)
            .filter(a -> a instanceof Arg)
            .map(a -> ((Arg) a).value())
            .findFirst()
            .orElseThrow())
        .collect(Collectors.toList());

    sb.append("    ");
    if (!annotation.condition().isEmpty()) {
      sb.append("if (").append(annotation.condition()).append(") ");
    }
    sb.append("t.register(");
    sb.append(String.join(",\n      ", fixedArgs));
    if (!varArgs.isEmpty()) {
      sb.append(",\n      ");
      sb.append(String.join(", ", varArgs));
    }
    sb.append(");\n");
  }

  private static String str(String str) {
    return "\"" + str + "\"";
  }
}
