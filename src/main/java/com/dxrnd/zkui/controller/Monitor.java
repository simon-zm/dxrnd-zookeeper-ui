/**
 *
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.dxrnd.zkui.controller;

import com.dxrnd.zkui.utils.CmdUtil;
import com.dxrnd.zkui.utils.ServletUtil;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("serial")
@RestController
@RequestMapping("/monitor")
public class Monitor{

    private final static Logger logger = LoggerFactory.getLogger(Monitor.class);
    @Autowired
    @Qualifier("globalProps")
    private Properties globalProps;
    @GetMapping
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Monitor Action!");
        try {
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");

            Map<String, Object> templateParam = new HashMap<>();
            StringBuffer stats = new StringBuffer();
            for (String zkObj : zkServerLst) {
                stats.append("<br/><hr/><br/>").append("Server: ").append(zkObj).append("<br/><hr/><br/>");
                String[] monitorZKServer = zkObj.split(":");
                stats.append(CmdUtil.INSTANCE.executeCmd("stat", monitorZKServer[0], monitorZKServer[1]));
                stats.append(CmdUtil.INSTANCE.executeCmd("envi", monitorZKServer[0], monitorZKServer[1]));
            }
            templateParam.put("stats", stats);
            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "monitor.ftl.html");

        } catch (IOException | TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
