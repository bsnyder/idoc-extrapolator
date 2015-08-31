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

import static org.junit.Assert.*;

public class ExtrapolatorTest {

   private final static Logger LOG = LoggerFactory.getLogger(ExtrapolatorTest.class);

   private Extrapolator ex;
   private String path = getClass().getClassLoader().getResource("idocs").getPath();

   public void grabFilesFromDirectory(String pathToDir) throws Exception {
      ex = new Extrapolator(pathToDir, 5);
      assertNotNull(ex);
      String[] files = ex.grabFilesFromDirectory(pathToDir);
      assertNotNull(files);
      assertEquals(1, files.length);
      assertEquals("test-idoc.xml", files[0]);
   }

   @Test
   public void testGrabFilesFromDirectory() throws Exception {
      String path = getClass().getClassLoader().getResource("idocs").getPath();
      grabFilesFromDirectory(path);
   }

   @Test(expected = FileNotFoundException.class)
   public void testGrabFilesFromDirectoryWithBadDirectoryName() throws Exception {
      grabFilesFromDirectory("foo");
   }

   @Test
   public void testGenerateNewFileNameFromOldFileName() throws Exception {
//      String path = getClass().getClassLoader().getResource("idocs").getPath();
      ex = new Extrapolator(path, 5);
      String[] files = ex.grabFilesFromDirectory(path);
      assertNotNull(files);
      assertEquals(1, files.length);
      assertEquals("test-idoc.xml", files[0]);
      String newFileName = ex.generateNewFileNameFromOldFileName(new File(files[0]));
      assertEquals("test-idoc_1.xml", newFileName);
   }

   @Test
   public void testParseXmlFileAndWriteToNewFile() throws Exception {
      String testIdocName = "/test-idoc.xml";
      String newIdocName = "/new-idoc.xml";
      File testIdocFile = new File(path + testIdocName);
      File newIdocFile = new File(path + newIdocName);
      ex = new Extrapolator(path, 2);
      ex.parseOldFileAndWriteToNewFile(testIdocFile, newIdocFile);
      assertTrue(testIdocFile.exists());
      checkUniqueId(newIdocFile);
      cleanUp(newIdocFile);
   }

   private void checkUniqueId(File file) throws Exception {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(file);
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      String xpathExpr = "ARTMAS07/IDOC/E1BPE1MATHEAD/MATERIAL/text()";
      XPathExpression expr = xpath.compile(xpathExpr);
      String actualText = (String) expr.evaluate(doc, XPathConstants.STRING);
      assertEquals("000000000003051918_BATCH_0", actualText);
   }

   private void cleanUp(File file) {
      FileUtils.deleteQuietly(file);
   }


}
