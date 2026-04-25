package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Optional;

/**
 * Default {@link PomReader} that lazily parses a {@code pom.xml} on disk and resolves
 * dotted paths against its DOM. Caches the parsed {@link Document} after the first read.
 *
 * <p>The instance is bound to a single file. Missing file or parse errors degrade to
 * "every read returns empty" with a single error logged on first access.
 */
public final class DomPomReader implements PomReader {

    private final File pomFile;
    private boolean attempted;
    private Document document;

    public DomPomReader(File pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public Optional<String> read(String path) {
        Element root = root();
        if (root == null) {
            return Optional.empty();
        }
        if (path.startsWith("properties.")) {
            String key = path.substring("properties.".length());
            Element properties = childElement(root, "properties");
            if (properties == null) {
                return Optional.empty();
            }
            return textValue(childElement(properties, key));
        }
        Element node = root;
        for (String segment : path.split("\\.")) {
            node = childElement(node, segment);
            if (node == null) {
                return Optional.empty();
            }
        }
        return textValue(node);
    }

    private Element root() {
        if (!attempted) {
            attempted = true;
            if (pomFile == null || !pomFile.isFile()) {
                LOG.error("pom reader: file not found: {}", pomFile == null ? "<null>" : pomFile.getAbsolutePath());
                return null;
            }
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                document = db.parse(pomFile);
            } catch (Exception e) {
                LOG.error(e, () -> "pom reader: failed to parse " + pomFile.getAbsolutePath());
                document = null;
            }
        }
        return document == null ? null : document.getDocumentElement();
    }

    private static Element childElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static Optional<String> textValue(Element element) {
        if (element == null) {
            return Optional.empty();
        }
        String text = element.getTextContent();
        return text == null ? Optional.empty() : Optional.of(text.trim());
    }
}
