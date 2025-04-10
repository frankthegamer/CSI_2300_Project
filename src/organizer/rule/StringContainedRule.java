package organizer.rule;

import java.io.FileInputStream;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;



public class StringContainedRule implements Rule{
    
    private static final long MAX_FILE_SIZE = 100_000_000L; // change this to look for strings in files bigger than 100MB

    private String input;
    private boolean caseSensitive;
    private boolean useRegex;

    public StringContainedRule(String input, boolean caseSensitive, boolean useRegex){
        this.input = input;
        this.caseSensitive = caseSensitive;
        this.useRegex = useRegex;
    }
    

    public StringContainedRule(JSONObject json){
        this.input = json.getString("substring");
        this.caseSensitive = json.optBoolean("caseSensitive", false);
        this.useRegex = json.optBoolean("useRegex", false);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "StringContainedRule");
        json.put("substring", input);
        json.put("caseSensitive", caseSensitive);
        json.put("useRegex", useRegex);
        return json;
    }

    @Override
    public boolean matches(Path file){
        if (file.toFile().length() > MAX_FILE_SIZE) {
            System.out.println("(Skipped) File too large: " + file.getFileName());
            return false;
        }
        try {
            String text = extractText(file);
            if (text == null) {
                return false;
            }
            if (useRegex) {
                Pattern pattern = caseSensitive ? Pattern.compile(input) : Pattern.compile(input, Pattern.CASE_INSENSITIVE);
                return pattern.matcher(text).find();
            }
            String compareText = caseSensitive ? text : text.toLowerCase();
            String compareInput = caseSensitive ? input : input.toLowerCase();
            return compareText.contains(compareInput);
         } catch (IOException e) {
            System.err.println("Error processing file" + file + ": " + e.getMessage());
            return false;
        }
    }

    private String extractText(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase();
    
        try {
            if (name.endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(file.toFile());
                     XWPFDocument doc = new XWPFDocument(fis);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
    
            if (name.endsWith(".txt")) {
                return Files.readString(file);
            }
    
            if (name.endsWith(".pdf")) {
                try (PDDocument doc = PDDocument.load(file.toFile())) {
                    return new PDFTextStripper().getText(doc);
                }
            }
    
            if (name.endsWith(".xlsx")) {
                try (FileInputStream fis = new FileInputStream(file.toFile());
                     XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                    StringBuilder text = new StringBuilder();
                    workbook.forEach(sheet -> sheet.forEach(row -> row.forEach(cell -> {
                        switch (cell.getCellType()) {
                            case STRING -> text.append(cell.getStringCellValue()).append(" ");
                            case NUMERIC -> text.append(cell.getNumericCellValue()).append(" ");
                            default -> {}
                        }
                    })));
                    return text.toString();
                }
            }
    
            if (name.endsWith(".pptx")) {
                try (FileInputStream fis = new FileInputStream(file.toFile());
                     XMLSlideShow ppt = new XMLSlideShow(fis)) {
                    StringBuilder text = new StringBuilder();
                    ppt.getSlides().forEach(slide -> slide.getShapes().forEach(shape -> {
                        if (shape instanceof XSLFTextShape textShape) {
                            text.append(textShape.getText()).append(" ");
                        }
                    }));
                    return text.toString();
                }
            }
    
            System.out.println("Unsupported file type: " + name);
        } catch (Exception e) {
            System.err.println("Error extracting from " + name + ": " + e.getMessage());
        }
    
        return "";
    }
    


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringContainedRule other) {
            return input.equals(other.input) && caseSensitive == other.caseSensitive && useRegex == other.useRegex;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return input.hashCode() * 31 + Boolean.hashCode(caseSensitive) * 17 + Boolean.hashCode(useRegex);
    }

    @Override
    public String toString() {
        return "Text in file: " + input + " (Case Sensitive: " + caseSensitive + ", Regex: " + useRegex + ")";
    }
}