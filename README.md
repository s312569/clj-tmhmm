# clj-tmhmm

A Clojure library to run TMHMM and parse results.

## Installation

Leiningren: [clj-tmhmm "0.1.1"]

In project: (:require [clj-tmhmm.core :as tm])

## Usage

To run TMHMM (assuming the TMHMM perl script is in your path) on a
collection of fasta sequences (see `clj-fasta`):
```clojure
user> (with-open [r (io/reader "/path/to/fasta-file.fasta")]
        (tmhmm (fasta-seq r) "/path/out-prefix"))
(#object[java.io.File 0x1e6855b2 "/path/out-prefix-1.tmhmm"])
user>
```
Results are sent to the outfile in the TMHMM 'short' format.

To run TMHMM on a file of fasta formatted protein sequences use
`tmhmm-file` which returns a file object containing the results in
the TMHMM 'short' format:
```clojure
user> (tmhmm-file "/path/to/fasta-file.fasta" "/path/out-prefix")
(#object[java.io.File 0x1e6855b2 "/path/out.prefix-1.tmhmm"])
user>
``` 

To parse a TMHMM results file that is in the TMHMM 'short' format
use `tmhmm-seq` which returns a lazy list of maps with the result of
TMHMM on each protein:
```clojure
user> (with-open [r (io/reader "path/to/result/file")]
        (doall (->> (tmhmm-seq r)
                    (take 2))))
({:accession "c10010_g1_i1|m.3253", :len 150, :expaa 0.0, :first60 0.0,
 :predhel 0, :topology "o"} {:accession "c10035_g1_i1|m.3256", :len 66,
 :expaa 0.0, :first60 0.0, :predhel 0, :topology "i"})
user> 
```

If you have a collection of fasta sequences, `filter-tmhmm` will
filter sequences containing >= to the specified number of
trans-membrane domains:
```clojure
user> (with-open [r (io/reader "/path/to/fasta-file.fasta")]
                    (filter-tmhmm (fa/fasta-seq r)))
({:accession "c10692_g1_i1|m.3373", :description "c10692_g1_i1|g.3373 
 ORF c10692_g1_i1|g.3373 c10692_g1_i1|m.3373 type:5prime_partial len:153
 (+) c10692_g1_i1:3-461(+)", :sequence "GTGILSIGSALLGADLVFGFDVDLNSIETAQK
SARDRGLLGVEFIRIDVRRVGRLRKFRGTVDTVVMNPPFGTRLRGADFCFIEAAVKISKGNIYSLHKTSTRN
QLVKKIKRNLSRETRALAELRFDLAKSYKFHKMKEKEILVDLLAVLAE"} {:accession 
"c11063_g1_i1|m.3444", :description "c11063_g1_i1|g.3444  ORF
 c11063_g1_i1|g.3444 c11063_g1_i1|m.3444 type:internal len:118 (-)
 c11063_g1_i1:2-352(-)", :sequence "KTKSFFLMLLLLGGDIESNPGPTTCQICKQIQQTEE
ENVCSICQNCMVEGPSQATIVIDDADTRPTYEKQPIEQPEKPPQIFTDTKSINNVYQIDPTHPIIPSEHYSN
YLRQFENIK"})
user> 
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
