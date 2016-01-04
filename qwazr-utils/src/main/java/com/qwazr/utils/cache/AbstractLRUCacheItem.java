/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils.cache;

import com.qwazr.utils.LockUtils;

public abstract class AbstractLRUCacheItem<K> implements Comparable<K> {

	private final LockUtils.ReadWriteLock rwl = new LockUtils.ReadWriteLock();

	private boolean populated = false;

	protected abstract void populate() throws Exception;

	protected abstract boolean expired();

	final public void join() throws Exception {
		rwl.r.lock();
		try {
			if (populated)
				return;
		} finally {
			rwl.r.unlock();
		}
		rwl.w.lock();
		try {
			if (populated)
				return;
			populate();
			populated = true;
		} finally {
			rwl.w.unlock();
		}
	}
}
