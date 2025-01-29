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

/* I have added class below, approch is first convert json data into java object, then we can use data,
 * then we can tagged a pdf documennt and will do a proper tag hirerchy, JSON bounding box coordinates are converted to PDF points.
Invisible overlay text is added at the exact positions for accurate tagging without altering the visual appearance, the design style also given, i can use that
author sarthak bora
 */

public class AddAccessibilityTags {

	public static void main(String[] args) throws IOException {
		/* I have added file paths, because adding file take long time */
		String inputPdf = "C:/File/exercise2.pdf";
		String outputPdf = "output_tagged.pdf";
		String jsonMappingFile = "C:/File/Exercise_JSON.json";

		/*
		 * converting Json data into Java Object, i am using gson,can do this with
		 * objectmapper also
		 */
		JsonObject jsonMapping = loadJsonMapping(jsonMappingFile);

		/* opening pdf and added pdf writer also */

		PdfReader reader = new PdfReader(inputPdf);
		PdfWriter writer = new PdfWriter(outputPdf);
		PdfDocument pdfDoc = new PdfDocument(reader, writer);

		/* Enable tagging & metadata */
		pdfDoc.setTagged();
		PdfCatalog catalog = pdfDoc.getCatalog();
		catalog.setLang(new PdfString(jsonMapping.get("Document").getAsJsonObject().get("Language").getAsString()));
		PdfDocumentInfo info = pdfDoc.getDocumentInfo();
		info.setTitle(jsonMapping.get("Document").getAsJsonObject().get("Title").getAsString());

		/* Get the struct tree root */
		PdfStructTreeRoot structTreeRoot = pdfDoc.getStructTreeRoot();

		/* Create a root element one by one, linking it to the structTreeRoot */
		PdfStructElem rootElement = new PdfStructElem(pdfDoc, PdfName.Div); // Using PdfDocument here

		/* proceess the page here there is only one page */
		JsonArray pages = jsonMapping.get("Document").getAsJsonObject().getAsJsonArray("Pages");
		for (int i = 0; i < pages.size(); i++) {
			JsonObject pageObject = pages.get(i).getAsJsonObject();
			PdfPage page = pdfDoc.getPage(i + 1);

			/* Correctly create a section under the root element ,using PdfDocument here */
			PdfStructElem sectElement = new PdfStructElem(pdfDoc, PdfName.Sect);

			JsonArray tagObjects = pageObject.getAsJsonArray("TagObjects");
			for (JsonElement contentElement : tagObjects) {
				JsonObject contentObject = contentElement.getAsJsonObject();
				/* passing document here */
				addContentTag(pdfDoc, page, sectElement, contentObject, pageObject);
			}
		}

		/* Close document */
		pdfDoc.close();
		System.out.println("Accessibility tags added successfully to " + outputPdf);
	}

	private static JsonObject loadJsonMapping(String jsonMappingFile) throws IOException {
		Gson gson = new Gson();
		try (Reader reader = new FileReader(jsonMappingFile)) {
			return gson.fromJson(reader, JsonObject.class);
		}
	}

	private static void addContentTag(PdfDocument pdfDoc, PdfPage page, PdfStructElem parentElement,
			JsonObject contentObject, JsonObject pageObject) {
		String tagType = contentObject.get("tag").getAsString();
		JsonObject bbox = contentObject.has("bBox") ? contentObject.getAsJsonObject("bBox") : new JsonObject();
		float x = bbox.get("Left").getAsFloat() * pageObject.get("PageWidth").getAsFloat();
		float y = (1 - bbox.get("Top").getAsFloat()) * pageObject.get("PageHeight").getAsFloat();
		float width = bbox.get("Width").getAsFloat() * pageObject.get("PageWidth").getAsFloat();
		float height = bbox.get("Height").getAsFloat() * pageObject.get("PageHeight").getAsFloat();

		/* Passing PdfDocument as the first argument */
		PdfStructElem contentElement = new PdfStructElem(pdfDoc, new PdfName(tagType)); // ✅ Using PdfDocument here

		/* Add the new content element as a child of the parent */
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

			break;

		default:
			System.out.println("Unsupported tag type: " + tagType);
			break;
		}
	}
}
