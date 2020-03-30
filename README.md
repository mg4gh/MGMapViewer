# MGMapViewer

## Introduction
This app is based on the [mapsforge](https://github.com/mapsforge/mapsforge) library available via github. 
[Here](./docs/History.md) it is described, how this app was originated.

## Examples
<img src="./docs/screenshot/mapsforge_map.png" width="200" />&nbsp; 
<img src="./docs/screenshot/multi_map1.png" width="200" />


## License 
This software is licensed under [LGPLv3](./LICENSE)


## User documentation
The documentation consists of some markdown pages for "users" of the app:
- [Feature Overview description](./docs/FeatureOverview.md)  
- [Getting Started: Installation and first Usage of MGMapViewer](./docs/GettingStartedUsage.md)  
- [Feature Detailed description](./docs/FeatureDetails.md) - TODO 


## Developer documentation
As already stated in the introduction the capabilities to display maps from different sources are almost completely 
inherited from the  [mapsforge](https://github.com/mapsforge/mapsforge) project. 

The following documentation tries to summarize the most important aspects of the addons of this app.
It consists of
- a few more markdown pages, especially with some modelling views,
  - [Track data model](./docs/Model.md) - This class model describes the most relevant classes that are used to store
  a track with all its data.
  - [Application Model](./docs/MGMapViewer.md) - This class model describes the main architecture of the app. 
  The main activity is MGMapActivity. Most of its functions is added via different kinds of MGMicroService. The
  corresponding application is MGMapApplication, which mainly provides several Observables that keep the state 
  of the the app. 
  - [MGMapViewer View Model](./docs/images/MGMapViewer_ViewModel.PNG) - This page tries to describe how different view layers are 
  combined to the big picture of this app. 
  
  
-  the [javadoc](./docs/javadoc/index.html) of the code


