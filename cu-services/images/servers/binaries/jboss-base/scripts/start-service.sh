#!/bin/bash

set -x

export ENV_FILE="/etc/environment"
# Ajout des variables d'environnement
export CU_USER=$1
export CU_PASSWORD=$2
export CU_REST_IP=$3	
export CU_DATABASE_NAME=$4
export JAVA_HOME=/cloudunit/java/$5
export JBOSS_HOME=/cloudunit/binaries

# Database password for Manager
export MANAGER_DATABASE_PASSWORD=$6
# To do difference between main and test env
export ENV_EXEC=$7

# ENVOI NOTIFICATION CHANGEMENT DE STATUS
if [ $ENV_EXEC = "integration" ];
then
    export MYSQL_ENDPOINT=cuplatform_testmysql_1.mysql.cloud.unit
else
    export MYSQL_ENDPOINT=cuplatform_mysql_1.mysql.cloud.unit
fi

pid1=0
pid2=0

term_handler() {
  if [ $pid1 -ne 0 ]; then
    $JBOSS_HOME/bin/jboss-cli.sh -c --user=$CU_USER --password=$CU_PASSWORD --command=:shutdown
	/cloudunit/scripts/waiting-for-shutdown.sh java 30
	rm -rf /cloudunit/appconf/standalone/logs/*
	$JAVA_HOME/bin/java -jar /cloudunit/tools/cloudunitAgent-1.0-SNAPSHOT.jar SERVER $MYSQL_ENDPOINT $CU_DATABASE_NAME $CU_USER STOP $MANAGER_DATABASE_PASSWORD
  fi
  if [ $pid2 -ne 0 ]; then
    kill -SIGTERM "$pid2"
  fi
  exit 42;
}


if [ ! -f /init-service-ok ]; then

	ln -s /cloudunit/appconf/standalone /cloudunit/binaries/standalone
	ln -s /cloudunit/appconf/standalone.conf /cloudunit/binaries/bin/standalone.conf

	echo "Start Services and configure password for $1"

	# Transforme les variables en variables d'environnements
	echo "CU_USER=$CU_USER" >> $ENV_FILE
	echo "CU_PASSWORD=$CU_PASSWORD" >> $ENV_FILE
	echo "CU_REST_IP=$CU_REST_IP" >> $ENV_FILE
	echo "CU_DATABASE_NAME=$CU_DATABASE_NAME" >> $ENV_FILE
	echo "JAVA_HOME=$JAVA_HOME" >> $ENV_FILE
   	echo "JAVA_OPTS=" >> $ENV_FILE
   	echo "JBOSS_HOME=$JBOSS_HOME" >> $ENV_FILE

	# Ajout de l'utilisateur et modif du home directory
	useradd $1 && echo "$CU_USER:$CU_PASSWORD" | chpasswd && echo "root:$CU_PASSWORD" | chpasswd

	# GENERATION CLES SSH POUR LIEN AVEC MODULES
	ssh-keygen -t rsa -N "" -f /root/.ssh/id_rsa
	mkdir -p $CU_USER_HOME/.ssh
	cp /root/.ssh/id_rsa.pub $CU_USER_HOME/.ssh
	cp /root/.ssh/id_rsa $CU_USER_HOME/.ssh
	cp /root/.bashrc $CU_USER_HOME
	cp /root/.profile $CU_USER_HOME

	# Affection du homedirectory à l'utilisateur unix
	usermod -d $CU_USER_HOME $CU_USER

	# Ajout du Shell à l'utilisateur
	usermod -s /bin/bash $CU_USER

	# Création de l'utilisateur admin de JBOSS
	echo "+-- Add Jboss admin user"
	$JBOSS_HOME/bin/add-user.sh --silent=true $CU_USER $CU_PASSWORD

	# to close the first call
	touch /init-service-ok
fi

# Le montage /cloudunit n'appartient qu'à l'utilisateur créé
chown -R $CU_USER:$CU_USER /cloudunit

# Lancement de ssh et jboss
/usr/sbin/sshd
until [ "`nc -z localhost 22 && echo $?`" -eq "0" ];
do
	echo "\nWaiting for sshd process to start"
	sleep 1
done
cd

su - $CU_USER -c "$JBOSS_HOME/bin/standalone.sh -P=/etc/environment -Djboss.bind.address.management=0.0.0.0 -Djboss.bind.address=0.0.0.0&"

ps -ef
env

# test du démarrage de jboss
/cloudunit/scripts/test-jboss-start.sh

$JAVA_HOME/bin/java -jar /cloudunit/tools/cloudunitAgent-1.0-SNAPSHOT.jar SERVER $MYSQL_ENDPOINT $CU_DATABASE_NAME $CU_USER START $MANAGER_DATABASE_PASSWORD

# The sshd pid could be double : father and son
pid1=`pidof sshd | awk '{if ($2) {print $2;} else {print $1}}'`
pid2=`pidof java`

# wait indefinetely
while true
do
  tail -f /dev/null & wait ${!}
done

tailf /var/log/faillog
