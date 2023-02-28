//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.intellij.openapi.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.xml.util.XmlStringUtil;

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.filter.Filter;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;
import org.jdom.output.XMLOutputter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public final class JDOMUtil {
    public static final Pattern XPOINTER_PATTERN = Pattern.compile("xpointer\\((.*)\\)");
    public static final Namespace XINCLUDE_NAMESPACE =
            Namespace.getNamespace("xi", "http://www.w3.org/2001/XInclude");
    public static final Pattern CHILDREN_PATTERN = Pattern.compile("/([^/]*)(/[^/]*)?/\\*");
    private static volatile XMLInputFactory XML_INPUT_FACTORY;
    private static final JDOMUtil.EmptyTextFilter CONTENT_FILTER = new JDOMUtil.EmptyTextFilter();

    private static XMLInputFactory getXmlInputFactory() {
        XMLInputFactory factory = XML_INPUT_FACTORY;
        if (factory != null) {
            return factory;
        } else {
            synchronized (JDOMUtil.class) {
                factory = XML_INPUT_FACTORY;
                if (factory != null) {
                    return factory;
                } else {
                    String property =
                            System.setProperty(
                                    "javax.xml.stream.XMLInputFactory",
                                    "com.sun.xml.internal.stream.XMLInputFactoryImpl");

                    try {
                        factory = XMLInputFactory.newFactory();
                    } finally {
                        if (property != null) {
                            System.setProperty("javax.xml.stream.XMLInputFactory", property);
                        } else {
                            System.clearProperty("javax.xml.stream.XMLInputFactory");
                        }
                    }

                    if (!SystemInfo.isIbmJvm) {
                        try {
                            factory.setProperty(
                                    "http://java.sun.com/xml/stream/properties/report-cdata-event",
                                    true);
                        } catch (Exception var8) {
                            getLogger()
                                    .error(
                                            "cannot set \"report-cdata-event\" property for"
                                                    + " XMLInputFactory",
                                            var8);
                        }
                    }

                    factory.setProperty("javax.xml.stream.isCoalescing", true);
                    factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
                    factory.setProperty("javax.xml.stream.supportDTD", false);
                    XML_INPUT_FACTORY = factory;
                    return factory;
                }
            }
        }
    }

    private JDOMUtil() {}

    private static Logger getLogger() {
        return JDOMUtil.LoggerHolder.ourLogger;
    }

    private static Element loadUsingStaX(Reader reader, SafeJdomFactory factory)
            throws JDOMException, IOException {
        Element var3;
        try {
            XMLStreamReader xmlStreamReader = getXmlInputFactory().createXMLStreamReader(reader);

            try {
                var3 =
                        SafeStAXStreamBuilderWrapper.build(
                                xmlStreamReader,
                                true,
                                true,
                                factory == null ? SafeStAXStreamBuilderWrapper.FACTORY : factory);
            } finally {
                xmlStreamReader.close();
            }
        } catch (XMLStreamException var13) {
            throw new JDOMException(var13.getMessage(), var13);
        } finally {
            reader.close();
        }

        return var3;
    }

    public static Element load(Path file, SafeJdomFactory factory)
            throws JDOMException, IOException {
        try {
            return loadUsingStaX(
                    new InputStreamReader(
                            CharsetToolkit.inputStreamSkippingBOM(
                                    new BufferedInputStream(Files.newInputStream(file))),
                            StandardCharsets.UTF_8),
                    factory);
        } catch (ClosedFileSystemException var3) {
            throw new IOException("Cannot read file from closed file system: " + file, var3);
        }
    }

    public static Element load(InputStream stream) throws JDOMException, IOException {
        return stream == null ? null : load(stream, null);
    }

    public static Element load(InputStream stream, SafeJdomFactory factory)
            throws JDOMException, IOException {
        return loadUsingStaX(new InputStreamReader(stream, StandardCharsets.UTF_8), factory);
    }

    public static void writeElement(Element element, Writer writer, String lineSeparator)
            throws IOException {
        writeElement(element, writer, createOutputter(lineSeparator));
    }

    public static void writeElement(Element element, Writer writer, XMLOutputter xmlOutputter)
            throws IOException {
        try {
            xmlOutputter.output(element, writer);
        } catch (NullPointerException var4) {
            getLogger().error(var4);
            printDiagnostics(element, "");
        }
    }

    public static String writeElement(Element element) {
        return writeElement(element, "\n");
    }

    public static String writeElement(Element element, String lineSeparator) {
        String var10000;
        try {
            StringWriter writer = new StringWriter();
            writeElement(element, writer, lineSeparator);
            var10000 = writer.toString();
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }

        return var10000;
    }

    public static Format createFormat(String lineSeparator) {
        Format var10000 =
                Format.getCompactFormat()
                        .setIndent("  ")
                        .setTextMode(TextMode.TRIM)
                        .setEncoding("UTF-8")
                        .setOmitEncoding(false)
                        .setOmitDeclaration(false)
                        .setLineSeparator(lineSeparator);

        return var10000;
    }

    public static XMLOutputter createOutputter(String lineSeparator) {
        return new JDOMUtil.MyXMLOutputter(createFormat(lineSeparator));
    }

    private static String escapeChar(
            char c, boolean escapeApostrophes, boolean escapeSpaces, boolean escapeLineEnds) {
        switch (c) {
            case '\t':
                return escapeLineEnds ? "&#9;" : null;
            case '\n':
                return escapeLineEnds ? "&#10;" : null;
            case '\r':
                return escapeLineEnds ? "&#13;" : null;
            case ' ':
                return escapeSpaces ? "&#20" : null;
            case '"':
                return "&quot;";
            case '&':
                return "&amp;";
            case '\'':
                return escapeApostrophes ? "&apos;" : null;
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            default:
                return null;
        }
    }

    public static String escapeText(String text, boolean escapeSpaces, boolean escapeLineEnds) {
        return escapeText(text, false, escapeSpaces, escapeLineEnds);
    }

    public static String escapeText(
            String text, boolean escapeApostrophes, boolean escapeSpaces, boolean escapeLineEnds) {
        StringBuilder buffer = null;

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            String quotation = escapeChar(ch, escapeApostrophes, escapeSpaces, escapeLineEnds);
            buffer = XmlStringUtil.appendEscapedSymbol(text, buffer, i, quotation, ch);
        }

        String var10000 = buffer == null ? text : buffer.toString();

        return var10000;
    }

    private static void printDiagnostics(Element element, String prefix) {
        JDOMUtil.ElementInfo info = getElementInfo(element);
        prefix = prefix + "/" + info.name;
        if (info.hasNullAttributes) {
            System.err.println(prefix);
        }

        Iterator var3 = element.getChildren().iterator();

        while (var3.hasNext()) {
            Element child = (Element) var3.next();
            printDiagnostics(child, prefix);
        }
    }

    private static JDOMUtil.ElementInfo getElementInfo(Element element) {
        boolean hasNullAttributes = false;
        StringBuilder buf = new StringBuilder(element.getName());
        List<Attribute> attributes = getAttributes(element);
        int length = attributes.size();
        if (length > 0) {
            buf.append("[");

            for (int idx = 0; idx < length; ++idx) {
                Attribute attr = attributes.get(idx);
                if (idx != 0) {
                    buf.append(";");
                }

                buf.append(attr.getName());
                buf.append("=");
                buf.append(attr.getValue());
                if (attr.getValue() == null) {
                    hasNullAttributes = true;
                }
            }

            buf.append("]");
        }

        return new JDOMUtil.ElementInfo(buf, hasNullAttributes);
    }

    public static boolean isEmpty(Element element) {
        return element == null || !element.hasAttributes() && element.getContent().isEmpty();
    }

    public static List<Attribute> getAttributes(Element e) {

        return e.hasAttributes() ? e.getAttributes() : Collections.emptyList();
    }

    private static class ElementInfo {

        final CharSequence name;
        final boolean hasNullAttributes;

        private ElementInfo(CharSequence name, boolean attributes) {
            this.name = name;
            this.hasNullAttributes = attributes;
        }
    }

    private static final class MyXMLOutputter extends XMLOutputter {
        MyXMLOutputter(Format format) {
            super(format);
        }

        public String escapeAttributeEntities(String str) {
            String var10000 = JDOMUtil.escapeText(str, false, true);

            return var10000;
        }

        public String escapeElementEntities(String str) {
            String var10000 = JDOMUtil.escapeText(str, false, false);

            return var10000;
        }
    }

    private static final class EmptyTextFilter implements Filter<Content> {
        private EmptyTextFilter() {}

        public boolean matches(Object obj) {
            return !(obj instanceof Text)
                    || !CharArrayUtil.containsOnlyWhiteSpaces(((Text) obj).getText());
        }
    }

    private static class LoggerHolder {
        private static final Logger ourLogger = Logger.getInstance(JDOMUtil.class);
    }
}
