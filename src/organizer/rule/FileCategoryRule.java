package organizer.rule;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import org.json.JSONObject;

public class FileCategoryRule implements Rule{
    private static final Map<String, Set<String>> CATEGORIES = new HashMap<>();
    static {
        CATEGORIES.put("Image", Set.of(".jpg",".jpeg", ".png",".gif",".bmp",".svg",".webp",".heic"));
        CATEGORIES.put("Document", Set.of(".txt",".docx",".pdf",".md",".doc",".xlsx",".html",".pptx",".ppt"));
        CATEGORIES.put("Audio", Set.of(".mp3",".wav",".m4a",".aac",".aiff",".flac",".pcm"));
        CATEGORIES.put("Video", Set.of(".mp4",".mov",".wmv",".avi",".mkv",".flv"));
    } 
    
    private String category;
    
    public FileCategoryRule(String category){
        this.category = category;
    }

    public FileCategoryRule(JSONObject json){
        this.category = json.getString("category");
    }

    @Override
    public boolean matches(Path file){
        String fileName = file.getFileName().toString().toLowerCase();
        Set<String> extensions = CATEGORIES.getOrDefault(category, new HashSet<>());
        return extensions.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        json.put("type", "FileCategoryRule");
        json.put("category", category);
        return json;
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof FileCategoryRule other){
            return category.equals(other.category);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return category.hashCode();
    }

    @Override
    public String toString(){
        return "Catergory: " + category;
    }
    
}
