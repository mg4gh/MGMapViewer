<small><small>[Back to Index](../../../index.md)</small></small>

## Developer Features: power saving alarm

Make sure that you have enabled [developer](../Developer/developer.md) mode. 

Use <img src="../../../icons/group_task.svg" width="24"/> + <img src="../../../icons/settings.svg" width="24"/>
to open the main settings preference screen. Scroll down to the <span style="color:gray">*Behaviour settings*</span>
preference catecory and switch on <span style="color:gray">*Power saving alarm*</span>.

There is one thread of the MGMapApplication that is cyclically every 10s checking, whether the logging process is still working properly.
If not, this thread tries to restart the logging process. 

As a side feature of this cyclic check, this thread verifies how much time is passed since the last check. If the time exceeds the 10s by
the factor of 1.5 (so at least 15s since the last check), this is considered as abnormal behaviour. There is an escalation counter to verify, if 
this happens multiple times in a sequence. If the escalation counter exceeds the limit (>3), then an alarm is triggered. 

There might be multiple reasons for such a situation, but one of them is that Android tries to save energy and doesn't execute this app anymore in a proper way.
The reason might also be, that there is currently no suitable GNSS signal, so Android doesn't see a reason to wakeup the app.
This feature tries to detect such a case and to warn the user. 

In case of the alarm, the sound track of "Axel F" is played, the volume is set to loud.

As soon as any activity is resumed, the sound will be stopped and the escalation counter is reset.

This feature is in an experimental state.


<small><small>[Back to Index](../../../index.md)</small></small>