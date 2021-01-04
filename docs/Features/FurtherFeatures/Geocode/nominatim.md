<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: geocode provider - Nominatim

[Nominatim](https://nominatim.org/) is the geocode service of [OpenStreetMap](https://www.openstreetmap.de/). It is free and there
is no API_KEY required. So the configuration file "Nominatim.cfg" can be an empty file.

#### Forward search

The implementation for the Nominatim search engine defines a square of 10km around the current
center position of the map. Starting with zoom level 11, this size doubles with each lower
zoom level. The number of result is limited to 5.

Nominatim service doesn't provide an autocompletion - so e.g. while the search for "Hauptst 20, Heidelberg" (without last "r")
doesn't return any result, the search for "Hauptstr 20, Heidelberg" returns 5 results.

#### Reverse Search 

The reverse search returns always just one result, which is rather on a rough level.

#### Summary

The search results, forward and reverse are both with a basic quality. 

<small><small>[Back to Index](../../../index.md)</small></small>