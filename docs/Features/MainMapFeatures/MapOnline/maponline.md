## Main map Feature: maponline

A maponline layer consists of a directory with the name of the layer. This is a direct subdirectory of 
`./MGMapViewer/maps/maponline/`.
Inside of this directory
there is a description file with the fix name `config.xml`. 
Such a [config.xml](./config.xml) looks like:
 
<img src="./config.png" width="400" />

The description contains all data to access an online tile server. In the urlPart definition the {z} means zoom level, {x} the x-tile number and {y} the y-tile number.

<img src="./maponline_map.png" width="400" />
</br></br>

 