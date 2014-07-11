package com.elyxor.xeros.ldcs.util;

import java.io.IOException;
import java.nio.file.Path;

public interface LogWriterInterface  {
	public Path getPath();
	public String getFilename();
	public void write(String txt) throws IOException;
}
