#!/usr/bin/env bb

(require '[clojure.string :as str])

(def ipv4 "178.104.107.48")
(def ipv6 "2a01:4f8:1c18:6896::1")
(def materials "https://notnullmakers.com/public/media/ssh-basics.pdf")

(def names
  ["abel" "bob" "carmen" "dido" "elias" "fred" "gustav" "hank" "ian" "jules"
   "karel" "luna" "mia" "nina" "oleg" "paul" "quentin" "richard" "simon" "tomas"
   "ursula" "vit" "walter" "xena" "yale" "zara" "adam" "bert" "cameron" "david"
   "eva" "felix" "gabriel" "hunter" "ivan" "jana" "kevin" "leo" "mark" "nadia"
   "omar" "pablo" "qasim" "rose" "saul" "tara" "uma" "vivian" "wren" "xavier"
   "yael" "zelda"])

(defn gen-card [name]
  (let [password (str "nnm-101-" name)
        domain-ssh (str "ssh " name "@training.notnullmakers.com")
        ip4-ssh (str "ssh " name "@" ipv4)
        ip6-ssh (str "ssh " name "@" ipv6)]
    (str "
    <div class='page'>
      <h2>Přístupové údaje: " name "</h2>
      <hr>
      <p><strong>Přihlašovací jméno:</strong> <code>" name "</code></p>
      <p><strong>Přihlašovací heslo:</strong> <code>" password "</code></p>
      <hr>
      <pre>" domain-ssh "</pre>
      <pre>" ip4-ssh "</pre>
      <pre>" ip6-ssh "</pre>
      <hr>
      <p><strong>Materiály:</strong><br>
      <a href='" materials "'>" materials "</a></p>
    </div>")))

(def html-template
  (str "
<!DOCTYPE html>
<html>
<head>
<meta charset='UTF-8'>
<style>
  @page { size: A5; margin: 15mm; }
  body { font-family: sans-serif; line-height: 1.5; color: #333; }
  .page { page-break-after: always; border: 1px dashed #ccc; padding: 20px; height: 100%; }
  code, pre { background: #f4f4f4; padding: 4px 8px; border-radius: 4px; font-family: monospace; }
  pre { display: block; margin: 10px 0; font-size: 1.1em; border: 1px solid #ddd; }
  h2 { margin-top: 0; color: #2c3e50; text-transform: capitalize; }
  hr { border: 0; border-top: 1px solid #eee; margin: 20px 0; }
</style>
</head>
<body>
" (str/join "\n" (map gen-card names)) "
</body>
</html>"))

(spit "training_credentials.html" html-template)
(println "Generated training_credentials.html with" (count names) "entries.")
