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
import com.dxrnd.zkui.domain.History;
import com.dxrnd.zkui.utils.ServletUtil;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("serial")
@RestController
@RequestMapping("/history")
public class ChangeLog {
    private final static Logger logger = LoggerFactory.getLogger(ChangeLog.class);

    @Value("${server.servlet.context-path}")
    private String contentPath;

    @Autowired
    @Qualifier("globalProps")
    private Properties globalProps;

    @GetMapping
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("History Get Action!");
        try {
            Dao dao = new Dao(globalProps);
            Map<String, Object> templateParam = new HashMap<>();
            List<History> historyLst = dao.fetchHistoryRecords();
            templateParam.put("historyLst", historyLst);
            templateParam.put("historyNode", "");
            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "history.ftl.html");
        } catch (TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }

    @PostMapping
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("History Post Action!");
        try {
            Dao dao = new Dao(globalProps);
            Map<String, Object> templateParam = new HashMap<>();
            String action = request.getParameter("action");
            List<History> historyLst;
            if (action.equals("showhistory")) {

                String historyNode = request.getParameter("historyNode");
                historyLst = dao.fetchHistoryRecordsByNode("%" + historyNode + "%");
                templateParam.put("historyLst", historyLst);
                templateParam.put("historyNode", historyNode);
                ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "history.ftl.html");

            } else {
                response.sendRedirect(contentPath + "/history");
            }
        } catch (TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
