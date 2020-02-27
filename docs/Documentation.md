# Documentation

## Introduction
This app is based on the [mapsforge](https://github.com/mapsforge/mapsforge) library available via github. 
[Here](./History.md) it is described, how this app was originated.

## User documentation
The documentation consists of some markdown pages for "users" of the app:
- [Feature Overview description](./FeatureOverview.md)  
- [Getting Started: Installation and first Usage of MGMapViewer](./GettingStartedUsage.md)  
- [Feature Detailed description](./Detailed.md) - TODO 


## Developer documentation
As already stated in the introduction the capabilities to display maps from different sources are almost completely 
inherited from the  [mapsforge](https://github.com/mapsforge/mapsforge) project. 

The following documentation tries to summarize the most important aspects of the addons of this app.
It consists of
- a few more markdown pages, especially with some modelling views,
  - [Track data model](./Model.md) - This class model describes the most relevant classes that are used to store
  a track with all its data.
  - [Application Model](./MGMapViewer.md) - This class model describes the main architecture of the app. 
  The main activity is MGMapActivity. Most of its functions is added via different kinds of MGMicroService. The
  corresponding application is MGMapApplication, which mainly provides several Observables that keep the state 
  of the the app. 
  - [MGMapViewer View Model](./images/MGMapViewer_ViewModel.PNG) - This page tries to describe how different view layers are 
  combined to the big picture of this app. 
  
  
-  the [javadoc](./javadoc/index.html) of the code


