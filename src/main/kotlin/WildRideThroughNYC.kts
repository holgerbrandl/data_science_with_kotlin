//@file:DependsOn("de.mpicbg.scicomp:krangl:0.10.2")
@file:DependsOn("de.mpicbg.scicomp:krangl:0.11-SNAPSHOT")
//@file:DependsOn("com.github.holgerbrandl:kravis:0.4")
@file:DependsOn("com.github.holgerbrandl:kravis:0.5-SNAPSHOT")
@file:DependsOn("ml.dmlc:xgboost4j:0.80")


import krangl.*
import krangl.experimental.oneHot
import kravis.*
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter


//' # Predict taxi trip duration in NYC

//' For detail see https://www.kaggle.com/c/nyc-taxi-trip-duration

val dataRoot = File("/Users/brandl/Desktop/taxi_data")

dataRoot.listFiles().forEach { println(it) }

operator fun File.div(fileName: String) = this.resolve(fileName)

//var allTrainData = DataFrame.readCSV(dataRoot / "train.csv")
//var trainData = allTrainData.sampleFrac(0.3)
var trainData = DataFrame.readTSV(File("/Users/brandl/someTaxiRides.csv"))

var testData = DataFrame.readCSV(dataRoot / "test.csv")

//Live@KC Explore structure and differences between test and training data
trainData
trainData.head()
trainData.schema()

// reconfigure output width
PRINT_MAX_WIDTH = 80
trainData.schema()

trainData["passenger_count"]

testData.schema()
//end

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


/**Inferrred attribute */
val distance = "distance"


trainData["vendor_id"]
trainData[vendor_id]


fun prepareFeatures(trainData: DataFrame): DataFrame {
    var trainData = trainData

    val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    trainData = trainData.addColumns(
        pickup_datetime to { it[pickup_datetime].map<String> { LocalDateTime.parse(it, datePattern) } }
    )

//toto join with weather data
    // calculate the distance of the trip

    trainData = trainData.addColumn(distance) { df ->
        val longDist = df[pickup_longitude] - df[dropoff_longitude]
        val latDist = df[pickup_latitude] - df[dropoff_latitude]
        (longDist * longDist + latDist * latDist).asDoubles().map { Math.sqrt(it!!) }
    }

    trainData = trainData.addColumns(
        "month" to { it[pickup_datetime].asType<LocalDateTime>().mapNonNull { it.month } },
        "month" to { it[pickup_datetime].map<LocalDateTime>() { it.month } },
        "wday" to { it[pickup_datetime].asType<LocalDateTime>().mapNonNull { it.dayOfWeek } },
        "hour" to { it[pickup_datetime].asType<LocalDateTime>().mapNonNull { it.hour } },
        "work" to { it["hour"].map<Int> { (8..18).contains(it) } }
    )

    return trainData
}


trainData = prepareFeatures(trainData)
trainData.schema()


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Do some visual inspection
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


trainData.plot(x = passenger_count).geomBar()


//trainData.plot(trip_duration).geomHistogram()
trainData.filter { it[trip_duration] lt 1000 }.plot(x = trip_duration).geomHistogram()

trainData.count(passenger_count)
    .plot(x = passenger_count.asDiscreteVariable, y = "n")
    .geomBar(stat = Stat.identity)
    .xLabel("# Passengers")


fun DataFrame.constrainCoord(): DataFrame = filter {
    with(it[pickup_longitude]) { (this gt -74.1) AND (this lt -73.7) } AND
        with(it[pickup_latitude]) { (this gt 40.55) AND (this lt 40.9) }
}

trainData.plot(x = pickup_longitude, y = pickup_latitude).geomPoint(alpha = .1)
trainData
    .constrainCoord()
    .plot(x = pickup_longitude, y = pickup_latitude).geomPoint(alpha = .1, size = .3)

trainData.addColumn("speed") { it[distance] / it[trip_duration] * 3.6 }
//todo do heatmap


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Build a predictive model with xgboost
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


//trainData.writeCSV(File("trainData.dump.txt"))
//var trainData =DataFrame.readCSV(File("trainData.dump.txt"))


// LIVE@KC Create helper to split train and validation data (inkl refac into extension method)
fun DataFrame.splitTrainTest(): Pair<DataFrame, DataFrame> {
    return shuffle().run {
        val n = (nrow * 0.3).toInt();
        slice(1..n) to slice(n..nrow)
    }
}


// https://github.com/khud/sparklin/issues/34
//val (trainMatDf, valMatDf) = trainFeat.splitTrainTest()
val dataSplit = trainData.splitTrainTest()
val trainMatDf = dataSplit.first
var valMatDf = dataSplit.second

//trainMatDf = trainMatDf.select{ !startsWith("month") }.select{ !startsWith("wday") }
//trainMatDf = trainMatDf.select{ listOf(passenger_count, "dist")}.remove(trip_duration)


//end
//trainMatDf.writeCSV(File("trainMatDf.dump.txt"))
//var trainMatDf =DataFrame.readCSV(File("trainMatDf.dump.txt"))

fun DataFrame.selectPredictors(): DataFrame = select(
    vendor_id, passenger_count, pickup_longitude, pickup_latitude, dropoff_longitude, dropoff_latitude, "dist", "month", "wday", "hour", "work"
).oneHot<Month>("month")
    .oneHot<DayOfWeek>("wday")
    .addColumn("work") { rows.map { if (it["work"] as Boolean) 1 else 0 } }


fun DataFrame.buildTrainMatrix(responseVariable: String = trip_duration): DMatrix {
    val x = selectPredictors().toFloatMatrix()
    val xLong: FloatArray = x.reduce { left, right -> left + right }
    val y = this[responseVariable].asDoubles().map { it!!.toFloat() }.toFloatArray()
    return DMatrix(xLong, nrow, ncol - 1).apply {
        label = y
    }
}

// visualize feature matrix
//trainMatDf.head(2000)
//    .addColumn("id"){rowNumber}
//    .gather("predictor", "value", columns= { except("id")})
//    .plot("predictor", "id", fill="value")
//    .geomTile()
//    .theme(axisTitleX=ElementTextBlank())


//trainMatDf.toFloatMatrix().reduce { left, right -> left + right }.size
// Construct the training input for XGBoost
val trainMat = trainMatDf.buildTrainMatrix()



val params = hashMapOf<String, Any>().apply {
    put("objective", "reg:linear")
    put("eval_metric", "rmse")
}


val watches = hashMapOf<String, DMatrix>().apply {
    put("train", trainMat)
    put("validation", valMatDf.buildTrainMatrix())
}

// number of boosting iteration =3 would just build a simple 2-step function model
val nround = 10
val booster = XGBoost.train(trainMat, params, nround, watches, null, null)

// predict trip duration
//var predicts = booster.predict(trainMat)
var predicts = booster.predict(valMatDf.buildTrainMatrix())
//unwrap result
val predictUnwrapped = predicts.map { it.first() }

predicts.size

val predTripDurcation = "predicted_trip_duration"
valMatDf = valMatDf.addColumn(predTripDurcation) { predictUnwrapped }
valMatDf.schema()

//LIVE@KC explore korrelation
valMatDf.plot(x = trip_duration, y = predTripDurcation).geomPoint(alpha = .1).scaleXLog10().scaleYLog10()

// explore feature importance
//booster.getFeatureScore(null)

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Prepare submission file
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun DataFrame.buildTestMatrix(): DMatrix {
    val x = toFloatMatrix()
    val xLong: FloatArray = x.reduce { left, right -> left + right }
    return DMatrix(xLong, nrow, ncol)
}


val testFeat = prepareFeatures(testData).selectPredictors().buildTestMatrix()

var testPrediction = booster.predict(testFeat).map { it.first() }

testData.schema()
val kaggleSubmission = testData
    .addColumn("trip_duration") { testPrediction }
    .select("id", "trip_duration")

kaggleSubmission.schema()
kaggleSubmission.writeCSV(File("kotlin4kaggle.csv"))
