# SSH commented speedrun from beginner to advanced

## Setup for Lab

```
# Add directly into ~/.ssh/config or in a file in the ~/.ssh/ directory and include it with the following line:
# Include training_config
Host europe
    HostName 2.28.229.111 # now unavailable, could also be e.g. training.notnullmakers.com or nbg1-training1.notnullmakers.com
    User root
    IdentityFile %d/.ssh/hetzner-nnm
    IdentitiesOnly yes

Host europe6
    HostName 2a01:4f8:1c18:8773::1
    User root
    IdentityFile %d/.ssh/hetzner-nnm
    IdentitiesOnly yes

Host west-coast
    HostName 5.78.213.143 # now unavailable, could also be e.g. hil1-training1.notnullmakers.com
    User root
    IdentityFile %d/.ssh/hetzner-nnm
    IdentitiesOnly yes

Host west-coast6
    HostName 2a01:4ff:1f0:c470::1
    User root
    IdentityFile %d/.ssh/hetzner-nnm
    IdentitiesOnly yes
```

This configuration can be reduced further to:

```
Host europe
	HostName 2.28.229.111

Host europe6
	HostName 2a01:4f8:1c18:8773::1

Host west-coast
	HostName 5.78.213.143

Host west-coast6
	HostName 2a01:4ff:1f0:c470::1

Host europe europe6 west-coast west-coast6
    User root
    IdentityFile %d/.ssh/hetzner-nnm
    IdentitiesOnly yes
```

On `nbg1-training1` host add into /etc/hosts file:

```
# /etc/hosts
5.78.213.143 west-coast
2a01:4ff:1f0:c470::1 west-coast
```

Get known hosts entries for host

```
ssh-keygen -F training.notnullmakers.com
```

Get SSH public key finger prints

```
# europe
for f in /etc/ssh/ssh_host_*.pub; do ssh-keygen -lf ${f}; done
256 SHA256:sneUGkP/3zOHwCFZoDlA0xFVCQQzgvTWz3Jc14mc+LQ root@nbg1-training1 (ECDSA)
256 SHA256:QX9HDKeDpSVQ9PFKZ6s5TvvUAkvCIBHlRw0A38ePoH8 root@nbg1-training1 (ED25519)
3072 SHA256:zV+yUxCiF4iy/buQe4qOW3VRFYldgjgs4J/Cn74NYs8 root@nbg1-training1 (RSA)

# west-coast
for f in /etc/ssh/ssh_host_*.pub; do ssh-keygen -lf ${f}; done
256 SHA256:lMjnry01G7X+U17ZbjGYt+AJavgUMMiJqU/GRniMlDU root@hil1-training1 (ECDSA)
256 SHA256:KE6B/ZUE0inDJ2gEL9fGE8ClWZRV0d6LCrB8c1BzNCY root@hil1-training1 (ED25519)
3072 SHA256:mhm7rXmee59T7SIwFCYGNlIw1dTTl7GIo4dOUBzcKUM root@hil1-training1 (RSA)
```


That way, we can address the server by this alias without the need to write fully qualified names from DNS or IP addresses.

On `hil1-training1` we can add the following limitation:

```
# /etc/ssh/sshd_config
Match Address !2.28.229.111,!2a01:4f8:1c18:8773::1,*
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

## Connecting using a jump host to RDP

`ssh -L 3389:prg-app-2:3389 -J sdrssrv01 prg-srv-1`

## Setting up CIFS/ Samba for efficient file sharing

- passwords
- limit to localhost

```
sudo apt update && sudo apt install samba cifs-utils
testparm

grep -vE "^\s*(#|;|$)" /etc/samba/smb.conf
[global]
   # interfaces = 127.0.0.0/8 ::1 lo
   # bind interfaces only = yes
   workgroup = WORKGROUP
   smb ports = 445
   log file = /var/log/samba/log.%m
   max log size = 1000
   logging = file
   panic action = /usr/share/samba/panic-action %d
   server role = standalone server
   server min protocol = SMB3_00
   server smb encrypt = required
   ntlm auth = ntlmv2-only
   vfs objects = fruit streams_xattr
   fruit:metadata = stream
   fruit:model = MacSamba
   fruit:posix_rename = yes
   obey pam restrictions = yes
   unix password sync = yes
   passwd program = /usr/bin/passwd %u
   passwd chat = *Enter\snew\s*\spassword:* %n\n *Retype\snew\s*\spassword:* %n\n *password\supdated\ssuccessfully* .
   pam password change = yes
   map to guest = Never
   invalid users = root
   usershare allow guests = no
[homes]
   comment = Home Directories
   browseable = no
   read only = no
   create mask = 0600
   directory mask = 0700
   valid users = %S
   path = /home/%S
   root preexec = test -d /home/%S

---
sudo systemctl disable --now nmbd
sudo systemctl restart smbd
sudo smbpasswd -a administrator
New SMB password:
Retype new SMB password:
Added user administrator.

# Test:
# smbclient //localhost/$USER -U $USER

# /var/lib/samba/private/passdb.tdb


mount -t cifs //<SERVER-IP>/<username> /mnt/myhome -o username=<username>,uid=$(id -u),gid=$(id -g),seal
(add _netdev for systemd to wait on network devices, if in fstab)

# grep -vE "^\s*(#|;|$)" /etc/samba/smb.conf
```

## Configure nftables for Port Forwarding using NAT

Forward tcp port 3222 from `nbg1-training1` to `hil1-training1` tcp port 22 (where SSHD listens by default).

On `nbg1-training1`:

```
nft add table ip nat
nft 'add chain ip nat prerouting { type nat hook prerouting priority -100; }'
nft 'add rule ip nat prerouting tcp dport 3222 dnat to tcp dport map { 3222 : 5.78.213.143 . 22 }'

nft 'add chain ip nat postrouting { type nat hook postrouting priority 100; }'
nft 'add rule ip nat postrouting ip daddr 5.78.213.143 masquerade'

nft add table ip6 nat
nft 'add chain ip6 nat prerouting { type nat hook prerouting priority -100; }'
nft 'add rule ip6 nat prerouting tcp dport 3222 dnat ip6 to tcp dport map { 3222 : 2a01:4ff:1f0:c470::1 . 22 }'

nft 'add chain ip6 nat postrouting { type nat hook postrouting priority 100; }'
nft 'add rule ip6 nat postrouting ip6 daddr 2a01:4ff:1f0:c470::1 masquerade'
# This should work with snat as well, but it did not for me as discussed later in the note.
# nft 'add rule ip6 nat postrouting ip6 daddr 2a01:4ff:1f0:c470::1 snat to 2a01:4f8:1c18:8773::1'

nft list ruleset
sysctl -w net.ipv4.conf.eth0.forwarding=1
sysctl -w net.ipv6.conf.eth0.forwarding=1
```

You can watch the traffic on `nbg1-training1` with `tcpdump -nn port 3222 or host west-coast`.

The IPv6 masquerade didn't work for me possibly because of the missing module CONFIG_NF_NAT_MASQUERADE_IPV6
in Debian. (https://superuser.com/questions/1751062/ipv6-masquerading-on-linux)

You can make these changes permanent by `nft list ruleset > /etc/nftables.conf && chmod 644 /etc/nftables.conf` and enabling the service `systemctl enable nftables`.
Then you should add the sysctl commands to `/etc/sysctl.d/99-sysctl.conf`.

Don't forget to add appropriate filtering rules for other traffic that should not be routed/ forwarded.

## nginx Reverse Proxy for TCP

Relay tcp port 3022 from `nbg1-training1` to `hil1-training1` tcp port 22 (where SSHD listens by default).

Install the appropriate packages as needed on `nbg1-training1`. On Debian that can be achieved using `apt install nginx libnginx-mod-stream`.

Then add the appropriate configuration:

```
# /etc/nginx/nginx.conf
stream {
        server {
                listen 3022;
                proxy_pass [2a01:4ff:1f0:c470::1]:22;
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
