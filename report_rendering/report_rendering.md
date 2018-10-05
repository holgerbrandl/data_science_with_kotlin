## How to render the wild ride?


```bash
#cd /Users/brandl/projects/kotlin/data_science_with_kotlin/report_rendering/nyc

kts2html.sh /Users/brandl/projects/kotlin/data_science_with_kotlin/src/main/kotlin/WildRideThroughNYC.kts
```

To render the ipynb separately simplyt change the output format

```bash
jupyter nbconvert --ExecutePreprocessor.kernel_name=kotlin  --to notebook --execute  WildRideThroughNYC.ipynb
```


## More basic example Example

```bash
#cd /Users/brandl/projects/kotlin/data_science_with_kotlin/report_rendering/examples

kts2html.sh IrisReport.kts
kts2html.sh /Users/brandl/projects/kotlin/data_science_with_kotlin/src/main/kotlin/misc_tests.kts

## start jupy
jupyter

```

## System Setup


1. Install `R` form http://r-project.org and `rmarkdown` package (which we currently need for kts to markdown conversion

```bash
R -e 'install.packages("rmarkdown")'
```

2. Install https://github.com/aaren/notedown

3. Install Jupyter from http://jupyter.org/

**TODO** this maybbe outdated, so the latest jupyter may be just fin

notedown doesn't work with Jupter Notebook 5.1.0 #60
https://github.com/aaren/notedown/issues/60

So make sure to get the latest version with

```
jupyter notebook --version

sudo pip3 uninstall notebook
sudo pip3 install notebook==5.0.0
sudo pip3 install notebook==5.2.1
pip install --user  https://github.com/aaren/notedown/tarball/kernelspec
notedown --help

jupyter notebook --help
jupyter notebook 

```

4. Install the kotlin kernel from https://github.com/ligee/kotlin-jupyter


