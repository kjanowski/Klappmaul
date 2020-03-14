----------------------------------------
Installation
----------------------------------------

The included version of MaryTTS requires Java 8, it appears that it does not work with newer Java versions.
Accordingly, the variable "JAVA_HOME" in the batch files in the "bin" folder must be adjusted to point to the Java 8 installation directory.


----------------------------------------
Starting the Application
----------------------------------------

The necessary batch files are found in the "bin" folder. They are numbered.

1. Start the MaryTTS server and wait until the console window shows the text "started in <xxx> s on port 59125".
2. Start a Klappmaul robot.
3. Start a compatible control application, e.g. the included "DefaultControlApp".


----------------------------------------
Controlling the Klappmaul Application
----------------------------------------

The camera perspective can be controlled with the keyboard.

- W: zoom in
- S: zoom out
- A or D: horizontal rotation around the character
- Arrow up or down: vertical rotation

The KlappmaulEngine uses the configured port to listen for command messages from a compatible control application.
Alternatively, command messages can be entered directly into the console.

To quit the application, you can either close the 3D window or enter the word "exit" into the console.


----------------------------------------
Configuration
----------------------------------------

The configuration files are found in the folder "res/config".

Network Connection
------------------
The IP address and the port which are required for connecting to the KlappmaulEngine are set in "appConfig_Klappmaul.properties". By default, they are set to 127.0.0.1 and 1241.

3D Window
---------
Parameters for adjusting the initial display of the 3D window are found in "engineConfig_Klappmaul.properties".
Those are the window position and size, as well as the camera orientation.

Klappmaul
---------
In "robotConfig_Klappmaul.properties" you can find all parameters which directly concern the appearance and functionality of the virtual robot.

- name: The name to be shown in the window title.
- textures.world.*: The textures shown as a box surrounding the character.
- model.robot.*: Parameters concerning the character's 3D model.
- speech.engine: Selection of the speech engine - either MaryTTS, abstract MIDI sounds or silent text display.
- speech.*: Additional parameters required by the selected speech engine.