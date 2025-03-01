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
import com.dxrnd.zkui.vo.LeafBean;
import com.dxrnd.zkui.vo.ZKNode;
import freemarker.template.TemplateException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
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
@RequestMapping("/home")
public class Home {
    private final static Logger logger = LoggerFactory.getLogger(Home.class);

    @Value("${server.servlet.context-path}")
    private String contentPath;

    @Autowired
    @Qualifier("globalProps")
    private Properties globalProps;

    @GetMapping
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("Home Get Action!");
        try {

            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");

            Map<String, Object> templateParam = new HashMap<>();
            String zkPath = request.getParameter("zkPath");
            String navigate = request.getParameter("navigate");
            ClassPathResource classPathResource = new ClassPathResource("application.properties");
            Properties globalProps = PropertiesLoaderUtils.loadProperties(classPathResource);
            ZooKeeper zk = ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps);
            List<String> nodeLst;
            List<LeafBean> leafLst;
            String currentPath, parentPath, displayPath;
            String authRole = (String) request.getSession().getAttribute("authRole");
            if (authRole == null) {
                authRole = ZooKeeperUtil.ROLE_USER;
            }

            if (zkPath == null || zkPath.equals("/")) {
                templateParam.put("zkpath", "/");
                ZKNode zkNode = ZooKeeperUtil.INSTANCE.listNodeEntries(zk, "/", authRole);
                nodeLst = zkNode.getNodeLst();
                leafLst = zkNode.getLeafBeanLSt();
                currentPath = "/";
                displayPath = "/";
                parentPath = "/";
            } else {
                templateParam.put("zkPath", zkPath);
                ZKNode zkNode = ZooKeeperUtil.INSTANCE.listNodeEntries(zk, zkPath, authRole);
                nodeLst = zkNode.getNodeLst();
                leafLst = zkNode.getLeafBeanLSt();
                currentPath = zkPath + "/";
                displayPath = zkPath;
                parentPath = zkPath.substring(0, zkPath.lastIndexOf("/"));
                if (parentPath.equals("")) {
                    parentPath = "/";
                }
            }

            templateParam.put("displayPath", displayPath);
            templateParam.put("parentPath", parentPath);
            templateParam.put("currentPath", currentPath);
            templateParam.put("nodeLst", nodeLst);
            templateParam.put("leafLst", leafLst);
            templateParam.put("breadCrumbLst", displayPath.split("/"));
            templateParam.put("scmRepo", globalProps.getProperty("scmRepo"));
            templateParam.put("scmRepoPath", globalProps.getProperty("scmRepoPath"));
            templateParam.put("navigate", navigate);

            ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "home.ftl.html");

        } catch (KeeperException | InterruptedException | TemplateException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }

    }

    @PostMapping
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Home Post Action!");
        try {
            Dao dao = new Dao(globalProps);
            String zkServer = globalProps.getProperty("zkServer");
            String[] zkServerLst = zkServer.split(",");

            Map<String, Object> templateParam = new HashMap<>();
            String action = request.getParameter("action");
            String currentPath = request.getParameter("currentPath");
            String displayPath = request.getParameter("displayPath");
            String newProperty = request.getParameter("newProperty");
            String newValue = request.getParameter("newValue");
            String newNode = request.getParameter("newNode");

            String[] nodeChkGroup = request.getParameterValues("nodeChkGroup");
            String[] propChkGroup = request.getParameterValues("propChkGroup");

            String searchStr = request.getParameter("searchStr").trim();
            String authRole = (String) request.getSession().getAttribute("authRole");

            switch (action) {
                case "Save Node":
                    if (!newNode.equals("") && !currentPath.equals("") && authRole.equals(ZooKeeperUtil.ROLE_ADMIN)) {
                        //Save the new node.
                        ZooKeeperUtil.INSTANCE.createFolder(currentPath + newNode, "foo", "bar", ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps));
                        request.getSession().setAttribute("flashMsg", "Node created!");
                        dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Creating node: " + currentPath + newNode);
                    }
                    response.sendRedirect(contentPath + "/home?zkPath=" + displayPath);
                    break;
                case "Save Property":
                    if (!newProperty.equals("") && !currentPath.equals("") && authRole.equals(ZooKeeperUtil.ROLE_ADMIN)) {
                        //Save the new node.
                        ZooKeeperUtil.INSTANCE.createNode(currentPath, newProperty, newValue, ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps));
                        request.getSession().setAttribute("flashMsg", "Property Saved!");
                        if (ZooKeeperUtil.INSTANCE.checkIfPwdField(newProperty)) {
                            newValue = ZooKeeperUtil.INSTANCE.SOPA_PIPA;
                        }
                        dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Saving Property: " + currentPath + "," + newProperty + "=" + newValue);
                    }
                    response.sendRedirect(contentPath + "/home?zkPath=" + displayPath);
                    break;
                case "Update Property":
                    if (!newProperty.equals("") && !currentPath.equals("") && authRole.equals(ZooKeeperUtil.ROLE_ADMIN)) {
                        //Save the new node.
                        ZooKeeperUtil.INSTANCE.setPropertyValue(currentPath, newProperty, newValue, ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps));
                        request.getSession().setAttribute("flashMsg", "Property Updated!");
                        if (ZooKeeperUtil.INSTANCE.checkIfPwdField(newProperty)) {
                            newValue = ZooKeeperUtil.INSTANCE.SOPA_PIPA;
                        }
                        dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Updating Property: " + currentPath + "," + newProperty + "=" + newValue);
                    }
                    response.sendRedirect(contentPath + "/home?zkPath=" + displayPath);
                    break;
                case "Search":
                    Set<LeafBean> searchResult = ZooKeeperUtil.INSTANCE.searchTree(searchStr, ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps), authRole);
                    templateParam.put("searchResult", searchResult);
                    ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "search.ftl.html");
                    break;
                case "Delete":
                    if (authRole.equals(ZooKeeperUtil.ROLE_ADMIN)) {

                        if (propChkGroup != null) {
                            for (String prop : propChkGroup) {
                                List delPropLst = Arrays.asList(prop);
                                ZooKeeperUtil.INSTANCE.deleteLeaves(delPropLst, ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps));
                                request.getSession().setAttribute("flashMsg", "Delete Completed!");
                                dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Deleting Property: " + delPropLst.toString());
                            }
                        }
                        if (nodeChkGroup != null) {
                            for (String node : nodeChkGroup) {
                                List delNodeLst = Arrays.asList(node);
                                ZooKeeperUtil.INSTANCE.deleteFolders(delNodeLst, ServletUtil.INSTANCE.getZookeeper(request, response, zkServerLst[0], globalProps));
                                request.getSession().setAttribute("flashMsg", "Delete Completed!");
                                dao.insertHistory((String) request.getSession().getAttribute("authName"), request.getRemoteAddr(), "Deleting Nodes: " + delNodeLst.toString());
                            }
                        }

                    }
                    response.sendRedirect(contentPath + "/home?zkPath=" + displayPath);
                    break;
                default:
                    response.sendRedirect(contentPath + "/home");
            }

        } catch (InterruptedException | TemplateException | KeeperException ex) {
            logger.error(Arrays.toString(ex.getStackTrace()));
            ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
        }
    }
}
