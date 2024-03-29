NDS-controller Android host
===============

[Link to the NDS/3DS program](https://github.com/Louisvh/NDS-controller/).

## Purpose
NDS-controller is a client app for the Nintendo DS that allows the DS to 
connect to an Android device over WiFi and to function as an input device. 
The main use case is controlling emulators: most emulated real-time games 
tends to be a pain to play on a touch screen and greatly benefit from being 
played on a gamepad. Sure, a Bluetooth gamepad only costs like $50, but the 
Nintendo DS that is gathering dust in your closet is free!

I started this project long ago, shelved it after losing interest 
and un-shelved it after buying a 3DS. The NDS version started as an exercise 
in making something pretty and functional, but I lost interest before it became 
functional. I decided to skip the "pretty" step in the 3DS version and got it
working first. The "pretty" may come at some later time. The NDS version has 
now also been updated to work with the Android app.

**NOTE:** A flashcart or CFW is required on your (3)DS!

## Usage 

Build the Android app using `./gradlew build`. Either sign the generated release-version.apk with your own keys, or install the debug-version.apk (Android does not
allow installing unsigned release apks, but unsigned debug apks are fair game).

Alternatively, just [download the signed release apk](https://github.com/Louisvh/NDS-controller-android-host/releases/download/v0.3/NDS-controller-v0.3.apk) from this repository.

*NDS:*
Build the client into a .nds file using devkitARM or download it __[here](https://github.com/Louisvh/NDS-controller/releases/download/v1.2.0/NDS-controller.nds)__.
Run it using the compatible homebrew-/flashcard of your choice. Follow the 
instructions on-screen to connect it to the same WiFi network your phone is 
on (a tethered hotspot is fine too). Run the NDS-controller app on your 
Android device and follow the instructions from there.

*3DS:*
Build the client using devkitARM or download it from __[here](https://github.com/Louisvh/NDS-controller/releases/download/v1.2.0/NDS-controller.cia)__. Install it 
using your preferred method. Run the app in Android, match your 3DS client 
to the IP address displayed on the screen and follow the instructions on the 
Android app.


3DS .cia link, scan in FBI to install:  
![cia QR v1.2.0](https://user-images.githubusercontent.com/6605273/31919870-b90b7636-b865-11e7-8b23-934e8c221887.png)


## LICENSE
:[MIT LICENSE](LICENSE)

