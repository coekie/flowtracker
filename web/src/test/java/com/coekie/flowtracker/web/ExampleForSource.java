package com.coekie.flowtracker.web;

/**
 * Example code for testing {@link CodeResource}
 */
public class ExampleForSource {
  @SuppressWarnings("unused")
  void go() {
    System.out.println("line 9");
    System.out.println("line 10");

    // multiple branches (labels) on the same line
    if (System.currentTimeMillis() > 0) {System.out.println("t");} else {System.out.println("f");}

    // one statement on multiple lines
    System.out.println("line 16"
        + System.currentTimeMillis() + "line 17");

    System.out.println("end");
  }
}
