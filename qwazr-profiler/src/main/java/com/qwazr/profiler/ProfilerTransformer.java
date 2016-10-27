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
package com.qwazr.profiler;

import com.qwazr.utils.WildcardMatcher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ProfilerTransformer implements ClassFileTransformer {

	private final WildcardMatcher[] wildcardMatchers;

	public ProfilerTransformer(WildcardMatcher... wildcardMatchers) {
		this.wildcardMatchers = wildcardMatchers;
	}

	@Override
	public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain, final byte[] classFileBuffer) {
		for (WildcardMatcher matcher : wildcardMatchers)
			if (matcher.match(className))
				return profile(classFileBuffer);
		return classFileBuffer;
	}

	private byte[] profile(final byte[] classFileBuffer) {
		final ClassReader cr = new ClassReader(classFileBuffer);
		final ClassWriter cw = new ClassWriter(cr, 0);
		final ClassVisitor cv = new ProfilerVisitor(cw);
		cr.accept(cv, 0);
		return cw.toByteArray();
	}

}
