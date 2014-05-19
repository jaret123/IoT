package com.elyxor.xeros.ldcs;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.FilenameFilter;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileWatcher {
	
	final static Logger logger = LoggerFactory.getLogger(FileWatcher.class);
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
	WatchService watcher = null;
	Path watchDir = Paths.get(AppConfiguration.getLocalPath());
	FilenameFilter fileFilter = (FilenameFilter)new WildcardFileFilter(AppConfiguration.getFilePattern(), IOCase.INSENSITIVE);
	
	
	
	public void watch() throws Exception {
		logger.info("Starting LDCS");
		Path dir = Paths.get(AppConfiguration.getLocalPath());
		
		logger.info("watching files in " + dir.toString());
		new Thread(new FileScanner()).start();
		
		watcher = FileSystems.getDefault().newWatchService();
		for (;;) {

		    //wait for key to be signaled
			WatchKey key = dir.register(watcher, ENTRY_CREATE);
		    try {		    	
		        key = watcher.take();
		    } catch (InterruptedException x) {
		        return;
		    }

		    for (WatchEvent<?> event: key.pollEvents()) {
		        WatchEvent.Kind<?> kind = event.kind();
		        if (kind == OVERFLOW) {
		            continue;
		        }
		        
		        @SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>)event;
		        Path filename = ev.context();
		        
		        if ( fileFilter.accept(dir.toFile(), filename.toFile().getName() )) {
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
	
	public class FileScanner implements Runnable {
		
		public FileScanner() {}
		
		public void scanFiles() {		
			if (watchDir.toFile().isDirectory()) {
				logger.info("scanning for files in " + watchDir.toString());
				File pathFile = watchDir.toFile();
				File[] files = pathFile.listFiles(fileFilter);				
				for (File f : files) {
					logger.debug(String.format("%1s", f.getAbsolutePath()));
					Path child = watchDir.resolve(f.getAbsolutePath());
					new Thread(new FileAcquirer(child)).start();
				}
			}
		}

		@Override
		public void run() {
			try {
				for (;;) {
					scanFiles();
					Thread.sleep(30000);
				}				
			} catch(Exception ex) {
				logger.warn("Exception caught scanning files", ex);
			}						
		}
	}
	
	public class FileAcquirer implements Runnable {
		
		Path fileToUpload = null;
		String destFilePath = null;
		long createTime;
		
		public FileAcquirer(Path path) {
			fileToUpload = path;
			destFilePath = Paths.get(AppConfiguration.getArchivePath()).toAbsolutePath().toFile().getAbsolutePath();
			createTime = path.toFile().lastModified();
		}

        @Override
        public void run() {
        	String srcFileName = fileToUpload.toFile().getName();
        	logger.info("waiting to lock " + srcFileName);        	
        	try {
        		Thread.sleep(AppConfiguration.getFileLockWait());
        		int responseStatus = new HttpFileUploader().postFile(fileToUpload, createTime);
        		if (responseStatus == 200 ) {
	        		String uploadTime = sdf.format(new Date());
	        		Path newFileLocation = Paths.get(String.format("%1s/%2s.%3s", destFilePath, srcFileName, uploadTime ));
	        		Files.move(fileToUpload, newFileLocation);
	        		logger.info("archived " + newFileLocation.toString());
        		} else {
        			logger.warn("not moving file due to http post response");
        		}
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
    			    	logger.debug("waiting for release of " + file.getName());
    			    	channel.close();
						Thread.sleep(AppConfiguration.getFileLockWait());
    			    }
    			    catch (Exception e) {
    			    	logger.debug("waiting... " + file.getName());
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
