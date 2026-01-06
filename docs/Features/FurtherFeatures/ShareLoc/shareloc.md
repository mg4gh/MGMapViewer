<small><small>[Back to Index](../../../index.md)</small></small>

## Further Feature: Share Location

This feature provides the option to to share your location with another user of this app. There are mainly two use cases considered:
- Share position usage</br>
You want to meet somebody and it's easy, if you see other persons position.
In this case one user is sending its position and the second user is listening to it immediately.
- Emergency usage</br>
In case of an emergency, e.g. if after an accident, it may happen that you are not able to help yourself.
In this scenario the second person is usually not checking the position of the first person all the time. Rather some time later the second person
wants to check the last position of the first person.

### Activate/Deactivate feature

In the GNSS settings section you can activate/deactivate this feature. 
To use this feature you need to accept the location sharing conditions.

### Share Location Settings

After activating this feature you can open the "Share Location Settings" dialog with <img src="../../../icons/group_record1.svg" width="24"/> + <img src="../../../icons/shareloc.svg" width="24"/>.
In the top section you find information about yourself. Here you can also register/unregister the usage.

#### Registration

For the use of this feature you need first to register - otherwise it would not be possible to share data with somebody specific.
The key for the registration is an email address. This email address represents your identity. After entering the email address
you receive a mail with a confirmationId, which you need to enter in the app to prove your identity.
In your device a private/public key pair is generated, and (if this confirmationId is correct) you receive a signed certificate for this
key. The private key is kept only in your device.

#### Share location with/from ...

In this section you can determine with whom you want to share your location or from whom you want to get the location. In both cases you need to create an entry for the other person.
Edit the email address of this entry to match the desired person. Verify that the certificate of others person is valid (updated automatically) - otherwise location sharing will not work.
Additionally you need to set until when you want to share or receive a location. Make sure that also the CheckBox is checked. Finally save these settings to activate them.

### Data Privacy

This feature is realized privacy friendly. It works with a minimum of data. 
See also [privacy](../../../privacy.md#share-location) description for this feature.


<small><small>[Back to Index](../../../index.md)</small></small>