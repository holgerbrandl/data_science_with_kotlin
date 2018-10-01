#!/usr/bin/env bash

if [ $# -lt 1 ]; then
     echo "Usage: kts2html.sh <kotlin_scrpts> [additional arguments]+" >&2 ; exit 1;
fi

inputScript=$1
#inputScript=IrisReport.kts

## todo replace with kscriptlet
reportName=$(basename $inputScript .kts)

# https://www.r-project.org/
Rscript - ${inputScript} <<"EOF"
knitr::spin(commandArgs(T)[1], doc = "^//'[ ]?", knit=F)
EOF

mv ${inputScript%.kts}.Rmd $reportName.Rmd

# https://github.com/holgerbrandl/kscript
kscript -t 'lines.map { it.replace("{r }", "")}.print()' ${reportName}.Rmd > ${reportName}.md


if [ ! -f "${reportName}.md" ]; then
    echo "Markdown conversion of $inputScript failed" >&2 ; exit 1;
fi

# https://github.com/aaren/notedown
notedown ${reportName}.md > ${reportName}.ipynb

# http://jupyter.org/install
jupyter nbconvert --ExecutePreprocessor.kernel_name=kotlin --execute --to html ${reportName}.ipynb --output ${reportName}

