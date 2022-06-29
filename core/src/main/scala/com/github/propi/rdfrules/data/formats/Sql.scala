package com.github.propi.rdfrules.data.formats

import com.github.propi.rdfrules.data.TripleItem.Uri
import com.github.propi.rdfrules.data.formats.Sql.{Name, Row, Table}
import com.github.propi.rdfrules.data.{Quad, RdfReader, RdfSource, RdfWriter, Triple, TripleItem}
import com.github.propi.rdfrules.utils.BasicExtractors.{AnyToBoolean, AnyToDouble, AnyToInt}
import com.github.propi.rdfrules.utils.InputStreamBuilder
import net.sf.jsqlparser.expression.operators.relational.{ExpressionList, MultiExpressionList}
import net.sf.jsqlparser.parser.{CCJSqlParser, CCJSqlParserConstants, StreamProvider}
import net.sf.jsqlparser.statement.create.table
import net.sf.jsqlparser.statement.create.table.{ColDataType, ColumnDefinition, CreateTable, ForeignKeyIndex}
import net.sf.jsqlparser.statement.insert.Insert

import java.io.BufferedInputStream
import java.net.URLEncoder
import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 25. 9. 2019.
  */
trait Sql {

  implicit def sqlReader(rdfSource: RdfSource.Sql.type): RdfReader = (inputStreamBuilder: InputStreamBuilder) => (f: Quad => Unit) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    try {
      val parser = new CCJSqlParser(new StreamProvider(is, "UTF-8"))
      parser.setErrorRecovery(true)
      val stream = Iterator.continually {
        val stmt = parser.SingleStatement()
        if (parser.getToken(1).kind == CCJSqlParserConstants.ST_SEMICOLON) parser.getNextToken
        stmt
      }.takeWhile(_ => parser.getToken(1).kind != CCJSqlParserConstants.EOF).filter(_ != null)
      stream.foldLeft(Map.empty[Name, Table]) { (metadata, stmt) =>
        stmt match {
          case createTable: CreateTable =>
            val table = Table(createTable)
            metadata + (table.name -> table)
          case insert: Insert =>
            Row(insert, metadata).flatMap(_.toTriples(metadata)).foreach(x => f(x.toQuad))
            metadata
          case _ => metadata
        }
      }
    } finally {
      is.close()
    }
  }

  implicit def sqlWriter(rdfSource: RdfSource.Sql.type): RdfWriter = ???

}


object Sql {

  case class Name private(value: String) {
    override def toString: String = value
  }

  implicit def stringToName(x: String): Name = Name(x.replaceAll("^`|`$", ""))

  sealed trait AttributeType

  object AttributeType {

    case object Integer extends AttributeType

    case object Double extends AttributeType

    case object Text extends AttributeType

    case object Boolean extends AttributeType

    def apply(colDataType: ColDataType): AttributeType = colDataType.getDataType.toUpperCase match {
      case "BIT" | "BYTE" | "SINGLE" | "TINYINT" | "SMALLINT" | "MEDIUMINT" | "INT" | "INTEGER" => Integer
      case "BOOL" | "BOOLEAN" => Boolean
      case "BIGINT" | "FLOAT" | "DOUBLE" | "DECIMAL" | "NUMERIC" | "REAL" | "LONG" | "NUMBER" => Double
      case _ => Text
    }

  }

  sealed trait Index

  object Index {

    case object PrimaryKey extends Index

    case class ForeignKey(table: Name, col: Name) extends Index

    def apply(index: table.Index): Option[Index] = index match {
      case index: ForeignKeyIndex => Option(index.getReferencedColumnNames).map(_.asScala).getOrElse(Nil).headOption.map { col =>
        ForeignKey(index.getTable.getName, col)
      }
      case index if index.getType.toUpperCase == "PRIMARY KEY" => Some(PrimaryKey)
      case _ => None
    }

    def apply(columnDefinition: ColumnDefinition): Option[Index] = {
      val defs = Option(columnDefinition.getColumnSpecStrings).iterator.flatMap(_.asScala).toVector
      val defsu = defs.map(_.toUpperCase)
      val fki = defsu.indexOfSlice(Seq("FOREIGN", "KEY", "REFERENCES"))
      if (fki >= 0 && fki + 4 < defs.length) {
        new CCJSqlParser(defs(fki + 4)).ColumnsNamesList().asScala.headOption.map { col =>
          ForeignKey(defs(fki + 3), col)
        }
      } else if (defsu.indexOfSlice(Seq("PRIMARY", "KEY")) >= 0) {
        Some(PrimaryKey)
      } else {
        None
      }
    }

  }

  case class Col(name: Name, `type`: AttributeType, index: Option[Index])

  object Col {
    def apply(columnDefinition: ColumnDefinition): Col = Col(columnDefinition.getColumnName, AttributeType(columnDefinition.getColDataType), Index(columnDefinition))
  }

  case class Table(name: Name, cols: collection.Map[Name, Col])

  object Table {
    def apply(createTable: CreateTable): Table = {
      val indexes = Option(createTable.getIndexes).iterator.flatMap(_.iterator().asScala).flatMap(x => Option(x.getColumnsNames).map(_.asScala).getOrElse(Nil).headOption.flatMap(colName => Index(x).map((colName: Name) -> _))).toMap
      val cols = mutable.LinkedHashMap(createTable.getColumnDefinitions.iterator().asScala.map(Col.apply).map(x => x.name -> (if (x.index.isEmpty && indexes.contains(x.name)) x.copy(index = indexes.get(x.name)) else x)).toList: _*)
      Table(createTable.getTable.getName, cols)
    }
  }

  case class Row(table: Name, data: Map[Name, String]) {
    def toTriples(metadata: Map[Name, Table]): List[Triple] = {
      def encode(x: String): String = URLEncoder.encode(x, "UTF-8")

      val tripleItems = (for {
        table <- metadata.get(table).iterator
        (colName, value) <- data.iterator
        col <- table.cols.get(colName)
      } yield {
        (col.index match {
          case Some(Index.PrimaryKey) => Left(Uri(s"${encode(table.name.value)}/${encode(col.name.value)}/${encode(value.stripPrefix("'").stripSuffix("'"))}"))
          case Some(Index.ForeignKey(ftable, fcol)) => Right(Uri(s"${encode(col.name.value)}") -> Uri(s"${encode(ftable.value)}/${encode(fcol.value)}/${encode(value.stripPrefix("'").stripSuffix("'"))}"))
          case None => Right(
            Uri(s"${encode(col.name.value)}") -> ((value, col.`type`) match {
              case (AnyToInt(value), AttributeType.Integer) => TripleItem.Number(value)
              case (AnyToDouble(value), AttributeType.Double) => TripleItem.Number(value)
              case (AnyToBoolean(value), AttributeType.Boolean) => TripleItem.BooleanValue(value)
              case (value, _) => TripleItem.Text(value.stripPrefix("'").stripSuffix("'"))
            })
          )
        }): Either[TripleItem.Uri, (TripleItem.Uri, TripleItem)]
      }).toList
      tripleItems.iterator.collectFirst {
        case Left(pk) => pk
      }.iterator.flatMap { s =>
        tripleItems.iterator.collect {
          case Right((p, o)) => Triple(s, p, o)
        }
      }.toList
    }
  }

  object Row {
    def apply(insert: Insert, metadata: Map[Name, Table]): Iterator[Row] = {
      val tableName: Name = insert.getTable.getName
      val cols = Option(insert.getColumns)
        .map(_.iterator().asScala.map(_.getColumnName: Name))
        .orElse(metadata.get(tableName).map(_.cols.valuesIterator.map(_.name)))
        .iterator.flatten.toVector
      Option(insert.getItemsList).iterator.collect {
        case x: MultiExpressionList => x.getExprList.iterator().asScala
        case x: ExpressionList => Iterator(x)
      }.flatten.map(x => cols.iterator.zip(x.getExpressions.iterator().asScala.map(_.toString)).toMap).map(Row(tableName, _))
    }
  }

}