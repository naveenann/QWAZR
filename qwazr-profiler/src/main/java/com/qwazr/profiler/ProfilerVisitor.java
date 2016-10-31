package com.qwazr.profiler;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.util.CheckMethodAdapter;

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
		mv = new CheckMethodAdapter(mv);
		mv = new ProfileMethod(access, name, desc, mv);
		return mv;
	}

	final class ProfileMethod extends AdviceAdapter implements Opcodes {

		private final String methodKey;
		private final int methodId;

		public ProfileMethod(final int access, final String name, final String desc, final MethodVisitor mv) {
			super(ASM5, mv, access, name, desc);
			this.methodKey = owner + ":" + name + "@" + desc;
			this.methodId = ProfilerManager.register(methodKey);
		}

		@Override
		final protected void onMethodEnter() {
			visitLdcInsn(methodKey);
			visitIntInsn(SIPUSH, methodId);
			visitMethodInsn(INVOKESTATIC, "com/qwazr/profiler/ProfilerManager", "methodEnter", "(Ljava/lang/String;I)V",
					false);
		}

		@Override
		final protected void onMethodExit(final int opcode) {
			visitLdcInsn(methodKey);
			visitIntInsn(SIPUSH, methodId);
			visitMethodInsn(INVOKESTATIC, "com/qwazr/profiler/ProfilerManager", "methodExit", "(Ljava/lang/String;I)V",
					false);
		}

	}
}
