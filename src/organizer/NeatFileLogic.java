package organizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.*;

public class NeatFileLogic {
    private final Object fileProcessingLock = new Object();
    private final Set<NeatGroup> groups = Collections.synchronizedSet(new HashSet<>());

    public boolean addGroup(NeatGroup group){

        // checks for conflicting target directories for same rules and watch directories
        for(NeatGroup existing : groups){
            if(existing.getRules().equals(group.getRules()) &&
            existing.getWatchDirectories().equals(group.getWatchDirectories()) &&
            !existing.getTargetDirectory().equals(group.getTargetDirectory())){
            
            System.out.println("Error: A group with the same rules and watch directories but different target (" + 
                existing.getTargetDirectory() + " vs. " + group.getTargetDirectory() + ") already exists!");
            return false;
            }
        }

       // checks for EXACT duplicate group
        if(groups.contains(group)){
            System.out.println("Error: A group with the same rules, watch directories, and target already exists!");
            return false;
        }
        groups.add(group);
        return true;
    }
    
    public void processFile(Path file){
        synchronized(fileProcessingLock){       // ensures files are processed one at a time

        System.out.println("Processing file: " + file);
    
        List<NeatGroup> matchingGroups = new ArrayList<>(); 
        for(NeatGroup group : groups) {    // check if file is in one of group's watch directories + satisfies all group criteria
            boolean inWatchDir = group.getWatchDirectories().stream().anyMatch(watchDir -> file.startsWith(watchDir));
            if (inWatchDir && group.matches(file)){
                matchingGroups.add(group);     
            }
        }
        if (matchingGroups.isEmpty()){  // no match
            System.out.println("No matching group for: " + file);
            return;
        }
        if (matchingGroups.size() > 1 ) {  // checks if matching groups have same target directory
            Path target = matchingGroups.get(0).getTargetDirectory();
            boolean conflict = matchingGroups.stream().anyMatch(g -> !g.getTargetDirectory().equals(target));
            if (conflict) {
                System.out.println("Conflict: File "+ file + " matches multiple groups with different targets: " + 
                    matchingGroups.stream()
                        .map(g -> g.getTargetDirectory().toString())
                        .collect(Collectors.joining(", ")));
                return; // then don't move file
            }    
        }

// moves file to target of first matching group                          
        NeatGroup group = matchingGroups.get(0);
        Path targetDir = group.getTargetDirectory();
        Path targetFile = targetDir.resolve(file.getFileName());
        
        // check file already in target directory
        if (file.equals(targetFile)) {
            System.out.println("Skipping move — file already in target location: " + file);
            return;
        }

        if (!Files.exists(file)) {
            System.out.println("File no longer exists: " + file);
            return;
        }

        try {
            Files.createDirectories(targetDir);  // create if dir doesn't exist
            Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Moved " + file + " to " + targetFile);
        } catch (IOException e) {
            System.out.println("Failed to move " + file + " to " + targetFile + ": " + e.getMessage());
        }

        }
    }

    public void clearGroups() {
        groups.clear();
    }
}


    