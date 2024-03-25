<small><small>[Back to Index](../../../index.md)</small></small>

## Developer Features: Preferences 

**Caution:** use this feature only, if you exactly know what you do!

There is an option to set manually shared preferences values. Create in the config folder a file called preferences.properties with some preferences, e.g.

```
FSRouting.RoutingAlgorithm=BidirectionalAStar
FSSearch.reverseSearchOn=Boolean:true
```
By default these preferences are considered as String preferences. Using the prefix "Boolean:" in the value is changing this behaviour to a Boolean preference.

<small><small>[Back to Index](../../../index.md)</small></small>