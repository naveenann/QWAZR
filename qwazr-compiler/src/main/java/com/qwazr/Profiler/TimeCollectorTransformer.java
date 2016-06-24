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

import com.qwazr.utils.StringUtils;
import com.qwazr.utils.WildcardMatcher;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class TimeCollectorTransformer implements ClassFileTransformer {

	private Logger LOGGER = LoggerFactory.getLogger(TimeCollectorTransformer.class);

	private final WildcardMatcher[] wildcardMatchers;

	private final ClassPool classPool;
	private final CtClass timeCollectorClass;

	public TimeCollectorTransformer(WildcardMatcher... wildcardMatchers) throws NotFoundException {
		this.wildcardMatchers = wildcardMatchers;
		this.classPool = ClassPool.getDefault();
		this.timeCollectorClass = classPool.get(ProfilerAgent.TimeCollector.class.getName());
	}

	@Override
	public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {
		try {
			for (WildcardMatcher matcher : wildcardMatchers)
				if (matcher.match(className))
					return profile(className);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new IllegalClassFormatException(e.getMessage());
		}
		return classfileBuffer;
	}

	private byte[] profile(final String className) throws CannotCompileException, IOException, NotFoundException {
		final CtClass cc = classPool.get(StringUtils.replaceChars(className, '/', '.'));
		for (CtMethod m : cc.getMethods()) {
			final String classMethod = cc.getName() + ":" + m.getLongName();
			m.insertBefore("com.qwazr.profiler.ProfilerAgent.TimeCollector _qwazr_timeColl = com.qwazr.profiler.ProfilerAgent.enter(\"" + classMethod + "\");");
			m.insertAfter("_qwazr_timeColl.exit();");
		}
		final byte[] byteCode = cc.toBytecode();
		cc.detach();
		return byteCode;
	}

}
