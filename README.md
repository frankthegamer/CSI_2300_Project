# NeatFile - Smart Desktop Organizer

NeatFile is a JavaFX desktop application that helps you automatically organize files from specified folders into destination directories based on custom rules. It's a user-friendly, flexible, and visually interactive tool for file management.

---

## Features

- **Group-based organization**
  - Each group has its own watch directories, rules, and target directory.
- **Flexible rule types**
  - Organize files based on:
    - File extension
    - File name content
    - File content (text within files)
    - File category (image, document, audio, video)
    - Last modified date
- **Simple UI**
  - Add watch folders and rules with buttons.
  - Click to remove them
  
- **Conflict detection**
  - Prevents duplicate or conflicting groups.
- **Manual scanning**
  - Automatically scans every 5 seconds for new files.

---

## How to Use

### 1. Launching the App
-- Run the app using the provided `.bat` file (recommended) or from the terminal with JavaFX and dependencies configured.
- The main window shows **Watch Paths**, **Rules**, and **Target Path** sections.

### 2. Creating a Group
- Click the "+" button near the dropdown to create a new group.
- The app will auto-select the new group.
- If already created, the first group will be selected on startup

### 3. Adding Watch Paths
- Click the small "+" button under the Watch Paths section.
- Choose a folder to watch. You can add multiple.

### 4. Setting a Target Directory
- Click the folder icon under "Target Path".
- Select the folder where matching files should be moved.
- The label updates to show the folder name, with a tooltip for the full path.

### 5. Adding Rules
- Choose from several rule buttons:
  - **Extension Rule:** Move files with certain extensions.
  - **String Rule:** Move files containing specific text.
  - **Category Rule:** Move based on general file type.
  - **Name Rule:** Match keywords in file names.
  - **Last Modified Rule:** Move files modified more than a number of days.

### 6. Finalizing
- Click the **"Finalize"** button at the bottom.
- This saves your groups and rules to `groups.json`.
- A small popup will confirm that your changes were saved.

---

## Important Notes

- Conflicting groups (with same watch dirs, rules and different target) are not saved to rules.json after finalizing; they will have to be removed first.
- If a file with the same name already exists in the target folder, the new one is renamed.

---

## Requirements

- Java 17+
- JavaFX SDK (make sure it's correctly added to your project)
- JSON (org.json) library

---

## Project Structure
```
NeatFile/
├── src/
│   ├── organizer/
│   │   ├── NeatFileApp.java
│   │   ├── NeatFileLogic.java
│   │   ├── NeatGroup.java
│   │   ├── style.css
│   │   ├── folder_icon.png  
│   ├── organizer/rule/
│   │   ├── Rule.java
│   │   ├── FileExtensionRule.java
│   │   ├── StringContainedRule.java
│   │   ├── FileCategoryRule.java
│   │   ├── NameHasRule.java
│   │   ├── LastModifiedRule.java 
├── groups.json (auto-generated)
```

---

## Credits
Created by Frank Richter as a final project for CSI 2300

---

