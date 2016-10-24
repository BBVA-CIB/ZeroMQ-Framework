; zeromq-w7-installation.nsi
;
; This script remember the directory, 
; has uninstall support and (optionally) installs start menu shortcuts.
;
; It will install zeromq-w7-setup.nsi into a directory that the user selects,

;--------------------------------
!include "MUI2.nsh"

!define _INSTATALLER_VERSION "1_0_0"
!define _ZERMQ_VERSION "4_0_4"

!define MUI_WELCOMEFINISHPAGE_BITMAP "D:\desarrollo\mensajeria\0mq\instalacion\windows\Avatar-bbva.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "D:\desarrollo\mensajeria\0mq\instalacion\windows\Avatar-bbva.bmp"
!define MUI_ICON "D:\desarrollo\mensajeria\0mq\instalacion\windows\ZeroMQ.ico"
!define MUI_UNICON "D:\desarrollo\mensajeria\0mq\instalacion\windows\ZeroMQ.ico"
!define MUI_HEADERIMAGE_BITMAP "D:\desarrollo\mensajeria\0mq\instalacion\windows\ZeroMQ.ico"
!define MUI_HEADER_TEXT "ZeroMQ Installation"
!define MUI_WELCOMEPAGE_TITLE "ZeroMQ ${_ZERMQ_VERSION} installation for Windows"
!define MUI_WELCOMEPAGE_TEXT "This installation will copy to your system all the libraries that your applications will need to use the ZeroMQ message framework."

!define MUI_DIRECTORYPAGE_TEXT_DESTINATION "Directory"


!define MUI_UNFINISHPAGE_NOAUTOCLOSE


!insertmacro MUI_PAGE_WELCOME
!define MUI_PAGE_HEADER_SUBTEXT "Choose which components of the zeromq installation you need to install"
!insertmacro MUI_PAGE_COMPONENTS
!define MUI_PAGE_HEADER_SUBTEXT "Destination for the ZeroMQ ${_ZERMQ_VERSION} libraries."
!define MUI_DIRECTORYPAGE_TEXT_TOP "All the files that you chose in the last step, will be copied in the directory you choose below."
!insertmacro MUI_PAGE_DIRECTORY

!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_TITLE "ZeroMQ has been installed successfully"	
!define MUI_FINISHPAGE_TEXT "Remember to provide the installation directory to the java.library.path variable. You can use the format: -Djava.library.path=[path]. Adding this directory to the system path is not necessary."
!insertmacro MUI_PAGE_FINISH


!define MUI_PAGE_HEADER_TEXT "ZeroMQ ${_ZERMQ_VERSION} Uninstall confirmation"
!define MUI_PAGE_HEADER_SUBTEXT ""
!define MUI_UNCONFIRMPAGE_TEXT_TOP "This process will remove all the zeromq native libraries and all the registry keys added."
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!define MUI_FINISHPAGE_TITLE "ZeroMQ has been unstalled successfully"
!define MUI_FINISHPAGE_TEXT "ZeroMQ ${_ZERMQ_VERSION} libraries and all the files they need has been removed from your computer."	
!insertmacro MUI_UNPAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

; The name of the installer
Name "zeromq-w7-setup-${_INSTATALLER_VERSION}"

; The file to write
OutFile "zeromq-w7-setup-${_ZERMQ_VERSION}.exe"

; The default installation directory
InstallDir c:\zeromq\

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\NSIS_zeromq" "Install_Dir"

; Request application privileges for Windows Vista
; RequestExecutionLevel admin


; The stuff to install
Section "ZeroMQ ${_ZERMQ_VERSION} libraries" Section1

  SectionIn RO
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  ;File "zeromq-w7-setup.nsi"
  
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v90-mt-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v90-mt-gd-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v100-mt-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v100-mt-gd-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v110-mt-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v110-mt-gd-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v120-mt-4_0_4.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\libzmq-v120-mt-gd-4_0_4.dll"

 
  
  ; Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\NSIS_zeromq "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\zeromq" "DisplayName" "NSIS zeromq"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\zeromq" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\zeromq" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\zeromq" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
SectionEnd

Section "Java Binding for ZeroMQ" Section2
File "D:\desarrollo\mensajeria\0mq\instalacion\windows\jzmq.dll"
SectionEnd

Section "System Libraries" Section3


  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp100.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp110.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp110_clr0400.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp120.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp120_clr0400.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp120d.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp140.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcp140d.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr100.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr100_clr0400.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr110.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr110_clr0400.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr120.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr120_clr0400.dll"
  File "D:\desarrollo\mensajeria\0mq\instalacion\windows\msvcr120d.dll"
  
SectionEnd
; Optional section (can be disabled by the user)
;Section "Start Menu Shortcuts"

;  CreateDirectory "$SMPROGRAMS\zeromq"
;  CreateShortCut "$SMPROGRAMS\zeromq\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
;  CreateShortCut "$SMPROGRAMS\zeromq\zeromq (MakeNSISW).lnk" "$INSTDIR\zeromq.nsi" "" "$INSTDIR\zeromq.nsi" 0
  
;SectionEnd

;--------------------------------


LangString DESC_Section1 ${LANG_ENGLISH} "This option will install all the ZeroMQ native libraries."
LangString DESC_Section2 ${LANG_ENGLISH} "This option will install the java binding which allows you to develope applications in java using ZeroMQ."
LangString DESC_Section3 ${LANG_ENGLISH} "This option will install all the native windows libraries that some of the computers does not have."

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${Section1} $(DESC_Section1)
  !insertmacro MUI_DESCRIPTION_TEXT ${Section2} $(DESC_Section2)
  !insertmacro MUI_DESCRIPTION_TEXT ${Section3} $(DESC_Section3)
!insertmacro MUI_FUNCTION_DESCRIPTION_END


; Uninstaller

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\zeromq"
  DeleteRegKey HKLM SOFTWARE\NSIS_zeromq

  ; Remove files and uninstaller
  ; Delete $INSTDIR\zeromq-w7-setup.nsi
  
  Delete $INSTDIR\jzmq.dll
  Delete $INSTDIR\libzmq-v90-mt-4_0_4.dll
  Delete $INSTDIR\libzmq-v90-mt-gd-4_0_4.dll
  Delete $INSTDIR\libzmq-v100-mt-4_0_4.dll
  Delete $INSTDIR\libzmq-v100-mt-gd-4_0_4.dll
  Delete $INSTDIR\libzmq-v110-mt-4_0_4.dll
  Delete $INSTDIR\libzmq-v110-mt-gd-4_0_4.dll
  Delete $INSTDIR\libzmq-v120-mt-4_0_4.dll
  Delete $INSTDIR\libzmq-v120-mt-gd-4_0_4.dll
  Delete $INSTDIR\msvcp100.dll
  Delete $INSTDIR\msvcp110.dll
  Delete $INSTDIR\msvcp110_clr0400.dll
  Delete $INSTDIR\msvcp120.dll
  Delete $INSTDIR\msvcp120_clr0400.dll
  Delete $INSTDIR\msvcp120d.dll
  Delete $INSTDIR\msvcp140.dll
  Delete $INSTDIR\msvcp140d.dll
  Delete $INSTDIR\msvcr100.dll
  Delete $INSTDIR\msvcr100_clr0400.dll
  Delete $INSTDIR\msvcr110.dll
  Delete $INSTDIR\msvcr110_clr0400.dll
  Delete $INSTDIR\msvcr120.dll
  Delete $INSTDIR\msvcr120_clr0400.dll
  Delete $INSTDIR\msvcr120d.dll
  

  Delete $INSTDIR\uninstall.exe

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\zeromq\*.*"

  ; Remove directories used
  RMDir "$SMPROGRAMS\zeromq"
  RMDir "$INSTDIR"

SectionEnd

;!insertmacro MUI_PAGE_FINISH
