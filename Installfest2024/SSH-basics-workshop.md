# SSH Basics (SSH Prakticky)

## SSH with many keys on client

```
-oPubkeyAuthentication=no
-oIdentityFile=%d/.ssh/id_ed25519
-oIdentitiesOnly=yes
-oProxyJump=<intermediate host>
```

## Ad-hoc create many users for training purposes

```
for u in petra jan karel anna; do 
  adduser --quiet --disabled-password --gecos "$u" $u;
  # set password
  echo "$u:nnm-101-$u" | chpasswd;
done
```

Get more czech names:
https://cs.wikipedia.org/wiki/Seznam_rodn%C3%BDch_jmen_v_%C4%8Cesku
