/**
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.dxrnd.zkui.controller;

import com.dxrnd.zkui.dao.Dao;
import com.dxrnd.zkui.utils.ServletUtil;
import com.dxrnd.zkui.utils.ZooKeeperUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10, // 10 MB
        maxFileSize = 1024 * 1024 * 50, // 50 MB
        maxRequestSize = 1024 * 1024 * 100)      // 100 MB
@RestController
@RequestMapping("/import")
public class Import {
    private final static Logger logger = LoggerFactory.getLogger(Import.class);

    @Value("${server.servlet.context-path}")
    private String contentPath;

    @Autowired
    @Qualifier("globalProps")
    private Properties globalProps;

    @PostMapping
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Importing Action!");
        try {
            Dao dao = new Dao(globalProps);
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");

            StringBuilder sbFile = new StringBuilder();
            String scmOverwrite = "false";
            String scmServer = "";
            String scmFilePath = "";
            String scmFileRevision = "";
            String uploadFileName = "";

            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1034);
            ServletFileUpload upload = new ServletFileUpload(factory);
            List items = upload.parseRequest(request);

            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                if (item.isFormField()) {
                    if (item.getFieldName().equals("scmOverwrite")) {
                        scmOverwrite = item.getString();
                    }
                    if (item.getFieldName().equals("scmServer")) {
                        scmServer = item.getString();
                    }
                    if (item.getFieldName().equals("scmFilePath")) {
                        scmFilePath = item.getString();
                    }
                    if (item.getFieldName().equals("scmFileRevision")) {
                        scmFileRevision = item.getString();
                    }

                } else {
                    uploadFileName = item.getName();
                    sbFile.append(item.getString("UTF-8"));
                }
            }

            InputStream inpStream;

            if (sbFile.toString().length() == 0) {
                uploadFileName = scmServer + scmFileRevision + "@" + scmFilePath;
                logger.debug("P4 file Processing " + uploadFileName);
                dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Importing P4 File: " + uploadFileName + "<br/>" + "Overwrite: " + scmOverwrite);
                URL url = new URL(uploadFileName);
                URLConnection conn = url.openConnection();
                inpStream = conn.getInputStream();

            } else {
                logger.debug("Upload file Processing " + uploadFileName);
                dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Uploading File: " + uploadFileName + "<br/>" + "Overwrite: " + scmOverwrite);
                inpStream = new ByteArrayInputStream(sbFile.toString().getBytes("UTF-8"));
            }

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(inpStream));
            String inputLine;
            List<String> importFile = new ArrayList<>();
            Integer lineCnt = 0;
            while ((inputLine = br.readLine()) != null) {
                lineCnt++;
                // Empty or comment?
                if (inputLine.trim().equals("") || inputLine.trim().startsWith("#")) {
                    continue;
                }
                if (inputLine.startsWith("-")) {
                    //DO nothing.
                } else if (!inputLine.matches("/.+=.+=.*")) {
                    throw new IOException("Invalid format at line " + lineCnt + ": " + inputLine);
                }

                importFile.add(inputLine);
            }
            br.close();

            ZooKeeperUtil.INSTANCE.importData(importFile, Boolean.valueOf(scmOverwrite), ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps));
            for (String line : importFile) {
                if (line.startsWith("-")) {
                    dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "File: " + uploadFileName + ", Deleting Entry: " + line);
                } else {
                    dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "File: " + uploadFileName + ", Adding Entry: " + line);
                }
            }
            request.getSession().setAttribute("flashMsg", "Import Completed!");
            response.sendRedirect(contentPath + "/home");
        } catch (FileUploadException | IOException | InterruptedException | KeeperException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
