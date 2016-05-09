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
package com.qwazr.utils.file;

import com.qwazr.utils.LockUtils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

abstract class TrackedAbstract<T> implements TrackedInterface {

	private final LockUtils.ReadWriteLock rwl;
	private final Set<FileChangeConsumer> consumerSet;
	protected final File trackedFile;

	public TrackedAbstract(final File trackedFile) {
		this.rwl = new LockUtils.ReadWriteLock();
		this.consumerSet = new LinkedHashSet<>();
		this.trackedFile = trackedFile;
	}

	final public void register(final FileChangeConsumer consumer) {
		synchronized (consumerSet) {
			consumerSet.add(consumer);
		}
	}

	final public void unregister(final FileChangeConsumer consumer) {
		synchronized (consumerSet) {
			consumerSet.remove(consumer);
		}
	}

	protected void notify(final ChangeReason reason, final File file) {
		synchronized (consumerSet) {
			consumerSet.forEach((consumer) -> consumer.accept(reason, file));
		}
	}

	protected abstract void apply(T status);

	protected abstract T getChanges();

	@Override
	final public void check() {

		if (rwl.read(() -> getChanges()) == null)
			return;

		rwl.write(() -> {
			final T changes = getChanges();
			if (changes != null)
				apply(changes);
		});
	}

	final public File getFile() {
		return trackedFile;
	}

}
