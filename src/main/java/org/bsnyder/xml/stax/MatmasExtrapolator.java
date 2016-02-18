package org.bsnyder.xml.stax;

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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An Extrapolator implementation for MATMAS IDocs
 */
public class MatmasExtrapolator extends Extrapolator {

    private final static Logger LOG = LoggerFactory.getLogger(MatmasExtrapolator.class);

    public MatmasExtrapolator(String inputDirectoryName, int totalNumberOfFilesToCreate) throws FileNotFoundException {
        super(inputDirectoryName, totalNumberOfFilesToCreate);
    }

    @Override
    void parseOldFileAndWriteToNewFile(File xmlFileToParse, File newFile) throws FileNotFoundException {
        LOG.debug("Parsing existing doc: {}", xmlFileToParse.getAbsolutePath());
        LOG.debug("Creating new doc: {}", newFile.getAbsolutePath());

        final String docNumElementName = "DOCNUM";

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
            writer = outputFactory.createXMLEventWriter(outputStream, "UTF-8");

            while(eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                int eventType = event.getEventType();

                switch(eventType) {
                    case XMLEvent.START_ELEMENT:
                        if (event.asStartElement().getName().getLocalPart().equals(docNumElementName)) {
                            LOG.debug("Found the DOCNUM element!");
                            // Write the DOCNUM element
                            write(writer, event);
                            // Grab the next event (should be the DOCNUM content) as characters
                            event = eventReader.nextEvent();
                            String existingId = event.asCharacters().getData();
                            // Create a new unique ID for the document
                            final String uniqueId = createNewUniqueId(existingId);
                            final Characters characters = eventFactory.createCharacters(uniqueId);
                            // Write the new ID to the new file
                            write(writer, characters);
                        } else {
                            write(writer, event);
                        }
                        break;
                    default:
                        write(writer, event);
                }
                /*
                if (event.isStartElement()) {

                    // Is this the MATMAS05 element?
//                    LOG.debug("Event: {}", event.asStartElement().getName().getLocalPart());
                    if (!event.asStartElement().getName().getLocalPart().equals(docNumElementName)){
                        write(writer, event);
                        /-*
                        event = eventReader.nextEvent();
                        // Is this the IDOC element?
                        LOG.debug("2) Event: {}", event.asStartElement().getName().getLocalPart());
                        if (event.asStartElement().getName().getLocalPart().equals(idocElementName)) {
                           write(writer, event);
                            event = eventReader.nextEvent();
                        }
                        *-/
                    } else {
                        // This should be the DOCNUM element
//                        LOG.debug("Event: {}", event.asStartElement().getName().getLocalPart());
                        if (event.asStartElement().getName().getLocalPart().equals(docNumElementName)) {
                            LOG.debug("Found the DOCNUM element!");
                            // Write the DOCNUM element
                            write(writer, event);
                            // Grab the next event (should be the DOCNUM content) as characters
                            event = eventReader.nextEvent();
                            String existingId = event.asCharacters().getData();
                            // Create a new unique ID for the document
                            final String uniqueId = createNewUniqueId(existingId);
                            final Characters characters = eventFactory.createCharacters(uniqueId);
                            // Write the new ID to the new file
                            write(writer, characters);
                        }
                    }
                } else {
                    LOG.debug("Writing event: {}", event);
                    write(writer, event);
                }
                */
            }

        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    @Override
    String createNewUniqueId(String existingId) {
        return "4970786_" + fileCounter;
    }
}
