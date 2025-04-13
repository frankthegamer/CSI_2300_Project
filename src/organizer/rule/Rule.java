package organizer.rule;

import java.nio.file.Path;
import org.json.JSONObject;

public interface Rule {

    boolean matches(Path file);  // check if the rule matches a file
    boolean equals(Object obj);  // check if two rules are equal
    int hashCode();             // generate a hash code for the rule

    JSONObject toJSON();
    static Rule fromJSON(JSONObject json){
        String type = json.getString("type");
        return switch (type){
            case "FileCategoryRule" -> new FileCategoryRule(json);
            case "FileExtensionRule" -> new FileExtensionRule(json);
            case "LastAccessedRule" -> new LastModifiedRule(json);
            case "NameHasRule" -> new NameHasRule(json);
            case "StringContainedRule" -> new StringContainedRule(json);
            default -> throw new IllegalArgumentException("Unknown rule type: " + type);
        };
    }

    @Override
    String toString();
}



