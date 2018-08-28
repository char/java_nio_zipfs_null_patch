# nio_zipfs_null_patch

Java's NIO ZipPath rejects paths containing NUL bytes.
This program allows you to generate an alternative `zipfs.jar` that allows NUL bytes to appear in paths.

In order to use the generated JAR, prepend it to your bootclasspath using the JVM argument `-Xbootclasspath/p:<path/to/zipfs.patched.jar>`
