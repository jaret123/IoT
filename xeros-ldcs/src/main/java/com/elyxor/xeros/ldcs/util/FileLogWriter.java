package com.elyxor.xeros.ldcs.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLogWriter implements LogWriterInterface {
	final static Logger logger = LoggerFactory.getLogger(FileLogWriter.class);

	private File _logFile = null;

	public FileLogWriter(Path path, String filename) {
		_logFile = new File(path.toString(), filename);
	}
	public String getFilename() {
		return _logFile.getName();
	}
	public Path getPath() {
		return _logFile.toPath();
	}
	@Override
	public void write(String txt) throws IOException {
		FileWriter fileWriter = null;
		BufferedWriter out = null;

        if (!_logFile.isDirectory()) {
            _logFile.mkdirs();
        }
        try {
			fileWriter = new FileWriter(_logFile, true);
			out = new BufferedWriter(fileWriter);	
			out.write(txt);
		}
		finally {
			if (null != out) { try { out.close(); } catch (Exception e2) {} }
			if (null != fileWriter) { try { fileWriter.close(); } catch (Exception e2) {} }
			logger.info("Wrote log to file " + _logFile.getName());
		}
	}
}
