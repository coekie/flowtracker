package com.coekie.flowtracker.agent;

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

import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.CoreInitializer;
import java.io.IOException;

/** Main class when not running as an agent */
public class Main {
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      usage();
    } else switch (args[0]) {
      case "jvmopts":
        System.out.println(CoreInitializer.JVM_OPTS);
        break;
      case "usage":
        usage();
        break;
      default:
        System.err.println("Unrecognized command '" + args[0] + "'");
        usage();
    }
  }

  private static void usage() throws IOException {
    try (var in = Main.class.getResourceAsStream("/USAGE.md")) {
      requireNonNull(in).transferTo(System.out);
    }
  }
}
