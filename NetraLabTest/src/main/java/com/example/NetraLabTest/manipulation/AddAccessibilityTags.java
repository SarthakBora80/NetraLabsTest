
import com.itextpdf.kernel.pdf.tagutils.*;
import com.itextpdf.kernel.pdf.PdfCatalog;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.listener.*;
import com.itextpdf.kernel.pdf.tagging.PdfStructTreeRoot;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import com.itextpdf.layout.element.Paragraph;

/*
 *  I have created Addaccessibility Tag class and there i have added three file which i got through email
 *  i have dowloaded that and given local folder path
 * 
 *   author sarthak bora
 * */

public class AddAccessibilityTags {

    public static void main(String[] args) throws IOException {
        // Input PDF file path
        String inputPdf = "C:\\File\\exercise2.pdf";
        // Output PDF file path
        String outputPdf = "output_tagged.pdf";
        // JSON mapping file path
        String jsonMappingFile = "C:\\File\\Exercise_JSON.json";

        // Load the JSON mapping data
        JsonObject jsonMapping = loadJsonMapping(jsonMappingFile);

        // Open the existing PDF
        PdfReader reader = new PdfReader(inputPdf);
        PdfWriter writer = new PdfWriter(outputPdf);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);

        // Enable tagging and set PDF/UA compliance metadata
        pdfDoc.setTagged();
        PdfCatalog catalog = pdfDoc.getCatalog();
        catalog.setLang(new PdfString(jsonMapping.get("Document").getAsJsonObject().get("Language").getAsString()));
        PdfDocumentInfo info = pdfDoc.getDocumentInfo();
        info.setTitle(jsonMapping.get("Document").getAsJsonObject().get("Title").getAsString());

        // Create the root structure element
        PdfStructTreeRoot structTreeRoot = catalog.getStructTreeRoot();
        PdfStructureElement rootElement = new PdfStructureElement(structTreeRoot, PdfName.Document);

        // Process each page and add tags
        JsonArray pages = jsonMapping.get("Document").getAsJsonObject().getAsJsonArray("Pages");
        for (int i = 0; i < pages.size(); i++) {
            JsonObject pageObject = pages.get(i).getAsJsonObject();
            PdfPage page = pdfDoc.getPage(i + 1);
            PdfStructureElement sectElement = new PdfStructureElement(rootElement, PdfName.Sect);

            JsonArray tagObjects = pageObject.getAsJsonArray("TagObjects");
            for (JsonElement contentElement : tagObjects) {
                JsonObject contentObject = contentElement.getAsJsonObject();
                addContentTag(page, sectElement, contentObject, pageObject);
            }
        }

        // Close the PDF document
        pdfDoc.close();
        System.out.println("Accessibility tags added successfully to " + outputPdf);
    }

    private static JsonObject loadJsonMapping(String jsonMappingFile) throws IOException {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(jsonMappingFile)) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    private static void addContentTag(PdfPage page, PdfStructureElement parentElement, JsonObject contentObject, JsonObject pageObject) {
        String tagType = contentObject.get("tag").getAsString();
        JsonObject bbox = contentObject.has("bBox") ? contentObject.getAsJsonObject("bBox") : new JsonObject();
        float x = bbox.get("Left").getAsFloat() * pageObject.get("PageWidth").getAsFloat();
        float y = (1 - bbox.get("Top").getAsFloat()) * pageObject.get("PageHeight").getAsFloat();
        float width = bbox.get("Width").getAsFloat() * pageObject.get("PageWidth").getAsFloat();
        float height = bbox.get("Height").getAsFloat() * pageObject.get("PageHeight").getAsFloat();

        PdfStructureElement contentElement = new PdfStructureElement(parentElement, new PdfName(tagType));

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

                break;

            default:
                System.out.println("Unsupported tag type: " + tagType);
                break;
        }
    }
}