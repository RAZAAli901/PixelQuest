package com.pixelquest.tools.quran

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** Reading Tanzil's XML files: downloaded data, so no DTDs and no external entities. */
object Xml {
    fun read(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        return try {
            factory.newDocumentBuilder().parse(file)
        } catch (e: Exception) {
            throw QuranBuildException("${file.name} isn't readable XML: ${e.message}")
        }
    }

    /** The child elements of [parent] named [tag], in order. */
    fun children(parent: Element, tag: String): List<Element> {
        val nodes = parent.childNodes
        return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }
    }

    /** [element]'s attribute [name] as a whole number, or a failure naming it. */
    fun int(element: Element, name: String): Int =
        element.getAttribute(name).toIntOrNull() ?: throw QuranBuildException("<${element.tagName}> has no whole number $name")
}
