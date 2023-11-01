<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: height data

This app is able to handle offline height data from ".hgt" files in various situations. These .hgt based height data will be added to all points. 
Especially they are useful in case of route planning, since you get immediately in idea about the accumulated  height gain of the route and as well they are the 
basis for the visualisation of the [height profile](../HeightProfile/hprof.md) of a route.

Information about the SRTM (Shuttle Radar Topography Mission) project can be found:
- on [Wikipedia](https://en.wikipedia.org/wiki/Shuttle_Radar_Topography_Mission)  
  "The resolution of the raw data is one arcsecond (30 m along the equator) and coverage includes Africa, Europe, North America, South America, Asia, and Australia."
- on [Earthdata](https://lpdaac.usgs.gov/products/srtmgl1v003/)

### Manual Download

There are different options for the download of the hgt data:
- From **lpdaac.usgs.gov**:  
  - Register via  https://urs.earthdata.nasa.gov/home
  - Once you are registered and logged in, you can select in your profile the menu item
    <span style="color:gray">*Application/Authorized Apps*</span>. Go to the end of the list and check
    <span style="color:gray">*Show applications that can be auto-authorized*</span>. Then select the
    entry <span style="color:gray">LP DAAC Data Pool</span> and click the **Authorize** butten.
  - Now you can download e.g. with the link
    <span style="color:gray">https://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL1.003/2000.02.11/N54E014.SRTMGL1.hgt.zip</span>
    the .hgt file for N54E014. Each file provides data for a rectangle with one degree of latitude and one degree of longitude.
- From **ESA**
  - Simply go to http://step.esa.int/auxdata/dem/SRTMGL1/ , all files like "N54E014.SRTMGL1.hgt.zip" are available for HTTP download.

Place these downloaded hgt files as they are in the directory "MGMapViewer/hgt" (do **not** unzip them).

So this folder might look like this:  
<img src="./HgtFolder2.jpg" width="400" />

### Automated Download

The [hgt grid layer](../../MainMapFeatures/MapGrid/hgt.md) provides an easy overview about the available height data.

THe Section [Downloading of missing hgt data](../../MainMapFeatures/MapGrid/hgt.md#download) describes the downlaod of missing hgt data in detail.
This automatic download is based on the ESA data (see above).

### Developer information

Here is some more information for [developer](./developer.md).

<small><small>[Back to Index](../../../index.md)</small></small>