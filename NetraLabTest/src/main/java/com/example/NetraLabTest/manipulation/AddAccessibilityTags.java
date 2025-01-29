 package com.example.NetraLabTest.manipulation;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.tagging.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.google.gson.*;
import java.io.*;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.tagging.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.google.gson.*;
import java.io.*;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.tagging.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.google.gson.*;
import java.io.*;

public class AddAccessibilityTags {

    public static void main(String[] args) throws IOException {
        // Corrected file paths
        String inputPdf = "C:/File/exercise2.pdf";
        String outputPdf = "output_tagged.pdf";
        String jsonMappingFile = "C:/File/Exercise_JSON.json";

        // Load JSON mapping data
        JsonObject jsonMapping = loadJsonMapping(jsonMappingFile);

        // Open PDF
        PdfReader reader = new PdfReader(inputPdf);
        PdfWriter writer = new PdfWriter(outputPdf);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        // Enable tagging & metadata
        pdfDoc.setTagged();
        PdfCatalog catalog = pdfDoc.getCatalog();
        catalog.setLang(new PdfString(jsonMapping.get("Document").getAsJsonObject().get("Language").getAsString()));
        PdfDocumentInfo info = pdfDoc.getDocumentInfo();
        info.setTitle(jsonMapping.get("Document").getAsJsonObject().get("Title").getAsString());

        // Get the struct tree root
        PdfStructTreeRoot structTreeRoot = pdfDoc.getStructTreeRoot();

        // Create a root element, linking it to the structTreeRoot
        PdfStructElem rootElement = new PdfStructElem(pdfDoc, PdfName.Div); // Using PdfDocument here

        // Process each page
        JsonArray pages = jsonMapping.get("Document").getAsJsonObject().getAsJsonArray("Pages");
        for (int i = 0; i < pages.size(); i++) {
            JsonObject pageObject = pages.get(i).getAsJsonObject();
            PdfPage page = pdfDoc.getPage(i + 1);

            // Correctly create a section under the root element
            PdfStructElem sectElement = new PdfStructElem(pdfDoc, PdfName.Sect); // Using PdfDocument here

            JsonArray tagObjects = pageObject.getAsJsonArray("TagObjects");
            for (JsonElement contentElement : tagObjects) {
                JsonObject contentObject = contentElement.getAsJsonObject();
                addContentTag(pdfDoc, page, sectElement, contentObject, pageObject); // Pass PdfDocument here
            }
        }

        // Close document
        pdfDoc.close();
        System.out.println("Accessibility tags added successfully to " + outputPdf);
    }

    private static JsonObject loadJsonMapping(String jsonMappingFile) throws IOException {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(jsonMappingFile)) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    private static void addContentTag(PdfDocument pdfDoc, PdfPage page, PdfStructElem parentElement, JsonObject contentObject, JsonObject pageObject) {
        String tagType = contentObject.get("tag").getAsString();
        JsonObject bbox = contentObject.has("bBox") ? contentObject.getAsJsonObject("bBox") : new JsonObject();
        float x = bbox.get("Left").getAsFloat() * pageObject.get("PageWidth").getAsFloat();
        float y = (1 - bbox.get("Top").getAsFloat()) * pageObject.get("PageHeight").getAsFloat();
        float width = bbox.get("Width").getAsFloat() * pageObject.get("PageWidth").getAsFloat();
        float height = bbox.get("Height").getAsFloat() * pageObject.get("PageHeight").getAsFloat();

        // Correct constructor usage: Passing PdfDocument as the first argument
        PdfStructElem contentElement = new PdfStructElem(pdfDoc, new PdfName(tagType)); // ✅ Using PdfDocument here

        // Add the new content element as a child of the parent
        parentElement.addKid(contentElement);

        switch (tagType) {
            case "P":
            case "H1":
            case "H2":
            case "H3":
            case "H4":
            case "H5":
            case "H6":
                if (contentObject.has("lines")) {
                    JsonArray lines = contentObject.getAsJsonArray("lines");
                    for (JsonElement lineElement : lines) {
                        JsonObject lineObject = lineElement.getAsJsonObject();
                        String text = lineObject.get("text").getAsString();
                        Paragraph paragraph = new Paragraph(text);
                        paragraph.setFixedPosition(x, y - height, width);
                        paragraph.setOpacity(0.0f); // Invisible overlay
                        new Canvas(page, page.getPageSize()).add(paragraph);
                    }
                }
                break;

            case "Table":
                // Handle tables here
                break;

            case "Figure":
                // Handle images here
                break;

            case "LI":
                // Handle lists here
                break;

            default:
                System.out.println("Unsupported tag type: " + tagType);
                break;
        }
    }
}


