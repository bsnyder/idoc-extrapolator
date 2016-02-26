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

public class ExtrapolatorTest {

    private final static Logger LOG = LoggerFactory.getLogger(ExtrapolatorTest.class);

    private String inputPath = getClass().getClassLoader().getResource("artmas-idocs").getPath() + "/";
    private String outputPath = inputPath + "/output";
    private Extrapolator ex;

    public ExtrapolatorTest() {
        File outputDir = new File(outputPath);
        outputDir.mkdir();
    }

    public void grabFilesFromDirectory(String pathToDir) throws Exception {
        ex = new DefaultExtrapolator(pathToDir, 5);
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
        ex = new DefaultExtrapolator(inputPath, 1);
        String testIdocName = "test-idoc.xml";
        File testIdocFile = new File(inputPath + "/" + testIdocName);
        String newIdocName = "test-idoc_1.xml";
        String newFileName = ex.generateNewFileNameFromOldFileName(testIdocFile);
        assertEquals(newIdocName, newFileName);
    }

    private void cleanUp(File file) {
        FileUtils.deleteQuietly(file);
    }

    private class DefaultExtrapolator extends Extrapolator {

        public DefaultExtrapolator(String inputDirectoryName, int totalNumberOfFilesToCreate) throws FileNotFoundException {
            super(inputDirectoryName, totalNumberOfFilesToCreate);
        }

        @Override
        String createNewUniqueId(String existingId) {
            return null;
        }

        @Override
        void parseOldFileAndWriteToNewFile(File xmlFileToParse, File newFile) throws FileNotFoundException {
            // Null impl
        }
    }

}
