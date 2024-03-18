# SSH Basics

## SSH with many keys on client

```
-oPubkeyAuthentication=no
-oIdentityFile=%d/.ssh/id_ed25519
-oIdentitiesOnly=yes
-oProxyJump=<intermediate host>
```
