package org.slf4j;
/** Test-only stand-in; the real slf4j is on the classpath in the game. */
public interface Logger { void warn(String format, Object... args); }
