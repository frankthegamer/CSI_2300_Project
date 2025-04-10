package organizer.rule;

import java.nio.file.Path;
import org.json.JSONObject;

public interface Rule {

    boolean matches(Path file);
    boolean equals(Object obj);
    int hashCode();

    JSONObject toJSON();
    static Rule fromJSON(JSONObject json){
        String type = json.getString("type");
        return switch (type){
            case "FileCategoryRule" -> new FileCategoryRule(json);
            case "FileExtensionRule" -> new FileExtensionRule(json);
            case "LastAccessedRule" -> new LastAccessedRule(json);
            case "NameHasRule" -> new NameHasRule(json);
            case "StringContainedRule" -> new StringContainedRule(json);
            default -> throw new IllegalArgumentException("Unknown rule type: " + type);
        };
    }

    @Override
    String toString();
}



