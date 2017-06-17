package javatools.database;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.StringModifier;
import javatools.filehandlers.CSVFile;
import javatools.filehandlers.CSVLines;
import javatools.filehandlers.UTF8Writer;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * This abstract class provides a simple Wrapper for an SQL data base. It is
 * implemented by OracleDatabase, PostgresDatabase and MySQLDatabase. <BR>
 * Example:
 * 
 * <PRE>
 * 
 * Database d=new OracleDatabase("user","password"); 
 * for(String food : d.query("SELECT foodname FROM food", ResultIterator.StringWrapper)) {
 *   System.out.print(food);
 * } 
 * -> Pizza Spaghetti Saltimbocca
 * </PRE>
 *  
 * It is possible to execute multiple INSERT statements by a bulk loader: 
 * <PRE>
 *   d=new OracleDatabase(...);
 *   Database.Inserter i=d.newInserter(tableName);
 *   i.insert(7,"Hallo");
 *   i.insert(8,"Ciao");   
 *   ...
 *   i.close();
 * </PRE>
 * 
 * The inserters are automatically flushed every 1000 insertions and when
 * closed. They are flushed and closed when the database is closed.
 * <P>
 * Unfortunately, the same datatype is called differently on different database
 * systems, behaves differently and is written down differently. There is an
 * ANSI standard, but of course nobody cares. This is why Database.java provides
 * a method getSQLType(int), which takes any of the SQL datatypes defined in
 * java.sql.Types (e.g. VARCHAR) and returns an object of the class
 * javatools.SQLType. This object then behaves according to the conventions of
 * the respective database system. Each implementation of Database.java should
 * return SQLType-objects tailored to the specific database (e.g. OracleDatabase
 * maps BOOLEAN to NUMBER(1) and replaces quotes in string literals by double
 * quotes). By default, the ANSI datatypes are returned.<BR>
 * Example:
 * 
 * <PRE>
 * 
 * Database d=new OracleDatabase("user","password");
 * d.getSQLType(java.sql.Types.VARCHAR).format("Bobby's") 
 * -> 'Bobby"s' 
 * 
 * d=new MySQLDatabase("user","password","database");
 * d.getSQLType(java.sql.Types.VARCHAR).format("Bobby's") 
 * -> 'Bobby\'s'
 * 
 * </PRE>
 * 
 * Technical issues: Implementations of Database.java can extend the ANSI type
 * classes given in SQLType.java and they can modify the maps java2SQL and
 * type2SQL provided by Database.java. See OracleDatabase.java for an example.
 * For each datatype class, there should only be one instance (per scale).
 * Unfortunately, Java does not allow instances to have different method
 * implementation, so that each datatype has to be a class. It would be
 * convenient to handle datatypes as enums, but enums cannot be extended. To
 * facilitate modifying the ANSI types for subclasses, the getSQLType-method is
 * non-static. Implementations of this class should have a noarg-constructor to
 * enable calls to getSQLType without establishing a database connection.

 */
public abstract class Database {
  

  /*****************************************************************************************************************    
   ****                                    Attributes                                                           ****
   *****************************************************************************************************************/
  
  // ---------------------------------------------------------------------
  //           Configuration
  // ---------------------------------------------------------------------
  
  /** indicates whether automatic reconnection and requerying is attempted 
   *  when a broken connection is discovered after an informative (SELECT) query
   *  if this is set to false, a ConnectionBrokenSQLException is thrown instead */
  boolean autoReconnectOnSelect=true;
  
  
  /** indicates whether automatic reconnection and requerying is attempted 
   *  when a broken connection is discovered after an update (INSERT, UPDATE) query
   *  if this is set to false, a ConnectionBrokenSQLException is thrown instead 
   *  @Note: In rare cases this may lead to an update query being executed twice,
   *  so treat with care! */  
  boolean autoReconnectOnUpdate=false;
  
  

  /** number of milliseconds a connection validity check may 
   *  take until we decide the server won't answer 
   *  Note: If I'm not mistaken, typical latency over an averagely good internet connection (same continent) 
   *  has a latency of under 100 ms, that's why the limit is set to 150, 
   *  feel free to adapt if this seems problematic/too large
   *  Note: only has any effect iff autoReconnectOnX is true 
   *  Note: Only works for databases where the driver supports isValid checks
   *  otherwise we will simply issue a dummy query and see whether we get an answer */
  int validityCheckTimeout=150;
  
  
  // ---------------------------------------------------------------------
  //           Internals
  // ---------------------------------------------------------------------

  /** Handle for the database */
  protected Connection connection;
  
  /** 

  /** Describes this database */
  protected String description = "Unconnected default database";

  /** The type of the resultSet (Forward only by default) */
  protected int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

  /** The concurrency type of the resultSet (read only by default) */
  protected int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
  
  /** The fetchsize to be used on default, i.e. number of rows to be pulled at once 
   *  (default:0 means all results are pulled in directly) */
  protected int fetchsize = 0;

  /** The Driver registered for this database instance
   * TODO: it may be more reasonable to share the same driver instance for all database insances
   *       of the same type...check that and adapt */
  protected Driver driver = null;

  /** Returns the connection */
  public Connection getConnection() {
    return (connection);
  }

  /** the default transaction mode into which to change for transactions */
  private static int defaultTransactionMode = Connection.TRANSACTION_REPEATABLE_READ;

  /** keep track of original transaction mode setting */
  private int originalTransactionMode;

  /** Holds all active inserters to close them in the end*/
  protected List<Inserter> inserters = new ArrayList<Inserter>();

  /** tells whether the database is already closed */
  private boolean closed = false;

  /** The mapping from Java to SQL */
  public Map<Class<?>, SQLType> java2SQL = new HashMap<Class<?>, SQLType>();
  {
    java2SQL.put(Boolean.class, SQLType.ansiboolean);
    java2SQL.put(boolean.class, SQLType.ansiboolean);
    java2SQL.put(String.class, SQLType.ansivarchar);
    java2SQL.put(java.util.Date.class, SQLType.ansitimestamp);
    java2SQL.put(java.util.Calendar.class, SQLType.ansitimestamp);
    java2SQL.put(int.class, SQLType.ansiinteger);
    java2SQL.put(Integer.class, SQLType.ansiinteger);
    java2SQL.put(long.class, SQLType.ansibigint);
    java2SQL.put(Long.class, SQLType.ansibigint);
    java2SQL.put(float.class, SQLType.ansifloat);
    java2SQL.put(Float.class, SQLType.ansifloat);
    java2SQL.put(double.class, SQLType.ansifloat);
    java2SQL.put(Double.class, SQLType.ansifloat);
    java2SQL.put(Character.class, SQLType.ansichar);
    java2SQL.put(char.class, SQLType.ansichar);
  };

  /** The mapping from type codes (as defined in java.sql.Types) to SQL */
  public Map<Integer, SQLType> type2SQL = new HashMap<Integer, SQLType>();
  {
    type2SQL.put(Types.BLOB, SQLType.ansiblob);
    type2SQL.put(Types.VARCHAR, SQLType.ansivarchar);
    type2SQL.put(Types.TIMESTAMP, SQLType.ansitimestamp);
    type2SQL.put(Types.DATE, SQLType.ansitimestamp);
    type2SQL.put(Types.INTEGER, SQLType.ansiinteger);
    type2SQL.put(Types.SMALLINT, SQLType.ansismallint);
    type2SQL.put(Types.DOUBLE, SQLType.ansifloat);
    type2SQL.put(Types.REAL, SQLType.ansifloat);
    type2SQL.put(Types.FLOAT, SQLType.ansifloat);
    type2SQL.put(Types.BOOLEAN, SQLType.ansiboolean);
    type2SQL.put(Types.BIT, SQLType.ansiboolean);
    type2SQL.put(Types.CHAR, SQLType.ansichar);
    type2SQL.put(Types.BIGINT, SQLType.ansibigint);
    type2SQL.put(Types.NUMERIC, SQLType.ansifloat);    
  };

  
  /*****************************************************************************************************************    
   ****                                   Initiate and ShutDown                                                 ****
   *****************************************************************************************************************/
  
  
  /** (re-)connects to the database specified */
  public void reconnect ()throws SQLException{
    close(connection);
    connect();
  }
  
  /** connects to the database specified */
  public abstract void connect ()throws SQLException;
  
  

  /** Closes a connection */
  public static void close(Connection connection) {
    try {
      if ((connection != null) && !connection.isClosed()) connection.close();
    } catch (SQLException e) {
    }
  }

  /** Closes a statement */
  public static void close(Statement statement) {
    try {
      if (statement != null) statement.close();
    } catch (SQLException e) {    
      //Announce.error(e); //hook here for debugging
    }
  }

  /** Closes a result set */
  public static void close(ResultSet rs) {
    
    try{
      if(rs.isClosed())
        return;
    } catch (SQLException e) {
      //Announce.error(e); //hook here for debugging
    }
    try {
      close(rs.getStatement());
    } catch (SQLException e) {
      //Announce.error(e); //hook here for debugging
    }
    try {
      if (rs != null) rs.close();
    } catch (SQLException e) {
      //Announce.error(e); //hook here for debugging
    }
  }

  /** Closes the connection */
  public void close() {
    if (closed) // we need to make sure we only close it once (either manually or by finalizer)
    return;
    if (inTransactionMode) {
      try {
        commitTransaction();
      } catch (TransactionSQLException ex) {
        Announce.error(ex);
      }
    }
    while (inserters.size() != 0)
      inserters.get(0).close();
    close(connection);
    try {
      DriverManager.deregisterDriver(driver);
    } catch (SQLException ex) {
      Announce.error(ex);
    }
    closed = true;
  }
  

  /** Flush the connection 
 * @throws SQLException */
  public void flush() throws SQLException {
    if (inTransactionMode) {
      try {
        commitTransaction();
      } catch (TransactionSQLException ex) {
        Announce.error(ex);
      }
    }
    for(Inserter inserter:inserters)
    	inserter.flush();
  }

  /** Closes the connection */
  public void finalize() {
    try {
      close();
    } catch (Exception e) {
      Announce.error(e);
    }
    ;
  }
  
  
  
  /*****************************************************************************************************************    
   ****                                         Configuration                                                   ****
   *****************************************************************************************************************/

  /** TRUE if the required JAR is there*/
  public boolean jarAvailable() {
    return (true);
  }

  
  
  /** indicates whether automatic reconnection and requerying is attempted 
   *  when a broken connection is discovered after an informative (SELECT) query
   *  if this is set to false, a ConnectionBrokenSQLException is thrown instead */
  public boolean isAutoReconnectingOnSelect() {
    return autoReconnectOnSelect;
  }


  /** enable/disable automatic reconnection and requerying  
   *  when a broken connection is discovered after an informative (SELECT) query
   *  if this is set to false, a ConnectionBrokenSQLException is thrown instead */
  public void setAutoReconnectOnSelect(boolean autoReconnectOnSelect) {
    this.autoReconnectOnSelect = autoReconnectOnSelect;
  }

  /** indicates whether automatic reconnection and requerying is attempted 
   *  when a broken connection is discovered after an update (INSERT, UPDATE) query
   *  if this is set to false, a ConnectionBrokenSQLException is thrown instead 
   *  @Note: In rare cases this may lead to an update query being executed twice,
   *  so treat with care! */
  public boolean isAutoReconnectingOnUpdate() {
    return autoReconnectOnUpdate;
  }


  /** enables/disables whether automatic reconnection and requerying is attempted 
   *  when a broken connection is discovered after an update (INSERT, UPDATE) query
   *  if this is set to false, a ConnectionBrokenSQLException is thrown instead 
   *  @Note: In rare cases this may lead to an update query being executed twice,
   *  so treat with care! */
  public void setAutoReconnectOnUpdate(boolean autoReconnectOnUpdate) {
    this.autoReconnectOnUpdate = autoReconnectOnUpdate;
  }
  
  /** gets the current default fetchsize affecting all queries 
   *  where no specific fetchsize is provided as query argument
   *  The fetchsize determines how many result rows are pulled in from the server at once. 
   *  @Note: Not all database drivers may implement this properly/in a way supported by this class */
  public int getFetchsize() {
	return fetchsize;
}

  /** sets the default fetchsize affecting all following queries 
   *  where no specific fetchsize is provided as query argument
   *  The fetchsize determines how many result rows are pulled in from the server at once. 
   *  @Note: Not all database drivers may implement this properly/in a way supported by this class 
   *  	e.g. for Postgre this has only an effect when using transactions */
  public void setFetchsize(int fetchsize) {
	this.fetchsize = fetchsize;
  }

/** time in milliseconds after which a connection is considered broken 
   *  when no answer is received within that time frame
   */
  public int getValidityCheckTimeout() {
    return validityCheckTimeout;
  }

  /** sets the amount of time a database has to answer to a connection probing 
   *  before the connection is considered broken 
   *  @Note: the value cannot be smaller than 0 */
  public void setValidityCheckTimeout(int validityCheckTimeout) {
    if(validityCheckTimeout<0)
      validityCheckTimeout=0;
    this.validityCheckTimeout = validityCheckTimeout;
  }
  

  
  /*****************************************************************************************************************    
   ****                                      Query Execution                                                    ****
   *****************************************************************************************************************/
  

  /**
   * Prepares the query internally for a call (e.g. adds a semicolon). This
   * implementation does nothing
   */
  protected String prepareQuery(String sql) {
    return (sql);
  }

  /** Returns the resultSetConcurrency */
  public int getResultSetConcurrency() {
    return resultSetConcurrency;
  }

  /** Sets the resultSetConcurrency */
  public void setResultSetConcurrency(int resultSetConcurrency) {
    this.resultSetConcurrency = resultSetConcurrency;
  }

  /** Returns the resultSetType */
  public int getResultSetType() {
    return resultSetType;
  }

  /** Sets the resultSetType */
  public void setResultSetType(int resultSetType) {
    this.resultSetType = resultSetType;
  }
  
  /** Checks whether the connection to the database is still alive */
  public boolean connected()  {
    try{
      return (!connection.isClosed())&&connection.isValid(validityCheckTimeout);
    } catch (SQLFeatureNotSupportedException nosupport){
      try{ //This should work for: H2, MySQL, MS SQL Server, PostgreSQL, SQLite; Oracle is treated differently, see OracleDatabase
        ResultSet rs=executeQuery("SELECT 1",resultSetType,resultSetConcurrency,null); 
        close(rs);
        return true;
      }catch (SQLException ex){
        return false;
      }
    } catch (SQLException ex){
    	Announce.warning("Connection check failed, this should not happen.",ex);
      return false;
      //throw new RuntimeException("This is very unexpected and actually should never happen.", ex);
    }    
  }

  /** called when query execution failed due to some exception; 
   *  checks connectivity and if connection is broken may attempt to reconnect 
   *  depending on the given parameters,
   *  if the connection is still alive, reconnection fails or 
   *  autoReconnect is not enabled throws a fitting exception */
  protected void attemptReconnect(SQLException cause, boolean autoReconnect)
  throws SQLException{
    boolean connected=connected();
    /* if execution fails, the connection might be broken or there is another problem (with the query)
     * if connection is broken and reconnecting enabled, we try to reconnect, otherwise:
     * if connection is alive, we throw the actual error else a ConnectionIsBrokenSQLException */
    if(connected)
      throw cause;
    else{ 
      if(autoReconnect){        
        try{
          reconnect();
        } catch (SQLException ex2) {          
          throw new ConnectionBrokenSQLException("Connection is broken. Reconnection attempt failed.\n" +
                                                 "Original exception at first try was:\n "+cause,ex2);        
        }
      }
      else{
        throw new ConnectionBrokenSQLException("Connection is broken, " +
            "did not try to reconnect and re-execute query.", cause);
      }
    }   
  }

  /**
   * Returns the results for a query as a ResultSet with given type, concurrency and
   * fetchsize. Does not check whether query is an update or select query!
   * The preferred way to execute a query is by the query(String,
   * ResultIterator) method, because it ensures that the statement is closed
   * afterwards.
   * External code should always call one of the 'query' or the 'executeUpdate'
   * methods.
   */
  protected ResultSet executeQuery(String sql, int resultSetType, int resultSetConcurrency, Integer fetchsize) 
  throws SQLException {
    Statement stmnt = connection.createStatement(resultSetType, resultSetConcurrency);
    if (fetchsize != null) stmnt.setFetchSize(fetchsize);
    else stmnt.setFetchSize(this.fetchsize);
    return (stmnt.executeQuery(sql));    
  }
  
  /**
   * Returns the results for a query as a ResultSet with given type, concurrency and
   * fetchsize. The preferred way to execute a query is by the query(String,
   * ResultIterator) method, because it ensures that the statement is closed
   * afterwards. If the query is an update query (i.e. INSERT/DELETE/UPDATE) the
   * method calls executeUpdate and returns null. The preferred way to execute
   * an update query is via the executeUpdate method, because it does not create
   * an open statement.
   */
  public ResultSet query(CharSequence sqlcs, int resultSetType, int resultSetConcurrency, Integer fetchsize) throws SQLException {
    String sql = prepareQuery(sqlcs.toString());
    if (sql.toUpperCase().startsWith("INSERT") || sql.toUpperCase().startsWith("UPDATE") || sql.toUpperCase().startsWith("DELETE") 
        || sql.toUpperCase().startsWith("CREATE") || sql.toUpperCase().startsWith("DROP")
        || sql.toUpperCase().startsWith("ALTER")) {
      executeUpdate(sql);
      return (null);
    }
    try {
      return executeQuery(sql, resultSetType, resultSetConcurrency, fetchsize);
    } catch (SQLException e) {
      attemptReconnect(e,autoReconnectOnSelect);
      return executeQuery(sql, resultSetType, resultSetConcurrency, fetchsize);            
    }
  }

  /**
   * Returns the results for a query as a ResultSet with given type and
   * concurrency. The preferred way to execute a query is by the query(String,
   * ResultIterator) method, because it ensures that the statement is closed
   * afterwards. If the query is an update query (i.e. INSERT/DELETE/UPDATE) the
   * method calls executeUpdate and returns null. The preferred way to execute
   * an update query is via the executeUpdate method, because it does not create
   * an open statement.
   */
  public ResultSet query(CharSequence sqlcs, int resultSetType, int resultSetConcurrency) throws SQLException {
    return query(sqlcs, resultSetType, resultSetConcurrency, null);
  }

  /**
   * Returns the results for a query as a ResultSet with default type and
   * concurrency (read comments!). The preferred way to execute a query is by
   * the query(String, ResultWrapper) method, because it ensures that the
   * statement is closed afterwards. If you use the query(String) method
   * instead, be sure to call Database.close(ResultSet) on the result set,
   * because this ensures that the underlying statement is closed. The preferred
   * way to execute an update query (i.e. INSERT/DELETE/UPDATE) is via the
   * executeUpdate method, because it does not create an open statement. If
   * query(String) is called with an update query, this method calls
   * executeUpdate automatically and returns null.
   */
  public ResultSet query(CharSequence sql) throws SQLException {
    return (query(sql, resultSetType, resultSetConcurrency));
  }

  /** Executes an SQL update query, returns the number of rows added/deleted */
  public int executeUpdate(CharSequence sqlcs) throws SQLException {
    String sql = prepareQuery(sqlcs.toString());
    try {
      return executeUpdateQuery(sql);
    } catch (SQLException e) {
      attemptReconnect(e, autoReconnectOnUpdate);          
      return executeUpdateQuery(sql);
    }  
  }
  
  /** Executes an SQL update query, returns the number of rows added/deleted */
  protected int executeUpdateQuery(String sqlcs) throws SQLException {
    String sql = prepareQuery(sqlcs.toString());
    try {
      Statement s = connection.createStatement();
      int result = s.executeUpdate(sql);
      close(s);
      return (result);
    } catch (SQLException e) {
      throw new SQLException(sql + "\n" + e.getMessage());
    }
  }
  


  /** Returns the results for a query as a ResultIterator */
  public <T> ResultIterator<T> query(CharSequence sql, ResultIterator.ResultWrapper<T> rc) throws SQLException {
    return (new ResultIterator<T>(query(sql, resultSetType, resultSetConcurrency), rc));
  }

  /** Returns a single value (or null) */
  public <T> T queryValue(CharSequence sql, ResultIterator.ResultWrapper<T> rc) throws SQLException {
    ResultIterator<T> results = new ResultIterator<T>(query(sql), rc);
    T result = results.nextOrNull();
    results.close(); //note: if resultiterators complain about being closed twice, hook here
    return (result);
  }

  /** Returns TRUE if the resultset is not empty */
  public boolean exists(CharSequence sql) throws SQLException {
    ResultSet rs = query(sql);
    boolean result = rs.next();
    close(rs);
    return (result);
  }

  
  // ---------------------------------------------------------------------
  //                  Transactions
  // ---------------------------------------------------------------------  
  
  /** indicates whether autocommit was enabled before we switched if off to start a transaction */
  boolean autoCommitWasOn = true;
  
  boolean inTransactionMode = false;

  /** Initiates a transaction by disabling autocommit and enabling transaction mode */
  public void startTransaction() throws InitTransactionSQLException {
    if (!inTransactionMode) {
      try {
        autoCommitWasOn = connection.getAutoCommit();
        if (autoCommitWasOn) connection.setAutoCommit(false);
      } catch (SQLException ex) {
        throw new InitTransactionSQLException("Could not check and disable autocommit \nError was" + ex, ex);
      }
      try {
        originalTransactionMode = connection.getTransactionIsolation();
      } catch (SQLException ex) {
        throw new InitTransactionSQLException("Could not get hold of transaction isolation\nError was" + ex, ex);
      }
      try {
        connection.setTransactionIsolation(defaultTransactionMode);
      } catch (SQLException ex) {
        throw new InitTransactionSQLException("Could not set transaction isolation mode\nError was" + ex, ex);
      }
      inTransactionMode = true;
    }
  }

  /** commits the transaction aggregated so far 
   * if the commit fails the transaction is rolled back!*/
  protected void commitTransaction() throws TransactionSQLException {
    try {
      connection.commit();
    } catch (SQLException ex) {
      CommitTransactionSQLException commitfail = new CommitTransactionSQLException("Could not commit transaction.", ex);
      try {
        resetTransaction();
      } catch (RollbackTransactionSQLException rex) {
        throw new RollbackTransactionSQLException(rex.getMessage(), commitfail);
      }
      throw commitfail;
    }
  }

  /** resets the transaction rolling it back and closing it  */
  public void resetTransaction() throws TransactionSQLException {
    try {
      connection.rollback();
    } catch (SQLException ex2) {
      throw new RollbackTransactionSQLException("Could not rollback transaction.");
    }
    endTransaction();
  }

  /** executes the transaction and switches back from transaction mode into autocommit mode */  
  public void endTransaction() throws TransactionSQLException {
	    if (inTransactionMode) {
	        commitTransaction();
	        try {
	          connection.setTransactionIsolation(originalTransactionMode);
	        } catch (SQLException ex) {
	          throw new TransactionSQLException("Could not shutdown transaction mode\n Error was:" + ex, ex);
	        }
	        try {
	          if (autoCommitWasOn) connection.setAutoCommit(true);
	        } catch (SQLException ex) {
	          throw new StartAutoCommitSQLException("Could not start autocommit\n Error was:" + ex, ex);
	        }
	        inTransactionMode = false;
	      }
  }
  
  /** Please use the version without parameter
   *  @param	flush	deprecated, will be removed 
   *  @Note The flush parameter is deprecated and will be removed as the transaction end always requires a commit */
  @Deprecated  //TODO: remove after a while
  public void endTransaction(@Deprecated boolean flush) throws TransactionSQLException {
	  endTransaction();
  }

  
  // ---------------------------------------------------------------------
  //                  Locking
  // ---------------------------------------------------------------------  

  /** Locks a table in write mode, i.e. other db connections can only read the table, but not write to it */
  public void lockTableWriteAccess(Map<String, String> tableAndAliases) throws SQLException {
    throw new SQLException("Sorry this functionality is not implemented for your database system");
  }

  /** Locks a table in read mode, i.e. only this connection can read or write the table */
  public void lockTableReadAccess(Map<String, String> tableAndAliases) throws SQLException {
    throw new SQLException("Sorry this functionality is not implemented for your database system");
  }

  /** releases all locks the connection holds, commits the current transaction and ends it */
  public void releaseLocksAndEndTransaction() throws SQLException {
    throw new SQLException("Sorry this functionality is not implemented for your database system");
  }

  
  // ---------------------------------------------------------------------
  //                  Describe Queries
  // ---------------------------------------------------------------------  

  /** The minal column width for describe() */
  public static final int MINCOLUMNWIDTH = 3;

  /** The screen width for describe() */
  public static final int SCREENWIDTH = 120;

  /** Appends something to a StringBuilder with a fixed length */
  protected static void appendFixedLen(StringBuilder b, Object o, int len) {
    String s = o == null ? "null" : o.toString();
    if (s.length() > len) s = s.substring(0, len);
    b.append(s);
    for (int i = s.length(); i < len; i++)
      b.append(' ');
  }

  /**
   * Returns a String-representation of a ResultSet, maximally maxrows rows (or
   * all for -1)
   */
  public static String describe(ResultSet r, int maxrows) throws SQLException {
    StringBuilder b = new StringBuilder();
    int columns = r.getMetaData().getColumnCount();
    int width = SCREENWIDTH / columns - 1;
    if (width < MINCOLUMNWIDTH) {
      columns = SCREENWIDTH / (MINCOLUMNWIDTH + 1);
      width = MINCOLUMNWIDTH;
    }
    int screenwidth = (width + 1) * columns;
    for (int column = 1; column <= columns; column++) {
      appendFixedLen(b, r.getMetaData().getColumnLabel(column), width);
      b.append('|');
    }
    b.append('\n');
    for (int i = 0; i < screenwidth; i++)
      b.append('-');
    b.append('\n');
    for (; maxrows != 0; maxrows--) {
      if (!r.next()) {
        for (int i = 0; i < screenwidth; i++)
          b.append('-');
        b.append('\n');
        break;
      }
      for (int column = 1; column <= columns; column++) {
        appendFixedLen(b, r.getObject(column), width);
        b.append('|');
      }
      b.append('\n');
    }
    if (maxrows == 0 && r.next()) b.append("...\n");
    close(r);
    return (b.toString());
  }

  /** Returns a String-representation of a ResultSet */
  public static String describe(ResultSet r) throws SQLException {
    return (describe(r, -1));
  }

  
  
  
  /*****************************************************************************************************************    
   ****                                     Query Generation                                                    ****
   *****************************************************************************************************************/

  /** Returns an SQLType for the given Type as defined in java.sql.Types */
  public SQLType getSQLType(int t) {
    return (type2SQL.get(t));
  }

  /**
   * Returns an SQLType for the given Type as defined in java.sql.Types with a
   * scale
   */
  public SQLType getSQLType(int t, int scale) {
    SQLType s = getSQLType(t);
    s.scale = scale;
    return (s);
  }

  /** Returns an SQLType for the given class */
  public SQLType getSQLType(Class<?> c) {
    return (java2SQL.get(c));
  }

  /** returns the database system specific expression for if-null functionality 
   * i.e. ifnull(a,b) returns b if a is null and a otherwise */
  public String getSQLStmntIFNULL(String a, String b) {
    Announce.error("Your database system class needs to implement this functionality.");
    return "";
  }

  /** Formats an object appropriately (provided that its class is in java2SQL) */
  public String format(Object o) {
    SQLType t = getSQLType(o.getClass());
    if (t == null) {
      t = getSQLType(String.class);
      return t.format(o.toString());
    }else
      return (t.format(o));
  }

  /** Formats an object appropriately (provided that its class is in java2SQL) 
   *  and assigns NULL if the given object is a null pointer */
  public String formatNullToNull(Object o) {
    if (o == null) return "NULL";
    else return format(o);
  }
  


  /** 
   * Produces an SQL fragment casting the given value to the given type   * 
   */
  public String cast(String value, String type) {
    StringBuilder sql = new StringBuilder("CAST(");
    sql.append(value).append(" AS ").append(type).append(")");
    return sql.toString();
  }


  /** Makes an SQL query limited to n results */
  public String limit(String sql, int n) {
    return (sql + " LIMIT " + n);
  }

  /** Makes sure a query response starts at the n-th result */
  public String offset(String sql, int n) {
    return (sql + " OFFSET " + n);
  }
  
  
  /*****************************************************************************************************************    
   ****                                      Table Management                                                   ****
   *****************************************************************************************************************/
  
  /** 
   * Produces an SQL fragment representing an autoincrementing column type
   * s.t. if used during table creation a column can be declared to get by default 
   * an integer value assigned according to an internal sequence counter
   * Example:
   * createTable("tableWithSingleAutoIncrementingIDColumn", "ID", autoincrementColumn()) 
   */
  public String autoincrementColumn() {
    Announce.error("This functionality is not provided for this database type. It may simply lack implementation at the Database class.");
    return null;
  }

  /**
   * Creates or rewrites an SQL table. Attributes is an alternating sequence of
   * a name (String) and a type (from java.sql.Type).
   */
  public void createTable(String name, Object... attributes) throws SQLException {
    Announce.doingDetailed("Creating table " + name);
    try {
      executeUpdate("DROP TABLE " + name);
    } catch (SQLException e) {
      //Announce.warning(e);  //no exception handling on default as an exception might just state that the table is not there, which is perfectly okay when it is created the first time
    }
    StringBuilder b = new StringBuilder("CREATE TABLE ").append(name).append(" (");
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
    Announce.doneDetailed();
  }

  /** checks if a table with the given name exists (or rather whether it can be accessed).
   * @param table  name of the table to be checked 
   * @note if there is any error with the database connection,
   * the function will also return false. */
  public boolean existsTable(String table) {
	  ResultSet rs=null;
    try {    	
      rs = query("SELECT * FROM " + table + " LIMIT 1");      
    } catch (SQLException ex) {    	
    	Announce.debug(ex); 
      return false;
    }
    if(rs!=null)
    	Database.close(rs);
    
    return true;
  }

  /** Creates an index name*/
  public String indexName(String table, String... attributes) {
    StringBuffer sb = new StringBuffer();
    sb.append("I_");
    sb.append(table.hashCode());
    sb.append("_");
    StringBuffer att = new StringBuffer();
    for (int i = 0; i < attributes.length; i++) {
      att.append(attributes[i]);
      att.append("_");
    }
    if (att.length() > 0) {
      att.setLength(att.length() - 1);
    }
    sb.append(att.hashCode());
    return sb.toString().replace("-", "m");
  }



  /** Returns the command to create one index on a table */
  public String createIndexCommand(String table, boolean unique, String... attributes) {
    StringBuilder sql = new StringBuilder("CREATE ");
    if (unique) sql.append("UNIQUE ");
    sql.append("INDEX ");
    sql.append(indexName(table, attributes));
    sql.append(" ON ").append(table).append(" (");
    for (String a : attributes)
      sql.append(a).append(", ");
    sql.setLength(sql.length() - 2);
    sql.append(")");
    return (sql.toString());
  }

  public void createIndex(String table, boolean unique, String... attributes) throws SQLException {
    Announce.doingDetailed("Creating index " + indexName(table, attributes) + " on table " + table);
    String comand = createIndexCommand(table, unique, attributes);
    Announce.debug(comand);
    try {
      executeUpdate("DROP INDEX " + indexName(table, attributes));
    } catch (SQLException e) {
    }
    executeUpdate(comand);
    Announce.doneDetailed();
  }

  /** Creates non-unique single indices on a table */
  public void createIndices(String table, String... attributes) throws SQLException {
    for (String a : attributes) {
      createIndex(table, false, a);
    }
  }

  /** makes the given attributes/columns the primary key of the given table*/
  public void createPrimaryKey(String table, String... attributes) throws SQLException {
    Announce.doingDetailed("Creating primary Key on table " + table);
    StringBuilder sql = new StringBuilder("ALTER TABLE ");
    sql.append(table);
    sql.append(" ADD PRIMARY KEY (");
    for (String a : attributes)
      sql.append(a).append(", ");
    sql.setLength(sql.length() - 2);
    sql.append(")");
    Announce.debug(sql);
    try {
      executeUpdate("ALTER TABLE " + table + " DROP PRIMARY KEY");
    } catch (SQLException e) {
      // throw e; //hook here for exception handling; usually disabled as no primary key may exist when we create the new one (which is not an error) 
    }
    executeUpdate(sql.toString());
    Announce.doneDetailed();
  }
  
  /** creates a view with given name over the query */
  public void createView(String name, String query) throws SQLException {
    Announce.doingDetailed("Creating view " + name);
    Announce.messageDetailed(" with query: "+query);
    
    StringBuilder sql = new StringBuilder("CREATE VIEW ");
    sql.append(name);
    sql.append(" AS (");
    sql.append(query);
    sql.append(")");
    Announce.debug(sql);
    try {
      executeUpdate("DROP VIEW " + name );
    } catch (SQLException e) {
      // throw e; //hook here for exception handling; usually disabled as the view might not yet exist when we want to create it (which is not an error) 
    }
    executeUpdate(sql.toString());
    Announce.doneDetailed();
  }
  
  
  
  /*****************************************************************************************************************    
   ****                                    Import/Export                                                           ****
   *****************************************************************************************************************/
  
  /** Produces a CSV version of the table*/
  public void makeCSV(String table, File output, char separator) throws IOException, SQLException {
    makeCSVForQuery("SELECT * FROM " + table, output, separator);
  }

  /** Produces a CSV version of the table*/
  public void dumpCSV(String table, File output, char separator) throws IOException, SQLException {
    dumpQueryAsCSV("SELECT * FROM " + table, output, separator);
  }

  /** Produces a CSV version of the query*/
  public void makeCSVForQuery(String selectCommand, File output, char separator) throws IOException, SQLException {
    ResultSet r = query(selectCommand);
    Writer out = new UTF8Writer(output);
    int columns = r.getMetaData().getColumnCount();
    for (int column = 1; column <= columns; column++) {
      out.write(r.getMetaData().getColumnLabel(column));
      if (column == columns) out.write("\n");
      else out.write(separator + " ");
    }
    while (r.next()) {
      for (int column = 1; column <= columns; column++) {
        Object o = r.getObject(column);
        out.write(o == null ? "null" : o.toString());
        if (column == columns) out.write("\n");
        else out.write(separator + " ");
      }
    }
    close(r);
    out.close();
  }

  /** Produces a CSV version of the query*/
  public void dumpQueryAsCSV(String selectCommand, File output, char separator) throws IOException, SQLException {
    ResultSet r = query(selectCommand);
    ResultSetMetaData meta = r.getMetaData();
    int numCols = meta.getColumnCount();
    List<String> labels = new ArrayList<String>();
    for (int i = 1; i <= numCols; i++)
      labels.add(meta.getColumnLabel(i));
    CSVFile csv = new CSVFile(output, false, separator + "", labels);
    while (r.next()) {
      Object[] cols = new String[numCols];
      for (int column = 0; column < numCols; column++)
        cols[column] = r.getObject(column + 1);
      csv.write(cols);
    }
    close(r);
    csv.close();
  }

  /** Loads a CSV file into a table*/
  public void loadCSV(String table, File input, boolean clearTable, char separator) throws IOException, SQLException {
    if (clearTable) executeUpdate("DELETE FROM " + table);
    Inserter bulki = newInserter(table);
    CSVLines csv = new CSVLines(input);
    if (csv.numColumns() != null && csv.numColumns() != bulki.numColumns()) {
      throw new SQLException("File " + input.getName() + " has " + csv.numColumns() + " columns, but table " + table + " has " + bulki.numColumns());
    }
    for (List<String> values : csv) {
      if (values.size() != bulki.numColumns()) {
        Announce.warning("Line cannot be read from file", input.getName(), "into table", table, ":\n", values);
        continue;
      }
      bulki.insert(values);
    }
    bulki.close();
  }

  
  
  /*****************************************************************************************************************    
   ****                                           Misc.                                                         ****
   *****************************************************************************************************************/

  public String toString() {
    return (description);
  }


  /** Runs a user-interface and closes */
  public void runInterface() {
    Announce.message("Connected to", this);
    while (true) {
      D.p("Enter an SQL query (possibly of multiple lines), followed by a blank line (or just a blank line to quit):");
      StringBuilder sql = new StringBuilder();
      String s;
      while ((s = D.r()).length() != 0)
        sql.append(s).append("\n");
      if (sql.length() == 0) break;
      sql.setLength(sql.length() - 1);
      Announce.doing("Querying database");
      if (sql.length() == 0) break;
      try {
        ResultSet result = query(sql.toString());
        Announce.done();
        if (result != null) D.p(describe(result, 50));
      } catch (SQLException e) {
        Announce.failed();
        e.printStackTrace(System.err);
        Announce.message("\n\n... but don't give up, try again!");
      }
    }
    Announce.doing("Closing database");
    close();
    Announce.done();
  }

  
  

  
  /** Test routine */
  public static void main(String[] args) throws Exception {
    new PostgresDatabase("postgres", "postgres", null, null, null).runInterface();
    //    System.out.println("Does table 'facts' exist:"+ new PostgresDatabase("postgres", "postgres", "yago", null, null).existsTable("facts"));
    //    System.out.println("Does table 'factssss' exist:"+ new PostgresDatabase("postgres", "postgres", "yago", null, null).existsTable("factssss"));    
  }
  
  
  
  
  
  /*****************************************************************************************************************    
   ****                                      Inserters                                                          ****
   *****************************************************************************************************************/
  
  
  /** Represents a bulk loader*/
  public class Inserter implements Closeable {

    /** the currently cached values */
    protected List<List<Object>> values=new ArrayList<List<Object>>();

    /** Table where the data will be inserted*/
    protected String tableName;
    
    /** The locally prepared Query */
    String query=null;

    /** Column types*/
    protected SQLType[] columnTypes;

    /** Tells after how many commands we will flush the batch*/
    private int batchThreshold = 1000;

    /** tells whether the inserter is already closed */
    private boolean closed = false;
     
 
    
    /*****************************************************************************************************************    
     ****                      Inserter - Initiation and Shutdown                                                 ****
     *****************************************************************************************************************/
    
    
    /** Creates a bulk loader*/
    public Inserter(String table) throws SQLException {
      inserters.add(this);
      setTargetTable(table);
    }

    /** Creates a bulk loader for a table with column types given by Java classes*/
    public Inserter(String table, Class<?>... columnTypes) throws SQLException {
      inserters.add(this);
      setTargetTable(table, columnTypes);    
    }
    
    

    /** Creates a bulk loader with column types from java.sql.Type */
    public Inserter(String table, int... columnTypes) throws SQLException {
      inserters.add(this);
      setTargetTable(table, columnTypes);   
    }
    
    /** Creates a bulk loader for specific coloumns of a table with column types given by their names and Java classes*/
    public Inserter(String table, String[] colnames, Class<?>[] coltypes) throws SQLException {
      inserters.add(this);
      setTargetTable(table, colnames, coltypes);
    }
    
    

    /** Flushes and closes */
    @Override
    public synchronized void close() {
      if (closed) // closing once is enough
      return;
      try {
        flush();
      } catch (SQLException e) {
        Announce.error(e);
      }
      inserters.remove(this);
      closed = true;
    }


    @Override
    protected void finalize() {
      close();   
    }

    
    
    /*****************************************************************************************************************    
     ****                          Inserter - Attribute Accessors                                                 ****
     *****************************************************************************************************************/

    public void setBatchThreshold(int size) {
      batchThreshold = size;
    }
    
    /** returns the batch size set (i.e. after how many entries gathered the inserter should be flushed) */
    public int getBatchThreshold(){
      return batchThreshold;
    }
    
    /** Returns the table name*/
    public String getTableName() {
     return tableName;
    }
    
    /** returns the number of entries gathered */
    public int getBatchSize(){
      return values.size();
    }
    
    
    /** Returns the number of columns*/
    public int numColumns() {
      return (columnTypes.length);
    }
    

    /*****************************************************************************************************************    
     ****                         Inserter - Data insertion                                                       ****
     *****************************************************************************************************************/

    // ---------------------------------------------------------------------
    //           Preparation
    // ---------------------------------------------------------------------


    /** Sets the target table into which values shall be inserted */
    protected void setTargetTable(String table) throws SQLException {
      ResultSet r = query(limit("SELECT * FROM " + table, 1));
      ResultSetMetaData meta = r.getMetaData();
      columnTypes = new SQLType[meta.getColumnCount()];
      for (int i = 0; i < columnTypes.length; i++) {
        columnTypes[i] = getSQLType(meta.getColumnType(i + 1));
      }
      Database.close(r);
      tableName = table;
      table = "INSERT INTO " + table + " VALUES(";
      for (int i = 0; i < columnTypes.length - 1; i++)
        table = table + "?, ";
      table += "?)";
      query=table;
    }

    /** Sets the target table into which values shall be inserted 
     *  with the types of the table columns explicitly given */
    protected void setTargetTable(String table, Class<?>... columnTypes) throws SQLException {
      this.columnTypes = new SQLType[columnTypes.length];
      for (int i = 0; i < columnTypes.length; i++) {
        this.columnTypes[i] = getSQLType(columnTypes[i]);
      }
      tableName = table;
      table = "INSERT INTO " + table + " VALUES(";
      for (int i = 0; i < columnTypes.length - 1; i++)
        table = table + "?, ";
      table += "?)";
      query=table;
    }

    /** Sets the target table into which values shall be inserted 
     *   with the types of the table columns explicitly given (as java.sql.Type types)*/
    protected void setTargetTable(String table, int... columnTypes) throws SQLException {
      this.columnTypes = new SQLType[columnTypes.length];
      for (int i = 0; i < columnTypes.length; i++) {
        this.columnTypes[i] = getSQLType(columnTypes[i]);
      }
      tableName = table;
      table = "INSERT INTO " + table + " VALUES(";
      for (int i = 0; i < columnTypes.length - 1; i++)
        table = table + "?, ";
      table += "?)";
      query=table;
    }

    protected void setTargetTable(String table, String[] colnames, Class<?>[] coltypes) throws SQLException {
      if(colnames.length!=coltypes.length)
        throw new SQLException("Column types and names do not match.");
      this.columnTypes = new SQLType[colnames.length];
      int i=0;
      for (Class<?> col:coltypes) {
        this.columnTypes[i] = getSQLType(col);
        i++;
      }
      tableName = table;
      table = "INSERT INTO " + table + "(";
      table+=StringModifier.implode(colnames, ",");
      table+=") VALUES(";
      for (i = 0; i < columnTypes.length - 1; i++)
        table = table + "?, ";
      table += "?)";
      query =table;
    }
    
    // ---------------------------------------------------------------------
    //           Inserts
    // ---------------------------------------------------------------------


    /** Inserts a row*/
    public void insert(List<Object> row) throws SQLException {
      this.values.add(row);
      if (values.size() % batchThreshold == 0) flush();
    }
    
    /** Inserts a row*/
    public void insert(Object... values) throws SQLException {
      insert(Arrays.asList(values));
    }


    /** Flushes the batch*/
    public synchronized void flush() throws SQLException {
      
      if(values.isEmpty())
        return;
      List<List<Object>> oldBatch=values;
      values=new ArrayList<List<Object>>();
      try {
        flush(oldBatch);     
      } catch (SQLException e) {  

        String details = e.getNextException() == null ? "" : e.getNextException().getMessage();
        SQLException ex= new SQLException(e.getMessage() + "\n\n" + details);
        try{
          attemptReconnect(ex, autoReconnectOnUpdate);        
          flush(oldBatch);
        } catch (SQLException ex2) {
          values.addAll(oldBatch);
          throw ex2;          
        }                
      }      
    }

    protected void flush(List<List<Object>> batch) throws SQLException{
      PreparedStatement preparedStatement = getConnection().prepareStatement(query);
      for(List<Object> row: batch){
        try {
          for (int i = 0; i < row.size(); i++) {
            preparedStatement.setObject(i + 1, row.get(i), columnTypes[i].getTypeCode());
          }
          preparedStatement.addBatch();
        } catch (SQLException e) {
          throw new SQLException("Bulk-insert into " + tableName + " " + row + "\n" + e.getMessage());
        }
      }      
      preparedStatement.executeBatch();
      preparedStatement.clearBatch();
      Database.close(preparedStatement);      
    }
  }



  /** Returns an inserter for a table with specific column types*/
  public Inserter newInserter(String table) throws SQLException {
    return (new Inserter(table));
  }

  /** Returns an inserter for a table with specific column types*/
  public Inserter newInserter(String table, Class<?>... argumentTypes) throws SQLException {
    return (new Inserter(table, argumentTypes));
  }

  /** Returns an inserter for a table with specific column types given as java.sql.Type constants*/
  public Inserter newInserter(String table, int... argumentTypes) throws SQLException {
    return (new Inserter(table, argumentTypes));
  }


  
  
  /*****************************************************************************************************************    
   ****                                    Exceptions                                                           ****
   *****************************************************************************************************************/
  
  public static class ConnectionBrokenSQLException extends SQLException{
    private static final long serialVersionUID = 1L;
    public ConnectionBrokenSQLException(){
      super();
    }
    public ConnectionBrokenSQLException(String message){
      super(message);
    }
    public ConnectionBrokenSQLException(Throwable cause){
      super(cause);
    }         
    public ConnectionBrokenSQLException(String message, Throwable cause){
      super(message,cause);
    }       
  }

  public static class TransactionSQLException extends SQLException {

    private static final long serialVersionUID = 1L;

    public TransactionSQLException() {
      super();
    }

    public TransactionSQLException(String message) {
      super(message);
    }

    public TransactionSQLException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class InitTransactionSQLException extends TransactionSQLException {

    private static final long serialVersionUID = 1L;

    public InitTransactionSQLException() {
      super();
    }

    public InitTransactionSQLException(String message) {
      super(message);
    }

    public InitTransactionSQLException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class CommitTransactionSQLException extends TransactionSQLException {

    private static final long serialVersionUID = 1L;

    public CommitTransactionSQLException() {
      super();
    }

    public CommitTransactionSQLException(String message) {
      super(message);
    }

    public CommitTransactionSQLException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class RollbackTransactionSQLException extends TransactionSQLException {

    private static final long serialVersionUID = 1L;

    public RollbackTransactionSQLException() {
      super();
    }

    public RollbackTransactionSQLException(String message) {
      super(message);
    }

    public RollbackTransactionSQLException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class StartAutoCommitSQLException extends TransactionSQLException {

    private static final long serialVersionUID = 1L;

    public StartAutoCommitSQLException() {
      super();
    }

    public StartAutoCommitSQLException(String message) {
      super(message);
    }

    public StartAutoCommitSQLException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
