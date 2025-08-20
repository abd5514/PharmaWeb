package org.tab.data;

import org.tab.utils.PropReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class TestDataReader {

    public static String getXMLData(String dataFor) {
        String nodeValue = null;
        try{
            File file = new File(PropReader.get("testDataFile", "src/test/resources/testData.xml"));
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document document = documentBuilder.parse(file);
            nodeValue =  document.getElementsByTagName(dataFor).item(0).getTextContent();
        }
        catch(Exception e)
        {
            nodeValue = null;
        }

        return nodeValue;
    }

}
