package com.qwazr.profiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;

public class ProfilerVisitor extends ClassVisitor {

	private String owner = null;
	private boolean isInterface = false;
	private boolean isAbstract = false;

	private String profilerClass = ProfilerManager.class.getName().replace('.', '/');

	public ProfilerVisitor(final ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public void visit(final int version, final int access, final String name, final String signature,
			final String superName, final String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		owner = name;
		isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
			final String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (profilerClass.equals(owner))
			return mv;
		if (isInterface || isAbstract || mv == null)
			return mv;
		mv = new AnalyzerAdapter(owner, access, name, desc, mv);
		mv = new ProfileMethod(access, name, desc, mv);
		return mv;
	}

	final class ProfileMethod extends AdviceAdapter {

		private final String methodKey;
		private final int methodId;
		private int localTimeVar;

		public ProfileMethod(final int access, final String name, final String desc, final MethodVisitor mv) {
			super(Opcodes.ASM5, mv, access, name, desc);
			this.methodKey = owner + ":" + name + "@" + desc;
			this.methodId = ProfilerManager.register(methodKey);
		}

		@Override
		final protected void onMethodEnter() {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
			localTimeVar = newLocal(Type.LONG_TYPE);
			super.visitVarInsn(Opcodes.LSTORE, localTimeVar);
		}

		@Override
		final protected void onMethodExit(final int opcode) {
			super.visitLdcInsn(methodKey);
			super.visitIntInsn(SIPUSH, methodId);
			super.visitVarInsn(Opcodes.LLOAD, localTimeVar);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qwazr/profiler/ProfilerManager", "methodCalled",
					"(Ljava/lang/String;IJ)V", false);
		}
	}
}
