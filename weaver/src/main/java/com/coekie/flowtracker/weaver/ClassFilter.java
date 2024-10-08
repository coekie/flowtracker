package com.coekie.flowtracker.weaver;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Parses and applies rules for what classes to include in instrumentation.
 * <p>
 * Comma-separated list of rules. Rules can either start with a '+' to include some classes
 * or with '-' to exclude them. A rule ending with a '*' indicates the prefix should match, or else
 * the complete name must match.
 * A rule value of '%base' means the recommended rules.
 * For inner classes, rules are applied to the outer class. So including 'foo.Bar' includes
 * 'foo.Bar' and all its inner classes.
 * <p>
 * For example '-foo.Bar,+foo.*,%base,-*' means don't apply it to 'foo.Bar', apply it to
 * everything else in the foo package (and subpackages), apply it recommended classes, don't apply
 * it to anything else.
 */
public class ClassFilter {
  private final List<Rule> rules;

  public ClassFilter(String rulesStr, String recommended) {
    this.rules = parseRules(rulesStr, recommended);
  }

  public boolean include(String name) {
    String outerName = outerName(name);
    for (Rule rule : rules) {
      if (rule.matches(outerName)) {
        return rule.include;
      }
    }
    return false;
  }

  private static String outerName(String className) {
    int index = className.indexOf('$');
    return index == -1 ? className : className.substring(0, index);
  }

  private static List<Rule> parseRules(String rulesStr, String recommended) {
    List<Rule> rules = new ArrayList<>();

    for (String str : rulesStr.split(",")) {
      if (str.equals("%base")) {
        rules.addAll(parseRules(requireNonNull(recommended), null));
      } else {
        boolean include;
        switch (str.charAt(0)) {
          case '-':
            include = false;
            break;
          case '+':
            include = true;
            break;
          default:
            throw new IllegalArgumentException("Rule in class filter must start with + or -. Got: "
                + str + " in " + rulesStr);
        }

        boolean prefix;
        if (str.endsWith("*")) {
          prefix = true;
          str = str.substring(1, str.length() - 1);
        } else {
          prefix = false;
          str = str.substring(1);
        }
        if (str.contains("*")) {
          throw new IllegalArgumentException("Class filter rules only support * at the end. Got: "
              + str + " in " + rulesStr);

        }
        str = str.replace('.', '/');
        rules.add(new Rule(include, prefix, str));
      }
    }

    return rules;
  }

  private static class Rule {
    private final boolean include;
    private final boolean prefix;
    private final String str;

    private Rule(boolean include, boolean prefix, String str) {
      this.include = include;
      this.prefix = prefix;
      this.str = str;
    }

    private boolean matches(String className) {
      if (prefix) {
        return className.startsWith(str);
      } else {
        return className.equals(str);
      }
    }
  }
}
