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

import com.dxrnd.zkui.utils.LdapAuth;
import com.dxrnd.zkui.utils.ServletUtil;
import com.dxrnd.zkui.utils.ZooKeeperUtil;
import freemarker.template.TemplateException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("serial")
@RestController
@RequestMapping("/login")
public class Login {

    private final static Logger logger = LoggerFactory.getLogger(Login.class);

    @Value("${server.servlet.context-path}")
    private String contentPath;

    @Autowired
    @Qualifier("globalProps")
    private Properties globalProps;

    @GetMapping
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("Login Action!");
        try {
            Map<String, Object> templateParam = new HashMap<>();
            templateParam.put("uptime", globalProps.getProperty("uptime"));
            templateParam.put("loginMessage", globalProps.getProperty("loginMessage"));
            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
        } catch (TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }

    @PostMapping
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("Login Post Action!");
        try {
            Map<String, Object> templateParam = new HashMap<>();
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(Integer.valueOf(globalProps.getProperty("sessionTimeout")));
            //TODO: Implement custom authentication logic if required.
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String role = null;
            Boolean authenticated = false;
            //if ldap is provided then it overrides roleset.
            if (globalProps.getProperty("ldapAuth").equals("true")) {
                authenticated = new LdapAuth().authenticateUser(globalProps.getProperty("ldapUrl"), username, password, globalProps.getProperty("ldapDomain"));
                if (authenticated) {
                    JSONArray jsonRoleSet = (JSONArray) ((JSONObject) new JSONParser().parse(globalProps.getProperty("ldapRoleSet"))).get("users");
                    for (Iterator it = jsonRoleSet.iterator(); it.hasNext(); ) {
                        JSONObject jsonUser = (JSONObject) it.next();
                        if (jsonUser.get("username") != null && jsonUser.get("username").equals("*")) {
                            role = (String) jsonUser.get("role");
                        }
                        if (jsonUser.get("username") != null && jsonUser.get("username").equals(username)) {
                            role = (String) jsonUser.get("role");
                        }
                    }
                    if (role == null) {
                        role = ZooKeeperUtil.ROLE_USER;
                    }

                }
            } else {
                JSONArray jsonRoleSet = (JSONArray) ((JSONObject) new JSONParser().parse(globalProps.getProperty("userSet"))).get("users");
                for (Iterator it = jsonRoleSet.iterator(); it.hasNext(); ) {
                    JSONObject jsonUser = (JSONObject) it.next();
                    if (jsonUser.get("username").equals(username) && jsonUser.get("password").equals(password)) {
                        authenticated = true;
                        role = (String) jsonUser.get("role");
                    }
                }
            }
            if (authenticated) {
                logger.info("Login successful: " + username);
                session.setAttribute("authName", username);
                session.setAttribute("authRole", role);
                response.sendRedirect(contentPath + "/home");
            } else {
                session.setAttribute("flashMsg", "Invalid Login");
                ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
            }

        } catch (ParseException | TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
