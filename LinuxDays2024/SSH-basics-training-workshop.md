# SSH Basics / Training (SSH Prakticky)

## Setup for Lab

```
# Add directly into ~/.ssh/config or in a file in the ~/.ssh/ directory and include it with the following line:
# Include training_config
Host europe
        HostName 162.55.58.201 # now unavailable, could also be e.g. training.notnullmakers.com or nbg1-training1.notnullmakers.com
        User root
        IdentityFile %d/.ssh/hetzner-nnm
        IdentitiesOnly yes

Host europe6
        HostName 2a01:4f8:1c1b:84b8::1 # now unavailable, could also be e.g. training.notnullmakers.com or nbg1-training1.notnullmakers.com
        User root
        IdentityFile %d/.ssh/hetzner-nnm
        IdentitiesOnly yes

Host west-coast
        HostName 5.78.122.215 # now unavailable, could also be e.g. hil1-training1.notnullmakers.com
        User root
        IdentityFile %d/.ssh/hetzner-nnm
        IdentitiesOnly yes

Host west-coast6
        HostName 2a01:4ff:1f0:9536::1 # now unavailable, could also be e.g. hil1-training1.notnullmakers.com
        User root
        IdentityFile %d/.ssh/hetzner-nnm
        IdentitiesOnly yes
```

This configuration can be reduced further to:

```
Host europe
	HostName 162.55.58.201

Host europe6
	HostName 2a01:4f8:1c1b:84b8::1

Host west-coast
	HostName 5.78.122.215

Host west-coast6
	HostName 2a01:4ff:1f0:9536::1

Host europe europe6 west-coast west-coast6
        User root
        IdentityFile %d/.ssh/hetzner-nnm
        IdentitiesOnly yes
```

On `nbg1-training1` host add into /etc/hosts file:

```
# /etc/hosts
5.78.122.215 west-coast
2a01:4ff:1f0:9536::1 west-coast
```

That way, we can address the server by this alias without the need to write fully qualified names from DNS or IP addresses.

On `hil1-training1` we can add the following limitation:

```
# /etc/ssh/sshd_config
Match Address !162.55.58.201,!2a01:4f8:1c1b:84b8::1,*
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

## Configure nftables for Port Forwarding using NAT

Forward tcp port 3222 from `nbg1-training1` to `hil1-training1` tcp port 22 (where SSHD listens by default).

On `nbg1-training1`:

```
nft add table ip nat
nft 'add chain ip nat prerouting { type nat hook prerouting priority -100; }'
nft 'add rule ip nat prerouting tcp dport 3222 dnat to tcp dport map { 3222 : 5.78.122.215 . 22 }'

nft 'add chain ip nat postrouting { type nat hook postrouting priority 100; }'
nft 'add rule ip nat postrouting ip daddr 5.78.122.215 masquerade'

nft add table ip6 nat
nft 'add chain ip6 nat prerouting { type nat hook prerouting priority -100; }'
nft 'add rule ip6 nat prerouting tcp dport 3222 dnat ip6 to tcp dport map { 3222 : 2a01:4ff:1f0:9536::1 . 22 }'

nft 'add chain ip6 nat postrouting { type nat hook postrouting priority 100; }'
nft 'add rule ip6 nat postrouting ip6 daddr 2a01:4ff:1f0:9536::1 masquerade'
# This should work with snat as well, but it did not for me as discussed later in the note.
# nft 'add rule ip6 nat postrouting ip6 daddr 2a01:4ff:1f0:9536::1 snat to 2a01:4f8:1c1b:84b8::1'

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
                proxy_pass [2a01:4ff:1f0:9536::1]:22;
        }
}
```

Check the validity of the configuration `nginx -t` and apply the configuration by `systemctl reload nginx`.

