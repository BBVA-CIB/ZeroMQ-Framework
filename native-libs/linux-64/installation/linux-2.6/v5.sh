#!/bin/bash
#: Name  : v5.sh
#: Desc  : This script installs the zeromq native libraries in the linux v5.
#: Author: JLJB Comunytek BBVA  
#: Date  : 28/10/2015
############################################################################



#Constants
##########
LOCALPATH=./linux-2.6/
LIBZMQ_LA=libzmq.la
LIBJZMQ_LA=libjzmq.la


DATE=$(date +%d%m%Y)


if [[ -d /var/log ]]; then
        ERRORS_FILE=/var/log/zeromq-installation-errors-linux-5-$DATE.log
else
        ERRORS_FILE=./zeromq-installation-errors-linux-5-$DATE.log
fi

printf "%s\n" "---------------------- Installing ZeroMq core libraries. ----------------------------"
if [[ -d $ZEROMQPATH ]]; then
	./libtool --mode=install /usr/bin/install -c $LOCALPATH$LIBZMQ_LA "$ZEROMQPATH" 2>> $ERRORS_FILE
else
	./libtool --mode=install /usr/bin/install -c $LOCALPATH$LIBZMQ_LA '/usr/local/lib' 2>> $ERRORS_FILE
fi

return_code=$?

#if some error occurs, the script will exit with error code launched by the previous , otherwise continue
if [[ $return_code  -ne 0 ]]; then
	printf "%s\n" " ===== An error has been occurred while calling libtool to install zeromq libraries. For more information see the log file."
	printf "%s\n" " ===== Log file: $FILE_ERRORS"
	exit $return_code
fi

printf "%s\n" "----------------------- Installing JNI binding for ZeroMq ---------------------------"
if [[ -d $ZEROMQPATH ]]; then
	./libtool --mode=install /usr/bin/install -c $LOCALPATH$LIBJZMQ_LA "$ZEROMQPATH" 2>> $ERRORS_FILE
else
	./libtool --mode=install /usr/bin/install -c $LOCALPATH$LIBJZMQ_LA "/usr/local/lib" 2>> $ERRORS_FILE
fi

error_code=$?

if [[ $error_code -ne 0 ]]; then
        printf "%s\n" " ===== An error has been occurred while calling libtool to install zeromq JNI. For more information see the log file."
        printf "%s\n" " ===== Log file: $ERRORS_FILE"
        exit $error_code
fi

printf "%s\n" "----------------------- Native libraries and JNI for ZeroMq has been successfully installed. -------------"

printf "\n%s\n\n" "===== ZeroMQ has been installed succsessfully at $(date +%D%T) ====="
exit 0
