# SignalTracker

## What is it?
SignalTracker is an Android application built as part of my dissertation project that is used to measure mobile phone signal strength on the London Underground. It lets users tag stations, automatically measure signal strength, report these to a server, and identify the station a user is at. 

## Main Features
* Identify where a user is on the Underground by mapping WiFi MAC addresses to user tagged locations
* Synchronise WiFi/station mappings amongst users via a server 
* Persistent and resilient storage of signal readings
* Collect mobile signal strength, WiFi readings, and useable bandwidth
* Usable GUI for collection and building station-location mappings

## Screenshot
<img src="http://i.imgur.com/aoI5x3c.png"  height="277" width="154" > 

## Installing
If you want to use SignalTracker it is available for download on the Google Play store:
https://play.google.com/store/apps/details?id=tprz.signaltracker

Alternatively clone this repo and deploy it yourself. 

## Building
SignalTracker targets uses Gradle and should build under Android Studio.

## Usage and Results
SignalTracker was deployed and used by several commuters on the London Underground over a period of weeks. The results shown below were generated using SignalTracker. 
![SignalTracker results](http://i.imgur.com/w4F8ih3.png "SignalTracker Results")

## Acknowledgements
SignalTracker is based off of the Funf project (http://funf.org/). Thanks to their team for putting together the base framework. 

## Contributing
Happy to consider any pull requests!