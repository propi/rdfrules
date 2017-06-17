package javatools.database;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import javatools.administrative.D;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  
 

The class PostgresDatabase implements the Database-interface for a
PostgreSQL data base. Make sure that the file "postgresql-<I>version</I>.jdbc3.jar" of the 
Postgres distribution is in the classpath. When using Eclipse, add 
the file via Project ->Properties ->JavaBuildPath ->Libraries 
->ExternalJARFile.<BR>
Example:
<PRE>
     Database d=new PostgresDatabase("user","password");     
     d.queryColumn("SELECT foodname FROM food WHERE origin=\"Italy\"")
     -> [ "Pizza Romana", "Spaghetti alla Bolognese", "Saltimbocca"]
     Database.describe(d.query("SELECT * FROM food WHERE origin=\"Italy\"")
     -> foodname |origin  |calories |
        ------------------------------
        Pizza Rom|Italy   |10000    |
        Spaghetti|Italy   |8000     |
        Saltimboc|Italy   |8000     |        
</PRE>
This class also provides SQL datatypes (extensions of SQLType.java) that
behave according to the conventions of Postgres. For example, VARCHAR string literals print 
inner quotes as doublequotes.*/
public class PostgresDatabase extends Database {

  /** holds the user name */
  private String user=null;
  private String password=null;
  private String database=null;
  private String host=null;
  private String port=null; 
  
  /** Holds the default schema*/
  protected String schema = null;
  /** indicates whether to use ssl */
  private boolean useSSL=false; 
 

  /** Constructs a non-functional PostgresDatabase for use of getSQLType*/
  public PostgresDatabase() {
    java2SQL.put(String.class,postgretext);
    type2SQL.put(Types.VARCHAR,postgrevarchar);    
    type2SQL.put(Types.BLOB, postgretext);
  }
  
  /** Constructs a new Database from a user, a password and a host
   * @throws ClassNotFoundException 
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws SQLException */
  public PostgresDatabase(String user, String password, String database, String host, String port, boolean useSSL) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this(user,password,database,host,port,null,useSSL);        
  }

  public PostgresDatabase(String user, String password, String database, String host, String port) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this(user, password, database, host, port, false);
  }

  /** Constructs a new Database from a user, a password and a host, setting also the (preferred) schema (public stays fallback schema) */
  public PostgresDatabase(String user, String password, String database, String host, String port, String schema, boolean useSSL) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this();
    if (password == null) password = "";
    if (host == null || host.length() == 0) host = "localhost";
    if (port == null || port.length() == 0) port = "5432";
    driver = (Driver) Class.forName("org.postgresql.Driver").newInstance();
    DriverManager.registerDriver(driver);
    this.user=user;
    this.password=password;
    this.database=database;
    this.host=host;
    this.port=port;
    this.useSSL=useSSL;
    this.schema=schema;
    connect();    
  }
  
  /** Constructs a new Database from a user, a password and a host, setting also the (preferred) schema (public stays fallback schema) */
  public PostgresDatabase(String user, String password, String database, String host, String port, String schema) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this(user, password, database, host, port, schema, false);    
  }

  /** connects to the database specified */
  @Override
  public void connect () throws SQLException{
    String url = "jdbc:postgresql://" + host + ":" + port + (database == null ? "" : "/" + database) + (useSSL ? "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory" : "");
    connection = DriverManager.getConnection(url, user, password);
    connection.setAutoCommit(true);
    setSchema(schema);
    description = "Postgres database '" + database + "' as '" + user + "' at " + host + ":" + port + " using schema '" + schema+"'";
  }
  

  
  /** Sets the default schema*/
  public void setSchema(String s) throws SQLException {
    if(s==null) return;
    executeUpdate("SET search_path TO "+s+", public");
    schema=s;
    description=description.substring(0,description.lastIndexOf(' '))+" "+schema;
  }
  /** Sets the default schema*/
  public void setExclusiveSchema(String s) throws SQLException {
    executeUpdate("SET search_path TO "+s+"");
    schema=s;
    description=description.substring(0,description.lastIndexOf(' '))+" "+schema;
  }
  
  /* Varchar handling 
   * !Note: Currently all VARCHAR strings are preceded by an E' this is because Postgre is switching its behaviour
   * with regard to backslash interpretation. In the current version (8.3) backslashs are by default interpreted as 
   * an escape character. This behaviour will in the future be turned of by default. Currently warnings are printed
   * into the server log whenever a String appears to contain a backslash used as escape character to warn about this behaviour change
   * These warnings can overload the server by filling the hard-disc if a lot of queries arrive in a short time.
   * This can be avoided by the preceding E' as this declares that in the following string backslashs indeed are escapes.
   * Another solution would be to set the control variable at the server to do not consider backslashs as escapes and remove 
   * the preceding E' as well as the backslash escape treatment (replace("\\", "\\\\")) but this could(?) affect other applications.
   * However once the default behaviour at the server has changed, the latter solution would do.
   * For details about the server variable look for parameter 'standard_conforming_strings (boolean)' here: 
   * http://www.postgresql.org/docs/8.3/static/runtime-config-compatible.html
 */
  public static class PostgreVarchar extends SQLType.ANSIvarchar {
    public PostgreVarchar(int size) {
      super(size);
    }  
    public PostgreVarchar() {
      super();
    } 
    public String toString() {
      return("VARCHAR("+scale+")");
    }
    public String format(Object o) {      
      
      String s=o.toString();
      if(s.length()>scale) s=s.substring(0,scale);
      s=s.replace("'", "''").replace("\\", "\\\\");
      
      return("E'"+s+"'");
    } 
  }
  public static PostgreVarchar postgrevarchar=new PostgreVarchar();
  
  
  public static class Postgretext extends SQLType {
    public Postgretext(int size) {
      typeCode=Types.BLOB;
      scale=0;
    }  
    public Postgretext() {
      this(0);
    }        
    public String format(Object o) {
      String s=o.toString().replace("'", "''").replace("\\", "\\\\");
      //if(s.length()>scale) s=s.substring(0,scale);
      return("E'"+s+"'");
    }
    public String toString() {
      return("TEXT");
    }
}
public static Postgretext postgretext=new Postgretext();
  
  
  /**
   * Creates or rewrites an SQL table. Attributes is an alternating sequence of
   * a name (String) and a type (from java.sql.Type).
   */
  public void createTable(String name, Object... attributes) throws SQLException {
    try {
      executeUpdate("DROP TABLE "+schema+"." + name);
    } catch (SQLException e) {
      //Announce.message(e); //hook here for debugging; usually disabled as exceptions are normal in cases where the table did not yet exist before
    }
    StringBuilder b = new StringBuilder("CREATE TABLE ").append(schema).append(".").append(name).append(" (");
    for (int i = 0; i < attributes.length; i += 2) {
      b.append(attributes[i]).append(' ');
      if (attributes[i + 1] instanceof Integer) {
        b.append(getSQLType((Integer) attributes[i + 1])).append(", ");
      } else {
        b.append(getSQLType((Class<?>) attributes[i + 1])).append(", ");
      }
    }
    b.setLength(b.length() - 2);
    b.append(')');
    executeUpdate(b.toString());
  }



  @Override
  public boolean jarAvailable() {
    try {
      Class.forName("org.postgresql.Driver").newInstance();
      return true;
    } catch (Exception e) {
    }
    return false;
  }
  
  
  // ---------------------------------------------------------------------
  //           DB specific SQL variations of common functionality
  // ---------------------------------------------------------------------
  
  /** 
   * Produces an SQL fragment casting the given value to the given type   * 
   */
  @Override
   public String cast(String value, String type){
     StringBuilder sql=new StringBuilder("CAST(");
     sql.append(value).append(" AS ").append(type).append(")");
     return sql.toString();    
   }
  
  /** returns the database system specific expression for ifnull functionality 
   * i.e. ifnull(a,b) returns b if a is null and a otherwise */
  @Override
  public String getSQLStmntIFNULL(String a, String b){
    return "COALESCE("+a+","+b+")";
  }
  
  /** 
   * Produces an SQL fragment representing column properties for an autoincrementing integer column
   * s.t. if used during table creation a column can declared to get by default an 
   * integer value assigned according to an internal self-incrementing sequence counter
   * Example:
   * createTable("tableWithSingleAutoIncrementingIDColumn", "ID", autoincrementColumn()) 
   */
  @Override
   public String autoincrementColumn(){
     return "SERIAL";
   }
  

  // ---------------------------------------------------------------------
  //                 Transactions
  // ---------------------------------------------------------------------
  


  /** Initiates a transaction by disabling autocommit and enabling transaction mod
   * Note: In Postgres transactions have a name e*/
  public void startTransaction() throws InitTransactionSQLException {
    super.startTransaction();
    try{
      Statement stmnt=connection.createStatement();
      stmnt.executeUpdate("BEGIN");   
      close(stmnt);
    }catch(SQLException ex){
      throw new InitTransactionSQLException("Could not start transaction.", ex);
    }
  }
  
  /** commits the transaction aggregated so far 
   * if the commit fails the transaction is rolled back!*/
  protected void commitTransaction() throws TransactionSQLException {
  try{
      Statement stmnt=connection.createStatement();
      stmnt.executeUpdate("COMMIT");    
      close(stmnt);
    connection.commit();                    
  }catch(SQLException ex){
    CommitTransactionSQLException commitfail=new CommitTransactionSQLException("Could not commit transaction.", ex);
    try{
      resetTransaction();
    }catch (RollbackTransactionSQLException rex){
      throw new RollbackTransactionSQLException(rex.getMessage(),commitfail);
    }
    throw commitfail;
  }
  } 
  
  /** Locks a table in write mode, i.e. other db connections can only read the table, but not write to it */
  public void lockTableWriteAccess(Map<String, String> tablesAndAliases) throws SQLException{   
    for(String table : tablesAndAliases.keySet()){
      String sql="LOCK TABLE "+table+" IN EXCLUSIVE MODE";
      connection.createStatement().executeUpdate(sql);
    }
  }
  
  /** Locks a table in read mode, i.e. only this connection can read or write the table */
  public void lockTableReadAccess(Map<String, String> tablesAndAliases) throws SQLException{    
    for(String table : tablesAndAliases.keySet()){
      String sql="LOCK TABLE "+table+" IN ACCESS EXCLUSIVE MODE";
      connection.createStatement().executeUpdate(sql);
    }
  }
  
  /** releases all locks the connection holds, commits the current transaction and ends it */
  @Override
  public void releaseLocksAndEndTransaction() throws SQLException{        
    endTransaction(true);
  }
  

  


  public static void main(String[] args) {
    try {
      Database d = new PostgresDatabase("postgres", "postgres", "postgres", null, null);
      //d.executeUpdate("CREATE table test (a integer, b varchar)");
      d.executeUpdate("INSERT into test values (1,2)");
      ResultSet s = d.query("select * from test");
      s.next();
      D.p(s.getString(1));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
