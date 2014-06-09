package com.elyxor.xeros.ldcs.util;

import java.io.IOException;

public interface LogWriterInterface  {
	public String getPath();
	public String getFilename();
	public void write(String txt) throws IOException;
}
