/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.tools;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XPathTool extends AbstractTool {

    private final static XPathFactory xPathFactory = XPathFactory.newInstance();
    private final static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    private XPath xPath;
    private Map<String, XPathExpression> xPathMap;

    @Override
    public void load(File parentDir) {
	synchronized (xPathFactory) {
	    xPath = xPathFactory.newXPath();
	}
	xPathMap = new HashMap<String, XPathExpression>();
    }

    @Override
    public void unload() {
	xPathMap.clear();
	xPathMap = null;
    }

    public XPathDocument readDocument(File file) throws ParserConfigurationException, SAXException, IOException {
	return new XPathDocument(file);
    }

    private XPathExpression getXPathExpression(String xpath_expression) throws XPathExpressionException {
	synchronized (xPathMap) {
	    XPathExpression xPathExpression = xPathMap.get(xpath_expression);
	    if (xPathExpression != null)
		return xPathExpression;
	    synchronized (xPath) {
		xPathExpression = xPath.compile(xpath_expression);
	    }
	    xPathMap.put(xpath_expression, xPathExpression);
	    return xPathExpression;
	}
    }

    public void clearXpathCache() {
	synchronized (xPathMap) {
	    xPathMap.clear();
	}
    }

    public class XPathDocument {

	private final Document document;

	private XPathDocument(File file) throws ParserConfigurationException, SAXException, IOException {
	    final DocumentBuilder builder;
	    synchronized (docFactory) {
		builder = docFactory.newDocumentBuilder();
	    }
	    document = builder.parse(file);
	}

	private Object xpath(String xpath_expression, Object object, QName xPathResult)
			throws XPathExpressionException {
	    if (object == null)
		object = document;
	    XPathExpression xPathExpression = getXPathExpression(xpath_expression);
	    synchronized (xPathExpression) {
		synchronized (xPath) {
		    return xPathExpression.evaluate(object, xPathResult);
		}
	    }
	}

	public Node xpath_node(String xpath_expression, Object object) throws XPathExpressionException {
	    return (Node) xpath(xpath_expression, object, XPathConstants.NODE);
	}

	public Node[] xpath_nodes(String xpath_expression, Object object) throws XPathExpressionException {
	    NodeList nodeList = (NodeList) xpath(xpath_expression, object, XPathConstants.NODESET);
	    if (nodeList == null)
		return null;
	    Node[] nodes = new Node[nodeList.getLength()];
	    for (int i = 0; i < nodes.length; i++)
		nodes[i] = nodeList.item(i);
	    return nodes;
	}

	public String xpath_text(String xpath_expression, Object object) throws XPathExpressionException {
	    return (String) xpath(xpath_expression, object, XPathConstants.STRING);
	}

	public Boolean xpath_boolean(String xpath_expression, Object object) throws XPathExpressionException {
	    return (Boolean) xpath(xpath_expression, object, XPathConstants.BOOLEAN);
	}

	public Number xpath_number(String xpath_expression, Object object) throws XPathExpressionException {
	    return (Number) xpath(xpath_expression, object, XPathConstants.NUMBER);
	}
    }

}