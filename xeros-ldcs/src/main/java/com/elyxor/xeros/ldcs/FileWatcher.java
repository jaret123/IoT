package com.elyxor.xeros.ldcs;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.StandardCopyOption.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;





import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileWatcher {
	
	private static Logger logger = LoggerFactory.getLogger(FileWatcher.class);
	
	WatchService watcher = null;
	
	public void watch() throws Exception {
		logger.info("Starting LDCS");
		Path dir = Paths.get(AppConfiguration.getLocalPath());
		FilenameFilter ff = (FilenameFilter)new WildcardFileFilter(AppConfiguration.getFilePattern(), IOCase.INSENSITIVE);
		
		if (dir.toFile().isDirectory()) {
			File pathFile = dir.toFile();			
			File[] files = pathFile.listFiles(ff);
			for (File f : files) {
				logger.debug(String.format("%1s", f.getAbsolutePath()));
				Path child = dir.resolve(f.getAbsolutePath());
	            FileAcquirer fa = new FileAcquirer(child);
	            fa.run();	
			}
		}
		
		
		watcher = FileSystems.getDefault().newWatchService();
		for (;;) {

		    //wait for key to be signaled
			WatchKey key = dir.register(watcher, ENTRY_CREATE);
		    try {
		    	logger.info("watching files in " + dir.toString());
		        key = watcher.take();
		    } catch (InterruptedException x) {
		        return;
		    }

		    for (WatchEvent<?> event: key.pollEvents()) {
		        WatchEvent.Kind<?> kind = event.kind();
		        if (kind == OVERFLOW) {
		            continue;
		        }
		        logger.info("caught " + kind.toString());

		        WatchEvent<Path> ev = (WatchEvent<Path>)event;
		        Path filename = ev.context();
		        
		        if ( ff.accept(dir.toFile(), filename.toFile().getName() )) {
		            Path child = dir.resolve(filename);
		            FileAcquirer fa = new FileAcquirer(child);
		            fa.run();		            
		        }
		    }
		    boolean valid = key.reset();
		    if (!valid) {
		        break;
		    }
		}
		logger.info("Ending LDCS");
	}
	
	public class FileAcquirer implements Runnable {
		
		Path fileToUpload = null;
		String destFilePath = null;
		
		public FileAcquirer(Path path) {
			fileToUpload = path;
			destFilePath = Paths.get(AppConfiguration.getArchivePath()).toAbsolutePath().toFile().getAbsolutePath();
		}

        @Override
        public void run() {
        	String srcFileName = fileToUpload.toFile().getName();
        	logger.info("waiting to lock " + srcFileName);
        	boolean hasLock = getLock(fileToUpload.toFile());
        	try {
        		new HttpFileUploader().postFile(fileToUpload);
        		// check for good status
        		Path newFileLocation = Paths.get(String.format("%1s/%2s.%3s", destFilePath, srcFileName, System.currentTimeMillis() ));
        		Files.move(fileToUpload, newFileLocation);
        		logger.debug("archived");
        	} catch (Exception ex) {
        		logger.warn("Failed to get/send file", ex);
        	}
        	
        }
        
    	public boolean getLock(File file) {
    		FileChannel channel = null;
    		FileLock lock = null;	
    	    boolean hasExclusive = false;
    	    
    		try {
    		    while (!hasExclusive) {    		    	
    			    try {
    			    	channel = new RandomAccessFile(file, "rw").getChannel();
    			    	lock = channel.tryLock();
    			    	hasExclusive = true;
    			    }    			    
    			    catch (OverlappingFileLockException e) {
    			    	logger.info("waiting for release of " + file.getName());
    			    	channel.close();
    			    	Thread.currentThread().sleep(2000);
    			    }
    			    catch (Exception e) {
    			    	logger.info("waiting... " + file.getName());
    			    }
    		    }
    		    lock.release();
    		    logger.info("locked");
    		} catch (Exception ex) {
    			logger.info("no lock", ex);
    		}
    		finally {
    			if ( channel!=null ) {
    				try {
    					channel.close();
    				} catch (Exception ex) {}
    			}
    			
    		}
    		return false;
    	}

    }


	

}
