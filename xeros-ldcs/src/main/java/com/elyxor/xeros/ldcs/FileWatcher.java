package com.elyxor.xeros.ldcs;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileWatcher {
	
	private static Logger logger = LoggerFactory.getLogger(FileWatcher.class);
	
	WatchService watcher = null;
	
	public void watch() throws Exception {
		logger.info("Starting LDCS");
		Path dir = Paths.get(AppConfiguration.getLocalPath());
		watcher = FileSystems.getDefault().newWatchService();
		for (;;) {

		    //wait for key to be signaled
			WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
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

	            Path child = dir.resolve(filename);
	            FileAcquirer fa = new FileAcquirer(child);
	            fa.run();		            
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
		
		public FileAcquirer(Path path) {
			this.fileToUpload = path;
		}

        @Override
        public void run() {
        	logger.info("waiting to lock " + fileToUpload.toFile().getName());
        	boolean hasLock = getLock(fileToUpload.toFile());
        	try {
        		logger.info("uploading " + fileToUpload.toFile().getName());
        		new HttpFileUploader().postFile(fileToUpload);        		
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
    		    	channel = new RandomAccessFile(file, "rw").getChannel();
    			    try {	
    			    	lock = channel.tryLock();
    			    	hasExclusive = true;
    			    }
    			    catch (OverlappingFileLockException e) {
    			    	logger.info("waiting for release of " + file.getName());
    			    	channel.close();
    			    	Thread.currentThread().sleep(2000);
    			    }
    		    }
    		    lock.release();
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
