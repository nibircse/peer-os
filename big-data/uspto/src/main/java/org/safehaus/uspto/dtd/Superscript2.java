package org.safehaus.uspto.dtd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class Superscript2 extends SingleCollection<Converter>{

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private static final String title = "Superscript2";
	
	public Superscript2(Logger logger) {
		super();
		this.logger = logger;
	}
	
	public Superscript2(Element element, Logger logger)
	{
		super(element);
		this.logger = logger;
		
		NodeList nodeList = element.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) node;
				if (childElement.getNodeName().equals("b")) {
					elements.add(new Bold(childElement, logger));
				}
				else if (childElement.getNodeName().equals("i")) {
					elements.add(new Italic(childElement, logger));
				}
				else if (childElement.getNodeName().equals("o")) {
					elements.add(new Overscore(childElement, logger));
				}
				else if (childElement.getNodeName().equals("u")) {
					elements.add(new Underscore(childElement, logger));
				}
				else
				{
					logger.warn("Unknown Element {} in {} node", childElement.getNodeName(), title);
				}
			}
			else if (node.getNodeType() == Node.TEXT_NODE) {
				Text childText = (Text) node;
				elements.add(new TextNode(childText));
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

	@Override
	public String toString() {
		StringBuffer toStringBuffer = new StringBuffer(title+":");
		toStringBuffer.append(super.toString());
		return toStringBuffer.toString();
	}

	@Override
	public String getTitle() {
		return title;
	}
	
}
