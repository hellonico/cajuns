(ns small-tiles.core
 (:require
  [small-tiles.e01]
  [small-tiles.e02]
  [small-tiles.e03]
  )
 )

(defn ^:export init[]
 (small-tiles.e03/init)
 )