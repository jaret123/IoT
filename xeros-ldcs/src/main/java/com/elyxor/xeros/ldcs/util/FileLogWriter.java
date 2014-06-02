package com.elyxor.xeros.ldcs.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elyxor.xeros.ldcs.dai.PortManager;

public class FileLogWriter implements LogWriterInterface {
	final static Logger logger = LoggerFactory.getLogger(FileLogWriter.class);

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
			logger.info("Wrote log to file " + _logFile.getName());
		}
	}
}
