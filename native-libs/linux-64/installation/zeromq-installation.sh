#!/bin/bash
#: Name  : zeromq-installation.sh
#: Desc  : This script installs the zeromq native libraries in the linux system.
#	   Initially it identifies the versions 5 an 7 for the cores 2.6 and 3.10
#: Author: JLJB Comunytek BBVA	
#: Date  : 28710/2015
######################################################################################



#Constants
###########
PROGRAM_NAME="zeromq.run"
VERSION="1.0"
E_NOROOT=100
ROOT_UID=0



#Enable in case $UID hasn't been inizialited in the OS
######################################################
#UID =$(id -u)


#funcions
showHelp()
{

	printf "\n%s\n\n" "########### ZeroMQ libraries installation ###########"
	printf "\t%s\n" "zeromq.run makes the installation of all the zeromq libraries needed"
	printf "\t%s\n\n" "to execute any program tha uses this iterface to send or receive messages."
	printf "\t%s\n" "To launch the installation only execute ./zermq.run is enougth. This way"
	printf "\t%s\n\n" "will install all the libraries needed in the /usr/local/lib directoryi."
	printf "\t%s\n" "If you need to install them in other directory, you only have to export"
	printf "\t%s\n" "the environment variable ZEROMQPATH with the absolute path you want to use"
	printf "\t%s\n\n" "in the meaner export ZEROMQPATH=/usr/local/de/..."
	printf "\t%s\n\n" "You must bee root to launch this program"

}
showArguments()
{
	printf "\n%s\n\n" "########### ZeroMQ libraries installation ###########"
	printf "%s\n\n"  "Parameters availables:"
	printf "\t%-15s:\t %s\n" "--help | -h" "Shows help about zeromq.run does"
	printf "\t%-15s:\t %s\n" "--version | -v" "Shows the zeromq.run version"
	printf "\n%s\n\n" "No other arguments are availables in this version"
}
showVersion()
{
	printf "\n%s\n\n" "########### ZeroMQ libraries installation ###########"
	printf "\t%-15s %s\n" "PROGRAM NAME:" "$PROGRAM_NAME"
	printf "\t%-15s %s\n\n" "VERSION:" "$VERSION"
}

if [[ $# -ne 0 ]]; then
        
case "$1" in
	--help | -h)
	showHelp
	exit 0
	;;
	--version | -v)
	showVersion
	exit 0
	;;
esac
	showArguments
	exit 0

fi

if [[ $UID -ne $ROOT_UID ]]; then 
  printf "\n%s\n\n" " =========== ERROR: You Must be root to run this program ==================="
  exit $E_NOROOT
fi

KERNEL_VERSION=$(uname -r)

printf "\n%s\n\n" "---------- Kernel version detected: $KERNEL_VERSION ----------"

if [[ $KERNEL_VERSION = *2.6* ]]; then 
	#Calling to install zeromq libraries for linux 5
	printf "%s\n\n" "---------- Calling to install zeromq libraries for linux 5 ----------"
	./linux-2.6/v5.sh
else
	#Calling to install zeromq libraries for linux 7
	printf "%s\n\n" "---------- Calling to install zeromq libraries for linux 7 ----------"
	./linux-3.10/v7.sh
fi

return_code=$?

if [[ $return_code -ne 0 ]]; then
	exit $return_code
fi

exit 0

