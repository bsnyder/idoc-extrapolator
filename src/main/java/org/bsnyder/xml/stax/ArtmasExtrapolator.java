package org.bsnyder.xml.stax;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An Extrapolator implementation for ARTMAS IDocs
 */
public class ArtmasExtrapolator extends Extrapolator {

    public ArtmasExtrapolator(String inputDirectoryName, int totalNumberOfFilesToCreate) throws FileNotFoundException {
        super(inputDirectoryName, totalNumberOfFilesToCreate);
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
                        }
                    }

                    if (event.isStartElement()) {
                        // Is this the <E1BPE1MATHEAD> element?
                        if (event.asStartElement().getName().getLocalPart().equals(e1pbe1matheadElement)) {
                            // The <E1BPE1MATHEAD> element has been located now find the <MATERIAL> element
                            // Write the <E1BPE1MATHEAD> element
                            write(writer, event);
                            event = eventReader.nextEvent();
                            // Is this is the <MATERIAL> element?
                            if (event.isStartElement() &&
                                    event.asStartElement().getName().getLocalPart().equals(materialElement)) {
                                // This *is* the <MATERIAL> element so replace the ID
                                // Write the <MATERIAL> start element
                                write(writer, event);
                                event = eventReader.nextEvent();
                                String existingId = event.asCharacters().getData();
                                // Append new id to the <MATERIAL> element text
                                final String uniqueId = createNewUniqueId(existingId);
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

}
