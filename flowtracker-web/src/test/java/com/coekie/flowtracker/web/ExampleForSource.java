package com.coekie.flowtracker.web;

/**
 * Example code for testing {@link SourceResource}
 */
public class ExampleForSource {
  @SuppressWarnings("unused")
  void go() {
    System.out.println("line 9");
    System.out.println("line 10");
    // deliberately multiple branches (labels) on the same line
    if (System.currentTimeMillis() > 0) {System.out.println("t");} else {System.out.println("f");}
    System.out.println("end");
  }
}
