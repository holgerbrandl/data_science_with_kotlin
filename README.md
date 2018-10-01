# Building Data Science Workflows with Kotlin

### >> [slides](https://holgerbrandl.github.io/data_science_with_kotlin/data_science_with_kotlin.html) <<


## Abstract

Kotlin's language design and its great tooling provide a wonderful framework for data science. Still evolving are libraries for convenient and _kotlin-esque_ table manipulation and reporting.

In this session I would like to present the design and features of `krangl`, which is a {K}otlin DSL for data w{rangl}ing. By mimicking well established concepts from pandas and R, it implements a grammar of data manipulation using a modern functional-style API. It allows to filter, transform, aggregate and reshape tabular data. Clearly static types are preferable when using Kotlin, but very often data is fluent and has no immediate type. To bridge this gap, `krangl` provides means to toggle between typed and untyped data.

As an example, we will discuss how to compete at kaggle with workflows written in Kotlin. To facilitate that, we will use Jupyter to convert Kotlin scripts into HTML/notebooks.



## About me

Holger Brandl works as a data scientist at the Max Planck Institute of Molecular Cell Biology and Genetics (Dresden, Germany). He holds a Ph.D. degree in machine learning, and has developed new concepts in the field of computational linguistics. More recently he has co-authored publications in high-ranking journals such as Nature and Science. He is actively contributing to the Kotlin community by developing tools and libraries for bioinformatics, high-performance computing and data-science.



## Sources

For slide sources see `docs`

For example code see `src`


# Taxi challenge

We picked the taxi challenge from Kaggle as an example

https://www.kaggle.com/c/nyc-taxi-trip-duration


## Other interesting kernels of the Taxi Competition


* [NYC Taxi EDA - Update: The fast & the curious](https://www.kaggle.com/headsortails/nyc-taxi-eda-update-the-fast-the-curious)


* [From EDA to the Top (LB 0.367)](https://www.kaggle.com/gaborfodor/from-eda-to-the-top-lb-0-367)


* [NYCT - from A to Z with XGBoost](https://www.kaggle.com/karelrv/nyct-from-a-to-z-with-xgboost-tutorial)



All kernels can be found at https://www.kaggle.com/c/nyc-taxi-trip-duration/kernels.

## References

Also see [Kotlin’s emerging data-science ecosystem](https://holgerbrandl.github.io/kotlin4ds_kotlin_night_frankfurt//emerging_kotlin_ds_ecosystem.html which I gave at the first Kotlin Night in Frankurt in spring 2018


Repo Pointers

* https://github.com/holgerbrandl/krangl
* https://github.com/holgerbrandl/kravis
* https://github.com/ligee/kotlin-jupyter
* https://www.rstudio.com/



# talk pointers

https://medium.com/@Pinterest_Engineering/the-case-against-kotlin-2c574cb87953

![](.kotlinconf_2018_krangl_abstract_images/644a6593.png)

https://kotlinfrompython.wordpress.com/2017/05/27/why-program-in-kotlin-instead-of-python/

what is easier to read
![](.kotlinconf_2018_krangl_abstract_images/254de216.png)

or a table header


no real performance data
