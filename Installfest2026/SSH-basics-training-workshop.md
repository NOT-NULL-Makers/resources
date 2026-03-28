# SSH Basics / Training (SSH Prakticky)

## Setup for Lab

```
# Add directly into ~/.ssh/config or in a file in the ~/.ssh/ directory and include it with the following line:
# Include training_config
Host training europe
        Hostname training.notnullmakers.com

Host europe4
        Hostname 178.104.107.48

Host europe6
        Hostname 2a01:4f8:1c18:6896::1

Host west-coast4
        Hostname 5.78.187.117

Host west-coast6
        Hostname 2a01:4ff:1f0:1411::1

Host training europe europe6 west-coast4 west-coast6
        User root
        IdentityFile %d/.ssh/hetzner-nnm
        IdentitiesOnly yes
```

On `nbg1-training1` host add into /etc/hosts file:

```
# /etc/hosts
5.78.187.117 west-coast
2a01:4ff:1f0:1411::1 west-coast
```

That way, we can address the server by this alias without the need to write fully qualified names from DNS or IP addresses.

On `hil1-training1` we can add the following limitation:

```
# /etc/ssh/sshd_config
Match Address !178.104.107.48,!2a01:4f8:1c18:6896::1,*
        DenyGroups training
```

Don't forget to restart sshd to apply the configuration e.g. with `systemctl restart sshd`.

Which should limit the users in the group training to connect from `nbg1-training1`.

## Create The Lab Users

```
addgroup training
for u in abel bob carmen dido elias fred gustav hank ian jules karel luna mia nina oleg paul quentin richard simon tomas ursula vit walter xena yale zara \
  adam bert cameron david eva felix gabriel hunter ivan jana kevin leo mark nadia omar pablo qasim rose saul tara uma vivian wren xavier yael zelda; do 
  adduser --quiet --disabled-password --ingroup training --comment "" $u;
  # set password
  echo "$u:nnm-101-$u" | chpasswd;
done
```

## nginx Reverse Proxy for TCP

Relay tcp port 3022 from `nbg1-training1` to `hil1-training1` tcp port 22 (where SSHD listens by default).

Install the appropriate packages as needed on `nbg1-training1`. On Debian that can be achieved using `apt install nginx libnginx-mod-stream`.

Then add the appropriate configuration:

```
# /etc/nginx/nginx.conf
stream {
        server {
                listen 3022;
                proxy_pass [2a01:4ff:1f0:1411::1]:22;
        }
}
```

Check the validity of the configuration `nginx -t` and apply the configuration by `systemctl reload nginx`.

## More Articles to Read

Petr Krčmář about scp/ sftp:

https://www.root.cz/clanky/protokol-scp-mizi-proc-je-jednoduchy-prenos-souboru-po-ssh-problem/

and

https://www.root.cz/clanky/konec-protokolu-scp-plny-der-a-neopravitelny-ale-uzivatelsky-prijemny/

and this commit by Damien Miller:

https://undeadly.org/cgi?action=article;sid=20210910074941 and

OpenSSH 10.0 release notes: https://www.openssh.com/txt/release-10.0

https://www.root.cz/clanky/openssh-ma-vlastni-obranu-proti-hadani-hesel-jak-presne-funguje/

```
This release switches scp(1) from using the legacy scp/rcp protocol
to using the SFTP protocol by default.
```
