package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;

/** Represents an instruction that stores a (possibly tracked) value in an object. */
abstract class Store {
  final FlowFrame frame;

  Store(FlowFrame frame) {
    this.frame = frame;
  }

  abstract void insertTrackStatements(FlowMethodAdapter methodNode);

  FlowValue getStackFromTop(int indexFromTop) {
    FlowValue value = frame.getStack(frame.getStackSize() - indexFromTop - 1);
    // this is a bit fragile/awkward. we call getStackFromTop in the constructor of Stores, and then
    // call initCreationFrame from here so that the FlowValue has a chance to find its corresponding
    // frame after analysis is over but before we start adding more instructions (which would make
    // it harder to find the frame back). note that we're just using the frame as the store here to
    // get a reference to the analyzer; the frame that the value was created at is a different
    // frame.
    value.initCreationFrame(frame.analyzer);
    return value;
  }
}
