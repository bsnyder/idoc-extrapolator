package org.bsnyder.xml.stax;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExtrapolatorTest {

   Extrapolator ex;

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
      String path = getClass().getClassLoader().getResource("idocs").getPath();
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
      String path = getClass().getClassLoader().getResource("idocs").getPath();
      ex = new Extrapolator(path, 2);
      ex.parseOldFileAndWriteToNewFile(new File(path + "/test-idoc.xml"), new File(path + "/new-idoc.xml"));
      // TODO Verify that files have been written
   }

}
