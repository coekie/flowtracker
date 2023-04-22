package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/** A char received from a {@link String#charAt(int)} or {@link CharSequence#charAt(int)} call. */
class CharAtValue extends TrackableValue {
  /** The charAt() call */
  private final MethodInsnNode mInsn;

  /** Local variable storing the target String or CharSequence */
  private TrackLocal targetStringLocal;
  /** Local variable storing the index */
  private TrackLocal indexLocal;

  CharAtValue(FlowMethodAdapter flowMethodAdapter, MethodInsnNode mInsn) {
    super(flowMethodAdapter, Type.CHAR_TYPE, mInsn);
    this.mInsn = mInsn;
    // Note: when it can return something else than char, we get type with Type.getReturnType(mInsn.desc)
  }

  @Override void insertTrackStatements() {
    // on the stack before the charAt call: String target, int index

    targetStringLocal = flowMethodAdapter.newLocalForObject(Type.getObjectType(mInsn.owner),
        "CharAtValue targetString");
    indexLocal = flowMethodAdapter.newLocalForIndex("CharAtValue index");

    InsnList toInsert = new InsnList();

    // store target and index
    toInsert.add(indexLocal.store());
    toInsert.add(targetStringLocal.store());

    // put them back on the stack for the actual operation
    toInsert.add(targetStringLocal.load());
    toInsert.add(indexLocal.load());

    flowMethodAdapter.instructions.insertBefore(mInsn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert,
        "CharAtValue.loadSourceTracker: StringHook.getStringTracker(targetString)");
    toInsert.add(targetStringLocal.load());
    if (mInsn.owner.equals("java/lang/String")) {
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/hook/StringHook",
              "getStringTracker",
              "(Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
              false));
    } else if (mInsn.owner.equals("java/lang/CharSequence")) {
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/hook/StringHook",
              "getCharSequenceTracker",
              "(Ljava/lang/CharSequence;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
              false));
    }
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "CharAtValue.loadSourceIndex");
    toInsert.add(indexLocal.load());
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    doLoadSourcePointFromTrackerAndIndex(toInsert);
  }
}
