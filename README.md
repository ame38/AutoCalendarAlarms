# AutoCalendarAlarms

Small Android app: pick which calendars (categories) you care about and it
automatically sets an alarm before those events start, so you don't have to
set reminders by hand every time.

## Using it

You'll need an Android device or emulator with some calendars synced to it
(a Google account works fine). There's no Play Store listing, so just clone
the repo, open it in Android Studio, and run it on your device.

First launch it'll ask for calendar read permission (to see your events) and
notification permission (to actually show you the alarm). Both are needed
for it to do anything useful.

From there:

1. On the main screen, check off the calendars you actually want alarms for.
2. Pick how far ahead you want to be warned (5/15/30/60 min, whatever suits you).
3. Tap "view upcoming events" to see what's coming up and have alarms scheduled for them.
4. Just leave it be — you'll get a notification before each event starts. There's a refresh button on the events screen if you add stuff to your calendar and want it picked up right away, and alarms should survive a reboot too.

## License

Apache 2.0, see LICENSE.
