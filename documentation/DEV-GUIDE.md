
# CloudUnit developpement environment

You are reading the wright guide, if you want to setup an environment to contribute to CloudUnit development.
If you simply wish to test CloudUnit, you should maybe run our [Demo Vagrant box](https://github.com/Treeptik/CloudUnit/blob/master/DEMO-GUIDE.md), if you want to setup a CloudUnit server, in order to frequently use it read our [Server guide](https://github.com/Treeptik/cloudunit/blob/master/SERVER-GUIDE.md).

## Requirements

* A linux Ubuntu/Debian or MacOSX computer. Windows is not tested yet. 
* Vagrant 1.7.8+
* VirtualBox 5.0.4+
* Npm 4.x+ for Grunt and Bower stack

## Installation 

CloudUnit uses Docker and Java but others components. As pre-requisites, you need to install them to have a complete dev stack.

### Pre-requisites

## Local DNS

You need to add a local DNS entry pointing to the vagrant IP address. More precisely, any address ending with demo.cloudunit.io shoud point to `192.168.50.4`. On Ubuntu, a simple way to achieve this is to install dnsmasq:
```
$ sudo apt-get install dnsmasq
```
Then edit the file `/etc/dnsmasq.conf` and add the line:
```
address=/admin.cloudunit.dev/192.168.50.4
```
Finally, restart dnsmasq:
```
$ sudo service dnsmasq restart

```

### Pre-requisites



