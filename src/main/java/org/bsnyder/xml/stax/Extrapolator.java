/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2015 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 */
package org.bsnyder.xml.stax;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility to multiply a set of IDocs to grow them larger.
 *
 * @author bsnyder
 */
public class Extrapolator {

    private final static Logger LOG = LoggerFactory.getLogger(Extrapolator.class);
    private String inputDirectoryName = null;
    private String outputDirectoryName = null;
    private int totalNumberOfFilesToCreate = 0;
    private int numberOfCopiesPerFile = 0;
    private AtomicInteger fileCounter = new AtomicInteger();

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
        Stopwatch stopwatch = Stopwatch.createStarted();
        final String[] xmlFiles = grabFilesFromDirectory(inputDirectoryName);
        /*
         * 1) Loop over all XML files in the directory
         * 2) Divide the numberOfFilesToCreate by the xmlFiles.length to determine how many copies
         *    of each file needs to be created
         * 3) Loop over each file to extrapolate it that many times
         */
        numberOfCopiesPerFile = totalNumberOfFilesToCreate / xmlFiles.length;
        LOG.debug("Creating {} copies of each file", numberOfCopiesPerFile);
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
        stopwatch.stop();
        LOG.debug("Elapsed time: {}", stopwatch);
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
     * This method is uuuuuugly -- don't hate me.
     *
     * @param xmlFileToParse
     * @param newFile
     * @throws FileNotFoundException
     */
    void parseOldFileAndWriteToNewFile(File xmlFileToParse, File newFile) throws FileNotFoundException {

//        LOG.debug("Parsing existing doc: {}", xmlFileToParse.getAbsolutePath());
//        LOG.debug("Creating new doc: {}", newFile.getAbsolutePath());

        final String tabnamElement = "TABNAM";
        final String e1pbe1matheadElement = "E1BPE1MATHEAD";
        final String materialElement = "MATERIAL";

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        XMLEventWriter writer = null;

        try {
            // Set up the IDoc to parse
            inputStream = new FileInputStream(xmlFileToParse);
            outputStream = new FileOutputStream(newFile);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();

            // Create the new file and hook up a writer to it
            writer =  outputFactory.createXMLEventWriter(outputStream, "UTF-8");
//            writer = new IndentingXMLEventWriter(outputFactory.createXMLEventWriter(outputStream, "UTF-8"));
//            writer = new IndentingXMLEventWriter(outputFactory.createXMLEventWriter(System.out, "UTF-8"));

            while(eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    // Is this is the <TABNAM> element?
                    if (event.asStartElement().getName().getLocalPart().equals(tabnamElement)) {
                        //Write the <TABNAM> start element
                        write(writer, event);
                        event = eventReader.nextEvent();
                        if (event.isCharacters() && event.asCharacters().isCData()) {
                            String cData = event.asCharacters().getData();
                            // Write the CDATA
                            write(writer, eventFactory.createCData(cData));
                            event = eventReader.nextEvent();
                            // Write the <TABNAM> end element
                            write(writer, event);
                            event = eventReader.nextEvent();
                            // Write the newline event
//                            write(writer, event);
//                            event = eventReader.nextEvent();
                        }
                    }

                    if (event.isStartElement()) {
                        // Is this the <E1BPE1MATHEAD> element?
                        if (event.asStartElement().getName().getLocalPart().equals(e1pbe1matheadElement)) {
                            // The <E1BPE1MATHEAD> element has been located now find the <MATERIAL> element
                            // Write the <E1BPE1MATHEAD> element
                            write(writer, event);
//                            event = eventReader.nextEvent();
                            // Write the newline event
//                            write(writer, event);
                            event = eventReader.nextEvent();
                            // Is this is the <MATERIAL> element?
                            if (event.isStartElement() &&
                                    event.asStartElement().getName().getLocalPart().equals(materialElement)) {
                                // This *is* the <MATERIAL> element so replace the ID
                                // Write the <MATERIAL> start element
                                write(writer, event);
                                event = eventReader.nextEvent();
                                String existingId = event.asCharacters().getData();
//                                LOG.debug("Found existing id: {}", existingId);
                                // Append new id to the <MATERIAL> element text
                                final String uniqueId = createNewUniqueId(existingId);
//                                LOG.debug("Creating unique id: {}", uniqueId);
                                final Characters characters = eventFactory.createCharacters(uniqueId);
                                // Write the new ID to the new file
                                write(writer, characters);
                                event = eventReader.nextEvent();
                                // Write the <MATERIAL> end element
                                write(writer, event);
                            } else {
                                write(writer, event);
                            }
                        } else {
                            write(writer, event);
                        }
                    }
                } else {
                    write(writer, event);
                }
            }
        inputStream.close();
        writer.flush();
        writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(XMLEventWriter writer, XMLEvent event) throws XMLStreamException {
//        LOG.debug("Writing event: {}", event);
        writer.add(event);
    }

    /**
     * Append the &lt;MATERIAL&gt; element text with unique string "_BATCH_" + num
     */
    String createNewUniqueId(String existingId) {
        String newValue = existingId + "_BATCH_" + fileCounter;
        return newValue;
    }

    static class XmlFilesOnly implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".xml")) return true;
            return false;
        }
    }

}
