package com.example.NetraLabTest.manipulation;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.tagging.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.google.gson.*;
import java.io.*;

/*
 * Adding AccessibilityTags to a PDF document based on JSON DATA
 * 
 * autor sarthak bora
 * ***/
public class AddAccessibilityTags {

	public static void main(String[] args) {
		/* I have added file path here and created output_sample.pdf file */
		String inputPdf = "C:/File/Exercise.pdf";
		String outputPdf = "output_sample.pdf";
		String jsonMappingFile = "C:/File/Exercise_JSON.json";

		try {
			/*
			 * First I am loading Json data that mean converting Json Data into java object
			 */
			JsonElement jsonMappingElement = loadJsonMapping(jsonMappingFile);
			JsonObject jsonMapping;

			/*
			 * In our json file json data is presnet in array format that's why i have added
			 * below check
			 */

			if (jsonMappingElement.isJsonObject()) {
				jsonMapping = jsonMappingElement.getAsJsonObject();
			} else if (jsonMappingElement.isJsonArray()) {
				JsonArray jsonArray = jsonMappingElement.getAsJsonArray();
				if (jsonArray.size() > 0 && jsonArray.get(0).isJsonObject()) {
					jsonMapping = jsonArray.get(0).getAsJsonObject(); // Extract first object
				} else {
					throw new JsonSyntaxException("Error: JSON root is an empty array or does not contain an object.");
				}
			} else {
				throw new JsonSyntaxException("Unexpected JSON format. Expected Object or Array.");
			}

			/*
			 * After extracting json data from array , i am checking whether first node is
			 * document or not
			 */
			if (!jsonMapping.has("Document") || !jsonMapping.get("Document").isJsonObject()) {
				throw new JsonSyntaxException("Error: JSON does not contain a 'Document' object.");
			}

			JsonObject documentObject = jsonMapping.getAsJsonObject("Document");

			/* opening pdf and writing in pdf */
			PdfReader reader = new PdfReader(inputPdf);
			PdfWriter writer = new PdfWriter(outputPdf);
			PdfDocument pdfDoc = new PdfDocument(reader, writer);

			/* Enable tagging and metadata */
			pdfDoc.setTagged();
			PdfCatalog catalog = pdfDoc.getCatalog();
			catalog.setLang(new PdfString(documentObject.get("Language").getAsString()));

			PdfDocumentInfo info = pdfDoc.getDocumentInfo();
			info.setTitle(documentObject.get("Title").getAsString());

			/* Correct Structure Root */
			PdfStructTreeRoot structTreeRoot = pdfDoc.getStructTreeRoot();
			PdfStructElem rootElement = new PdfStructElem(pdfDoc, PdfName.Document);
			structTreeRoot.addKid(rootElement); // Add root element to structure tree

			JsonArray pages = documentObject.getAsJsonArray("Pages");

			/*
			 * Check if the PDF has enough pages int pdfPageCount =
			 * pdfDoc.getNumberOfPages(); if (pages.size() > pdfPageCount) { throw new
			 * IndexOutOfBoundsException("JSON contains data for " + pages.size() +
			 * " pages, but the PDF has only " + pdfPageCount + " pages."); }
			 */

			JsonObject pageObject = pages.get(1).getAsJsonObject();
			PdfPage page = pdfDoc.getPage(1); // Pages are 1-indexed

			/* Use rootElement as the parent for section elements */
			PdfStructElem sectElement = new PdfStructElem(pdfDoc, PdfName.Sect);
			rootElement.addKid(sectElement); // Add sectElement to rootElement

			JsonArray tagObjects = pageObject.getAsJsonArray("TagObjects");
			for (JsonElement contentElement : tagObjects) {
				JsonObject contentObject = contentElement.getAsJsonObject();
				addContentTag(pdfDoc, page, sectElement, contentObject, pageObject);
			}

			/* Close document */
			pdfDoc.close();
			System.out.println("Accessibility tags added successfully to " + outputPdf);

		} catch (IOException | JsonSyntaxException e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static JsonElement loadJsonMapping(String jsonMappingFile) throws IOException {
		Gson gson = new Gson();
		try (Reader reader = new FileReader(jsonMappingFile)) {
			return gson.fromJson(reader, JsonElement.class);
		}
	}

	private static void addContentTag(PdfDocument pdfDoc, PdfPage page, PdfStructElem parentElement,
			JsonObject contentObject, JsonObject pageObject) {
		String tagType = contentObject.get("tag").getAsString();
		JsonObject bbox = contentObject.has("bBox") ? contentObject.getAsJsonObject("bBox") : new JsonObject();

		float x = getJsonFloat(bbox, "Left", 0) * getJsonFloat(pageObject, "PageWidth", 1);
		float y = (1 - getJsonFloat(bbox, "Top", 0)) * getJsonFloat(pageObject, "PageHeight", 1);
		float width = getJsonFloat(bbox, "Width", 0) * getJsonFloat(pageObject, "PageWidth", 1);
		float height = getJsonFloat(bbox, "Height", 0) * getJsonFloat(pageObject, "PageHeight", 1);

		/* Create content element */
		PdfStructElem contentElement = new PdfStructElem(pdfDoc, new PdfName(tagType));
		parentElement.addKid(contentElement); // Add contentElement to parentElement

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
					paragraph.setOpacity(1.0f); // Make text visible
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

	private static float getJsonFloat(JsonObject obj, String key, float defaultValue) {
		return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsFloat() : defaultValue;
	}
}