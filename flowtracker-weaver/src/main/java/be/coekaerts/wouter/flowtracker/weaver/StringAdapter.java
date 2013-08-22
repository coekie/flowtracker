package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import be.coekaerts.wouter.flowtracker.hook.StringHook;

/**
 * Instrumentation for {@link String}.
 * 
 * Adds a hack to make it possible for {@link StringHook} to get the tracker based on the
 * String.value and String.offset.
 */
public class StringAdapter extends ClassAdapter {
	private static final Type CONTENT_EXTRACTOR_TYPE =
		Type.getType("Lbe/coekaerts/wouter/flowtracker/hook/StringHook$StringContentExtractor;");

	/** true if we've seen the field named "offset". JDK6 has this, JDK7 does not. */
	private boolean hasOffsetField;

	public StringAdapter(ClassVisitor cv) {
		super(cv);
	}

	@Override public FieldVisitor visitField(int access, String name, String desc, String signature,
			Object value) {
		if (name.equals("offset")) hasOffsetField = true;
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if ("contentEquals".equals(name) && "(Ljava/lang/CharSequence;)Z".equals(desc)) {
			return new StringContainsAdapter(mv, access, name, desc);
		} else {
			return mv;
		}
	}
	
	/**
	 * Adapter for {@link String#contentEquals(CharSequence)}.
	 */
	private class StringContainsAdapter extends GeneratorAdapter {

		public StringContainsAdapter(MethodVisitor mv, int access, String name, String desc) {
			super(mv, access, name, desc);
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			// Insert code that exposes char[] and offset:
			// 	if (s instance of StringContentExtractor) {
			// 		((StringContentExtractor)s).setContent(value, offset);
			//		return false;
			// 	}
			// 	notExtractor: ... normal implementation
			
			loadArg(0);
			instanceOf(CONTENT_EXTRACTOR_TYPE);
			Label notExtractor = newLabel();
			this.ifZCmp(EQ, notExtractor);
			loadArg(0);
			checkCast(CONTENT_EXTRACTOR_TYPE);
			loadThis();
			getField(Types.STRING, "value", Types.CHAR_ARRAY);

			if (hasOffsetField) {
				loadThis();
				getField(Types.STRING, "offset", Type.INT_TYPE);
			} else {
				push(0);
			}

			invokeVirtual(CONTENT_EXTRACTOR_TYPE, Method.getMethod("void setContent(char[],int)"));
			visitInsn(Opcodes.ICONST_0);
			returnValue();
			mark(notExtractor);
		}
		
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			// the part we insert in visitCode() has maxStack 3,
			// and doesn't leave anything behind on the stack
			super.visitMaxs(Math.max(3, maxStack), maxLocals);
		}
	}
}
