(ns scripts.cryogen-migration
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))



(defn map->md-metadata "Takes a cryogen metadata map, and parses understood keys into markdown metadata"
  [inp-map]
  (println (format "Remapping metadata: %s, %s, %s" (:title inp-map)
                   (:date inp-map)
                   (apply str (interpose ", " (:tags inp-map)))))
  (format "Title: %s\nDate: %s\nTags: %s"
          (or (:title inp-map) "FIXME")
          (:date inp-map)
          (or (apply str (interpose ", " (:tags inp-map))) "FIXME")))

(defn get-post-data [opts path]
  (try (let [[_ post-date post-filename] (re-find #"^(\d{4}-\d{2}-\d{2})-([. \w -]+).md"
                                                  (fs/file-name path))
             _ (println "Gathered filename data")
             post-content (slurp (str path))
             [_ post-metadata] (re-find #"^(\{[\w: \n\[\]\{\}\#\"-,]+\})\n"
                                        post-content)
             _ (println (format "Gathered metadata:\n%s" post-metadata))
             {:keys [title tags] :as parsed-metadata } (edn/read-string 
                                                        post-metadata)
             new-metadata (assoc parsed-metadata :date post-date)
             
             new-content (str/replace post-content post-metadata (map->md-metadata new-metadata))]
         
         (println (format "Processed %s" post-filename))
         {:cryogen/filename post-filename
          :cryogen/post-date post-date
          :cryogen/post-tags tags
          :cryogen/post-title title
          :cryogen/post-content new-content
          :cryogen/old-post-metatdata post-metadata
          :cryogen/old-post-content post-content})
       (catch Exception e {:cryogen/path path
                            :cryogen/exception e})))

(defn spit-new-post [{:keys [post-dir]:as opts} {:keys [:cryogen/filename :cryogen/post-content]:as post}]
  (spit (str post-dir filename ".md") post-content))


(defn get-cryogent-posts [{:keys [:croygen/posts-directory] :as opts}]
  (let [post-paths (fs/glob posts-directory "*.m")
        post-data (map (partial get-post-data opts) post-paths)]
    (assoc opts :cryogen/post-paths 9)))

(defn main [opts]
  (->> (get-cryogent-posts {:croygen/posts-directory "posts/"}
                           )
       ()))

(comment
  (get-post-data {} "posts/2021-03-18-wallflower-session-fifteen.md")

  

  (map (partial spit-new-post {:post-dir "./posts/"}) (map (partial get-post-data {}) 
                                                           (fs/glob "./posts" "*.md")))
  
  
  (edn/read (fs/file "post/2021-03-09-wallflower-session-two.md"))

  (edn/read-string "")

  (apply str (interpose ", " ["rpg" "session log" "lancer" "wallflower"]))

  (re-find #"^(\{[\w: \n\[\]\{\}\#\"-]+\})\n" "{:title \"Wallflower Session 5: Ghosts On The Wind\"
:layout :post
:date \"2021-05-14\"
:tags [\"rpg\" \"session log\" \"lancer\" \"wallflower\"]}\n")
  )