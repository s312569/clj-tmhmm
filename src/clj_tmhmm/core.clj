(ns clj-tmhmm.core
  (:require [clj-fasta.core :as fa]
            [biodb.core :as bdb]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as st]
            [clj-commons-exec :refer [sh]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- split-fields
  [line]
  (let [fs (st/split line #"\s")
        vals (map #(st/split % #"=") (rest fs))]
    (->> (cons (first fs)
               (map #(condp = (first %)
                       "len" (Integer/parseInt (second %))
                       "ExpAA" (Float/parseFloat (second %))
                       "First60" (Float/parseFloat (second %))
                       "PredHel" (Integer/parseInt (second %))
                       "Topology" (second %))
                    vals))
         vec)))

(defn tmhmm-seq
  "Takes a reader opened on a TMHMM results file (in the 'short'
  format) and returns and lazy list of maps representing the TMHMM
  result for each protein."
  [reader]
  (let [ks [:accession :len :expaa :first60 :predhel :topology]]
    (->> (line-seq reader)
         (map split-fields)
         (map #(into {} (map vector ks %))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; run tmhmm
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tmhmm
  "Runs TMHMM on a collection of protein fasta sequences (see
  clj-fasta) and returns a collection of result files. Processes the
  sequences 10,000 at a time and outputs the results to outfile in the
  TMHMM 'short' format."
  [coll outfile]
  (let [c (atom 0)
        wd (fs/temp-dir "tmhmm-wd")
        fl (atom [])]
    (try
      (doall
       (pmap #(let [o (fs/absolute (str outfile "-" (swap! c inc) ".tmhmm"))
                    in (fs/absolute (fa/fasta->file % (fs/temp-file "tmhmm-in-")))]
                (swap! fl conj o)
                (try
                  (with-open [out (io/output-stream o)]
                    (let [tm @(sh ["tmhmm" "-workdir" (str wd) "-short" (str in)]
                                  {:out out} :close-err? false)]
                      (if (= 0 (:exit tm))
                        o
                        (if (:err tm)
                          (throw (Exception. (str "TMHMM error: " (:err tm))))
                          (throw (Exception. (str "Exception: " (:exception tm))))))))
                  (finally
                    (fs/delete in))))
             (partition-all 10000 coll)))
      (catch Exception e
        (doseq [f @fl] (fs/delete f))
        (throw e))
      (finally
        (fs/delete-dir wd)))))

(defn tmhmm-file
  "Runs TMHMM on each protein in a file of fasta formatted protein
  sequences. Process the sequences 10,000 at a time and outputs the
  results to outfile in the TMHMM 'short' format."
  [file outfile]
  (with-open [r (io/reader file)]
    (tmhmm (fa/fasta-seq r) outfile)))

(defn filter-tmhmm
  "Takes a collection of fasta protein sequences (see clj-fasta) and
  filters those sequences that contain >= 'tms'. Is 'semi-lazy' as it
  processes 1000 sequence chunks of the collection at a time."
  ([coll] (filter-tmhmm coll 2))
  ([coll tms]
   (->> (pmap #(let [rf (tmhmm % (fs/temp-file "tmhmm-out-"))
                     rset (with-open [r (io/reader (first rf))]
                            (->> (tmhmm-seq r)
                                 (filter (fn [x] (>= (:predhel x) tms)))
                                 (map :accession)
                                 set))]
                 (try
                   (filter (fn [x] (rset (:accession x))) %)
                   (finally (fs/delete (first rf)))))
              (partition-all 1000 coll))
        (reduce concat))))
