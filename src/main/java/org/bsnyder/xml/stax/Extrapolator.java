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

import javax.xml.stream.*;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Extrapolator {

    private final String dirname;
    private final int numberOfFiles;
    private AtomicInteger counter;

    public Extrapolator(String dirname, int numberOfFiles) {
        this.dirname = dirname;
        this.numberOfFiles = numberOfFiles;
    }

    public synchronized void extrapolate() {
        final String[] xmlFiles = grabFilesFromDirectory(dirname);
        for(int i = 0; i < xmlFiles.length; ++i) {
            File oldFile = new File(xmlFiles[i]);
            if (oldFile.exists()) {
                String newFileName = generateNewFileNameFromOldFileName(oldFile);
                parseXmlFileAndWriteToNewFile(oldFile, newFileName);
            }
        }
    }

    private String generateNewFileNameFromOldFileName(File oldFile) {
        int fileNum = counter.incrementAndGet();
        String newFileName = null;
        if (fileNum < numberOfFiles) {
            final String baseName = oldFile.getName().substring(0, oldFile.getName().length() - 4);
            newFileName = baseName + "_" + fileNum + ".xml";
        }
        return newFileName;
    }

    private String[] grabFilesFromDirectory(String dirname) {
        File dir = new File(dirname);
        String[] xmlFiles = null;
        if (dir.exists()) {
            xmlFiles = dir.list(new XmlFilesOnly());

        }
        return xmlFiles;
    }

    private void parseXmlFileAndWriteToNewFile(File xmlFileToParse, String newFileName) {

        final String elementToMatch = "E1BPE1MATHEAD";
        final String materialElement = "MATERIAL";

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        InputStream inputStream = null;
        XMLEventWriter writer = null;

        try {
            inputStream = new FileInputStream(xmlFileToParse);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();

            writer = outputFactory.createXMLEventWriter(new FileWriter(newFileName));

            XMLEvent startEvent = eventFactory.createStartDocument();
            writer.add(startEvent);

            while(eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    String element = startElement.getName().toString();
                    if (element.equals(elementToMatch)) {
                        // The unique element has been located now find the <MATERIAL> element
                        event = eventReader.nextEvent();
                        if (event.isStartElement()) {
                            startElement = event.asStartElement();
                            element = startElement.getName().toString();
                            if (element.equals(materialElement)) {
                                // TODO: Locate and replace the <MATERIAL> element text with unique string "_BATCH_" + num
                                Characters characters = event.asCharacters();
                                String data = characters.getData();
                                String newValue = data + "_BATCH_" + counter;
                                writer.add(eventFactory.createCharacters(newValue));
                            } else {
                                // Write everything else to the new file
                                writer.add(event);
                            }
                        }
                    } else {
                        // Write everything else to the new file
                        writer.add(event);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.out.println("Trouble closing the InputStream");
                e.printStackTrace();
            } catch (XMLStreamException e) {
                System.out.println("Trouble closing the XMLEventWriter");
                e.printStackTrace();
            }
        }
    }

    private void writeNewXmlFile() {

    }

    private class XmlFilesOnly implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".xml")) return true;
            return false;
        }
    }

}
