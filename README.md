# Anon-GateWay

## Abstrato

Densenvolvimento de uma rede overlay de anonimização do originador através de nós intermédios anonimizadores, que permitem simular uma rede end-to-end.

## Correr o CORE
```
sudo /etc/init.d/core-daemon start
core-gui
```

## Correr o Server
```
mini-httpd -d /srv/ftp/
ps –ef
```

## Correr o AnonGW
```
AnonGW target-server 10.3.3.1 port 80 overlay-peers 10.1.1.1
```

## Wget ao AnonGW
```
wget http://10.1.1.1/ficheiro
```


## Autores

* [Diogo Ribeiro](https://github.com/ribeiropdiogo)
* [José Monteiro](https://github.com/DxMonteiro)
* [Rui Reis](https://github.com/Syrayse)
