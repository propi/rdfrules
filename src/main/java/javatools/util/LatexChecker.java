package javatools.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.FileSet;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga)

This utility checks which files are referenced by a Latex File.

******* Note that we cannot guarantee that this tool finds all references!! *********

@author Fabian M. Suchanek
*/
public class LatexChecker {

  /** Include statements*/
  public static final Set<String> includeStatements=new FinalSet<String>("\\include","\\input","\\includegraphics","\\documentclass","\\bibliography","\\bibliographystyle");

  /** Extensions to consider*/
  public static final String[] extensions=new String[]{"tex","cls","bib","pdf","eps","jpg","bst","png"};

  /** Returns all files referenced in this latex file*/
  public static Set<String> references(File latexFile) throws IOException {
    Set<String> result=new HashSet<String>();
    for(String line : new FileLines(latexFile)) {
      if(line.trim().startsWith("%")) continue;
      for(String stat : includeStatements) {
        for(int i=line.indexOf(stat);i!=-1;i=line.indexOf(stat,i+1)) {
          int j=line.indexOf('{',i);
          if(j==-1 ||  Character.isLetter(line.charAt(i+stat.length()))) continue;
          int k=line.indexOf('}',i);
          if(k==-1 || j+1==k) continue;
          result.add(line.substring(j+1,k));
        }
      }
    }
    return(result);
  }
  
  /** Returns all referenced files, recursively*/
  public static Set<File> referencedBy(File latexFile) throws IOException {
    Announce.doing("Analyzing",latexFile);
    Set<File> result=new HashSet<File>();
    result.add(latexFile);
    for(String filename : references(latexFile)) {
      File f=new File(latexFile.getParent()+'/'+filename);
      for(String extension : extensions) {
        f=FileSet.newExtension(f, extension);
        if(f.exists()) break;
      }
      if(!f.exists()) {
        Announce.warning("**** File not found:",filename);
        continue;
      }
      result.add(f);
      if(FileSet.extension(f).equals(".tex")) result.addAll(referencedBy(f));
    }
    Announce.done();
    return(result);
  }
  
  /** returns all superfluous files. WITHOUT WARRANTY*/
  public static Set<File> nonReferenced(Set<File> otherFiles) {
    Set<File> folders=new HashSet<File>();
    for(File f : otherFiles) folders.add(f.getParentFile());
    Set<File> result=new HashSet<File>();
    for(File folder : folders) {
      for(File f : folder.listFiles()) {
        if(!otherFiles.contains(f)) result.add(f);
      }
    }
    return(result);
  }
  
  /** returns all referenced and all superfluous files of a given latex file*/
  public static void main(String[] args) throws Exception {
    args=new String[]{"c:/fabian/conferences/vlds2012_urdf/main.tex"};
    File latexFile=new File(args[0]);
    Set<File> referenced=referencedBy(latexFile);
    Announce.doing("Referenced files");
    for(File f : referenced) {
      Announce.message(f);
    }
    Announce.doneDoing("Non-referenced");
    Announce.warning("There is no warranty that these files are really non-referenced!");
    for(File f : nonReferenced(referenced)) {
      Announce.message("del",f);
    }    
    Announce.done();
  }
}
