package organizer.rule;

import java.nio.file.Path;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class NameHasRule implements Rule{
    private String input;
    private boolean caseSensitive;
    private boolean useRegex;

    public NameHasRule(String input, boolean caseSensitive, boolean useRegex){
        this.input = input;
        this.caseSensitive = caseSensitive;
        this.useRegex = useRegex;
    }

    public NameHasRule(JSONObject json) {
        this.input = json.getString("substring");
        this.caseSensitive = json.optBoolean("caseSensitive", false);
        this.useRegex = json.optBoolean("useRegex", false);

        
    }

    @Override
    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        json.put("type", "NameHasRule");
        json.put("substring", input);
        json.put("caseSensitive", caseSensitive);
        json.put("useRegex", useRegex);
        return json;
    }

    @Override
    public boolean matches(Path file){
        String fileName = file.getFileName().toString();
        if(useRegex){
            Pattern pattern = caseSensitive ? Pattern.compile(input) : Pattern.compile(input, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(fileName).find();
        }
        String compareName = caseSensitive ? fileName : fileName.toLowerCase();
        String compareInput = caseSensitive ? input : input.toLowerCase();
        return compareName.contains(compareInput);
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof NameHasRule other){
            return input.equals(other.input) && caseSensitive == other.caseSensitive && useRegex == other.useRegex;
        }
        return false;
        }
    

    @Override
    public int hashCode(){
        return input.hashCode() * 31 + Boolean.hashCode(caseSensitive) * 17 + Boolean.hashCode(useRegex);

    }

    @Override
    public String toString(){
        return "File Name: " + input + " (Case Sensitive: " + caseSensitive + ", Regex: " + useRegex + ")";
    }

}
    
