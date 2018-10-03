@file:MavenRepository("repo1", "https://jitpack.io")

@file:DependsOn("com.github.holgerbrandl:kscript-annotations:1.2")
@file:DependsOn("de.mpicbg.scicomp:krangl:0.10.2")
@file:DependsOn("com.github.holgerbrandl:kravis:0.4")
@file:DependsOn("ml.dmlc:xgboost4j:0.80")

//@file:DependsOn("com.github.haifengl:smile-core:1.5.1")
//@file:DependsOn("com.github.haifengl:smile-plot:1.5.1")

import krangl.*
import kravis.*
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// Challenge: Predict taxi trip duration in NYC

// https://www.kaggle.com/c/nyc-taxi-trip-duration

val dataRoot = File("/Users/brandl/Desktop/taxi_data")

dataRoot.listFiles().forEach { println(it) }

operator fun File.div(fileName: String) = this.resolve(fileName)

var allTrainData = DataFrame.readCSV(dataRoot / "train.csv")
var trainData = allTrainData.sampleFrac(0.3)
var testData = DataFrame.readCSV(dataRoot / "test.csv")

//var trainData = DataFrame.readTSV(File("someTaxiRides.csv"))

trainData.schema()


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



fun prepareFeatures(nycFeatures: DataFrame): DataFrame {
    var trainData = nycFeatures
    val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    trainData = trainData.addColumns(
        pickup_datetime to { it[pickup_datetime].map<String> { LocalDateTime.parse(it, datePattern) } }
    )
//toto join with weather data
// 4 feature engineering
    /** The distance of the trip in m*/
    val distance = "dist"

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
        "work" to { it["hour"].map<Int> { (8..18).contains(it) } },
        "speed" to { it[distance] / it[trip_duration] * 3.6 }
    )

    return trainData
}


//
// do some visual inspection
//


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
    .plot(x = pickup_longitude, y = pickup_latitude).geomPoint(alpha = .1)



//
// build a model with xgboost
//

val trainFeat = prepareFeatures(trainData)
val testFeat = prepareFeatures(testData)
trainData.schema()


fun Array<DoubleArray>.toFloatMatrix(): Array<FloatArray> = map { it.map { it.toFloat() }.toFloatArray() }.toTypedArray()


val x = irisData.remove("Species", "Petal.Length").toDoubleMatrix().toFloatMatrix()
val xLong: FloatArray = x.reduce { left, right -> left + right }
val y = irisData["Petal.Length"].asType<Double>().map { it!!.toFloat() }.toFloatArray()

//val trainMat = DMatrix("agaricus.txt.test")
val trainMat = DMatrix(xLong, trainFeat.nrow, trainFeat.nrow)
trainMat.label = y


//val testMat = DMatrix("agaricus.txt.train")
//val validMat = DMatrix("valid.svm.txt") // todo where is this one?

val params = hashMapOf<String, Any>().apply {
    //    put("colsample_bytree", 0.4)
//    put("gamma", 0)
//    put("learning_rate", 0.07)
//    put("min_child_weight", 1.5)
//    put("eta", 1.0)
//    put("max_depth", 3)
//    put("silent", 1)
    put("objective", "reg:linear")
    put("eval_metric", "rmse")
}

// https://www.kaggle.com/fashionlee/using-xgboost-for-regression
//our_params={'eta':0.1,'seed':0,'subsample':0.8,'colsample_bytree':0.8,'objective':'reg:linear','max_depth':3,'min_child_weight':1}


//https://www.kaggle.com/pablocastilla/predict-house-prices-with-xgboost-regression
//xgboost.XGBRegressor(colsample_bytree=0.4, gamma=0, learning_rate=0.07, max_depth=3, min_child_weight=1.5, n_estimators=10000, reg_alpha=0.75, reg_lambda=0.45, subsample=0.6, seed=42)

// https://stackoverflow.com/questions/33209391/how-to-use-xgboost-algorithm-for-regression-in-r

// Specify a watch list to see model accuracy on data sets
//our_params={'eta':0.1,'seed':0,'subsample':0.8,'colsample_bytree':0.8,'objective':'reg:linear','max_depth':3,'min_child_weight':1}

val watches = hashMapOf<String, DMatrix>().apply {
    put("train", trainMat)
//    put("test", testMat)
}

// number of boosting iteration =3 would just build a simple 2-step function model
val nround = 10
val booster = XGBoost.train(trainMat, params, nround, watches, null, null)


// build feature map
// https://stackoverflow.com/questions/37627923/how-to-get-feature-importance-in-xgboost
// dump with feature map
//        booster.getFeatureScore()
//        booster.imp
val model_dump_with_feature_map = booster.getModelDump("featmap.txt", false)

//        predict on test set
//var dtest = DMatrix("test.svm.txt")
// predict
var predicts = booster.predict(trainMat)
//unwrap
val predictUnwrapped = predicts.map { it.first() }

//https://www.kaggle.com/fashionlee/using-xgboost-for-regression
//from sklearn.metrics import mean_squared_error
//    import math
//    testScore=math.sqrt(mean_squared_error(y_test.values,y_pred))
//print(testScore)

predicts.joinToString(",")
predicts.first().joinToString(",")

predicts.size

trainData = trainData.addColumn("predicted_pet_length") { predictUnwrapped }


trainData.plot(x = "Petal.Length", y = "predicted_pet_length").geomPoint()


booster.getFeatureScore(null)


//
// prepare submission file
//


//https://www.kaggle.com/michaelpawlus/xgboost-example-0-76178/code