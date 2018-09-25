

# Fetch the data


```bash
kaggle competitions download -c nyc-taxi-trip-duration
```



# rebuild report

```bash
echo '
@file:DependsOn("com.github.holgerbrandl:kravis:0.4-SNAPSHOT")
//@file:DependsOn("com.github.holgerbrandl:kravis:-SNAPSHOT")

//@file:DependsOn("ml.dmlc:xgboost4j:0.80")

@file:DependsOn("com.github.haifengl:smile-core:1.5.1")
@file:DependsOn("com.github.haifengl:smile-plot:1.5.1")
    
@file:KotlinOpts("-J-Xmx5g")
@file:MavenRepository("jitpack.io","https://jitpack.io" )

'  > taxi_shell_launcher.kts


#kscript --idea taxi_shell_launcher.kts
kscript --clear-cache
#kshell_from_kscript.sh taxi_shell_launcher.kts
export PATH=/Users/brandl/projects/kotlin/kscript/misc/kshell_launcher/:$PATH
kshell_from_kscript.sh taxi_shell_launcher.kts


```


