package com.elyxor.xeros.ldcs.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class FileLogWriter implements LogWriterInterface {

	private File _logFile = null;

	public FileLogWriter(Path path, String filename) {
		
		_logFile = new File(path.toString(), filename);
		
	}

	
	@Override
	public void write(String txt) throws IOException {

		FileWriter fileWriter = null;
		BufferedWriter out = null;
		try {
			fileWriter = new FileWriter(_logFile, true);
			out = new BufferedWriter(fileWriter);	
			out.write(txt);
		}
		finally {
			if (null != out) { try { out.close(); } catch (Exception e2) {} }
			if (null != fileWriter) { try { fileWriter.close(); } catch (Exception e2) {} }
		}
	}
}
