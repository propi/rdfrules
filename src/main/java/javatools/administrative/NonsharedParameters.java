package javatools.administrative;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javatools.database.Database;
import javatools.database.MySQLDatabase;
import javatools.database.OracleDatabase;
import javatools.database.PostgresDatabase;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;



/** 
  This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
  It is licensed under the Creative Commons Attribution License 
  (see http://creativecommons.org/licenses/by/3.0) by 
  the YAGO-NAGA team (see http://mpii.de/yago-naga).
    
  
   This is a nonshared, i.e. instantiable variation of the Parameters class. 
   It allows to have different parameter settings handled simultaneously, 
   i.e. each component (or each component instance) using NonsharedParameters can maintain
   its own parameters. This allows, for instance, that two components running at the same time
   work on different databases, both obtained through their own NonsharedParameters instance.  
   
   While the old 'Parameters' class is more convenient to use (less objects passed around),
   this version makes it easier to integrate your components with other components that also 
   use the Parameters/NonsharedParameters to load (and maintain) their settings.
   
   Therefore please consider using the NonsharedParameters instead of the Parameters, 
   if your code may ever be used in parallel with another project that might use one 
   of the Parameters classes. 
  
  Provides an interface for an ini-File. The ini-File may contain parameters of the form
  <PRE>
  parameterName = value
  ...
  </PRE>
  It may also contain comments or section headers (i.e. anything that does not match the
  above pattern). Parameter names are not case sensitive. Initial and terminal spaces
  are trimmed for both parameter names and values. Boolean parameters accept multiple
  ways of expressing "true" (namely "on", "true", "yes" and "active").
  
  You may also use ${param} constructs to refer to parameters set earlier, 
  these references are replaced at the time the file is read by the current value of 'param' (null if it does not have a value!)
  
  Alternatively you can use $[param] references, 
  they will be replaced by the current value of 'param' at runtime.
  Thus, param could be set/overriden after the actual reference appears, i.e. always reflects current settings. 
  
  
  This class does function as an object. 
  Example:
  <PRE>
    // Read data from my.ini
    NonsharedParameters params = new NonsharedParameters("my.ini");
    // Abort with error message if the following parameters are not specified
    params.ensureParameters(
       "firstPar - some help text for the first parameter",
       "secondPar - some help text for the secondparameter"
    );
    // Retrieve the value of a parameter
    String p=params.get("firstPar");
  </PRE>
  You can load parameters from multiple files. These will overlay.
  You can reference a .ini file in another .ini file using the 'include' parameter, 
  included files will be loaded at the point of the 'include' statements, 
  parameter settings following the include statement can
  overwrite parameter settings of this included ini file.   
  Example:
   Content of main.ini:
   <PRE>
     //load database configuration
     include = db_myserver.ini
     // overwrite or add parameters
     databasePort = 5555
     myOtherParameter = 4
   </PRE>
   Content of db_myserver.ini:
   <PRE>
     databaseSystem = postgres
     databaseDatabase = example
     databaseUser = albert
     databaseHost = localhost
     databasePort = 5432
   </PRE>
   If main.ini is loaded, the 'databasePort' parameter will have value '5555' in the resulting NonsharedParameters instance.
   Include is recursive, make sure you do not generate a cycle!
     
*/
public class NonsharedParameters implements Cloneable{

  
 
   
  /** Holds the filename of the ini-file */
  public File iniFile=null;
  
  /** Holds the path that should be assumed to be the base path to the current directory for all local path values */ 
  public String basePath=null;
  
  /** Contains the values for the parameters*/
  public Map<String,String> values=null;

  /** Holds the pattern used for ini-file-entries */
  public static Pattern INIPATTERN=Pattern.compile(" *(\\w+) *= *(.*) *");
  public static Pattern READTIMEVARIABLEPATTERN=Pattern.compile("(.*)\\$\\{(\\w+)\\}(.*)");
  public static Pattern RUNTIMVARIABLEPATTERN=Pattern.compile("(.*)\\$\\[(\\w+)\\](.*)");

  /** Holds words that count as "no" for boolean parameters */
  public static FinalSet<String> no=new FinalSet<String>(new String [] {
        "inactive",
        "off",
        "false",
        "no",
        "none"
  });
  
  /*****************************************************************************************************************    
   ****                                   Initiate and ShutDown                                                 ****
   *****************************************************************************************************************/
  
  /** Constructors*/
  public NonsharedParameters(){};
  public NonsharedParameters(File iniFile) throws ParameterFileException{    
	  init(iniFile);
  };
  public NonsharedParameters(String iniFile) throws ParameterFileException{
	  init(iniFile);
  };
  
  public NonsharedParameters(File iniFile, String localPath) throws ParameterFileException{    
    if(localPath!=null)
    	basePath=localPath.endsWith("/")?localPath:localPath+"/";    		
    init(iniFile);
  };
  public NonsharedParameters(String iniFile, String localPath) throws ParameterFileException{
	if(localPath!=null)
	   	basePath=localPath.endsWith("/")?localPath:localPath+"/";
    init(iniFile);
  };
  
  public NonsharedParameters(NonsharedParameters other){
    this.basePath=other.basePath;
    this.iniFile=other.iniFile;    
    for (Map.Entry<String,String> entry:other.values.entrySet())
     values.put(entry.getKey(),entry.getValue());      
  };

  /** Cloning implementation */
  @Override 
  public NonsharedParameters clone(){
    try {
      NonsharedParameters other=(NonsharedParameters) super.clone();
      other.values=new TreeMap<String,String>();
      for (Map.Entry<String,String> entry:values.entrySet())
        other.values.put(entry.getKey(),entry.getValue());            
      return other;
    } catch (CloneNotSupportedException e) {
        throw new Error("Is too",e);
    }
  }
  
  
  /*****************************************************************************************************************    
   ****                                     Parameter Access                                                    ****
   *****************************************************************************************************************/
  
  /** Returns a value for a date parameter;  */
  public Timestamp getTimestamp(String s) throws UndefinedParameterException {
    return Timestamp.valueOf(get(s));
  }

  /** Returns a value for a date parameter, returning the default value if undefined; */
  public Timestamp getTimestamp(String s, Timestamp defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?getTimestamp(s):defaultValue);
  }

  /** Returns a value for a file or folder parameter; same as getFile but returns the path as String 
   *  also adjusts local paths such that a global path is returned (if a base path is set)*/
  public String getPath(String s) throws UndefinedParameterException {
    if(basePath==null)
      return get(s);
    else{
      String path=get(s);
      if(path.startsWith("[CONFDIR]"))  //TODO: generally allow to use $[param] to refer to other parameters inside parameter values in contrast to ${param} style this would reflect dynamic changes at runtime!
        return basePath+path.substring(9);
//      else if(path.startsWith("../"))
//          return basePath+path;
      else 
        return path;
    }
  }

  /** Returns a value for a file or folder parameter, returning the default value if undefined; same as getFile but returns the path as String*/
  public String getPath(String s, String defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?getPath(s):defaultValue);
  }
  
  
  /** Returns a value for a file or folder parameter; same as getPath but returns an actual File instance */
  public File getFile(String s) throws UndefinedParameterException {
    return(new File(getPath(s)));
  }

  /** Returns a value for a file or folder parameter, returning the default value if undefined; same as getPath but returns an actual File instance */
  public File getFile(String s, File defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?new File(getPath(s)):defaultValue);
  }
  
  /** Returns a URI built from a given parameter's value 
   * @throws URISyntaxException */
  public URI getURI(String s) throws UndefinedParameterException, URISyntaxException{
    return new URI(get(s));
  }
  
  /** Returns a URI built from a given parameter's value, but returns a given default value if the parameter is undefined
   * @throws URISyntaxException */
  public URI getURI(String s, URI defaultValue) throws UndefinedParameterException, URISyntaxException {
    return(isDefined(s)?new URI(get(s)):defaultValue);
  }
    
  /** Returns a URL built from a given parameter's value 
   * @throws MalformedURLException */
  public URL getURL(String s) throws UndefinedParameterException, MalformedURLException{
    return new URL(get(s));
  }
  
  /** Returns a URL built from a given parameter's value, but returns a given default value if the parameter is undefined
   * @throws MalformedURLException 
   * @throws URISyntaxException */
  public URL getURL(String s, URL defaultValue) throws UndefinedParameterException, MalformedURLException {
    return(isDefined(s)?new URL(get(s)):defaultValue);
  }

  /** Returns a value for an integer parameter as Integer object*/
  public Integer getInteger(String s) throws UndefinedParameterException {
    return(Integer.parseInt(get(s)));
  }
  
  /** Returns a value for an integer parameter returning the default value if undefined*/
  public Integer getInteger(String s, Integer defaultValue) throws UndefinedParameterException {
    if(isDefined(s))
    	return Integer.parseInt(get(s));
    else 
    	return defaultValue;
  }
  
  /** Returns a value for an integer parameter*/
  public int getInt(String s) throws UndefinedParameterException {
    return(Integer.parseInt(get(s)));
  }

  /** Returns a value for an integer parameter returning the default value if undefined*/
  public int getInt(String s, int defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Integer.parseInt(get(s)):defaultValue);
  }
  
  /** Returns a value for an integer parameter*/
  public Float getFloatObject(String s) throws UndefinedParameterException {
    return(Float.parseFloat(get(s)));
  }
  
  /** Returns a value for an integer parameter returning the default value if undefined*/
  public Float getFloatObject(String s, Float defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Float.parseFloat(get(s)):defaultValue);
  }

  /** Returns a value for an integer parameter*/
  public float getFloat(String s) throws UndefinedParameterException {
    return(Float.parseFloat(get(s)));
  }

  /** Returns a value for an integer parameter returning the default value if undefined*/
  public float getFloat(String s, float defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Float.parseFloat(get(s)):defaultValue);
  }
  
  /** Returns a value for an integer parameter*/
  public double getDouble(String s) throws UndefinedParameterException {
    return(Double.parseDouble(get(s)));
  }

  /** Returns a value for an integer parameter returning the default value if undefined*/
  public Double getDouble(String s, Double defaultValue) throws UndefinedParameterException {
    return(isDefined(s)?Double.parseDouble(get(s)):defaultValue);
  }
  
  /** Returns a value for a boolean parameter */
  public Boolean getBooleanObject(String s) throws UndefinedParameterException  {
    String v=get(s);
    return(!no.contains(v.toLowerCase()));
  }
  
  /** Returns a value for a boolean parameter, returning a default value by default */
  public Boolean getBooleanObject(String s, Boolean defaultValue) {
    String v=get(s,defaultValue?"yes":"no");
    return(!no.contains(v.toLowerCase()));
  }

  /** Returns a value for a boolean parameter */
  public boolean getBoolean(String s) throws UndefinedParameterException  {
    String v=get(s);
    return(!no.contains(v.toLowerCase()));
  }

  /** Returns a value for a boolean parameter, returning a default value by default */
  public boolean getBoolean(String s, boolean defaultValue) {
    String v=get(s,defaultValue?"yes":"no");
    return(!no.contains(v.toLowerCase()));
  }

  /** Returns a value for a list parameter */
  public List<String> getList(String s) throws UndefinedParameterException  {
    if(!isDefined(s)) return(null);
    return(Arrays.asList(get(s).split("\\s*,\\s*")));
  }
  
  /** Returns a value for a map parameter */
  public Map<String,String> getMap(String s) throws UndefinedParameterException  {
    if(!isDefined(s)) return(null);
    Map<String,String> map =new HashMap<String,String>();
    for(String entry:get(s).split("\\s*,\\s*")){
      String[] entrypair=entry.split("\\s*-->\\s*");
      if (entrypair.length<2)
        return null;
      map.put(entrypair[0].toLowerCase(), entrypair[1]);
    }      
    return map;
  }
  
  /** Returns a value for a map parameter */
  public String getMapEntry(String s, String key) throws UndefinedParameterException  {
    if(!isDefined(s)) return(null);       
    for(String entry:get(s).split("\\s*,\\s*")){
      String[] entrypair=entry.split("\\s*-->\\s*");
      if (entrypair.length<2)
        return null;
      if(entrypair[0].toLowerCase().equals(key.toLowerCase()))
        return entrypair[1];
    }      
    return null;
  }


  /** Returns a value for a parameter*/
  public String get(String s) throws UndefinedParameterException  {
    if(values==null) throw new RuntimeException("Call init() before get()!");
    String pname=s.indexOf(' ')==-1?s:s.substring(0,s.indexOf(' '));

    String v=values.get(pname.toLowerCase());
    if(v==null) throw new UndefinedParameterException(s,iniFile);
    
	Matcher m = RUNTIMVARIABLEPATTERN.matcher(v);
	if(m.matches()){
		v=m.group(1)+get(m.group(2))+m.group(3);
	}
        
    return(v);
  }

  /** Returns a value for a parameter, returning a default value by default */
  public String get(String s, String defaultValue)  {
    if(values==null) throw new RuntimeException("Call init() before get()!");
    String pname=s.indexOf(' ')==-1?s:s.substring(0,s.indexOf(' '));
    String v=values.get(pname.toLowerCase());
    if(v==null) return(defaultValue);
    return(v);
  }
  
  /** Returns a value for a parameter. If not present, asks the user for it */
  public String getOrRequest(String s, String description) {
    if (values == null)
      throw new RuntimeException("Call init() before get()!");
    String v = values.get(s.toLowerCase());
    if (v == null) {
      D.println(description);
      v = D.read();
    }
    return (v);
  }

  /**
   * Returns a value for a parameter. If not present, asks the user for it and
   * adds it
   */
  public String getOrRequestAndAdd(String s, String description)
      throws IOException {
    String v = getOrRequest(s, description);
    add(s, v);
    return (v);
  }

  /**
   * Returns a value for a parameter. If not present, asks the user for it and
   * adds it
   */
  public boolean getOrRequestAndAddBoolean(String s, String description)
      throws IOException {
    boolean v = getOrRequestBoolean(s, description);
    add(s, v ? "yes" : "no");
    return (v);
  }

  /** Returns a value for a parameter. If not present, asks the user for it */
  public File getOrRequestFileParameter(String s, String description) {
    while (true) {
      String fn = getOrRequest(s, description);
      File f = new File(fn);
      if (f.exists())
        return (f);
      D.println("File not found",fn);
      remove(s);
    }
  }
  
  /** Returns a value for a parameter. If not present, asks the user for it */
  public boolean getOrRequestBoolean(String s, String description) {
    while (true) {
      String fn = getOrRequest(s, description).toLowerCase();
      if (fn.equals("true") || fn.equals("yes"))
        return (true);
      if (fn.equals("false") || fn.equals("no"))
        return (false);
    }
  }

  /** Returns a value for a parameter. If not present, asks the user for it */
  public int getOrRequestInteger(String s, String description) {
    while (true) {
      String fn = getOrRequest(s, description);
      try {
        return(Integer.parseInt(fn));
      } catch(Exception e) {}
      remove(s);
    }
  }
  
  /** Adds a value to the map and to the ini file, if not yet there */
  public void add(String key, String value) throws IOException {
    if (values == null || iniFile == null)
      throw new RuntimeException("Call init() before get()!");
    if (values.containsKey(key.toLowerCase()))
      return;
    values.put(key.toLowerCase(), value);
    Writer w = new FileWriter(iniFile, true);
    w.write(key + " = " + value + "\n");
    w.close();
  }
  
  /** Removes a value from the mapping (NOT: from the file) */
  public String remove(String parameter) {
    return(values.remove(parameter.toLowerCase()));
  }

  /** sets a value for a parameter */
  public void set(String param, String value){
	  if(values==null) throw new RuntimeException("Call init() before get()!");
	  values.put(param.toLowerCase(), value);
  }
  
  /** Initializes the parameters from a file
 * @throws ParameterFileException */
  public void init(File f) throws ParameterFileException {
    init(f,true);
  }
  public void init(File f, boolean mainIni) throws ParameterFileException  {
    if(f.equals(iniFile)) return;    
    if(mainIni){
      values=new TreeMap<String,String>();    
      iniFile=f;
      if(basePath==null)
        basePath=(f.getParent()!=null?f.getParent()+"/":"");
    }
    try{        
    if (!iniFile.exists()) {
      Announce.error("The initialisation file",
          iniFile.getCanonicalPath(),
          "was not found.");
    }
    String lastAttrib=null;
    for (String l : new FileLines(f)) {
    	Matcher m = READTIMEVARIABLEPATTERN.matcher(l);
    	if(m.matches()){
    		l=m.group(1)+values.get(m.group(2))+m.group(3);
    	}
      m = INIPATTERN.matcher(l);
      if (!m.matches()) {
        if(lastAttrib!=null) {
          values.put(lastAttrib, values.get(lastAttrib)+l);
          if(!l.trim().endsWith(",")) lastAttrib=null;
        }
        continue;
      }       
      String s = m.group(2).trim();
      if (s.startsWith("\""))
        s = s.substring(1);
      if (s.endsWith("\""))
        s = s.substring(0, s.length() - 1);
      
      if(m.group(1).toLowerCase().equals("include")){
        if(s.startsWith("/"))
          init(s,false);
        else
          init((f.getParent()!=null?f.getParent()+"/":"")+s,false);        
      }
      else{
        values.put(m.group(1).toLowerCase(), s);
        if(s.trim().endsWith(",")) lastAttrib=m.group(1).toLowerCase();
        else lastAttrib=null;
      }
    }
    } catch (IOException ex){
		  throw new ParameterFileException("Problem when reading '"+iniFile.getPath()+"'",ex);
    }
  }
  
  /** Seeks the file in all given folders
 * @throws ParameterFileException */  
  public void init(String filename, File... folders) throws ParameterFileException  {
    boolean found=false;
    for(File folder : folders) {
      if(new File(folder, filename).exists()) {
        if(found) 
          throw new ParameterFileException("INI-file "+filename+"occurs twice in given folders");
        init(new File(folder, filename));
        found = true;
      }
    }
  }

  
  /** Initializes the parameters from a file
 * @throws ParameterFileException */
  public void init(String filename) throws ParameterFileException {
    init(filename,true);
  }
  public void init(String file, boolean mainIni) throws ParameterFileException  {
    Announce.message("Loading ini file '"+file+"'");
    init(new File(file),mainIni);    
  }
  
  
  /** Tells whether a parameter is defined */
  public boolean isDefined(String s) {
    if (values == null)
      throw new RuntimeException("Call init() before get()!");
    String pname = s.indexOf(' ') == -1 ? s : s
        .substring(0, s.indexOf(' '));
    return (values.containsKey(pname.toLowerCase()));
  }
  
  /** Reports an error message and aborts if the parameters are undefined.
   * p may contain strings of the form "parametername explanation"*/
  public void ensureParameters(String... p) {
    if (values == null)
      throw new RuntimeException("Call init() before ensureParameters()!");
    boolean OK = true;
    StringBuilder b = new StringBuilder(
        "The following parameters are undefined in ").append(iniFile);
    for (String s : p) {
      if (!isDefined(s)) {
        b.append("\n       ").append(s);
        OK = false;
      }
    }
    if (OK)
      return;
    Announce.error(b.toString());
  }

  /** Parses the arguments of the main method and tells whether a parameter is on or off */
  public boolean getBooleanArgument(String[] args,String... argnames) {
    String arg = " ";
    for (String s : args)
      arg += s + ' ';
    String p = "\\W(";
    for (String s : argnames)
      p += s + '|';
    if (p.endsWith("|"))
      p = p.substring(0, p.length() - 1);
    p += ")\\W";
    Matcher m = Pattern.compile(p).matcher(arg);
    if (!m.find())
      return (false);
    String next = arg.substring(m.end()).toLowerCase();
    if (next.indexOf(' ') != -1)
      next = next.substring(0, next.indexOf(' '));
    if (next.equals("off"))
      return (false);
    if (next.equals("0"))
      return (false);
    if (next.equals("false"))
      return (false);
    String previous = arg.substring(0, m.start()).toLowerCase();
    if (previous.indexOf(' ') != -1)
      previous = previous.substring(previous.lastIndexOf(' ') + 1);
    if (previous.equals("no"))
      return (false);
    return (true);
  }
  
  /** Deletes all current values*/
  public void reset() {
    iniFile=null;
    values=null;
  }
  
  /** Returns the database defined in this ini-file */
  public Database getDatabase() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    ensureParameters("databaseSystem - either Oracle, Postgres or MySQL",
        "databaseUser - the user name for the database (also: databaseDatabase, databaseInst,databasePort,databaseHost,databaseSchema)",
        "databasePassword - the password for the database"
    );
        
    // Retrieve the obligatory parameters
    String system=this.get("databaseSystem").toUpperCase();
    String user=this.get("databaseUser");    
    String password=this.get("databasePassword");
    String host=null;
    String schema=null;
    String inst=null;
    String port=null;
    String database=null;
    Integer fetchsize=this.getInteger("databaseFetchSize",null);

    
    // Retrieve the optional parameters
    try {
      host=this.get("databaseHost");
    } catch(Exception e){Announce.debug("Warning: "+e);};
    try {
      schema=this.get("databaseSchema");
    } catch(Exception e){Announce.debug("Warning: "+e);};
    try {
      port=this.get("databasePort");    
    } catch(Exception e){Announce.debug("Warning: "+e);};
    if(system.equals("ORACLE")){
      try {
        inst=this.get("databaseSID");
      } catch(UndefinedParameterException e) {Announce.debug("Warning: "+e);}
    }
    try {
      database=this.get("databaseDatabase");
    } catch(UndefinedParameterException e) {Announce.debug("Warning: "+e);}    
    
    // Initialize the database
    Database db=null;
    // ------ ORACLE ----------
    if(system.equals("ORACLE")) {
      db=(new OracleDatabase(user,password,host,port,inst));
    }
    //  ------ MySQL----------
    if(system.equals("MYSQL")) {
      db=(new MySQLDatabase(user,password,database,host,port));
    }
    //  ------ Postgres----------
    if(system.equals("POSTGRES")) {
      db=(new PostgresDatabase(user,password,database,host,port,schema));
    }
    if (db==null)
    	throw new RuntimeException("Unsupported database system "+system);
    if(fetchsize!=null)
    	db.setFetchsize(fetchsize);
    return db;    	
  }
  
  
  /** Checks whether a parameter matching the field name is available.
   *  There are two ways to declare attributes to configurable parameters,
   *  either write them all upper case of use the prefix "cp_"; 
   *  in the latter case the prefix is ignored when matching parameters to the field.
   *  Returns the parameter value in the format corresponding the field. 
   * @param field  the attribute against which to match the parameters
   * @return  an object representing the value of a parameter that matches the given attribute
   *          iff there is a match, otherwise null
   *  @Note	this will become protected, please use the initiateClassAttributes method (if it does not work for you, let me(Steffen) know
   *  TODO: make protected
   */
  @Deprecated //will become protected, use the initiateClassAttributes method instead
  public Object matchObjectAttribut(Field field) throws IllegalAccessException{   
      String parameterName = (field.getName().startsWith("cp_")?field.getName().substring(2):field.getName());
      if ((parameterName.equals(parameterName.toUpperCase())||field.getName().startsWith("cp_")) && isDefined(parameterName)) {
        if (field.getType() == Integer.class || field.getType() == int.class) return getInteger(parameterName);
        else if (field.getType() == Boolean.class || field.getType() == boolean.class) return new Boolean(getBoolean(parameterName));
        else if (field.getType() == Float.class || field.getType() == float.class) return new Float(getFloat(parameterName));
        else if (field.getType() == Double.class || field.getType() == double.class) return new Double(getDouble(parameterName));
        else if (D.indexOf(List.class, (Object[]) field.getType().getInterfaces()) != -1) return getList(parameterName);
        else if (D.indexOf(List.class, field.getType()) != -1) return getList(parameterName);
        else return get(parameterName);
      }
      return null;
  }  
  
  
  /** 
   * Checks for all full-upper-case class attributes whether there is a matching parameter 
   * and sets its value to the parameter value.  
   * @param classname  name of the class for which to check
   * @param	object	object, which needs to be an instance of the given class
   */
  public void initiateClassAttributes(String className, Object object) {
	  try{
		  Class<?> cl=Class.forName(className);
		  for (Field field : cl.getDeclaredFields()) {
			  field.setAccessible(true);
		      Object value=matchObjectAttribut(field);
		      if(value!=null)
		        field.set(this, value);
		      field.setAccessible(false);
		  }
	  } catch (IllegalAccessException ex){
		  throw new RuntimeException(ex); 
	  } catch (ClassNotFoundException ex){
		  throw new RuntimeException(ex); 
	  }
  }
  
  /** 
   * Checks for all full-upper-case class attributes whether there is a matching parameter 
   * and sets its value to the parameter value. Does this also for parameters inherited from super-classes.
   * @param classname  name of the class for which to check
   * @param	object	object, which needs to be an instance of the given class
   */  
  public void initiateAllClassAttributes(Object object) {
	  try{
		  Class<?> cl=object.getClass();
		  while(cl!=null){
			  for (Field field : cl.getDeclaredFields()) {
				  field.setAccessible(true);
				  Object value=matchObjectAttribut(field);
				  if(value!=null)
					  field.set(object, value);
				  field.setAccessible(false);
			  }
			  cl=cl.getSuperclass();
		  }
	  } catch (IllegalAccessException ex){
		  throw new RuntimeException(ex); 
	  }
  }
  
  /** 
   * Checks for all full-upper-case class attributes whether there is a matching parameter 
   * and sets its value to the parameter value.  
   * @param classname  name of the class for which to check
   * @param	object	object, which needs to be an instance of the given class
   */  
  public void initiateClassAttributes(Object object) {
	  try{
		  Class<?> cl=object.getClass();		  
			  for (Field field : cl.getDeclaredFields()) {
				  field.setAccessible(true);
				  Object value=matchObjectAttribut(field);
				  if(value!=null)
					  field.set(object, value);
				  field.setAccessible(false);
			  }			  		  
	  } catch (IllegalAccessException ex){
		  throw new RuntimeException(ex); 
	  }
  }  

  
  
  /** Returns all defined parameters*/
  public Set<String> parameters() {
    return(values.keySet());
  }
  
  /** Stores the parameters in a given file 
 * @throws ParameterFileException */
  public void saveAs(String file) throws ParameterFileException  {
	  try{
	    Writer w = new FileWriter(file, false);
	    for(Map.Entry<String, String> entry:values.entrySet())
	      w.write(entry.getKey() + " = " + entry.getValue() + "\n");
	    w.close();
	  } catch (IOException ex){
		  throw new ParameterFileException("Problem when writing to '"+iniFile.getPath()+"'",ex);
	  }
  }
  
  
	/*****************************************************************************************************************    
	 ****                                     Exceptions                                                          ****
	 *****************************************************************************************************************/

  /** Thrown for an undefined Parameter */
  public static class UndefinedParameterException extends RuntimeException {    

	  private static final long serialVersionUID = -7648653481162390257L;

	  public UndefinedParameterException(String s, File f) {
		  super("The parameter "+s+" is undefined in "+f);
	  }
  }



  /** Reading or writing the parameters ini file failed for some reason 
   *  In some cases the enclosing program might resolve the issue by providing another file */
  public static class ParameterFileException extends IOException{
	  private static final long serialVersionUID = 1L;
	  public ParameterFileException(){
		  super();
	  }
	  public ParameterFileException(String message){
		  super(message);
	  }
	  public ParameterFileException(Throwable cause){
		  super(cause);
	  }         
	  public ParameterFileException(String message, Throwable cause){
		  super(message,cause);
	  }   

  }


	/*****************************************************************************************************************    
	 ****                                     Test Main                                                           ****
	 *****************************************************************************************************************/
  
  /** Test routine */  
  public static void main(String[] args) throws Exception {    
    NonsharedParameters params= new NonsharedParameters();
    params.init("testdata/testconfig.ini");
    D.p(params.values);
    params.set("someSetting", "1");
    NonsharedParameters params2=params.clone();
    params2.set("someSetting", "2");
    D.p(params.values);
    D.p(params2.values);    
  }
}
