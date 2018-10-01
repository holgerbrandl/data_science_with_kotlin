//' ## Flowers Analysis

//@file:DependsOnMaven("com.github.holgerbrandl:kravis:0.4-SNAPSHOT")
@file:DependsOnMaven("com.github.holgerbrandl:kravis:0.4")


import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import kravis.geomCol
import kravis.plot

//' peak into iris data
//irisData

enum class Gender { male, female }
data class Person(val name: String, val gender: Gender, val heightCm: Int, val weightKg: Double)

//' define some persons
val persons = listOf(
    Person("Max", Gender.male, 192, 80.3),
    Person("Anna", Gender.female, 162, 56.3),
    Person("Maria", Gender.female, 172, 66.3)
)

SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice()

//' peek into persons
persons

//' visualize some persons
persons.plot(x = {name}, y = { weightKg }, fill = { gender.toString() })
    .geomCol()
    .xLabel("height [m]")
    .yLabel("weight [kg]")
    .title("Body Size Distribution")
    .show()