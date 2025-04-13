package organizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.*;

public class NeatFileLogic {
    private final Object fileProcessingLock = new Object();  
    private final Set<NeatGroup> groups = Collections.synchronizedSet(new HashSet<>()); 

    private NeatFileApp app;

    public void setApp(NeatFileApp app) { // allows access to app for status updates
        this.app = app;
    }


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
        if (groups.contains(group)) {
            if (app != null) {
                    Stage stage = (Stage) app.getPrimaryStage();
                    app.finalizeStatus(stage, "Duplicate group not added!");
            }
            System.out.println("Error: A group with the same rules, watch directories, and target already exists!");
            return false;
        }

    
        groups.add(group);
        return true;
    }
    
    public void processFile(Path file){   // processes file according to rules of groups it belongs to
        synchronized(fileProcessingLock){    // ensures files are processed one at a time

        System.out.println("Processing file: " + file);
    
        List<NeatGroup> eligibleGroups = new ArrayList<>(); 
        for(NeatGroup group : groups) {    // check if file is in watch directories + satisfies all group criteria
            boolean inWatchDir = group.getWatchDirectories().stream().anyMatch(watchDir -> file.startsWith(watchDir));
            if (inWatchDir && group.matches(file)){
                eligibleGroups.add(group);     
            }
        }
        if (eligibleGroups.isEmpty()){  // no match
            System.out.println("No matching group for: " + file);
            return;
        }
        if (eligibleGroups.size() > 1 ) {  // multiple matching groups
            Path target = eligibleGroups.get(0).getTargetDirectory();
            boolean conflict = eligibleGroups.stream().anyMatch(g -> !g.getTargetDirectory().equals(target));
            if (conflict) {
                System.out.println("Conflict: File "+ file + " matches multiple groups with different targets: " + 
                    eligibleGroups.stream()
                        .map(g -> g.getTargetDirectory().toString())
                        .collect(Collectors.joining(", ")));
                return; // then don't move file
            }    
        }

// moves file to target of first matching group                          
        NeatGroup group = eligibleGroups.get(0);
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
            Files.createDirectories(targetDir);  // ensure directory exists

            String fileName = file.getFileName().toString(); 
            String baseName = fileName;
            String extension = "";

            int dotIndex = fileName.lastIndexOf('.');

            if(dotIndex > 0 && dotIndex < fileName.length() - 1){
                baseName = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex); // includes the dot
            }

            Path uniqueTargetFile = targetDir.resolve(fileName);
            int counter = 1;
            while (Files.exists(uniqueTargetFile)) {
                uniqueTargetFile = targetDir.resolve(baseName + "_" + counter + extension);
                counter++;
            }

            Files.move(file, uniqueTargetFile);
            System.out.println("Moved " + file + " to " + uniqueTargetFile);
            
        } catch (IOException e) {
            System.out.println("Failed to move " + file + " to " + targetFile + ": " + e.getMessage());
            
        }

        }
    }

    public void clearGroups() { 
        groups.clear();
    }
}


    