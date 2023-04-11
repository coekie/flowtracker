package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

/** The storing of a value in an array, e.g. a char in a char[]. */
// on the stack: char[] target, int index, char toStore 
class ArrayStore extends Store {
  private final InsnNode storeInsn;
  /** Method in ArrayHook to call as a replacement for the array store operation */
  private final String hookMethod;

  ArrayStore(InsnNode storeInsn, Frame<FlowValue> frame, String hookMethod) {
    super(frame);
    this.storeInsn = storeInsn;
    this.hookMethod = hookMethod;
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    FlowValue stored = getStored();

    InsnList toInsert = new InsnList();

    methodNode.addComment(toInsert, "begin ArrayStore.insertTrackStatements: "
        + "ArrayHook.set*(array, arrayIndex, value [already on stack], source, sourceIndex)");

    // note: we do this even for UntrackableValues
    stored.ensureTracked();
    stored.loadSourceTracker(toInsert);
    stored.loadSourceIndex(toInsert);

    methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);

    Method hook = Method.getMethod(hookMethod);

    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "be/coekaerts/wouter/flowtracker/hook/ArrayHook", hook.getName(), hook.getDescriptor(),
        false));

    methodNode.addComment(toInsert, "end ArrayStore.insertTrackStatements");

    methodNode.instructions.insert(storeInsn, toInsert);
    methodNode.instructions.remove(storeInsn); // our hook takes care of the storing
  }

  /** The value being stored */
  private FlowValue getStored() {
    return getStackFromTop(0);
  }

  static ArrayStore createCharArrayStore(InsnNode storeInsn, Frame<FlowValue> frame) {
    return new ArrayStore(storeInsn, frame,
        "void setChar(char[],int,char,be.coekaerts.wouter.flowtracker.tracker.Tracker,int)");
  }

  static ArrayStore createByteArrayStore(InsnNode storeInsn, Frame<FlowValue> frame) {
    return new ArrayStore(storeInsn, frame,
        "void setByte(byte[],int,byte,be.coekaerts.wouter.flowtracker.tracker.Tracker,int)");
  }
}
