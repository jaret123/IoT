package com.elyxor.xeros.ldcs;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;


public class FileWatcher {
	
	private static Logger logger = LoggerFactory.getLogger(FileWatcher.class);
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
	String currentDir = Paths.get("").toAbsolutePath().getParent().toString();
	
	WatchService watcher = null;
	Path watchDir = Paths.get(currentDir, "/input");
	Path archiveDir = Paths.get(currentDir, "/archive");
	FilenameFilter fileFilter = (FilenameFilter)new WildcardFileFilter("*Log.txt", IOCase.INSENSITIVE);
	private Boolean fileLockToken = false;
	
	
	public void watch() throws Exception {
		logger.info("Starting LDCS");
		if (!watchDir.toFile().isDirectory()) {
			new File(watchDir.toString()).mkdirs();
		}
		logger.info("watching files in " + watchDir.toString());
		new Thread(new FileScanner()).start();
		
		watcher = FileSystems.getDefault().newWatchService();
		for (;;) {

		    //wait for key to be signaled
			WatchKey key = watchDir.register(watcher, ENTRY_CREATE);
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
		        
		        if ( fileFilter.accept(watchDir.toFile(), filename.toFile().getName() )) {
		            Path child = watchDir.resolve(filename);
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
				synchronized(fileLockToken) {
					File[] files = pathFile.listFiles(fileFilter);				
					for (File f : files) {
						logger.info(String.format("%1s", f.getAbsolutePath()));
						Path child = watchDir.resolve(f.getAbsolutePath());
						new Thread(new FileAcquirer(child)).start();
					}
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
			destFilePath = archiveDir.toString();
			createTime = path.toFile().lastModified();
		}

        @Override
        public void run() {
        	String srcFileName = fileToUpload.toFile().getName();
    		if (!archiveDir.toFile().isDirectory()) {
    			new File(archiveDir.toString()).mkdirs();
    		}
        	synchronized(fileLockToken) {
	        	logger.info("waiting to lock " + srcFileName);        	
	        	try {        		
	        		Thread.sleep(10000);
	        		if ( getLock(fileToUpload.toFile())) {
		        		int responseStatus = new HttpFileUploader().postFile(fileToUpload, createTime);
		        		if (responseStatus == 200 ) {
			        		String uploadTime = sdf.format(new Date());
			        		Path newFileLocation = Paths.get(String.format("%1s/%2s.%3s", destFilePath, srcFileName, uploadTime ));
			        		Files.move(fileToUpload, newFileLocation);
			        		logger.info("archived " + newFileLocation.toString());
		        		} else {
		        			logger.warn("not moving file due to http post response");
		        		}
	        		}
	        	} catch (Exception ex) {
	        		logger.warn("Failed to get/send file", ex);
	        	}
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
						Thread.sleep(10000);
    			    }
    			    catch (Exception e) {
    			    	logger.info("waiting... " + file.getName());
    			    }
    		    }
    		    lock.release();
    		    logger.info("locked");
    		    return true;
    		} catch (Exception ex) {
    			logger.warn("no lock", ex);
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
