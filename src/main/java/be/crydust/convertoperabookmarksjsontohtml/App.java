package be.crydust.convertoperabookmarksjsontohtml;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class App {
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("ERROR 2 arguments expected");
			System.out.println("Usage: java -jar x.jar inputFile outputFile");
			System.out.println("inputFile example: C:\\Users\\kristof\\AppData\\Roaming\\Opera Software\\Opera Stable\\Bookmarks");
			System.out.println("outputFile example: C:\\Users\\kristof\\Desktop\\bookmarks.html");
			return;
		}

		final Path inputFile = Paths.get(args[0]);
		if (!Files.exists(inputFile)) {
			System.out.println("ERROR inputFile doesn't exist");
			return;
		}

		final Path outputFile = Paths.get(args[1]);
		if (Files.exists(outputFile)) {
			System.out.println("ERROR outputFile already exist");
			return;
		}

		try (final BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8);
			 final JsonReader rdr = Json.createReader(new BufferedInputStream(Files.newInputStream(inputFile)))) {

			writer.write("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n");
			writer.write("<!-- This is an automatically generated file.\n");
			writer.write("     It will be read and overwritten.\n");
			writer.write("     DO NOT EDIT! -->\n");
			writer.write("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
			writer.write("<TITLE>Bookmarks</TITLE>\n");
			writer.write("<H1>Bookmarks</H1>\n");
			writer.write("<DL><p>\n");

			final JsonObject roots = rdr.readObject().getJsonObject("roots");
			walkFolder(1, roots.getJsonObject("bookmark_bar"), writer);
			walkFolder(1, roots.getJsonObject("custom_root").getJsonObject("speedDial"), writer);
			walkFolder(1, roots.getJsonObject("other"), writer);

			writer.write("</DL><p>\n");
		}

		System.out.println("SUCCESS written to " + outputFile);
	}

	private static void walkFolder(int depth, JsonObject folder, Writer writer) throws IOException {
		writeIndent(depth, writer);
		writer.write("<DT><H3>");
		writer.write(escapeHtml4(folder.getString("name")));
		writer.write("</H3>\n");
		writeIndent(depth, writer);
		writer.write("<DL><p>\n");
		for (JsonValue child : folder.getJsonArray("children")) {
			final JsonObject urlOrFolder = child.asJsonObject();
			final String type = urlOrFolder.getString("type");
			switch (type) {
				case "folder":
					walkFolder(depth + 1, urlOrFolder, writer);
					break;
				case "url":
					writeIndent(depth + 1, writer);
					writer.write("<DT><A HREF=\"");
					writer.write(escapeHtml4(urlOrFolder.getString("url")));
					writer.write("\">");
					writer.write(escapeHtml4(urlOrFolder.getString("name")));
					writer.write("</A>\n");
					break;
				default:
					throw new RuntimeException("unknown type " + type);
			}
		}
		writeIndent(depth, writer);
		writer.write("</DL><p>\n");
	}

	private static void writeIndent(int depth, Writer writer) throws IOException {
		for (int i = 0; i < depth; i++) {
			writer.write("    ");
		}
	}
}
