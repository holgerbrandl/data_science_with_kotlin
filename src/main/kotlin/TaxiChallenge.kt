import krangl.*
import kravis.*
import kravis.render.RserveEngine
import java.io.File

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

// default private --> needs to be public
operator fun File.div(fileName: String) = this.resolve(fileName)


fun main(args: Array<String>) {

//    // https://github.com/khud/sparklin/issues/28
//    class Foo{
//        override fun toString(): String {
//            throw RuntimeException("something went wrong")
//        }
//    }
//
//    Foo()


    //' download the data with
    // kaggle competitions download -c nyc-taxi-trip-duration


    val dataRoot = File("/Users/brandl/.kaggle/competitions/nyc-taxi-trip-duration")
    dataRoot.listFiles().joinToString(",\\n")

    // fixme zip support seems broken
//    DataFrame.readCSV(dataRoot/"train.zip")
    val allTrainData = DataFrame.readCSV(dataRoot / "train.csv")

    //' peek into structure
    allTrainData
    allTrainData.schema()

    //' use just some for eda
    val trainData = allTrainData.sampleFrac(0.1)
    trainData.writeTSV(File("someTaxiRides.csv"))
//    val trainData = DataFrame.readTSV(File("someTaxiRides.csv")).sampleFrac(0.1)

    val testData = DataFrame.readCSV(dataRoot / "test.csv")

    testData.schema()


    trainData.names //--> convert to enum


    //' ## Basic inspection
//    We find:

//    vendor_id only takes the values 1 or 2, presumably to differentiate two taxi companies
    // fixme add enum support to krangl verbs
//    trainData.count(TaxiColumns.vendor_id)
    trainData.count(vendor_id)


//    pickup_datetime and (in the training set) dropoff_datetime are combinations of date and time that we will have to re-format into a more useful shape

//    passenger_count takes a median of 1 and a maximum of 9 in both data sets
    trainData.count(passenger_count)

    SessionPrefs.RENDER_BACKEND = RserveEngine()
    trainData.ggplot(x = passenger_count.asDiscreteVariable).geomBar()

    trainData.count(passenger_count)
        .ggplot(x = passenger_count.asDiscreteVariable, y = "n")
        .geomBar(stat = Stat.identity)
        .xLabel("# Passengers")

    //' Do we actually have no passengers
    trainData.filter { it[passenger_count] eq 0 }
    //' yes --> remove them later

//    The pickup/dropoff_longitute/latitute describes the geographical coordinates where the meter was activate/deactivated.
    trainData.schema()
    //todo can we render this on an actual map
    trainData.ggplot(x = pickup_longitude, y = pickup_latitude).geomPoint(alpha = .1)


//        store_and_fwd_flag is a flag that indicates whether the trip data was sent immediately to the vendor (“N”) or held in the memory of the taxi because there was no connection to the server (“Y”). Maybe there could be a correlation with certain geographical areas with bad reception?
    trainData.count(store_and_fwd_flag)
    trainData.ggplot(x = pickup_longitude, y = pickup_latitude, color = store_and_fwd_flag).geomPoint()
    //-> no seems irrelavant

    // todo draw lines from to using geom_segment (needed)

//    trip_duration: our target feature in the training data is measured in seconds.
    trainData.schema()
    trainData
        .ggplot(x = trip_duration).geomHistogram()
        .scaleXLog10("labels" to "comma".asRExpression)
        .xLabel("Trip Duration [s]")

    // use minutes instead
    //fixme it[trip_duration]/60 throws unsupported operator exception
    trainData.addColumn("trip_duration_min") { it[trip_duration] / 60.0 }
        .ggplot(x = "trip_duration_min").geomHistogram()
        .scaleXLog10("labels" to "comma".asRExpression)
        .xLabel("Trip Duration [min]")

//    we find
//    the majority of rides follow a rather smooth distribution that looks almost log-normal with a peak just short of 1000 seconds, i.e. about 17 minutes.
//    There are several suspiciously short rides with less than 10 seconds duration.
//    Additionally, there is a strange delta-shaped peak of trip_duration just before the 1e5 seconds mark and even a few way above it:

    // 1.4 Missing values
    //todo redo once AggFun.na is there
//    trainData.summarizeAt({ all() }, AggFun.)


}


