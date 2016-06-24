/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.Profiler;

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.WildcardMatcher;
import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collection;

public class TimeCollectorTransformer implements ClassFileTransformer {

	private final Collection<WildcardMatcher> wildcardMatchers;

	public TimeCollectorTransformer(Collection<WildcardMatcher> wildcardMatchers) {
		this.wildcardMatchers = wildcardMatchers;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		try {
			for (WildcardMatcher matcher : wildcardMatchers)
				if (matcher.match(className))
					return profile(className);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalClassFormatException(e.getMessage());
		}
		return classfileBuffer;
	}

	private byte[] profile(String className) throws CannotCompileException, IOException, NotFoundException {
		final ClassPool cp = ClassPool.getDefault();
		final CtClass cc = cp.get(StringUtils.replaceChars(className, '/', '.'));
		for (CtMethod m : cc.getMethods()) {
			m.addLocalVariable("__elapsedTime", CtClass.longType);
			m.insertBefore("__elapsedTime = System.currentTimeMillis();");
			m.insertAfter("{__elapsedTime = System.currentTimeMillis() - __elapsedTime;"
					+ "System.out.println(\"Method Executed in ms: \" + __elapsedTime);}");
		}
		byte[] byteCode = cc.toBytecode();
		cc.detach();
		return byteCode;
	}

}
