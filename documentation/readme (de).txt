----------------------------------------
Installation
----------------------------------------

Das mitgelieferte MaryTTS benötigt Java 8, mit neueren Java-Versionen scheint es nicht zu funktionieren.
Die Variable "JAVA_HOME" in den Batch-Dateien im Ordner "bin" muss entsprechend angepasst werden und auf den Java-Installationsordner verweisen.


----------------------------------------
Anwendung starten
----------------------------------------

Die notwendigen Batch-Dateien befinden sich im Ordner "bin" und sind durchnummeriert.

1. MaryTTS-Server starten und abwarten, bis im Konsolenfenster der Zusatz "started in <xxx> s on port 59125" erscheint.
2. Klappmaul starten.
3. Eine kompatible Kontrollanwendung starten, z.B. die mitgelieferte "DefaultControlApp" oder Visual SceneMaker mit einem Executor, der das RobotEngine-Protokoll unterstützt.


----------------------------------------
Anwendung steuern
----------------------------------------

Die Kamera-Ansicht kann mit der Tastatur gesteuert werden.

- W: hineinzoomen
- S: herauszoomen
- A bzw. D: horizontale Drehung um den Charakter
- Pfeil hoch bzw. Pfeil runter: vertikale Drehung

Die KlappmaulEngine wartet auf dem konfigurierten Port auf Command Messages von einer kompatiblen Kontrollanwendung.
Alternativ können Command Messages direkt in der Konsole eingegeben werden.

Um die Anwendung zu beenden, kann man entweder die 3D-Ansicht schließen oder "exit" in die Konsole eingeben.


----------------------------------------
Konfiguration
----------------------------------------

Die Konfigurationsdateien befinden sich im Ordner "res/config".

Netzwerk-Verbindung
-------------------
Die IP-Adresse und der Port, die zur Ansteuerung der KlappmaulEngine benötigt werden, befinden sich in "appConfig_Klappmaul.properties". Standardmäßig sind diese auf 127.0.0.1 und 1241 eingestellt.

3D-Fenster
----------
Parameter, um die Start-Darstellung des 3D-Fensters anzupassen, befinden sich in "engineConfig_Klappmaul.properties".
Dies sind die Fenster-Position und -Größe sowie der Winkel der Kamera relativ zum Koordinatensystem des Charakters.

Klappmaul
---------
In "robotConfig_Klappmaul.properties" befinden sich schließlich alle Parameter, welche direkt die Funktionsweise des Roboters betreffen.

- name: Der Name, der im Fenstertitel angezeigt wird.
- textures.world.*: Die Texturen, die als Würfel um das Klappmaul herum angezeigt werden.
- model.robot.*: Parameter, welche das 3D-Modell betreffen.
- speech.engine: Auswahl der Sprachausgabe - entweder MaryTTS, abstrakter MIDI-Klang oder stumme Textausgabe.
- speech.*: Weitere Parameter speziell für die gewählte Sprachausgabe.