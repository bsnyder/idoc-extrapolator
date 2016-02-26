/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2016 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 */
package org.bsnyder.xml.stax;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MatmasExtrapolatorTest {

    private final static Logger LOG = LoggerFactory.getLogger(MatmasExtrapolatorTest.class);

    private String inputPath = getClass().getClassLoader().getResource("matmas-idocs").getPath() + "/";
    private String outputPath = inputPath + "output";
    private Extrapolator ex;

    public MatmasExtrapolatorTest() {
        File outputDir = new File(outputPath);
        outputDir.mkdir();
    }

    private void grabFilesFromDirectory(String pathToDir) throws Exception {
        ex = new MatmasExtrapolator(pathToDir, 5);
        assertNotNull(ex);
        String[] files = ex.grabFilesFromDirectory(pathToDir);
        assertNotNull(files);
        assertEquals(4, files.length);
    }

    @Test
    public void testParseXmlFileAndWriteNewFile() throws Exception {
        String testIdocName = "MATMAS05_0000000004970786_BES_MAT_0000533.xml";
        String newIdocName = "MATMAS05_test.xml";
        parseXmlFileandWriteNewFile(testIdocName, newIdocName);
    }

    private void parseXmlFileandWriteNewFile(String testIdocName, String newIdocName) throws Exception {
        File testIdocFile = new File(inputPath + "/" + testIdocName);
        File newIdocFile = new File(outputPath + "/" + newIdocName);
        ex = new MatmasExtrapolator(inputPath, 2);
        ex.parseOldFileAndWriteToNewFile(testIdocFile, newIdocFile);
        assertTrue(testIdocFile.exists());
        assertTrue(newIdocFile.exists());
        String existingId = extractId(testIdocFile);
        String newId = ex.createNewUniqueId(existingId);
        assertTrue(newId.matches(existingId + "_BATCH_\\d{0,100}"));
        cleanUp(newIdocFile);
    }

    String extractId(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);

/*
        // begin debugging
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(domSource, result);
        writer.flush();
        LOG.debug(writer.toString());
        // end debugging
*/

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        String exprString = "MATMAS05/IDOC/E1MARAM/MATNR/text()";
        XPathExpression xpathExpr = xpath.compile(exprString);
        String actualText = (String) xpathExpr.evaluate(doc, XPathConstants.STRING);
        return actualText;
    }

    @Test
    public void testExtrapolate() throws Exception {
        int numberOfIdocs = 12;
        ex = new MatmasExtrapolator(inputPath, numberOfIdocs);
        ex.extrapolate();
        File dir = new File(outputPath);
        String[] xmlFilesOutput = null;
        if (dir.exists()) {
            xmlFilesOutput = dir.list(new Extrapolator.XmlFilesOnly());
        }
        String[] xmlFilesInput = ex.grabFilesFromDirectory(inputPath);
        int numberOfCopiesPerFile = numberOfIdocs / xmlFilesInput.length;
        int expectedNumberOfXmlFiles = xmlFilesInput.length * numberOfCopiesPerFile;
        String errorMsg = "Found '" + xmlFilesOutput.length + "' XML files in dir: '"  + inputPath + "'";
        assertEquals(errorMsg, expectedNumberOfXmlFiles, xmlFilesOutput.length);
    }

    private void cleanUp(File file) {
        FileUtils.deleteQuietly(file);
    }

}
