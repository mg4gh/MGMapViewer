# How MGMapViewer was originated

Formerly I was a user of [oruxmaps](https://www.oruxmaps.com). But at a bike tour through the black forrest 
I recognized one day that the app showed me an elevation gain of about 1400m when I was close to the place 
for the overnight stay. After saving the track the height gain was reduced to less than 1000m. And since I was
using a barometer based height measurement it was clear, that a significant smoothing function make no sense. 
At a few other tours I verified this observation and although it was in most cases less significant than described 
above, it was still not acceptable.
So I wrote a [bug report](https://oruxmaps.org/forum/index.php?topic=4448.msg11716#msg11716) and expected that 
the orux people will solve this bug. At least I expected an option to switch off this smoothing function for barometric
height measurement.

(Un)fortunately they didn't and I was quite annoyed about this. Due to this I was searching the internet for good
alternatives. Up to this point I had read a couple of times the term mapsforge, but I didn't really got an idea was it is about.
To my surprise I recognized that mapsforge is a software library that contains everything that is necessary  to have an
excellent mapviewer (like I was used from oruxmaps). In fact all the nice map viewer functionality in oruxmaps is inherited  
from mapsforge. 

So although mapsforge is freely available on github there were a few none trivial problems for me:
- Up to this point of time I never tried to develop an app
- I didn't know anything about the Android design patterns 
- I wasn't used to handle AndroidStudio 
- ... and beside the sample application code with some really good examples there is almost no documentation in mapsforge.

On the other hand I had some advantages:
- I studied computer science
- Although I'm working as a system architect I was well familiar with Java coding (rather with eclipse based environment)
- And almost 20 years ago I was owner of a Magellan Meridian Platinum GPS device with expensive Mapsend maps. 
  I learned from a yahoo group most of the decoding of the Mapsend proprietary data format ... and at the end I was able to modify this kind of data to correct 
  and enrich the way data of these maps. All this happened before google maps and OSM became famous.

After the first small success experiences with the sample app from mapsforge and a lot of debugging to understand a bit the
structure of the code I decided, that I will try to make my own app - just for me - just for my personal use. This was still in 2017.

Since this time the app was working quite well for me, but I made some shortcuts (I knew the device it was for), so some aspects were
realized rather dirty. From time to time I added a few features, so the code become a little bit confusing at some points ... <br/>
Now in 2020 I decided to spend some time for a renewal of the code. With all the experiences from the last 
few years I refactored the app. After spending quite a lot of time it's too pitty to use it only for myself. So the next step is to 
provide the code via github to everybody (who is able to find it there).



