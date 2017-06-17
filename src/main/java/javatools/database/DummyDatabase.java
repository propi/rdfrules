package javatools.database;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.filehandlers.FileLines;

/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * This class provides a dummy database that knows just one single table.
 * All queries run on this table and the WHERE clause is ignored.
 * Example:<PRE>
   Database d=new DummyDatabase(Arrays.asList("arg1","relation","arg2"),
				"Albert_Einstein","bornOnDate","1879-03-14",
				"Elvis_Presley","bornIn","Tupelo"
				);
  </PRE>
 */

public class DummyDatabase extends Database {
    /** Holds the table */
	public List<List<String>> columns = new ArrayList<List<String>>();
    /** Holds the column names */
	public List<String> columnNames = new ArrayList<String>();

	/** Number of rows*/
	public int numRows=0;
	
	/** Executes an SQL update query, returns the number of rows added/deleted */
	public int executeUpdate(CharSequence sqlcs) throws SQLException {
		Announce.debug(sqlcs);
		return (0);
	}

	/** Creates a dummy database */
	public DummyDatabase() {
		description="Empty dummy database";
	}

	/** Creates a dummy database */
	public DummyDatabase(List<String> columnNames, List<List<String>> columns) {
		this.columns=columns;
		for(String columnName : columnNames) this.columnNames.add(columnName.toLowerCase());
		if(columns.size()!=0) numRows=columns.get(0).size();
		description="Dummy database with schema "+columnNames+" and "+numRows+" rows";
	}

	/** Creates a dummy database */
	public DummyDatabase(List<String> columnNames, String... valuesAsRows) {
		for(String columnName : columnNames) {
			this.columnNames.add(columnName.toLowerCase());
			columns.add(new ArrayList<String>());
		}
		int col=0;
		for(String value : valuesAsRows) {
			columns.get(col).add(value);
			col++;
			if(col%columnNames.size()==0) {
				numRows++;
				col=0;
			}
		}
		description="Dummy database with schema "+columnNames+" and "+numRows+" rows";
	}
	
	/** Creates a dummy database with values from a TSV file 
	 * @throws IOException */
	public DummyDatabase(List<String> columnNames, File values) throws SQLException {
		for(String columnName : columnNames) {
			this.columnNames.add(columnName.toLowerCase());
			columns.add(new ArrayList<String>());
		}
		int col=0;
		try {
			for(String line : new FileLines(values,"Loading "+values)) {
				String[] split=line.split("\t");
				for(int i=0;i<columns.size();i++) {
					if(split.length>i)
					columns.get(col).add(split[i]);
					else columns.get(col).add(null);
				}
					numRows++;
			}
		} catch (IOException e) {
			throw new SQLException(e);
		}
		description="Dummy database with schema "+columnNames+" and "+numRows+" rows from file "+values;
	}
	
	/** Executes a query */
	public ResultSet query(CharSequence sqlcs, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		String sql = prepareQuery(sqlcs.toString());
		if (sql.toUpperCase().startsWith("INSERT")
				|| sql.toUpperCase().startsWith("UPDATE")
				|| sql.toUpperCase().startsWith("DELETE")
				|| sql.toUpperCase().startsWith("CREATE")
				|| sql.toUpperCase().startsWith("DROP")
				|| sql.toUpperCase().startsWith("ALTER")) {
			executeUpdate(sql);
			return (null);
		}
		Matcher m=Pattern.compile("(?i)SELECT (.*) FROM.*").matcher(sqlcs);
		if(!m.matches()) throw new SQLException("Unsupported query "+sqlcs);
		if(m.group(1).equals("*")) return(new DummyResultSet(columns));
		List<List<String>> cols = new ArrayList<List<String>>();
		for(String col : m.group(1).toLowerCase().split(",")) {
			int pos=columnNames.indexOf(col.trim());
			if(pos==-1) cols.add(Arrays.asList(new String[numRows]));
			else cols.add(columns.get(pos));
		}
		return (new DummyResultSet(cols));
	}

	/** Wraps just the data */
	public static class DummyResultSet implements ResultSet {

		/** Holds the table */
		public List<List<String>> columns = new ArrayList<List<String>>();
		
		/** Points to the current row */
		public int index = -1;

		/** Number of rows*/
		public int numRows=0;
		
		/** Constructs a result set */
		public DummyResultSet(List<List<String>> columns) {
			this.columns=columns;
			if(columns.size()!=0) numRows=columns.get(0).size();
		}

		@Override
		public boolean absolute(int row) throws SQLException {
			if (row >= numRows)
				return (false);
			index = row;
			return true;
		}

		@Override
		public void afterLast() throws SQLException {

		}

		@Override
		public void beforeFirst() throws SQLException {
			index = -1;
		}

		@Override
		public void cancelRowUpdates() throws SQLException {
		}

		@Override
		public void clearWarnings() throws SQLException {
		}

		@Override
		public void close() throws SQLException {
		}

		@Override
		public void deleteRow() throws SQLException {
		}

		@Override
		public int findColumn(String columnLabel) throws SQLException {
			return -1;
		}

		@Override
		public boolean first() throws SQLException {
			if (numRows == 0)
				return false;
			index = 0;
			return (true);
		}

		@Override
		public Array getArray(int columnIndex) throws SQLException {
			return null;

		}

		@Override
		public Array getArray(String columnLabel) throws SQLException {
			return null;

		}

		@Override
		public InputStream getAsciiStream(int columnIndex) throws SQLException {
			return null;

		}

		@Override
		public InputStream getAsciiStream(String columnLabel)
				throws SQLException {
			return null;
		}

		@Override
		public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
			return null;
		}

		@Override
		public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
			return null;
		}

		@Override
		public BigDecimal getBigDecimal(int columnIndex, int scale)
				throws SQLException {

			return null;
		}

		@Override
		public BigDecimal getBigDecimal(String columnLabel, int scale)
				throws SQLException {

			return null;
		}

		@Override
		public InputStream getBinaryStream(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public InputStream getBinaryStream(String columnLabel)
				throws SQLException {

			return null;
		}

		@Override
		public Blob getBlob(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Blob getBlob(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public boolean getBoolean(int columnIndex) throws SQLException {
			return (columns.get(columnIndex).get(index).equals("true")
					|| columns.get(columnIndex).get(index).equals("1") || columns.get(columnIndex).get(index).equals("yes"));
		}

		@Override
		public boolean getBoolean(String columnLabel) throws SQLException {

			return false;
		}

		@Override
		public byte getByte(int columnIndex) throws SQLException {
			return (Byte.parseByte(columns.get(columnIndex).get(index)));

		}

		@Override
		public byte getByte(String columnLabel) throws SQLException {

			return 0;
		}

		@Override
		public byte[] getBytes(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public byte[] getBytes(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Reader getCharacterStream(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Reader getCharacterStream(String columnLabel)
				throws SQLException {

			return null;
		}

		@Override
		public Clob getClob(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Clob getClob(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public int getConcurrency() throws SQLException {

			return 0;
		}

		@Override
		public String getCursorName() throws SQLException {

			return null;
		}

		@Override
		public Date getDate(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Date getDate(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Date getDate(int columnIndex, Calendar cal) throws SQLException {

			return null;
		}

		@Override
		public Date getDate(String columnLabel, Calendar cal)
				throws SQLException {

			return null;
		}

		@Override
		public double getDouble(int columnIndex) throws SQLException {
			return (Double.parseDouble(columns.get(columnIndex).get(index)));

		}

		@Override
		public double getDouble(String columnLabel) throws SQLException {

			return 0;
		}

		@Override
		public int getFetchDirection() throws SQLException {

			return 0;
		}

		@Override
		public int getFetchSize() throws SQLException {

			return 0;
		}

		@Override
		public float getFloat(int columnIndex) throws SQLException {
			return (Float.parseFloat(columns.get(columnIndex).get(index)));

		}

		@Override
		public float getFloat(String columnLabel) throws SQLException {

			return 0;
		}

		@Override
		public int getHoldability() throws SQLException {

			return 0;
		}

		@Override
		public int getInt(int columnIndex) throws SQLException {
			return (Integer.parseInt(columns.get(columnIndex).get(index)));
		}

		@Override
		public int getInt(String columnLabel) throws SQLException {

			return 0;
		}

		@Override
		public long getLong(int columnIndex) throws SQLException {
			return (Long.parseLong(columns.get(columnIndex).get(index)));

		}

		@Override
		public long getLong(String columnLabel) throws SQLException {

			return 0;
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {

			return new ResultSetMetaData() {

				@Override
				public String getCatalogName(int column) throws SQLException {
					return null;
				}

				@Override
				public String getColumnClassName(int column)
						throws SQLException {
					return null;
				}

				@Override
				public int getColumnCount() throws SQLException {
					return(columns.size());
				}

				@Override
				public int getColumnDisplaySize(int column) throws SQLException {
					return 20;
				}

				@Override
				public String getColumnLabel(int column) throws SQLException {
					return "Column"+column;
				}

				@Override
				public String getColumnName(int column) throws SQLException {
					return "Column"+column;
				}

				@Override
				public int getColumnType(int column) throws SQLException {
					return Types.VARCHAR;
				}

				@Override
				public String getColumnTypeName(int column) throws SQLException {
					return "VARCHAR";
				}

				@Override
				public int getPrecision(int column) throws SQLException {
					return 0;
				}

				@Override
				public int getScale(int column) throws SQLException {
					return 0;
				}

				@Override
				public String getSchemaName(int column) throws SQLException {
					return null;
				}

				@Override
				public String getTableName(int column) throws SQLException {
					return "DummyTable";
				}

				@Override
				public boolean isAutoIncrement(int column) throws SQLException {
					return false;
				}

				@Override
				public boolean isCaseSensitive(int column) throws SQLException {
					return true;
				}

				@Override
				public boolean isCurrency(int column) throws SQLException {
					return false;
				}

				@Override
				public boolean isDefinitelyWritable(int column)
						throws SQLException {
					return false;
				}

				@Override
				public int isNullable(int column) throws SQLException {
					return 0;
				}

				@Override
				public boolean isReadOnly(int column) throws SQLException {
					return true;
				}

				@Override
				public boolean isSearchable(int column) throws SQLException {
					return false;
				}

				@Override
				public boolean isSigned(int column) throws SQLException {
					return false;
				}

				@Override
				public boolean isWritable(int column) throws SQLException {
					return false;
				}

				@Override
				public boolean isWrapperFor(Class<?> iface) throws SQLException {
					return false;
				}

				@Override
				public <T> T unwrap(Class<T> iface) throws SQLException {
					return null;
				}
				
			};
		}

		@Override
		public Reader getNCharacterStream(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Reader getNCharacterStream(String columnLabel)
				throws SQLException {

			return null;
		}

		@Override
		public NClob getNClob(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public NClob getNClob(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public String getNString(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public String getNString(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Object getObject(int columnIndex) throws SQLException {
			return (columns.get(columnIndex-1)
					.get(index));

		}

		@Override
		public Object getObject(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Object getObject(int columnIndex, Map<String, Class<?>> map)
				throws SQLException {

			return null;
		}

		@Override
		public Object getObject(String columnLabel, Map<String, Class<?>> map)
				throws SQLException {

			return null;
		}

		@Override
		public Ref getRef(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Ref getRef(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public int getRow() throws SQLException {

			return index;
		}

		@Override
		public RowId getRowId(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public RowId getRowId(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public SQLXML getSQLXML(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public SQLXML getSQLXML(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public short getShort(int columnIndex) throws SQLException {
			return (Short.parseShort(columns.get(columnIndex-1).get(index)));

		}

		@Override
		public short getShort(String columnLabel) throws SQLException {

			return 0;
		}

		@Override
		public Statement getStatement() throws SQLException {

			return null;
		}

		@Override
		public String getString(int columnIndex) throws SQLException {

			return columns.get(columnIndex-1).get(index);
		}

		@Override
		public String getString(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Time getTime(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Time getTime(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Time getTime(int columnIndex, Calendar cal) throws SQLException {

			return null;
		}

		@Override
		public Time getTime(String columnLabel, Calendar cal)
				throws SQLException {

			return null;
		}

		@Override
		public Timestamp getTimestamp(int columnIndex) throws SQLException {

			return null;
		}

		@Override
		public Timestamp getTimestamp(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public Timestamp getTimestamp(int columnIndex, Calendar cal)
				throws SQLException {

			return null;
		}

		@Override
		public Timestamp getTimestamp(String columnLabel, Calendar cal)
				throws SQLException {

			return null;
		}

		@Override
		public int getType() throws SQLException {

			return 0;
		}

		@Override
		public URL getURL(int columnIndex) throws SQLException {

			try {
				return new URL(columns.get(columnIndex-1).get(index));
			} catch (MalformedURLException e) {
				throw new SQLException(e);
			}

		}

		@Override
		public URL getURL(String columnLabel) throws SQLException {

			return null;
		}

		@Override
		public InputStream getUnicodeStream(String columnLabel)
				throws SQLException {

			return null;
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {

			return null;
		}

		@Override
		public void insertRow() throws SQLException {

		}

		@Override
		public boolean isAfterLast() throws SQLException {

			return false;
		}

		@Override
		public boolean isBeforeFirst() throws SQLException {

			return false;
		}

		@Override
		public boolean isClosed() throws SQLException {

			return false;
		}

		@Override
		public boolean isFirst() throws SQLException {

			return index == 0;
		}

		@Override
		public boolean isLast() throws SQLException {

			return index == columns.size() - 1;
		}

		@Override
		public boolean last() throws SQLException {

			return index == columns.size() - 1;
		}

		@Override
		public void moveToCurrentRow() throws SQLException {

		}

		@Override
		public void moveToInsertRow() throws SQLException {

		}

		@Override
		public boolean next() throws SQLException {
			index++;
			return index < numRows;
		}

		@Override
		public boolean previous() throws SQLException {
			if (index < 0)
				return false;
			index--;
			return (true);
		}

		@Override
		public void refreshRow() throws SQLException {

		}

		@Override
		public boolean relative(int rows) throws SQLException {

			return false;
		}

		@Override
		public boolean rowDeleted() throws SQLException {

			return false;
		}

		@Override
		public boolean rowInserted() throws SQLException {

			return false;
		}

		@Override
		public boolean rowUpdated() throws SQLException {

			return false;
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {

		}

		@Override
		public void setFetchSize(int rows) throws SQLException {

		}

		@Override
		public void updateArray(int columnIndex, Array x) throws SQLException {

		}

		@Override
		public void updateArray(String columnLabel, Array x)
				throws SQLException {

		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x)
				throws SQLException {

		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x)
				throws SQLException {

		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x, int length)
				throws SQLException {

		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x,
				int length) throws SQLException {

		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x,
				long length) throws SQLException {

		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x,
				long length) throws SQLException {

		}

		@Override
		public void updateBigDecimal(int columnIndex, BigDecimal x)
				throws SQLException {

		}

		@Override
		public void updateBigDecimal(String columnLabel, BigDecimal x)
				throws SQLException {

		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x)
				throws SQLException {

		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x)
				throws SQLException {

		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x,
				int length) throws SQLException {

		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x,
				int length) throws SQLException {

		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x,
				long length) throws SQLException {

		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x,
				long length) throws SQLException {

		}

		@Override
		public void updateBlob(int columnIndex, Blob x) throws SQLException {

		}

		@Override
		public void updateBlob(String columnLabel, Blob x) throws SQLException {

		}

		@Override
		public void updateBlob(int columnIndex, InputStream inputStream)
				throws SQLException {

		}

		@Override
		public void updateBlob(String columnLabel, InputStream inputStream)
				throws SQLException {

		}

		@Override
		public void updateBlob(int columnIndex, InputStream inputStream,
				long length) throws SQLException {

		}

		@Override
		public void updateBlob(String columnLabel, InputStream inputStream,
				long length) throws SQLException {

		}

		@Override
		public void updateBoolean(int columnIndex, boolean x)
				throws SQLException {

		}

		@Override
		public void updateBoolean(String columnLabel, boolean x)
				throws SQLException {

		}

		@Override
		public void updateByte(int columnIndex, byte x) throws SQLException {

		}

		@Override
		public void updateByte(String columnLabel, byte x) throws SQLException {

		}

		@Override
		public void updateBytes(int columnIndex, byte[] x) throws SQLException {

		}

		@Override
		public void updateBytes(String columnLabel, byte[] x)
				throws SQLException {

		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x)
				throws SQLException {

		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader)
				throws SQLException {

		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x, int length)
				throws SQLException {

		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader,
				int length) throws SQLException {

		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x, long length)
				throws SQLException {

		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader,
				long length) throws SQLException {

		}

		@Override
		public void updateClob(int columnIndex, Clob x) throws SQLException {

		}

		@Override
		public void updateClob(String columnLabel, Clob x) throws SQLException {

		}

		@Override
		public void updateClob(int columnIndex, Reader reader)
				throws SQLException {

		}

		@Override
		public void updateClob(String columnLabel, Reader reader)
				throws SQLException {

		}

		@Override
		public void updateClob(int columnIndex, Reader reader, long length)
				throws SQLException {

		}

		@Override
		public void updateClob(String columnLabel, Reader reader, long length)
				throws SQLException {

		}

		@Override
		public void updateDate(int columnIndex, Date x) throws SQLException {

		}

		@Override
		public void updateDate(String columnLabel, Date x) throws SQLException {

		}

		@Override
		public void updateDouble(int columnIndex, double x) throws SQLException {

		}

		@Override
		public void updateDouble(String columnLabel, double x)
				throws SQLException {

		}

		@Override
		public void updateFloat(int columnIndex, float x) throws SQLException {

		}

		@Override
		public void updateFloat(String columnLabel, float x)
				throws SQLException {

		}

		@Override
		public void updateInt(int columnIndex, int x) throws SQLException {

		}

		@Override
		public void updateInt(String columnLabel, int x) throws SQLException {

		}

		@Override
		public void updateLong(int columnIndex, long x) throws SQLException {

		}

		@Override
		public void updateLong(String columnLabel, long x) throws SQLException {

		}

		@Override
		public void updateNCharacterStream(int columnIndex, Reader x)
				throws SQLException {

		}

		@Override
		public void updateNCharacterStream(String columnLabel, Reader reader)
				throws SQLException {

		}

		@Override
		public void updateNCharacterStream(int columnIndex, Reader x,
				long length) throws SQLException {

		}

		@Override
		public void updateNCharacterStream(String columnLabel, Reader reader,
				long length) throws SQLException {

		}

		@Override
		public void updateNClob(int columnIndex, NClob nClob)
				throws SQLException {

		}

		@Override
		public void updateNClob(String columnLabel, NClob nClob)
				throws SQLException {

		}

		@Override
		public void updateNClob(int columnIndex, Reader reader)
				throws SQLException {

		}

		@Override
		public void updateNClob(String columnLabel, Reader reader)
				throws SQLException {

		}

		@Override
		public void updateNClob(int columnIndex, Reader reader, long length)
				throws SQLException {

		}

		@Override
		public void updateNClob(String columnLabel, Reader reader, long length)
				throws SQLException {

		}

		@Override
		public void updateNString(int columnIndex, String nString)
				throws SQLException {

		}

		@Override
		public void updateNString(String columnLabel, String nString)
				throws SQLException {

		}

		@Override
		public void updateNull(int columnIndex) throws SQLException {

		}

		@Override
		public void updateNull(String columnLabel) throws SQLException {

		}

		@Override
		public void updateObject(int columnIndex, Object x) throws SQLException {

		}

		@Override
		public void updateObject(String columnLabel, Object x)
				throws SQLException {

		}

		@Override
		public void updateObject(int columnIndex, Object x, int scaleOrLength)
				throws SQLException {

		}

		@Override
		public void updateObject(String columnLabel, Object x, int scaleOrLength)
				throws SQLException {

		}

		@Override
		public void updateRef(int columnIndex, Ref x) throws SQLException {

		}

		@Override
		public void updateRef(String columnLabel, Ref x) throws SQLException {

		}

		@Override
		public void updateRow() throws SQLException {

		}

		@Override
		public void updateRowId(int columnIndex, RowId x) throws SQLException {

		}

		@Override
		public void updateRowId(String columnLabel, RowId x)
				throws SQLException {

		}

		@Override
		public void updateSQLXML(int columnIndex, SQLXML xmlObject)
				throws SQLException {

		}

		@Override
		public void updateSQLXML(String columnLabel, SQLXML xmlObject)
				throws SQLException {

		}

		@Override
		public void updateShort(int columnIndex, short x) throws SQLException {

		}

		@Override
		public void updateShort(String columnLabel, short x)
				throws SQLException {

		}

		@Override
		public void updateString(int columnIndex, String x) throws SQLException {

		}

		@Override
		public void updateString(String columnLabel, String x)
				throws SQLException {

		}

		@Override
		public void updateTime(int columnIndex, Time x) throws SQLException {

		}

		@Override
		public void updateTime(String columnLabel, Time x) throws SQLException {

		}

		@Override
		public void updateTimestamp(int columnIndex, Timestamp x)
				throws SQLException {

		}

		@Override
		public void updateTimestamp(String columnLabel, Timestamp x)
				throws SQLException {

		}

		@Override
		public boolean wasNull() throws SQLException {

			return false;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {

			return false;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {

			return null;
		}

		public <T> T getObject(int arg0, Class<T> arg1) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public <T> T getObject(String arg0, Class<T> arg1) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
      // TODO Auto-generated method stub
      return null;
    }

	}
	
	public static void main(String[] args) {
		Database d=new DummyDatabase(Arrays.asList("arg1","relation","arg2"),
				"Albert_Einstein","bornOnDate","1879-03-14",
				"Elvis_Presley","bornIn","Tupelo"
				);
		d.runInterface();
	}

  @Override
  public void connect() throws SQLException {
    // TODO Auto-generated method stub
    
  }
}
