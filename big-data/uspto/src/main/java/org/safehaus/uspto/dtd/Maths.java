package org.safehaus.uspto.dtd;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.BasicDBObject;

public class Maths implements Converter{

	private static final String title = "Maths";
	
	protected Logger logger;
	
	private String id;
	private String number;
	private Image image;
	
	public Maths(Logger logger) {
		this.logger = logger;
	}
	
	public Maths(Element element, Logger logger)
	{
		this.logger = logger;
		
		NamedNodeMap nodemap = element.getAttributes();
		for (int i=0; i < nodemap.getLength(); i++)
		{
			Node childNode = nodemap.item(i);
			
			if (childNode.getNodeType() == Node.ATTRIBUTE_NODE) {
				Attr attribute = (Attr) childNode;
				if (attribute.getNodeName().equals("id")) {
					id = attribute.getNodeValue();
				}
				else if (attribute.getNodeName().equals("num")) {
					number = attribute.getNodeValue();
				}
				else
				{
					logger.warn("Unknown Attribute {} in {} node", attribute.getNodeName(), title);
				}
			}
		}

		NodeList nodeList = element.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) node;
				if (childElement.getNodeName().equals("img")) {
					image = new Image(childElement, logger);
				}
				else if (childElement.getNodeName().equals("math")) {
					//TODO complete this.
				}
				else
				{
					logger.warn("Unknown Element {} in {} node", childElement.getNodeName(), title);
				}
			}
			else if (node.getNodeType() == Node.TEXT_NODE) {
				//ignore
			}
			else if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				//ignore
			}
			else
			{
				logger.warn("Unknown Node {} in {} node", node.getNodeName(), title);
			}
		}

	}

	public String getId() {
		return id;
	}

	public String getNumber() {
		return number;
	}

	public Image getImage() {
		return image;
	}

	@Override
	public String toString() {
		StringBuffer toStringBuffer = new StringBuffer(title+":");
		if (id != null)
		{
			toStringBuffer.append(" Id: ");
			toStringBuffer.append(id);
		}
		if (number != null)
		{
			toStringBuffer.append(" num: ");
			toStringBuffer.append(number);
		}
		if (image != null)
		{
			toStringBuffer.append(" ");
			toStringBuffer.append(image);
		}
		return toStringBuffer.toString();
	}

	public JSONObject toJSon() {
		JSONObject jsonObject = new JSONObject();
		if (id != null)
		{
			jsonObject.put("Id", id);
		}
		if (number != null)
		{
			jsonObject.put("Num", number);
		}
		if (image != null)
		{
			jsonObject.put(image.getTitle(), image.toJSon());
		}
		return jsonObject;
	}

	public BasicDBObject toBasicDBObject() {
		BasicDBObject basicDBObject = new BasicDBObject();
		if (id != null)
		{
			basicDBObject.put("Id", id);
		}
		if (number != null)
		{
			basicDBObject.put("Num", number);
		}
		if (image != null)
		{
			basicDBObject.put(image.getTitle(), image.toBasicDBObject());
		}
		return basicDBObject;
	}
	
	public String getTitle() {
		return title;
	}

}
