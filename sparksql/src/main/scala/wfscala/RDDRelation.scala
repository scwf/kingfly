package wfscala


import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

// One method for defining the schema of an RDD is to make a case class with the desired column
// names and types.
case class Record(key: Int, value: String)

object RDDRelation {
  def main(args: Array[String]) {
    val sc = new SparkContext("local", "RDDRelation")
    val sqlContext = new SQLContext(sc)

    // Importing the SQL context gives access to all the SQL functions and implicit conversions.
    import sqlContext._

    val rdd = sc.parallelize((1 to 100).map(i => Record(i, s"val_$i")))
    // Any RDD containing case classes can be registered as a table.  The schema of the table is
    // automatically inferred using scala reflection.
    rdd.registerAsTable("records")

    // Once tables have been registered, you can run SQL queries over them.
    println("Result of SELECT *:")
    sql("SELECT * FROM records").collect().foreach(println)

    // Aggregation queries are also supported.
    val count = sql("SELECT COUNT(*) FROM records").collect().head.getInt(0)
    println(s"COUNT(*): $count")

    // The results of SQL queries are themselves RDDs and support all normal RDD functions.  The
    // items in the RDD are of type Row, which allows you to access each column by ordinal.
    val rddFromSql = sql("SELECT key, value FROM records WHERE key < 10")

    println("Result of RDD.map:")
    rddFromSql.map(row => s"Key: ${row(0)}, Value: ${row(1)}").collect.foreach(println)

    // Queries can also be written using a LINQ-like Scala DSL.
    rdd.where('key === 1).orderBy('value.asc).select('key).collect().foreach(println)

    // Write out an RDD as a parquet file.
    rdd.saveAsParquetFile("pair.parquet")

    // Read in parquet file.  Parquet files are self-describing so the schmema is preserved.
    val parquetFile = sqlContext.parquetFile("pair.parquet")

    // Queries can be run using the DSL on parequet files just like the original RDD.
    parquetFile.where('key === 1).select('value as 'a).collect().foreach(println)

    // These files can also be registered as tables.
    parquetFile.registerAsTable("parquetFile")
    sql("SELECT * FROM parquetFile").collect().foreach(println)
  }
}
