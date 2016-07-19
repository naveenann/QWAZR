/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.compiler;

import com.qwazr.utils.DirectoryWatcher;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LockUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.server.ServerException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class JavaCompiler implements Closeable {

	private final static Logger LOGGER = LoggerFactory.getLogger(JavaCompiler.class);

	private final int javaSourcePrefixSize;
	private final File javaSourceDirectory;
	private final File javaClassesDirectory;

	private final String classPath;

	private final LockUtils.ReadWriteLock compilerLock;

	private final DirectoryWatcher directorWatcher;

	private final ConcurrentHashMap<URI, Date> compilableMap;
	private final ConcurrentHashMap<URI, CompilerStatus.DiagnosticStatus> diagnosticMap;

	private JavaCompiler(final ExecutorService executorService, final File javaSourceDirectory,
			final File javaClassesDirectory, final String classPath) throws IOException {
		this.classPath = classPath;
		this.compilableMap = new ConcurrentHashMap<>();
		this.diagnosticMap = new ConcurrentHashMap<>();
		this.javaSourceDirectory = javaSourceDirectory;
		String javaSourcePrefix = javaSourceDirectory.getAbsolutePath();
		javaSourcePrefixSize =
				javaSourcePrefix.endsWith("/") ? javaSourcePrefix.length() : javaSourcePrefix.length() + 1;
		this.javaClassesDirectory = javaClassesDirectory;
		if (this.javaClassesDirectory != null && !this.javaClassesDirectory.exists())
			this.javaClassesDirectory.mkdir();
		compilerLock = new LockUtils.ReadWriteLock();
		compileDirectory(javaSourceDirectory);
		directorWatcher =
				DirectoryWatcher.register(javaSourceDirectory.toPath(), path -> compileDirectory(path.toFile()));
		executorService.execute(directorWatcher);
	}

	@Override
	public void close() {
		IOUtils.close(directorWatcher);
	}

	static JavaCompiler newInstance(final ExecutorService executorService, final File javaSourceDirectory,
			final File javaClassesDirectory, final File... classPathDirectories)
			throws IOException, URISyntaxException {
		Objects.requireNonNull(javaSourceDirectory, "No source directory given (null)");
		Objects.requireNonNull(javaClassesDirectory, "No class directory given (null)");
		final List<URL> urlList = new ArrayList<>();
		urlList.add(javaClassesDirectory.toURI().toURL());
		final String classPath = buildClassPath(classPathDirectories, urlList);
		return new JavaCompiler(executorService, javaSourceDirectory, javaClassesDirectory, classPath);
	}

	private final static String buildClassPath(final File[] classPathArray, final Collection<URL> urlCollection)
			throws MalformedURLException, URISyntaxException {
		final List<String> classPathes = new ArrayList<>();

		URLClassLoader classLoader = (URLClassLoader) URLClassLoader.getSystemClassLoader();
		if (classLoader != null && classLoader.getURLs() != null) {
			for (URL url : classLoader.getURLs()) {
				String path = new File(url.toURI()).getAbsolutePath();
				classPathes.add(path);
				urlCollection.add(url);
			}
		}

		if (classPathArray != null) {
			for (File classPathFile : classPathArray) {
				if (classPathFile.isDirectory()) {
					for (File f : classPathFile.listFiles((FileFilter) FileFileFilter.FILE)) {
						classPathes.add(f.getAbsolutePath());
						urlCollection.add(f.toURI().toURL());
					}
				} else if (classPathFile.isFile()) {
					classPathes.add(classPathFile.getAbsolutePath());
					urlCollection.add(classPathFile.toURI().toURL());
				}
			}
		}
		if (classPathes.isEmpty())
			return null;
		return StringUtils.join(classPathes, File.pathSeparator);
	}

	private void compile(final javax.tools.JavaCompiler compiler, final Collection<File> javaFiles) throws IOException {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			final Iterable<? extends JavaFileObject> sourceFileObjects =
					fileManager.getJavaFileObjectsFromFiles(javaFiles);
			final List<String> options = new ArrayList<>();
			if (classPath != null) {
				options.add("-classpath");
				options.add(classPath);
			}
			options.add("-d");
			options.add(javaClassesDirectory.getAbsolutePath());
			options.add("-sourcepath");
			options.add(javaSourceDirectory.getAbsolutePath());
			javax.tools.JavaCompiler.CompilationTask task =
					compiler.getTask(null, fileManager, diagnostics, options, null, sourceFileObjects);
			final Date date = new Date();
			task.call();
			for (File file : javaFiles) {
				final URI fileUri = file.toURI();
				compilableMap.put(fileUri, date);
				diagnosticMap.remove(fileUri);
			}
			for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
				if (diagnostic == null)
					continue;
				final JavaFileObject source = diagnostic.getSource();
				if (source == null)
					continue;
				final URI uri = source.toUri();
				if (uri != null)
					diagnosticMap.put(uri, new CompilerStatus.DiagnosticStatus(date, diagnostic));
				if (LOGGER.isWarnEnabled())
					LOGGER.warn(
							String.format("Error on line %d in %s%n%s", diagnostic.getLineNumber(), source.getName(),
									diagnostic.getMessage(null)));
			}
		}
	}

	private Collection<File> filterUptodate(final File parentDir, final File[] javaSourceFiles) {
		if (javaSourceFiles == null)
			return null;
		final Collection<File> finalJavaFiles = new ArrayList<>();
		if (javaSourceFiles.length == 0)
			return finalJavaFiles;
		final File parentClassDir =
				new File(javaClassesDirectory, parentDir.getAbsolutePath().substring(javaSourcePrefixSize));
		for (File javaSourceFile : javaSourceFiles) {
			final File classFile =
					new File(parentClassDir, FilenameUtils.removeExtension(javaSourceFile.getName()) + ".class");
			if (classFile.exists() && classFile.lastModified() > javaSourceFile.lastModified()) {
				compilableMap.put(javaSourceFile.toURI(), new Date(classFile.lastModified()));
				continue;
			}
			finalJavaFiles.add(javaSourceFile);
		}
		return finalJavaFiles;
	}

	private void compileDirectory(javax.tools.JavaCompiler compiler, File sourceDirectory) {
		final Collection<File> javaFiles = filterUptodate(sourceDirectory, sourceDirectory.listFiles(javaFileFilter));
		if (javaFiles != null && javaFiles.size() > 0) {
			if (LOGGER.isInfoEnabled())
				LOGGER.info("Compile " + javaFiles.size() + " JAVA file(s) at " + sourceDirectory);
			try {
				compile(compiler, javaFiles);
			} catch (IOException e) {
				throw new ServerException(e);
			}
		}
		for (File dir : sourceDirectory.listFiles((FileFilter) DirectoryFileFilter.INSTANCE))
			compileDirectory(compiler, dir);
	}

	private void compileDirectory(File sourceDirectory) {
		if (sourceDirectory == null)
			return;
		if (!sourceDirectory.isDirectory())
			return;
		compilerLock.writeEx(() -> {
			javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			Objects.requireNonNull(compiler, "No compiler is available. This feature requires a JDK (not a JRE).");
			compileDirectory(compiler, sourceDirectory);
		});
	}

	private final JavaFileFilter javaFileFilter = new JavaFileFilter();

	private class JavaFileFilter implements FileFilter {

		@Override
		final public boolean accept(File file) {
			if (!file.isFile())
				return false;
			return file.getName().endsWith(".java");
		}
	}

	CompilerStatus getStatus() {
		return new CompilerStatus(compilableMap, diagnosticMap);
	}
}
