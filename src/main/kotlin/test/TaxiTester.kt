package test

import krangl.*
import kravis.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author Holger Brandl
 */


// use enum instead
// there does not seem to be from-within string completion
val vendor_id = "vendor_id"
val pickup_datetime = "pickup_datetime"
val dropoff_datetime = "dropoff_datetime"
val passenger_count = "passenger_count"
val pickup_longitude = "pickup_longitude"
val pickup_latitude = "pickup_latitude"
val dropoff_longitude = "dropoff_longitude"
val dropoff_latitude = "dropoff_latitude"
val store_and_fwd_flag = "store_and_fwd_flag"
val trip_duration = "trip_duration"


fun main(args: Array<String>) {

//    var trainData = DataFrame.readTSV(File("someTaxiRides.csv")) //.sampleFrac(0.1)
    var trainData = DataFrame.readTSV(File("someTaxiRides.csv"))

    trainData = trainData.addColumn("dist") {
        val longDist = it[pickup_longitude] - it[dropoff_longitude];
        val latDist = it[pickup_latitude] - it[dropoff_latitude];
        (longDist * longDist + latDist * latDist).asDoubles().map { Math.sqrt(it!!) }
    }

// https://stackoverflow.com/questions/639695/how-to-convert-latitude-or-longitude-to-meters
    trainData.plot(x = "dist").geomHistogram().scaleXLog10()
//            ((it[pickup_longitude] - it[dropoff_longitude]) * (it[pickup_longitude] - it[dropoff_longitude]) + (it[pickup_latitude] - it[dropoff_latitude]) * (it[pickup_latitude] - it[dropoff_latitude])).asDoubles().map{ Math.sqrt(it!!)}

    val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    trainData = trainData.addColumns(
        pickup_datetime to { it[pickup_datetime].map<String> { LocalDateTime.parse(it, datePattern) } }
    )


    trainData
        .addColumn("hour of the day") { df -> df[pickup_datetime].map<LocalDateTime> { it.hour } }
        .plot(x = "hour of the day", fill = vendor_id.asDiscreteVariable)
        .geomBar()

    trainData
        .addColumn("hour of the day") { df -> df[pickup_datetime].map<LocalDateTime> { it.hour } }
        .plot(x = "hour of the day", color = vendor_id.asDiscreteVariable)
        .geomPoint(stat = Stat.count)
        .geomLine(stat = Stat.count)
//        .spec

}