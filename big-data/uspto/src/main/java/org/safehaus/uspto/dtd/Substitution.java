package org.safehaus.uspto.dtd;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.BasicDBObject;

public class Substitution implements Converter{

	private static final String title = "Substitution";
	
	protected Logger logger;
	
	private Relation relation;
	
	public Substitution(Logger logger) {
		this.logger = logger;
	}

	public Substitution(Element element, Logger logger)
	{
		this.logger = logger;
		
		NodeList nodes = element.getChildNodes();

		for (int j=0; j < nodes.getLength(); j++)
		{
			Node node = nodes.item(j);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) node;
				if (childElement.getNodeName().equals("relation"))
				{
					relation = new Relation(childElement, logger);
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
	
	public Relation getRelation() {
		return relation;
	}
	
	@Override
	public String toString() {
		StringBuffer toStringBuffer = new StringBuffer(title+":");
		if (relation != null)
		{
			toStringBuffer.append(" ");
			toStringBuffer.append(relation);
		}
		return toStringBuffer.toString();
	}
	
	public JSONObject toJSon() {
		JSONObject jsonObject = new JSONObject();
		if (relation != null)
		{
			jsonObject.put(relation.getTitle(), relation.toJSon());
		}
		return jsonObject;
	}

	public BasicDBObject toBasicDBObject() {
		BasicDBObject basicDBObject = new BasicDBObject();
		if (relation != null)
		{
			basicDBObject.put(relation.getTitle(), relation.toBasicDBObject());
		}
		return basicDBObject;
	}
	
	public String getTitle() {
		return title;
	}
	
}
