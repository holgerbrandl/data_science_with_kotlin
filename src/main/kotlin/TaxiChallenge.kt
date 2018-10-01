import krangl.*
import kravis.*
import kravis.render.LocalR
import kravis.render.RserveEngine
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * @author Holger Brandl
 */

//https://blog.egorand.me/where-do-i-put-my-constants-in-kotlin/
//https://blog.jetbrains.com/kotlin/2013/06/static-constants-in-kotlin/
// use enum instead
// there does not seem to be from-within string completion


// start with fancy scratch magic here

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

// default private --> needs to be public
operator fun File.div(fileName: String) = this.resolve(fileName)


/** The distance of the trip in m*/
val distance = "dist"


fun main(args: Array<String>) {
    print(vendor_id)
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
    var trainData = allTrainData.sampleFrac(0.3)
    trainData.writeTSV(File("someTaxiRides.csv"))
//    var trainData = DataFrame.readTSV(File("someTaxiRides.csv")).sampleFrac(0.1)

    val testData = DataFrame.readCSV(dataRoot / "csv")

    testData.schema()


    trainData.names //--> convert to enum


    //' ## Basic inspection
//    We find:

//    getVendor_id only takes the values 1 or 2, presumably to differentiate two taxi companies
    // fixme add enum support to krangl verbs
//    trainData.count(TaxiColumns.getVendor_id)
    trainData.count(vendor_id)


//    getPickup_datetime and (in the training set) getDropoff_datetime are combinations of date and time that we will have to re-format into a more useful shape

//    getPassenger_count takes a median of 1 and a maximum of 9 in both data sets
    trainData.count(passenger_count)

    SessionPrefs.RENDER_BACKEND = RserveEngine() // todo fix accidental date support
    SessionPrefs.RENDER_BACKEND = LocalR()
    trainData.plot(x = passenger_count.asDiscreteVariable).geomBar()

    trainData.count(passenger_count)
        .plot(x = passenger_count.asDiscreteVariable, y = "n")
        .geomBar(stat = Stat.identity)
        .xLabel("# Passengers")

    //' Do we actually have no passengers
    trainData.filter { it[passenger_count] eq 0 }
    //' yes --> remove them later

//    The pickup/dropoff_longitute/latitute describes the geographical coordinates where the meter was activate/deactivated.
    trainData.schema()
    //todo can we render this on an actual map
    trainData.plot(x = pickup_longitude, y = pickup_latitude).geomPoint(alpha = .1)


//        getStore_and_fwd_flag is a flag that indicates whether the trip data was sent immediately to the vendor (“N”) or held in the memory of the taxi because there was no connection to the server (“Y”). Maybe there could be a correlation with certain geographical areas with bad reception?
    trainData.count(store_and_fwd_flag)
    trainData.plot(x = pickup_longitude, y = pickup_latitude, color = store_and_fwd_flag).geomPoint()
    //-> no seems irrelavant

    // todo draw lines from to using geom_segment (needed)

    // 1.4 Missing values
    //todo redo once AggFun.na is there
//    trainData.summarizeAt({ all() }, AggFun.)


    // 1.6 reformatting features

    //For our following analysis, we will turn the data and time from characters into *date* objects. We also recode *vendor\_id* as a factor. This makes it easier to visualise relationships that involve these features.
//        mutate(getPickup_datetime = ymd_hms(getPickup_datetime),
//            getDropoff_datetime = ymd_hms(getDropoff_datetime),
//            getVendor_id = factor(getVendor_id),
//            getPassenger_count = factor(getPassenger_count))

    trainData = trainData.addColumns(
        pickup_datetime to { it[pickup_datetime].map<String> { } }
    )

    // https://www.programiz.com/kotlin-programming/examples/string-date
    //yyyy-MM-dd HH:mm:ss

    // start with a basic example
    val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val exampleDate = LocalDateTime.parse("2016-06-18 15:26:12", datePattern)
    // http://natty.joestelmach.com/try.jsp#

    trainData = trainData.addColumns(
        pickup_datetime to { it[pickup_datetime].map<String> { LocalDateTime.parse(it, datePattern) } }
    )

    //
    //' # 2 Individual feature visualisations
    //
    trainData.schema()


//    getTrip_duration: our target feature in the training data is measured in seconds.
    trainData.schema()
    trainData
        .plot(x = trip_duration).geomHistogram()
        .scaleXLog10("labels" to "comma".asRExpression)
        .xLabel("Trip Duration [s]")

    // use minutes instead
    //fixme it[getTrip_duration]/60 throws unsupported operator exception
    trainData.addColumn("trip_duration_min") { it[trip_duration] / 60.0 }
        .plot(x = "trip_duration_min").geomHistogram()
        .scaleXLog10("labels" to "comma".asRExpression)
        .xLabel("Trip Duration [min]")

//    we find
//    the majority of rides follow a rather smooth distribution that looks almost log-normal with a peak just short of 1000 seconds, i.e. about 17 minutes.
//    There are several suspiciously short rides with less than 10 seconds duration.
//    Additionally, there is a strange delta-shaped peak of getTrip_duration just before the 1e5 seconds mark and even a few way above it:

    // plot pickup dates througout the year
    exampleDate.year
    trainData.plot(x = pickup_datetime).geomHistogram()

    //todo
//    // zoom into january
//    train %>%
//        filter(getPickup_datetime > ymd("2016-01-20") & getPickup_datetime < ymd("2016-02-10")) %>%
//        plot(aes(getPickup_datetime)) +
//        geom_histogram(fill = "red", bins = 120)

    //rowwise
//    trainData.rows.filter { (it[getPickup_datetime] as LocalDateTime).toLocalDate().isAfter(LocalDate.parse("2016-01-20")) }.first()
    trainData.countExpr {
        it[pickup_datetime].map<LocalDateTime> { localDateTime ->
            //        localDateTime.toLocalDate().isAfter(LocalDate.parse("2016-01-20"))
            //refact to
            with(localDateTime.toLocalDate()) { isAfter(LocalDate.parse("2016-01-20")) && isBefore(LocalDate.parse("2016-02-10")) }
        }
    }

    // use for vis

//        localDateTime.toLocalDate().isAfter(LocalDate.parse("2016-01-20"))

    trainData.filter {
        it[pickup_datetime].isMatching<LocalDateTime> {
            toLocalDate().isAfter(LocalDate.parse("2016-01-20")) && toLocalDate().isBefore(LocalDate.parse("2016-02-10"))
        }
    }.plot(x = pickup_datetime)
        .geomHistogram()
        .title("Pickup Counts in Winter")

    // check various other aspects
    trainData.plot(x = passenger_count.asDiscreteVariable).geomBar()
    trainData.plot(x = passenger_count.asDiscreteVariable, fill = vendor_id.asDiscreteVariable).geomBar(position = PositionDodge2())
    // The vast majority of rides had only a single passenger, with two passengers being the (distant) second most popular option.
    //
    //Towards larger passenger numbers we are seeing a smooth decline through 3 to 4, until the larger crowds (and larger cars) give us another peak at 5 to 6 passengers.
    //
    //Vendor 2 has significantly more trips in this data set than vendor 1 (note the logarithmic y-axis). This is true for every day of the week.

    trainData.plot(x = vendor_id).geomBar()
    trainData.plot(x = store_and_fwd_flag).geomBar()
    // fixme how to sort that
    trainData.addColumn("day of the week") { it[pickup_datetime].map<LocalDateTime> { it.dayOfWeek } }.plot("day of the week").geomBar()
    trainData.addColumn("hour of the day") { it[pickup_datetime].map<LocalDateTime> { it.hour } }.plot("hour of the day").geomBar()
    // or better line //fixme how to do this in plot2
//    trainData.addColumn("hour of the day"){ it[getPickup_datetime].map<LocalDateTime>{ it.hour}}.plot("hour of the day").geomBar()

// by vendor
//    trainData.count(getVendor_id)
    trainData
        .addColumn("hour of the day") { df -> df[test.pickup_datetime].map<LocalDateTime> { it.hour } }
        .plot(x = "hour of the day", fill = test.vendor_id.asDiscreteVariable)
        .geomBar()

    trainData
        .addColumn("hour of the day") { df -> df[test.pickup_datetime].map<LocalDateTime> { it.hour } }
        .plot(x = "hour of the day", color = test.vendor_id.asDiscreteVariable)
        .geomPoint(stat = Stat.count)
        .geomLine(stat = Stat.count)
//        .spec


    // nice insight vendor 2 owns the big cars
    trainData.count(vendor_id)
    trainData.plot(x = passenger_count.asDiscreteVariable, fill = vendor_id.asDiscreteVariable).geomBar(position = PositionDodge2())


    // We find an interesting pattern with Monday being the quietest day and Friday very busy. This is the same for the two different vendors, with getVendor_id == 2 showing significantly higher trip numbers.
    //
    //As one would intuitively expect, there is a strong dip during the early morning hours. There we also see not much difference between the two vendors. We find another dip around 4pm and then the numbers increase towards the evening.

    SessionPrefs.RENDER_BACKEND = RserveEngine() // todo fix accidental date support

    // Finally, we will look at a simple overview visualisation of the pickup/dropoff latitudes and longitudes:
    trainData.plot(x = pickup_longitude).geomHistogram()
    trainData.plot(x = pickup_latitude).geomHistogram()

//    Here we had constrain the range of latitude and longitude values, because there are a few cases which are way outside the NYC boundaries. The resulting distributions are consistent with the focus on Manhattan that we had already seen on the map. These are the most extreme values from the pickup_latitude feature:


//    dropoff_longitude > -74.05 & dropoff_longitude < -73.7)
//    dropoff_latitude > 40.6 & dropoff_latitude < 40.9
    trainData
        // note must do: extract receiver function here via refactoring
//        .filter{ with(it[dropoff_longitude]){ (this gt -74.05) AND (this gt -73.05)}}
        .constrainCoord()
        .plot(x = pickup_longitude, y = pickup_latitude).geomPoint()

    trainData
//        .remove(pickup_datetime)
        .plot(x = pickup_longitude, y = pickup_latitude)
        .geomPoint(color = RColor.red, alpha = .3)
        .geomPoint(Aes(x = dropoff_longitude, y = dropoff_latitude), alpha = .3, color = RColor.blue)
        .xLabel("longitude")
        .xLabel("latitude")

    // 3 Feature Relations

    // maybe later

    // 4 feature engineering

    trainData = trainData.addColumn(distance) { df ->
        val longDist = df[pickup_longitude] - df[dropoff_longitude]
        val latDist = df[pickup_latitude] - df[dropoff_latitude]
        (longDist * longDist + latDist * latDist).asDoubles().map { Math.sqrt(it!!) }
    }

    trainData.sampleN(5e4.toInt()).plot(distance).geomHistogram()


//    mutate(speed = dist/trip_duration*3.6,
//
//        date = date(pickup_datetime),
//
//        month = month(pickup_datetime, label = TRUE),
//
//        wday = wday(pickup_datetime, label = TRUE),
//
//        wday = fct_relevel(wday, c("Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun")),
//
//        hour = hour(pickup_datetime),
//
//        work = (hour %in% seq(8,18)) & (wday %in% c("Mon","Tues","Wed","Thurs","Fri")),
//
//    jfk_trip = (jfk_dist_pick < 2e3) | (jfk_dist_drop < 2e3),
//
//    lg_trip = (lg_dist_pick < 2e3) | (lg_dist_drop < 2e3),
//
//    blizzard = !( (date < ymd("2016-01-22") | (date > ymd("2016-01-29"))) )
//    )

    trainData = trainData.addColumns(
        "month" to { it[pickup_datetime].asType<LocalDateTime>().mapNonNull { it.month } },
        "month" to { it[pickup_datetime].map<LocalDateTime>() { it.month } },
        "wday" to { it[pickup_datetime].asType<LocalDateTime>().mapNonNull { it.dayOfWeek } },
        "hour" to { it[pickup_datetime].asType<LocalDateTime>().mapNonNull { it.hour } },
        "work" to { it["hour"].map<Int> { (8..18).contains(it) } },
        "speed" to { it[distance] / it[trip_duration] * 3.6 }
    )


    trainData.plot(distance, trip_duration)
        .geomPoint()
        .scaleXLog10()
        .scaleYLog10()
        .labs(x = "Direct distance [m]", y = "Trip duration [s]")
    //
    //```
    //
    //
    //
    //We find:
    //
    //
    //
    //- The distance generally increases with increasing *trip\_duration*
    //
    //
    //
    //- Here, the 24-hour trips look even more suspicious and are even more likely to be artefacts in the data.
    //
    //
    //
    //- In addition, there are number of trips with very short distances, down to 1 metre, but with a large range of apparent *trip\_durations*
    //
    //
    //
    //Let's filter the data a little bit to remove the extreme (and the extremely suspicious) data points, and bin the data into a 2-d histogram. This plot shows that in log-log space the *trip\_duration* is increasing slower than linear for larger *dist*ance values:

    //' # Speed
    // Distance over time is of course velocity, and by computing the average apparent velocity of our taxis we will have another diagnostic to remove bogus values. Of course, we won't be able to use *speed* as a predictor for our model, since it requires knowing the travel time, but it can still be helpful in cleaning up our training data and finding other features with predictive power. This is the *speed* distribution:


    trainData.plot("speed").geomHistogram().scaleXLog10()

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 5 Data cleaning
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    trainData.plot(x = trip_duration).geomHistogram().scaleXLog10()

    trainData.filter { it[trip_duration] gt 2 * 3600 }.plot(x = trip_duration).geomHistogram()


//            filter(speed > 2 & speed < 1e2) %>%
//
//        plot(aes(speed)) +
//
//            geom_histogram(fill = "red", bins = 50) +
//
//            labs(x = "Average speed [km/h] (direct distance)")

    // #' 5.3 Final cleaning


//        Here we apply the cleaning filters that are discussed above. This code block is likely to expand as the analysis progresses.

//
//        train <- train %>%
//
//            filter(trip_duration < 22*3600,
//
//                dist > 0 | (near(dist, 0) & trip_duration < 60),
//
//        jfk_dist_pick < 3e5 & jfk_dist_drop < 3e5,
//
//        trip_duration > 10,
//
//        speed < 100)

    trainData.countExpr { it[distance] gt 0 }

    trainData = trainData
        // just consider reasonable short trips
        .filter { it[trip_duration] gt 10 * 3600 }
        .filter { it[distance] gt 0 }
        .filter { it[trip_duration] gt 60 }
//

    //todo cool spurious trips 5.2 Intermission - The best spurious trips

    // 6 External data

    var weatherData = DataFrame.readCSV(File("/Users/brandl/projects/kotlin/krangl-kotlinconf/data/weather_data_nyc_centralpark_2016.csv"))


    // parse the dates
    val weatherDatePattern = DateTimeFormatter.ofPattern("d-M-yyyy")
    LocalDate.parse("1-1-2016", weatherDatePattern)
    weatherData = weatherData.addColumn("date") {
        it["date"].map<String> { LocalDate.parse(it, weatherDatePattern) }
    }

    // remove odd Ts with minimal value
    weatherData = weatherData.addColumns(
        "rain" to { weatherData.rows.map { if (it["precipitation"] == "T") 0.01 else it["precipitation"].toString().toDouble() } },
        "s_fall" to { weatherData.rows.map { if (it["snow fall"] == "T") 0.01 else it["snow fall"].toString().toDouble() } },
        "s_depth" to { weatherData.rows.map { if (it["snow depth"] == "T") 0.01 else it["snow depth"].toString().toDouble() } },
        "has_snow" to { (it["s_fall"] gt 0) OR (it["s_depth"] gt 0) },
        "has_rain" to { it["rain"] gt 0 }
    ).rename("maximum temperature" to "max_temp", "minimum temperature" to "min_temp")

    val weatherSelect = weatherData.select("date", "rain", "s_fall", "has_snow", "has_rain", "s_depth", "max_temp", "min_temp")

    trainData = trainData.addColumn("date") { it[pickup_datetime].map<LocalDateTime> { it.toLocalDate() } }.leftJoin(weatherSelect, by = "date")

    // save intermediate result
    trainData.writeTSV(File("taxi_with_weather.txt"))
    trainData = DataFrame.readTSV(File("taxi_with_weather.txt"))

    // 6.1.2 Visualisation and impact on trip_duration


    //fixme why is speed always 0
    trainData.groupBy("date").summarize("median_speed" to { it["speed"].median() }).plot("date", "median_speed")
        .geomLine(color = RColor.orange, size = 1.5.roundToInt())
        .labs(x = "Date", y = "Median speed")

//            rain = as.numeric(ifelse(precipitation == "T", "0.01", precipitation)),
//
//    s_fall = as.numeric(ifelse(`snow fall` == "T", "0.01", `snow fall`)),
//
//    s_depth = as.numeric(ifelse(`snow depth` == "T", "0.01", `snow depth`)),
//
//    all_precip = s_fall + rain,
//
//    has_snow = (s_fall > 0) | (s_depth > 0),
//
//    has_rain = rain > 0,
//
//    max_temp = `maximum temperature`,
//
//    min_temp = `minimum temperature`)

    // join on date (using objects)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 7 Correlations overview
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // todo how to remove correlated features

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 9 A simple model and prediction
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // objective predict the target `trip_duration`

    trainData = trainData.remove(id)


    // todo implement this

    // todo build final feature matrix


    //do a roc curve
//    predicts

}

fun DataFrame.constrainCoord(): DataFrame = filter {
    with(it[dropoff_longitude]) { (this gt -74.05) AND (this lt -73.05) } AND
        with(it[dropoff_latitude]) { (this gt 40.6) AND (this lt 40.9) }
}

object CurrentStep {
    @JvmStatic
    fun main(args: Array<String>) {

        var trainData = DataFrame.readTSV(File("someTaxiRides.csv"))

        val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        trainData = trainData.addColumns(
            pickup_datetime to { it[pickup_datetime].map<String> { LocalDateTime.parse(it, datePattern) } }
        )

        trainData = trainData.addColumn(distance) { df ->
            val longDist = df[pickup_longitude] - df[dropoff_longitude]
            val latDist = df[pickup_latitude] - df[dropoff_latitude]
            (longDist * longDist + latDist * latDist).asDoubles().map { Math.sqrt(it!!) }
        }


        trainData.filter {
            it[pickup_datetime].isMatching<LocalDateTime> {
                toLocalDate().isAfter(LocalDate.parse("2016-01-20")) && toLocalDate().isBefore(LocalDate.parse("2016-02-10"))
            }
        }.plot(x = pickup_datetime)
            .geomHistogram()
            .title("Pickup Counts in Winter")

    }
}

