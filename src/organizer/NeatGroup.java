package organizer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import organizer.rule.Rule;

public class NeatGroup {
    private Set<Rule> rules;
    private Set<Path> watchDirectories;
    private Path targetDirectory;

    public NeatGroup(Set<Path> watchDirectories, Path targetDirectory){
        this.rules = new HashSet<>();
        this.watchDirectories = new HashSet<>(watchDirectories);
        this.targetDirectory = targetDirectory;
    }

    public void addRule(Rule rule){
        rules.add(rule);  
    }

    public void removeRule(Rule rule){
        rules.remove(rule);
    }

    public void addWatchDirectory(Path directory){
        watchDirectories.add(directory);
    }

    public boolean matches(Path file){
        return rules.stream().allMatch(rule -> rule.matches(file));
    }

    public Set<Rule> getRules(){
        return new HashSet<>(rules);
    }

    public Set<Path> getWatchDirectories(){
        return new HashSet<>(watchDirectories);
    }

    public Path getTargetDirectory(){
        return targetDirectory;
    }

    public void setWatchDirectories(Set<Path> watchDirectories) {
        this.watchDirectories = new HashSet<>(watchDirectories);
    }

    // Add setter for targetDirectory
    public void setTargetDirectory(Path targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @Override
    public boolean equals(Object obj){    // ensures distinct group rules, watch directories or target directory
        if(this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NeatGroup other = (NeatGroup) obj;

        return rules.equals(other.rules)
               && watchDirectories.equals(other.watchDirectories) 
               && (targetDirectory == null ? other.targetDirectory == null : targetDirectory.equals(other.targetDirectory));
    }

    @Override
    public int hashCode(){
        int result = rules.hashCode();
        result = 31 * result + watchDirectories.hashCode();
        result = 31 * result + (targetDirectory != null ? targetDirectory.hashCode() : 0);
        return result;
    }
}


