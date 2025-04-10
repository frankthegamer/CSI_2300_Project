package organizer.rule;

import java.nio.file.Path;
import java.util.Set;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileExtensionRule implements Rule{
    private Set<String> extensions;

    public FileExtensionRule(Set<String> extensions){
        this.extensions = new HashSet<>();
        for(String ext : extensions){          // adds a '.' if not entered
            if (ext.startsWith(".")) {
                this.extensions.add(ext);
            } else {
                this.extensions.add("." + ext);
            }
        }
    }

    public FileExtensionRule(JSONObject json){
        this.extensions = new HashSet<>();
        JSONArray extensionsArray = json.getJSONArray("extensions");
        for(int i = 0; i < extensionsArray.length(); i++){
            extensions.add(extensionsArray.getString(i));
        }
    }

    @Override
    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        json.put("type", "FileExtensionRule");
        JSONArray extensionsArray = new JSONArray();
        for (String ext : extensions){
            extensionsArray.put(ext);
        }
        json.put("extensions", extensionsArray);
        return json;
    }



    @Override
    public boolean matches(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return extensions.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileExtensionRule other) {
            return extensions.equals(other.extensions);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return extensions.hashCode();
    }

    @Override
    public String toString() {
        return "Has Extensions: " + String.join(", ", extensions);
    }

    
}
