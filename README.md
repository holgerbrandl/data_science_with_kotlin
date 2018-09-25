

# Fetch the data


```bash
kaggle competitions download -c nyc-taxi-trip-duration
```



# rebuild report

```bash
echo '
@file:DependsOn("com.github.holgerbrandl:kravis:0.4-SNAPSHOT")
//@file:DependsOn("com.github.holgerbrandl:kravis:-SNAPSHOT")

@file:KotlinOpts("-J-Xmx5g")
@file:MavenRepository("jitpack.io","https://jitpack.io" )

'  > taxi_shell_launcher.kts


#kscript --idea taxi_shell_launcher.kts
kshell_from_kscript.sh taxi_shell_launcher.kts


```






## ToDO

* send to expression detect and paste mode console should work in `kts` files
* Fix travis for kravis
* purge import buffer for send-to-console hotykey


https://www.kaggle.com/c/google-analytics-customer-revenue-prediction/?utm_medium=email&utm_source=intercom&utm_campaign=comp-launch-20180912

https://medium.com/@maxsiani/tackling-a-problem-with-machine-learning-6f4650cf80a9

1.5 lm workflow

1h krangl io



## explore
* https://plot.ly/scala/
* explore tablesaw + vis api
* jOOQ



* execute in built-in terminal https://youtrack.jetbrains.com/issue/IDEA-131964