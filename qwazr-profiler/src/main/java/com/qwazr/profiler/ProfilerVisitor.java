package com.qwazr.profiler;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;

public class ProfilerVisitor extends ClassVisitor {

	private String owner = null;

	private String profilerClass = ProfilerManager.class.getName().replace('.', '/');

	public ProfilerVisitor(final ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public void visit(final int version, final int access, final String name, final String signature,
			final String superName, final String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		owner = name;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
			final String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (profilerClass.equals(owner))
			return mv;
		mv = new AnalyzerAdapter(owner, access, name, desc, mv);
		mv = new ProfileMethod(access, name, desc, mv);
		return mv;
	}

	final class ProfileMethod extends AdviceAdapter {

		private final String methodKey;
		private final int methodId;
		private int localTimeVar;
		private final Label startFinally = new Label();

		public ProfileMethod(final int access, final String name, final String desc, final MethodVisitor mv) {
			super(Opcodes.ASM5, mv, access, name, desc);
			this.methodKey = owner + ":" + name + "@" + desc;
			this.methodId = ProfilerManager.register(methodKey);
		}

		@Override
		final public void visitCode() {
			super.visitCode();
			mv.visitLabel(startFinally);
			localTimeVar = newLocal(Type.LONG_TYPE);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
			mv.visitVarInsn(Opcodes.LSTORE, localTimeVar);
		}

		@Override
		final public void visitMaxs(final int maxStack, final int maxLocals) {
			final Label endFinally = new Label();
			mv.visitTryCatchBlock(startFinally,
					endFinally, endFinally, null);
			mv.visitLabel(endFinally);
			onFinally(ATHROW);
			mv.visitMaxs(maxStack + 4, maxLocals + 4);
		}

		@Override
		final protected void onMethodEnter() {

		}

		@Override
		final protected void onMethodExit(final int opcode) {
			if (opcode != ATHROW)
				onFinally(opcode);
		}

		private void onFinally(int opcode) {
			mv.visitLdcInsn(methodKey);
			mv.visitIntInsn(Opcodes.SIPUSH, methodId);
			mv.visitVarInsn(Opcodes.LLOAD, localTimeVar);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qwazr/profiler/ProfilerManager", "methodCalled",
					"(Ljava/lang/String;IJ)V", false);
			mv.visitInsn(opcode);
		}

	}
}
