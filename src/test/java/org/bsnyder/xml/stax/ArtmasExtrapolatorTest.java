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
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArtmasExtrapolatorTest {

    private final static Logger LOG = LoggerFactory.getLogger(ArtmasExtrapolatorTest.class);

    private String inputPath = getClass().getClassLoader().getResource("artmas-idocs").getPath() + "/";
    private String outputPath = inputPath + "/output";
    private Extrapolator ex;

    public ArtmasExtrapolatorTest() {
        File outputDir = new File(outputPath);
        outputDir.mkdir();
    }

    public void grabFilesFromDirectory(String pathToDir) throws Exception {
        ex = new ArtmasExtrapolator(pathToDir, 5);
        assertNotNull(ex);
        String[] files = ex.grabFilesFromDirectory(pathToDir);
        assertNotNull(files);
        assertEquals(3, files.length);
    }

    @Test
    public void testGrabFilesFromDirectory() throws Exception {
        String path = getClass().getClassLoader().getResource("artmas-idocs").getPath();
        grabFilesFromDirectory(path);
    }

    @Test(expected = FileNotFoundException.class)
    public void testGrabFilesFromDirectoryWithBadDirectoryName() throws Exception {
        grabFilesFromDirectory("foo");
    }

    @Test
    public void testGenerateNewFileNameFromOldFileName() throws Exception {
        ex = new ArtmasExtrapolator(inputPath, 1);
        String testIdocName = "test-idoc.xml";
        File testIdocFile = new File(inputPath + "/" + testIdocName);
        String newIdocName = "test-idoc_1.xml";
        String newFileName = ex.generateNewFileNameFromOldFileName(testIdocFile);
        assertEquals(newIdocName, newFileName);
    }

    @Test
    public void testParseSmallXmlFileAndWriteNewFile() throws Exception {
        String testIdocName = "test-idoc.xml";
        String newIdocName = "new-idoc.xml";
        parseXmlFileandWriteNewFile(testIdocName, newIdocName);
    }

    @Test
    public void testParseMediumXmlFileAndWriteNewFile() throws Exception {
        String testIdocName = "O_701_0000001352676640.xml";
        String newIdocName = "O_701_0000001352676640_1.xml";
        parseXmlFileandWriteNewFile(testIdocName, newIdocName);
    }

    private void parseXmlFileandWriteNewFile(String testIdocName, String newIdocName) throws Exception {
        File testIdocFile = new File(inputPath + "/" + testIdocName);
        File newIdocFile = new File(outputPath + "/" + newIdocName);
        ex = new ArtmasExtrapolator(inputPath, 2);
        ex.parseOldFileAndWriteToNewFile(testIdocFile, newIdocFile);
        assertTrue(testIdocFile.exists());
        assertTrue(newIdocFile.exists());
        String existingId = extractId(testIdocFile);
        String expectedUniqueId = ex.createNewUniqueId(existingId);
        checkUniqueId(newIdocFile, expectedUniqueId);
        cleanUp(newIdocFile);
    }

    String extractId(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        String xpathExpr = "ARTMAS07/IDOC/E1BPE1MATHEAD/MATERIAL/text()";
        XPathExpression expr = xpath.compile(xpathExpr);
        String actualText = (String) expr.evaluate(doc, XPathConstants.STRING);
        return actualText;
    }

    @Test
    public void testExtrapolate() throws Exception {
        int numberOfIdocs = 129;
        ex = new ArtmasExtrapolator(inputPath, numberOfIdocs);
        ex.extrapolate();
        File dir = new File(outputPath);
        String[] xmlFilesOutput = null;
        if (dir.exists()) {
            xmlFilesOutput = dir.list(new Extrapolator.XmlFilesOnly());
        }
        String[] xmlFilesInput = ex.grabFilesFromDirectory(inputPath);
        int numberOfCopiesPerFile = numberOfIdocs / xmlFilesInput.length;
        int expectedNumberOfXmlFiles = xmlFilesInput.length * numberOfCopiesPerFile;
        assertEquals(expectedNumberOfXmlFiles, xmlFilesOutput.length);
    }

    private void checkUniqueId(File file, String expectedUniqueId) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        String xpathExpr = "ARTMAS07/IDOC/E1BPE1MATHEAD/MATERIAL/text()";
        XPathExpression expr = xpath.compile(xpathExpr);
        String actualText = (String) expr.evaluate(doc, XPathConstants.STRING);

        assertEquals(expectedUniqueId, actualText);
    }

    private void cleanUp(File file) {
        FileUtils.deleteQuietly(file);
    }

}