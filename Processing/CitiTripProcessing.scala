import org.apache.spark.sql.types._
import java.lang.Math


val bikeDataPath = ("s3a://citibiketripdata/201907-citibike-tripdata.csv")


/*
|-- tripduration: string (nullable = true)
 |-- starttime: string (nullable = true)
 |-- stoptime: string (nullable = true)
 |-- start station id: string (nullable = true)
 |-- start station name: string (nullable = true)
 |-- start station latitude: string (nullable = true)
 |-- start station longitude: string (nullable = true)
 |-- end station id: string (nullable = true)
 |-- end station name: string (nullable = true)
 |-- end station latitude: string (nullable = true)
 |-- end station longitude: string (nullable = true)
 |-- bikeid: string (nullable = true)
 |-- usertype: string (nullable = true)
 |-- birth year: string (nullable = true)
 |-- gender: string (nullable = true)



*/

// def lowerRemoveAllWhitespace(schema: struct) = {
//   val bikeData = spark.read.format("csv").option("header", "false").schema(schema).option("mode", "DROPMALFORMED").load(bikeDataPath)
//   val bikeDF2 = bikeData.withColumn("starttime",dateToTimeStamp($"starttime"))
//   val bikeDF3 = bikeDF2.withColumn("stoptime",dateToTimeStamp($"stoptime"))
  
// }

//data citi bike prep  

def loadCitiTripData (bikeDataPath : String): DataFrame =  { 

	val schema = new StructType().add("tripduration",DoubleType,true).add("starttime",StringType,true).add("stoptime",StringType,true).add("start station id",StringType,true).add("start station name",StringType,true).add("start station latitude",StringType,true).add("start station longitude",StringType,true).add("end station id",StringType,true).add("end station name",StringType,true).add("end station latitude",StringType,true).add("end station longitude",StringType,true).add("bikeid",StringType,true).add("usertype",StringType,true).add("birth year",IntegerType,true).add("gender",StringType,true)
	val bikeData = spark.read.format("csv").option("header", "false").schema(schema).option("mode", "DROPMALFORMED").load(bikeDataPath)
	bikeData
}



val secToMinTime = udf((time_in_sec: Double) => { 
	val min = Math.round(time_in_sec/60 * 100.00)/100.00
	min
})

val dateToTimeStamp = udf((starttime: String) => { 
	starttime.split(':')(0)
})

//geocoding

val zipPath: String = "hdfs://ec2-35-163-178-143.us-west-2.compute.amazonaws.com:9000/zipcode_tables/"

def createZipMap (zipTablePath : String) = {
	val zipTable = sc.textFile(zipPath)
	val zipRDD = zipTable.map(line => line.split(','))
	val idZipRDD = zipRDD.map(line=>(line(0),line(1))).collectAsMap()
idZipRDD

}
val zipMap = createZipRDD(zipPath)

val getZipWithID = udf((startStion: String) => { 
	
	if(zipMap.contains(startStion)) 
		 if(zipMap(startStion).length >0){
		 	zipMap(startStion)
		 } else {
		 	val none = "00000"
			none
		 }
		 

	else{
		val none = "00000"
		none
	} 
	
})





val bikeData = loadCitiTripData(bikeDataPath)
val bikeDataStart = bikeData.withColumn("starttime",dateToTimeStamp($"starttime"))
val bikeDataStop = bikeDataStart.withColumn("stoptime",dateToTimeStamp($"stoptime"))

val joinedDFWithZip = bikeDataStop.withColumn("start station id",getZipWithID($"start station id"))
val joinedDFWithZip1 = joinedDFWithZip.withColumn("end station id",getZipWithID($"end station id"))


// create DF for departure stations with the distribution of final destinations

def joinedDepartAndDuration = {

	val departureDF = joinedDFWithZip1.select("starttime", "start station id", "end station id").groupBy("starttime", "start station id", "end station id").count()

	val durationDF = joinedDFWithZip1.select("starttime", "start station id", "end station id", "tripduration").groupBy("starttime", "start station id", "end station id").avg("tripduration")
	val durationInMin= durationDF.withColumn("avg(tripduration)",secToMinTime($"avg(tripduration)").alias("duration"))
	val joinSeq = Seq("starttime", "start station id", "end station id")
	val deptzips_duration= departureDF.join(durationInMin, joinSeq).orderBy($"starttime".asc, $"start station id".asc)
	deptzips_duration
}


val joinedDF = joinedDepartAndDuration

:require postgresql-42.2.8.jar
val prop = new java.util.Properties
prop.setProperty("driver", "org.postgresql.Driver")
prop.setProperty("user", "")
prop.setProperty("password", "")

val url = "jdbc:postgresql://10.0.0.9:5432/testing"
val table = "t2"
val table = "sample_data_table"

joinedDFWithZip1.write.mode("overwrite").jdbc(url, "t2", prop)

//departureDF.orderBy($"starttime".asc, $"start station id".asc).show()

// // create DF for arrival stations with the distribution of start destinations
// val arrivalDF = bikeDF3.select("starttime", "end station id", "start station id").groupBy("starttime", "end station id", "start station id").count()
// arrivalDF.orderBy($"starttime".asc, $"end station id".asc).show()



// create DF for depart stations with the distribution of age
// val ageDF_depart = bikeDF3.select("starttime", "start station id", "end station id", "birth year").groupBy("starttime", "start station id", "end station id").avg("birth year")

// //ageDF.orderBy($"starttime".asc, $"start station id".asc).show()

// // create DF for arrival stations with the distribution of age
// val ageDF_arrival = bikeDF3.select("starttime", "end station id", "start station id", "birth year").groupBy("starttime", "end station id", "start station id").avg("birth year")
// ageDF_arrival.orderBy($"starttime".asc, $"end station id".asc).show()

// val yearToAge = udf((yearInt: Integer) => { 
// 	val age = 2019-yearInt
// 	age
// })

//calcuated age df
// val ageDF_arrival2 = ageDF_arrival.withColumn("avg(birth year)",yearToAge($"avg(birth year)").alias("age"))

// val ageDF_depart2 = ageDF_depart.withColumn("avg(birth year)",yearToAge($"avg(birth year)").alias("age"))
// ageDF_depart2.orderBy($"starttime".asc, $"start station id".asc).show()







