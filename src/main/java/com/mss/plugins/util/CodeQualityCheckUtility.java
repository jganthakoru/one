package com.mss.plugins.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CodeQualityCheckUtility {

	private final Logger log = Logger.getLogger(CodeQualityCheckUtility.class.getName());

	public List<String> collectViolations(String plugintype, String filePath, String tagName) throws ParserConfigurationException, SAXException, IOException {

		List<String> errorList = new ArrayList<String>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(filePath);
		String tageName = plugintype.equals("checkstyle")?"error":"violation";
		NodeList fileListNodes = document.getElementsByTagName("file");
		for (int i = 0; i < fileListNodes.getLength(); i++) {
			 Node fileNode = fileListNodes.item(i);
		     if(fileNode.getNodeType() == Node.ELEMENT_NODE) {
		    	Element fileElement = (Element) fileNode;
				String fileName = fileElement.getAttribute("name");
				NodeList errorNodes = fileElement.getElementsByTagName(tageName);
				for (int j = 0; j < errorNodes.getLength(); j++) {
					Node errorNode = errorNodes.item(j);
						if (errorNode.getNodeType() == Node.ELEMENT_NODE) {
							Element errorElement = (Element) errorNode;
							if(plugintype.equals("checkstyle")) {
							String errorMessage = errorElement.getAttribute("message");
							String lineNumber = errorElement.getAttribute("line");
							errorList.add("At File : " + fileName + "::" + errorMessage + " At Line No " + lineNumber);
						 }else {
							 errorList.add("At File : " + fileName + "::" + errorElement.getTextContent().trim() + " At Line No " + errorElement.getAttribute("beginline"));
						}
			     }
			  }
		    }
		}
		if(plugintype.equals("pmd")) {
			errorList.stream().forEach(error -> log.severe(error));
		}
		return errorList;
	}

	public void writeReportsToHtml(String filePath, List<String> checkStyleErrors, List<String> pmdErrors)
			throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write("<html><head><title>Code Quality Report</title></head><body>");
			writer.write("<h2 style=\"color:blue;\">Code Quality Report for Checkstyle and PMD</h2>");

			writer.write("<h2 style=\"color:red;\">CheckStyle Violations:</h2><ul>");
			writer.write("<div>");
			writer.write("<ul>");
			for (String error : checkStyleErrors) {
				writer.write("<li>" + error + "</li>");
			}
			writer.write("</ul>");

			writer.write("<h2 style=\"color:red;\">PMD Violations:</h2>");
			writer.write("<div>");
			writer.write("<ul>");
			for (String error : pmdErrors) {
				writer.write("<li>" + error + "</li>");
			}
			writer.write("</ul>");
			writer.write("</body></html>");
		}
		log.info("Report written to " + filePath);
	}
}