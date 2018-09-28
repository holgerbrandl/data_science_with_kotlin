@file:MavenRepository("repo1", "https://jitpack.io")

@file:DependsOn("com.github.holgerbrandl:kscript-annotations:1.2")
@file:DependsOn("de.mpicbg.scicomp:krangl:0.11-SNAPSHOT")
@file:DependsOn("com.github.holgerbrandl:kravis:0.4-SNAPSHOT")
@file:DependsOn("com.github.haifengl:smile-core:1.5.1")
@file:DependsOn("com.github.haifengl:smile-plot:1.5.1")

import krangl.DataFrame
import krangl.head
import krangl.readCSV
import krangl.schema
import kravis.geomHistogram
import kravis.plot
import java.io.File


// Challenge: Predict taxi trip duration in NYC

// https://www.kaggle.com/c/nyc-taxi-trip-duration

val dataRoot = File("/Users/brandl/Desktop/taxi_data")

dataRoot.listFiles().forEach { println(it) }

operator fun File.div(fileName: String) = this.resolve(fileName)

val trainData = DataFrame.readCSV(dataRoot / "train.csv")
val testData = DataFrame.readCSV(dataRoot / "test.csv")


/** a unique identifier for each trip*/
val id: String = "id"
/** a code indicating the provider associated with the trip record*/
val vendor_id: String = "vendor_id"
/** date and time when the meter was engaged*/
val pickup_datetime: String = "pickup_datetime"
/** date and time when the meter was disengaged*/
val dropoff_datetime: String = "dropoff_datetime"
/** the number of passengers in the vehicle (driver entered value)*/
val passenger_count: String = "passenger_count"
/** the longitude where the meter was engaged*/
val pickup_longitude: String = "pickup_longitude"
/** the latitude where the meter was engaged*/
val pickup_latitude: String = "pickup_latitude"
/** the longitude where the meter was disengaged*/
val dropoff_longitude: String = "dropoff_longitude"
/** the latitude where the meter was disengaged*/
val dropoff_latitude: String = "dropoff_latitude"
/** This flag indicates whether the trip record was held in vehicle memory before sending to the vendor because the vehicle did not have a connection to the server. Y=store and forward; N=not a store and forward trip**/
val store_and_fwd_flag: String = "store_and_fwd_flag"
/** duration of the trip in seconds */
val trip_duration: String = "trip_duration"


trainData.head()
trainData.schema()

trainData["vendor_id"]

// pull in data description


trainData.plot(trip_duration).geomHistogram()

// zoom in using coordCartesian




