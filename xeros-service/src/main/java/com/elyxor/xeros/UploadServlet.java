package com.elyxor.xeros;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@WebServlet(asyncSupported = false, 
            name = "UploadServlet", 
            urlPatterns = {"/upload/*"},
            loadOnStartup = 1 )
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = -7167306752229009722L;
	
	private static final Logger logger = LoggerFactory.getLogger(UploadServlet.class);	
	private FileItemFactory factory;
	private SimpleDateFormat sdf = null;
	private static final int BUFSIZE = 4096;
	private static final int MAX_FILE_SIZE = 30000;
	@Autowired private RSService rsService;

	public String getTempFolder() {
		return System.getProperty("java.io.tmpdir");
	}
	
	@Override
	public void init() throws ServletException {
	    super.init();
	    sdf = new SimpleDateFormat("yyyyMMdd.HHMMss");
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		rsService = (RSService)wac.getBean("RSServiceImpl");
	    FileCleaningTracker fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(getServletContext());
	    factory = new DiskFileItemFactory(MAX_FILE_SIZE, new File(getTempFolder()));
	    ((DiskFileItemFactory)factory).setFileCleaningTracker(fileCleaningTracker);
	}
	

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
		ServletFileUpload upload = new ServletFileUpload(factory);
		PrintWriter out = response.getWriter();
        StringBuffer obuf = new StringBuffer();
        
		try {		    
			@SuppressWarnings("unchecked")
            List<FileItem> uploadedItems = upload.parseRequest(request);
			for( FileItem fileItem : uploadedItems ) {			 
				String fullFileName = fileItem.getName();
				String slashType = (fullFileName.lastIndexOf("\\") > 0) ? "\\" : "/";
				String fileName = fullFileName.substring(fullFileName.lastIndexOf(slashType) + 1, fullFileName.length());
				fileName = String.format( "%1s_%2s", sdf.format(new Date()), fileName );
				String uploadedFileName = getTempFolder() + "/" + fileName;
				logger.info("Resolving " + uploadedFileName );
				File uploadedFile = new File( uploadedFileName );
				logger.info("Writing to " + uploadedFile.getAbsolutePath() );
				fileItem.write(uploadedFile);
				File f = uploadedFile.getAbsoluteFile();
				rsService.parseCollectionFile(f);
				obuf.append( "DONE" );
			}
		} 
		catch (Exception e) {
		    logger.error("Failed to upload file", e); 
            response.setStatus(500);
		}
		finally {
		    out.println(obuf.toString());
		    out.flush();
		    out.close();
		}
	}
}
