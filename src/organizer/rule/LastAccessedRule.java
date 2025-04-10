package organizer.rule;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.io.IOException;
import org.json.JSONObject;



public class LastAccessedRule implements Rule{
    private long days;

    public LastAccessedRule(long days){
        this.days = days;
    }

    public LastAccessedRule(JSONObject json){
        this.days = json.getLong("days");
    }

    @Override
    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        json.put("type","LastAccessedRule");
        json.put("days", days);
        return json;
    }

    @Override
    public boolean matches(Path file){
        try {
            FileTime lastAccess = (FileTime) Files.getAttribute(file, "lastAccessTime");
            long thresholdTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000);
            return lastAccess.toMillis() < thresholdTime;
        } catch(IOException e) {
            System.err.println("Error accessing file " + file + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof LastAccessedRule other){
            return days == other.days;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Long.hashCode(days);
    }

    @Override
    public String toString(){
        return "Older than " + days + " days";
    }
}

