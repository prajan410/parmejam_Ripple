## Inspiration

The world is so reliant on cellular data or wifi, to the point that the signal being down tends to be a disaster. But what if there is an actual disaster? What if signal goes down and a person is in danger? Everyone's phone just becomes a useless brick at that point. I designed this app to use a phones in-built technology to increase anyone's chance of survival, even by a little bit. People die waiting for signal. Ripple finds another way forward.

## What it does

Ripple creates an offline emergency mesh network using Bluetooth Low Energy. Imagine a victim of an earthquake trapped under rubble. Signal is down and emergency services do not know someone is trapped and in danger. The victim presses SOS on the ripple app and the app sends a signal to nearby devices via bluetooth. This begins a chain reaction where every phone sends the signal to phones near it, taking advantage of the movement of people escaping the danger. As they move towards safe areas, their phones carry the signal, and the second their phone connects to the internet, or the signal reaches someone's device with internet, the signal is uploaded to the cloud for emergency services to see and act on it. There are 3 levels of danger that the user can select, allowing emergency services to designate the appropriate resources, and the app informs the cloud that the emergency is resolved once the SOS signal is turned off.

## How we built it

This was a solo project. I used an Android stack that uses the foreground BLE service as a scanner and an advertiser. I use Room DB for the deduplication by packetID (to avoid two phones sending the same signal back and forth), Firebase Firestore as the cloud that receives the signal ultimately, FusedLocation to get a highly-accurate GPS, and a ConnectivityManager to upload the signal when the connection returns. 

## Challenges we ran into

When testing the bluetooth signal being sent, Device A would initiate SOS with signal down, and Device B was supposed to receive, but both devices would send the same signal back and forth between each other, sending multiple packets of the same signal to the cloud. It turned out to be a simple fix of checking the Mac Address of the signal received and comparing it with your own mac address, but identifying the issue took hours.

## Accomplishments that we're proud of

I had a vision and I successfully pulled it off! The mesh actually works and  could send the signal infinitely until it reaches a device with internet. It doesnt duplicate each signal, so each new SOS signal creates only one new entry on firebase. It has been tested in real world, and works perfectly.

## What I learned

Sometimes the solution to a problem is amazingly simple. I spent hours trying to fix the infinite relay, but one simple Mac Address checker fixed it. BLE scanners fire 10 times per second per device. Without the checker, it was flooding firebase with thousands of duplicates. 

## What's next for parmejam_Ripple
Something I really wanted to implement but was unable to due to lack of skillset was to have the scanner be able to run in the background. Currently, the app only receives signals from other devices when the app is open. While having the app keep scanning in the background is possible, I was unable to do it. If background scanning was implemented, this could mean everyone's phone would aid in saving lives without anyone even realizing it. 
