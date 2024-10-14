# Krejzy věci s SSH'

## wstunnel

Download binary from https://github.com/erebe/wstunnel to the server and the client

```
# Drop in the search path for all to use, add executable permission
chmod +x /usr/local/bin/wstunnel

# Launch on server to listen on all IPs for incomming connections
wstunnel server wss://[::]:8080 --restrict-to 127.0.0.1:22

# Now launch on the client, replace host <example> with the servers IP or DNS name
wstunnel client -L tcp://9999:127.0.0.1:22 wss://<example>.com:8080

# Now you can connect to the server using the websocket
ssh -p9999 -oPubkeyAuthentication=no <user-on-remote-server>@localhost
```

More information: 
https://www.root.cz/clanky/websocket-jako-cesta-k-uniku-z-prilis-restriktivni-site/

## nftables NAT/ Port Forwarding and nginx Reverse Proxy

Please see the workshop notes held the same day before the talk.

## Limiting Users Based on Group Membership in sshd_config

```
# /etc/ssh/sshd_config
Match Address !192.0.2.1,!2001:db8:4321::1234,*
        DenyGroups training
```

## More Articles To Read

Geolocation script for nftables:
https://github.com/wirefalls/geo-nft

Root series by Petr Krčmář about nftables, especially the following articles:
https://www.root.cz/clanky/nftables-akce-provadene-nad-pravidly-vcetne-nastaveni-nat/

https://www.root.cz/clanky/nftables-struktury-pro-zvyseni-vykonu-firewallu/

https://www.root.cz/clanky/nftables-priklad-konfigurace-firewallu-a-vzorove-situace/

Root article by Ondřej Caletka about new SSH limiting features:
https://www.root.cz/clanky/openssh-ma-vlastni-obranu-proti-hadani-hesel-jak-presne-funguje/

Restrict keyword in authorized_keys
https://manpages.debian.org/stable/openssh-server/authorized_keys.5.en.html#restrict
