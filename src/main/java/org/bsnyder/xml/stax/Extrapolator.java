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
    private AtomicInteger counter = new AtomicInteger();

    public Extrapolator(String dirname, int numberOfFiles) {
        this.dirname = dirname;
        this.numberOfFiles = numberOfFiles;
    }

    public synchronized void extrapolate() throws FileNotFoundException {
        final String[] xmlFiles = grabFilesFromDirectory(dirname);
        for(int i = 0; i < xmlFiles.length; ++i) {
            File oldFile = new File(xmlFiles[i]);
            if (oldFile.exists()) {
                String newFileName = generateNewFileNameFromOldFileName(oldFile);
                parseXmlFileAndWriteToNewFile(oldFile, newFileName);
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

    void parseXmlFileAndWriteToNewFile(File xmlFileToParse, String newFileName) throws FileNotFoundException {

        if (!xmlFileToParse.exists()) {
            throw new FileNotFoundException(" Unable to open file '" + xmlFileToParse.getPath() + "' because it does not exist");
        } else if (!xmlFileToParse.isFile()) {
            throw new FileNotFoundException("'" + xmlFileToParse.getPath() + "' is not a file");
        }

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
                    locateElementAndReplace(elementToMatch, materialElement, writer, eventReader, eventFactory, event);
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
        /*
        finally {
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
        */
    }

    void locateElementAndReplace(String elementToMatch, String materialElement, XMLEventWriter writer, XMLEventReader eventReader, XMLEventFactory eventFactory, XMLEvent event) throws XMLStreamException {
        StartElement startElement = event.asStartElement();
        String element = startElement.getName().toString();
        if (element.equals(elementToMatch)) {
            // The unique element has been located now find the <MATERIAL> element
            event = eventReader.nextEvent();
            if (event.isStartElement()) {
                startElement = event.asStartElement();
                element = startElement.getName().toString();
                if (element.equals(materialElement)) {
                    replaceText(writer, eventFactory, event);

                }
                /*
                // TODO Verifiy that the following is not needed
                else {
                    // Write everything else to the new file
                    writer.add(event);
                }
                */
            }
        } else {
            // Write everything else to the new file
            writer.add(event);
        }
    }

    void replaceText(XMLEventWriter writer, XMLEventFactory eventFactory, XMLEvent event) throws XMLStreamException {
        // Replace the <MATERIAL> element text with unique string "_BATCH_" + num
        Characters characters = event.asCharacters();
        String data = characters.getData();
        String newValue = data + "_BATCH_" + counter;
        writer.add(eventFactory.createCharacters(newValue));
    }

    class XmlFilesOnly implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".xml")) return true;
            return false;
        }
    }

}
