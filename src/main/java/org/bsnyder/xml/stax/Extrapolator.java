package org.bsnyder.xml.stax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility to multiply a set of IDocs to grow them larger.
 *
 * @author bsnyder
 */
public abstract class Extrapolator {

    private final static Logger LOG = LoggerFactory.getLogger(Extrapolator.class);

    private String inputDirectoryName = null;
    private String outputDirectoryName = null;
    private int totalNumberOfFilesToCreate = 0;
    private int numberOfCopiesPerFile = 0;
    protected AtomicInteger fileCounter = new AtomicInteger();

    public Extrapolator(String inputDirectoryName, int totalNumberOfFilesToCreate) throws FileNotFoundException {
        this.inputDirectoryName = inputDirectoryName;
        this.totalNumberOfFilesToCreate = totalNumberOfFilesToCreate;

        handleDirectory(inputDirectoryName);
    }

    void handleDirectory(String inputDirectoryName) throws FileNotFoundException {
        File dir = new File(inputDirectoryName);
        if (dir.exists()) {
            this.outputDirectoryName = inputDirectoryName + "/output/";
            createDirIfNotExists(outputDirectoryName);
        } else {
            throw new FileNotFoundException(inputDirectoryName + " Does not exist");
        }
    }

    void createDirIfNotExists(String outputDirectoryName) {
        File outputDir = new File(outputDirectoryName);
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
    }

    public synchronized void extrapolate() throws IOException {
        final String[] xmlFiles = grabFilesFromDirectory(inputDirectoryName);
        /*
         * 1) Loop over all XML files in the directory
         * 2) Divide the numberOfFilesToCreate by the xmlFiles.length to determine how many copies
         *    of each file needs to be created
         * 3) Loop over each file to duplicate it that many times, replacing the unique id
         */
        numberOfCopiesPerFile = totalNumberOfFilesToCreate / xmlFiles.length;
        int totalCopies = numberOfCopiesPerFile * xmlFiles.length;
        LOG.debug("Creating {} copies of each file for a total of {} copies", numberOfCopiesPerFile, totalCopies);
        for(int i = 0; i < xmlFiles.length; ++i) {
            fileCounter.set(0);
            for (int j = 0; j <= numberOfCopiesPerFile; ++j) {
                if (numberOfCopiesPerFile > 0 && fileCounter.get() < numberOfCopiesPerFile) {
                    File oldFile = new File(inputDirectoryName + xmlFiles[i]);
                    if (!oldFile.exists()) {
                        throw new FileNotFoundException(" Unable to open file '" + oldFile.getAbsolutePath() + "' because it does not exist");
                    } else if (!oldFile.isFile()) {
                        throw new FileNotFoundException("'" + oldFile.getAbsolutePath() + "' is not a file");
                    } else {
                        String newFileName = generateNewFileNameFromOldFileName(oldFile);
                        File newFile = new File(outputDirectoryName + newFileName);
                        if (!newFile.exists()) {
                            newFile.createNewFile();
                        }
                        parseOldFileAndWriteToNewFile(oldFile, newFile);
                    }
                }
            }
        }
    }

    String generateNewFileNameFromOldFileName(File oldFile) {
        int fileNum = fileCounter.incrementAndGet();
        String newFileName = null;
        if (fileNum <= totalNumberOfFilesToCreate) {
            final String baseName = oldFile.getName().substring(0, oldFile.getName().length() - 4);
            newFileName = baseName + "_" + fileNum + ".xml";
        }
        return newFileName;
    }

    String[] grabFilesFromDirectory(String pathToDir) throws FileNotFoundException {
        File dir = new File(pathToDir);
        String[] xmlFiles = null;
        if (dir.exists()) {
            xmlFiles = dir.list(new XmlFilesOnly());
            if (null == xmlFiles || xmlFiles.length == 0) {
                throw new FileNotFoundException("Unable to locate XML files in directory '" + pathToDir + "'");
            }
        } else {
            throw new FileNotFoundException("Unable to locate directory '" + pathToDir + "'");
        }
        return xmlFiles;
    }


    /**
     * Override this method to implement the XML element handling according to the document type (e.g., ARTMAS, MATMAS, etc.)
     * @param xmlFileToParse
     * @param newFile
     * @throws FileNotFoundException
     */
    abstract void parseOldFileAndWriteToNewFile(File xmlFileToParse, File newFile) throws FileNotFoundException;

    /**
     * Override this method to create a unique ID to use as the primary key
     * @param existingId
     * @return
     */
    abstract String createNewUniqueId(String existingId);

    void write(XMLEventWriter writer, XMLEvent event) throws XMLStreamException {
//        LOG.debug("Writing event: {}", event);
        writer.add(event);
    }

    static class XmlFilesOnly implements FilenameFilter {

        public boolean accept(File dir, String name) {
            if (name.endsWith(".xml")) return true;
            return false;
        }
    }

}
