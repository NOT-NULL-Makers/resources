```clojure
(ns ipv6-day
  (:import (java.net InetAddress Inet6Address Inet4Address Socket)))

(comment
  (import '(java.net InetAddress Inet6Address Inet4Address))

  (def original-properties (System/getProperties))

  (System/getProperties)

  (InetAddress/getByName "google.com")
  (vec (InetAddress/getAllByName "google.com"))

  ;; Disable resolution cache
  (System/setProperty "sun.net.inetaddr.ttl" "0")
  (System/setProperty "sun.net.inetaddr.stale.ttl" "0")
  (System/setProperty "sun.net.inetaddr.negative.ttl" "0")

  ;; default -1 -> forever
  (select-keys (System/getProperties) ["sun.net.inetaddr.ttl"
                                       "sun.net.inetaddr.stale.ttl"
                                       "sun.net.inetaddr.negative.ttl"])

  (java.security.Security/setProperty "networkaddress.cache.ttl" "0")
  ;; (java.security.Security/setProperty "networkaddress.cache.negative.ttl" "0")
  ;; (java.security.Security/setProperty "networkaddress.cache.stale.ttl" "0")

  ;; https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/doc-files/net-properties.html
  ;; https://docs.oracle.com/en/java/javase/21/docs/api/system-properties.html

  ;; https://datatracker.ietf.org/doc/draft-ietf-happy-happyeyeballs-v3/

  ;; https://bugs.openjdk.org/browse/JDK-8170568
  ;; https://bugs.openjdk.org/browse/JDK-8179037

  ;; Beware _JAVA_OPTIONS environment variable

  (System/setProperty "java.net.preferIPv6Addresses" "true")
  (System/setProperty "java.net.preferIPv4Stack" "true")
  (System/setProperty "java.net.preferIPv6Addresses" "false")
  (select-keys (System/getProperties) ["java.net.preferIPv6Addresses" "java.net.preferIPv4Stack"])

  (time (InetAddress/getByName "google.com"))
  (time (vec (InetAddress/getAllByName "google.com")))
  (time (InetAddress/getByName "notnullmakers.com"))

  (Inet6Address/getByName "google.com")
  (aget (InetAddress/getAllByName "google.com") 0)

  (InetAddress/getLocalHost)
  (InetAddress/getLoopbackAddress)
  (vec (InetAddress/getAllByName "localhost"))

  (System/setProperty "java.net.preferIPv6Addresses" "system")
  (System/setProperty "java.net.preferIPv6Addresses" "true")

  (InetAddress/systemAddressesOrder)

  (Socket/connect )

  ;; Must have permission to send ICMP_ECHO using a raw socket, or will try TCP
  ;; Best effort is made by the implementation to try to reach the host, but firewalls and server
  ;; configuration may block requests resulting in a unreachable status while some specific ports
  ;; may be accessible. A typical implementation will use ICMP ECHO REQUESTs if the privilege can
  ;; be obtained, otherwise it will try to establish a TCP connection on port 7 (Echo) of the
  ;; destination host.
  (map (fn [address] [(str address) (.isReachable address 2000)]) (vec (InetAddress/getAllByName "google.com")))

  (map (fn [address] [(str address) (.isReachable address 2000)]) (vec (InetAddress/getAllByName "nebezi.cz")))

  (System/getProperties)

  ;(ns-unmap *ns* 'Inet6Address)
    (InetAddress/getByName "")
    (UUID/randomUUID)
  
    ;assert data.length == 16 : "data must be 16 bytes in length";
  
    ;for (int i = 0; i < 8; ++i) {
    ;     msb = msb << 8 | (long) (data [i] & 255);
    ;}
  
    ;for (int i = 8; i < 16; ++i) {
    ;     lsb = lsb << 8 | (long) (data [i] & 255);
    ;}
  
    ; index is the first index in the byte array ba that should be read,
    ; upper-bound is the position in the array that shouldn't be read (max length of array)
    )
  
  (declare from-inet-address)
  
  (defrecord inet4-address [^int address]
  
    Object
    (toString [inet4]
      (.toString (from-inet-address inet4))))
  
  ;; Should also do reader literals
  (defrecord inet6-address [^long msb ^long lsb]
  
    Object
    (toString [inet6]
      (.toString (from-inet-address inet6))))
  
  (defn bytes->num
    [ba index upper-bound]
    (loop [i index acc 0]
      (if (< i upper-bound)
        (recur (inc i) (+ (bit-shift-left acc 8) (bit-and (aget ba i) 0xff)))
        acc)))
  
  (defn num->bytes
    [ba num index upper-bound]
    (loop [i index shift (* (- (dec upper-bound) index) 8)]   ; 56 for long ; 24 for int presumably
      (if (< i upper-bound)
        (do (aset ba i (unchecked-byte (bit-and (bit-shift-right num shift) 0xff)))
            (recur (inc i) (- shift 8)))
        ba)))
  
  (defn ->inet-address
    [^String inet-str]
    (let [inet (InetAddress/getByName inet-str)]
      (cond (instance? Inet6Address inet)
            (let [^bytes inet6-bytes (.getAddress ^Inet6Address inet)]
              (inet6-address. (bytes->num inet6-bytes 0 8)
                              (bytes->num inet6-bytes 8 16)))
  
            (instance? Inet4Address inet)
            (let [^bytes inet4-bytes (.getAddress ^Inet4Address inet)]
              (inet4-address. (unchecked-int (bytes->num inet4-bytes 0 4)))))))
  
  (defn from-inet-address
    [inet]
    (cond (instance? inet6-address inet)
          (-> ^bytes (make-array Byte/TYPE 16)
              (num->bytes (.msb inet) 0 8)                    ; get the first half of the address as long
              (num->bytes (.lsb inet) 8 16)                   ; get the second half of the address as long
              (Inet6Address/getByAddress))
  
          (instance? inet4-address inet)
          (-> ^bytes (make-array Byte/TYPE 4)
              (num->bytes (.address inet) 0 4)
              (Inet4Address/getByAddress))))
  
  (comment
  
    (def address-1 (->inet-address "2001:db8::1"))            ; 48 B
    (def address-2 (->inet-address "2a02:8308:299:4600::7b87"))
    (def address-3 (->inet-address "2001:db8::1"))
  
    (def address-old (InetAddress/getByName "2001:db8::1"))
  ; 24 B Impl
  ; 24 B Inet6Address
  ; 16 B Reference
  ; 32 B Holder
)
```
