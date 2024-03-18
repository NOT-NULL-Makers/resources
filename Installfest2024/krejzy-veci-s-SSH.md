# Krejzy věci s SSH

## Creating a network namespace to simulate 50 ms RTT

```
sudo ip netns add ns-bad
sudo ip link add veth-good type veth peer name veth-bad
sudo ip link set veth-bad netns ns-bad
sudo ip netns exec ns-bad ip link set dev veth-bad up && sudo ip netns exec ns-bad ip address add 172.16.0.1/24 dev veth-bad
sudo ip link set dev veth-good up && sudo ip addr add 172.16.0.2/24 dev veth-good
sudo ip netns exec ns-bad tc qdisc add dev veth-bad root netem delay 25ms 
sudo tc qdisc add dev veth-good root netem delay 25ms
```

## Destroying that setup

```
sudo ip netns del ns-bad
sudo ip link del veth-good
```

## Run sshd in the network namespace

```
sudo ip netns exec ns-bad bash
/usr/sbin/sshd
# or
sudo ip netns exec ns-bad /usr/sbin/sshd
```

## Stunnel configuration

Copy file with BOM and insert the rest of the configuration.

```
# Create BOM for the configuration file
sudo bash -c 'echo -e "\xef\xbb\xbf; BOM before the semicolon!" > /etc/stunnel/stunnel.conf'
```

### Server

```
cat /etc/stunnel/server.conf 
; BOM before the semicolon!
setuid = stunnel4
setgid = stunnel4

[example server]
accept     = 2999
connect    = 127.0.0.1:9999
debug      = 3
PSKsecrets = /etc/stunnel/psk.txt
setuid     = stunnel4
setgid     = stunnel4
```

### Client

```
cat /etc/stunnel/client.conf 
; BOM before the semicolon!
setuid = stunnel4
setgid = stunnel4

[example client]
client     = yes
accept     = 127.0.0.1:2999
connect    = 172.16.0.1:2999
debug      = 3
PSKsecrets = /etc/stunnel/psk.txt
setuid     = stunnel4
setgid     = stunnel4
```

### Pre-Shared Key and Permissions

```
# Create random Pre-Shared Key
sudo bash -c 'dd if=/dev/urandom bs=32 count=1 | base64 > /etc/stunnel/psk.txt'
sudo bash -c 'sed -i "1s/^/psk:/" /etc/stunnel/psk.txt'
# Correct permissions, this is for Debian
sudo chmod 640 /etc/stunnel/psk.txt
sudo chown :stunnel4 /etc/stunnel/psk.txt
```

## Run a bandwidth test with stunnel

```
# Turn on the loopback device in the network namespace
sudo ip netns exec ns-bad ip link set dev lo up
# Turn on stunnel in the network namespace
sudo ip netns exec ns-bad bash
stunnel /etc/stunnel/server.conf
# Listen for data for the bandwidth test
nc.traditional -lp 9999 > /dev/null
# Now outside the namespace
exit
sudo stunnel /etc/stunnel/client.conf
dd if=/dev/zero bs=1M status=progress | nc 127.0.0.1 9999
# You can check that it really goes over veth e.g. with sar from sysstat
sar -n DEV --iface=veth-good 1
```

### Ending stunnel

```
# If needed, kill stunnel
pkill stunnel
```

## Test with SSH

```
dd if=/dev/zero bs=1M status=progress | ssh 172.16.0.1 "cat - > /dev/null"
# Or create a file filled from /dev/urandom, to prevent compression doing anything, a video file is usually suitable as well
```
