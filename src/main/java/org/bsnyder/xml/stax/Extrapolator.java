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

import javanet.staxutils.IndentingXMLEventWriter;
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

public class Extrapolator {

    private final static Logger LOG = LoggerFactory.getLogger(Extrapolator.class);
    private final String dirname;
    private final int numberOfFiles;
    private AtomicInteger counter = new AtomicInteger();

    public Extrapolator(String dirname, int numberOfFiles) {
        this.dirname = dirname;
        this.numberOfFiles = numberOfFiles;
    }

    public synchronized void extrapolate() throws FileNotFoundException {
        final String[] xmlFiles = grabFilesFromDirectory(dirname);
        for(int i = 0; i < xmlFiles.length; ++i) {
            File oldFile = new File(xmlFiles[i]);
            if (!oldFile.exists()) {
                throw new FileNotFoundException(" Unable to open file '" + oldFile.getPath() + "' because it does not exist");
            } else if (!oldFile.isFile()) {
                throw new FileNotFoundException("'" + oldFile.getPath() + "' is not a file");
            } else {
                String newFileName = generateNewFileNameFromOldFileName(oldFile);
                File newFile = new File(newFileName);
                parseOldFileAndWriteToNewFile(oldFile, newFile);
            }
        }
    }

    String generateNewFileNameFromOldFileName(File oldFile) {
        int fileNum = counter.incrementAndGet();
        String newFileName = null;
        if (fileNum < numberOfFiles) {
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
        } else {
            throw new FileNotFoundException("Unable to locate directory '" + pathToDir + "'");
        }
        return xmlFiles;
    }

    void parseOldFileAndWriteToNewFile(File xmlFileToParse, File newFile) throws FileNotFoundException {

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
            writer = new IndentingXMLEventWriter(outputFactory.createXMLEventWriter(outputStream, "UTF-8"));

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
                            write(writer, event);
                            event = eventReader.nextEvent();
                        }
                    }

                    // Is this the <E1BPE1MATHEAD> element?
                    if (event.asStartElement().getName().getLocalPart().equals(e1pbe1matheadElement)) {
                        // The <E1BPE1MATHEAD> element has been located now find the <MATERIAL> element
                        // Write the <E1BPE1MATHEAD> element
                        write(writer, event);
                        event = eventReader.nextEvent();
                        // Write the newline event
                        write(writer, event);
                        event = eventReader.nextEvent();
                        // Is this is the <MATERIAL> element?
                        if (event.isStartElement() &&
                                event.asStartElement().getName().getLocalPart().equals(materialElement)) {
                            // This *is* the <MATERIAL> element so replace the ID
                            // Write the <MATERIAL> start element
                            write(writer, event);
                            event = eventReader.nextEvent();
                            // Append new string to the <MATERIAL> element text
                            final Characters replacementText = appendMaterialElementText(eventFactory, eventReader, event);
                            // Write the new ID to the new file
                            write(writer, replacementText);
                            event = eventReader.nextEvent();
                            // Write the <MATERIAL> end element
                            write(writer, event);
                        } else {
                            write(writer, event);
                        }
                    } else {
                        write(writer, event);
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
     *
     * @param eventFactory
     * @param event
     * @return
     * @throws XMLStreamException
     */
    Characters appendMaterialElementText(XMLEventFactory eventFactory, XMLEventReader eventReader, XMLEvent event) throws XMLStreamException {
        String origValue = event.asCharacters().getData();
        LOG.debug("Original value: {}", origValue);
        String newValue = origValue + "_BATCH_" + counter;
        LOG.debug("New value: {}", newValue);
        return eventFactory.createCharacters(newValue);
    }

    class XmlFilesOnly implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".xml")) return true;
            return false;
        }
    }

}
